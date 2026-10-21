package app.melova.ui.theme

import android.content.Context

/** The student's light/dark choice; defaults to following the system. */
class ThemePreferences(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences("melova_settings", Context.MODE_PRIVATE)

    var mode: ThemeMode
        get() = prefs.getString(KEY_THEME_MODE, null)
            ?.let { saved -> ThemeMode.entries.firstOrNull { it.name == saved } }
            ?: ThemeMode.SYSTEM
        set(value) = prefs.edit().putString(KEY_THEME_MODE, value.name).apply()

    private companion object {
        const val KEY_THEME_MODE = "theme_mode"
    }
}
