package com.example.fluentread.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.rememberNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

@Composable
fun RegisterScreen(navController: NavController) {
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()

    CommonBackground(tabText = "Zaloguj się", onTabClick = { navController.navigate("login") }) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .imePadding(),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = "Zarejestruj się",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFEDE6B1)
            )

            Column(
                modifier = Modifier
                    .weight(1.6f)
                    .fillMaxWidth()
            ) {
                InputField(label = "Login", value = username, onValueChange = { username = it })

                Spacer(modifier = Modifier.height(8.dp))

                InputField(label = "Email", value = email, onValueChange = { email = it })


                PasswordField(
                    value = password,
                    onValueChange = { password = it },
                    passwordVisible = passwordVisible,
                    onVisibilityChange = { passwordVisible = !passwordVisible }
                )
            }

            Button(
                onClick = {
                    if (username.isBlank() || email.isBlank() || password.isBlank()) {
                        Toast.makeText(context, "Wszystkie pola muszą być wypełnione", Toast.LENGTH_SHORT).show()
                    } else {
                        isLoading = true
                        registerUser(auth, db, username, email, password, navController) {
                            isLoading = false
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFABA194)),
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White)
                } else {
                    Text(
                        text = "Zarejestruj",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFEDE6B1)
                    )
                }
            }
        }
    }
}


fun registerUser(
    auth: FirebaseAuth,
    db: FirebaseFirestore,
    username: String,
    email: String,
    password: String,
    navController: NavController,
    onCompletion: () -> Unit
) {
    auth.createUserWithEmailAndPassword(email, password)
        .addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val user = task.result?.user
                if (user != null) {
                    val userData = hashMapOf(
                        "uid" to user.uid,
                        "username" to username,
                        "email" to email
                    )

                    db.collection("users").document(user.uid)
                        .set(userData, SetOptions.merge())
                        .addOnSuccessListener {
                            Toast.makeText(navController.context, "Rejestracja zakończona sukcesem!", Toast.LENGTH_LONG).show()
                            navController.navigate("login")
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(navController.context, "Błąd zapisu danych: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                        .addOnCompleteListener {
                            onCompletion()
                        }
                }
            } else {
                Toast.makeText(navController.context, "Błąd rejestracji: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                onCompletion()
            }
        }
}

@Preview(showBackground = true)
@Composable
fun RegisterScreenPreview() {
    MaterialTheme {
        val navController = rememberNavController()
        RegisterScreen(navController = rememberNavController())
    }
}



