package no.utgdev.getstrong

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import no.utgdev.getstrong.ui.navigation.GetStrongApp
import no.utgdev.getstrong.ui.theme.GetStrongTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GetStrongTheme {
                GetStrongApp()
            }
        }
    }
}
