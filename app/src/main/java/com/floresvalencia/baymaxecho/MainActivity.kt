package com.floresvalencia.baymaxecho

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.floresvalencia.baymaxecho.ui.navigation.EchoApp
import com.floresvalencia.baymaxecho.ui.theme.BaymaxEchoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BaymaxEchoTheme {
                EchoApp()
            }
        }
    }
}
