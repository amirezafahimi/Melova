package app.melova.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import app.melova.platform.PlatformBackHandler
import app.melova.ui.layout.RtlContent
import app.melova.ui.theme.MelovaAccent
import app.melova.ui.theme.MelovaCard
import app.melova.ui.theme.MelovaError
import app.melova.ui.theme.MelovaControl
import app.melova.ui.theme.MelovaText
import app.melova.ui.theme.MelovaTextMuted

@Composable
fun MelovaTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    minLines: Int = 1,
    isSecret: Boolean = false,
    keyboardType: KeyboardType = if (isSecret) KeyboardType.Password else KeyboardType.Text,
    /** For phone numbers and email addresses, which read left to right in RTL too. */
    leftToRight: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = singleLine,
        minLines = minLines,
        shape = RoundedCornerShape(16.dp),
        textStyle = MaterialTheme.typography.bodyLarge.let {
            if (leftToRight) it.copy(textDirection = TextDirection.Ltr, textAlign = TextAlign.Left) else it
        },
        visualTransformation = if (isSecret) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MelovaAccent,
            unfocusedBorderColor = MelovaControl,
            focusedLabelColor = MelovaAccent,
            unfocusedLabelColor = MelovaTextMuted,
            cursorColor = MelovaAccent,
            focusedTextColor = MelovaText,
            unfocusedTextColor = MelovaText
        ),
        modifier = modifier.fillMaxWidth()
    )
}

/**
 * The app's dialog. A dialog is its own window with its own Compose root, and that root
 * takes its layout direction from the (LTR) window, so a stock AlertDialog draws its first
 * frame LTR and flips a frame later. Here RTL is provided as the very first thing inside
 * the window, so the dialog is RTL from its first frame: title at the right, confirm at
 * the left end of the button row.
 */
@Composable
fun MelovaDialog(
    title: String,
    onDismissRequest: () -> Unit,
    buttons: @Composable RowScope.() -> Unit,
    properties: DialogProperties = DialogProperties(),
    content: @Composable ColumnScope.() -> Unit
) {
    PlatformBackHandler(enabled = properties.dismissOnBackPress, onBack = onDismissRequest)
    RtlContent {
        Dialog(onDismissRequest = onDismissRequest, properties = properties) {
            RtlContent {
                Surface(
                    shape = RoundedCornerShape(28.dp),
                    color = MelovaCard,
                    tonalElevation = 6.dp,
                    modifier = Modifier.widthIn(min = 280.dp, max = 560.dp)
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleLarge,
                            color = MelovaText,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(16.dp))
                        Column(
                            modifier = Modifier
                                .weight(1f, fill = false)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            content = content
                        )
                        Spacer(Modifier.height(20.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                            verticalAlignment = Alignment.CenterVertically,
                            content = buttons
                        )
                    }
                }
            }
        }
    }
}

/**
 * A dialog holding a small form; [confirmEnabled] guards required fields. While [busy], the
 * confirm button shows progress and the dialog can't be dismissed.
 */
@Composable
fun FormDialog(
    title: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    confirmEnabled: Boolean = true,
    busy: Boolean = false,
    dismissOnOutsideTap: Boolean = true,
    content: @Composable () -> Unit
) {
    MelovaDialog(
        title = title,
        onDismissRequest = { if (!busy && dismissOnOutsideTap) onDismiss() },
        properties = DialogProperties(dismissOnBackPress = !busy, dismissOnClickOutside = !busy && dismissOnOutsideTap),
        buttons = {
            TextButton(onClick = onDismiss, enabled = !busy) { Text("انصراف", color = MelovaTextMuted) }
            TextButton(onClick = onConfirm, enabled = confirmEnabled && !busy) {
                if (busy) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = MelovaAccent, strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                }
                Text(confirmLabel, color = if (confirmEnabled && !busy) MelovaAccent else MelovaTextMuted)
            }
        }
    ) { content() }
}

@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    destructive: Boolean = true
) {
    MelovaDialog(
        title = title,
        onDismissRequest = onDismiss,
        buttons = {
            TextButton(onClick = onDismiss) { Text("انصراف", color = MelovaTextMuted) }
            TextButton(onClick = onConfirm) {
                Text(confirmLabel, color = if (destructive) MelovaError else MelovaAccent)
            }
        }
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MelovaTextMuted,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/** Dims the screen and blocks touches while a change is uploading or saving. */
@Composable
fun BusyOverlay(label: String?) {
    if (label == null) return
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {}
            ),
        contentAlignment = Alignment.Center
    ) {
        Surface(shape = RoundedCornerShape(24.dp), color = MelovaCard) {
            RtlContent {
                Row(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MelovaAccent,
                        strokeWidth = 2.5.dp
                    )
                    Text(label, style = MaterialTheme.typography.bodyMedium, color = MelovaText)
                }
            }
        }
    }
}
