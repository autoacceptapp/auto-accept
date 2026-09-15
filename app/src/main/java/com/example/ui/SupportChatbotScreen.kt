package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.launch

val ChatSlate950 = Color(0xFF020617)
val ChatSlate900 = Color(0xFF0F172A)
val ChatSlate800 = Color(0xFF1E293B)
val ChatCyan400 = Color(0xFF22D3EE)
val ChatSlate100 = Color(0xFFF1F5F9)
val ChatSlate300 = Color(0xFFCBD5E1)

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val text: String,
    val isUser: Boolean,
    val isError: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupportChatbotScreen(onNavigateBack: () -> Unit) {
    val coroutineScope = rememberCoroutineScope()
    
    // Replace with your actual Gemini API Key (e.g., from BuildConfig)
    val apiKey = "YOUR_GEMINI_API_KEY_HERE"

    val generativeModel = remember {
        GenerativeModel(
            modelName = "gemini-1.5-flash",
            apiKey = apiKey,
            systemInstruction = content {
                text("You are the official support agent for the 'Rapido Auto Accept' Android app. The app helps drivers auto-accept rides using Accessibility Services. Answer in short, friendly Hindi or English. Common fixes: If auto-accept fails, tell them to check the Master Switch, Distance/Fare filters, and allow 'Display Over Other Apps'. If Accessibility turns off automatically, tell them to disable Battery Optimization. To earn passes, tell them to use the Referral system. Do not break character.")
            }
        )
    }

    val chat = remember { generativeModel.startChat() }
    val messages = remember { mutableStateListOf<ChatMessage>() }
    var inputText by remember { mutableStateOf("") }
    var isTyping by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    // Add initial greeting
    LaunchedEffect(Unit) {
        messages.add(ChatMessage(text = "Hello! I am the Auto Accept Support Assistant. How can I help you today?", isUser = false))
    }

    // Auto-scroll to bottom
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    fun sendMessage() {
        if (inputText.isBlank()) return
        val textToSend = inputText.trim()
        inputText = ""
        messages.add(ChatMessage(text = textToSend, isUser = true))
        
        isTyping = true
        coroutineScope.launch {
            try {
                // If the user hasn't provided a key, mimic an error.
                if (apiKey == "YOUR_GEMINI_API_KEY_HERE") {
                    throw Exception("API Key missing! Please set GEMINI_API_KEY in code.")
                }
                
                val response = chat.sendMessage(textToSend)
                response.text?.let { reply ->
                    messages.add(ChatMessage(text = reply, isUser = false))
                }
            } catch (e: Exception) {
                messages.add(ChatMessage(text = "Oops! Something went wrong: ${e.message}", isUser = false, isError = true))
            } finally {
                isTyping = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.SupportAgent, contentDescription = null, tint = ChatCyan400, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("AI Support", fontWeight = FontWeight.Bold, color = ChatSlate100)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = ChatSlate100)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ChatSlate950)
            )
        },
        containerColor = ChatSlate950
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                state = listState,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item { Spacer(modifier = Modifier.height(8.dp)) }
                
                items(messages, key = { it.id }) { message ->
                    ChatBubble(message)
                }
                
                if (isTyping) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Start
                        ) {
                            Box(
                                modifier = Modifier
                                    .background(ChatSlate900, RoundedCornerShape(16.dp))
                                    .padding(horizontal = 16.dp, vertical = 12.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = ChatCyan400,
                                    strokeWidth = 2.dp
                                )
                            }
                        }
                    }
                }
                
                item { Spacer(modifier = Modifier.height(16.dp)) }
            }

            // Input Box
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ChatSlate900)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Ask a question...", color = ChatSlate300) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = ChatSlate950,
                        unfocusedContainerColor = ChatSlate950,
                        focusedBorderColor = ChatCyan400,
                        unfocusedBorderColor = ChatSlate800,
                        focusedTextColor = ChatSlate100,
                        unfocusedTextColor = ChatSlate100
                    ),
                    shape = RoundedCornerShape(24.dp),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    maxLines = 3
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = { sendMessage() },
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = if (inputText.isNotBlank()) ChatCyan400 else ChatSlate800,
                        contentColor = if (inputText.isNotBlank()) ChatSlate950 else ChatSlate300
                    ),
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ChatBubble(message: ChatMessage) {
    val isUser = message.isUser
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .background(
                    color = if (isUser) ChatCyan400 else ChatSlate900,
                    shape = RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isUser) 16.dp else 4.dp,
                        bottomEnd = if (isUser) 4.dp else 16.dp
                    )
                )
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text(
                text = message.text,
                color = if (isUser) ChatSlate950 else if (message.isError) Color(0xFFFCA5A5) else ChatSlate100,
                fontSize = 15.sp,
                lineHeight = 22.sp
            )
        }
    }
}
