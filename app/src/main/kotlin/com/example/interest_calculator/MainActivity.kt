package com.example.interest_calculator

import android.os.Bundle
import android.view.View
import android.view.animation.PathInterpolator
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.interest_calculator.ui.CalculatorApp
import com.example.interest_calculator.ui.theme.InterestTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        // Draw behind the status and navigation bars; Compose insets keep content clear of them.
        enableEdgeToEdge()

        // Splash hand-off: the icon swells slightly while the splash fades, revealing the app
        // underneath as it plays its own staggered entrance.
        splashScreen.setOnExitAnimationListener { provider ->
            val emphasized = PathInterpolator(0.3f, 0f, 0.8f, 0.15f)
            val icon: View? = runCatching { provider.iconView }.getOrNull()
            icon?.animate()?.scaleX(1.25f)?.scaleY(1.25f)?.setDuration(260L)?.setInterpolator(emphasized)?.start()
            provider.view.animate()
                .alpha(0f)
                .setDuration(260L)
                .setInterpolator(emphasized)
                .withEndAction { provider.remove() }
                .start()
        }

        setContent {
            InterestTheme {
                CalculatorApp()
            }
        }
    }
}
