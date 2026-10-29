package app.melova.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.backhandler.BackHandler

/** The system back gesture on Android; the browser's back is left to the router. */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun PlatformBackHandler(enabled: Boolean = true, onBack: () -> Unit) = BackHandler(enabled, onBack)
