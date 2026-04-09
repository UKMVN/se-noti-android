package com.senoti.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.senoti.app.MainActivity
import com.senoti.app.R
import com.senoti.app.SeNotiApplication
import com.senoti.app.data.NotificationEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class NotificationListenerService : NotificationListenerService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        private const val TAG = "NotiListener"
        private const val FOREGROUND_CHANNEL_ID = "se_noti_foreground"
        private const val FOREGROUND_NOTIFICATION_ID = 1001
        private const val PUBLISH_LOG_RETENTION_MS = 7L * 24 * 60 * 60 * 1000
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d(TAG, "Notification listener connected")
        startForegroundNotification()
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        Log.d(TAG, "Notification listener disconnected")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        sbn ?: return

        // Skip our own notifications to avoid loops
        if (sbn.packageName == packageName) return

        val notification = sbn.notification ?: return
        val extras = notification.extras

        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
        val subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()

        // Skip empty notifications
        if (title.isBlank() && text.isBlank()) return

        val appName = getAppName(sbn.packageName)
        val sourcePackageName = sbn.packageName
        val notificationKey = sbn.key
        val isClearable = sbn.isClearable

        val entity = NotificationEntity(
            packageName = sourcePackageName,
            appName = appName,
            title = title,
            text = text,
            subText = subText,
            timestamp = sbn.postTime
        )

        val app = application as? SeNotiApplication ?: return

        scope.launch {
            val settings = app.settingsRepository.getCurrentSettings()

            // Keep only notifications from the last 3 days
            try {
                app.repository.deleteExpiredNotifications()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to clean up old notifications", e)
            }

            // Save to local database
            try {
                app.repository.insert(entity)
                Log.d(TAG, "Saved notification from $sourcePackageName: $title")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save notification", e)
            }

            // Push to Ably if enabled and save log
            try {
                if (settings.isEnabled) {
                    val result = AblyPublisher.publish(entity, settings)
                    // Save publish log
                    val logEntry = AblyPublisher.createLogEntry(result, entity, settings)
                    val publishLogDao = app.database.publishLogDao()
                    val cutoff = System.currentTimeMillis() - PUBLISH_LOG_RETENTION_MS
                    publishLogDao.deleteOlderThan(cutoff)
                    publishLogDao.insert(logEntry)

                    if (!result.isSuccess) {
                        Log.e(TAG, "Ably publish failed: ${result.errorMessage}")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error during Ably push", e)
            }

            // Push to custom API if enabled and save log
            try {
                if (settings.pushApiEnabled) {
                    val apiResult = ApiPublisher.publish(entity, settings)
                    val apiLogEntry = ApiPublisher.createLogEntry(apiResult, entity)
                    val publishLogDao = app.database.publishLogDao()
                    val cutoff = System.currentTimeMillis() - PUBLISH_LOG_RETENTION_MS
                    publishLogDao.deleteOlderThan(cutoff)
                    publishLogDao.insert(apiLogEntry)

                    if (!apiResult.isSuccess) {
                        Log.e(TAG, "Custom API push failed: ${apiResult.errorMessage}")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error during custom API push", e)
            }

            if (isClearable) {
                if (settings.autoDeleteImmediately) {
                    try {
                        cancelNotification(notificationKey)
                        Log.d(TAG, "Auto-deleted notification immediately from $sourcePackageName: $title")
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to auto-delete notification immediately", e)
                    }
                } else if (settings.autoDeleteMinutes > 0) {
                    scheduleAutoDelete(
                        notificationKey = notificationKey,
                        packageName = sourcePackageName,
                        title = title,
                        delayMinutes = settings.autoDeleteMinutes
                    )
                }
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        Log.d(TAG, "Notification removed from ${sbn?.packageName}")
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

    private fun startForegroundNotification() {
        try {
            createNotificationChannel()

            val pendingIntent = PendingIntent.getActivity(
                this, 0,
                Intent(this, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(this, FOREGROUND_CHANNEL_ID)
                .setContentTitle("SE Noti is running")
                .setContentText("Listening for notifications...")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setOngoing(true)
                .setSilent(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setContentIntent(pendingIntent)
                .build()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                ServiceCompat.startForeground(
                    this,
                    FOREGROUND_NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                )
            } else {
                startForeground(FOREGROUND_NOTIFICATION_ID, notification)
            }

            Log.d(TAG, "Foreground notification started")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start foreground notification", e)
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            FOREGROUND_CHANNEL_ID,
            "Background Service",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Indicates the app is listening for notifications"
            setShowBadge(false)
        }

        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun getAppName(packageName: String): String {
        return try {
            val pm = applicationContext.packageManager
            val appInfo = pm.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
            pm.getApplicationLabel(appInfo).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            packageName
        }
    }

    private fun scheduleAutoDelete(
        notificationKey: String,
        packageName: String,
        title: String,
        delayMinutes: Int
    ) {
        scope.launch {
            try {
                delay(delayMinutes * 60_000L)
                cancelNotification(notificationKey)
                Log.d(TAG, "Auto-deleted notification from $packageName after $delayMinutes minutes: $title")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to auto-delete notification", e)
            }
        }
    }
}
