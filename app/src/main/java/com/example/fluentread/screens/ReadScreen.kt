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
    val userId = userViewModel.userId
    val db = userViewModel.db


    var currentTitle by remember { mutableStateOf("Tytuł książki") }
    var chapterContent by remember { mutableStateOf("") }
    var selectedWord by remember { mutableStateOf<String?>(null) }
    var isBookmarked by remember { mutableStateOf(false) }
    val scrollState = rememberLazyListState()
    var scrollRestored by remember { mutableStateOf(false) }
    var lastSavedOffset by remember { mutableStateOf(-1) }
    var manuallyBookmarked by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current



    // Ładowanie danych książki i rozdziału
    LaunchedEffect(Unit) {
        if (userId != null && bookId != null && chapter != null) {
            try {
                val bookRef = db.collection("books").document(bookId)
                val chapterRef = bookRef.collection("chapters").document(chapter)

                val (bookSnapshot, chapterSnapshot) = listOf(
                    bookRef.get(),
                    chapterRef.get()
                ).map { it.await() }

                currentTitle = bookSnapshot.getString("title") ?: "Tytuł nieznany"
                chapterContent = chapterSnapshot.getString("content") ?: ""

            } catch (e: Exception) {
                Log.e("ReadScreen", "Błąd podczas pobierania danych: ${e.localizedMessage}")
            }
        }
    }

    LaunchedEffect(chapterContent) {
        if (userId != null && bookId != null && chapter != null && chapterContent.isNotBlank()) {
            val bookmarkSnap = db.collection("books")
                .document(bookId)
                .collection("chapters").document(chapter)
                .collection("bookmarks").document(userId)
                .get()
                .await()

            if (bookmarkSnap.exists()) {
                isBookmarked = true
                val offset = bookmarkSnap.getLong("scrollOffset")?.toInt() ?: 0
                scrollState.scrollToItem(0, offset)
                lastSavedOffset = offset
            }
            scrollRestored = true
        }
    }

    LaunchedEffect(scrollState.firstVisibleItemScrollOffset) {
        val newOffset = scrollState.firstVisibleItemScrollOffset
        if (
            scrollRestored &&
            userId != null &&
            bookId != null &&
            chapter != null &&
            kotlin.math.abs(newOffset - lastSavedOffset) > 50 &&
            isBookmarked &&
            manuallyBookmarked // tylko jeśli użytkownik dodał zakładkę ręcznie
        )
        {
            db.collection("books").document(bookId)
                .collection("chapters").document(chapter)
                .collection("bookmarks").document(userId)
                .set(
                    mapOf(
                        "chapter" to chapter,
                        "scrollOffset" to newOffset,
                        ("preview" to chapterContent.split(Regex("[.!?]"))
                            .firstOrNull()?.take(100) ?: chapterContent.take(100)) as Pair<Any, Any>,
                        "timestamp" to System.currentTimeMillis()
                    )
                )
            lastSavedOffset = newOffset
        }
    }




    // Annotated text – każde słowo osobno oznaczone
    val wordsWithIndices = remember(chapterContent) {
        Regex("\\S+").findAll(chapterContent).map { it.range.first to it.value }.toList()
    }

    val annotatedText = remember(chapterContent, selectedWord) {
        buildAnnotatedString {
            var lastIndex = 0
            for ((index, word) in wordsWithIndices) {
                if (index > lastIndex) {
                    append(chapterContent.substring(lastIndex, index))
                }

                val clean = word.trim { it.isWhitespace() || it in ".:,!?\"()[]" }

                pushStringAnnotation(tag = "WORD", annotation = word)
//podkreślanie klikniętego słow - rozważyć
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

            if (lastIndex < chapterContent.length) {
                append(chapterContent.substring(lastIndex))
            }
        }
    }


    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(FluentSurfaceDark)
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        text = currentTitle,
                        style = FluentTypography.titleLarge,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { /* menu */ }) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_menu),
                            contentDescription = "Menu",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {
                        manuallyBookmarked = true
                        if (userId != null && bookId != null && chapter != null) {
                            val ref = db.collection("books").document(bookId)
                                .collection("chapters").document(chapter)
                                .collection("bookmarks").document(userId)

                            if (isBookmarked) {
                                ref.delete()
                                isBookmarked = false
                            } else {
                                val offset = scrollState.firstVisibleItemScrollOffset
                                val previewText = chapterContent.split(Regex("[.!?]"))
                                    .firstOrNull()?.take(100) ?: chapterContent.take(100)


                                ref.set(
                                    mapOf(
                                        "chapter" to chapter,
                                        "scrollOffset" to offset,
                                        "preview" to previewText,
                                        "timestamp" to System.currentTimeMillis()
                                    )
                                )
                                isBookmarked = true
                            }
                        }
                    }) {
                        Icon(
                            painter = painterResource(
                                id = if (isBookmarked) R.drawable.ic_bookmark_full else R.drawable.ic_bookmark
                            ),
                            contentDescription = if (isBookmarked) "Usuń zakładkę" else "Dodaj zakładkę",
                            tint = Color.White
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = FluentBackgroundDark
                )
            )
        },
        containerColor = FluentSurfaceDark
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            state = scrollState
        ) {
            item {
                Text(
                    text = "Chapter ${chapter ?: "?"}",
                    style = FluentTypography.titleMedium,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                )
            }

            item {
                ClickableText(
                    text = annotatedText,
                    style = FluentTypography.bodyLarge.copy(
                        fontSize = 16.sp,
                        lineHeight = 24.sp,
                        textAlign = TextAlign.Justify,
                        color = Color.White
                    ),
                    onClick = { offset ->
                        annotatedText.getStringAnnotations("WORD", offset, offset)
                            .firstOrNull()?.let { annotation ->
                                val cleanWord = annotation.item.trim { it.isWhitespace() || it in ".:,!?\"()[]" }
                                selectedWord = cleanWord
                            }
                    }
                )
            }
        }
    }
    if (selectedWord != null) {
        AlertDialog(
            onDismissRequest = { selectedWord = null },
            title = { Text("Słowo: $selectedWord") },
            text = { Text("Tutaj możesz dodać tłumaczenie, definicję lub inne akcje.") },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(onClick = {
                        if (userId != null && bookId != null && chapter != null) {
                            val word = selectedWord!!.lowercase()
                            val flashcardRef = db.collection("books")
                                .document(bookId)
                                .collection("chapters")
                                .document(chapter)
                                .collection("flashcards")
                                .document(word)

                            coroutineScope.launch {
                                try {
                                    val snapshot = flashcardRef.get().await()
                                    if (!snapshot.exists()) {
                                        flashcardRef.set(mapOf("word" to word))
                                        Toast.makeText(context, "Dodano do fiszek", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "To słowo już istnieje w fiszkach", Toast.LENGTH_SHORT).show()
                                    }
                                } catch (e: Exception) {
                                    Log.e("FlashcardAdd", "Błąd podczas dodawania fiszki: ${e.localizedMessage}")
                                    Toast.makeText(context, "Błąd podczas dodawania", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                        selectedWord = null
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
