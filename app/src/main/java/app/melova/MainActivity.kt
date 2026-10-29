package app.melova

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import app.melova.data.repository.StudentRepository
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
}
