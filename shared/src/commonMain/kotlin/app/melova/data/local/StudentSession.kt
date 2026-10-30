package app.melova.data.local

import app.melova.platform.KeyValueStore
import app.melova.platform.PlatformContext


/**
 * The account this install is using: a guest one it created on first launch, or the
 * student's own after signing in. The token is the key to it: the server keeps just its
 * hash, so a guest who loses it (clearing app data) loses the purchases.
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

    fun clear() = save("", "")

    private companion object {
        const val KEY_ID = "id"
        const val KEY_TOKEN = "token"
    }
}
