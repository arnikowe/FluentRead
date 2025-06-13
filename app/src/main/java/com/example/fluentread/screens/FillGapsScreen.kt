package com.example.fluentread.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.fluentread.R
import com.example.fluentread.ui.theme.FluentBackgroundDark
import com.example.fluentread.ui.theme.FluentSecondaryDark
import com.example.fluentread.ui.theme.FluentSurfaceDark
import com.example.fluentread.ui.theme.FluentTypography
import com.example.fluentread.viewmodel.UserViewModel
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FillGapsScreen(
    navController: NavHostController,
    bookId: String,
    chapter: String? = null,
    userViewModel: UserViewModel
) {
    val db = FirebaseFirestore.getInstance()
    val uid = userViewModel.userId ?: return
    var flashcards by remember { mutableStateOf<List<DocumentSnapshot>>(emptyList()) }
    var currentIndex by remember { mutableStateOf(0) }
    var userAnswer by remember { mutableStateOf("") }
    var showFeedback by remember { mutableStateOf(false) }
    var isCorrect by remember { mutableStateOf(false) }
    var correctCount by remember { mutableStateOf(0) }
    var wrongCount by remember { mutableStateOf(0) }
    var pendingAutoAdvance by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current


    LaunchedEffect(bookId, chapter) {
        val query = db.collection("users").document(uid)
            .collection("flashcards")
            .whereEqualTo("bookId", bookId)

        val filteredQuery = chapter?.let {
            query.whereEqualTo("chapter", it)
        } ?: query

        flashcards = filteredQuery.get().await().documents.shuffled()
    }

    if (flashcards.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Brak fiszek do ćwiczenia.")
        }
        return
    }

    val currentFlashcard = flashcards.getOrNull(currentIndex)
    val word = currentFlashcard?.getString("word") ?: ""
    val translation = currentFlashcard?.get("translation").toString()
    val rawContext = currentFlashcard?.getString("context") ?: ""
    val contextSentence = if (word.isNotBlank() && rawContext.contains(word, ignoreCase = true)) {
        rawContext.replace(Regex("\\b${Regex.escape(word)}\\b", RegexOption.IGNORE_CASE), "____")
    } else rawContext

    LaunchedEffect(pendingAutoAdvance) {
        if (pendingAutoAdvance) {
            kotlinx.coroutines.delay(1000)
            if (currentIndex == flashcards.lastIndex) {
                navController.navigate(
                    "summary_screen_fill?bookTitle=$bookId" +
                            "&chapter=${chapter ?: ""}" +
                            "&correct=$correctCount&wrong=$wrongCount"
                )
            } else {
                showFeedback = false
                isCorrect = false
                userAnswer = ""
                currentIndex++
            }
            pendingAutoAdvance = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(FluentSurfaceDark)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) {
                focusManager.clearFocus()
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Spacer(modifier = Modifier.height(35.dp))

            Text(
                text = "Uzupełnij brakujące słowo po angielsku:",
                style = FluentTypography.titleMedium,
                color = Color.White
            )

            LinearProgressIndicator(
                progress = (currentIndex + 1).toFloat() / flashcards.size,
                color = FluentSecondaryDark,
                trackColor = Color.Gray,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
            )

            Text(
                text = "${currentIndex + 1} / ${flashcards.size}",
                color = Color.White,
                style = FluentTypography.bodyMedium,
                modifier = Modifier.align(Alignment.End)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = translation,
                    style = FluentTypography.titleLarge,
                    color = FluentSecondaryDark,
                    fontSize = 28.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Clip,
                    softWrap = true,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 36.dp, max = 72.dp)
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.icon_goodanswer),
                        contentDescription = "Dobra odpowiedź",
                        tint = if (showFeedback && isCorrect) Color(0xFF85CE7F) else FluentBackgroundDark,
                        modifier = Modifier.size(32.dp)
                    )
                    Icon(
                        painter = painterResource(id = R.drawable.icon_badanswer),
                        contentDescription = "Zła odpowiedź",
                        tint = if (showFeedback && !isCorrect) Color(0xFFD56767) else FluentBackgroundDark,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
            DividerLine()
            Text(
                text = if (showFeedback && !isCorrect) "Poprawna odpowiedź: $word" else "Poprawna odpowiedź:",
                color = if (showFeedback && !isCorrect) Color.LightGray else Color.Transparent,
                style = FluentTypography.bodySmall
            )
            if (contextSentence.isNotBlank()) {
                Text(
                    text = "Kontekst: $contextSentence",
                    style = FluentTypography.bodySmall,
                    color = Color.LightGray
                )
            }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {

                Spacer(modifier = Modifier.weight(1f))
                OutlinedTextField(
                    value = userAnswer,
                    onValueChange = { userAnswer = it },
                    label = { Text("Wpisz brakujące słowo", color = Color.White) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = FluentSecondaryDark,
                        unfocusedBorderColor = FluentSecondaryDark,
                        focusedLabelColor = Color.White,
                        unfocusedLabelColor = Color.LightGray,
                        cursorColor = FluentSecondaryDark,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = {
                            wrongCount++
                            showFeedback = true
                            isCorrect = false
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
                            val correct = userAnswer.trim().equals(word, ignoreCase = true)
                            isCorrect = correct
                            showFeedback = true
                            if (correct) correctCount++ else wrongCount++
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
}
