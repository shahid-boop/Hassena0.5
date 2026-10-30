package com.example.services.android

import android.Manifest
import android.app.ActivityManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.AlarmClock
import android.provider.CalendarContract
import android.provider.ContactsContract
import android.provider.Settings
import android.view.KeyEvent
import androidx.core.content.ContextCompat
import com.example.models.DeviceTelemetrySnapshot
import com.example.models.PermissionStatusItem
import com.example.services.accessibility.HasseenaAccessibilityService
import com.example.services.notifications.HasseenaNotificationListenerService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.roundToInt

class AndroidControlService(
    private val context: Context,
    val appLauncherService: AppLauncherService
) {

    private val _isFlashlightOn = MutableStateFlow(false)
    val isFlashlightOn: StateFlow<Boolean> = _isFlashlightOn.asStateFlow()

    private val cameraManager: CameraManager? =
        context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager

    private val audioManager: AudioManager =
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private val torchCallback = object : CameraManager.TorchCallback() {
        override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
            _isFlashlightOn.value = enabled
        }
    }

    init {
        try {
            cameraManager?.registerTorchCallback(torchCallback, null)
        } catch (_: Exception) {
        }
    }

    // =========================================================================
    // 1. FLASHLIGHT CONTROL
    // =========================================================================
    fun setFlashlight(enable: Boolean): ToolOutcome {
        val cm = cameraManager ?: return ToolOutcome(
            success = false,
            spokenText = "Camera hardware service is unavailable.",
            detailText = "CameraManager not present on this device."
        )
        return try {
            val torchCameraId = cm.cameraIdList.firstOrNull { id ->
                cm.getCameraCharacteristics(id).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
            if (torchCameraId == null) {
                ToolOutcome(
                    success = false,
                    spokenText = "This device does not have a hardware flashlight.",
                    detailText = "FLASH_INFO_AVAILABLE is false on all cameras (common on emulators)."
                )
            } else {
                cm.setTorchMode(torchCameraId, enable)
                _isFlashlightOn.value = enable
                triggerHapticPulse()
                val stateWord = if (enable) "on" else "off"
                ToolOutcome(
                    success = true,
                    spokenText = "Flashlight turned $stateWord.",
                    detailText = "Camera torch ($torchCameraId) set to ${stateWord.uppercase()}"
                )
            }
        } catch (e: Exception) {
            ToolOutcome(
                success = false,
                spokenText = "Could not toggle flashlight.",
                detailText = "Error: ${e.localizedMessage ?: "Torch busy or unavailable"}"
            )
        }
    }

    // =========================================================================
    // 2. VOLUME CONTROL
    // =========================================================================
    fun adjustVolume(direction: VolumeDirection, targetPercent: Int? = null): ToolOutcome {
        return try {
            val stream = AudioManager.STREAM_MUSIC
            val maxVol = audioManager.getStreamMaxVolume(stream).coerceAtLeast(1)
            val currentVol = audioManager.getStreamVolume(stream)

            val newVol = when (direction) {
                VolumeDirection.UP -> (currentVol + (maxVol * 0.15f).roundToInt().coerceAtLeast(1)).coerceAtMost(maxVol)
                VolumeDirection.DOWN -> (currentVol - (maxVol * 0.15f).roundToInt().coerceAtLeast(1)).coerceAtLeast(0)
                VolumeDirection.MUTE -> 0
                VolumeDirection.MAX -> maxVol
                VolumeDirection.SET_PERCENT -> {
                    val pct = (targetPercent ?: 50).coerceIn(0, 100)
                    ((pct / 100f) * maxVol).roundToInt().coerceIn(0, maxVol)
                }
            }

            audioManager.setStreamVolume(stream, newVol, AudioManager.FLAG_SHOW_UI)
            val resultingPercent = ((newVol.toFloat() / maxVol) * 100f).roundToInt()
            triggerHapticPulse()
            ToolOutcome(
                success = true,
                spokenText = "Volume set to $resultingPercent percent.",
                detailText = "Media stream volume updated to $newVol / $maxVol ($resultingPercent%)"
            )
        } catch (e: SecurityException) {
            ToolOutcome(
                success = false,
                spokenText = "Do Not Disturb restricts volume changes.",
                detailText = "SecurityException: ${e.localizedMessage}"
            )
        } catch (e: Exception) {
            ToolOutcome(
                success = false,
                spokenText = "Unable to change volume.",
                detailText = e.localizedMessage ?: "AudioManager error"
            )
        }
    }

    enum class VolumeDirection { UP, DOWN, MUTE, MAX, SET_PERCENT }

    // =========================================================================
    // 3. BRIGHTNESS CONTROL
    // =========================================================================
    fun setScreenBrightnessPercent(percent: Int): ToolOutcome {
        val clamped = percent.coerceIn(5, 100)
        val rawValue = ((clamped / 100f) * 255f).roundToInt().coerceIn(10, 255)
        return if (Settings.System.canWrite(context)) {
            try {
                Settings.System.putInt(
                    context.contentResolver,
                    Settings.System.SCREEN_BRIGHTNESS_MODE,
                    Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
                )
                Settings.System.putInt(
                    context.contentResolver,
                    Settings.System.SCREEN_BRIGHTNESS,
                    rawValue
                )
                triggerHapticPulse()
                ToolOutcome(
                    success = true,
                    spokenText = "Brightness set to $clamped percent.",
                    detailText = "System SCREEN_BRIGHTNESS updated to $rawValue/255 ($clamped%)"
                )
            } catch (e: Exception) {
                ToolOutcome(
                    success = false,
                    spokenText = "Could not write system brightness.",
                    detailText = e.localizedMessage ?: "Settings write failed"
                )
            }
        } else {
            // Apply in-app brightness preview and offer Write Settings permission
            _requestedAppWindowBrightness.value = clamped / 100f
            ToolOutcome(
                success = true,
                spokenText = "Set assistant brightness to $clamped percent. Grant Modify System Settings in Permission Center for system-wide control.",
                detailText = "Applied window brightness ($clamped%). Enable Write System Settings for global OS brightness."
            )
        }
    }

    private val _requestedAppWindowBrightness = MutableStateFlow<Float?>(null)
    val requestedAppWindowBrightness: StateFlow<Float?> = _requestedAppWindowBrightness.asStateFlow()

    // =========================================================================
    // 4. MEDIA CONTROL
    // =========================================================================
    fun sendMediaKey(keyCode: Int, actionLabel: String): ToolOutcome {
        return try {
            val downEvent = KeyEvent(KeyEvent.ACTION_DOWN, keyCode)
            val upEvent = KeyEvent(KeyEvent.ACTION_UP, keyCode)
            audioManager.dispatchMediaKeyEvent(downEvent)
            audioManager.dispatchMediaKeyEvent(upEvent)
            triggerHapticPulse()
            ToolOutcome(
                success = true,
                spokenText = "$actionLabel.",
                detailText = "Dispatched media key event ($actionLabel)"
            )
        } catch (e: Exception) {
            ToolOutcome(
                success = false,
                spokenText = "Could not control media playback.",
                detailText = e.localizedMessage ?: "AudioManager media key error"
            )
        }
    }

    // =========================================================================
    // 5. HAPTICS / VIBRATION
    // =========================================================================
    fun triggerHapticPulse(durationMs: Long = 35L) {
        try {
            val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
            if (vibrator?.hasVibrator() == true) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(
                        VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE)
                    )
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(durationMs)
                }
            }
        } catch (_: Exception) {
        }
    }

    // =========================================================================
    // 6. BATTERY, NETWORK & DEVICE TELEMETRY
    // =========================================================================
    fun getDeviceTelemetry(): DeviceTelemetrySnapshot {
        val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val batteryPct = if (level >= 0 && scale > 0) ((level * 100f) / scale).roundToInt() else 100
        val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL
        val tempTenths = batteryIntent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 280) ?: 280
        val tempC = tempTenths / 10f

        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val activeNet = cm?.activeNetwork
        val caps = activeNet?.let { cm.getNetworkCapabilities(it) }
        val wifiConnected = caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
        val cellularConnected = caps?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true
        val netLabel = when {
            wifiConnected -> "Wi-Fi Connected"
            cellularConnected -> "Cellular Data"
            caps != null -> "Online"
            else -> "Offline"
        }

        val maxMusic = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        val curMusic = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        val musicPct = ((curMusic.toFloat() / maxMusic) * 100f).roundToInt()

        val maxRing = audioManager.getStreamMaxVolume(AudioManager.STREAM_RING).coerceAtLeast(1)
        val curRing = audioManager.getStreamVolume(AudioManager.STREAM_RING)
        val ringPct = ((curRing.toFloat() / maxRing) * 100f).roundToInt()

        val rawBrightness = try {
            Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS)
        } catch (_: Exception) {
            140
        }
        val brightnessPct = ((rawBrightness / 255f) * 100f).roundToInt().coerceIn(0, 100)

        val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        actManager?.getMemoryInfo(memInfo)
        val availRamMb = memInfo.availMem / (1024 * 1024)
        val totalRamMb = memInfo.totalMem / (1024 * 1024)

        val statFs = StatFs(Environment.getDataDirectory().path)
        val freeBytes = statFs.availableBytes
        val freeGb = ((freeBytes.toDouble() / (1024.0 * 1024.0 * 1024.0)) * 10.0).roundToInt() / 10f

        return DeviceTelemetrySnapshot(
            batteryPercent = batteryPct,
            isCharging = isCharging,
            batteryTempCelsius = tempC,
            wifiConnected = wifiConnected,
            networkTypeLabel = netLabel,
            musicVolumePercent = musicPct,
            ringVolumePercent = ringPct,
            screenBrightnessPercent = brightnessPct,
            availableRamMb = availRamMb,
            totalRamMb = totalRamMb,
            freeStorageGb = freeGb,
            deviceModel = "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL}",
            androidVersion = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
            flashlightOn = _isFlashlightOn.value
        )
    }

    // =========================================================================
    // 7. ALARMS, TIMERS, CALENDAR & CLIPBOARD
    // =========================================================================
    fun setAlarm(hour24: Int, minute: Int, label: String = "JARVIS Alarm"): ToolOutcome {
        return try {
            val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                putExtra(AlarmClock.EXTRA_HOUR, hour24.coerceIn(0, 23))
                putExtra(AlarmClock.EXTRA_MINUTES, minute.coerceIn(0, 59))
                putExtra(AlarmClock.EXTRA_MESSAGE, label)
                putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            val formattedMin = minute.toString().padStart(2, '0')
            ToolOutcome(
                success = true,
                spokenText = "Setting alarm for $hour24:$formattedMin.",
                detailText = "Dispatched AlarmClock.ACTION_SET_ALARM for $hour24:$formattedMin ($label)"
            )
        } catch (e: Exception) {
            ToolOutcome(
                success = false,
                spokenText = "Could not set alarm on this device.",
                detailText = e.localizedMessage ?: "No Clock app handling SET_ALARM"
            )
        }
    }

    fun setTimer(seconds: Int, label: String = "JARVIS Timer"): ToolOutcome {
        return try {
            val intent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
                putExtra(AlarmClock.EXTRA_LENGTH, seconds.coerceAtLeast(1))
                putExtra(AlarmClock.EXTRA_MESSAGE, label)
                putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            val mins = seconds / 60
            val spoken = if (mins > 0 && seconds % 60 == 0) {
                "Starting timer for $mins minutes."
            } else {
                "Starting timer for $seconds seconds."
            }
            ToolOutcome(
                success = true,
                spokenText = spoken,
                detailText = "Dispatched AlarmClock.ACTION_SET_TIMER for ${seconds}s"
            )
        } catch (e: Exception) {
            ToolOutcome(
                success = false,
                spokenText = "Could not start timer.",
                detailText = e.localizedMessage ?: "No Clock app handling SET_TIMER"
            )
        }
    }

    fun createCalendarEvent(title: String, startOffsetMinutes: Int = 60): ToolOutcome {
        return try {
            val startMillis = System.currentTimeMillis() + (startOffsetMinutes * 60_000L)
            val endMillis = startMillis + 3_600_000L
            val intent = Intent(Intent.ACTION_INSERT).apply {
                data = CalendarContract.Events.CONTENT_URI
                putExtra(CalendarContract.Events.TITLE, title)
                putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startMillis)
                putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endMillis)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            ToolOutcome(
                success = true,
                spokenText = "Opening calendar to schedule $title.",
                detailText = "Launched CalendarContract event insertion for '$title'"
            )
        } catch (e: Exception) {
            ToolOutcome(
                success = false,
                spokenText = "Unable to open Calendar event creator.",
                detailText = e.localizedMessage ?: "Calendar app unavailable"
            )
        }
    }

    fun copyToClipboard(label: String, text: String): ToolOutcome {
        return try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
            triggerHapticPulse()
            ToolOutcome(
                success = true,
                spokenText = "Copied to clipboard.",
                detailText = "Copied ${text.length} characters to Android clipboard"
            )
        } catch (e: Exception) {
            ToolOutcome(
                success = false,
                spokenText = "Could not copy to clipboard.",
                detailText = e.localizedMessage ?: "Clipboard error"
            )
        }
    }

    // =========================================================================
    // 8. COMMUNICATION ACTIONS (PHONE, SMS, SHARE, CONTACTS)
    // =========================================================================
    fun resolveContactPhoneNumber(queryNameOrNumber: String): Pair<String, String> {
        val cleaned = queryNameOrNumber.trim()
        if (cleaned.any { it.isDigit() } && cleaned.count { it.isDigit() } >= 3) {
            return cleaned to cleaned
        }
        val hasContactsPerm = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED

        if (hasContactsPerm) {
            try {
                val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
                val projection = arrayOf(
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER
                )
                val selection = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?"
                val args = arrayOf("%$cleaned%")
                context.contentResolver.query(uri, projection, selection, args, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val name = cursor.getString(0) ?: cleaned
                        val number = cursor.getString(1) ?: ""
                        if (number.isNotBlank()) return name to number
                    }
                }
            } catch (_: Exception) {
            }
        }
        return cleaned to cleaned
    }

    fun executeConfirmedPhoneCall(phoneNumber: String): ToolOutcome {
        val hasCallPerm = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CALL_PHONE
        ) == PackageManager.PERMISSION_GRANTED

        return try {
            val action = if (hasCallPerm && phoneNumber.any { it.isDigit() }) {
                Intent.ACTION_CALL
            } else {
                Intent.ACTION_DIAL
            }
            val intent = Intent(action, Uri.parse("tel:${Uri.encode(phoneNumber)}")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            ToolOutcome(
                success = true,
                spokenText = if (action == Intent.ACTION_CALL) "Calling $phoneNumber." else "Opening dialer for $phoneNumber.",
                detailText = "Executed $action for tel:$phoneNumber"
            )
        } catch (e: Exception) {
            ToolOutcome(
                success = false,
                spokenText = "Could not start phone call.",
                detailText = e.localizedMessage ?: "Telephony unavailable"
            )
        }
    }

    fun executeConfirmedSmsCompose(recipient: String, messageBody: String): ToolOutcome {
        return try {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("smsto:${Uri.encode(recipient)}")
                putExtra("sms_body", messageBody)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            ToolOutcome(
                success = true,
                spokenText = "Opening message composer for $recipient.",
                detailText = "Prepared SMS to $recipient: \"$messageBody\""
            )
        } catch (e: Exception) {
            ToolOutcome(
                success = false,
                spokenText = "Could not open messaging app.",
                detailText = e.localizedMessage ?: "SMS intent error"
            )
        }
    }

    fun shareTextContent(text: String, title: String = "Share from JARVIS AI Assistant"): ToolOutcome {
        return try {
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
            }
            val chooser = Intent.createChooser(sendIntent, title).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
            ToolOutcome(
                success = true,
                spokenText = "Opening share sheet.",
                detailText = "Shared content via Android chooser"
            )
        } catch (e: Exception) {
            ToolOutcome(
                success = false,
                spokenText = "Could not open share menu.",
                detailText = e.localizedMessage ?: "Share error"
            )
        }
    }

    // =========================================================================
    // 9. SETTINGS & PERMISSIONS CENTER AUDIT
    // =========================================================================
    fun openSystemSettingsScreen(action: String, packageSpecific: Boolean = false): ToolOutcome {
        return try {
            val intent = Intent(action).apply {
                if (packageSpecific) {
                    data = Uri.parse("package:${context.packageName}")
                }
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            ToolOutcome(
                success = true,
                spokenText = "Opening system settings.",
                detailText = "Opened $action"
            )
        } catch (_: Exception) {
            val fallback = Intent(Settings.ACTION_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            runCatching { context.startActivity(fallback) }
            ToolOutcome(
                success = true,
                spokenText = "Opening Android settings.",
                detailText = "Opened main Settings fallback"
            )
        }
    }

    fun openWifiControlPanel(): ToolOutcome {
        val action = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Settings.Panel.ACTION_WIFI
        } else {
            Settings.ACTION_WIFI_SETTINGS
        }
        return openSystemSettingsScreen(action)
    }

    fun getPermissionsStatusList(): List<PermissionStatusItem> {
        fun isPermGranted(perm: String): Boolean =
            ContextCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_GRANTED

        val list = mutableListOf(
            PermissionStatusItem(
                id = "microphone",
                name = "Microphone (Voice & Wake Word)",
                isGranted = isPermGranted(Manifest.permission.RECORD_AUDIO),
                whyNeeded = "Required for real-time Speech-to-Text, continuous voice conversation, and barge-in interruption.",
                manifestPermission = Manifest.permission.RECORD_AUDIO
            ),
            PermissionStatusItem(
                id = "camera",
                name = "Camera (Visual Lens & OCR)",
                isGranted = isPermGranted(Manifest.permission.CAMERA),
                whyNeeded = "Required for Visual Lens live camera viewfinder, object recognition, and page OCR.",
                manifestPermission = Manifest.permission.CAMERA
            ),
            PermissionStatusItem(
                id = "accessibility",
                name = "Accessibility Service (Screen & UI Control)",
                isGranted = HasseenaAccessibilityService.isAccessibilityServiceEnabled(context),
                whyNeeded = "User-enabled service allowing JARVIS to read visible screen text, scroll, go back, take screenshots, and tap controls on your command.",
                isSpecialAccess = true,
                settingsIntentAction = Settings.ACTION_ACCESSIBILITY_SETTINGS
            ),
            PermissionStatusItem(
                id = "notification_listener",
                name = "Notification Access (Read & Summarize)",
                isGranted = HasseenaNotificationListenerService.isNotificationAccessEnabled(context),
                whyNeeded = "Optional access allowing JARVIS to read and summarize active notifications when you ask.",
                isSpecialAccess = true,
                settingsIntentAction = Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS
            ),
            PermissionStatusItem(
                id = "contacts",
                name = "Contacts Lookup",
                isGranted = isPermGranted(Manifest.permission.READ_CONTACTS),
                whyNeeded = "Resolves contact names like 'Call Mom' or 'Message Ali' to phone numbers before asking for confirmation.",
                manifestPermission = Manifest.permission.READ_CONTACTS
            ),
            PermissionStatusItem(
                id = "phone",
                name = "Phone Calls",
                isGranted = isPermGranted(Manifest.permission.CALL_PHONE),
                whyNeeded = "Places user-confirmed phone calls directly from voice commands.",
                manifestPermission = Manifest.permission.CALL_PHONE
            ),
            PermissionStatusItem(
                id = "calendar",
                name = "Calendar Integration",
                isGranted = isPermGranted(Manifest.permission.READ_CALENDAR),
                whyNeeded = "Allows JARVIS to check and schedule calendar events.",
                manifestPermission = Manifest.permission.READ_CALENDAR
            ),
            PermissionStatusItem(
                id = "write_settings",
                name = "Modify System Settings (Brightness)",
                isGranted = Settings.System.canWrite(context),
                whyNeeded = "Allows JARVIS to change global OS screen brightness directly via voice commands.",
                isSpecialAccess = true,
                settingsIntentAction = Settings.ACTION_MANAGE_WRITE_SETTINGS
            )
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            list.add(
                PermissionStatusItem(
                    id = "post_notifications",
                    name = "Assistant Alerts & Notifications",
                    isGranted = isPermGranted(Manifest.permission.POST_NOTIFICATIONS),
                    whyNeeded = "Displays foreground assistant status and timer reminders.",
                    manifestPermission = Manifest.permission.POST_NOTIFICATIONS
                )
            )
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            list.add(
                PermissionStatusItem(
                    id = "bluetooth",
                    name = "Bluetooth Device Status",
                    isGranted = isPermGranted(Manifest.permission.BLUETOOTH_CONNECT),
                    whyNeeded = "Checks connected Bluetooth audio and accessory status.",
                    manifestPermission = Manifest.permission.BLUETOOTH_CONNECT
                )
            )
        }

        return list
    }

    data class ToolOutcome(
        val success: Boolean,
        val spokenText: String,
        val detailText: String
    )
}
