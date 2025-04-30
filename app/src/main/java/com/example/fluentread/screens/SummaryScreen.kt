package com.example.fluentread.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.example.fluentread.R
import com.example.fluentread.ui.theme.FluentBackgroundDark
import com.example.fluentread.ui.theme.FluentSecondaryDark

@Composable
fun SummaryScreen(
    bookTitle: String,
    chapter: String? = null,
    correctAnswers: Int,
    wrongAnswers: Int,
    onRepeat: () -> Unit,
    onBack: () -> Unit
) {
    val totalAnswers = correctAnswers + wrongAnswers
    val correctRatio = if (totalAnswers == 0) 0f else correctAnswers / totalAnswers.toFloat()
    val rating = (correctRatio * 100).toInt()

    val uniformElementModifier = Modifier
        .fillMaxWidth()
        .height(40.dp)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF6E4A36))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(modifier = Modifier.height(50.dp))
            Text(
                text = "Podsumowanie",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFF6EFC6)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = bookTitle + (chapter?.let { "\nrozdział $it" } ?: ""),
                fontSize = 16.sp,
                color = Color(0xFFF6EFC6),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))

            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(250.dp)) {

                CircularProgressIndicator(
                    progress = correctRatio,
                    strokeWidth = 12.dp,
                    color = Color(0xFF82B56F),
                    trackColor = Color(0xFFB5766F),
                    modifier = Modifier.fillMaxSize()
                )

                CircularProgressIndicator(
                    progress = rating / 100f,
                    strokeWidth = 12.dp,
                    color = FluentBackgroundDark,
                    trackColor = Color.Transparent,
                    modifier = Modifier
                        .fillMaxSize(0.75f)
                )

                Image(
                    painter = painterResource(id = R.drawable.bookworm),
                    contentDescription = "Bookworm",
                    modifier = Modifier.size(140.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Surface(
                modifier = uniformElementModifier,
                color = FluentBackgroundDark,
                shape = RoundedCornerShape(8.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$rating%",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = FluentSecondaryDark,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                modifier = uniformElementModifier,
                color = Color(0xFF82B56F),
                shape = RoundedCornerShape(8.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$correctAnswers dobrych odpowiedzi",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = FluentSecondaryDark,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                modifier = uniformElementModifier,
                color = Color(0xFFB5766F),
                shape = RoundedCornerShape(8.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$wrongAnswers złych odpowiedzi",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = FluentSecondaryDark,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onRepeat,
                modifier = uniformElementModifier,
                colors = ButtonDefaults.buttonColors(
                    containerColor = FluentBackgroundDark
                ),
                //border = BorderStroke(2.dp, FluentSecondaryDark),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "Powtórz jeszcze raz",
                    color = Color(0xFFF6EFC6),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = onBack,
                modifier = uniformElementModifier,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent
                ),
                border = BorderStroke(2.dp, FluentSecondaryDark),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "Powrót",
                    color = Color(0xFFF6EFC6),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

        }
    }
}
