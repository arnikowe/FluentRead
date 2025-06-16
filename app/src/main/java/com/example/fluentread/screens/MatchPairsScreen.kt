package com.example.fluentread.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
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
import com.example.fluentread.ui.theme.Background
import com.example.fluentread.ui.theme.FluentBackgroundDark
import com.example.fluentread.ui.theme.FluentSecondaryDark
import com.example.fluentread.ui.theme.FluentSurfaceDark
import com.example.fluentread.ui.theme.FluentTypography
import com.example.fluentread.viewmodel.UserViewModel
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay
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
    var selectedItem by remember { mutableStateOf<Pair<String, Boolean>?>(null) }
    var matchedPairs by remember { mutableStateOf<Set<Pair<String, String>>>(emptySet()) }
    var incorrectMatchItem by remember { mutableStateOf<String?>(null) }
    var incorrectWordMatchItem by remember { mutableStateOf<String?>(null) }

    val shakeTransition = rememberInfiniteTransition(label = "shakeTransition")
    val shakeOffset by shakeTransition.animateFloat(
        initialValue = -6f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(50, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shake"
    )

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
        selectedItem = null
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
            .background(Background)
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
                    val isSelected = selectedItem?.first == word && selectedItem?.second == true
                    val isShaking = incorrectWordMatchItem == word

                    val backgroundColor by animateColorAsState(
                        targetValue = when {
                            isMatched -> Color.Gray
                            isSelected -> FluentSecondaryDark
                            isShaking -> Color(0xFFD56767)
                            else -> FluentBackgroundDark
                        },
                        animationSpec = tween(durationMillis = 300),
                        label = "colorAnimWord"
                    )

                    LaunchedEffect(isShaking) {
                        if (isShaking) {
                            delay(300)
                            incorrectWordMatchItem = null
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(boxHeight)
                            .offset(x = if (isShaking) shakeOffset.dp else 0.dp)
                            .background(backgroundColor, shape = RoundedCornerShape(4.dp))
                            .clickable(enabled = !isMatched) {
                                val selected = selectedItem
                                if (selected == null) {
                                    selectedItem = word to true
                                } else {
                                    val (text, isWord) = selected
                                    val pair = if (isWord) Pair(text, word) else Pair(word, text)
                                    if (pairs.contains(pair)) {
                                        matchedPairs = matchedPairs + pair
                                    } else {
                                        incorrectMatchItem = if (isWord) word else text
                                        incorrectWordMatchItem = if (isWord) text else word
                                    }
                                    selectedItem = null
                                }
                            }
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = word,
                            color = when {
                                isMatched -> Color.LightGray
                                isSelected -> FluentBackgroundDark
                                else -> Color.White
                            }
                        )
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
                    val isSelected = selectedItem?.first == translation && selectedItem?.second == false
                    val isShaking = incorrectMatchItem == translation

                    val backgroundColor by animateColorAsState(
                        targetValue = when {
                            isMatched -> Color.Gray
                            isShaking -> Color(0xFFD56767)
                            isSelected -> FluentSecondaryDark
                            else -> FluentBackgroundDark
                        },
                        animationSpec = tween(durationMillis = 300),
                        label = "colorAnimTrans"
                    )

                    LaunchedEffect(isShaking) {
                        if (isShaking) {
                            delay(300)
                            incorrectMatchItem = null
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(boxHeight)
                            .offset(x = if (isShaking) shakeOffset.dp else 0.dp)
                            .background(backgroundColor, shape = RoundedCornerShape(4.dp))
                            .clickable(enabled = !isMatched) {
                                val selected = selectedItem
                                if (selected == null) {
                                    selectedItem = translation to false
                                } else {
                                    val (text, isWord) = selected
                                    val pair = if (isWord) Pair(text, translation) else Pair(translation, text)
                                    if (pairs.contains(pair)) {
                                        matchedPairs = matchedPairs + pair
                                    } else {
                                        incorrectMatchItem = if (isWord) translation else text
                                        incorrectWordMatchItem = if (isWord) text else translation
                                    }
                                    selectedItem = null
                                }
                            }

                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = translation,
                            color = if (isMatched) Color.LightGray else Color.White
                        )
                    }
                }
            }
        }
    }
}
