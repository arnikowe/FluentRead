package com.example.fluentread.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.fluentread.WelcomeScreen
import com.example.fluentread.screens.BookDetailsScreen
import com.example.fluentread.screens.ForgotPasswordScreen
import com.example.fluentread.screens.LoginScreen
import com.example.fluentread.screens.MainScreen
import com.example.fluentread.screens.RegisterScreen


@Composable
fun NavGraph(navController: NavHostController) {
    NavHost(navController = navController, startDestination = "register") {
        composable("welcome") { WelcomeScreen(navController) }
        composable("login") { LoginScreen(navController) }
        composable("register") { RegisterScreen(navController = navController) }
        composable("forgot_password") { ForgotPasswordScreen(navController) }
        composable("main") { MainScreen(navController) }
        composable(
            route = "bookDetails/{index}",
            arguments = listOf(navArgument("index") { type = NavType.IntType })
        ) { backStackEntry ->
            val index = backStackEntry.arguments?.getInt("index") ?: 0
            BookDetailsScreen(navController = navController, index = index)
        }

    }
}
