package com.example.fluentread.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.navigation.NavHostController
import com.example.fluentread.R
import com.example.fluentread.ui.theme.*
import com.example.fluentread.viewmodel.UserViewModel
import com.google.firebase.firestore.*
import kotlinx.coroutines.tasks.await


@Composable
fun FlashcardSetScreen(
    bookId: String,
    userViewModel: UserViewModel,
    navController: NavHostController,
    chapter: String? = null
) {
    val db = FirebaseFirestore.getInstance()
    val userId = userViewModel.userId ?: return
    var flashcards by remember { mutableStateOf<List<DocumentSnapshot>>(emptyList()) }
    var favorites by remember { mutableStateOf<Set<String>>(emptySet()) }
    var sortDialogVisible by remember { mutableStateOf(false) }
    var sortType by remember { mutableStateOf("original") }
    var selectedSortType by remember { mutableStateOf(sortType) }

    userViewModel.sessionSource = when {
        bookId == "favorites" -> "favorites"
        bookId == "all_flashcards" -> "all_flashcards"
        userViewModel.sessionSource == "flashcards" -> "flashcards"
        else -> "book"
    }


    LaunchedEffect(bookId, chapter) {
        when (bookId) {
            "favorites" -> {
                val favoriteSnap = db.collection("users").document(userId)
                    .collection("flashcards").document("favorite").get().await()
                val favoriteIds = favoriteSnap.get("ids") as? List<String> ?: emptyList()

                val favoriteDocs = favoriteIds.mapNotNull { id ->
                    val doc = db.collection("users").document(userId)
                        .collection("flashcards").document(id).get().await()
                    if (doc.exists()) doc else null
                }

                flashcards = favoriteDocs
            }

            "all_flashcards" -> {
                val allDocs = db.collection("users").document(userId)
                    .collection("flashcards")
                    .get().await().documents
                    .filter { it.id != "favorite" }
                flashcards = allDocs
            }

            else -> {
                val query = db.collection("users").document(userId)
                    .collection("flashcards")
                    .whereEqualTo("bookId", bookId)

                val finalQuery = if (chapter != null) {
                    query.whereEqualTo("chapter", chapter)
                } else {
                    query
                }

                val flashcardSnap = finalQuery.get().await()
                flashcards = flashcardSnap.documents
            }
        }

        val favoriteSnap = db.collection("users").document(userId)
            .collection("flashcards").document("favorite").get().await()
        favorites = (favoriteSnap.get("ids") as? List<String> ?: emptyList()).toSet()
    }



    val sortedFlashcards = when (sortType) {
        "alphabetical" -> flashcards.sortedBy { it.getString("word")?.lowercase() }
        else -> flashcards.sortedBy { it.getLong("timestamp") ?: 0L }
    }


    Column(modifier = Modifier
        .fillMaxSize()
        .background(Background)
        .padding(16.dp)) {
        Spacer(modifier = Modifier.height(55.dp))
        Text(
                text = "Powtórz",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFEDE6B1)
        )

        HorizontalDivider(thickness = 1.dp, color =Color(0xFFEDE6B1))

        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = FluentBackgroundDark.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(12.dp)
                )

        ) {
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .clickable {
                        val baseRoute = "repeat_mode/$bookId"
                        val route = if (chapter != null) {
                            "$baseRoute?chapter=$chapter"
                        } else {
                            baseRoute
                        }
                        userViewModel.resetFlashcardSession()
                        navController.navigate(route)
                    }
                    .align(Alignment.Center)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_flashcards_button),
                    contentDescription = "Start repetition",
                    modifier = Modifier.fillMaxSize(),
                    tint = Color.Unspecified
                )

            }

        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Fiszki",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFEDE6B1),
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { sortDialogVisible = true }) {
                Icon(Icons.Default.FilterList, contentDescription = "Sort", tint = Color(0xFFEDE6B1))
            }
        }

        HorizontalDivider(thickness = 1.dp, color = Color(0xFFEDE6B1))

        Spacer(modifier = Modifier.height(8.dp))

        var showDeleteDialog by remember { mutableStateOf(false) }
        var cardToDelete by remember { mutableStateOf<DocumentSnapshot?>(null) }

        LazyColumn {
            items(sortedFlashcards) { doc ->
                val word = doc.getString("word") ?: ""
                val translation = doc.getString("translation") ?: ""
                val id = doc.id
                val isFavorite = favorites.contains(id)

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onLongPress = {
                                    cardToDelete = doc
                                    showDeleteDialog = true
                                }
                            )
                        },
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Row(
                        modifier = Modifier
                            .padding(12.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = word,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF4B2E2E)
                            )
                            Text(text = translation, color = Color(0xFF4B2E2E))
                        }
                        IconButton(onClick = {
                            val newFavorites = favorites.toMutableSet().apply {
                                if (contains(id)) remove(id) else add(id)
                            }
                            favorites = newFavorites
                            db.collection("users").document(userId)
                                .collection("flashcards")
                                .document("favorite")
                                .set(mapOf("ids" to newFavorites.toList()))
                        }) {
                            Icon(
                                imageVector = if (isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                                contentDescription = "Favorite",
                                tint = FluentSecondaryDark
                            )
                        }
                    }
                }
            }
        }

// Dialog poza LazyColumn
        if (showDeleteDialog && cardToDelete != null) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("Chcesz usunąć słowo?") },
                confirmButton = {
                    TextButton(onClick = {
                        val docId = cardToDelete!!.id
                        db.collection("users").document(userId)
                            .collection("flashcards").document(docId)
                            .delete()
                            .addOnSuccessListener {
                                flashcards = flashcards.filter { it.id != docId }
                            }

                        showDeleteDialog = false
                    }) {
                        Text("Usuń")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) {
                        Text("Anuluj")
                    }
                }
            )
        }

    }




    if (sortDialogVisible) {
        AlertDialog(
            onDismissRequest = { sortDialogVisible = false },
            confirmButton = {
                Row {
                    TextButton(onClick = {
                        sortDialogVisible = false
                        selectedSortType = sortType
                    }) {
                        Text("Zamknij")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(onClick = {
                        sortType = selectedSortType
                        sortDialogVisible = false
                    }) {
                        Text("Zastosuj")
                    }
                }
            },
            title = { Text("Sortuj fiszki") },
            text = {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = selectedSortType == "original",
                            onClick = { selectedSortType = "original" }
                        )
                        Text("Oryginalna kolejność")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = selectedSortType == "alphabetical",
                            onClick = { selectedSortType = "alphabetical" }
                        )
                        Text("Alfabetyczna kolejność")
                    }
                }
            }
        )
    }

}
