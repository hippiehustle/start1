package com.kaos.evcryptoscanner.data.fcm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.kaos.evcryptoscanner.MainActivity
import com.kaos.evcryptoscanner.R
import com.kaos.evcryptoscanner.data.local.PreferencesManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class KaosFCMService : FirebaseMessagingService() {

    @Inject
    lateinit var preferencesManager: PreferencesManager

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        const val CHANNEL_TRADE_ALERTS = "trade_alerts"
        const val CHANNEL_SETUP_ALERTS = "setup_alerts"
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Timber.d("New FCM token: $token")
        serviceScope.launch {
            preferencesManager.setFcmToken(token)
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        Timber.d("FCM message received: ${message.data}")

        val title = message.notification?.title ?: message.data["title"] ?: "Kaos Scanner"
        val body = message.notification?.body ?: message.data["body"] ?: "New scan available"
        val state = message.data["state"]
        val readiness = message.data["readiness"]?.toIntOrNull()

        val channelId = when {
            state == "BUY" -> CHANNEL_TRADE_ALERTS
            readiness != null && readiness >= 70 -> CHANNEL_SETUP_ALERTS
            else -> CHANNEL_TRADE_ALERTS
        }

        showNotification(title, body, channelId)
    }

    private fun showNotification(title: String, body: String, channelId: String) {
        createNotificationChannels()

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("from_notification", true)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val tradeChannel = NotificationChannel(
                CHANNEL_TRADE_ALERTS,
                getString(R.string.notification_channel_trade_alerts),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts for BUY signals"
            }

            val setupChannel = NotificationChannel(
                CHANNEL_SETUP_ALERTS,
                getString(R.string.notification_channel_setup_alerts),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Alerts for high-readiness setups"
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(tradeChannel)
            notificationManager.createNotificationChannel(setupChannel)
        }
    }
}
