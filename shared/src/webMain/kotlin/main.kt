import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.window.ComposeViewport
import app.melova.audio.web.MelovaAudioJs
import app.melova.data.repository.StudentRepository
import app.melova.platform.ChatLinks
import app.melova.platform.MelovaPlatform
import app.melova.platform.WebContext
import app.melova.resources.Res
import app.melova.resources.iran_sans_fa_num
import app.melova.resources.iran_sans_fa_num_bold
import app.melova.resources.iran_sans_fa_num_light
import app.melova.resources.iran_sans_fa_num_medium
import app.melova.resources.melova_symbols
import app.melova.ui.MelovaRoot
import app.melova.ui.theme.ThemePreferences
import kotlinx.browser.document
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.preloadFont

@OptIn(ExperimentalComposeUiApi::class, ExperimentalResourceApi::class)
fun main() {
    MelovaPlatform.context = WebContext
    // Before any request, so the catalog already knows which lessons this student owns.
    StudentRepository(WebContext).restoreSession()
    MelovaAudioJs.watchChatLinks { ChatLinks.opening.value = it }
    val themePreferences = ThemePreferences(WebContext)
    ComposeViewport(document.body!!) {
        // The page keeps its loading text until every font is in, so no text ever shows in
        // a stand-in font or as boxes.
        val fonts = listOf(
            preloadFont(Res.font.iran_sans_fa_num_light, FontWeight.Light),
            preloadFont(Res.font.iran_sans_fa_num, FontWeight.Normal),
            preloadFont(Res.font.iran_sans_fa_num_medium, FontWeight.Medium),
            preloadFont(Res.font.iran_sans_fa_num_bold, FontWeight.Bold)
        )
        // Symbols IRANSans lacks (— … ✓ → ♭); the browser has no system fonts to fall back on,
        // and a preloaded family is used as the fallback for missing glyphs.
        val symbols by preloadFont(Res.font.melova_symbols)
        val fontFamilyResolver = LocalFontFamilyResolver.current
        var symbolsReady by remember { mutableStateOf(false) }
        LaunchedEffect(symbols) {
            val font = symbols ?: return@LaunchedEffect
            fontFamilyResolver.preload(FontFamily(font))
            symbolsReady = true
        }

        if (symbolsReady && fonts.all { it.value != null }) {
            LaunchedEffect(Unit) { document.getElementById("loading")?.remove() }
            MelovaRoot(themePreferences)
        }
    }
}
