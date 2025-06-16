package com.example.fluentread.screens
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import androidx.navigation.NavHostController
import com.example.fluentread.ui.theme.*
import com.example.fluentread.viewmodel.UserViewModel
import com.google.ai.client.generativeai.Chat
import com.google.firebase.firestore.DocumentReference

@Composable
fun ChatLibraryScreen(
    userViewModel: UserViewModel,
    navController: NavHostController
) {
    val uid = userViewModel.userId ?: return
    val db = FirebaseFirestore.getInstance()

    var searchQuery by remember { mutableStateOf("") }
    var finishedBooks by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    var currentBooks by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    var chaptersMap by remember { mutableStateOf<Map<String, List<String>>>(emptyMap()) }
    var chaptersReadMap by remember { mutableStateOf<Map<String, List<String>>>(emptyMap()) }

    val expandedBookId = remember { mutableStateOf<String?>(null) }

    // Load books
    LaunchedEffect(uid) {
        val userDoc = db.collection("users").document(uid).get().await()
        val finishedRefs = userDoc.get("finishedBooks") as? List<DocumentReference> ?: emptyList()

        val finished = finishedRefs.mapNotNull { ref ->
            val title = ref.get().await().getString("title")
            if (title != null) Pair(ref.id, title) else null
        }
        finishedBooks = finished

        val finishedIds = finished.map { it.first }

        val currentSnap = db.collection("users").document(uid).collection("currentRead").get().await()
        val current = currentSnap.documents.mapNotNull { doc ->
            val bookId = doc.id
            if (bookId !in finishedIds) {
                val title = db.collection("books").document(bookId).get().await().getString("title")
                if (title != null) Pair(bookId, title) else null
            } else null
        }
        currentBooks = current

        // Load chapters for finished books
        val chapterMap = mutableMapOf<String, List<String>>()
        finished.forEach { (bookId, _) ->
            val snap = db.collection("books").document(bookId).collection("chapters").get().await()
            chapterMap[bookId] = snap.documents.map { it.id }.sorted()
        }

        // Load chapters read for current books
        val readMap = mutableMapOf<String, List<String>>()
        current.forEach { (bookId, _) ->
            val currentReadDoc = db.collection("users").document(uid)
                .collection("readProgress").document(bookId).get().await()
            val chaptersMap = currentReadDoc.get("chaptersRead") as? Map<*, *>
            val chapterList = chaptersMap?.keys?.mapNotNull { it?.toString() }
            if (!chapterList.isNullOrEmpty()) {
                readMap[bookId] = chapterList.sorted()
            }
        }

        chaptersMap = chapterMap
        chaptersReadMap = readMap
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        Spacer(modifier = Modifier.height(60.dp))
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Szukaj po tytule...", color = Color.White) },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = "Szukaj", tint = Color.White)
            },
            modifier = Modifier.fillMaxWidth().padding(start = 12.dp,end=12.dp,top=12.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        val allBooks = (finishedBooks.map { Triple(it.first, it.second, true) } + currentBooks.map { Triple(it.first, it.second, false) })
            .filter { searchQuery.isBlank() || it.second.contains(searchQuery, ignoreCase = true) }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(allBooks) { (bookId, title, isFinished) ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(color = FluentBackgroundDark)
                        .clickable {
                            expandedBookId.value = if (expandedBookId.value == bookId) null else bookId
                        }
                        .padding(top = 16.dp,bottom=16.dp, start = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = title,
                            color = Color.White,
                            style = FluentTypography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 12.dp)
                        )
                    }

                    AnimatedVisibility(
                        visible = expandedBookId.value == bookId,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        val chapters = if (isFinished) {
                            chaptersMap[bookId] ?: emptyList()
                        } else {
                            chaptersReadMap[bookId] ?: emptyList()
                        }

                        Column(
                            modifier = Modifier
                                .padding(top = 8.dp)
                                .fillMaxWidth()
                                .background(color = FluentBackgroundDark)
                                .padding(12.dp)
                        ) {
                            chapters.forEach { chapter ->
                                val chapterInt = chapter.toIntOrNull() ?: return@forEach
                                val route = "screen_chat?bookId=$bookId&chapter=$chapterInt"

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp)
                                        .background(
                                            color = FluentSurfaceDark,
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .clickable {
                                            navController.navigate(route)
                                        }
                                        .padding(horizontal = 12.dp, vertical = 10.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "Rozdział $chapter",
                                            color = Color.White,
                                            style = FluentTypography.bodyMedium,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
