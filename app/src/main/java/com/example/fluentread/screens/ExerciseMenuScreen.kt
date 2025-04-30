package com.example.fluentread.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.fluentread.ui.theme.FluentBackgroundDark
import com.example.fluentread.ui.theme.FluentSecondaryDark
import com.example.fluentread.ui.theme.FluentSurfaceDark
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.DocumentSnapshot
import kotlinx.coroutines.tasks.await
import com.example.fluentread.viewmodel.UserViewModel


@Composable
fun ExerciseMenuScreen(
    bookId: String,
    chapter: String? = null,
    navController: NavHostController,
    userViewModel: UserViewModel
) {
    val db = FirebaseFirestore.getInstance()
    val userId = userViewModel.userId ?: return
    var flashcards by remember { mutableStateOf<List<DocumentSnapshot>>(emptyList()) }

    LaunchedEffect(bookId, chapter) {
        val query = db.collection("users").document(userId)
            .collection("flashcards")
            .whereEqualTo("bookId", bookId)

        val filteredQuery = chapter?.let {
            query.whereEqualTo("chapter", it)
        } ?: query

        flashcards = filteredQuery.get().await().documents
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FluentSurfaceDark)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(35.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ExerciseButton(
                label = "Uzupełnianie luk",
                modifier = Modifier.weight(1f)
            ) {
                navController.navigate("fill_gaps_screen/$bookId?chapter=$chapter")
            }
            ExerciseButton(
                label = "Dopasuj pary",
                modifier = Modifier.weight(1f)
            ) {
                navController.navigate("match_pairs_screen/$bookId?chapter=$chapter")
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ExerciseButton(
                label = "Anagramy",
                modifier = Modifier.weight(1f)
            ) {
                navController.navigate("anagram_screen/$bookId?chapter=$chapter")
            }
            ExerciseButton(
                label = "Quiz (ABC)",
                modifier = Modifier.weight(1f)
            ) {
                navController.navigate("multiple_choice_screen/$bookId?chapter=$chapter")
            }
        }
    }
}

@Composable
fun ExerciseButton(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxHeight(),
        colors = ButtonDefaults.buttonColors(
            containerColor = FluentSecondaryDark
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(
            text = label,
            textAlign = TextAlign.Center,
            color = FluentBackgroundDark,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp
        )
    }
}

