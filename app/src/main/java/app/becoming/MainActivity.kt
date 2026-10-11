package app.becoming

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.becoming.data.ThemeSetting
import app.becoming.domain.resolveDark
import app.becoming.ui.BecomingRoot
import app.becoming.ui.theme.BecomingTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val themeSettings = appContainer.settings.themeSetting
        setContent {
            val setting: ThemeSetting? by themeSettings.collectAsStateWithLifecycle(initialValue = null)
            val systemDark = isSystemInDarkTheme()
            // Wait one frame for DataStore so a forced theme never flashes the system one.
            val loaded = setting
            if (loaded != null) {
                val dark = resolveDark(loaded.dark, systemDark)
                DisposableEffect(dark) {
                    applySystemBars(dark)
                    onDispose { }
                }
                BecomingTheme(dark = dark) {
                    BecomingRoot()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // The day may have rolled over while the app was in the background.
        appContainer.repository.refreshToday()
    }

    /** Status/navigation bar icon contrast follows the app's effective theme, not just the system's. */
    private fun applySystemBars(dark: Boolean) {
        val statusBar = if (dark) SystemBarStyle.dark(Color.TRANSPARENT) else SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        val navigationBar = if (dark) SystemBarStyle.dark(DARK_SCRIM) else SystemBarStyle.light(LIGHT_SCRIM, DARK_SCRIM)
        enableEdgeToEdge(statusBarStyle = statusBar, navigationBarStyle = navigationBar)
    }

    private companion object {
        // Same scrims androidx.activity uses by default for 3-button navigation.
        val LIGHT_SCRIM = Color.argb(0xE6, 0xFF, 0xFF, 0xFF)
        val DARK_SCRIM = Color.argb(0x80, 0x1B, 0x1B, 0x1B)
    }
}
