package com.example.smartaiapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.smartaiapp.ui.theme.SmartAIAppTheme
import com.example.smartaiapp.utils.CommonButton
import com.example.smartaiapp.utils.CommonTextField
import com.example.smartaiapp.utils.PrinceBackgroundWrapper
import com.example.smartaiapp.viewmodels.ChatViewModel
import com.google.firebase.FirebaseApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)
        enableEdgeToEdge()
        setContent {
            SmartAIAppTheme {
                Scaffold(modifier =
                Modifier
                    .fillMaxSize()
                ) { innerPadding ->
                    Surface(
                        modifier = Modifier
                            .fillMaxSize(),
                        color = colorResource(R.color.white)
                    ) {
                        PrinceBackgroundWrapper(modifier = Modifier.padding(innerPadding)){
                            ChatScreen()
                        }
                    }
                }
            }
        }
    }
}



@Composable
fun ChatScreen(modifier : Modifier = Modifier) {
    val chatViewModel : ChatViewModel = viewModel()
    val listState = rememberLazyListState()
    val messages = chatViewModel.messages
    val userId = chatViewModel.currentUser.value?.uid ?: "anonymous_user" // Get current user ID or default
    val inputText = remember { mutableStateOf("") }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = modifier
            .imePadding()
            .fillMaxSize()
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                // Reduce padding here or remove if you want it tight
                .padding(horizontal = 4.dp, vertical = 2.dp) // smaller padding
        ) {
            items(messages) { message ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start
                ) {
                    Text(
                        text = message.text,
                        modifier = Modifier
                            .padding(4.dp) // smaller padding around message bubble
                            .background(if (message.isUser) colorResource(R.color.color_b7b7b7) else colorResource(R.color.color_2d3494), RoundedCornerShape(8.dp))
                            .padding(6.dp), // smaller inner padding for bubble
                        color = colorResource(R.color.white)
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp) // reduce vertical padding here
        ) {
            CommonTextField(
                value = inputText.value,
                onValueChange = { inputText.value = it },
                modifier = Modifier.weight(1f),
                placeHolder = stringResource(R.string.ask_something)
            )
            Spacer(modifier = Modifier.width(8.dp))
            CommonButton(label = stringResource(R.string.send), onClick = {
                if (inputText.value.isNotBlank()) {
                    chatViewModel.sendMessage(inputText.value, userId)
                    inputText.value = ""
                }
            })
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    SmartAIAppTheme {
        ChatScreen()
    }
}