package com.example.fluentread.menu

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.fluentread.R
import kotlinx.coroutines.launch
import com.example.fluentread.navigation.NavGraph
import com.example.fluentread.ui.theme.Background
import com.example.fluentread.ui.theme.FluentBackgroundDark
import com.example.fluentread.viewmodel.UserViewModel

@RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScaffold(navController: NavHostController) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route ?: ""
    val isReadScreen = currentRoute.startsWith("screen_read")
    val isFlashcardScreen = currentRoute.startsWith("repeat_mode")
    val userViewModel: UserViewModel = viewModel()


    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                DrawerContent(
                    onItemClick = { route ->
                        scope.launch {
                            drawerState.close()
                            navController.navigate(route) {
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    },
                    onLogout = {
                        scope.launch {
                            drawerState.close()
                            navController.navigate("login") {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    },
                    currentRoute = navController.currentBackStackEntry?.destination?.route ?: ""
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    modifier = Modifier.height(45.dp),
                    title = {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .padding(start = 2.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text(
                                text = when {
                                    isReadScreen -> userViewModel.currentTitle
                                    else -> ""
                                },
                                color = Color.White,
                                fontSize = 19.sp
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            scope.launch { drawerState.open() }
                        }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu", tint = Background,modifier = Modifier.size(28.dp))
                        }
                    },
                    actions = {
                        when {
                            isReadScreen -> {
                                IconButton(onClick = {
                                    val bookId = userViewModel.currentBookId
                                    val chapter = userViewModel.currentChapter
                                    val userId = userViewModel.userId
                                    val index = userViewModel.readScrollIndex
                                    val offset = userViewModel.readScrollOffset
                                    val content = userViewModel.chapterContent
                                    Log.d("Bookmark", "Klik: $bookId, $chapter, $userId, index=$index, offset=$offset, contentLen=${content.length}")

                                    if (bookId != null && chapter != null && userId != null && content.isNotBlank()) {
                                        userViewModel.toggleBookmark(bookId, chapter, userId, index, offset, content)
                                    }
                                }) {
                                    Icon(
                                        painterResource(
                                            if (userViewModel.isBookmarked) R.drawable.ic_bookmark_full
                                            else R.drawable.ic_bookmark
                                        ),
                                        contentDescription = "Bookmark",
                                        tint = Color(0xFFEDE6B1),
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                                IconButton(onClick = {
                                    userViewModel.toggleTextSettingsDialog()
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.Settings,
                                        contentDescription = "Ustawienia tekstu",
                                        tint = Color(0xFFEDE6B1),
                                        modifier = Modifier.size(28.dp)
                                    )
                                }

                            }
                            isFlashcardScreen -> {
                                IconButton(onClick = {
                                    userViewModel.toggleFlashcardSettingsDialog()
                                }) {
                                    Icon(Icons.Default.Settings, contentDescription = "Ustawienia", tint = Color(0xFFEDE6B1),modifier = Modifier.size(28.dp))
                                }
                                IconButton(onClick = {
                                    userViewModel.goToPreviousFlashcard()
                                }) {
                                    Icon(painter = painterResource(R.drawable.ic_previous), contentDescription = "Wstecz", tint = Color(0xFFEDE6B1))

                                }
                            }
                        }
                    },
                            colors = TopAppBarDefaults.topAppBarColors(containerColor = FluentBackgroundDark)
                )
            }
        ) { innerPadding ->
            NavGraph(navController = navController, modifier = Modifier.padding(innerPadding))
        }
    }
}
