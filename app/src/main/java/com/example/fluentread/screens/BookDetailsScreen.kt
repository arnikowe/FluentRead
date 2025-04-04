package com.example.fluentread.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.example.fluentread.R
import com.example.fluentread.ui.theme.FluentBackgroundDark
import com.example.fluentread.ui.theme.FluentSecondaryDark
import com.example.fluentread.ui.theme.FluentSurfaceDark
import com.example.fluentread.ui.theme.FluentTypography

@Composable
fun BookDetailsScreen(navController: NavHostController, index: Int) { Column(modifier = Modifier
    .fillMaxSize()
    .background(FluentSurfaceDark)
) {

    // Górny pasek z przyciskiem menu
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
            .height(60.dp)
            .background(FluentBackgroundDark)
    ) {
        IconButton(onClick = { /* TODO: obsługa kliknięcia menu */ }) {
            Icon(
                painter = painterResource(id = R.drawable.ic_menu),
                contentDescription = "Menu"
            )
        }
        Spacer(modifier = Modifier.width(8.dp))

    }

    Spacer(modifier = Modifier.height(24.dp))

    Box(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        Text(
            text = "Obecnie czytana książka",
            modifier = Modifier.align(Alignment.Center),
            textAlign = TextAlign.Center,
            style = FluentTypography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = FluentSecondaryDark
        )
    }

    Spacer(modifier = Modifier.height(8.dp))

    // Pasek separatora
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(2.dp)
            .background(FluentSecondaryDark)
            .padding(16.dp)
    )

    Spacer(modifier = Modifier.height(24.dp))
}
}
@Preview(showBackground = true)
@Composable
fun BookDetailsScreenPreview() {
    val navController = rememberNavController()
    BookDetailsScreen(navController = navController,1)
}