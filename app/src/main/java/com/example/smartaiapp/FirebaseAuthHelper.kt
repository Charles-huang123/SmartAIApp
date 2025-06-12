package com.example.smartaiapp

import com.google.firebase.auth.FirebaseAuth

object FirebaseAuthHelper {

    private var _auth: FirebaseAuth? = null

    fun init() {
        if (_auth == null) {
            _auth = FirebaseAuth.getInstance()
        }
    }

    fun addAuthenticationChangeListener(onChangeListener:(FirebaseAuth)->Unit){
        // Observe authentication state changes
        _auth?.addAuthStateListener { firebaseAuth ->
            onChangeListener.invoke(firebaseAuth)
        }
    }

    fun getInstance(): FirebaseAuth {
        return _auth
            ?: throw IllegalStateException("MyGeminiClient is not initialized. Call init(context) first.")
    }
}