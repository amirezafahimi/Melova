package app.melova.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.melova.data.model.LoginMethods
import app.melova.data.model.StudentAccount
import app.melova.data.repository.LoginChannel
import app.melova.platform.PlatformBackHandler
import app.melova.platform.formatPersianInteger
import app.melova.platform.rememberGoogleSignIn
import app.melova.presentation.AuthUiState
import app.melova.presentation.AuthViewModel
import app.melova.presentation.LOGIN_CODE_LENGTH
import app.melova.presentation.LoginGate
import app.melova.ui.components.MelovaBackground
import app.melova.ui.components.MelovaMark
import app.melova.ui.components.MelovaSegmentedControl
import app.melova.ui.theme.MelovaDimens
import app.melova.ui.components.MelovaTextField
import app.melova.ui.components.MelovaTopBar
import app.melova.ui.layout.LtrContent
import app.melova.ui.layout.PagePadding
import app.melova.ui.layout.RtlContent
import app.melova.ui.theme.MelovaAccent
import app.melova.ui.theme.MelovaCard
import app.melova.ui.theme.MelovaError
import app.melova.ui.theme.MelovaOutline
import app.melova.ui.theme.MelovaText
import app.melova.ui.theme.MelovaTextMuted

/** Why the student landed on the login screen, in their terms. */
enum class LoginReason(val message: String) {
    PROFILE("با ورود می‌توانید تمرینتان را برای استاد بفرستید و روی هر گوشی‌ای به درس‌هایتان دسترسی داشته باشید."),
    TEACHER("برای فرستادن تمرین برای استاد وارد شوید تا پاسخ استاد به دست خودتان برسد."),
    MESSAGE("برای پیام دادن به استاد وارد شوید تا پاسخ استاد به دست خودتان برسد."),
    PURCHASE("برای خرید درس وارد شوید تا درس همیشه برایتان بماند و گم نشود."),
    ADD_LESSON("برای افزودن درس وارد شوید تا درس همیشه برایتان بماند و گم نشود.")
}

/**
 * A tap on something that takes an account. Someone not signed in goes to login, and [action]
 * waits for them through the login page and the name page, so they land back on what they were
 * doing. Leaving login, or leaving the account on the name page, drops it.
 */
@Composable
internal fun rememberLoginGated(gate: LoginGate, onLogin: () -> Unit, action: () -> Unit): () -> Unit {
    var isHeld by rememberSaveable { mutableStateOf(false) }
    val latestAction by rememberUpdatedState(action)

    LaunchedEffect(gate) {
        when (gate) {
            LoginGate.SIGN_IN -> isHeld = false
            LoginGate.WAIT -> Unit
            LoginGate.OPEN -> if (isHeld) {
                isHeld = false
                latestAction()
            }
        }
    }
    return {
        if (gate == LoginGate.SIGN_IN) {
            isHeld = true
            onLogin()
        } else {
            action()
        }
    }
}

@Composable
fun LoginScreen(
    reason: LoginReason,
    methods: LoginMethods?,
    onBack: () -> Unit,
    onSignedIn: (StudentAccount) -> Unit,
    viewModel: AuthViewModel = viewModel { AuthViewModel() }
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val signInWithGoogle = rememberGoogleSignIn(viewModel::onGoogleResult)
    val onCodeStep = state.sentTo != null

    PlatformBackHandler(enabled = onCodeStep, onBack = viewModel::backToAddress)
    LaunchedEffect(state.signedIn) { state.signedIn?.let(onSignedIn) }
    LaunchedEffect(methods) {
        if (methods != null && !methods.phone && methods.email) viewModel.setChannel(LoginChannel.EMAIL)
    }

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
                MelovaTopBar(title = "ورود", onBack = if (onCodeStep) viewModel::backToAddress else onBack)
                MelovaMark(size = 56.dp, modifier = Modifier.align(Alignment.CenterHorizontally))
                Text(
                    text = "ورود یا ثبت‌نام",
                    style = MaterialTheme.typography.titleLarge,
                    color = MelovaText,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = reason.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MelovaTextMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))

                when {
                    onCodeStep -> CodeStep(
                        state = state,
                        onCodeChange = viewModel::setCode,
                        onResend = viewModel::requestCode,
                        onChangeAddress = viewModel::backToAddress
                    )
                    methods?.any == true -> {
                        AddressStep(
                            state = state,
                            methods = methods,
                            onGoogle = signInWithGoogle,
                            onChannelChange = viewModel::setChannel,
                            onAddressChange = viewModel::setAddress,
                            onSubmit = if (methods.isMock) viewModel::signInWithoutCode else viewModel::requestCode
                        )
                        Text(
                            text = "حساب ندارید؟ مشکلی نیست؛ با همین کار برایتان ساخته می‌شود.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MelovaTextMuted,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                        TextButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                            Text("فعلاً نه، بعداً وارد می‌شوم", color = MelovaTextMuted)
                        }
                    }
                    else -> Text(
                        text = "ورود الان در دسترس نیست. کمی بعد دوباره امتحان کنید.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MelovaTextMuted,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                state.error?.let { error ->
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodySmall,
                        color = MelovaError,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun AddressStep(
    state: AuthUiState,
    methods: LoginMethods,
    onGoogle: (clientId: String) -> Unit,
    onChannelChange: (LoginChannel) -> Unit,
    onAddressChange: (String) -> Unit,
    onSubmit: () -> Unit
) {
    val hasCodeLogin = methods.phone || methods.email
    methods.googleClientId?.let { clientId ->
        GoogleButton(enabled = !state.isBusy, onClick = { onGoogle(clientId) })
        if (hasCodeLogin) OrDivider()
    }
    if (methods.phone && methods.email) {
        val channels = listOf(LoginChannel.PHONE, LoginChannel.EMAIL)
        MelovaSegmentedControl(
            options = listOf("شماره موبایل", "ایمیل"),
            selectedIndex = channels.indexOf(state.channel),
            onSelect = { onChannelChange(channels[it]) },
            enabled = !state.isBusy
        )
    }
    if (hasCodeLogin) {
        val isPhone = state.channel == LoginChannel.PHONE
        MelovaTextField(
            value = state.address,
            onValueChange = onAddressChange,
            label = if (isPhone) "شماره موبایل" else "ایمیل",
            keyboardType = if (isPhone) KeyboardType.Phone else KeyboardType.Email,
            leftToRight = true
        )
        Button(
            onClick = onSubmit,
            enabled = !state.isBusy && state.address.isNotBlank(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MelovaAccent),
            modifier = Modifier.fillMaxWidth().height(MelovaDimens.PrimaryButtonHeight)
        ) {
            if (state.isBusy) {
                CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
            } else {
                Text(if (methods.isMock) "ورود" else "دریافت کد")
            }
        }
    }
}

@Composable
private fun CodeStep(
    state: AuthUiState,
    onCodeChange: (String) -> Unit,
    onResend: () -> Unit,
    onChangeAddress: () -> Unit
) {
    val sentTo = if (state.channel == LoginChannel.PHONE) "این شماره" else "این ایمیل"
    Text(
        text = "کد ${formatPersianInteger(LOGIN_CODE_LENGTH.toLong())} رقمی را که به $sentTo فرستادیم وارد کنید:",
        style = MaterialTheme.typography.bodyMedium,
        color = MelovaText,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth()
    )
    LtrContent {
        Text(
            text = state.sentTo.orEmpty(),
            style = MaterialTheme.typography.titleMedium,
            color = MelovaText,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
    CodeField(code = state.code, onCodeChange = onCodeChange, enabled = !state.isBusy)
    if (state.isBusy) {
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(strokeWidth = 2.dp, color = MelovaAccent, modifier = Modifier.size(20.dp))
        }
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val waitingForResend = state.resendInSeconds > 0
        TextButton(onClick = onResend, enabled = !waitingForResend && !state.isBusy) {
            Text(
                text = if (waitingForResend) {
                    "ارسال دوباره تا ${formatPersianInteger(state.resendInSeconds.toLong())} ثانیه‌ی دیگر"
                } else {
                    "ارسال دوباره"
                },
                color = if (waitingForResend) MelovaTextMuted else MelovaAccent
            )
        }
        TextButton(onClick = onChangeAddress, enabled = !state.isBusy) {
            Text(
                text = if (state.channel == LoginChannel.PHONE) "تغییر شماره" else "تغییر ایمیل",
                color = MelovaTextMuted
            )
        }
    }
}

/** One box per digit; the real text field is hidden behind them. Digits run left to right. */
@Composable
private fun CodeField(code: String, onCodeChange: (String) -> Unit, enabled: Boolean) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    LtrContent {
        BasicTextField(
            value = code,
            onValueChange = onCodeChange,
            enabled = enabled,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            cursorBrush = SolidColor(Color.Transparent),
            modifier = Modifier.fillMaxWidth().focusRequester(focus),
            decorationBox = { innerTextField ->
                Box {
                    Box(Modifier.size(1.dp).alpha(0f)) { innerTextField() }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        repeat(LOGIN_CODE_LENGTH) { index ->
                            CodeCell(
                                digit = code.getOrNull(index),
                                isNext = index == code.length,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        )
    }
}

@Composable
private fun CodeCell(digit: Char?, isNext: Boolean, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(12.dp)
    val borderColor by animateColorAsState(if (isNext) MelovaAccent else MelovaOutline)
    Box(
        modifier = modifier
            .height(56.dp)
            .clip(shape)
            .background(MelovaCard)
            .border(if (isNext) 2.dp else 1.dp, borderColor, shape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = digit?.toString().orEmpty(),
            style = MaterialTheme.typography.titleLarge,
            color = MelovaText
        )
    }
}

@Composable
private fun OrDivider() {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        HorizontalDivider(modifier = Modifier.weight(1f), color = MelovaOutline)
        Text("یا", style = MaterialTheme.typography.labelMedium, color = MelovaTextMuted)
        HorizontalDivider(modifier = Modifier.weight(1f), color = MelovaOutline)
    }
}

/** White in both themes, as Google's branding rules ask. */
@Composable
private fun GoogleButton(enabled: Boolean, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White, contentColor = Color(0xFF1F1F1F)),
        border = BorderStroke(1.dp, Color(0xFF747775)),
        modifier = Modifier.fillMaxWidth().height(MelovaDimens.PrimaryButtonHeight)
    ) {
        GoogleMark(size = 20.dp)
        Spacer(Modifier.width(10.dp))
        Text("ادامه با گوگل")
    }
}

private const val GOOGLE_MARK_GRID = 48f
private val GOOGLE_MARK_PATHS = listOf(
    Color(0xFFEA4335) to "M24 9.5c3.54 0 6.71 1.22 9.21 3.6l6.85-6.85C35.9 2.38 30.47 0 24 0 14.62 0 6.51 5.38 2.56 13.22l7.98 6.19C12.43 13.72 17.74 9.5 24 9.5z",
    Color(0xFF4285F4) to "M46.98 24.55c0-1.57-.15-3.09-.38-4.55H24v9.02h12.94c-.58 2.96-2.26 5.48-4.78 7.18l7.73 6c4.51-4.18 7.09-10.36 7.09-17.65z",
    Color(0xFFFBBC05) to "M10.53 28.59c-.48-1.45-.76-2.99-.76-4.59s.27-3.14.76-4.59l-7.98-6.19C.92 16.46 0 20.12 0 24c0 3.88.92 7.54 2.56 10.78l7.97-6.19z",
    Color(0xFF34A853) to "M24 48c6.48 0 11.93-2.13 15.89-5.81l-7.73-6c-2.15 1.45-4.92 2.3-8.16 2.3-6.26 0-11.57-4.22-13.47-9.91l-7.98 6.19C6.51 42.62 14.62 48 24 48z"
)

@Composable
private fun GoogleMark(size: Dp) {
    val paths = remember { GOOGLE_MARK_PATHS.map { (color, data) -> color to PathParser().parsePathString(data).toPath() } }
    Canvas(modifier = Modifier.size(size)) {
        scale(this.size.minDimension / GOOGLE_MARK_GRID, pivot = Offset.Zero) {
            paths.forEach { (color, path) -> drawPath(path, color) }
        }
    }
}
