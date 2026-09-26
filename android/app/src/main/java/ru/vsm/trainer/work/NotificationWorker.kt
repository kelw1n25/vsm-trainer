package ru.vsm.trainer.work

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit
import ru.vsm.trainer.R
import ru.vsm.trainer.core.ApiError
import ru.vsm.trainer.domain.NotificationSync
import ru.vsm.trainer.security.TokenStore

/**
 * Раз в 15 минут (минимум WorkManager), когда есть сеть, забирает новые уведомления backend —
 * новые сценарии, челленджи, сгорающие баллы, ачивки — и показывает их системными уведомлениями.
 */
@HiltWorker
class NotificationWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val sync: NotificationSync,
    private val tokens: TokenStore,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        if (tokens.load() == null) return Result.success()
        return try {
            sync.fresh().forEach(::show)
            Result.success()
        } catch (offline: ApiError.Offline) {
            Result.retry()
        } catch (error: ApiError) {
            Result.success()
        }
    }

    private fun show(item: ru.vsm.trainer.data.remote.dto.AppNotification) {
        val granted = ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (android.os.Build.VERSION.SDK_INT >= 33 && !granted) return
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(item.title)
            .setContentText(item.body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(item.body))
            .setAutoCancel(true)
            .build()
        // id уведомления сервера: повторный показ заменит, а не продублирует
        @Suppress("MissingPermission")
        NotificationManagerCompat.from(applicationContext).notify(item.id, notification)
    }

    companion object {
        const val CHANNEL = "trainer"
        private const val WORK = "notification-sync"

        fun createChannel(context: Context) {
            val channel = NotificationChannel(CHANNEL, context.getString(R.string.notification_channel), NotificationManager.IMPORTANCE_DEFAULT)
            channel.description = context.getString(R.string.notification_channel_description)
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<NotificationWorker>(15, TimeUnit.MINUTES)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(WORK, ExistingPeriodicWorkPolicy.KEEP, request)
        }
    }
}
