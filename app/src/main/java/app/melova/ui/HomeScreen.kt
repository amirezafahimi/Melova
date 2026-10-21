package app.melova.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.melova.data.model.Lesson
import app.melova.presentation.CourseUiState
import app.melova.ui.components.MelovaBackground
import app.melova.ui.components.MelovaChip
import app.melova.ui.components.MelovaMark
import app.melova.ui.components.MelovaProgressBar
import app.melova.ui.components.MelovaSurfaceCard
import app.melova.ui.components.SectionHeader
import app.melova.ui.layout.RtlContent
import app.melova.ui.theme.MelovaAccent
import app.melova.ui.theme.MelovaAccentSoft
import app.melova.ui.theme.MelovaGold
import app.melova.ui.theme.MelovaSecondary
import app.melova.ui.theme.MelovaSuccess
import app.melova.ui.theme.MelovaTextMuted
import app.melova.ui.theme.ThemeMode
import app.melova.ui.theme.MelovaCard
import app.melova.ui.theme.MelovaOutline
import app.melova.ui.theme.MelovaText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    state: CourseUiState,
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    onRefresh: () -> Unit,
    onLessonClick: (String) -> Unit,
    onSolfegeClick: () -> Unit
) {
    MelovaBackground {
        RtlContent {
            PullToRefreshBox(
                isRefreshing = state.isLoading && state.lessons.isNotEmpty(),
                onRefresh = onRefresh,
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.systemBars)
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item { HomeHeader(themeMode = themeMode, onThemeModeChange = onThemeModeChange) }
                    item {
                        ProgressHeroCard(
                            practiced = state.totalPracticed,
                            total = state.totalSections
                        )
                    }
                    item {
                        SectionHeader(
                            title = "درس‌ها",
                            trailing = state.lessons.takeIf { it.isNotEmpty() }?.let { "${it.size} درس" }
                        )
                    }

                    when {
                        state.lessons.isEmpty() && state.isLoading -> item { LoadingCard() }
                        state.lessons.isEmpty() && state.errorMessage != null -> item {
                            MessageCard(
                                icon = Icons.Default.CloudOff,
                                message = state.errorMessage ?: "",
                                actionLabel = "تلاش دوباره",
                                onAction = onRefresh
                            )
                        }
                        state.lessons.isEmpty() -> item {
                            MessageCard(
                                icon = Icons.Outlined.LibraryMusic,
                                message = "هنوز درسی منتشر نشده است. به‌زودی برمی‌گردیم!"
                            )
                        }
                        else -> itemsIndexed(state.lessons, key = { _, lesson -> lesson.id }) { index, lesson ->
                            LessonCard(
                                number = index + 1,
                                lesson = lesson,
                                practiced = state.practicedSections(lesson),
                                onClick = { onLessonClick(lesson.id) }
                            )
                        }
                    }

                    item { SectionHeader(title = "تمرین آزاد") }
                    item {
                        ExtraPracticeCard(
                            icon = Icons.Default.MusicNote,
                            title = "گام و سلفژ",
                            description = "گام‌های ماژور و مینور را بخوانید و دقت نت‌هایتان را ببینید.",
                            onClick = onSolfegeClick
                        )
                    }
                    item { Spacer(Modifier.height(12.dp)) }
                }
            }
        }
    }
}

@Composable
private fun HomeHeader(themeMode: ThemeMode, onThemeModeChange: (ThemeMode) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        MelovaMark(size = 44.dp)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Melova",
                style = MaterialTheme.typography.headlineSmall,
                color = MelovaText
            )
            Text(
                text = "امروز چه چیزی تمرین کنیم؟",
                style = MaterialTheme.typography.bodyMedium,
                color = MelovaTextMuted
            )
        }
        ThemeModeButton(themeMode = themeMode, onThemeModeChange = onThemeModeChange)
    }
}

/** Cycles system → light → dark. */
@Composable
private fun ThemeModeButton(themeMode: ThemeMode, onThemeModeChange: (ThemeMode) -> Unit) {
    val (icon, label) = when (themeMode) {
        ThemeMode.SYSTEM -> Icons.Default.BrightnessAuto to "پوسته: هماهنگ با گوشی"
        ThemeMode.LIGHT -> Icons.Default.LightMode to "پوسته: روشن"
        ThemeMode.DARK -> Icons.Default.DarkMode to "پوسته: تیره"
    }
    val next = when (themeMode) {
        ThemeMode.SYSTEM -> ThemeMode.LIGHT
        ThemeMode.LIGHT -> ThemeMode.DARK
        ThemeMode.DARK -> ThemeMode.SYSTEM
    }
    Surface(
        onClick = { onThemeModeChange(next) },
        shape = CircleShape,
        color = MelovaCard,
        border = BorderStroke(1.dp, MelovaOutline),
        modifier = Modifier.size(44.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = label, tint = MelovaAccentSoft)
        }
    }
}

@Composable
private fun ProgressHeroCard(practiced: Int, total: Int) {
    val fraction = if (total > 0) practiced.toFloat() / total else 0f
    val message = when {
        total == 0 -> "درس‌ها در حال آماده‌سازی‌اند."
        practiced == 0 -> "با اولین درس شروع کنید؛ هر بخش فقط چند دقیقه وقت می‌گیرد."
        practiced >= total -> "همه‌ی بخش‌ها را تمرین کرده‌اید. برای نمره‌ی بهتر دوباره بخوانید!"
        else -> "عالی پیش می‌روید؛ ادامه دهید."
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(
                Brush.linearGradient(
                    listOf(MelovaAccent.copy(alpha = 0.9f), MelovaSecondary.copy(alpha = 0.85f))
                )
            )
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "پیشرفت شما",
            style = MaterialTheme.typography.labelLarge,
            color = Color.White.copy(alpha = 0.85f)
        )
        Text(
            text = "$practiced از $total بخش تمرین شده",
            style = MaterialTheme.typography.headlineSmall,
            color = Color.White
        )
        MelovaProgressBar(
            progress = fraction,
            modifier = Modifier.fillMaxWidth(),
            color = Color.White,
            trackColor = Color.White.copy(alpha = 0.28f),
            followLayoutDirection = true
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.85f)
        )
    }
}

@Composable
private fun LessonCard(
    number: Int,
    lesson: Lesson,
    practiced: Int,
    onClick: () -> Unit
) {
    val available = lesson.sections.count { it.isAvailable }
    val isDone = available > 0 && practiced >= available
    MelovaSurfaceCard(onClick = onClick) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(MelovaAccent.copy(alpha = 0.35f), MelovaSecondary.copy(alpha = 0.25f))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = number.toString(),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MelovaAccentSoft
                    )
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = lesson.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MelovaText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (lesson.subtitle.isNotBlank()) {
                        Text(
                            text = lesson.subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MelovaTextMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MelovaTextMuted
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (lesson.level.isNotBlank()) MelovaChip(text = lesson.level)
                MelovaChip(text = "${lesson.sections.size} بخش", color = MelovaAccentSoft)
                if (lesson.hasIntro) MelovaChip(text = "آموزش صوتی", color = MelovaGold)
            }
            if (available > 0) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MelovaProgressBar(
                        progress = practiced.toFloat() / available,
                        modifier = Modifier.weight(1f),
                        color = if (isDone) MelovaSuccess else MelovaAccent,
                        trackColor = MelovaText.copy(alpha = 0.08f),
                        followLayoutDirection = true
                    )
                    Text(
                        text = if (isDone) "تکمیل شد" else "$practiced/$available",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isDone) MelovaSuccess else MelovaTextMuted
                    )
                }
            }
        }
    }
}

@Composable
private fun ExtraPracticeCard(
    icon: ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit
) {
    MelovaSurfaceCard(onClick = onClick) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(MelovaGold.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = MelovaGold)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleMedium, color = MelovaText)
                Text(text = description, style = MaterialTheme.typography.bodySmall, color = MelovaTextMuted)
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MelovaTextMuted
            )
        }
    }
}

@Composable
private fun LoadingCard() {
    MelovaSurfaceCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                color = MelovaAccent,
                strokeWidth = 2.dp
            )
            Text(
                text = "در حال دریافت درس‌ها…",
                style = MaterialTheme.typography.bodyMedium,
                color = MelovaTextMuted
            )
        }
    }
}

@Composable
internal fun MessageCard(
    icon: ImageVector,
    message: String,
    actionLabel: String? = null,
    onAction: () -> Unit = {}
) {
    MelovaSurfaceCard {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(icon, contentDescription = null, tint = MelovaTextMuted, modifier = Modifier.size(36.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MelovaTextMuted,
                textAlign = TextAlign.Center
            )
            if (actionLabel != null) {
                Button(
                    onClick = onAction,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MelovaAccent)
                ) { Text(actionLabel) }
            }
        }
    }
}
