package com.example.fluentread.screens

import android.widget.Toast
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.fluentread.R
import com.example.fluentread.dateClass.Book
import com.example.fluentread.ui.theme.*
import com.example.fluentread.viewmodel.UserViewModel
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun LibraryScreen(navController: NavController, userViewModel: UserViewModel) {
    val db = userViewModel.db
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    var searchQuery by remember { mutableStateOf("") }
    var showFilterDialog by remember { mutableStateOf(false) }
    var selectedGenres by remember { mutableStateOf<Set<String>>(emptySet()) }
    var selectedLevels by remember { mutableStateOf<Set<String>>(emptySet()) }

    var allBooks by remember { mutableStateOf<List<Book>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var showDialog by remember { mutableStateOf(false) }
    var selectedBookId by remember { mutableStateOf<String?>(null) }

    val genreTranslations = mapOf(
        "adventure" to "Przygoda",
        "biography" to "Biografia",
        "children" to "Dla dzieci",
        "classic" to "Klasyka",
        "drama" to "Dramat",
        "education" to "Edukacja",
        "fantasy" to "Fantastyka",
        "history" to "Historia",
        "horror" to "Horror",
        "mystery" to "Tajemnica",
        "poetry" to "Poezja",
        "psychological" to "Psychologiczne",
        "romance" to "Romans",
        "science fiction" to "Science fiction",
        "thriller" to "Thriller"
    )

    val levelTranslations = mapOf(
        "easy" to "Początkujący",
        "medium" to "Średniozaawansowany",
        "hard" to "Zaawansowany"
    )

    LaunchedEffect(Unit) {
        userViewModel.loadCurrentBooks()
    }

    LaunchedEffect(Unit) {
        try {
            val booksSnapshot = db.collection("books").get().await()
            allBooks = booksSnapshot.documents.map { doc ->
                Book(
                    id = doc.id,
                    title = doc.getString("title") ?: "",
                    cover = doc.getString("cover") ?: "",
                    author = doc.getString("author") ?: "",
                    genre = (doc.get("genre") as? List<*>)?.filterIsInstance<String>()?.toTypedArray() ?: emptyArray(),
                    level = doc.getString("level") ?: "",
                    wordCount = doc.getDouble("wordCount") ?: 0.0
                )
            }
        } catch (e: Exception) {
            errorMessage = "Błąd ładowania książek: ${e.localizedMessage}"
            Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
        } finally {
            isLoading = false
        }
    }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FluentSurfaceDark)
            .padding(12.dp)
            .pointerInput(Unit) {
                detectTapGestures(onTap = { focusManager.clearFocus() })
            }
    ) {
        Spacer(modifier = Modifier.height(55.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Szukaj po tytule lub autorze", color = Color.White) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Szukaj",
                        tint = Color.White
                    )
                },
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = { showFilterDialog = true },
                modifier = Modifier.size(30.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_filter),
                    contentDescription = "Filtr",
                    tint = FluentBackgroundDark
                )
            }
        }

        val allTags = selectedGenres.mapNotNull { genreTranslations[it] } +
                selectedLevels.mapNotNull { levelTranslations[it] }

        if (allTags.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                allTags.forEach { tag ->
                    Box(
                        modifier = Modifier
                            .background(FluentSecondaryDark, shape = MaterialTheme.shapes.medium)
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = tag,
                                color = FluentBackgroundDark
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Usuń filtr",
                                tint = FluentBackgroundDark,
                                modifier = Modifier
                                    .size(16.dp)
                                    .clickable {
                                        selectedGenres = selectedGenres - genreTranslations.entries.find { it.value == tag }?.key.orEmpty()
                                        selectedLevels = selectedLevels - levelTranslations.entries.find { it.value == tag }?.key.orEmpty()
                                    }
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        val isFiltering = searchQuery.isNotBlank() || selectedGenres.isNotEmpty() || selectedLevels.isNotEmpty()

        val filteredBooks = allBooks.filter { book ->
            val matchesSearch = searchQuery.isBlank() ||
                    book.title.contains(searchQuery, ignoreCase = true) ||
                    book.author.contains(searchQuery, ignoreCase = true)
            val matchesGenre = selectedGenres.isEmpty() ||
                    book.genre.any { it in selectedGenres }
            val matchesLevel = selectedLevels.isEmpty() ||
                    selectedLevels.contains(book.level)
            matchesSearch && matchesGenre && matchesLevel
        }

        if (isFiltering) {
            if (filteredBooks.isNotEmpty()) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    contentPadding = PaddingValues(4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredBooks) { book ->
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
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Brak książek spełniających kryteria",
                        color = Color.White
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item { CurrentReadingBooks(navController, userViewModel,"library") }
                item { BooksByLevel(level = "easy", navController, allBooks) }
                item { BooksByLevel(level = "medium", navController, allBooks) }
                item { BooksByLevel(level = "hard", navController, allBooks) }
            }
        }
    }

    if (showFilterDialog) {
        FilterDialog(
            selectedGenres = selectedGenres,
            selectedLevels = selectedLevels,
            genreTranslations = genreTranslations,
            levelTranslations = levelTranslations,
            onDismiss = { showFilterDialog = false },
            onApply = { genres, levels ->
                selectedGenres = genres
                selectedLevels = levels
                showFilterDialog = false
            }
        )
    }
}
@Composable
fun BooksByLevel(level: String, navController: NavController, books: List<Book>) {
    val filtered = books.filter { it.level.equals(level, ignoreCase = true) }
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    var showDialog by remember { mutableStateOf(false) }
    var selectedBookId by remember { mutableStateOf<String?>(null) }

    if (filtered.isNotEmpty()) {
        Column {
            Text(
                text = when (level) {
                    "easy" -> "Początkujące"
                    "medium" -> "Średniozaawansowane"
                    "hard" -> "Zaawansowane"
                    else -> "Inne"
                },
                color = FluentSecondaryDark,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            Spacer(modifier = Modifier.height(4.dp))
            DividerLine()
            Spacer(modifier = Modifier.height(4.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(FluentBackgroundDark)
                    .padding(vertical = 8.dp)
            ) {
                val itemWidth = (screenWidth - 40.dp) / 3

                LazyRow(
                    contentPadding = PaddingValues(horizontal = 5.dp),
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    items(filtered) { book ->
                        Box(
                            modifier = Modifier.width(itemWidth)
                        ) {
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

            Spacer(modifier = Modifier.height(4.dp))
            DividerLine()
        }
    }
}
@Composable
fun DividerLine() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(2.dp)
            .background(FluentSecondaryDark)
    )
}
@Composable
fun FilterDialog(
    selectedGenres: Set<String>,
    selectedLevels: Set<String>,
    genreTranslations: Map<String, String>,
    levelTranslations: Map<String, String>,
    onDismiss: () -> Unit,
    onApply: (Set<String>, Set<String>) -> Unit
) {
    var tempSelectedGenres by remember { mutableStateOf(selectedGenres) }
    var tempSelectedLevels by remember { mutableStateOf(selectedLevels) }
    var expandGenres by remember { mutableStateOf(false) }
    var expandLevels by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onApply(tempSelectedGenres, tempSelectedLevels) }) {
                Text("Zastosuj", color = FluentSecondaryDark)
            }
        },
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Filtry", color = FluentSecondaryDark)
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Zamknij", tint = FluentSecondaryDark)
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { expandLevels = !expandLevels },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Filtruj po poziomie", fontWeight = FontWeight.Bold, color = FluentSecondaryDark)
                    Spacer(modifier = Modifier.weight(1f))
                    Icon(
                        imageVector = if (expandLevels) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = "Rozwiń/Zwiń",
                        tint = FluentSecondaryDark
                    )
                }

                if (expandLevels) {
                    levelTranslations.forEach { (key, label) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    tempSelectedLevels = tempSelectedLevels.toMutableSet().apply {
                                        if (contains(key)) remove(key) else add(key)
                                    }
                                }
                                .padding(start = 8.dp, top = 2.dp, bottom = 2.dp)
                        ) {
                            Checkbox(
                                checked = tempSelectedLevels.contains(key),
                                onCheckedChange = null
                            )
                            Text(label)
                        }
                    }
                }

                Divider(modifier = Modifier.padding(vertical = 4.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { expandGenres = !expandGenres },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Filtruj po gatunku", fontWeight = FontWeight.Bold, color = FluentSecondaryDark)
                    Spacer(modifier = Modifier.weight(1f))
                    Icon(
                        imageVector = if (expandGenres) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = "Rozwiń/Zwiń",
                        tint = FluentSecondaryDark
                    )
                }

                if (expandGenres) {
                    Box(modifier = Modifier.heightIn(max = 200.dp)) {
                        LazyColumn {
                            items(genreTranslations.toList().sortedBy { it.second }) { (key, label) ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            tempSelectedGenres = tempSelectedGenres.toMutableSet().apply {
                                                if (contains(key)) remove(key) else add(key)
                                            }
                                        }
                                        .padding(start = 8.dp, top = 2.dp, bottom = 2.dp)
                                ) {
                                    Checkbox(
                                        checked = tempSelectedGenres.contains(key),
                                        onCheckedChange = null
                                    )
                                    Text(label)
                                }
                            }
                        }
                    }
                }

            }
        }
    )
}

