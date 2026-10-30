package com.example.services.ai

import android.content.Context
import android.provider.Settings
import android.view.KeyEvent
import com.example.models.LocalCommandResult
import com.example.models.PendingConfirmationAction
import com.example.services.accessibility.HasseenaAccessibilityService
import com.example.services.android.AndroidControlService
import com.example.services.notifications.HasseenaNotificationListenerService
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LocalCommandClassifier(
    private val context: Context,
    private val controlService: AndroidControlService
) {

    fun tryExecuteLocalCommand(rawInput: String): LocalCommandResult {
        val startNs = System.nanoTime()
        val cleaned = rawInput.trim()
        val lower = cleaned.lowercase(Locale.US)
            .removePrefix("hey jarvis ")
            .removePrefix("jarvis ")
            .removeSuffix(".")
            .removeSuffix("!")
            .removeSuffix("?")
            .trim()

        if (lower.isEmpty()) {
            return LocalCommandResult(matched = false)
        }

        fun elapsedMs(): Long = ((System.nanoTime() - startNs) / 1_000_000L).coerceAtLeast(1L)

        // 1. FLASHLIGHT COMMANDS
        if (lower in listOf("turn on flashlight", "flashlight on", "torch on", "turn on the flashlight", "enable flashlight", "switch on flashlight")) {
            val res = controlService.setFlashlight(true)
            return LocalCommandResult(
                matched = true,
                toolName = "Flashlight Control",
                spokenResponse = res.spokenText,
                detailSummary = res.detailText,
                executionLatencyMs = elapsedMs(),
                isError = !res.success
            )
        }
        if (lower in listOf("turn off flashlight", "flashlight off", "torch off", "turn off the flashlight", "disable flashlight", "switch off flashlight")) {
            val res = controlService.setFlashlight(false)
            return LocalCommandResult(
                matched = true,
                toolName = "Flashlight Control",
                spokenResponse = res.spokenText,
                detailSummary = res.detailText,
                executionLatencyMs = elapsedMs(),
                isError = !res.success
            )
        }

        // 2. VOLUME COMMANDS
        val setVolMatch = Regex("(?:set\\s+)?volume\\s+(?:to\\s+)?(\\d{1,3})%?").find(lower)
        if (setVolMatch != null) {
            val pct = setVolMatch.groupValues[1].toIntOrNull() ?: 50
            val res = controlService.adjustVolume(AndroidControlService.VolumeDirection.SET_PERCENT, pct)
            return LocalCommandResult(
                matched = true,
                toolName = "Volume Control",
                spokenResponse = res.spokenText,
                detailSummary = res.detailText,
                executionLatencyMs = elapsedMs(),
                isError = !res.success
            )
        }
        if (lower in listOf("increase volume", "volume up", "turn up volume", "louder", "raise volume", "turn up the volume")) {
            val res = controlService.adjustVolume(AndroidControlService.VolumeDirection.UP)
            return LocalCommandResult(
                matched = true,
                toolName = "Volume Control",
                spokenResponse = res.spokenText,
                detailSummary = res.detailText,
                executionLatencyMs = elapsedMs(),
                isError = !res.success
            )
        }
        if (lower in listOf("decrease volume", "volume down", "turn down volume", "quieter", "lower volume", "turn down the volume")) {
            val res = controlService.adjustVolume(AndroidControlService.VolumeDirection.DOWN)
            return LocalCommandResult(
                matched = true,
                toolName = "Volume Control",
                spokenResponse = res.spokenText,
                detailSummary = res.detailText,
                executionLatencyMs = elapsedMs(),
                isError = !res.success
            )
        }
        if (lower in listOf("mute", "mute volume", "silence volume")) {
            val res = controlService.adjustVolume(AndroidControlService.VolumeDirection.MUTE)
            return LocalCommandResult(
                matched = true,
                toolName = "Volume Control",
                spokenResponse = res.spokenText,
                detailSummary = res.detailText,
                executionLatencyMs = elapsedMs(),
                isError = !res.success
            )
        }
        if (lower in listOf("max volume", "maximum volume", "full volume")) {
            val res = controlService.adjustVolume(AndroidControlService.VolumeDirection.MAX)
            return LocalCommandResult(
                matched = true,
                toolName = "Volume Control",
                spokenResponse = res.spokenText,
                detailSummary = res.detailText,
                executionLatencyMs = elapsedMs(),
                isError = !res.success
            )
        }

        // 3. BRIGHTNESS COMMANDS
        val brightnessMatch = Regex("(?:set\\s+)?brightness\\s+(?:to\\s+)?(\\d{1,3})%?").find(lower)
        if (brightnessMatch != null) {
            val pct = brightnessMatch.groupValues[1].toIntOrNull() ?: 50
            val res = controlService.setScreenBrightnessPercent(pct)
            return LocalCommandResult(
                matched = true,
                toolName = "Brightness Control",
                spokenResponse = res.spokenText,
                detailSummary = res.detailText,
                executionLatencyMs = elapsedMs(),
                isError = !res.success
            )
        }
        if (lower in listOf("increase brightness", "brightness up", "brighten screen", "maximum brightness")) {
            val res = controlService.setScreenBrightnessPercent(90)
            return LocalCommandResult(
                matched = true,
                toolName = "Brightness Control",
                spokenResponse = res.spokenText,
                detailSummary = res.detailText,
                executionLatencyMs = elapsedMs()
            )
        }
        if (lower in listOf("decrease brightness", "brightness down", "dim screen", "low brightness")) {
            val res = controlService.setScreenBrightnessPercent(25)
            return LocalCommandResult(
                matched = true,
                toolName = "Brightness Control",
                spokenResponse = res.spokenText,
                detailSummary = res.detailText,
                executionLatencyMs = elapsedMs()
            )
        }

        // 4. WI-FI / BLUETOOTH / SETTINGS COMMANDS
        if (lower in listOf("turn wifi settings on", "turn wi-fi settings on", "open wifi settings", "open wi-fi settings", "wifi settings", "wi-fi settings", "turn on wifi", "turn on wi-fi", "turn off wifi", "turn off wi-fi")) {
            val res = controlService.openWifiControlPanel()
            return LocalCommandResult(
                matched = true,
                toolName = "Wi-Fi Control Panel",
                spokenResponse = "Opening Wi-Fi settings.",
                detailSummary = res.detailText,
                executionLatencyMs = elapsedMs()
            )
        }
        if (lower in listOf("bluetooth settings", "open bluetooth settings", "turn on bluetooth", "turn off bluetooth", "open bluetooth")) {
            val res = controlService.openSystemSettingsScreen(Settings.ACTION_BLUETOOTH_SETTINGS)
            return LocalCommandResult(
                matched = true,
                toolName = "Bluetooth Settings",
                spokenResponse = "Opening Bluetooth settings.",
                detailSummary = res.detailText,
                executionLatencyMs = elapsedMs()
            )
        }
        if (lower in listOf("open settings", "system settings", "android settings", "settings")) {
            val res = controlService.openSystemSettingsScreen(Settings.ACTION_SETTINGS)
            return LocalCommandResult(
                matched = true,
                toolName = "System Settings",
                spokenResponse = "Opening Settings.",
                detailSummary = res.detailText,
                executionLatencyMs = elapsedMs()
            )
        }

        // 5. BATTERY & SYSTEM TELEMETRY COMMANDS
        if (lower in listOf("show battery", "battery status", "battery level", "battery", "what is my battery", "how much battery do i have", "check battery")) {
            val telemetry = controlService.getDeviceTelemetry()
            val chargingStr = if (telemetry.isCharging) "charging" else "on battery power"
            return LocalCommandResult(
                matched = true,
                toolName = "Battery Telemetry",
                spokenResponse = "Battery is at ${telemetry.batteryPercent} percent and $chargingStr.",
                detailSummary = "Battery: ${telemetry.batteryPercent}% (${chargingStr.uppercase()}) • Temp: ${telemetry.batteryTempCelsius}°C",
                executionLatencyMs = elapsedMs()
            )
        }
        if (lower in listOf("device info", "system status", "phone status", "storage status", "ram status", "network status")) {
            val t = controlService.getDeviceTelemetry()
            return LocalCommandResult(
                matched = true,
                toolName = "System Telemetry",
                spokenResponse = "Running on ${t.deviceModel}, ${t.androidVersion}. Battery is ${t.batteryPercent} percent, ${t.networkTypeLabel}, with ${t.freeStorageGb} gigabytes free storage.",
                detailSummary = "${t.deviceModel} • ${t.androidVersion} • RAM: ${t.availableRamMb}/${t.totalRamMb} MB free • Free Storage: ${t.freeStorageGb} GB • Net: ${t.networkTypeLabel}",
                executionLatencyMs = elapsedMs()
            )
        }

        // 6. MEDIA PLAYBACK COMMANDS
        if (lower in listOf("play music", "pause music", "resume music", "toggle media", "play pause")) {
            val res = controlService.sendMediaKey(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, "Toggled media playback")
            return LocalCommandResult(
                matched = true,
                toolName = "Media Control",
                spokenResponse = res.spokenText,
                detailSummary = res.detailText,
                executionLatencyMs = elapsedMs()
            )
        }
        if (lower in listOf("next track", "next song", "skip song", "skip track")) {
            val res = controlService.sendMediaKey(KeyEvent.KEYCODE_MEDIA_NEXT, "Skipped to next track")
            return LocalCommandResult(
                matched = true,
                toolName = "Media Control",
                spokenResponse = res.spokenText,
                detailSummary = res.detailText,
                executionLatencyMs = elapsedMs()
            )
        }
        if (lower in listOf("previous track", "previous song")) {
            val res = controlService.sendMediaKey(KeyEvent.KEYCODE_MEDIA_PREVIOUS, "Returned to previous track")
            return LocalCommandResult(
                matched = true,
                toolName = "Media Control",
                spokenResponse = res.spokenText,
                detailSummary = res.detailText,
                executionLatencyMs = elapsedMs()
            )
        }

        // 7. ACCESSIBILITY & NAVIGATION COMMANDS ("Take screenshot", "Go home", "Go back", "Scroll down", "Read screen")
        if (lower in listOf("take screenshot", "take a screenshot", "capture screen", "screenshot")) {
            val a11y = HasseenaAccessibilityService.instance
            return if (a11y != null) {
                val ok = a11y.takeScreenshotAction()
                LocalCommandResult(
                    matched = true,
                    toolName = "Accessibility Screenshot",
                    spokenResponse = if (ok) "Taking screenshot." else "Screenshot requires Android 11 or higher.",
                    detailSummary = if (ok) "Executed GLOBAL_ACTION_TAKE_SCREENSHOT" else "Screenshot API unavailable",
                    executionLatencyMs = elapsedMs(),
                    isError = !ok
                )
            } else {
                LocalCommandResult(
                    matched = true,
                    toolName = "Screenshot Tool",
                    spokenResponse = "Enable JARVIS Accessibility Service in Permission Center to take voice screenshots.",
                    detailSummary = "Accessibility Service is currently disabled.",
                    executionLatencyMs = elapsedMs(),
                    isError = true,
                    recoveryHint = "Open Permission Center → Enable Accessibility Service"
                )
            }
        }
        if (lower in listOf("go home", "home screen", "navigate home")) {
            val a11y = HasseenaAccessibilityService.instance
            if (a11y != null && a11y.navigateHome()) {
                return LocalCommandResult(
                    matched = true,
                    toolName = "System Navigation",
                    spokenResponse = "Going home.",
                    detailSummary = "Executed GLOBAL_ACTION_HOME",
                    executionLatencyMs = elapsedMs()
                )
            } else {
                val homeIntent = android.content.Intent(android.content.Intent.ACTION_MAIN).apply {
                    addCategory(android.content.Intent.CATEGORY_HOME)
                    flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                }
                runCatching { context.startActivity(homeIntent) }
                return LocalCommandResult(
                    matched = true,
                    toolName = "System Navigation",
                    spokenResponse = "Going home.",
                    detailSummary = "Dispatched CATEGORY_HOME intent",
                    executionLatencyMs = elapsedMs()
                )
            }
        }
        if (lower in listOf("go back", "navigate back")) {
            val a11y = HasseenaAccessibilityService.instance
            return if (a11y != null && a11y.navigateBack()) {
                LocalCommandResult(
                    matched = true,
                    toolName = "Accessibility Navigation",
                    spokenResponse = "Going back.",
                    detailSummary = "Executed GLOBAL_ACTION_BACK",
                    executionLatencyMs = elapsedMs()
                )
            } else {
                LocalCommandResult(
                    matched = true,
                    toolName = "Accessibility Navigation",
                    spokenResponse = "Enable JARVIS Accessibility Service to perform system-wide back actions.",
                    detailSummary = "Accessibility Service not enabled",
                    executionLatencyMs = elapsedMs(),
                    isError = true,
                    recoveryHint = "Open Permission Center → Accessibility"
                )
            }
        }
        if (lower in listOf("scroll down", "scroll forward")) {
            val a11y = HasseenaAccessibilityService.instance
            return if (a11y != null) {
                val ok = a11y.scrollScreen(forward = true)
                LocalCommandResult(
                    matched = true,
                    toolName = "Accessibility Scroll",
                    spokenResponse = if (ok) "Scrolling down." else "No scrollable area detected on screen.",
                    detailSummary = "Accessibility scroll forward: $ok",
                    executionLatencyMs = elapsedMs()
                )
            } else {
                LocalCommandResult(
                    matched = true,
                    toolName = "Accessibility Scroll",
                    spokenResponse = "Enable JARVIS Accessibility Service to scroll screens by voice.",
                    detailSummary = "Accessibility Service disabled",
                    executionLatencyMs = elapsedMs(),
                    isError = true,
                    recoveryHint = "Open Permission Center → Accessibility"
                )
            }
        }
        if (lower in listOf("scroll up", "scroll backward")) {
            val a11y = HasseenaAccessibilityService.instance
            return if (a11y != null) {
                val ok = a11y.scrollScreen(forward = false)
                LocalCommandResult(
                    matched = true,
                    toolName = "Accessibility Scroll",
                    spokenResponse = if (ok) "Scrolling up." else "No scrollable area detected on screen.",
                    detailSummary = "Accessibility scroll backward: $ok",
                    executionLatencyMs = elapsedMs()
                )
            } else {
                LocalCommandResult(
                    matched = true,
                    toolName = "Accessibility Scroll",
                    spokenResponse = "Enable JARVIS Accessibility Service to scroll screens by voice.",
                    detailSummary = "Accessibility Service disabled",
                    executionLatencyMs = elapsedMs(),
                    isError = true,
                    recoveryHint = "Open Permission Center → Accessibility"
                )
            }
        }
        if (lower in listOf("read screen", "read my screen", "what is on my screen", "whats on my screen")) {
            val a11y = HasseenaAccessibilityService.instance
            return if (a11y != null) {
                val snap = a11y.readVisibleScreenContent()
                val preview = snap.visibleTexts.take(8).joinToString(", ")
                LocalCommandResult(
                    matched = true,
                    toolName = "Screen Reader",
                    spokenResponse = if (preview.isNotBlank()) "On screen: $preview" else "No readable text found on the active screen.",
                    detailSummary = snap.summary,
                    executionLatencyMs = elapsedMs()
                )
            } else {
                LocalCommandResult(
                    matched = true,
                    toolName = "Screen Reader",
                    spokenResponse = "Please enable JARVIS Accessibility Service in the Permission Center so I can read on-screen text.",
                    detailSummary = "Accessibility Service is currently disabled.",
                    executionLatencyMs = elapsedMs(),
                    isError = true,
                    recoveryHint = "Enable Accessibility Service in Permission Center"
                )
            }
        }

        // 8. NOTIFICATIONS READING COMMANDS
        if (lower in listOf("read notifications", "check notifications", "check my notifications", "read my notifications", "any notifications")) {
            return if (HasseenaNotificationListenerService.isNotificationAccessEnabled(context)) {
                HasseenaNotificationListenerService.instance?.refreshActiveNotifications()
                val items = HasseenaNotificationListenerService.notifications.value
                if (items.isEmpty()) {
                    LocalCommandResult(
                        matched = true,
                        toolName = "Notification Reader",
                        spokenResponse = "You have no active notifications.",
                        detailSummary = "0 active notifications in status bar.",
                        executionLatencyMs = elapsedMs()
                    )
                } else {
                    val top = items.take(3).joinToString(". ") { "${it.appName}: ${it.title} ${it.text}" }
                    LocalCommandResult(
                        matched = true,
                        toolName = "Notification Reader",
                        spokenResponse = "You have ${items.size} notifications. $top",
                        detailSummary = items.take(6).joinToString("\n") { "• [${it.appName}] ${it.title}: ${it.text}" },
                        executionLatencyMs = elapsedMs()
                    )
                }
            } else {
                LocalCommandResult(
                    matched = true,
                    toolName = "Notification Reader",
                    spokenResponse = "Grant Notification Access in JARVIS Permission Center so I can read your notifications.",
                    detailSummary = "Notification Listener permission not granted.",
                    executionLatencyMs = elapsedMs(),
                    isError = true,
                    recoveryHint = "Open Permission Center → Notification Access"
                )
            }
        }

        // 9. TIMER & ALARM COMMANDS
        val timerMatch = Regex("set\\s+(?:a\\s+)?timer\\s+for\\s+(\\d+)\\s*(second|seconds|sec|minute|minutes|min|hour|hours)").find(lower)
        if (timerMatch != null) {
            val amount = timerMatch.groupValues[1].toIntOrNull() ?: 1
            val unit = timerMatch.groupValues[2]
            val seconds = when {
                unit.startsWith("hour") -> amount * 3600
                unit.startsWith("min") -> amount * 60
                else -> amount
            }
            val res = controlService.setTimer(seconds)
            return LocalCommandResult(
                matched = true,
                toolName = "Timer Integration",
                spokenResponse = res.spokenText,
                detailSummary = res.detailText,
                executionLatencyMs = elapsedMs(),
                isError = !res.success
            )
        }

        val alarmMatch = Regex("set\\s+(?:an\\s+)?alarm\\s+for\\s+(\\d{1,2})(?::(\\d{2}))?\\s*(am|pm)?").find(lower)
        if (alarmMatch != null) {
            var hour = alarmMatch.groupValues[1].toIntOrNull() ?: 7
            val minute = alarmMatch.groupValues[2].toIntOrNull() ?: 0
            val amPm = alarmMatch.groupValues[3]
            if (amPm == "pm" && hour < 12) hour += 12
            if (amPm == "am" && hour == 12) hour = 0
            val res = controlService.setAlarm(hour, minute)
            return LocalCommandResult(
                matched = true,
                toolName = "Alarm Integration",
                spokenResponse = res.spokenText,
                detailSummary = res.detailText,
                executionLatencyMs = elapsedMs(),
                isError = !res.success
            )
        }

        // 10. SENSITIVE COMMUNICATION ACTIONS (Require User Confirmation!)
        if (lower.startsWith("call ") && lower.length > 5) {
            val rawTarget = cleaned.substring(5).trim()
            val (displayName, phone) = controlService.resolveContactPhoneNumber(rawTarget)
            return LocalCommandResult(
                matched = true,
                toolName = "Phone Call (Confirmation Required)",
                spokenResponse = "Call $displayName?",
                detailSummary = "Confirmation requested before calling $displayName ($phone)",
                executionLatencyMs = elapsedMs(),
                requiresConfirmation = true,
                pendingConfirmation = PendingConfirmationAction.PlacePhoneCall(
                    contactNameOrNumber = displayName,
                    phoneNumber = phone
                )
            )
        }

        val smsMatch = Regex("(?:send\\s+message\\s+to|text|message)\\s+([^:]+?)\\s+(?:saying|that|:)\\s+(.+)", RegexOption.IGNORE_CASE).find(cleaned)
        if (smsMatch != null) {
            val rawRecipient = smsMatch.groupValues[1].trim()
            val body = smsMatch.groupValues[2].trim()
            val (_, resolvedNumber) = controlService.resolveContactPhoneNumber(rawRecipient)
            return LocalCommandResult(
                matched = true,
                toolName = "SMS Composer (Confirmation Required)",
                spokenResponse = "Send this message to $rawRecipient?",
                detailSummary = "Confirmation requested before messaging $rawRecipient ($resolvedNumber)",
                executionLatencyMs = elapsedMs(),
                requiresConfirmation = true,
                pendingConfirmation = PendingConfirmationAction.SendSmsMessage(
                    recipient = resolvedNumber,
                    messageBody = body
                )
            )
        }

        // 11. LOCAL TIME & DATE COMMANDS
        if (lower in listOf("what time is it", "current time", "tell me the time", "time")) {
            val timeStr = SimpleDateFormat("h:mm a", Locale.US).format(Date())
            return LocalCommandResult(
                matched = true,
                toolName = "System Clock",
                spokenResponse = "It is $timeStr.",
                detailSummary = "Local system time: $timeStr",
                executionLatencyMs = elapsedMs()
            )
        }
        if (lower in listOf("what is todays date", "what is today's date", "todays date", "what day is it", "date")) {
            val dateStr = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.US).format(Date())
            return LocalCommandResult(
                matched = true,
                toolName = "System Calendar",
                spokenResponse = "Today is $dateStr.",
                detailSummary = "Local system date: $dateStr",
                executionLatencyMs = elapsedMs()
            )
        }

        // 12. APP LAUNCH COMMANDS ("Open YouTube", "Open WhatsApp", "Open Chrome", "Open camera")
        if ((lower.startsWith("open ") || lower.startsWith("launch ")) &&
            !lower.contains(" and ") &&
            !lower.contains("?") &&
            lower.split(" ").size <= 4
        ) {
            val outcome = controlService.appLauncherService.launchAppByNameOrAlias(lower)
            return LocalCommandResult(
                matched = true,
                toolName = "AppLauncherService",
                spokenResponse = outcome.spokenResponse,
                detailSummary = outcome.detailMessage,
                executionLatencyMs = elapsedMs(),
                isError = !outcome.launched,
                recoveryHint = if (!outcome.launched) "Verify the app is installed in Control Center → Installed Apps" else null
            )
        }

        return LocalCommandResult(matched = false)
    }
}
