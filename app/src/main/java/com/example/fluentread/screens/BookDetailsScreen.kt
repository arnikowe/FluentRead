package com.example.fluentread.screens

import android.util.Log
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.example.fluentread.R
import com.example.fluentread.ui.theme.*
import com.example.fluentread.viewmodel.UserViewModel
import kotlinx.coroutines.tasks.await

@Composable
fun BookDetailsScreen(navController: NavHostController, bookId: String, userViewModel: UserViewModel) {
    var expandedChapter by remember { mutableStateOf<String?>(null) }
    var availableFlashcards by remember { mutableStateOf<Map<Int, Boolean>>(emptyMap()) }

    LaunchedEffect(bookId) {
        userViewModel.loadBookDetails(bookId)
    }

    LaunchedEffect(userViewModel.chapters) {
        val chapters = userViewModel.chapters
        if (chapters.isEmpty()) return@LaunchedEffect

        val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
        val userId = userViewModel.userId ?: return@LaunchedEffect

        val newAvailableFlashcards = mutableMapOf<Int, Boolean>()

        chapters.forEach { chapterNumber ->
            val chapterInt = chapterNumber.toInt()
            val snapshot = db.collection("users").document(userId)
                .collection("flashcards")
                .whereEqualTo("bookId", bookId)
                .whereEqualTo("chapter", chapterInt.toString())
                .limit(1)
                .get()
                .await()

            val hasFlashcards = snapshot.documents.isNotEmpty()
            newAvailableFlashcards[chapterInt] = hasFlashcards
        }

        availableFlashcards = newAvailableFlashcards.toMap()
    }



    val currentTitle = userViewModel.bookTitle
    val currentAuthor = userViewModel.bookAuthor
    val chapters = userViewModel.chapters

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FluentSurfaceDark)
    ) {
        Spacer(modifier = Modifier.height(70.dp))

        Text(
            text = currentTitle,
            modifier = Modifier.align(Alignment.CenterHorizontally),
            textAlign = TextAlign.Center,
            style = FluentTypography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = currentAuthor,
            modifier = Modifier.align(Alignment.CenterHorizontally),
            textAlign = TextAlign.Center,
            style = FluentTypography.titleSmall,
            fontWeight = FontWeight.Normal,
            color = FluentSecondaryDark
        )

        Spacer(modifier = Modifier.height(8.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(top = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            chapters.forEach { chapterNumber ->
                val chapterName = "Chapter ${chapterNumber.toInt()}"
                val chapterInt = chapterNumber.toInt()

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(color = FluentBackgroundDark)
                            .clickable {
                                expandedChapter = if (expandedChapter == chapterName) null else chapterName
                            }
                            .padding(horizontal = 20.dp, vertical = 12.dp)
                    ) {
                        Text(
                            text = chapterName,
                            color = Color.White,
                            style = FluentTypography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    AnimatedVisibility(
                        visible = expandedChapter == chapterName,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(top = 8.dp)
                                .fillMaxWidth()
                                .background(color = FluentBackgroundDark)
                                .padding(12.dp)
                        ) {
                            listOf("Read", "Chat", "Flashcards").forEach { action ->
                                val iconRes = when (action) {
                                    "Read" -> R.drawable.book_text_icon
                                    "Chat" -> R.drawable.chat_text_icon
                                    "Flashcards" -> R.drawable.flashcard_text_icon
                                    else -> R.drawable.ic_launcher_foreground
                                }

                                val route = when (action) {
                                    "Read" -> "screen_read?bookId=$bookId&chapter=$chapterInt"
                                    "Chat" -> "screen_chat?bookId=$bookId&chapter=$chapterInt"
                                    "Flashcards" -> "screen_flashcard_set?bookId=$bookId&chapter=$chapterInt"
                                    else -> ""
                                }

                                val isFlashcardAvailable = availableFlashcards[chapterInt] == true

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp)
                                        .background(
                                            color = if (action == "Flashcards" && !isFlashcardAvailable) FluentSurfaceDark.copy(alpha = 0.4f) else FluentSurfaceDark,
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .then(
                                            if (action == "Flashcards" && !isFlashcardAvailable) Modifier
                                            else Modifier.clickable {
                                                if (action == "Read") {
                                                    navController.navigate(route) {
                                                        popUpTo("screen_book_details") {
                                                            inclusive = false
                                                        }
                                                        launchSingleTop = true
                                                    }
                                                } else {
                                                    navController.navigate(route)
                                                }
                                            }
                                        )
                                        .padding(horizontal = 12.dp, vertical = 10.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Image(
                                            painter = painterResource(id = iconRes),
                                            contentDescription = "$action Icon",
                                            modifier = Modifier
                                                .size(40.dp)
                                                .padding(end = 12.dp)
                                        )
                                        Text(
                                            text = action,
                                            color = Color.White,
                                            style = FluentTypography.bodyMedium,
                                            modifier = Modifier.weight(1f)
                                        )
                                        if (action == "Flashcards" && !isFlashcardAvailable) {
                                            Icon(
                                                painter = painterResource(id = R.drawable.ic_lock),
                                                contentDescription = "Locked",
                                                tint = Color.White.copy(alpha = 0.7f),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
