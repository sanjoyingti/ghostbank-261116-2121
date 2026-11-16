package com.aibuilt.ghostbank

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.aibuilt.ghostbank.ui.GhostBankRoot
import com.aibuilt.ghostbank.ui.theme.GhostBankTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as GhostBankApp).container
        setContent {
            GhostBankTheme {
                GhostBankRoot(container)
            }
        }
    }
}
