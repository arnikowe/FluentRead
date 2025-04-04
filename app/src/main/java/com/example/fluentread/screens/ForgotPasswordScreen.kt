package com.example.fluentread.screens

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.fluentread.R
import com.google.firebase.auth.FirebaseAuth

class ForgotPasswordActivity : ComponentActivity() {
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()

        setContent {
            ForgotPasswordScreen(
                navController = TODO()
            )
        }
    }
}

@Composable
fun ForgotPasswordScreen(navController: NavController) {
    var email by remember { mutableStateOf("") }
    val context = LocalContext.current

    CommonBackground(tabText = "Zaloguj się", onTabClick = { navController.navigate("login") }) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.Start) {
            Text(
                text = "Resetowanie hasła",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFEDE6B1)
            )

            Spacer(modifier = Modifier.height(16.dp))


            InputField(label = "Email", value = email, onValueChange = { email = it })

            Spacer(modifier = Modifier.height(30.dp))

            // Button
            Button(
                onClick = {
                    if (email.isNotEmpty()) {
                        FirebaseAuth.getInstance().sendPasswordResetEmail(email)
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    Toast.makeText(context, "Link do resetowania hasła został wysłany!", Toast.LENGTH_LONG).show()
                                    navController.popBackStack()
                                } else {
                                    Toast.makeText(context, "Błąd: ${task.exception?.localizedMessage}", Toast.LENGTH_LONG).show()
                                }
                            }
                    } else {
                        Toast.makeText(context, "Podaj poprawny adres e-mail", Toast.LENGTH_SHORT).show()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFABA194)),
                modifier = Modifier.fillMaxWidth().padding(top = 110.dp)
            ) {
                Text(text = "Zresetuj hasło", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFFEDE6B1))
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}



/*
@Preview(showBackground = true)
@Composable
fun ForgotPasswordScreenPreview() {
    MaterialTheme {
        ForgotPasswordScreen(navController = rememberNavController())
    }
}*/
