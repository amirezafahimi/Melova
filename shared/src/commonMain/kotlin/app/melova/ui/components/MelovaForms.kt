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
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import app.melova.platform.persianDigits
import app.melova.ui.layout.RtlContent
import app.melova.ui.theme.MelovaAccent
import app.melova.ui.theme.MelovaCard
import app.melova.ui.theme.MelovaError
import app.melova.ui.theme.MelovaControl
import app.melova.ui.theme.MelovaText
import app.melova.ui.theme.MelovaTextMuted
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.VisualTransformation
import app.melova.ui.theme.MelovaAccentSoft
import app.melova.ui.theme.MelovaDimens
import app.melova.ui.theme.MelovaSmallShape

/**
 * The app's text field: 48 dp tall with the label sitting on its border, where Material's stock
 * field is 56 dp and more. [label] names a form field; a field that explains itself (search, a
 * message) takes a [placeholder] instead.
 */
@Composable
fun MelovaTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String?,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    leadingIcon: ImageVector? = null,
    singleLine: Boolean = true,
    minLines: Int = 1,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    shape: Shape = MelovaSmallShape,
    isSecret: Boolean = false,
    keyboardType: KeyboardType = if (isSecret) KeyboardType.Password else KeyboardType.Text,
    /** For phone numbers and email addresses, which read left to right in RTL too. */
    leftToRight: Boolean = false
) {
    val interactionSource = remember { MutableInteractionSource() }
    val visualTransformation = if (isSecret) PasswordVisualTransformation() else VisualTransformation.None
    val colors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = MelovaAccent,
        unfocusedBorderColor = MelovaControl,
        focusedLabelColor = MelovaAccent,
        unfocusedLabelColor = MelovaTextMuted,
        focusedPlaceholderColor = MelovaTextMuted,
        unfocusedPlaceholderColor = MelovaTextMuted,
        focusedLeadingIconColor = MelovaAccentSoft,
        unfocusedLeadingIconColor = MelovaTextMuted,
        cursorColor = MelovaAccent,
        focusedTextColor = MelovaText,
        unfocusedTextColor = MelovaText
    )
    val textStyle = MaterialTheme.typography.bodyMedium.copy(color = MelovaText).let {
        if (leftToRight) it.copy(textDirection = TextDirection.Ltr, textAlign = TextAlign.Left) else it
    }
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        // The label rises half out of the field's top edge; this keeps it off whatever is above.
        modifier = modifier
            .padding(top = if (label != null) FloatingLabelGap else 0.dp)
            .fillMaxWidth()
            .heightIn(min = MelovaDimens.ButtonHeight),
        textStyle = textStyle,
        cursorBrush = SolidColor(MelovaAccent),
        singleLine = singleLine,
        minLines = minLines,
        maxLines = maxLines,
        visualTransformation = visualTransformation,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        interactionSource = interactionSource
    ) { innerTextField ->
        OutlinedTextFieldDefaults.DecorationBox(
            value = value,
            innerTextField = innerTextField,
            enabled = true,
            singleLine = singleLine,
            visualTransformation = visualTransformation,
            interactionSource = interactionSource,
            label = label?.let { { Text(it) } },
            placeholder = placeholder?.let { { Text(it, style = MaterialTheme.typography.bodyMedium) } },
            leadingIcon = leadingIcon?.let { { Icon(it, contentDescription = null, modifier = Modifier.size(MelovaDimens.Icon)) } },
            colors = colors,
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
            container = {
                OutlinedTextFieldDefaults.Container(
                    enabled = true,
                    isError = false,
                    interactionSource = interactionSource,
                    colors = colors,
                    shape = shape,
                    focusedBorderThickness = 1.5.dp,
                    unfocusedBorderThickness = 1.dp
                )
            }
        )
    }
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
                    shape = RoundedCornerShape(20.dp),
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
fun BusyOverlay(label: String?, progress: Float? = null) {
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
        Surface(shape = RoundedCornerShape(20.dp), color = MelovaCard) {
            RtlContent {
                Row(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // While a file is going out, how much of it; after that, only that it is working.
                    val sending = progress?.takeIf { it < 1f }
                    if (sending != null) {
                        CircularProgressIndicator(
                            progress = { sending },
                            modifier = Modifier.size(24.dp),
                            color = MelovaAccent,
                            trackColor = MelovaControl,
                            strokeWidth = 2.5.dp
                        )
                    } else {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MelovaAccent,
                            strokeWidth = 2.5.dp
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(label, style = MaterialTheme.typography.bodyMedium, color = MelovaText)
                        if (sending != null) {
                            Text(
                                text = persianDigits("${(sending * 100).toInt()}٪ فرستاده شد"),
                                style = MaterialTheme.typography.labelMedium,
                                color = MelovaTextMuted
                            )
                        }
                    }
                }
            }
        }
    }
}

private val FloatingLabelGap = 8.dp

/** The app's one switch. A null [onCheckedChange] leaves the tap to the row it sits in. */
@Composable
fun MelovaSwitch(checked: Boolean, onCheckedChange: ((Boolean) -> Unit)?, enabled: Boolean = true) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        enabled = enabled,
        colors = SwitchDefaults.colors(
            checkedTrackColor = MelovaAccent,
            checkedThumbColor = Color.White,
            uncheckedTrackColor = MelovaAccent.copy(alpha = 0.22f),
            uncheckedThumbColor = MelovaControl,
            uncheckedBorderColor = MelovaControl
        )
    )
}
