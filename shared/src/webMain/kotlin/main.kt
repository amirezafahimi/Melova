import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import app.melova.data.repository.StudentRepository
import app.melova.platform.MelovaPlatform
import app.melova.platform.WebContext
import app.melova.ui.MelovaRoot
import app.melova.ui.theme.ThemePreferences
import kotlinx.browser.document

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    MelovaPlatform.context = WebContext
    // Before any request, so the catalog already knows which lessons this student owns.
    StudentRepository(WebContext).restoreSession()
    val themePreferences = ThemePreferences(WebContext)
    document.getElementById("loading")?.remove()
    ComposeViewport(document.body!!) {
        MelovaRoot(themePreferences)
    }
}
