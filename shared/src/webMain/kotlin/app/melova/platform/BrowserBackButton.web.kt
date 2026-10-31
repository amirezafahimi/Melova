package app.melova.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import app.melova.audio.web.MelovaAudioJs

@Composable
actual fun BrowserBackButton(navController: NavController) {
    val current by navController.currentBackStackEntryAsState()
    val canGoBack = { !BackActions.isEmpty || navController.previousBackStackEntry != null }
    LaunchedEffect(Unit) {
        MelovaAudioJs.watchBack {
            BackActions.press(navController)
            MelovaAudioJs.holdBack(canGoBack())
        }
    }
    LaunchedEffect(current, BackActions.isEmpty) { MelovaAudioJs.holdBack(canGoBack()) }
}
