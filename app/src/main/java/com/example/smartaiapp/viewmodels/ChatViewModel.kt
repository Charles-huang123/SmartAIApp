package com.example.smartaiapp.viewmodels

import android.app.Application
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartaiapp.ChatMessage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ChatViewModel : ViewModel() {
    var messages = mutableStateListOf<ChatMessage>()
        private set
    fun sendMessage(inputMsg:String){
        messages.add(ChatMessage(inputMsg, isUser = true))
        viewModelScope.launch {
            delay(1000)
            messages.add(ChatMessage("Prince Bank: You said '$inputMsg'", isUser = false))
        }
    }
}