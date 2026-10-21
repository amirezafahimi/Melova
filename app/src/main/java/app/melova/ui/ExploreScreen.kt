package app.melova.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.melova.ui.components.BottomBarContentPadding
import app.melova.ui.components.MelovaBackground
import app.melova.ui.components.MelovaChip
import app.melova.ui.components.MelovaSurfaceCard
import app.melova.ui.layout.RtlContent
import app.melova.ui.theme.MelovaAccent
import app.melova.ui.theme.MelovaAccentSoft
import app.melova.ui.theme.MelovaGold
import app.melova.ui.theme.MelovaSecondary
import app.melova.ui.theme.MelovaText
import app.melova.ui.theme.MelovaTextMuted

/** Placeholder until course discovery is built. */
@Composable
fun ExploreScreen() {
    MelovaBackground {
        RtlContent {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .verticalScroll(rememberScrollState())
                    .padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = BottomBarContentPadding),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(text = "اکسپلور", style = MaterialTheme.typography.headlineSmall, color = MelovaText)

                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(96.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(MelovaAccent, MelovaSecondary))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.Explore, contentDescription = null, tint = Color.White, modifier = Modifier.size(48.dp))
                    }
                    MelovaChip(text = "به‌زودی", color = MelovaGold)
                    Text(
                        text = "استادها و دوره‌های تازه را کشف کنید",
                        style = MaterialTheme.typography.titleLarge,
                        color = MelovaText,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "اینجا به‌زودی می‌توانید دوره‌های استادهای مختلف را ببینید، نمونه‌ی صدایشان را بشنوید و دوره‌ی دلخواهتان را دنبال کنید.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MelovaTextMuted,
                        textAlign = TextAlign.Center
                    )
                }

                UpcomingRow(Icons.Outlined.Search, "جست‌وجو بر اساس دستگاه، سطح و استاد")
                UpcomingRow(Icons.Outlined.Headphones, "شنیدن نمونه‌ی هر دوره پیش از دنبال کردن")
                UpcomingRow(Icons.Outlined.School, "صفحه‌ی معرفی هر استاد و دوره‌هایش")
            }
        }
    }
}

@Composable
private fun UpcomingRow(icon: ImageVector, text: String) {
    MelovaSurfaceCard(contentPadding = 14.dp) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(icon, contentDescription = null, tint = MelovaAccentSoft)
            Text(text = text, style = MaterialTheme.typography.bodyMedium, color = MelovaText)
        }
    }
}
