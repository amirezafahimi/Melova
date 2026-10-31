package app.melova.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.backhandler.BackHandler
import androidx.navigation.NavController

/** What back does right now, newest first: the same answer for the system and the browser. */
internal object BackActions {
    private val actions = mutableStateListOf<() -> Unit>()

    val isEmpty: Boolean get() = actions.isEmpty()

    fun add(action: () -> Unit) {
        actions.add(action)
    }

    fun remove(action: () -> Unit) {
        actions.remove(action)
    }

    fun press(navController: NavController) {
        val action = actions.lastOrNull()
        when {
            action != null -> action()
            navController.previousBackStackEntry != null -> navController.popBackStack()
        }
    }
}

/** The system back gesture on Android and the browser's back on the web. */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun PlatformBackHandler(enabled: Boolean = true, onBack: () -> Unit) {
    BackHandler(enabled, onBack)
    val latest by rememberUpdatedState(onBack)
    val action = remember { { latest() } }
    DisposableEffect(enabled, action) {
        if (enabled) BackActions.add(action)
        onDispose { BackActions.remove(action) }
    }
}

/** Sends the browser's back button to the page on screen; Android has its own system back. */
@Composable
expect fun BrowserBackButton(navController: NavController)
