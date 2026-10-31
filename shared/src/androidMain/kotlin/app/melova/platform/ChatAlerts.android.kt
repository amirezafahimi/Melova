package app.melova.platform

import android.Manifest
import android.app.ActivityManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
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
import java.util.concurrent.TimeUnit

/** The extra on the launch intent that names the chat a notification opens. */
const val CHAT_EXTRA = "app.melova.CHAT"

private const val STORE = "chat_alerts"
private const val KEY_ON = "on"
private const val WORK_NAME = "chat_alerts"
private const val CHANNEL = "chat_messages"
private const val CHECK_EVERY_MINUTES = 15L

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
        return null
    }

    actual suspend fun turnOff(): PushAddress? {
        store.remove(KEY_ON)
        WorkManager.getInstance(appContext).cancelUniqueWork(WORK_NAME)
        return null
    }

    actual suspend fun address(): PushAddress? = null
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

/** Runs in the background without the app, so it sets up what MainActivity normally does. */
class ChatAlertWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        MelovaPlatform.context = applicationContext
        StudentRepository(applicationContext).restoreSession()
        // Offline or signed out: nothing to tell, and the next run looks again.
        val inbox = runCatching { ChatRepository(applicationContext).inbox() }.getOrElse { return Result.success() }
        if (isOnScreen()) return Result.success()
        val seen = KeyValueStore(applicationContext, STORE)
        inbox.chats.filter { it.unread > 0 }.forEach { chat ->
            if (chat.lastMessageAtMs > (seen.getLong(chat.id) ?: 0L)) {
                show(chat)
                seen.putLong(chat.id, chat.lastMessageAtMs)
            }
        }
        return Result.success()
    }

    private fun isOnScreen(): Boolean {
        val process = ActivityManager.RunningAppProcessInfo()
        ActivityManager.getMyMemoryState(process)
        return process.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
    }

    private fun show(chat: Chat) {
        val manager = applicationContext.getSystemService(NotificationManager::class.java) ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(NotificationChannel(CHANNEL, "پیام‌ها", NotificationManager.IMPORTANCE_HIGH))
        }
        val open = applicationContext.packageManager.getLaunchIntentForPackage(applicationContext.packageName)?.apply {
            putExtra(CHAT_EXTRA, chat.id)
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val tap = open?.let {
            PendingIntent.getActivity(
                applicationContext,
                chat.id.hashCode(),
                it,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        }
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL)
            .setSmallIcon(android.R.drawable.stat_notify_chat)
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
