package com.example.fluentread

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.navigation.compose.rememberNavController
import com.example.fluentread.navigation.MainContentHost
import com.example.fluentread.navigation.NavGraph
import com.example.fluentread.ui.theme.FluentReadTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            com.example.fluentread.ui.theme.FluentReadTheme {
                val navController = rememberNavController()
                MainContentHost(navController = navController)
            }
        }
    }
}
