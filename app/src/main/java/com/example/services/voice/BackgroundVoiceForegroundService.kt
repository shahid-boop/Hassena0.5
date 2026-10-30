package com.example.services.voice

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R

class BackgroundVoiceForegroundService : Service() {

    companion object {
        const val ACTION_START = "com.example.vxgfvh.voice.START"
        const val ACTION_STOP = "com.example.vxgfvh.voice.STOP"
        const val ACTION_STOP_REQUESTED = "com.example.vxgfvh.voice.STOP_REQUESTED"
        const val CHANNEL_ID = "hasseena_background_voice"
        const val NOTIFICATION_ID = 7401
    }

    override fun onCreate() {
        super.onCreate()
        val manager = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    "JARVIS background voice",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Visible status while JARVIS voice listening is enabled"
                    setShowBadge(false)
                }
            )
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                sendBroadcast(Intent(ACTION_STOP_REQUESTED).setPackage(packageName))
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_START, null -> {
                val notification = buildNotification()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    startForeground(
                        NOTIFICATION_ID,
                        notification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                    )
                } else {
                    startForeground(NOTIFICATION_ID, notification)
                }
            }
            else -> return START_NOT_STICKY
        }
        return START_NOT_STICKY
    }

    private fun buildNotification(): Notification {
        val openApp = PendingIntent.getActivity(
            this, 1,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val stop = PendingIntent.getService(
            this, 2,
            Intent(this, BackgroundVoiceForegroundService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("JARVIS AI — Voice listening ON")
            .setContentText("Microphone service active. Tap Stop to turn listening off.")
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setContentIntent(openApp)
            .addAction(android.R.drawable.ic_media_pause, "Stop listening", stop)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
