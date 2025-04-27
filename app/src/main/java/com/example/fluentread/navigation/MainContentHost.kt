package com.example.fluentread.navigation

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.fluentread.menu.MainScaffold
import com.example.fluentread.screens.*

@RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
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
