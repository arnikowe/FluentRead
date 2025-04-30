package com.example.fluentread.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.example.fluentread.viewmodel.UserViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.*
import org.json.JSONObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import com.example.fluentread.R
import com.example.fluentread.ui.theme.*
import kotlinx.coroutines.delay

@OptIn(ExperimentalAnimationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(bookId: String?, chapter: String?, userViewModel: UserViewModel) {
    if (bookId == null || chapter == null) return

    val chatViewModel = remember { ChatViewModel(userViewModel) }
    val chatMessages by chatViewModel.messages.collectAsState()
    val inputText = remember { mutableStateOf(TextFieldValue()) }
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val focusManager = LocalFocusManager.current
    var showTranslationDialog by remember { mutableStateOf(false) }
    var selectedTextToTranslate by remember { mutableStateOf("") }
    var translatedText by remember { mutableStateOf("Translating...") }



    LaunchedEffect(bookId, chapter) {
        chatViewModel.startChat(bookId, chapter)
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FluentSurfaceDark)
            .pointerInput(Unit) { detectTapGestures(onTap = { focusManager.clearFocus() }) }
            .padding(top = 55.dp, start = 8.dp, end = 8.dp, bottom = 8.dp)
    ) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            state = listState
        ) {
            items(chatMessages) { message ->
                AnimatedContent(targetState = message, label = "") { targetMessage ->
                    ChatMessageBubble(
                        message = targetMessage,
                        onLongPress = { tappedText ->
                            selectedTextToTranslate = tappedText
                            showTranslationDialog = true
                            translatedText = "Translating..."
                            coroutineScope.launch {
                                userViewModel.translateWord(tappedText, tappedText) { result ->
                                    translatedText = result
                                }
                            }
                        }
                    )
                }
            }
            if (chatViewModel.isTyping) {
                item {
                    ChatTypingAnimation()
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = inputText.value,
                onValueChange = { inputText.value = it },
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp),
                placeholder = {
                    Text(
                        "Type your reply...",
                        color = FluentPrimaryDark
                    )
                },
                singleLine = true,
                textStyle = LocalTextStyle.current.copy(color = FluentSecondaryDark),
                colors = TextFieldDefaults.textFieldColors(
                    containerColor = FluentBackgroundDark,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = FluentSecondaryDark
                )
            )
            IconButton(
                onClick = {
                    val text = inputText.value.text.trim()
                    if (text.isNotEmpty()) {
                        inputText.value = TextFieldValue()
                        coroutineScope.launch {
                            chatViewModel.sendUserMessage(text)
                            listState.animateScrollToItem(chatMessages.size)
                        }
                    }
                },
                modifier = Modifier.size(40.dp)
            ) {
                Icon(imageVector = Icons.Default.Send, contentDescription = "Send", tint=FluentBackgroundDark)

            }

        }
        if (showTranslationDialog) {
            AlertDialog(
                onDismissRequest = { showTranslationDialog = false },
                confirmButton = {
                    TextButton(onClick = { showTranslationDialog = false }) {
                        Text("OK")
                    }
                },
                title = { Text("Translation") },
                text = { Text(translatedText) },
                containerColor = FluentBackgroundDark,
                textContentColor = FluentSecondaryDark
            )
        }

    }
}

@Composable
fun ChatMessageBubble(
    message: ChatMessage,
    onLongPress: ((String) -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            color = if (message.isUser) FluentOnPrimaryDark else FluentBackgroundDark,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier
                .widthIn(max = 280.dp)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onLongPress = {
                            onLongPress?.invoke(message.text)
                        }
                    )
                }
        ) {
            Text(
                text = message.text,
                modifier = Modifier.padding(12.dp),
                color = FluentSecondaryDark
            )
        }
    }
}

@Composable
fun ChatTypingAnimation() {
    val composition by rememberLottieComposition(LottieCompositionSpec.Asset("typing_animation.json"))
    val progress by animateLottieCompositionAsState(composition, iterations = LottieConstants.IterateForever)
            LottieAnimation(
                composition = composition,
                progress = { progress },
                modifier = Modifier
                    .padding(8.dp)
                    .size(50.dp)
            )
}

class ChatViewModel(private val userViewModel: UserViewModel) {
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages = _messages.asStateFlow()

    var isTyping by mutableStateOf(false)
        private set

    private var summary: String = ""
    private var bookTitle: String = ""

    suspend fun startChat(bookId: String, chapter: String) {
        summary = userViewModel.getChapterSummary(bookId, chapter)
        bookTitle = userViewModel.getBookTitle(bookId)

        sendAiMessage("Let's start our discussion about the chapter from book titled '$bookTitle'. What are your thoughts on it?")
    }

    suspend fun sendUserMessage(text: String) {
        addMessage(text, isUser = true)
        isTyping = true
        val reply = getChatReply(summary, bookTitle, text, userViewModel.translateApi)
        isTyping = false
        sendAiMessage(reply)
    }

    private fun addMessage(text: String, isUser: Boolean) {
        _messages.value = _messages.value + ChatMessage(text, isUser)
    }

    private fun sendAiMessage(text: String) {
        addMessage(text, isUser = false)
    }

    private suspend fun getChatReply(summary: String, title: String, userInput: String, apiKey: String): String {
        val prompt = "You are discussing the book titled '$title'. Here's the chapter summary: '$summary'. The user said: '$userInput'. Continue the conversation in English with answer on '$userInput' a thoughtful follow-up question."

        val url = "https://api.openai.com/v1/chat/completions"
        val client = OkHttpClient()

        val messagesArray = org.json.JSONArray().put(
            JSONObject().put("role", "user").put("content", prompt)
        )

        val json = JSONObject()
            .put("model", "gpt-3.5-turbo")
            .put("messages", messagesArray)
            .put("temperature", 0.7)

        val requestBody = json.toString().toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .build()

        return suspendCancellableCoroutine { continuation ->
            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    continuation.resume("Error: ${e.localizedMessage}") {}
                }

                override fun onResponse(call: Call, response: Response) {
                    val responseText = response.body?.string()
                    if (!response.isSuccessful || responseText == null) {
                        continuation.resume("HTTP ${response.code}") {}
                        return
                    }

                    try {
                        val jsonObject = JSONObject(responseText)
                        val reply = jsonObject.getJSONArray("choices")
                            .getJSONObject(0)
                            .getJSONObject("message")
                            .getString("content")
                            .trim()
                        continuation.resume(reply) {}
                    } catch (e: Exception) {
                        continuation.resume("Error parsing response: ${e.localizedMessage}") {}
                    }
                }
            })
        }
    }
}


data class ChatMessage(val text: String, val isUser: Boolean)


suspend fun UserViewModel.getChapterSummary(bookId: String, chapter: String): String {
    val doc = db.collection("books").document(bookId).collection("chapters").document(chapter).get().await()
    return doc.getString("summary") ?: ""
}

suspend fun UserViewModel.getBookTitle(bookId: String): String {
    val doc = db.collection("books").document(bookId).get().await()
    return doc.getString("title") ?: "Unknown Title"
}
