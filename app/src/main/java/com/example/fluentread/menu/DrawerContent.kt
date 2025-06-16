package com.example.fluentread.menu

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.fluentread.R
import com.example.fluentread.ui.theme.*
import com.google.firebase.auth.FirebaseAuth

@Composable
fun DrawerContent(
    onItemClick: (String) -> Unit,
    onLogout: () -> Unit,
    currentRoute: String
) {
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .width(260.dp)
            .fillMaxHeight()
            .background(Background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(FluentBackgroundDark)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, end = 8.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(onClick = {
                        FirebaseAuth.getInstance().signOut()
                        Toast.makeText(context, "Wylogowano pomyślnie", Toast.LENGTH_SHORT).show()
                        onLogout()
                    }) {
                        Icon(
                            imageVector = Icons.Outlined.Logout,
                            contentDescription = "Wyloguj",
                            modifier = Modifier.size(32.dp),
                            tint = Background
                        )
                    }
                }
            }

            Image(
                painter = painterResource(R.drawable.logoznapisemmenu),
                contentDescription = "Logo",
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 150.dp)
                    .padding(16.dp)
            )

            val menuItems = listOf(
                "Strona główna" to "main",
                "Biblioteka" to "library",
                "Fiszki" to "screen_flashcards?bookId=dummy&chapter=dummy",
                "Czat" to "chat_library",
                "Statystyki" to "statistics_screen"
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(FluentBackgroundDark)
                    .padding(top = 12.dp)
            ) {
                Column(modifier = Modifier.fillMaxHeight()) {
                    menuItems.forEach { (title, route) ->
                        val isActive = currentRoute == route
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 0.dp, vertical = 2.dp)
                                .height(60.dp)
                                .background(FluentBackgroundDark)
                        ) {
                            val backgroundImage = if (isActive) {
                                painterResource(id = R.drawable.ic_bookshelves_highlighted)
                            } else {
                                painterResource(id = R.drawable.ic_bookshelves)
                            }

                            Image(
                                painter = backgroundImage,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )

                            NavigationDrawerItem(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 6.dp, vertical = 18.dp)
                                    .height(45.dp),
                                label = {
                                    val textColor = if (isActive) {
                                        Color(0xFFF0EBC6)
                                    } else {
                                        FluentBackgroundDark
                                    }
                                    Text(
                                        text = title,
                                        color = textColor,
                                        style = FluentTypography.titleMedium
                                    )
                                },
                                selected = false,
                                onClick = { onItemClick(route) },
                                shape = RectangleShape,
                                colors = NavigationDrawerItemDefaults.colors(
                                    selectedContainerColor = Color.Transparent,
                                    unselectedContainerColor = Color.Transparent,
                                    selectedIconColor = Color.White,
                                    unselectedIconColor = Color.White,
                                    selectedTextColor = Color.White,
                                    unselectedTextColor = Color.White
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}
