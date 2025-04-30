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

    val currentTitle = userViewModel.bookTitle
    val currentAuthor = userViewModel.bookAuthor
    val chapters = userViewModel.chapters
    var lastReadChapter by remember { mutableStateOf<Int?>(null) }
    var chaptersRead by remember { mutableStateOf<List<Int>>(emptyList()) }
    val showDialog = remember { mutableStateOf(false) }
    val pendingChatChapter = remember { mutableStateOf<Int?>(null) }



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
        var isFinished by remember { mutableStateOf<Boolean?>(null) }
        var isCurrent by remember { mutableStateOf<Boolean?>(null) }
        val userId = userViewModel.userId
        LaunchedEffect(bookId) {
            if (userId != null) {
                userViewModel.checkIfBookIsFinishedOrCurrent(userId, bookId) { finished, current ->
                    isFinished = finished
                    isCurrent = current
                }
            }
        }

        LaunchedEffect(bookId, isCurrent) {
            if (userId != null && isCurrent == true) {
                userViewModel.getChaptersRead(userId, bookId) { readChapters ->
                    chaptersRead = readChapters
                }
            }
        }

        val progress = when {
            isFinished == true && isCurrent != true -> 1f
            isCurrent == true -> if (chapters.isNotEmpty()) {
                chaptersRead.size.toFloat() / chapters.size.toFloat()
            } else 0f
            else -> 0f
        }
        LinearProgressIndicator(
            progress = progress,
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .height(6.dp)
                .align(Alignment.CenterHorizontally),
            color = FluentSecondaryDark
        )
        Spacer(modifier = Modifier.height(8.dp))

        LaunchedEffect(userViewModel.userId) {
            userViewModel.userId?.let { userId ->
                userViewModel.loadLastReadProgress(userId, bookId) { returnedBookId, chapter ->
                    if (returnedBookId != null && chapter != null) {
                        lastReadChapter = chapter
                    }
                }
            }
        }

        if (lastReadChapter != null) {
            Button(
                onClick = {
                    navController.navigate("screen_read?bookId=$bookId&chapter=$lastReadChapter")
                },
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .align(Alignment.CenterHorizontally),
                colors = ButtonDefaults.buttonColors(
                    containerColor = FluentSecondaryDark
                )
            ) {
                Text("Wznów rozdział ${lastReadChapter}", color = FluentBackgroundDark)
            }
        }
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = chapterName,
                                color = Color.White,
                                style = FluentTypography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )

                            if (lastReadChapter != null && chapterInt < lastReadChapter!!) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_check),
                                    contentDescription = "Przeczytane",
                                    tint =  FluentSurfaceDark,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

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
                            listOf("Czytanie", "Konwersacje", "Fiszki", "Ćwiczenia").forEach { action ->

                                val iconRes = when (action) {
                                    "Czytanie" -> R.drawable.book_text_icon
                                    "Konwersacje" -> R.drawable.chat_text_icon
                                    "Fiszki" -> R.drawable.flashcard_text_icon
                                    "Ćwiczenia" -> R.drawable.exercise_icon
                                    else -> R.drawable.ic_launcher_foreground
                                }

                                val route = when (action) {
                                    "Czytanie" -> "screen_read?bookId=$bookId&chapter=$chapterInt"
                                    "Konwersacje" -> "screen_chat?bookId=$bookId&chapter=$chapterInt"
                                    "Fiszki" -> "screen_flashcard_set?bookId=$bookId&chapter=$chapterInt"
                                    "Ćwiczenia" -> "exercise_selector?bookId=$bookId&chapter=$chapterInt"
                                    else -> ""
                                }


                                val isFlashcardAvailable = availableFlashcards[chapterInt] == true

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp)
                                        .background(
                                            color = if (action == "Fiszki" && !isFlashcardAvailable) FluentSurfaceDark.copy(alpha = 0.4f) else FluentSurfaceDark,
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .then(
                                            if (action == "Fiszki" && !isFlashcardAvailable) Modifier
                                            else Modifier.clickable {
                                                when (action) {
                                                    "Czytanie" -> {
                                                        navController.navigate(route) {
                                                            popUpTo("screen_book_details") { inclusive = false }
                                                            launchSingleTop = true
                                                        }
                                                    }
                                                    "Konwersacje" -> {
                                                        userViewModel.shouldAllowChat(bookId, chapterInt) { isAllowed ->
                                                            if (isAllowed) {
                                                                navController.navigate(route)
                                                            } else {
                                                                pendingChatChapter.value = chapterInt
                                                                showDialog.value = true
                                                            }
                                                        }
                                                    }
                                                    "Fiszki" -> {
                                                        navController.navigate(route) {
                                                            popUpTo("screen_book_details") { inclusive = false }
                                                            launchSingleTop = true
                                                        }
                                                    }
                                                    "Ćwiczenia" -> {
                                                        navController.navigate(route)
                                                    }
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
    if (showDialog.value && pendingChatChapter.value != null) {
        AlertDialog(
            onDismissRequest = { showDialog.value = false },
            title = { Text("Rozdział nieprzeczytany") },
            text = { Text("Nie przeczytałeś jeszcze tego rozdziału. Czy na pewno chcesz przeprowadzić konwersację?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        val chapter = pendingChatChapter.value!!
                        showDialog.value = false
                        navController.navigate("screen_chat?bookId=$bookId&chapter=$chapter")
                    }
                ) {
                    Text("Przejdź")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDialog.value = false
                        pendingChatChapter.value = null
                    }
                ) {
                    Text("Anuluj")
                }
            },
            containerColor = FluentBackgroundDark,
            titleContentColor = Color.White,
            textContentColor = Color.White
        )
    }

}
