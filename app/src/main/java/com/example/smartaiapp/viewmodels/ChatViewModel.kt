package com.example.smartaiapp.viewmodels

import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartaiapp.ChatMessage
import com.example.smartaiapp.FirebaseAuthHelper
import com.example.smartaiapp.FirebaseCollectionHelper
import com.example.smartaiapp.GeminiClient
import com.example.smartaiapp.utils.ExternalAPI
import com.example.smartaiapp.utils.ExternalAPIParameter
import com.example.smartaiapp.utils.GeminiExternalAPIHelper
import com.google.firebase.ai.Chat
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.FunctionResponsePart
import com.google.firebase.ai.type.asTextOrNull
import com.google.firebase.ai.type.content
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive
import java.util.Date

class ChatViewModel : ViewModel() {
    // ListenerRegistration to manage the Firestore snapshot listener
    private var messagesListener: ListenerRegistration? = null
    private var chat : Chat? = null

    // MutableState for the current user, observed by the UI
    var currentUser = mutableStateOf<FirebaseUser?>(null)
        private set // Only allow modifications within the ViewModel

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages = _messages.asStateFlow()

    suspend fun getCurrentBalance(accountNumber: String): JsonObject? {
        // In a real app, this would make an API call (e.g., to OpenWeatherMap)
        // For demonstration, we'll return mock data
        return when (accountNumber.toLowerCase()){
            "123456789" -> JsonObject(mapOf(
                "accountBalance" to JsonPrimitive(12000),
                "currency" to JsonPrimitive("USD"),
            ))
            "9898"-> JsonObject(mapOf(
                "accountBalance" to JsonPrimitive(5000),
                "currency" to JsonPrimitive("KHR"),
            ))
            else -> null
        }
    }

    init {
        GeminiExternalAPIHelper.apply {
            addExternalAPI(
                ExternalAPI(
                    methodName = ::getCurrentBalance.name,
                    methodDescription = "Only use this if you cannot provide answer by query generative AI Response and the key word used related to current user balance",
                    listParameters = listOf(
                        ExternalAPIParameter(
                            parameterName = "accountNumber",
                            parameterDescription = "123456789 or 9873 or 3232323",
                            isRequired = true
                        )
                    )
                )
            )
        }
        GeminiClient.init(listExternalAPI = GeminiExternalAPIHelper.getListExternalAPI())
        // Observe authentication state changes
        FirebaseAuthHelper.init()
        FirebaseAuthHelper.addAuthenticationChangeListener { auth->
            currentUser.value = auth.currentUser
            // Sign in anonymously if no user is logged in
            if (currentUser.value == null) {
                signInAnonymously()
            }else {
                // If a user is available (either newly signed in or already logged in),
                // start listening for messages for this user.
                startListeningForMessages(currentUser.value!!.uid)
            }
        }
        FirebaseCollectionHelper.init()
        chat = GeminiClient.getInstance().startChat()
    }

    /**
     * Starts a real-time listener for chat messages from Firestore.
     * This method will populate the `_messages` StateFlow whenever data changes.
     * @param userId The ID of the user whose messages to listen for.
     */
    private fun startListeningForMessages(userId: String) {
        // Remove any existing listener to prevent duplicate listeners
        messagesListener?.remove()

        // Update List Chat Message UI
        messagesListener = FirebaseCollectionHelper.getDatabaseListener(userId, onChange = { snapshot , e ->
            if (e != null) {
                Log.w("ChatViewModel", "Listen failed.", e)
                return@getDatabaseListener
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
        })
    }

    // Function to sign in anonymously if no user is authenticated
    private fun signInAnonymously() {
        viewModelScope.launch {
            try {
                // Sign in anonymously and await the result
                val result = FirebaseAuthHelper.getInstance().signInAnonymously().await()
                currentUser.value = result.user
                Log.d("ChatViewModel", "Signed in anonymously: ${currentUser.value?.uid}")
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Anonymous sign-in failed: ${e.message}", e)
            }
        }
    }

    fun sendMessage(userText: String,userId: String) {
        // Create a new Message object
        val message = ChatMessage(
            userId = userId,
            text = userText,
            timestamp = Date().time,
            isUser = true// Current timestamp
        )
        FirebaseCollectionHelper.saveMessage(message){ messageObj ->
            callGeminiChatFunction(messageObj.text, messageObj.userId)
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
                val prompt = userText
                val result = chat!!.sendMessage(prompt)
                val functionCalls = result.functionCalls
                val getCurrentBalanceCall = functionCalls.find { it.name == ::getCurrentBalance.name }
                val functionResponse = getCurrentBalanceCall?.let {
                    val accountNumber = it.args["accountNumber"]!!.jsonPrimitive.content
                    getCurrentBalance(accountNumber = accountNumber)
                }

                var finalResponse = result

                if(functionResponse != null){
                    finalResponse = chat!!.sendMessage(content("function") {
                        part(FunctionResponsePart("getAccountBalance", functionResponse))
                    })
                }
                if(finalResponse.candidates.isNotEmpty()){
                    val candidate = finalResponse.candidates.first().content
                    // 3. Save AI's response to Firestore
                    val aiMessage = ChatMessage(
                        userId = "AI",
                        text = candidate.parts.first().asTextOrNull() ?: "",
                        timestamp = Date().time,
                        isUser = false
                    )
                    FirebaseCollectionHelper.addMessage(sessionId,aiMessage)
                }

            } catch (e: Exception) {
                Log.e("ChatViewModel", "Error calling Cloud Function or processing response: ${e.message}", e)
                val errorMessage = ChatMessage(
                    userId = "AI",
                    text = "Oops! Something went wrong with the AI. Please try again.",
                    timestamp = Date().time,
                    isUser = false
                )
                FirebaseCollectionHelper.addMessage(sessionId,errorMessage)
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