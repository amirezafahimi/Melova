package app.melova

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import app.melova.data.repository.StudentRepository
import app.melova.platform.CHAT_EXTRA
import app.melova.platform.ChatLinks
import app.melova.platform.MelovaPlatform
import app.melova.ui.MelovaRoot
import app.melova.ui.theme.ThemePreferences

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MelovaPlatform.context = applicationContext
        enableEdgeToEdge()
        // Before any request, so the catalog already knows which lessons this student owns.
        StudentRepository(this).restoreSession()
        openTappedChat(intent)
        val themePreferences = ThemePreferences(this)
        setContent {
            MelovaRoot(themePreferences) { darkTheme ->
                // Status/navigation bar icons follow the app's theme, not only the system's.
                LaunchedEffect(darkTheme) {
                    val barStyle = if (darkTheme) {
                        SystemBarStyle.dark(Color.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                    }
                    enableEdgeToEdge(statusBarStyle = barStyle, navigationBarStyle = barStyle)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        openTappedChat(intent)
    }

    private fun openTappedChat(intent: Intent) {
        intent.getStringExtra(CHAT_EXTRA)?.let { ChatLinks.opening.value = it }
        intent.removeExtra(CHAT_EXTRA)
    }
}
