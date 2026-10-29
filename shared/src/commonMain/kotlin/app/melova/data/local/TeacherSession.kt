package app.melova.data.local

import app.melova.platform.KeyValueStore
import app.melova.platform.PlatformContext


/** The teacher code entered in the profile tab; its presence means teacher mode is on. */
class TeacherSession(context: PlatformContext) {
    private val prefs = KeyValueStore(context, "melova_teacher")

    var token: String?
        get() = prefs.getString(KEY_TOKEN)?.takeIf { it.isNotBlank() }
        set(value) {
            if (value.isNullOrBlank()) prefs.remove(KEY_TOKEN) else prefs.putString(KEY_TOKEN, value.trim())
        }

    private companion object {
        const val KEY_TOKEN = "token"
    }
}
