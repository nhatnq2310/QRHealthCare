package com.qrhealthcare.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.qrhealthcare.app.ui.navigation.AppNavigation
import com.qrhealthcare.app.ui.screens.intro.AnimatedIntroScreen
import com.qrhealthcare.app.ui.theme.QrHealthcareTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Keep the system splash extremely brief; our in-app animated intro
        // (AnimatedIntroScreen) is the real "boot animation".
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            QrHealthcareTheme {
                var showIntro by remember { mutableStateOf(true) }

                // The app is always present underneath; the intro overlays it
                // and fades away when finished, revealing the app smoothly.
                AppNavigation()

                AnimatedVisibility(
                    visible = showIntro,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    AnimatedIntroScreen(onFinished = { showIntro = false })
                }
            }
        }
    }
}
