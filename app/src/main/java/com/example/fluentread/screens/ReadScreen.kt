package com.example.fluentread.screens

import android.util.Log
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.toLowerCase
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.example.fluentread.R
import com.example.fluentread.ui.theme.FluentBackgroundDark
import com.example.fluentread.ui.theme.FluentSecondaryDark
import com.example.fluentread.ui.theme.FluentSurfaceDark
import com.example.fluentread.ui.theme.FluentTypography
import com.example.fluentread.viewmodel.UserViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReadScreen(bookId: String?, chapter: String?, userViewModel: UserViewModel, navController: NavController) {
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

    val content = userViewModel.chapterContent
    var isLoading by remember { mutableStateOf(true) }

    var translation by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var translationRequested by remember { mutableStateOf(false) }

    val textChunks = remember(content) {
        chunkTextBySentences(content, 1000)
    }

    var textPadding by remember { mutableStateOf(8.dp) }
    var fontSize by remember { mutableStateOf(16.sp) }
    var textColor by remember { mutableStateOf(Color.White) }
    var backgroundColor by remember { mutableStateOf(FluentSurfaceDark) }
    var selectedFont by remember { mutableStateOf(FontFamily.Default) }
    val availableTextColors = listOf(
        Color.White, Color.Black, Color.Gray, Color.Yellow, Color.Cyan, Color.Red, Color.Green, Color.Magenta
    )
    val availableBackgroundColors = listOf(
        FluentSurfaceDark, Color.White, Color(0xFFFAF3E0), Color(0xFF333333), Color(0xFFE0F7FA), Color(0xFFF8BBD0), Color(0xFFB2DFDB)
    )
    var selectedTextColor by remember { mutableStateOf(textColor) }
    var selectedBackgroundColor by remember { mutableStateOf(backgroundColor) }
    var detectedSentence by remember { mutableStateOf("") }

    val availableFonts = listOf(
        FontFamily.Default,
        FontFamily.Serif,
        FontFamily.SansSerif,
        FontFamily.Monospace
    )
    val fontNames = listOf(
        "Domyślna",
        "Szeryfowa",
        "Bezszeryfowa",
        "Monospace"
    )

    // Ładowanie treści
    LaunchedEffect(bookId, chapter) {
        if (bookId != null && chapter != null) {
            userViewModel.loadChapter(bookId, chapter)
        }
    }
    LaunchedEffect(bookId) {
        if (bookId != null && userId != null) {
            chapter?.toInt()?.let { userViewModel.updateLastRead(userId, bookId, it) }
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


    LaunchedEffect(Unit) {
        delay(1500)
        isLoading = false
    }

    if (isLoading) {
        val composition by rememberLottieComposition(LottieCompositionSpec.Asset("book_animation.json"))
        val progress by animateLottieCompositionAsState(composition)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(FluentSurfaceDark),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                LottieAnimation(
                    composition = composition,
                    progress = { progress },
                    modifier = Modifier.size(200.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                CircularProgressIndicator(color = FluentSecondaryDark)
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Rozpoczynamy czytanie...",
                    color = Color.White,
                    style = FluentTypography.bodyMedium
                )
            }
        }
    } else {
        Scaffold(
            modifier = Modifier.fillMaxSize().background(FluentSurfaceDark),
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
                    val annotated = remember(chunk, selectedWord, fontSize, textColor) {
                        val words = Regex("\\S+").findAll(chunk).map { it.range.first to it.value }.toList()
                        buildAnnotatedString {
                            var lastIndex = 0
                            for ((index, word) in words) {
                                if (index > lastIndex) append(chunk.substring(lastIndex, index))

                                val clean = word.replace(Regex("""^[^\p{L}\p{N}]+|[^\p{L}\p{N}]+$"""), "")

                                pushStringAnnotation(tag = "WORD", annotation = clean)
                                withStyle(
                                    SpanStyle(
                                        color = if (clean == selectedWord) Color.Black else textColor,
                                        background = if (clean == selectedWord) FluentSecondaryDark else Color.Transparent,
                                        fontSize = fontSize
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
                        style = FluentTypography.bodyLarge.copy(
                            lineHeight = 24.sp,
                            textAlign = TextAlign.Justify,
                            color = textColor,
                            fontSize = fontSize,
                            fontFamily = selectedFont
                        ),
                        modifier = Modifier
                            .background(backgroundColor)
                            .padding(textPadding),
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

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    var isLastChapter by remember { mutableStateOf(false) }

                    LaunchedEffect(bookId, chapter) {
                        if (bookId != null && chapter != null) {
                            try {
                                val chaptersSnapshot = userViewModel.db.collection("books").document(bookId).collection("chapters").get().await()
                                val totalChapters = chaptersSnapshot.size()
                                val currentChapter = chapter.toIntOrNull() ?: 0
                                isLastChapter = currentChapter >= totalChapters
                            } catch (e: Exception) {
                                Log.e("NextChapter", "Błąd sprawdzania rozdziału: ${e.localizedMessage}")
                            }
                        }
                    }
                    Button(
                        onClick = {
                            if (bookId != null && chapter != null) {
                                userViewModel.viewModelScope.launch {
                                    try {
                                        val nextChapter = (chapter.toIntOrNull() ?: 0) + 1

                                        if (isLastChapter) {
                                            userViewModel.addToFinished(bookId)
                                            userViewModel.removeBookFromCurrentRead(bookId)
                                            navController.navigate("bookDetails/$bookId")
                                        } else {
                                            userViewModel.saveChapterAsRead(userId, bookId, chapter.toInt())
                                            userViewModel.updateLastRead(userId, bookId, nextChapter)
                                            navController.navigate("screen_read?bookId=$bookId&chapter=$nextChapter")
                                        }
                                    } catch (e: Exception) {
                                        Log.e("NextChapter", "Błąd obsługi przycisku: ${e.localizedMessage}")
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = FluentBackgroundDark,
                            contentColor = FluentSecondaryDark
                        ),
                        shape = RectangleShape
                    ) {
                        Text(
                            text = if (isLastChapter) "Koniec. Powrót do spisu treści" else "Następny rozdział",
                            style = FluentTypography.titleMedium
                        )
                    }

                }
            }
        }
    }

    //TODO Może namieszać
    BackHandler {
        bookId?.let {
            navController.navigate("bookDetails/$it") {
                popUpTo("screen_read?bookId=$it&chapter=$chapter") { inclusive = true }
            }
        }
    }

    if (userViewModel.showTextSettingsDialog) {
        AlertDialog(
            onDismissRequest = { userViewModel.toggleTextSettingsDialog() },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Ustawienia wyglądu tekstu",
                        color = FluentSecondaryDark,
                        style = FluentTypography.titleMedium,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { userViewModel.toggleTextSettingsDialog() }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Zamknij",
                            tint = FluentSecondaryDark
                        )
                    }
                }
            }
            ,
            text = {
                Column {
                    var tempFontSize by remember { mutableStateOf(fontSize) }
                    var tempTextPadding by remember { mutableStateOf(textPadding) }
                    var tempSelectedTextColor by remember { mutableStateOf(selectedTextColor) }
                    var tempSelectedBackgroundColor by remember { mutableStateOf(selectedBackgroundColor) }
                    var tempSelectedFont by remember { mutableStateOf(selectedFont) }

                    Text("Czcionka")
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(vertical = 8.dp)
                    ) {
                        itemsIndexed(availableFonts) { index, font ->
                            Button(
                                onClick = { tempSelectedFont = font },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (font == tempSelectedFont) FluentSecondaryDark else FluentSurfaceDark
                                )
                            ) {
                                Text(
                                    text = fontNames[index],
                                    fontFamily = font,
                                    color = Color.White
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Rozmiar czcionki: ${tempFontSize.value.toInt()}")
                    Slider(
                        value = tempFontSize.value,
                        onValueChange = { tempFontSize = it.sp },
                        valueRange = 12f..30f
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Margines tekstu: ${tempTextPadding.value.toInt()}")
                    Slider(
                        value = tempTextPadding.value,
                        onValueChange = { tempTextPadding = it.dp },
                        valueRange = 0f..32f
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Kolor tekstu")
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(vertical = 8.dp)
                    ) {
                        items(availableTextColors) { color ->
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(color, shape = MaterialTheme.shapes.small)
                                    .border(
                                        width = if (color == tempSelectedTextColor) 3.dp else 1.dp,
                                        color = if (color == tempSelectedTextColor) Color.White else Color.Gray,
                                        shape = MaterialTheme.shapes.small
                                    )
                                    .clickable {
                                        if (colorsTooSimilar(color, tempSelectedBackgroundColor)) {
                                            Toast.makeText(context, "Kolor tekstu jest za podobny do tła!", Toast.LENGTH_SHORT).show()
                                        } else {
                                            tempSelectedTextColor = color
                                        }
                                    }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Kolor tła")
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(vertical = 8.dp)
                    ) {
                        items(availableBackgroundColors) { color ->
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(color, shape = MaterialTheme.shapes.small)
                                    .border(
                                        width = if (color == tempSelectedBackgroundColor) 3.dp else 1.dp,
                                        color = if (color == tempSelectedBackgroundColor) Color.White else Color.Gray,
                                        shape = MaterialTheme.shapes.small
                                    )
                                    .clickable {
                                        if (colorsTooSimilar(tempSelectedTextColor, color)) {
                                            Toast.makeText(context, "Kolor tła jest za podobny do tekstu!", Toast.LENGTH_SHORT).show()
                                        } else {
                                            tempSelectedBackgroundColor = color
                                        }
                                    }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            fontSize = 16.sp
                            textPadding = 8.dp
                            textColor = Color.White
                            backgroundColor = FluentSurfaceDark
                            selectedTextColor = Color.White
                            selectedBackgroundColor = FluentSurfaceDark
                            selectedFont = FontFamily.Default
                            userViewModel.toggleTextSettingsDialog()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = FluentBackgroundDark,
                            contentColor = FluentSecondaryDark
                        )
                    ) {
                        Text("Zresetuj ustawienia", style = FluentTypography.titleMedium)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = {
                            fontSize = tempFontSize
                            textPadding = tempTextPadding
                            textColor = tempSelectedTextColor
                            backgroundColor = tempSelectedBackgroundColor
                            selectedTextColor = tempSelectedTextColor
                            selectedBackgroundColor = tempSelectedBackgroundColor
                            selectedFont = tempSelectedFont
                            userViewModel.toggleTextSettingsDialog()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        border = BorderStroke(2.dp, FluentSecondaryDark),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = FluentSecondaryDark
                        )
                    ) {
                        Text("Zastosuj", style = FluentTypography.titleMedium)
                    }
                }
            },
            confirmButton = {}
        )
    }

    if (selectedWord != null) {
        val cleanedWord = selectedWord!!.replace(Regex("""^[^\p{L}\p{N}]+|[^\p{L}\p{N}]+$"""), "")

        LaunchedEffect(cleanedWord, translationRequested) {
            if (!translationRequested) {
                translationRequested = true
                detectedSentence = extractSentence(content, cleanedWord)
                Log.d("ExtractedSentence", "Word: $cleanedWord → Sentence: $detectedSentence")
                userViewModel.translateWord(cleanedWord, detectedSentence) { result ->
                translation = cleanTranslation(result.toLowerCase())
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
                            text = if (translation.isNotBlank()) cleanTranslation(translation) else "Ładowanie...",
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
                            contextSentence = detectedSentence,
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
fun cleanTranslation(text: String): String {
    return text.trim().removeSurrounding("\"")
}
fun colorsTooSimilar(color1: Color, color2: Color): Boolean {
    val threshold = 0.3f  // tolerancja
    val diff = listOf(
        kotlin.math.abs(color1.red - color2.red),
        kotlin.math.abs(color1.green - color2.green),
        kotlin.math.abs(color1.blue - color2.blue)
    ).average()

    return diff < threshold
}
