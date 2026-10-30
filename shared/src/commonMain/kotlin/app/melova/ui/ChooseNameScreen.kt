package app.melova.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.melova.platform.PlatformBackHandler
import app.melova.ui.components.BusyOverlay
import app.melova.ui.components.MelovaBackground
import app.melova.ui.components.MelovaMark
import app.melova.ui.components.MelovaTextField
import app.melova.ui.layout.RtlContent
import app.melova.ui.theme.MelovaAccent
import app.melova.ui.theme.MelovaText
import app.melova.ui.theme.MelovaTextMuted

/** The step after sign-in. Only a name, or leaving the account, gets past it. */
@Composable
fun ChooseNameScreen(isSaving: Boolean, onSave: (String) -> Unit, onSignOut: () -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }

    PlatformBackHandler {}

    MelovaBackground {
        RtlContent {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.systemBars)
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Spacer(Modifier.height(64.dp))
                MelovaMark(size = 72.dp, modifier = Modifier.align(Alignment.CenterHorizontally))
                Text(
                    text = "نام شما چیست؟",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MelovaText,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "استادها شما را با این نام می‌شناسند.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MelovaTextMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(4.dp))
                MelovaTextField(value = name, onValueChange = { name = it.take(MAX_NAME_LENGTH) }, label = "نام")
                Button(
                    onClick = { onSave(name) },
                    enabled = name.isNotBlank(),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MelovaAccent),
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) { Text("ادامه") }
                TextButton(onClick = onSignOut, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    Text("خروج از حساب", color = MelovaTextMuted)
                }
            }
        }
        BusyOverlay(if (isSaving) "در حال ذخیره…" else null)
    }
}
