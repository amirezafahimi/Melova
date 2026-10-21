package app.melova.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp
import app.melova.ui.components.MelovaBackground
import app.melova.ui.components.MelovaMark
import app.melova.ui.layout.RtlContent
import app.melova.ui.theme.MelovaTextMuted
import kotlinx.coroutines.delay
import app.melova.ui.theme.MelovaText

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    val reveal = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        reveal.animateTo(1f, tween(durationMillis = 700, easing = FastOutSlowInEasing))
        delay(700)
        onFinished()
    }

    MelovaBackground {
        RtlContent {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(reveal.value),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                MelovaMark(
                    size = 120.dp,
                    modifier = Modifier.scale(0.8f + 0.2f * reveal.value)
                )
                Spacer(Modifier.height(24.dp))
                Text(text = "Melova", style = MaterialTheme.typography.displaySmall, color = MelovaText)
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "آواز ایرانی را با بازخورد دقیق تمرین کنید",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MelovaTextMuted
                )
            }
        }
    }
}
