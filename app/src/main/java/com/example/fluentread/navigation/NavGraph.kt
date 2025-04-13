package com.example.fluentread.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.fluentread.screens.*
import com.example.fluentread.viewmodel.UserViewModel

@Composable
fun NavGraph(navController: NavHostController) {
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
            ReadScreen(bookId = bookId, chapter = chapter, userViewModel = userViewModel)
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
            route = "screen_flashcards?bookId={bookId}&chapter={chapter}",
            arguments = listOf(
                navArgument("bookId") { type = NavType.StringType; nullable = true },
                navArgument("chapter") { type = NavType.StringType; nullable = true }
            )
        ) { backStackEntry ->
            val bookId = backStackEntry.arguments?.getString("bookId")
            val chapter = backStackEntry.arguments?.getString("chapter")
            FlashcardsScreen(bookId = bookId, chapter = chapter, userViewModel = userViewModel)
        }
        composable(
            route = "screen_loading_route?bookId={bookId}&chapter={chapter}",
            arguments = listOf(
                navArgument("bookId") { type = NavType.StringType },
                navArgument("chapter") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val bookId = backStackEntry.arguments?.getString("bookId") ?: ""
            val chapter = backStackEntry.arguments?.getInt("chapter") ?: -1

            LoadingTransitionScreen(navController, bookId, chapter)
        }



    }
}
