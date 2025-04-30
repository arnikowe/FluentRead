package com.example.fluentread.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.fluentread.ui.theme.FluentBackgroundDark
import com.example.fluentread.ui.theme.FluentSecondaryDark
import com.example.fluentread.ui.theme.FluentSurfaceDark
import com.example.fluentread.ui.theme.FluentTypography
import com.example.fluentread.viewmodel.UserViewModel
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

@Composable
fun MatchPairsScreen(
    navController: NavHostController,
    bookId: String,
    chapter: String? = null,
    userViewModel: UserViewModel
) {
    val uid = userViewModel.userId ?: return
    val db = FirebaseFirestore.getInstance()

    var pairs by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    var shuffledWords by remember { mutableStateOf<List<String>>(emptyList()) }
    var shuffledTranslations by remember { mutableStateOf<List<String>>(emptyList()) }
    var selectedWord by remember { mutableStateOf<String?>(null) }
    var matchedPairs by remember { mutableStateOf<Set<Pair<String, String>>>(emptySet()) }

    LaunchedEffect(bookId, chapter) {
        val query = db.collection("users").document(uid)
            .collection("flashcards")
            .whereEqualTo("bookId", bookId)

        val filteredQuery = chapter?.let {
            query.whereEqualTo("chapter", it)
        } ?: query

        val docs = filteredQuery.get().await().documents
        val rawPairs = docs.mapNotNull { doc ->
            val word = doc.getString("word")
            val translations = doc.get("translation")
            val translation = when (translations) {
                is List<*> -> translations.firstOrNull()?.toString()
                is String -> translations
                else -> null
            }
            if (word != null && translation != null) Pair(word, translation) else null
        }.shuffled().take(12)

        pairs = rawPairs
        shuffledWords = rawPairs.map { it.first }.shuffled()
        shuffledTranslations = rawPairs.map { it.second }.shuffled()
        selectedWord = null
        matchedPairs = emptySet()
    }

    if (pairs.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Brak fiszek do dopasowania.")
        }
        return
    }

    if (matchedPairs.size == pairs.size) {
        LaunchedEffect(Unit) {
            navController.navigate(
                "summary_screen_match?bookTitle=$bookId" +
                        "&chapter=${chapter ?: ""}" +
                        "&correct=${pairs.size}&wrong=0"
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FluentSurfaceDark)
            .padding(16.dp)
    ) {
        Spacer(modifier = Modifier.height(35.dp))

        Text(
            text = "Dopasuj słowa do ich tłumaczeń:",
            style = FluentTypography.titleMedium,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            val boxHeight = 48.dp

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                shuffledWords.forEach { word ->
                    val isMatched = matchedPairs.any { it.first == word }
                    val isSelected = selectedWord == word
                    val backgroundColor = when {
                        isMatched -> Color.Gray
                        isSelected -> FluentSecondaryDark
                        else -> FluentBackgroundDark
                    }
                    val textColor = when {
                        isMatched -> Color.LightGray
                        isSelected -> FluentBackgroundDark
                        else -> Color.White
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(boxHeight)
                            .background(backgroundColor, shape = RoundedCornerShape(4.dp))
                            .clickable(enabled = !isMatched) { selectedWord = word }
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(text = word, color = textColor)
                    }
                }
            }


            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                shuffledTranslations.forEach { translation ->
                    val isMatched = matchedPairs.any { it.second == translation }
                    val backgroundColor = if (isMatched) Color.Gray else FluentBackgroundDark
                    val textColor = if (isMatched) Color.LightGray else Color.White

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(boxHeight)
                            .background(backgroundColor, shape = RoundedCornerShape(4.dp))
                            .clickable(enabled = !isMatched && selectedWord != null) {
                                val word = selectedWord
                                if (word != null && pairs.contains(Pair(word, translation))) {
                                    matchedPairs = matchedPairs + Pair(word, translation)
                                }
                                selectedWord = null
                            }
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(translation, color = textColor)
                    }
                }

            }
        }
    }
}
