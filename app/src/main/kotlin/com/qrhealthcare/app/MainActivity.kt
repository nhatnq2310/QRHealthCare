package com.qrhealthcare.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.qrhealthcare.app.ui.navigation.AppNavigation
import com.qrhealthcare.app.ui.theme.QrHealthcareTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Must be called before super.onCreate() so the system splash transitions
        // smoothly into the app theme.
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            QrHealthcareTheme {
                AppNavigation()
            }
        }
    }
}
