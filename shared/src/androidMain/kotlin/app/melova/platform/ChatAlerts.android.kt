package app.melova.platform

import android.Manifest
import android.app.ActivityManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import app.melova.data.model.Chat
import app.melova.data.model.ChatMessageKind
import app.melova.data.repository.ChatRepository
import app.melova.data.repository.StudentRepository
import app.melova.shared.R
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** The extra on the launch intent that names the chat a notification opens. */
const val CHAT_EXTRA = "app.melova.CHAT"

private const val STORE = "chat_alerts"
private const val KEY_ON = "on"
private const val KEY_LISTENING = "listening"
private const val WORK_NAME = "chat_alerts"
private const val CHANNEL = "chat_messages"
private const val LISTENING_CHANNEL = "chat_listening"
private const val LISTENING_NOTIFICATION = 1
private const val REMINDER_CHANNEL = "practice_reminder"
private const val REMINDER_NOTIFICATION = 2
private const val LOGO_COLOR = 0xFF9B7BFF.toInt()
private const val CHECK_EVERY_MINUTES = 15L
private const val MIN_RETRY_MS = 2_000L
private const val MAX_RETRY_MS = 60_000L

actual object ChatAlerts {
    private val store get() = KeyValueStore(appContext, STORE)

    actual suspend fun state(): NotificationState {
        val isOn = store.getInt(KEY_ON) == 1
        val isAllowed = NotificationManagerCompat.from(appContext).areNotificationsEnabled()
        return when {
            isOn && isAllowed -> NotificationState.ON
            isOn || (!isAllowed && Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) -> NotificationState.BLOCKED
            else -> NotificationState.OFF
        }
    }

    actual suspend fun turnOn(serverKey: suspend () -> String): PushAddress? {
        store.putInt(KEY_ON, 1)
        val request = PeriodicWorkRequestBuilder<ChatAlertWorker>(CHECK_EVERY_MINUTES, TimeUnit.MINUTES)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        WorkManager.getInstance(appContext).enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        keepListening()
        return null
    }

    actual suspend fun turnOff(): PushAddress? {
        store.remove(KEY_ON)
        WorkManager.getInstance(appContext).cancelUniqueWork(WORK_NAME)
        stopListening()
        return null
    }

    actual suspend fun address(): PushAddress? = null

    actual fun keepListening() {
        if (store.getInt(KEY_ON) != 1) return
        store.putInt(KEY_LISTENING, 1)
        startListening(appContext)
    }

    actual fun stopListening() {
        store.remove(KEY_LISTENING)
        appContext.stopService(Intent(appContext, ChatAlertService::class.java))
    }

    actual suspend fun isAllowed(): Boolean = NotificationManagerCompat.from(appContext).areNotificationsEnabled()

    actual fun atFirstChance(ask: () -> Unit) = ask()
}

private fun startListening(context: Context) {
    // Android refuses this from some background states; the worker covers until the next chance.
    runCatching { ContextCompat.startForegroundService(context, Intent(context, ChatAlertService::class.java)) }
}

@Composable
actual fun rememberNotificationPermission(onResult: (granted: Boolean) -> Unit): () -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission(), onResult)
    return remember(context, launcher) {
        {
            val needsAsking = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            if (needsAsking) launcher.launch(Manifest.permission.POST_NOTIFICATIONS) else onResult(true)
        }
    }
}

/**
 * Holds the chat socket open while the app is closed, so a message is told as soon as it lands.
 * Android keeps a service alive only with a notification of its own, which is the quiet one here.
 */
class ChatAlertService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var listening: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val started = runCatching { startForeground(LISTENING_NOTIFICATION, quietNotification()) }
        if (started.isFailure) {
            stopSelf()
            return START_NOT_STICKY
        }
        if (listening?.isActive != true) listening = scope.launch { listen() }
        return START_STICKY
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private suspend fun listen() {
        val teller = ChatAlertTeller(applicationContext)
        val repository = ChatRepository(applicationContext)
        var retryMs = MIN_RETRY_MS
        while (true) {
            runCatching {
                repository.listen(
                    onReady = {
                        retryMs = MIN_RETRY_MS
                        scope.launch { teller.tellNewMessages() }
                    },
                    onUpdate = { scope.launch { teller.tellNewMessages() } }
                )
            }
            currentCoroutineContext().ensureActive()
            delay(retryMs)
            retryMs = (retryMs * 2).coerceAtMost(MAX_RETRY_MS)
        }
    }

    private fun quietNotification() = run {
        val manager = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager?.createNotificationChannel(
                NotificationChannel(LISTENING_CHANNEL, "آماده‌ی دریافت پیام", NotificationManager.IMPORTANCE_MIN)
            )
        }
        val open = packageManager.getLaunchIntentForPackage(packageName)
        NotificationCompat.Builder(this, LISTENING_CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(LOGO_COLOR)
            .setContentTitle("ملووا")
            .setContentText("پیام‌های تازه را همان لحظه خبر می‌دهیم")
            .setContentIntent(open?.let { PendingIntent.getActivity(this, 0, it, PendingIntent.FLAG_IMMUTABLE) })
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }
}

/** Brings the listening back after the phone restarts or the app updates. */
class ChatAlertStarter : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action != Intent.ACTION_BOOT_COMPLETED && action != Intent.ACTION_MY_PACKAGE_REPLACED) return
        val store = KeyValueStore(context.applicationContext, STORE)
        if (store.getInt(KEY_ON) == 1 && store.getInt(KEY_LISTENING) == 1) startListening(context)
    }
}

/** The safety net if Android stops the service: it sees what the socket couldn't deliver. */
class ChatAlertWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        ChatAlertTeller(applicationContext).tellNewMessages()
        return Result.success()
    }
}

/** Runs without the app, so it sets up what MainActivity normally does. */
private class ChatAlertTeller(private val context: Context) {
    suspend fun tellNewMessages() = telling.withLock {
        MelovaPlatform.context = context
        StudentRepository(context).restoreSession()
        // Offline or signed out: nothing to tell, and the next look tries again.
        val inbox = runCatching { ChatRepository(context).inbox() }.getOrElse { return@withLock }
        if (isOnScreen()) return@withLock
        val seen = KeyValueStore(context, STORE)
        inbox.chats.filter { it.unread > 0 }.forEach { chat ->
            if (chat.lastMessageAtMs > (seen.getLong(chat.id) ?: 0L)) {
                show(chat)
                seen.putLong(chat.id, chat.lastMessageAtMs)
            }
        }
        // The weekly practice reminder, which a browser gets pushed; the server gives it once.
        runCatching { StudentRepository(context).claimPracticeReminder() }.getOrNull()?.let(::showReminder)
    }

    private fun showReminder(text: String) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(NotificationChannel(REMINDER_CHANNEL, "یادآوری تمرین", NotificationManager.IMPORTANCE_DEFAULT))
        }
        val open = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val tap = open?.let {
            PendingIntent.getActivity(context, REMINDER_NOTIFICATION, it, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        }
        val notification = NotificationCompat.Builder(context, REMINDER_CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(LOGO_COLOR)
            .setContentTitle("ملووا")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(tap)
            .setAutoCancel(true)
            .build()
        manager.notify(REMINDER_NOTIFICATION, notification)
    }

    private fun isOnScreen(): Boolean {
        val process = ActivityManager.RunningAppProcessInfo()
        ActivityManager.getMyMemoryState(process)
        return process.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
    }

    private fun show(chat: Chat) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(NotificationChannel(CHANNEL, "پیام‌ها", NotificationManager.IMPORTANCE_HIGH))
        }
        val open = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
            putExtra(CHAT_EXTRA, chat.id)
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val tap = open?.let {
            PendingIntent.getActivity(
                context,
                chat.id.hashCode(),
                it,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        }
        val logo = context.packageManager.getApplicationIcon(context.packageName).toBitmap()
        val notification = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setLargeIcon(logo)
            .setColor(LOGO_COLOR)
            .setContentTitle(chat.peerName)
            .setContentText(chat.alertText())
            .setContentIntent(tap)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        manager.notify(chat.id.hashCode(), notification)
    }

    private fun Chat.alertText(): String = when (lastMessage?.kind) {
        ChatMessageKind.VOICE -> "پیام صوتی"
        ChatMessageKind.PRACTICE -> "یک تمرین فرستاد"
        else -> lastMessage?.text.orEmpty()
    }
}

/** The service and the worker can look at once, and one message must be told once. */
private val telling = Mutex()
