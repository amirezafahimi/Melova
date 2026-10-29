package app.melova.ui.theme

import app.melova.platform.KeyValueStore
import app.melova.platform.PlatformContext

/** The student's light/dark choice; defaults to following the system. */
class ThemePreferences(context: PlatformContext) {
    private val prefs = KeyValueStore(context, "melova_settings")

    var mode: ThemeMode
        get() = prefs.getString(KEY_THEME_MODE)
            ?.let { saved -> ThemeMode.entries.firstOrNull { it.name == saved } }
            ?: ThemeMode.SYSTEM
        set(value) = prefs.putString(KEY_THEME_MODE, value.name)

    private companion object {
        const val KEY_THEME_MODE = "theme_mode"
    }
}
