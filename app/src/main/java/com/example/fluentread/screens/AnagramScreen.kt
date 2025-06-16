package com.example.fluentread.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.fluentread.R
import com.example.fluentread.ui.theme.Background
import com.example.fluentread.ui.theme.FluentBackgroundDark
import com.example.fluentread.ui.theme.FluentSecondaryDark
import com.example.fluentread.ui.theme.FluentSurfaceDark
import com.example.fluentread.ui.theme.FluentTypography
import com.example.fluentread.viewmodel.UserViewModel
import com.google.accompanist.flowlayout.FlowRow
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.delay

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AnagramScreen(
    navController: NavHostController,
    bookId: String,
    chapter: String? = null,
    userViewModel: UserViewModel
) {
    val uid = userViewModel.userId ?: return
    val db = FirebaseFirestore.getInstance()

    var flashcards by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    var currentIndex by remember { mutableStateOf(0) }
    var shuffledLetters by remember { mutableStateOf<List<Char>>(emptyList()) }
    var selectedIndices by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var showResult by remember { mutableStateOf(false) }
    var isCorrect by remember { mutableStateOf(false) }
    var pendingAutoAdvance by remember { mutableStateOf(false) }

    var correctCount by remember { mutableStateOf(0) }
    var wrongCount by remember { mutableStateOf(0) }

    LaunchedEffect(bookId, chapter) {
        val query = db.collection("users").document(uid)
            .collection("flashcards")
            .whereEqualTo("bookId", bookId)
            .let { if (chapter != null) it.whereEqualTo("chapter", chapter) else it }

        val words = query.get().await().documents.mapNotNull { doc ->
            val word = doc.getString("word")
            val translations = doc.get("translation")
            val translation = when (translations) {
                is List<*> -> translations.firstOrNull()?.toString()
                is String -> translations
                else -> null
            }
            if (word != null && translation != null) Pair(word, translation) else null
        }
        flashcards = words.shuffled()
        flashcards.firstOrNull()?.first?.let {
            shuffledLetters = it.toList().shuffled()
        }
    }

    if (flashcards.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Brak fiszek do ćwiczenia.")
        }
        return
    }

    if (currentIndex >= flashcards.size) {
        navController.navigate(
            "summary_screen_anagram?bookTitle=$bookId&chapter=${chapter ?: ""}&correct=$correctCount&wrong=$wrongCount"
        )
        return
    }

    val (currentWord, currentTranslation) = flashcards[currentIndex]
    val userGuess = selectedIndices.map { shuffledLetters[it] }.joinToString("")

    LaunchedEffect(pendingAutoAdvance) {
        if (pendingAutoAdvance) {
            delay(1800)
            showResult = false
            selectedIndices = emptySet()
            val nextIndex = currentIndex + 1
            if (nextIndex < flashcards.size) {
                currentIndex = nextIndex
                shuffledLetters = flashcards[nextIndex].first.toList().shuffled()
            } else {
                currentIndex++
            }
            pendingAutoAdvance = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(35.dp))

        Text("Ułóż słowo z rozsypanki liter:", style = FluentTypography.titleMedium, color = Color.White)

        LinearProgressIndicator(
            progress = (currentIndex + 1).toFloat() / flashcards.size,
            color = FluentBackgroundDark,
            trackColor = Color(0xFFB3A3A3),
            modifier = Modifier.fillMaxWidth().height(8.dp)
        )
        Text(
            text = "${currentIndex + 1} / ${flashcards.size}",
            color = Color.White,
            style = FluentTypography.bodyMedium,
            modifier = Modifier.align(Alignment.End)
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = currentTranslation,
                style = FluentTypography.titleLarge,
                color = FluentSecondaryDark
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(
                    painter = painterResource(id = R.drawable.icon_goodanswer),
                    contentDescription = "Dobra odpowiedź",
                    tint = if (showResult && isCorrect) Color(0xFF85CE7F) else FluentBackgroundDark,
                    modifier = Modifier.size(32.dp)
                )
                Icon(
                    painter = painterResource(id = R.drawable.icon_badanswer),
                    contentDescription = "Zła odpowiedź",
                    tint = if (showResult && !isCorrect) Color(0xFFD56767) else FluentBackgroundDark,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
        DividerLine()
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = buildAnnotatedString {
                    append("Twoje słowo: ")

                    withStyle(
                        style = SpanStyle(
                            fontWeight = FontWeight.Bold,
                            fontSize = FluentTypography.bodyLarge.fontSize * 1.1f
                        )
                    ) {
                        append(userGuess)
                    }
                },
                color = FluentSecondaryDark,
                style = FluentTypography.bodyLarge
            )


            IconButton(
                onClick = {
                    val lastIndex = selectedIndices.lastOrNull() ?: return@IconButton
                    selectedIndices = selectedIndices - lastIndex
                },
                modifier = Modifier.size(36.dp),
                enabled = userGuess.isNotEmpty()
            ) {
                Icon(
                    imageVector = Icons.Default.Undo,
                    contentDescription = "Cofnij",
                    tint = if (userGuess.isNotEmpty()) FluentSecondaryDark else Color.Transparent
                )
            }
        }

        Text(
            text = if (showResult && !isCorrect) "Poprawna odpowiedź: $currentWord" else "Poprawna odpowiedź:",
            color = if (showResult && !isCorrect) Color.LightGray else Color.Transparent,
            style = FluentTypography.bodySmall
        )

        Spacer(modifier = Modifier.height(8.dp))
        FlowRow(
            mainAxisSpacing = 8.dp,
            crossAxisSpacing = 8.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            shuffledLetters.forEachIndexed { index, letter ->
                val isSelected = index in selectedIndices
                Button(
                    onClick = { if (!isSelected) selectedIndices = selectedIndices + index },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSelected) Color.LightGray else FluentBackgroundDark
                    ),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(letter.toString(), fontWeight = FontWeight.Bold, color = FluentSecondaryDark)
                }
            }
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {

            Spacer(modifier = Modifier.weight(1f))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = {
                        isCorrect = false
                        wrongCount++
                        showResult = true
                        pendingAutoAdvance = true
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    shape = RoundedCornerShape(4.dp),
                    border = BorderStroke(1.dp, FluentBackgroundDark)
                ) {
                    Text("Pomiń", fontWeight = FontWeight.Bold, color = FluentBackgroundDark)
                }

                Button(
                    onClick = {
                        isCorrect = userGuess.equals(currentWord, ignoreCase = true)
                        if (isCorrect) correctCount++ else wrongCount++
                        showResult = true
                        pendingAutoAdvance = true
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = FluentSecondaryDark),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text("Sprawdź", fontWeight = FontWeight.Bold, color = FluentBackgroundDark)
                }
            }
        }

    }
}
