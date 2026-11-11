package com.app.aparatdownloader

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.app.aparatdownloader.security.KeyStoreManager
import com.app.aparatdownloader.ui.auth.AuthScreen
import com.app.aparatdownloader.ui.navigation.MainAppScreen
import com.app.aparatdownloader.ui.theme.LuminaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Prevent screen capture & recording
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )

        val keyStoreManager = KeyStoreManager(applicationContext)

        setContent {
            LuminaTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var isLoggedIn by remember {
                        mutableStateOf(
                            keyStoreManager.getToken() != null || keyStoreManager.getLicense() != null
                        )
                    }

                    if (isLoggedIn) {
                        MainAppScreen(onLogout = {
                            keyStoreManager.clearAll()
                            isLoggedIn = false
                        })
                    } else {
                        AuthScreen(onLoginSuccess = {
                            isLoggedIn = true
                        })
                    }
                }
            }
        }
    }
}
