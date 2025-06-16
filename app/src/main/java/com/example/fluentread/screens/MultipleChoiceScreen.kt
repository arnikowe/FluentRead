package com.example.fluentread.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.fluentread.ui.theme.Background
import com.example.fluentread.ui.theme.FluentBackgroundDark
import com.example.fluentread.ui.theme.FluentSecondaryDark
import com.example.fluentread.ui.theme.FluentSurfaceDark
import com.example.fluentread.ui.theme.FluentTypography
import com.example.fluentread.viewmodel.UserViewModel
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.DocumentSnapshot
import kotlinx.coroutines.tasks.await

@Composable
fun MultipleChoiceScreen(
    navController: NavHostController,
    bookId: String,
    chapter: String? = null,
    userViewModel: UserViewModel
) {
    val uid = userViewModel.userId ?: return
    val db = FirebaseFirestore.getInstance()

    var flashcards by remember { mutableStateOf<List<DocumentSnapshot>>(emptyList()) }
    var currentIndex by remember { mutableStateOf(0) }
    var selectedOption by remember { mutableStateOf<String?>(null) }
    var isAnswered by remember { mutableStateOf(false) }
    var isCorrect by remember { mutableStateOf(false) }
    var correctCount by remember { mutableStateOf(0) }
    var wrongCount by remember { mutableStateOf(0) }

    LaunchedEffect(bookId, chapter) {
        val query = db.collection("users").document(uid)
            .collection("flashcards")
            .whereEqualTo("bookId", bookId)

        val filteredQuery = chapter?.let {
            query.whereEqualTo("chapter", it)
        } ?: query

        flashcards = filteredQuery.get().await().shuffled()
    }

    if (flashcards.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Brak fiszek do ćwiczenia.")
        }
        return
    }

    if (currentIndex >= flashcards.size) {
        navController.navigate(
            "summary_screen_quiz?bookTitle=$bookId" +
                    "&chapter=${chapter ?: ""}" +
                    "&correct=$correctCount&wrong=$wrongCount"
        )
        return
    }

    val currentFlashcard = flashcards[currentIndex]
    val correctWord = currentFlashcard.getString("word") ?: ""
    val translation = currentFlashcard.get("translation")?.toString() ?: ""

    val options = remember(currentIndex) {
        val otherWords = flashcards.mapNotNull { it.getString("word") }
            .filter { it != correctWord }
            .shuffled()
            .take(3)
            .toMutableList()
        otherWords.add((0..otherWords.size).random(), correctWord)
        otherWords.shuffled()
    }

    LaunchedEffect(isAnswered) {
        if (isAnswered) {
            kotlinx.coroutines.delay(1000)
            if (selectedOption.equals(correctWord, ignoreCase = true)) {
                correctCount++
            } else {
                wrongCount++
            }
            selectedOption = null
            isAnswered = false
            currentIndex++
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Spacer(modifier = Modifier.height(35.dp))

        Text(
            text = "Wybierz poprawne angielskie słowo dla:",
            style = FluentTypography.titleMedium,
            color = Color.White
        )

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

        Text(
            text = translation,
            style = FluentTypography.titleLarge,
            color = FluentSecondaryDark
        )

        options.forEach { option ->
            val backgroundColor = when {
                !isAnswered -> FluentSecondaryDark
                option == correctWord -> Color(0xFF85CE7F)
                option == selectedOption -> Color(0xFFD56767)
                else -> FluentSecondaryDark
            }

            Button(
                onClick = {
                    if (!isAnswered) {
                        selectedOption = option
                        isCorrect = option.equals(correctWord, ignoreCase = true)
                        isAnswered = true
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = backgroundColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(option, color = FluentBackgroundDark)
            }
        }
    }
}
