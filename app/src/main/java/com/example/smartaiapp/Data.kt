package com.example.smartaiapp

import com.google.firebase.ai.type.FunctionDeclaration

data class ChatMessage(
    val id:String = "",
    val userId:String = "",
    val text: String = "",
    val timestamp: Long = 0L,
    val isUser:Boolean = false
)

