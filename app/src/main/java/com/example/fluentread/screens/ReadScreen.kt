package com.example.fluentread.screens

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
    val context = LocalContext.current
    val scrollState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    var selectedWord by remember { mutableStateOf<String?>(null) }
    var manuallyBookmarked by remember { mutableStateOf(false) }
    var scrollRestored by remember { mutableStateOf(false) }
    var lastSavedOffset by remember { mutableStateOf(-1) }

    val title = userViewModel.currentTitle
    val content = userViewModel.chapterContent
    val isBookmarked = userViewModel.isBookmarked

    // Ładowanie treści
    LaunchedEffect(bookId, chapter) {
        if (bookId != null && chapter != null) {
            userViewModel.loadChapter(bookId, chapter)
        }
    }

    // Przywracanie zakładki
    LaunchedEffect(content) {
        if (bookId != null && chapter != null && content.isNotBlank()) {
            userViewModel.checkBookmark(bookId, chapter, userId) { offset ->
                coroutineScope.launch {
                    scrollState.scrollToItem(0, offset)
                    lastSavedOffset = offset
                    scrollRestored = true
                }
            }
        }
    }

    // Auto-zapisywanie zakładki
    LaunchedEffect(scrollState.firstVisibleItemScrollOffset) {
        val newOffset = scrollState.firstVisibleItemScrollOffset
        if (scrollRestored && manuallyBookmarked && kotlin.math.abs(newOffset - lastSavedOffset) > 50) {
            userViewModel.toggleBookmark(bookId!!, chapter!!, userId, newOffset, content)
            lastSavedOffset = newOffset
        }
    }

    val annotatedText = remember(content, selectedWord) {
        val wordsWithIndices = Regex("\\S+").findAll(content).map { it.range.first to it.value }.toList()
        buildAnnotatedString {
            var lastIndex = 0
            for ((index, word) in wordsWithIndices) {
                if (index > lastIndex) append(content.substring(lastIndex, index))
                val clean = word.trim { it.isWhitespace() || it in ".:,!?\"()[]" }
                pushStringAnnotation(tag = "WORD", annotation = word)
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
            if (lastIndex < content.length) append(content.substring(lastIndex))
        }
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
                actions = {
                    IconButton(onClick = {
                        manuallyBookmarked = true
                        userViewModel.toggleBookmark(bookId!!, chapter!!, userId, scrollState.firstVisibleItemScrollOffset, content)
                    }) {
                        Icon(
                            painterResource(
                                if (isBookmarked) R.drawable.ic_bookmark_full else R.drawable.ic_bookmark
                            ),
                            contentDescription = null,
                            tint = Color.White
                        )
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

            item {
                ClickableText(
                    text = annotatedText,
                    style = FluentTypography.bodyLarge.copy(lineHeight = 24.sp, textAlign = TextAlign.Justify),
                    onClick = { offset ->
                        annotatedText.getStringAnnotations("WORD", offset, offset)
                            .firstOrNull()?.let {
                                selectedWord = it.item.trim { it.isWhitespace() || it in ".:,!?\"()[]" }
                            }
                    }
                )
            }
        }
    }

    // AlertDialog dla słowa
    if (selectedWord != null) {
        var translation by remember { mutableStateOf("") }
        var note by remember { mutableStateOf("") }

        // Automatyczne tłumaczenie słowa
        LaunchedEffect(selectedWord) {
            userViewModel.translateWord(selectedWord!!) { result ->
                translation = result
            }
        }

        AlertDialog(
            onDismissRequest = {
                selectedWord = null
                translation = ""
                note = ""
            },
            title = { Text("Słowo: $selectedWord") },
            text = {
                Column {
                    Text("Tłumaczenie: ${if (translation.isNotBlank()) translation else "Ładowanie..."}")
                    Spacer(modifier = Modifier.height(8.dp))
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
                            selectedWord!!.lowercase(),
                            translation,
                            note,
                            onSuccess = {
                                Toast.makeText(context, "Dodano do fiszek", Toast.LENGTH_SHORT).show()
                                selectedWord = null
                                translation = ""
                                note = ""
                            },
                            onError = {
                                Toast.makeText(context, "Błąd przy dodawaniu", Toast.LENGTH_SHORT).show()
                                selectedWord = null
                            }
                        )
                    }) {
                        Text("Dodaj")
                    }

                    TextButton(onClick = { selectedWord = null }) {
                        Text("Zamknij")
                    }
                }
            }
        )

    }
}
