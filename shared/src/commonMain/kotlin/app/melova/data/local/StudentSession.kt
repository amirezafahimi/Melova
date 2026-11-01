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

    /** Whether this install has met the sign-in screen, which the profile opens on only once. */
    var loginOffered: Boolean
        get() = prefs.getInt(KEY_LOGIN_OFFERED) == 1
        set(value) = prefs.putInt(KEY_LOGIN_OFFERED, if (value) 1 else 0)

    /** Set while someone who signed in hasn't given a name, so closing the app doesn't skip the question. */
    var awaitingName: Boolean
        get() = prefs.getInt(KEY_AWAITING_NAME) == 1
        set(value) = prefs.putInt(KEY_AWAITING_NAME, if (value) 1 else 0)

    fun clear() {
        save("", "")
        awaitingName = false
    }

    private companion object {
        const val KEY_ID = "id"
        const val KEY_TOKEN = "token"
        const val KEY_LOGIN_OFFERED = "login_offered"
        const val KEY_AWAITING_NAME = "awaiting_name"
    }
}
