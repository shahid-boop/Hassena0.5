package com.example.services.notifications

import android.app.Notification
import android.content.Context
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.example.models.CapturedNotification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class HasseenaNotificationListenerService : NotificationListenerService() {

    override fun onListenerConnected() {
        super.onListenerConnected()
        instance = this
        _isConnected.value = true
        refreshActiveNotifications()
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        if (instance === this) {
            instance = null
            _isConnected.value = false
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        refreshActiveNotifications()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        refreshActiveNotifications()
    }

    fun refreshActiveNotifications() {
        try {
            val rawList = activeNotifications ?: emptyArray()
            val mapped = rawList.mapNotNull { sbn ->
                val extras = sbn.notification?.extras ?: return@mapNotNull null
                val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim().orEmpty()
                val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.trim().orEmpty()
                if (title.isBlank() && text.isBlank()) return@mapNotNull null
                val appLabel = try {
                    val appInfo = packageManager.getApplicationInfo(sbn.packageName, 0)
                    packageManager.getApplicationLabel(appInfo).toString()
                } catch (_: Exception) {
                    sbn.packageName.substringAfterLast('.')
                }
                CapturedNotification(
                    key = sbn.key ?: "${sbn.packageName}_${sbn.postTime}",
                    packageName = sbn.packageName,
                    appName = appLabel,
                    title = title.ifBlank { appLabel },
                    text = text,
                    timestamp = sbn.postTime
                )
            }.sortedByDescending { it.timestamp }.take(25)

            _notifications.value = mapped
        } catch (_: Exception) {
            // Listener may not be bound yet
        }
    }

    companion object {
        @Volatile
        var instance: HasseenaNotificationListenerService? = null
            private set

        private val _isConnected = MutableStateFlow(false)
        val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

        private val _notifications = MutableStateFlow<List<CapturedNotification>>(emptyList())
        val notifications: StateFlow<List<CapturedNotification>> = _notifications.asStateFlow()

        fun isNotificationAccessEnabled(context: Context): Boolean {
            if (_isConnected.value && instance != null) return true
            val flat = Settings.Secure.getString(
                context.contentResolver,
                "enabled_notification_listeners"
            ) ?: return false
            return flat.contains(context.packageName, ignoreCase = true)
        }
    }
}
