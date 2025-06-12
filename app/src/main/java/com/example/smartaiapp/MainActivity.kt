package com.example.smartaiapp

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
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
import java.text.SimpleDateFormat
import java.util.Locale

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
                    .imePadding()
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
fun ChatScreen(viewModel: ChatViewModel = androidx.lifecycle.viewmodel.compose.viewModel()) {
    val messageInput = remember { mutableStateOf("") }
    // Collect the messages from the ViewModel's StateFlow as Compose state
    val messages = viewModel.messages.collectAsState()
    val userId = viewModel.currentUser.value?.uid ?: "anonymous_user" // Get current user ID or default

    // Format for displaying timestamps
    val dateFormatter = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Display the userId on the UI for multi-user apps
        Text(
            text = stringResource(R.string.Conversation),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(8.dp)
        )

        // LazyColumn to display chat messages
        LazyColumn(
            modifier = Modifier
                .weight(1f) // Takes up all available space
                .fillMaxWidth(),
            reverseLayout = true, // Shows latest messages at the bottom
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 8.dp) // Padding at the bottom for new messages
        ) {
            // Display messages in reverse order to show latest at the bottom
            items(messages.value.reversed(), key = { it.id }) { message ->
                // Conditionally apply alignment and color based on message type
                val isUserMessage = message.isUser
                val alignment = if (isUserMessage) Alignment.CenterEnd else Alignment.CenterStart
                val bubbleColor = if (isUserMessage) colorResource(R.color.color_b7b7b7)
                else colorResource(R.color.color_2d3494)

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentSize(align = alignment) // Align bubble to start/end
                ) {
                    MessageBubble(message = message, dateFormatter = dateFormatter, bubbleColor = bubbleColor)
                }
            }
        }

        // Input field and send button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            CommonTextField(
                value = messageInput.value,
                onValueChange = {
                    messageInput.value = it
                },
                placeHolder = stringResource(R.string.ask_something),
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            CommonButton(
                label = stringResource(R.string.send),
                onClick = {
                if (messageInput.value.isNotBlank() && userId != "anonymous_user") {
                    viewModel.sendMessage(messageInput.value, userId)
                    messageInput.value = "" // Clear the input field after sending
                } else if (userId == "anonymous_user") {
                    Log.e("ChatScreen", "User ID not yet available. Message not sent.")
                    // Optionally show a user-friendly message or disable the button
                }
            })
        }
    }
}


@Composable
fun MessageBubble(message: ChatMessage, dateFormatter: SimpleDateFormat, bubbleColor: androidx.compose.ui.graphics.Color) {
    Card(
        modifier = Modifier
            .widthIn(max = 300.dp), // Limit bubble width
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = bubbleColor), // Dynamic background color
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(8.dp)
        ) {
            // Show truncated User ID for user, or "AI" for AI messages
            Text(
                text = if (message.isUser) "User ID : ${message.userId.substring(0, 8)}" else stringResource(R.string.ai_name),
                style = MaterialTheme.typography.labelSmall,
                color = if (message.isUser) colorResource(R.color.white) else colorResource(R.color.white)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = message.text,
                style = MaterialTheme.typography.bodyMedium,
                color = if (message.isUser) colorResource(R.color.white) else colorResource(R.color.white)
            )
            Text(
                text = dateFormatter.format(message.timestamp),
                style = MaterialTheme.typography.labelSmall,
                color = if (message.isUser) colorResource(R.color.white) else colorResource(R.color.white),
                modifier = Modifier.align(Alignment.End)
            )
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