package com.example.smartaiapp

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.firestore

object FirebaseCollectionHelper {
    private var _db: FirebaseFirestore? = null

    fun init() {
        if (_db == null) {
            _db = Firebase.firestore
        }
    }

    fun getInstance(): FirebaseFirestore {
        return _db
            ?: throw IllegalStateException("MyGeminiClient is not initialized. Call init(context) first.")
    }

    fun getDatabaseListener(userId: String,onChange:(QuerySnapshot?,FirebaseFirestoreException?)->Unit):ListenerRegistration?{
        var listener : ListenerRegistration ? = null
        if(_db != null){
          listener =  _db!!.collection(getDatabasePath(userId))
                // Fetch messages and order them by timestamp to ensure chronological display
                .orderBy("timestamp")
                .addSnapshotListener { snapshot, e ->
                    onChange.invoke(snapshot,e)
                }
        }
        return listener
    }

    fun saveMessage(message:ChatMessage,onSuccess:(ChatMessage)->Unit){
        val dbPath = getDatabasePath(message.userId)
        if(_db != null){
            _db!!.collection(dbPath)
                .add(message) // add() automatically generates a document ID
                .addOnSuccessListener { documentReference ->
                    Log.d("ChatViewModel", "DocumentSnapshot added with ID: ${documentReference.id}")
                    onSuccess.invoke(message)
                }
                .addOnFailureListener { e ->
                    Log.d("ChatViewModel", "Error adding document", e)
                }
        }
    }

    fun addMessage(userId: String,message: ChatMessage){
        if(_db != null){
            _db!!.collection(getDatabasePath(userId))
                .add(message)
                .addOnSuccessListener { aiDocRef ->
                    Log.d("ChatViewModel", "AI message added with ID: ${aiDocRef.id}")
                }
                .addOnFailureListener { e ->
                    Log.e("ChatViewModel", "Error adding AI message to Firestore", e)
                }
        }
    }

    private fun getDatabasePath(userId:String) = "artifacts/${userId}/public/data/messages"
}