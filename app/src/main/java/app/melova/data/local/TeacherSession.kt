package app.melova.data.local

import android.content.Context

/** The teacher code entered in the profile tab; its presence means teacher mode is on. */
class TeacherSession(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences("melova_teacher", Context.MODE_PRIVATE)

    var token: String?
        get() = prefs.getString(KEY_TOKEN, null)?.takeIf { it.isNotBlank() }
        set(value) {
            prefs.edit().apply {
                if (value.isNullOrBlank()) remove(KEY_TOKEN) else putString(KEY_TOKEN, value.trim())
            }.apply()
        }

    private companion object {
        const val KEY_TOKEN = "token"
    }
}
