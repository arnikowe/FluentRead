package com.example.fluentread.screens

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fluentread.R
import com.example.fluentread.ui.theme.FluentBackgroundDark
import com.example.fluentread.ui.theme.FluentSecondaryDark
import com.example.fluentread.ui.theme.FluentSurfaceDark
import com.example.fluentread.ui.theme.FluentTypography
import com.example.fluentread.viewmodel.UserViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReadScreen(bookId: String?, chapter: String?, userViewModel: UserViewModel) {
    val userId = userViewModel.userId ?: return
    userViewModel.currentBookId = bookId
    userViewModel.currentChapter = chapter

    val context = LocalContext.current
    val scrollState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    var selectedWord by remember { mutableStateOf<String?>(null) }
    var manuallyBookmarked by remember { mutableStateOf(false) }
    var scrollRestored by remember { mutableStateOf(false) }
    var lastSavedOffset by remember { mutableStateOf(-1) }

    val title = userViewModel.currentTitle
    val content = userViewModel.chapterContent
    val isBookmarked by userViewModel::isBookmarked

    var translation by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var translationRequested by remember { mutableStateOf(false) }

    val textChunks = remember(content) {
        chunkTextBySentences(content, 1000)
    }


    // Ładowanie treści
    LaunchedEffect(bookId, chapter) {
        if (bookId != null && chapter != null) {
            userViewModel.loadChapter(bookId, chapter)
        }
    }

    // Przywracanie zakładki
    LaunchedEffect(content) {
        if (bookId != null && chapter != null && content.isNotBlank()) {
            userViewModel.checkBookmark(bookId, chapter, userId) { index, offset ->
                coroutineScope.launch {
                    scrollState.scrollToItem(index, offset)
                    lastSavedOffset = index * 10000 + offset
                    scrollRestored = true
                }
            }
        }
    }
    LaunchedEffect(scrollState.firstVisibleItemIndex, scrollState.firstVisibleItemScrollOffset) {
        userViewModel.updateReadingPosition(
            scrollState.firstVisibleItemIndex,
            scrollState.firstVisibleItemScrollOffset
        )
    }


    Scaffold(
        modifier = Modifier.fillMaxSize().background(FluentSurfaceDark),
        topBar = {
            TopAppBar(
                title = { Text(title, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { /* menu */ }) {
                        Icon(painterResource(R.drawable.ic_menu), contentDescription = null, tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = FluentBackgroundDark)
            )
        },
        containerColor = FluentSurfaceDark
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.padding(paddingValues).padding(16.dp),
            state = scrollState
        ) {
            item {
                Text(
                    text = "Chapter ${chapter ?: "?"}",
                    color = Color.White,
                    style = FluentTypography.titleMedium,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            items(textChunks) { chunk ->
                val annotated = remember(chunk, selectedWord) {
                    val words = Regex("\\S+").findAll(chunk).map { it.range.first to it.value }.toList()
                    buildAnnotatedString {
                        var lastIndex = 0
                        for ((index, word) in words) {
                            if (index > lastIndex) append(chunk.substring(lastIndex, index))

                            val clean = word.replace(Regex("""^[^\p{L}\p{N}]+|[^\p{L}\p{N}]+$"""), "")

                            pushStringAnnotation(tag = "WORD", annotation = clean)
                            withStyle(
                                SpanStyle(
                                    color = if (clean == selectedWord) Color.Black else Color.White,
                                    background = if (clean == selectedWord) FluentSecondaryDark else Color.Transparent,
                                    fontSize = 16.sp
                                )
                            ) {
                                append(word)
                            }
                            pop()
                            lastIndex = index + word.length
                        }
                        if (lastIndex < chunk.length) append(chunk.substring(lastIndex))
                    }
                }

                ClickableText(
                    text = annotated,
                    style = FluentTypography.bodyLarge.copy(lineHeight = 24.sp, textAlign = TextAlign.Justify),
                    onClick = { offset ->
                        annotated.getStringAnnotations("WORD", offset, offset)
                            .firstOrNull()?.let {
                                selectedWord = it.item
                                Log.d("SelectedWord", "Clicked: ${it.item}")
                            }
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

        }
    }

    if (selectedWord != null) {
        val cleanedWord = selectedWord!!.replace(Regex("""^[^\p{L}\p{N}]+|[^\p{L}\p{N}]+$"""), "")

        LaunchedEffect(cleanedWord, translationRequested) {
            if (!translationRequested) {
                translationRequested = true
                val sentence = extractSentence(content, cleanedWord)
                Log.d("ExtractedSentence", "Word: $cleanedWord → Sentence: $sentence")
                userViewModel.translateWord(cleanedWord, sentence) { result ->
                    translation = result
                }
            }
        }

        AlertDialog(
            onDismissRequest = {
                selectedWord = null
                translation = ""
                note = ""
                translationRequested = false
            },
            title = { Text("Tłumaczenie słowa: $selectedWord") },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (translation.isNotBlank()) translation else "Ładowanie...",
                            style = FluentTypography.titleMedium,
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 8.dp),
                            textAlign = TextAlign.Left
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text("Twoja notatka") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(onClick = {
                        userViewModel.saveFlashcard(
                            bookId!!,
                            chapter!!,
                            userId,
                            cleanedWord.lowercase(),
                            translation,
                            note,
                            onSuccess = {
                                Toast.makeText(context, "Dodano do fiszek", Toast.LENGTH_SHORT).show()
                                selectedWord = null
                                translation = ""
                                note = ""
                                translationRequested = false
                            },
                            onError = {
                                Toast.makeText(context, "Błąd przy dodawaniu", Toast.LENGTH_SHORT).show()
                                selectedWord = null
                                translationRequested = false
                            }
                        )
                    }) {
                        Text("Dodaj")
                    }

                    TextButton(onClick = {
                        selectedWord = null
                        translation = ""
                        note = ""
                        translationRequested = false
                    }) {
                        Text("Zamknij")
                    }
                }
            }
        )
    }

}

fun extractSentence(content: String, word: String): String {
    val sentences = content.split(Regex("(?<=[.!?])\\s+"))
    val cleanWord = word.lowercase()

    return sentences.firstOrNull { sentence ->
        Regex("""\b${Regex.escape(cleanWord)}\b""", RegexOption.IGNORE_CASE)
            .containsMatchIn(sentence)
    }?.trim() ?: word
}


fun chunkTextBySentences(content: String, maxChunkLength: Int): List<String> {
    val sentences = content.split(Regex("(?<=[.!?])\\s+"))
    val chunks = mutableListOf<String>()
    var currentChunk = StringBuilder()

    for (sentence in sentences) {
        if ((currentChunk.length + sentence.length) > maxChunkLength) {
            chunks.add(currentChunk.toString())
            currentChunk = StringBuilder()
        }
        currentChunk.append(sentence).append(" ")
    }

    if (currentChunk.isNotEmpty()) {
        chunks.add(currentChunk.toString())
    }

    return chunks
}