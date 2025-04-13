package com.example.fluentread.screens

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.example.fluentread.R
import com.example.fluentread.ui.theme.FluentBackgroundDark
import com.example.fluentread.ui.theme.FluentSecondaryDark
import com.example.fluentread.ui.theme.FluentSurfaceDark
import com.example.fluentread.ui.theme.FluentTypography
import com.example.fluentread.viewmodel.UserViewModel
import kotlinx.coroutines.tasks.await
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll


@Composable
fun BookDetailsScreen(navController: NavHostController, bookId: String, userViewModel: UserViewModel) {
    var expandedChapter by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(bookId) {
        userViewModel.loadBookDetails(bookId)
    }

    val currentTitle = userViewModel.bookTitle
    val currentAuthor = userViewModel.bookAuthor
    val chapters = userViewModel.chapters

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FluentSurfaceDark)
    ) {
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
            text = currentTitle,
            modifier = Modifier.align(Alignment.CenterHorizontally),
            textAlign = TextAlign.Center,
            style = FluentTypography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = currentAuthor,
            modifier = Modifier.align(Alignment.CenterHorizontally),
            textAlign = TextAlign.Center,
            style = FluentTypography.titleSmall,
            fontWeight = FontWeight.Normal,
            color = FluentSecondaryDark
        )

        Spacer(modifier = Modifier.height(8.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(top = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            chapters.forEach { chapterNumber ->
                val chapterName = "Chapter ${chapterNumber.toInt()}"
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(color = FluentBackgroundDark)
                            .clickable {
                                expandedChapter = if (expandedChapter == chapterName) null else chapterName
                            }
                            .padding(horizontal = 20.dp, vertical = 12.dp)
                    ) {
                        Text(
                            text = chapterName,
                            color = Color.White,
                            style = FluentTypography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    AnimatedVisibility(
                        visible = expandedChapter == chapterName,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(top = 8.dp)
                                .fillMaxWidth()
                                .background(color = FluentBackgroundDark)
                                .padding(12.dp)
                        ) {
                            listOf("Read", "Chat", "Flashcards").forEach { action ->
                                val iconRes = when (action) {
                                    "Read" -> R.drawable.book_text_icon
                                    "Chat" -> R.drawable.chat_text_icon
                                    "Flashcards" -> R.drawable.flashcard_text_icon
                                    else -> R.drawable.ic_launcher_foreground
                                }

                                val route = if (action == "Read")
                                    "screen_loading_route?bookId=${bookId}&chapter=${chapterNumber.toInt()}"
                                else
                                    "screen_${action.lowercase()}?bookId=$bookId&chapter=${chapterNumber.toInt()}"


                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp)
                                        .background(
                                            color = FluentSurfaceDark,
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .clickable {
                                            if (action == "Read") {
                                                navController.navigate(route) {
                                                    popUpTo("screen_book_details") {
                                                        inclusive = false
                                                    }
                                                    launchSingleTop = true
                                                }
                                            } else {
                                                navController.navigate(route)
                                            }
                                        }
                                        .padding(horizontal = 12.dp, vertical = 10.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Image(
                                            painter = painterResource(id = iconRes),
                                            contentDescription = "$action Icon",
                                            modifier = Modifier
                                                .size(40.dp)
                                                .padding(end = 12.dp)
                                        )
                                        Text(
                                            text = action,
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

        Spacer(modifier = Modifier.height(24.dp))
    }
}



@Preview(showBackground = true)
@Composable
fun BookDetailsScreenPreview() {
    val navController = rememberNavController()

    val mockViewModel = object : UserViewModel() {
        override val userId: String? = "mockUser123"
        override val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
    }

    BookDetailsScreen(
        navController = navController,
        bookId = "mockBookId123",
        userViewModel = mockViewModel
    )
}

