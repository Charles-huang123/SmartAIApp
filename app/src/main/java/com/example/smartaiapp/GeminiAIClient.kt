package com.example.smartaiapp

import com.google.firebase.Firebase
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.FunctionDeclaration
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.Tool

object GeminiClient {

    private var _geminiClient: GenerativeModel? = null

    fun init(
        modelName: String = MODEL_NAME,
        listExternalAPI:List<Tool>
    ) {
        if (_geminiClient == null) {
         _geminiClient = Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel(
                modelName = modelName,
                // Provide the function declaration to the model.
                tools = listExternalAPI,
            )
        }
    }

    fun getInstance(): GenerativeModel {
        return _geminiClient
            ?: throw IllegalStateException("MyGeminiClient is not initialized. Call init(context) first.")
    }
}