package app.melova.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import app.melova.ui.theme.MelovaAccent
import app.melova.ui.theme.MelovaSecondary
import org.jetbrains.compose.resources.decodeToImageBitmap

/** The student's photo; without one, the first letter of their name or a person icon. */
@Composable
fun StudentAvatar(name: String, photo: ByteArray?, size: Dp, modifier: Modifier = Modifier) {
    val bitmap = remember(photo) { photo?.let { runCatching { it.decodeToImageBitmap() }.getOrNull() } }
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(Brush.linearGradient(listOf(MelovaAccent, MelovaSecondary))),
        contentAlignment = Alignment.Center
    ) {
        when {
            bitmap != null -> Image(
                bitmap = bitmap,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            name.isNotBlank() -> Text(text = name.trim().take(1), fontSize = (size.value * 0.42f).sp, color = Color.White)
            else -> Icon(Icons.Outlined.Person, contentDescription = null, tint = Color.White, modifier = Modifier.size(size * 0.55f))
        }
    }
}
