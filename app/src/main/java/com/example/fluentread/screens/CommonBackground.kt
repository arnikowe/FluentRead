package com.example.fluentread.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.fluentread.R

@Composable
fun CommonBackground(
    tabText: String? = null,
    onTabClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val tabHeight = 70.dp

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF0EBC6))
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Bottom
        ) {
            Image(
                painter = painterResource(id = R.drawable.bookworm),
                contentDescription = "Header",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(350.dp),
                contentScale = ContentScale.Crop
            )

            if (tabText != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(tabHeight)
                        .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                        .background(Color(0xFF8C715C))
                        .padding(16.dp)
                        .let {
                            if (onTabClick != null) it.clickable { onTabClick() } else it
                        },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (tabText.isNotEmpty()) {
                        Text(
                            text = tabText,
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFEDE6B1)
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color(0xFF6E4F3A))
                    .padding(16.dp)
            ) {
                content()
            }
        }
    }
}

@Composable
fun InputField(label: String, value: String, onValueChange: (String) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = label, fontSize = 14.sp, color = Color(0xFFEDE6B1))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = TextStyle(color = Color.Black, fontSize = 18.sp),
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White, shape = RoundedCornerShape(8.dp))
                .padding(8.dp)
        )
    }
}

@Composable
fun PasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    passwordVisible: Boolean,
    onVisibilityChange: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = "Hasło", fontSize = 14.sp, color = Color(0xFFEDE6B1))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White, shape = RoundedCornerShape(8.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                textStyle = TextStyle(color = Color.Black, fontSize = 18.sp),
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                modifier = Modifier
                    .weight(1f)
                    .padding(8.dp)
            )
            IconButton(onClick = onVisibilityChange) {
                Icon(
                    imageVector = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                    contentDescription = "Toggle Password Visibility",
                    tint = Color.Gray
                )
            }
        }
    }
}

