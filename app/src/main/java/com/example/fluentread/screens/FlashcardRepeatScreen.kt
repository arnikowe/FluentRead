package com.example.fluentread.screens

import android.annotation.SuppressLint
import android.util.Log
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.example.fluentread.ui.theme.*
import com.example.fluentread.viewmodel.UserViewModel
import com.google.firebase.firestore.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.navigation.NavHostController
import com.example.fluentread.R
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlin.math.roundToInt


@SuppressLint("UnrememberedMutableState")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlashcardRepeatScreen(
    flashcards: List<DocumentSnapshot>,
    userViewModel: UserViewModel,
    navController: NavHostController
) {
    val userId = userViewModel.userId ?: return
    LaunchedEffect(Unit) {
        userViewModel.resetFlashcards(flashcards)
        val initialFlashcards = if (userViewModel.shuffleFlashcards) {
            flashcards.shuffled()
        } else {
            userViewModel.currentFlashcards
        }
        val firstFlashcard = initialFlashcards.firstOrNull()
        val bookId = firstFlashcard?.getString("bookId")
        val chapter = firstFlashcard?.get("chapter")?.toString()

        if (bookId != null) {
            userViewModel.loadBookDetails(bookId)
        }
        userViewModel.currentChapter = chapter
    }


    val showTranslation = userViewModel.showTranslation
    val languageMode = userViewModel.flashcardLanguageMode
    var showDialog = userViewModel.showFlashcardSettings
    val currentCard = userViewModel.currentFlashcards.getOrNull(userViewModel.flashcardIndex)
    val totalCards = userViewModel.currentFlashcards.size
    val currentIndex = userViewModel.flashcardIndex.coerceAtMost(totalCards)



    val note = remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(currentCard?.id) {
        val id = currentCard?.id ?: return@LaunchedEffect
        val ref = FirebaseFirestore.getInstance()
            .collection("users").document(userId)
            .collection("flashcards").document(id)
        try {
            val updatedSnap = ref.get().await()
            note.value = updatedSnap.getString("note") ?: ""
        } catch (e: Exception) {
            Log.e("Note", "Błąd pobierania notatki: ${e.localizedMessage}")
        }
    }


    LaunchedEffect(Unit) {
        val ref = FirebaseFirestore.getInstance()
            .collection("users").document(userId)
            .collection("flashcards").document("favorite")

        try {
            val snapshot = ref.get().await()
            val ids = snapshot.get("ids") as? List<String> ?: emptyList()
            userViewModel.favoriteIds.value = ids.toSet()
            Log.d("Favorites", "Załadowane ${ids.size} ulubionych")
        } catch (e: Exception) {
            Log.e("Favorites", "Błąd ładowania ulubionych: ${e.localizedMessage}")
        }
    }


    val offsetX = remember { mutableFloatStateOf(0f) }
    val coroutineScope = rememberCoroutineScope()

    if (userViewModel.sessionFinished) {
        LaunchedEffect(Unit) {
            navController.navigate(
                "summary_screen?bookTitle=${userViewModel.bookTitle}&chapter=${userViewModel.currentChapter}&correct=${userViewModel.knowCount}&wrong=${userViewModel.dontKnowCount}"
            )
        }
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) {
                focusManager.clearFocus()
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF6E4A36))
                .padding(top = 65.dp, start = 16.dp, end = 16.dp, bottom = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(10.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            ) {
                LinearProgressIndicator(
                    progress = currentIndex / totalCards.toFloat(),
                    color = FluentBackgroundDark,
                    trackColor =FluentOnPrimaryDark,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(4.dp))
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "$currentIndex / $totalCards",
                    color = FluentOnPrimaryDark,
                    fontSize = 14.sp,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(2.dp, Color(0xFFD56767)),
                    color = Color.Transparent,
                    modifier = Modifier.padding(4.dp)
                ) {
                    Text(
                        text = " ${userViewModel.dontKnowCount}",
                        color = Color(0xFFD56767),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        fontWeight = FontWeight.Bold
                    )
                }
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(2.dp, Color(0xFF85CE7F)),
                    color = Color.Transparent,
                    modifier = Modifier.padding(4.dp)
                ) {
                    Text(
                        text = " ${userViewModel.knowCount}",
                        color = Color(0xFF85CE7F),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        fontWeight = FontWeight.Bold
                    )
                }
            }


            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .offset { IntOffset(offsetX.floatValue.roundToInt(), 0) }
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures(
                            onDragEnd = {
                                val lastIndex = flashcards.lastIndex
                                Log.d(
                                    "FlashcardSwipe",
                                    "Flashcard count=${flashcards.size}, lastIndex=$lastIndex, currentIndex=${userViewModel.flashcardIndex}"
                                )

                                when {
                                    offsetX.floatValue > 200 -> {
                                        Log.d("FlashcardSwipe", "Swipe RIGHT (know)")
                                        userViewModel.incrementKnow()
                                        userViewModel.goToNextFlashcard(lastIndex)
                                    }

                                    offsetX.floatValue < -200 -> {
                                        Log.d("FlashcardSwipe", "Swipe LEFT (don't know)")
                                        userViewModel.incrementDontKnow()
                                        userViewModel.goToNextFlashcard(lastIndex)
                                    }

                                    else -> {
                                        Log.d(
                                            "FlashcardSwipe",
                                            "No significant swipe: offset=${offsetX.floatValue}"
                                        )
                                    }
                                }

                                coroutineScope.launch {
                                    kotlinx.coroutines.delay(150)
                                    offsetX.floatValue = 0f
                                }
                            },
                            onHorizontalDrag = { change, dragAmount ->
                                offsetX.value += dragAmount
                                change.consume()
                            }
                        )
                    }
                    .clickable { userViewModel.toggleTranslation() },
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(6.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Text(
                        text = when {
                            currentCard == null -> ""
                            showTranslation xor (languageMode == "PL") -> currentCard.getString("translation")
                                ?: ""

                            else -> currentCard.getString("word") ?: ""
                        },
                        modifier = Modifier.align(Alignment.Center),
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4B2E2E)
                    )
                    IconButton(
                        onClick = {
                            val id = currentCard?.id ?: return@IconButton
                            val ref = FirebaseFirestore.getInstance().collection("users")
                                .document(userId).collection("flashcards").document("favorite")

                            val isFavorite = userViewModel.favoriteIds.value.contains(id)
                            val update = if (isFavorite) {
                                Log.d("Favorites", "Usuwam $id z ulubionych")
                                FieldValue.arrayRemove(id)
                            } else {
                                Log.d("Favorites", "Dodaję $id do ulubionych")
                                FieldValue.arrayUnion(id)
                            }

                            ref.update(mapOf("ids" to update))
                                .addOnSuccessListener {
                                    val updated = userViewModel.favoriteIds.value.toMutableSet()
                                    if (isFavorite) updated.remove(id) else updated.add(id)
                                    userViewModel.favoriteIds.value = updated
                                }
                                .addOnFailureListener {
                                    Log.e("Favorites", "Błąd aktualizacji: ${it.localizedMessage}")
                                }

                        },
                        modifier = Modifier.align(Alignment.TopEnd)
                    ) {
                        Icon(
                            imageVector = if (userViewModel.favoriteIds.value.contains(currentCard?.id))
                                Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = "Ulubione",
                            tint = FluentSecondaryDark,
                            modifier = Modifier.size(80.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = FluentSecondaryDark),
                elevation = CardDefaults.cardElevation()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(FluentSecondaryDark)
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Notatka",
                            fontWeight = FontWeight.Bold,
                            color = FluentBackgroundDark,
                            fontSize = 18.sp
                        )
                        Icon(
                            painter = painterResource(id = R.drawable.ic_note),
                            contentDescription = "Notatka",
                            tint = FluentBackgroundDark,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    TextField(
                        modifier = Modifier.focusRequester(focusRequester),
                        value = note.value,
                        onValueChange = {
                            note.value = it
                            currentCard?.id?.let { id ->
                                FirebaseFirestore.getInstance()
                                    .collection("users").document(userId)
                                    .collection("flashcards").document(id)
                                    .update("note", it)
                            }
                        },
                        colors = TextFieldDefaults.textFieldColors(
                            containerColor = Color.Transparent,
                            cursorColor = FluentBackgroundDark,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        textStyle = LocalTextStyle.current.copy(color = FluentBackgroundDark)
                    )
                }

            }


            Spacer(modifier = Modifier.height(24.dp))

        }

        if (showDialog) {
            AlertDialog(
                onDismissRequest = { showDialog = false },
                confirmButton = {},
                title = {
                    Column {
                        Text("Konfiguracja fiszki", fontWeight = FontWeight.Bold, color = FluentSecondaryDark)
                        HorizontalDivider(thickness = 1.dp, color = FluentSecondaryDark)
                    }
                },
                text = {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            val selectedColor = Color(0xFFF6EFC6)
                            val borderColor = Color(0xFFD4BC95)
                            val selectedTextColor = Color(0xFF4B2E2E)

                            Button(
                                onClick = { userViewModel.setFlashcardLanguage("EN") },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (languageMode == "EN") selectedColor else Color.Transparent
                                ),
                                border = BorderStroke(1.dp, borderColor),
                                shape = RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    "angielski",
                                    color = if (languageMode == "EN") selectedTextColor else borderColor
                                )
                            }

                            Button(
                                onClick = { userViewModel.setFlashcardLanguage("PL") },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (languageMode == "PL") selectedColor else Color.Transparent
                                ),
                                border = BorderStroke(1.dp, borderColor),
                                shape = RoundedCornerShape(topEnd = 12.dp, bottomEnd = 12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    "polski",
                                    color = if (languageMode == "PL") selectedTextColor else borderColor
                                )
                            }
                        }


                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = { userViewModel.resetFlashcards(userViewModel.currentFlashcards) },
                            colors = ButtonDefaults.buttonColors(containerColor =  FluentBackgroundDark),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Zresetuj fiszki", color = FluentSecondaryDark)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = { userViewModel.toggleFlashcardSettingsDialog() },
                            colors = ButtonDefaults.buttonColors(containerColor =  FluentBackgroundDark),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Zamknij", color = FluentSecondaryDark)
                        }
                    }
                }
            )
        }
    }
}

