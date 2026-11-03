package com.lifetimere.app

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.lifetimere.app.ui.LifeTimeApp
import com.lifetimere.app.ui.theme.LifeTimeTheme

class MainActivity : ComponentActivity() {
    override fun onResume() {
        super.onResume()
        (application as LifeTimeApplication).recoverReviews()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        setContent {
            LifeTimeTheme { LifeTimeApp() }
        }
    }
}
