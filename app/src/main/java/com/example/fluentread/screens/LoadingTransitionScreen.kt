package com.example.fluentread.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.fluentread.R
import com.example.fluentread.ui.theme.FluentSecondaryDark
import com.example.fluentread.ui.theme.FluentSurfaceDark
import com.example.fluentread.ui.theme.FluentTypography
import kotlinx.coroutines.delay

import com.airbnb.lottie.compose.*

@Composable
fun LoadingTransitionScreen(
    navController: NavHostController,
    bookId: String,
    chapter: Int
) {
    LaunchedEffect(Unit) {
        delay(2000)
        navController.navigate("screen_read?bookId=$bookId&chapter=$chapter") {
            popUpTo("screen_loading_route") {
                inclusive = true
            }

            launchSingleTop = true
        }

    }

    val composition by rememberLottieComposition(LottieCompositionSpec.Asset("book_animation.json"))
    val progress by animateLottieCompositionAsState(composition)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(FluentSurfaceDark),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            LottieAnimation(
                composition = composition,
                progress = { progress },
                modifier = Modifier.size(200.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            CircularProgressIndicator(color = FluentSecondaryDark)
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Rozpoczynamy czytanie...",
                color = Color.White,
                style = FluentTypography.bodyMedium
            )
        }
    }
}

