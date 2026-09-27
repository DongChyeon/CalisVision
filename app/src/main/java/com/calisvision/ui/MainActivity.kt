package com.calisvision.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.calisvision.CalisVisionApp
import com.calisvision.ui.navigation.CalisNavHost
import com.calisvision.ui.theme.CalisVisionTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as CalisVisionApp).container
        setContent {
            CalisVisionTheme {
                CalisNavHost(container)
            }
        }
    }
}
