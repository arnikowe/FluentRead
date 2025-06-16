package com.example.fluentread.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.example.fluentread.dateClass.Book
import com.example.fluentread.ui.theme.*
import androidx.compose.material.icons.*
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.ui.unit.sp
import com.example.fluentread.R
import com.example.fluentread.viewmodel.UserViewModel
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

@Composable
fun FlashcardsScreen(
    userViewModel: UserViewModel,
    navController: NavHostController
) {
    val db = FirebaseFirestore.getInstance()
    val userId = userViewModel.userId ?: return

    var searchQuery by remember { mutableStateOf("") }
    var bookStats by remember { mutableStateOf<List<Triple<Book, Int, String>>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    userViewModel.sessionSource = "flashcards"

    LaunchedEffect(Unit) {
        isLoading = true

        val flashcardDocs = db.collection("users").document(userId)
            .collection("flashcards")
            .get().await().documents

        val favoriteSnap = db.collection("users").document(userId)
            .collection("flashcards").document("favorite").get().await()

        val favoriteIds = (favoriteSnap.get("ids") as? List<String>) ?: emptyList()

        val bookIdToCount = flashcardDocs.groupingBy { it.getString("bookId") ?: "" }
            .eachCount()

        val bookList = mutableListOf<Triple<Book, Int, String>>()

        if (favoriteIds.isNotEmpty()) {
            val favBook = Book(
                id = "favorites",
                title = "Ulubione",
                author = "",
                cover = "",
                genre = arrayOf(),
                level = "",
                wordCount = favoriteIds.size.toDouble()
            )
            bookList.add(Triple(favBook, favoriteIds.size, "favorites"))
        }
        val allFlashcardsCount = flashcardDocs.count { it.id != "favorite" }
        if (allFlashcardsCount > 0) {
            val allBook = Book(
                id = "all_flashcards",
                title = "Wszystkie fiszki",
                author = "",
                cover = "",
                genre = arrayOf(),
                level = "",
                wordCount = allFlashcardsCount.toDouble()
            )
            bookList.add(Triple(allBook, allFlashcardsCount, "all_flashcards"))
        }

        for ((bookIdKey, count) in bookIdToCount) {
            try {
                val doc = db.collection("books").document(bookIdKey).get().await()
                val book = Book(
                    id = bookIdKey,
                    title = doc.getString("title") ?: "Nieznany tytuł",
                    author = doc.getString("author") ?: "Nieznany autor",
                    cover = doc.getString("cover") ?: "",
                    genre = arrayOf(),
                    level = doc.getString("level") ?: "",
                    wordCount = doc.getDouble("wordCount") ?: 0.0
                )
                bookList.add(Triple(book, count, bookIdKey))
            } catch (_: Exception) {}
        }

        bookStats = bookList
        isLoading = false
    }


    Column(modifier = Modifier.fillMaxSize().background(Background) .padding(15.dp)) {
        Spacer(modifier = Modifier.height(65.dp))
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = {
                Text("Szukaj po tytule lub autorze", color = Color.White)
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Ikona wyszukiwania",
                    tint = Color.White
                )
            },
            modifier = Modifier.fillMaxWidth()
        )


        Spacer(modifier = Modifier.height(12.dp))

            LazyColumn {
                val filtered = bookStats.filter {
                    it.first.title.contains(searchQuery, ignoreCase = true) ||
                            it.first.author.contains(searchQuery, ignoreCase = true)
                }

                items(filtered) { item ->
                    val (book, count, id) = item
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(FluentBackgroundDark)
                            .padding(vertical = 8.dp)
                            .clickable {
                                userViewModel.sessionSource = when (id) {
                                    "favorites" -> "favorites"
                                    "all_flashcards" -> "all_flashcards"
                                    else -> "flashcards"
                                }
                                navController.navigate("flashcard_set/${id}")
                            }
                        ,
                        elevation = CardDefaults.cardElevation(),
                        colors = CardDefaults.cardColors(containerColor = FluentBackgroundDark)
                    ) {
                        Row(modifier = Modifier.padding(4.dp)) {
                            AsyncImage(
                                model = if (book.id == "favorites" || book.id == "all_flashcards") R.drawable.book_cover else book.cover,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(100.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Fit
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.align(Alignment.CenterVertically)) {
                                Text(book.title, fontWeight = FontWeight.Bold, color = FluentSecondaryDark, fontSize = 20.sp)
                                Text("Autor: ${book.author}", color = FluentSecondaryDark)
                                Text("Liczba fiszek: $count", color = FluentSecondaryDark)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

    }
}
