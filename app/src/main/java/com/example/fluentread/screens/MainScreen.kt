package com.example.fluentread.screens

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import coil.compose.rememberAsyncImagePainter
import com.example.fluentread.R
import com.example.fluentread.ui.theme.*
import com.example.fluentread.dateClass.Book
import com.example.fluentread.viewmodel.UserViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

@Composable
fun MainScreen(navController: NavController, userViewModel: UserViewModel)
 {
     val userId = userViewModel.userId
     val db = userViewModel.db

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FluentSurfaceDark)
    ) {
        // Pasek górny
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .background(FluentBackgroundDark)
        ) {
            IconButton(onClick = { /* TODO: menu */ }) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_menu),
                    contentDescription = "Menu"
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Obecnie czytana książka",
            style = FluentTypography.titleMedium,
            fontWeight = FontWeight.Medium,
            color = FluentSecondaryDark
        )

        Spacer(modifier = Modifier.height(8.dp))

        DividerLine()

        Spacer(modifier = Modifier.height(24.dp))

        var currentTitle by remember { mutableStateOf("Tytuł książki") }
        var currentBookId by remember { mutableStateOf<String?>(null) }

        LaunchedEffect(Unit) {

            if (userId != null) {
                try {
                    val userDoc = db.collection("users").document(userId).get().await()
                    val lastReadRef = userDoc.get("lastRead") as? DocumentReference

                    if (lastReadRef != null) {
                        val bookSnapshot = lastReadRef.get().await()
                        if (bookSnapshot.exists()) {
                            currentTitle = bookSnapshot.getString("title") ?: "Tytuł nieznany"
                            currentBookId = bookSnapshot.id
                        }
                    }
                } catch (e: Exception) {
                    Log.e("MainScreen", "Błąd podczas pobierania lastRead: ${e.localizedMessage}")
                }
            }
        }

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
                    .clickable {
                        currentBookId?.let { id ->
                            navController.navigate("bookDetails/$id")
                        }
                    }
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = currentTitle,
                    style = FluentTypography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = FluentSecondaryDark
                )

                Spacer(modifier = Modifier.height(8.dp))

                LinearProgressIndicator(
                    progress = 0.4f,
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

        DividerLine()

        Spacer(modifier = Modifier.height(12.dp))

        CurrentReadingBooks(navController = navController,userViewModel)

        Spacer(modifier = Modifier.height(24.dp))

        DividerLine()
    }
}

@Composable
fun CurrentReadingBooks(navController: NavController, userViewModel: UserViewModel) {
    val userId = userViewModel.userId
    val db = userViewModel.db

    val context = LocalContext.current

    var books by remember { mutableStateOf<List<Book>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(userId) {
        if (userId != null) {
            try {
                Log.d("FirestoreDebug", "Fetching document for userId: $userId")
                val userDoc = db.collection("users").document(userId).get().await()
                val currentRead = userDoc.get("currentRead") as? List<DocumentReference> ?: emptyList()

                Log.d("FirestoreDebug", "Found ${currentRead.size} book references")

                val fetchedBooks = currentRead.mapNotNull { docRef ->
                    try {
                        val snapshot = docRef.get().await()
                        if (snapshot.exists()) {
                            Log.d("FirestoreDebug", "Fetched book: ${snapshot.id}")
                            Book(
                                id = snapshot.id,
                                title = snapshot.getString("title") ?: "Nieznany tytuł",
                                cover = snapshot.getString("cover") ?: "",
                                author = snapshot.getString("author") ?: "Nieznany autor",
                                genre = (snapshot.get("genre") as? List<*>)?.filterIsInstance<String>()?.toTypedArray() ?: emptyArray(),
                                level = snapshot.getString("level") ?: ""
                            )
                        } else {
                            Log.w("FirestoreDebug", "Book document does not exist: ${docRef.path}")
                            null
                        }
                    } catch (e: Exception) {
                        Log.e("FirestoreDebug", "Error fetching book: ${e.localizedMessage}")
                        null
                    }
                }

                books = fetchedBooks
                isLoading = false
            } catch (e: Exception) {
                errorMessage = "Błąd połączenia z Firestore: ${e.localizedMessage}"
                Log.e("FirestoreDebug", "Error fetching user document: ${e.localizedMessage}", e)
                isLoading = false
            }
        } else {
            errorMessage = "Nie zalogowano użytkownika"
            Log.e("FirestoreDebug", "UserId is null")
            isLoading = false
        }
    }

    when {
        isLoading -> {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

        errorMessage != null -> {
            Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = errorMessage ?: "Wystąpił nieznany błąd",
                    color = MaterialTheme.colorScheme.error
                )
            }
        }

        else -> {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 5.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                items(books) { book ->
                    BookItem(
                        imageUrl = book.cover,
                        onClick = {
                            navController.navigate("bookDetails/${book.id}")
                        }
                    )
                }
            }
        }
    }
}


@Composable
fun BookItem(imageUrl: String, onClick: () -> Unit) {
    val painter = rememberAsyncImagePainter(
        model = imageUrl,
        placeholder = painterResource(id = R.drawable.book_cover),
        error = painterResource(id = R.drawable.book_cover)
    )

    Image(
        painter = painter,
        contentDescription = "Okładka książki",
        modifier = Modifier
            .size(160.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
    )
}


@Composable
fun DividerLine() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(2.dp)
            .background(FluentSecondaryDark)
            .padding(16.dp)
    )
}

@Composable
fun MainScreenStatic() {
    val fakeNavController = rememberNavController()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FluentSurfaceDark)
    ) {
        Text(
            text = "Podgląd głównego ekranu (Mock)",
            style = FluentTypography.titleLarge,
            color = FluentSecondaryDark,
            modifier = Modifier.padding(16.dp)
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(3) {
                BookItem(
                    imageUrl = "https://res.cloudinary.com/demo/image/upload/sample.jpg",
                    onClick = { /* brak nawigacji w Preview */ }
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreviewStatic() {
    MainScreenStatic()
}

