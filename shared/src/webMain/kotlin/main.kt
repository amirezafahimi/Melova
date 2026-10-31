import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.platform.Font
import androidx.compose.ui.window.ComposeViewport
import app.melova.audio.web.MelovaAudioJs
import app.melova.data.repository.StudentRepository
import app.melova.platform.ChatLinks
import app.melova.platform.MelovaPlatform
import app.melova.platform.WebContext
import app.melova.resources.Res
import app.melova.resources.iran_sans
import app.melova.resources.iran_sans_bold
import app.melova.resources.iran_sans_light
import app.melova.resources.iran_sans_medium
import app.melova.resources.melova_symbols
import app.melova.resources.noto_color_emoji
import app.melova.resources.noto_color_emoji_flags
import app.melova.ui.MelovaRoot
import app.melova.ui.theme.ThemePreferences
import kotlinx.browser.document
import kotlinx.coroutines.CancellationException
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.getFontResourceBytes
import org.jetbrains.compose.resources.getSystemResourceEnvironment
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
            preloadFont(Res.font.iran_sans_light, FontWeight.Light),
            preloadFont(Res.font.iran_sans, FontWeight.Normal),
            preloadFont(Res.font.iran_sans_medium, FontWeight.Medium),
            preloadFont(Res.font.iran_sans_bold, FontWeight.Bold)
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
            var emojiReady by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) { emojiReady = loadEmojiFonts(fontFamilyResolver) }
            MelovaRoot(themePreferences, lateFontsLoaded = emojiReady)
        }
    }
}

/**
 * The browser gives Compose no emoji of its own, so they come from these fonts. They load after
 * the page is up: they're too big to hold it for, and a failed download only leaves boxes.
 */
@OptIn(ExperimentalResourceApi::class)
private suspend fun loadEmojiFonts(resolver: FontFamily.Resolver): Boolean = try {
    val environment = getSystemResourceEnvironment()
    listOf("noto_color_emoji" to Res.font.noto_color_emoji, "noto_color_emoji_flags" to Res.font.noto_color_emoji_flags)
        .forEach { (name, resource) ->
            resolver.preload(FontFamily(Font(name, getFontResourceBytes(environment, resource))))
        }
    true
} catch (error: CancellationException) {
    throw error
} catch (error: Throwable) {
    false
}
