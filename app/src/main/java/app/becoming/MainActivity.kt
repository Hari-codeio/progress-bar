package app.becoming

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import app.becoming.ui.BecomingRoot
import app.becoming.ui.theme.BecomingTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            BecomingTheme {
                BecomingRoot()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // The day may have rolled over while the app was in the background.
        appContainer.repository.refreshToday()
    }
}
