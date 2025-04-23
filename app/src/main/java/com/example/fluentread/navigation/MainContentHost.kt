package com.example.fluentread.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.fluentread.menu.MainScaffold
import com.example.fluentread.screens.*

@Composable
fun MainContentHost(navController: NavHostController) {
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route ?: ""

    val excludedRoutes = listOf("login", "register", "forgot_password")

    if (excludedRoutes.any { currentRoute.startsWith(it) }) {
        NavGraph(navController = navController)
    } else {
        MainScaffold(navController = navController)
    }
}
