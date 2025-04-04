package com.example.fluentread.screens

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.google.firebase.auth.FirebaseAuth

@Composable
fun LoginScreen(navController: NavController) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()

    CommonBackground(tabText = "Zarejestruj się", onTabClick = { navController.navigate("register") }) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .imePadding(), // uwzględnia klawiaturę ekranową
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = "Zaloguj się",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFEDE6B1)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Środkowa część z wagą
            Column(
                modifier = Modifier
                    .weight(3f)
                    .fillMaxWidth()
            )
            {
                InputField(label = "Email", value = email, onValueChange = { email = it })

                Spacer(modifier = Modifier.height(4.dp))

                PasswordField(
                    password,
                    onValueChange = { password = it },
                    passwordVisible = passwordVisible,
                    onVisibilityChange = { passwordVisible = !passwordVisible }
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Zapomniałeś hasła?",
                    fontSize = 14.sp,
                    color = Color(0xFFEDE6B1),
                    modifier = Modifier
                        .align(Alignment.End)
                        .clickable { navController.navigate("forgot_password") }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    if (email.isBlank() || password.isBlank()) {
                        Toast.makeText(context, "Podaj email i hasło", Toast.LENGTH_SHORT).show()
                    } else {
                        isLoading = true
                        loginUser(auth, email, password, navController) { isLoading = false }
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
                        text = "Zaloguj",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFEDE6B1)
                    )
                }
            }
        }
    }
}


/**
 * Logowanie użytkownika w Firebase Authentication.
 */
fun loginUser(
    auth: FirebaseAuth,
    email: String,
    password: String,
    navController: NavController,
    onCompletion: () -> Unit
) {
    auth.signInWithEmailAndPassword(email, password)
        .addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Toast.makeText(navController.context, "Zalogowano pomyślnie!", Toast.LENGTH_LONG).show()
                navController.navigate("main") // Przekierowanie do ekranu głównego
            } else {
                Toast.makeText(navController.context, "Błąd logowania: ${task.exception?.message}", Toast.LENGTH_LONG).show()
            }
            onCompletion()
        }
}


@Preview(showBackground = true)
@Composable
fun LoginScreenPreview() {
    MaterialTheme {
        LoginScreen(navController = rememberNavController())
    }
}
