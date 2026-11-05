package app.melova.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.melova.platform.LocalFile
import app.melova.platform.rememberImagePicker
import app.melova.presentation.StudentUiState
import app.melova.ui.components.BusyOverlay
import app.melova.ui.components.ConfirmDialog
import app.melova.ui.components.MelovaBackground
import app.melova.ui.components.MelovaSurfaceCard
import app.melova.ui.components.MelovaTextField
import app.melova.ui.components.MelovaTopBar
import app.melova.ui.components.StudentAvatar
import app.melova.ui.layout.LtrContent
import app.melova.ui.layout.PagePadding
import app.melova.ui.layout.RtlContent
import app.melova.ui.theme.MelovaAccent
import app.melova.ui.theme.MelovaText
import app.melova.ui.theme.MelovaTextMuted

internal const val MAX_NAME_LENGTH = 60

/** The student's name and photo. Once the server offers sign-in, the profile asks for that first. */
@Composable
fun EditProfileScreen(
    state: StudentUiState,
    onSaveName: (String) -> Unit,
    onPhotoPicked: (LocalFile) -> Unit,
    onRemovePhoto: () -> Unit,
    onBack: () -> Unit
) {
    val account = state.account
    var name by rememberSaveable(account?.id) { mutableStateOf(account?.name.orEmpty()) }
    var confirmingRemove by rememberSaveable { mutableStateOf(false) }
    val pickPhoto = rememberImagePicker { picked -> picked?.let(onPhotoPicked) }

    MelovaBackground {
        RtlContent {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.systemBars)
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(PagePadding),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MelovaTopBar(title = "ویرایش پروفایل", onBack = onBack)
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(top = 12.dp)
                        .size(116.dp)
                        .clip(CircleShape)
                        .clickable(enabled = account != null, onClickLabel = "انتخاب عکس", onClick = pickPhoto)
                ) {
                    StudentAvatar(name, state.photo, size = 116.dp)
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .height(40.dp)
                            .background(Color.Black.copy(alpha = 0.45f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.PhotoCamera, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }
                if (state.photo != null) {
                    TextButton(
                        onClick = { confirmingRemove = true },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) { Text("حذف عکس", color = MelovaTextMuted) }
                }
                MelovaTextField(value = name, onValueChange = { name = it.take(MAX_NAME_LENGTH) }, label = "نام")
                Text(
                    text = "استاد این نام را کنار اجراهایی که می‌فرستید می‌بیند.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MelovaTextMuted
                )
                account?.signedInAs?.let { identity ->
                    MelovaSurfaceCard(contentPadding = 12.dp) {
                        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text("ورود با", style = MaterialTheme.typography.labelMedium, color = MelovaTextMuted)
                            LtrContent {
                                Text(
                                    text = identity,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MelovaText
                                )
                            }
                        }
                    }
                }
                Button(
                    onClick = {
                        if (name.trim() != account?.name) onSaveName(name)
                        onBack()
                    },
                    enabled = account != null && name.isNotBlank(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MelovaAccent),
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) { Text("ذخیره") }
            }
        }
        BusyOverlay(if (state.isSavingProfile) "در حال ذخیره…" else null)
    }

    if (confirmingRemove) {
        ConfirmDialog(
            title = "حذف عکس",
            message = "عکس پروفایل شما حذف شود؟",
            confirmLabel = "حذف",
            onConfirm = {
                confirmingRemove = false
                onRemovePhoto()
            },
            onDismiss = { confirmingRemove = false }
        )
    }
}
