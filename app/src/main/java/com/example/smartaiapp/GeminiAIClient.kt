package com.example.smartaiapp

import com.google.firebase.Firebase
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Content
import com.google.firebase.ai.type.FunctionCallingConfig
import com.google.firebase.ai.type.FunctionDeclaration
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.Part
import com.google.firebase.ai.type.TextPart
import com.google.firebase.ai.type.Tool
import com.google.firebase.ai.type.ToolConfig

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
             systemInstruction = Content(
                 role = "model",
                 parts = listOf(
                     TextPart(       "You are a helpful and versatile AI assistant. " +
                             "You can answer general knowledge questions, engage in conversation, and utilize provided tools. " +
                             "If a user's request explicitly and clearly requires a specific tool, use that tool. " +
                             "However, if the available tools lack the necessary functionality to fulfill the user's request, " +
                             "or if the request is a general question that can be answered with your core generative AI capabilities, " +
                             "then provide a direct, natural language response. " +
                             "Do not invent tool arguments or call tools unnecessarily. Prioritize providing a helpful response."),
                 )
             )
            )
        }
    }

    fun getInstance(): GenerativeModel {
        return _geminiClient
            ?: throw IllegalStateException("MyGeminiClient is not initialized. Call init(context) first.")
    }
}