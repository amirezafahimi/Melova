package app.melova.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.unit.dp
import app.melova.ui.layout.RtlContent
import app.melova.ui.theme.MelovaAccent
import app.melova.ui.theme.MelovaCard
import app.melova.ui.theme.MelovaError
import app.melova.ui.theme.MelovaOutline
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
    isSecret: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = singleLine,
        minLines = minLines,
        shape = RoundedCornerShape(16.dp),
        textStyle = MaterialTheme.typography.bodyLarge,
        visualTransformation = if (isSecret) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        keyboardOptions = if (isSecret) KeyboardOptions(keyboardType = KeyboardType.Password) else KeyboardOptions.Default,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MelovaAccent,
            unfocusedBorderColor = MelovaOutline,
            focusedLabelColor = MelovaAccent,
            unfocusedLabelColor = MelovaTextMuted,
            cursorColor = MelovaAccent,
            focusedTextColor = MelovaText,
            unfocusedTextColor = MelovaText
        ),
        modifier = modifier.fillMaxWidth()
    )
}

/** A dialog holding a small form; [confirmEnabled] guards required fields. */
@Composable
fun FormDialog(
    title: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    confirmEnabled: Boolean = true,
    content: @Composable () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MelovaCard,
        shape = RoundedCornerShape(28.dp),
        title = { RtlContent { Text(title, style = MaterialTheme.typography.titleLarge, color = MelovaText) } },
        text = {
            RtlContent {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) { content() }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = confirmEnabled) {
                Text(confirmLabel, color = if (confirmEnabled) MelovaAccent else MelovaTextMuted)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("انصراف", color = MelovaTextMuted) }
        }
    )
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
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MelovaCard,
        shape = RoundedCornerShape(28.dp),
        title = { RtlContent { Text(title, style = MaterialTheme.typography.titleLarge, color = MelovaText) } },
        text = { RtlContent { Text(message, style = MaterialTheme.typography.bodyMedium, color = MelovaTextMuted) } },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmLabel, color = if (destructive) MelovaError else MelovaAccent)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("انصراف", color = MelovaTextMuted) }
        }
    )
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
