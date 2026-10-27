package com.floresvalencia.echoguard

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.floresvalencia.echoguard.data.repository.EchoRepository
import com.floresvalencia.echoguard.ui.navigation.EchoApp
import com.floresvalencia.echoguard.ui.theme.EchoGuardTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            EchoGuardTheme {
                EchoApp()
            }
        }
    }

    /** Cada toque reinicia el contador de inactividad del modo personal. */
    override fun onUserInteraction() {
        super.onUserInteraction()
        EchoRepository.touch()
    }
}
