package com.example.fluentread.screens


import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.fluentread.R
import com.example.fluentread.ui.theme.FluentBackgroundDark
import com.example.fluentread.ui.theme.FluentSurfaceDark
import com.example.fluentread.ui.theme.FluentTypography
import com.example.fluentread.viewmodel.UserViewModel
import kotlinx.coroutines.tasks.await

@Composable
fun ReadScreen(bookId: String?, chapter: String?,userViewModel: UserViewModel) {
    val userId = userViewModel.userId
    val db = userViewModel.db
    var currentTitle by remember { mutableStateOf("Tytuł książki") }
    LaunchedEffect(Unit) {
        if (userId != null) {
            try {
                val bookRef = bookId?.let { db.collection("books").document(it) }
                val bookSnapshot = bookRef?.get()?.await()
                if (bookSnapshot != null) {
                    currentTitle = bookSnapshot.getString("title") ?: "Tytuł nieznany"
                }

            } catch (e: Exception) {
                Log.e("ReadScreen", "Błąd podczas pobierania danych: ${e.localizedMessage}")
            }
        }
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FluentSurfaceDark)
    ) {
        // Górny pasek z przyciskiem menu
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
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
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = currentTitle,
                modifier = Modifier.align(Alignment.Center),
                textAlign = TextAlign.Center,
                style = FluentTypography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
        Box(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Chapter ${chapter ?: "?"}",
                style = FluentTypography.titleMedium,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentWidth(Alignment.CenterHorizontally)
            )
        }


    }


}