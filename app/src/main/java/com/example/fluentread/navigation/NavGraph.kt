package com.example.fluentread.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.fluentread.screens.*
import com.example.fluentread.viewmodel.UserViewModel
import androidx.navigation.compose.composable
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await


@Composable
fun NavGraph(navController: NavHostController, modifier: Modifier = Modifier) {
    val userViewModel: UserViewModel = viewModel()
    val startDestination = if (userViewModel.userId != null) "main" else "register"

    NavHost(navController = navController, startDestination = startDestination) {
        composable("login") { LoginScreen(navController) }
        composable("register") { RegisterScreen(navController) }
        composable("forgot_password") { ForgotPasswordScreen(navController) }
        composable("main") { MainScreen(navController, userViewModel) }
        composable(
            route = "bookDetails/{bookId}",
            arguments = listOf(navArgument("bookId") { type = NavType.StringType })
        ) { backStackEntry ->
            val bookId = backStackEntry.arguments?.getString("bookId") ?: ""
            BookDetailsScreen(navController, bookId, userViewModel)
        }
        composable(
            route = "screen_read?bookId={bookId}&chapter={chapter}",
            arguments = listOf(
                navArgument("bookId") { type = NavType.StringType; nullable = true },
                navArgument("chapter") { type = NavType.StringType; nullable = true }
            )
        ) { backStackEntry ->
            val bookId = backStackEntry.arguments?.getString("bookId")
            val chapter = backStackEntry.arguments?.getString("chapter")
            ReadScreen(bookId = bookId, chapter = chapter, userViewModel = userViewModel, navController = navController)
        }
        composable(
            route = "screen_chat?bookId={bookId}&chapter={chapter}",
            arguments = listOf(
                navArgument("bookId") { type = NavType.StringType; nullable = true },
                navArgument("chapter") { type = NavType.StringType; nullable = true }
            )
        ) { backStackEntry ->
            val bookId = backStackEntry.arguments?.getString("bookId")
            val chapter = backStackEntry.arguments?.getString("chapter")
            ChatScreen(bookId = bookId, chapter = chapter, userViewModel = userViewModel)
        }

        composable(
            route = "screen_flashcards",
        ) {
            FlashcardsScreen(
                userViewModel = userViewModel,
                navController = navController
            )
        }

        composable("flashcard_set/{bookId}") { backStackEntry ->
            val bookId = backStackEntry.arguments?.getString("bookId") ?: ""
            FlashcardSetScreen(bookId, userViewModel, navController)
        }

        composable(
            route = "repeat_mode/{bookId}?chapter={chapter}",
            arguments = listOf(
                navArgument("bookId") { type = NavType.StringType },
                navArgument("chapter") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val bookId = backStackEntry.arguments?.getString("bookId") ?: ""
            val chapter = backStackEntry.arguments?.getString("chapter")
            val userId = userViewModel.userId ?: return@composable
            val flashcardsState = remember { mutableStateOf<List<DocumentSnapshot>>(emptyList()) }
            val isLoading = remember { mutableStateOf(true) }

            LaunchedEffect(bookId, chapter) {
                val db = FirebaseFirestore.getInstance()
                try {
                    if (bookId == "favorites") {
                        val snap = db.collection("users").document(userId)
                            .collection("flashcards").document("favorite").get().await()
                        val ids = snap.get("ids") as? List<String> ?: emptyList()
                        flashcardsState.value = ids.mapNotNull {
                            db.collection("users").document(userId)
                                .collection("flashcards").document(it).get().await().takeIf { it.exists() }
                        }
                    } else {
                        var query = db.collection("users").document(userId)
                            .collection("flashcards")
                            .whereEqualTo("bookId", bookId)

                        if (chapter != null) {
                            query = query.whereEqualTo("chapter", chapter)
                        }

                        val snap = query.get().await()
                        flashcardsState.value = snap.documents
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    isLoading.value = false
                }
            }

            if (isLoading.value) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color.White)
                }
            } else if (flashcardsState.value.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Brak fiszek do powtórki", color = Color.White)
                }
            } else {
                FlashcardRepeatScreen(
                    flashcards = flashcardsState.value,
                    userViewModel = userViewModel,
                    navController = navController
                )
            }
        }

        composable(
            "screen_flashcard_set?bookId={bookId}&chapter={chapter}"
        ) { backStackEntry ->
            val bookId = backStackEntry.arguments?.getString("bookId") ?: ""
            val chapter = backStackEntry.arguments?.getString("chapter")
            FlashcardSetScreen(
                bookId = bookId,
                chapter = chapter,
                userViewModel = userViewModel,
                navController = navController
            )
        }

        composable(
            route = "summary_screen?bookTitle={bookTitle}&chapter={chapter}&correct={correct}&wrong={wrong}",
            arguments = listOf(
                navArgument("bookTitle") { type = NavType.StringType },
                navArgument("chapter") { type = NavType.StringType; nullable = true },
                navArgument("correct") { type = NavType.IntType },
                navArgument("wrong") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val bookTitle = backStackEntry.arguments?.getString("bookTitle") ?: ""
            val chapter = backStackEntry.arguments?.getString("chapter")
            val correct = backStackEntry.arguments?.getInt("correct") ?: 0
            val wrong = backStackEntry.arguments?.getInt("wrong") ?: 0

            SummaryScreen(
                bookTitle = bookTitle,
                chapter = chapter,
                correctAnswers = correct,
                wrongAnswers = wrong,
                onRepeat = {
                    navController.popBackStack("repeat_mode/{bookId}?chapter={chapter}", inclusive = false)
                }
            )
        }
        composable("library") {
            LibraryScreen(navController = navController, userViewModel = userViewModel)
        }


    }
}
