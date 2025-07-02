package com.yifeplayte.wommo.activity

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.yifeplayte.wommo.activity.pages.Main

class MainActivity : ComponentActivity() {
    companion object {
        lateinit var appContext: Context private set
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        appContext = this
        enableEdgeToEdge()
        window.isNavigationBarContrastEnforced = false  // Xiaomi moment, this code must be here

        setContent {
            Main()
        }
    }
}