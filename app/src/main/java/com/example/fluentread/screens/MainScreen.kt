package com.example.fluentread.screens

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.rememberNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import com.example.fluentread.R
import com.example.fluentread.ui.theme.*



@Composable
fun MainScreen(navController: NavController) {
    Column(modifier = Modifier
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

        // Napis
        Text(
            text = "Obecnie czytana książka",
            style = FluentTypography.titleMedium,
            fontWeight = FontWeight.Medium,
            color = FluentSecondaryDark
        )

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

        // Sekcja książki
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .background(FluentBackgroundDark, shape = RoundedCornerShape(8.dp))
                .padding(12.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.bookbutton),
                contentDescription = "Okładka książki",
                modifier = Modifier
                    .size(120.dp)
                    .clickable { /* np. przejście do szczegółów */ }
            )

            Spacer(modifier = Modifier.width(16.dp))

            // Tytuł i postęp
            Column(
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Tytuł książki",
                    style = FluentTypography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = FluentSecondaryDark
                )

                Spacer(modifier = Modifier.height(8.dp))

                LinearProgressIndicator(
                    progress = 0.4f, // 40% przeczytane
                    modifier = Modifier.fillMaxWidth(),
                    color = FluentSurfaceDark
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Moja biblioteka",
            style = FluentTypography.titleMedium,
            fontWeight = FontWeight.Medium,
            color = FluentSecondaryDark
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Pasek separatora
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(FluentSecondaryDark)
                .padding(16.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(5) { index -> // <- później zamienisz na dane z bazy
                BookItem(
                    imageRes = R.drawable.book_cover, // Tymczasowa grafika
                    onClick = {
                        // TODO: Przejście do szczegółów książki
                        navController.navigate("bookDetails/$index")
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

// Pasek separatora
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(FluentSecondaryDark)
        )

    }
}
@Composable
fun BookItem(imageRes: Int, onClick: () -> Unit) {
    Image(
        painter = painterResource(id = imageRes),
        contentDescription = "Okładka książki",
        modifier = Modifier
            .size(120.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
    )
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    val navController = rememberNavController()
    MainScreen(navController = navController)
}

