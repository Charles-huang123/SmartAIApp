package com.example.smartaiapp.viewmodels

import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartaiapp.ChatMessage
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.firestore
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.functions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Date

class ChatViewModel : ViewModel() {
    // Initialize Firebase Firestore and Auth instances
    private val db: FirebaseFirestore = Firebase.firestore
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val functions: FirebaseFunctions = Firebase.functions
    // ListenerRegistration to manage the Firestore snapshot listener
    private var messagesListener: ListenerRegistration? = null

    // MutableState for the current user, observed by the UI
    var currentUser = mutableStateOf<FirebaseUser?>(null)
        private set // Only allow modifications within the ViewModel

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages = _messages.asStateFlow()

    // Initialize user authentication when the ViewModel is created
    init {
        // Observe authentication state changes
        auth.addAuthStateListener { firebaseAuth ->
            currentUser.value = firebaseAuth.currentUser
            // Sign in anonymously if no user is logged in
            if (currentUser.value == null) {
                signInAnonymously()
            }else {
                // If a user is available (either newly signed in or already logged in),
                // start listening for messages for this user.
                startListeningForMessages(currentUser.value!!.uid)
            }
        }
    }

    /**
     * Starts a real-time listener for chat messages from Firestore.
     * This method will populate the `_messages` StateFlow whenever data changes.
     * @param userId The ID of the user whose messages to listen for.
     */
    private fun startListeningForMessages(userId: String) {
        // Remove any existing listener to prevent duplicate listeners
        messagesListener?.remove()

        val collectionPath = "artifacts/${userId}/public/data/messages"

        messagesListener = db.collection(collectionPath)
            // Fetch messages and order them by timestamp to ensure chronological display
            .orderBy("timestamp")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w("ChatViewModel", "Listen failed.", e)
                    return@addSnapshotListener
                }

                if (snapshot != null && !snapshot.isEmpty) {
                    val messagesList = snapshot.documents.mapNotNull { document ->
                        try {
                            // Convert Firestore document to Message data class
                            document.toObject(ChatMessage::class.java)?.copy(id = document.id, isUser = document.data?.get("user") as? Boolean ?: false)
                        } catch (e: Exception) {
                            Log.e("ChatViewModel", "Error converting document to Message: ${e.message}", e)
                            null
                        }
                    }
                    _messages.value = messagesList
                    Log.d("ChatViewModel", "Messages updated: ${messagesList.size}")
                } else {
                    Log.d("ChatViewModel", "Current data: null or empty")
                    _messages.value = emptyList() // Clear messages if no data
                }
            }
    }

    // Function to sign in anonymously if no user is authenticated
    private fun signInAnonymously() {
        viewModelScope.launch {
            try {
                // Sign in anonymously and await the result
                val result = auth.signInAnonymously().await()
                currentUser.value = result.user
                Log.d("ChatViewModel", "Signed in anonymously: ${currentUser.value?.uid}")
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Anonymous sign-in failed: ${e.message}", e)
            }
        }
    }

    /**
     * Sends a chat message to Firestore.
     * @param text The content of the message.
     * @param userId The ID of the user sending the message.
     */
    fun sendMessage(text: String, userId: String) {
        // Create a new Message object
        val message = ChatMessage(
            userId = userId,
            text = text,
            timestamp = Date().time,
            isUser = true// Current timestamp
        )

        // Add the message to the 'chats' collection in Firestore.
        // For a simple "hello" test, we'll just add it to a 'messages' subcollection
        // under a specific user's public chat data, as recommended in the immersive.
        // In a real app, you might structure this differently (e.g., chat rooms).
        val collectionPath = "artifacts/${userId}/public/data/messages" // Example path for public data

        db.collection(collectionPath)
            .add(message) // add() automatically generates a document ID
            .addOnSuccessListener { documentReference ->
                Log.d("ChatViewModel", "DocumentSnapshot added with ID: ${documentReference.id}")
                callGeminiChatFunction(text, userId)
            }
            .addOnFailureListener { e ->
                Log.d("ChatViewModel", "Error adding document", e)
            }
    }
    /**
     * Calls the Firebase Cloud Function to chat directly with Gemini.
     * @param userText The user's input text.
     * @param sessionId The session ID for the conversation (user's UID).
     */
    private fun callGeminiChatFunction(userText: String, sessionId: String) { // Renamed for clarity
        viewModelScope.launch {
            try {
                Log.d("ChatViewModel", "Calling chatWithGemini Cloud Function...")
                val data = hashMapOf(
                    "text" to userText
                    // sessionId is NOT needed here, as the function gets it from context.auth.uid
                )

                // Call the 'chatWithGemini' Cloud Function
                val result = functions
                    .getHttpsCallable("chatWithGemini") // <<< NEW FUNCTION NAME
                    .call(data)
                    .await()

                Log.d("ChatViewModel", "Cloud Function call successful. Result: ${result.data}")
                val responseData = result.data as? Map<String, Any>
                val fulfillmentText = responseData?.get("text") as? String // Expecting 'text' key from Gemini response

                if (!fulfillmentText.isNullOrBlank()) {
                    // 3. Save AI's response to Firestore
                    val aiMessage = ChatMessage(
                        userId = "AI",
                        text = fulfillmentText,
                        timestamp = Date().time,
                        isUser = false
                    )
                    val collectionPath = "artifacts/${sessionId}/public/data/messages"

                    db.collection(collectionPath)
                        .add(aiMessage)
                        .addOnSuccessListener { aiDocRef ->
                            Log.d("ChatViewModel", "AI message added with ID: ${aiDocRef.id}")
                        }
                        .addOnFailureListener { e ->
                            Log.e("ChatViewModel", "Error adding AI message to Firestore", e)
                        }
                } else {
                    Log.w("ChatViewModel", "Gemini returned an empty or null fulfillment text.")
                    val collectionPath = "artifacts/${sessionId}/public/data/messages"
                    val fallbackMessage = ChatMessage(
                        userId = "AI",
                        text = "I'm sorry, I couldn't get a clear response from the AI. Can you rephrase?",
                        timestamp = Date().time,
                        isUser = false
                    )
                    db.collection(collectionPath)
                        .add(fallbackMessage)
                        .addOnSuccessListener { Log.d("ChatViewModel", "Fallback AI message added.") }
                        .addOnFailureListener { Log.w("ChatViewModel", "Error adding fallback AI message", it) }
                }

            } catch (e: Exception) {
                Log.e("ChatViewModel", "Error calling Cloud Function or processing response: ${e.message}", e)
                val errorMessage = ChatMessage(
                    userId = "AI",
                    text = "Oops! Something went wrong with the AI. Please try again.",
                    timestamp = Date().time,
                    isUser = false
                )
                val collectionPath = "artifacts/${sessionId}/public/data/messages"
                db.collection(collectionPath)
                    .add(errorMessage)
                    .addOnSuccessListener { Log.d("ChatViewModel", "Error AI message added.") }
                    .addOnFailureListener { Log.w("ChatViewModel", "Error adding error AI message", it) }
            }
        }
    }
    // Remember to remove the listener when the ViewModel is no longer needed
    override fun onCleared() {
        super.onCleared()
        messagesListener?.remove()
        Log.d("ChatViewModel", "Firestore listener removed.")
    }
}