package app.melova.data.local

import app.melova.platform.KeyValueStore
import app.melova.platform.PlatformContext


/**
 * The student account this install created on first launch. The token is its only key:
 * the server keeps just its hash, so losing it (clearing app data) loses the purchases.
 */
class StudentSession(context: PlatformContext) {
    private val prefs = KeyValueStore(context, "melova_student")

    val studentId: String?
        get() = prefs.getString(KEY_ID)?.takeIf { it.isNotBlank() }

    val token: String?
        get() = prefs.getString(KEY_TOKEN)?.takeIf { it.isNotBlank() }

    fun save(studentId: String, token: String) {
        prefs.putString(KEY_ID, studentId)
        prefs.putString(KEY_TOKEN, token)
    }

    private companion object {
        const val KEY_ID = "id"
        const val KEY_TOKEN = "token"
    }
}
