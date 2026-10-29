package app.melova.data.local

import android.content.Context

/**
 * The student account this install created on first launch. The token is its only key:
 * the server keeps just its hash, so losing it (clearing app data) loses the purchases.
 */
class StudentSession(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences("melova_student", Context.MODE_PRIVATE)

    val studentId: String?
        get() = prefs.getString(KEY_ID, null)?.takeIf { it.isNotBlank() }

    val token: String?
        get() = prefs.getString(KEY_TOKEN, null)?.takeIf { it.isNotBlank() }

    fun save(studentId: String, token: String) {
        prefs.edit().putString(KEY_ID, studentId).putString(KEY_TOKEN, token).apply()
    }

    private companion object {
        const val KEY_ID = "id"
        const val KEY_TOKEN = "token"
    }
}
