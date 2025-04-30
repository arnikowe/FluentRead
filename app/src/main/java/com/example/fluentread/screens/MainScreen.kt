package com.example.fluentread.screens

import android.util.Log
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.fluentread.R
import com.example.fluentread.ui.theme.*
import com.example.fluentread.viewmodel.UserViewModel
import com.example.fluentread.dateClass.Book
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FieldValue
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun MainScreen(navController: NavController, userViewModel: UserViewModel) {
    val userId = userViewModel.userId
    val db = userViewModel.db
    val context = LocalContext.current

    var currentTitle by remember { mutableStateOf("Tytuł książki") }
    var currentBookId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        userViewModel.loadCurrentBooks()
    }

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
                    } else {
                        currentTitle = "Brak ostatnio czytanej książki"
                        currentBookId = null
                    }
                } else {
                    currentTitle = "Brak ostatnio czytanej książki"
                    currentBookId = null
                }
            } catch (e: Exception) {
                Log.e("MainScreen", "Błąd podczas pobierania lastRead: ${e.localizedMessage}")
            }
        }
    }


    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FluentSurfaceDark)
            .padding(12.dp)
    ) {
        Spacer(modifier = Modifier.height(55.dp))

        Text(
            text = "Obecnie czytana książka",
            style = FluentTypography.titleMedium,
            fontWeight = FontWeight.Medium,
            color = FluentSecondaryDark,
            modifier = Modifier.padding(horizontal = 4.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))
        DividerLine()
        Spacer(modifier = Modifier.height(16.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .background(FluentBackgroundDark, shape = RoundedCornerShape(8.dp))
                .clickable {
                    if (currentBookId != null) {
                        currentBookId?.let { id -> navController.navigate("bookDetails/$id") }
                    } else {
                        Toast.makeText(context, "Brak ostatnio czytanej książki", Toast.LENGTH_SHORT).show()
                    }
                }
                .padding(12.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.bookbutton),
                contentDescription = "Okładka książki",
                modifier = Modifier.size(120.dp)
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

                Spacer(modifier = Modifier.height(12.dp))

                if (currentBookId != null) {
                    var chaptersRead by remember { mutableStateOf<List<Int>>(emptyList()) }
                    val totalChaptersCount = userViewModel.totalChapters[currentBookId] ?: 1
                    LaunchedEffect(currentBookId) {
                        userViewModel.userId?.let { userId ->
                            userViewModel.getChaptersRead(userId, currentBookId!!) { readChapters ->
                                chaptersRead = readChapters
                            }
                        }
                    }
                    val progress = chaptersRead.size.toFloat() / totalChaptersCount.toFloat()

                    LinearProgressIndicator(
                        progress = progress.coerceIn(0f, 1f),
                        modifier = Modifier.fillMaxWidth(),
                        color = FluentSecondaryDark
                    )
                }
            }
        }

        CurrentReadingBooks(navController = navController, userViewModel = userViewModel,"main")

        Spacer(modifier = Modifier.height(24.dp))

        DividerLine()
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun CurrentReadingBooks(navController: NavController, userViewModel: UserViewModel,  sourceScreen: String) {
    val books = userViewModel.currentBooks
    val isLoading = userViewModel.isCurrentBooksLoading
    val errorMessage = userViewModel.currentBooksError
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val itemWidth = (screenWidth - 40.dp) / 3

    var showDialog by remember { mutableStateOf(false) }
    var selectedBookId by remember { mutableStateOf<String?>(null) }

    Spacer(modifier = Modifier.height(24.dp))

    Text(
        text = "Moja biblioteka",
        style = FluentTypography.titleMedium,
        fontWeight = FontWeight.Medium,
        color = FluentSecondaryDark,
        modifier = Modifier.padding(horizontal = 4.dp)
    )

    Spacer(modifier = Modifier.height(8.dp))
    DividerLine()
    Spacer(modifier = Modifier.height(12.dp))

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .background(FluentBackgroundDark, RoundedCornerShape(8.dp))
            .padding(16.dp),
        contentAlignment = Alignment.TopStart
    ) {
        when {
            isLoading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            errorMessage != null -> {
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error
                )
            }
            books.isEmpty() -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Nie masz żadnych obecnie czytanych książek.",
                        color = FluentSecondaryDark
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    if (sourceScreen == "main") {
                        Text(
                            text = "Przejdź do biblioteki",
                            color = FluentPrimaryDark,
                            modifier = Modifier.clickable {
                                navController.navigate("library")
                            }
                        )
                    } else {
                        Text(
                            text = "Weź coś do ręki z półki",
                            color = FluentPrimaryDark
                        )
                    }
                }
            }
            else -> {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 5.dp),
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    items(books) { book ->
                        Box(modifier = Modifier.width(itemWidth)) {
                            BookItem(
                                imageUrl = book.cover,
                                onClick = { navController.navigate("bookDetails/${book.id}") },
                                onLongClick = {
                                    selectedBookId = book.id
                                    showDialog = true
                                }
                            )
                        }
                    }
                }
            }
        }
    }


    if (showDialog && selectedBookId != null) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Usuń książkę") },
            text = { Text("Czy na pewno chcesz usunąć tę książkę z obecnie czytanych?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        selectedBookId?.let { userViewModel.removeBookFromCurrentRead(it) }
                        showDialog = false
                    }
                ) {
                    Text("Usuń", color = FluentBackgroundAccent)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDialog = false }
                ) {
                    Text("Anuluj", color = FluentBackgroundAccent)
                }
            }
        )
    }
}

@Composable
fun BookItem(
    imageUrl: String,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
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
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onClick() },
                    onLongPress = { onLongClick() }
                )
            }
    )
}


