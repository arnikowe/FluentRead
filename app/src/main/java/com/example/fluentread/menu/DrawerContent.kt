package com.example.fluentread.menu

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.fluentread.R
import com.example.fluentread.ui.theme.*
import com.google.firebase.auth.FirebaseAuth
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast

@Composable
fun DrawerContent(onItemClick: (String) -> Unit, onLogout: () -> Unit) {
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .width(260.dp)
            .fillMaxHeight()
            .background(FluentSurfaceDark)
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, end = 16.dp),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(onClick = { /* TODO: Obsługa ustawień */ }) {
                    Icon(
                        imageVector = Icons.Outlined.Settings,
                        contentDescription = "Ustawienia",
                        modifier = Modifier.size(32.dp),
                        tint = FluentBackgroundDark
                    )
                }
                IconButton(onClick = {
                    FirebaseAuth.getInstance().signOut()
                    Toast.makeText(context, "Wylogowano pomyślnie", Toast.LENGTH_SHORT).show()
                    onLogout()
                }) {
                    Icon(
                        imageVector = Icons.Outlined.Logout,
                        contentDescription = "Wyloguj",
                        modifier = Modifier.size(32.dp),
                        tint = FluentBackgroundDark
                    )
                }
            }

            Image(
                painter = painterResource(R.drawable.bookworm),
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
                        colors = NavigationDrawerItemDefaults.colors(
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
