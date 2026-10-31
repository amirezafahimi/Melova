package app.melova.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.melova.platform.NotificationState
import app.melova.platform.rememberNotificationPermission
import app.melova.presentation.ChatUiState
import app.melova.presentation.ChatViewModel
import app.melova.ui.components.ConfirmDialog
import app.melova.ui.components.MelovaBackground
import app.melova.ui.components.MelovaNavRow
import app.melova.ui.components.MelovaTopBar
import app.melova.ui.components.SectionHeader
import app.melova.ui.layout.RtlContent
import app.melova.ui.theme.MelovaAccent
import app.melova.ui.theme.MelovaAccentTint
import app.melova.ui.theme.ThemeMode

/** Theme, notifications and leaving the account, opened from Profile. */
@Composable
fun SettingsScreen(
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    canChat: Boolean,
    chatState: ChatUiState,
    chatViewModel: ChatViewModel,
    isSignedIn: Boolean,
    onSignOut: () -> Unit,
    onBack: () -> Unit
) {
    var confirmingSignOut by rememberSaveable { mutableStateOf(false) }

    MelovaBackground {
        RtlContent {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.systemBars),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item { MelovaTopBar(title = "تنظیمات", onBack = onBack) }
                item { ThemePicker(themeMode = themeMode, onThemeModeChange = onThemeModeChange) }
                if (canChat) {
                    item { SectionHeader(title = "اعلان‌ها") }
                    item { NotificationsRow(chatState.notifications, chatViewModel) }
                }
                if (isSignedIn) {
                    item { SectionHeader(title = "حساب") }
                    item {
                        MelovaNavRow(
                            icon = Icons.AutoMirrored.Outlined.Logout,
                            title = "خروج از حساب",
                            onClick = { confirmingSignOut = true },
                            showArrow = false
                        )
                    }
                }
            }
        }
    }

    if (confirmingSignOut) {
        ConfirmDialog(
            title = "خروج از حساب",
            message = "درس‌ها و پاسخ‌های استاد روی حسابتان می‌مانند و با ورود دوباره برمی‌گردند.",
            confirmLabel = "خروج",
            onConfirm = {
                confirmingSignOut = false
                onSignOut()
            },
            onDismiss = { confirmingSignOut = false }
        )
    }
}

@Composable
private fun NotificationsRow(state: NotificationState, viewModel: ChatViewModel) {
    val permission = rememberNotificationPermission { granted ->
        if (granted) viewModel.enableNotifications() else viewModel.onNotificationsRefused()
    }

    LaunchedEffect(Unit) { viewModel.refreshNotifications() }

    val isOn = state == NotificationState.ON
    val canToggle = state == NotificationState.ON || state == NotificationState.OFF
    MelovaNavRow(
        icon = Icons.Outlined.Notifications,
        title = "اعلان پیام‌ها",
        subtitle = when (state) {
            NotificationState.ON -> "پیام‌های تازه را خبر می‌دهیم"
            NotificationState.OFF -> "وقتی پیام تازه‌ای برسد خبردار شوید"
            NotificationState.BLOCKED -> "اعلان ملووا مسدود است. از تنظیمات گوشی یا مرورگر آن را باز کنید."
            NotificationState.NEEDS_INSTALL -> "روی آیفون، اول ملووا را به صفحه‌ی اصلی گوشی اضافه کنید."
            NotificationState.UNSUPPORTED -> "این مرورگر اعلان را پشتیبانی نمی‌کند."
        },
        onClick = {
            when (state) {
                NotificationState.ON -> viewModel.disableNotifications()
                NotificationState.OFF -> permission()
                else -> Unit
            }
        },
        showArrow = false,
        trailing = {
            if (canToggle) {
                Switch(
                    checked = isOn,
                    onCheckedChange = null,
                    colors = SwitchDefaults.colors(
                        checkedTrackColor = MelovaAccent,
                        uncheckedTrackColor = MelovaAccentTint,
                        uncheckedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )
            }
        }
    )
}
