package com.example.fluentread.menu

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.fluentread.R
import com.example.fluentread.ui.theme.*
import com.example.fluentread.ui.theme.FluentTypography

@Composable
fun DrawerContent(onItemClick: (String) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(FluentSurfaceDark)
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
        ) {
            // Górne ikony w prawym rogu
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, end = 16.dp),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(onClick = { /* Ustawienia */ }) {
                    Icon(
                        imageVector = Icons.Outlined.Settings,
                        contentDescription = "Ustawienia",
                        modifier = Modifier.size(32.dp),
                        tint = FluentBackgroundDark
                    )
                }
                IconButton(onClick = { /* Profil */ }) {
                    Icon(
                        imageVector = Icons.Outlined.Logout,
                        contentDescription = "Logout",
                        modifier = Modifier.size(32.dp),
                        tint = FluentBackgroundDark
                    )
                }

            }

            // Logo
            Image(
                painter = painterResource(R.drawable.bookworm),
                contentDescription = "Logo",
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 150.dp)
                    .padding(16.dp)
            )

            // Menu items z indywidualnym tłem
            val menuItems = listOf(
                "Strona główna" to "main",
                "Fiszki" to "screen_flashcards?bookId=dummy&chapter=dummy",
                "Czat" to "screen_chat?bookId=dummy&chapter=dummy"
            )

            menuItems.forEach { (title, route) ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 0.dp, vertical = 6.dp)
                        .height(60.dp)
                        .background(FluentBackgroundDark)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.bookshelves),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    NavigationDrawerItem(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 6.dp, vertical = 20.dp)
                            .height(27.dp),
                        label = {
                            Text(
                                text = title,
                                color = FluentBackgroundDark,
                                style = FluentTypography.titleMedium
                            )
                        },
                        selected = false,
                        onClick = { onItemClick(route) },
                        shape = RectangleShape,
                        colors = androidx.compose.material3.NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = FluentSecondaryDark,
                            unselectedContainerColor = FluentSecondaryDark,
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

