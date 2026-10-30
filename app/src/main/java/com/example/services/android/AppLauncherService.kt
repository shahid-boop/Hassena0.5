package com.example.services.android

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.AlarmClock
import android.provider.CalendarContract
import android.provider.MediaStore
import android.provider.Settings
import com.example.models.InstalledAppInfo
import java.util.Locale

class AppLauncherService(private val context: Context) {

    data class AppLaunchOutcome(
        val launched: Boolean,
        val spokenResponse: String,
        val detailMessage: String,
        val packageName: String? = null
    )

    private var cachedApps: List<InstalledAppInfo> = emptyList()

    private val commonAppPackages = mapOf(
        "youtube" to listOf("com.google.android.youtube", "com.google.android.apps.youtube.mango"),
        "whatsapp" to listOf("com.whatsapp", "com.whatsapp.w4b"),
        "chrome" to listOf("com.android.chrome", "org.chromium.chrome"),
        "browser" to listOf("com.android.chrome", "org.mozilla.firefox"),
        "gmail" to listOf("com.google.android.gm"),
        "maps" to listOf("com.google.android.apps.maps"),
        "google maps" to listOf("com.google.android.apps.maps"),
        "spotify" to listOf("com.spotify.music"),
        "play store" to listOf("com.android.vending"),
        "photos" to listOf("com.google.android.apps.photos"),
        "files" to listOf("com.google.android.apps.nbu.files", "com.android.documentsui"),
        "calculator" to listOf("com.google.android.calculator", "com.android.calculator2"),
        "calendar" to listOf("com.google.android.calendar", "com.android.calendar"),
        "clock" to listOf("com.google.android.deskclock", "com.android.deskclock"),
        "contacts" to listOf("com.google.android.contacts", "com.android.contacts"),
        "messages" to listOf("com.google.android.apps.messaging", "com.android.mms"),
        "phone" to listOf("com.google.android.dialer", "com.android.dialer"),
        "instagram" to listOf("com.instagram.android"),
        "telegram" to listOf("org.telegram.messenger"),
        "netflix" to listOf("com.netflix.mediaclient")
    )

    fun refreshInstalledApps(): List<InstalledAppInfo> {
        val pm = context.packageManager
        val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfos = try {
            pm.queryIntentActivities(mainIntent, PackageManager.MATCH_ALL)
        } catch (_: Exception) {
            emptyList()
        }

        val apps = resolveInfos.mapNotNull { info ->
            val pkg = info.activityInfo?.packageName ?: return@mapNotNull null
            if (pkg == context.packageName) return@mapNotNull null
            val label = info.loadLabel(pm)?.toString()?.trim().orEmpty()
            if (label.isBlank()) return@mapNotNull null
            val isSystem = (info.activityInfo.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            InstalledAppInfo(
                appName = label,
                packageName = pkg,
                isSystemApp = isSystem
            )
        }.distinctBy { it.packageName }.sortedBy { it.appName.lowercase(Locale.US) }

        cachedApps = apps
        return apps
    }

    fun getInstalledApps(): List<InstalledAppInfo> {
        return if (cachedApps.isEmpty()) refreshInstalledApps() else cachedApps
    }

    fun launchAppByNameOrAlias(rawQuery: String): AppLaunchOutcome {
        val target = rawQuery.trim()
            .removePrefix("open ")
            .removePrefix("launch ")
            .removePrefix("start ")
            .removeSuffix(" app")
            .trim()
        val lower = target.lowercase(Locale.US)

        if (lower.isEmpty()) {
            return AppLaunchOutcome(
                launched = false,
                spokenResponse = "Which app would you like me to open?",
                detailMessage = "Specify an application name such as YouTube, Settings, Camera, or Chrome."
            )
        }

        // 1. Direct Android System Settings & Core Intents
        when (lower) {
            "settings", "android settings", "system settings" -> {
                return launchIntentSafely(
                    Intent(Settings.ACTION_SETTINGS),
                    spokenSuccess = "Opening Settings.",
                    detail = "Launched Android System Settings"
                )
            }
            "wifi", "wi-fi", "wifi settings", "wi-fi settings" -> {
                return launchIntentSafely(
                    Intent(Settings.ACTION_WIFI_SETTINGS),
                    spokenSuccess = "Opening Wi-Fi settings.",
                    detail = "Launched Android Wi-Fi Settings"
                )
            }
            "bluetooth", "bluetooth settings" -> {
                return launchIntentSafely(
                    Intent(Settings.ACTION_BLUETOOTH_SETTINGS),
                    spokenSuccess = "Opening Bluetooth settings.",
                    detail = "Launched Android Bluetooth Settings"
                )
            }
            "display", "display settings", "brightness settings" -> {
                return launchIntentSafely(
                    Intent(Settings.ACTION_DISPLAY_SETTINGS),
                    spokenSuccess = "Opening Display settings.",
                    detail = "Launched Android Display Settings"
                )
            }
            "sound", "sound settings", "volume settings" -> {
                return launchIntentSafely(
                    Intent(Settings.ACTION_SOUND_SETTINGS),
                    spokenSuccess = "Opening Sound settings.",
                    detail = "Launched Android Sound Settings"
                )
            }
            "camera" -> {
                val cameraIntent = Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)
                val fallbackIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                return if (tryStartActivity(cameraIntent) || tryStartActivity(fallbackIntent)) {
                    AppLaunchOutcome(true, "Opening Camera.", "Launched system camera")
                } else {
                    launchInstalledPackageMatch("camera", "Camera")
                }
            }
            "phone", "dialer" -> {
                return launchIntentSafely(
                    Intent(Intent.ACTION_DIAL),
                    spokenSuccess = "Opening Phone.",
                    detail = "Launched system dialer"
                )
            }
            "messages", "sms", "messaging" -> {
                val smsIntent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_APP_MESSAGING)
                }
                if (tryStartActivity(smsIntent)) {
                    return AppLaunchOutcome(true, "Opening Messages.", "Launched default messaging app")
                }
            }
            "contacts" -> {
                val contactsIntent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_APP_CONTACTS)
                }
                if (tryStartActivity(contactsIntent)) {
                    return AppLaunchOutcome(true, "Opening Contacts.", "Launched contacts app")
                }
            }
            "calendar" -> {
                val calIntent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_APP_CALENDAR)
                }
                if (tryStartActivity(calIntent)) {
                    return AppLaunchOutcome(true, "Opening Calendar.", "Launched calendar app")
                }
            }
            "clock", "alarms", "timer" -> {
                val clockIntent = Intent(AlarmClock.ACTION_SHOW_ALARMS)
                if (tryStartActivity(clockIntent)) {
                    return AppLaunchOutcome(true, "Opening Clock.", "Launched alarms & clock")
                }
            }
            "calculator" -> {
                val calcIntent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_APP_CALCULATOR)
                }
                if (tryStartActivity(calcIntent)) {
                    return AppLaunchOutcome(true, "Opening Calculator.", "Launched calculator app")
                }
            }
        }

        return launchInstalledPackageMatch(lower, target)
    }

    private fun launchInstalledPackageMatch(lowerQuery: String, displayQuery: String): AppLaunchOutcome {
        val pm = context.packageManager
        val apps = getInstalledApps()

        // 1. Check exact or partial match in installed launcher activities
        val exactMatch = apps.firstOrNull { it.appName.lowercase(Locale.US) == lowerQuery }
            ?: apps.firstOrNull { it.appName.lowercase(Locale.US).contains(lowerQuery) }
            ?: apps.firstOrNull { it.packageName.lowercase(Locale.US).contains(lowerQuery) }

        if (exactMatch != null) {
            val launchIntent = pm.getLaunchIntentForPackage(exactMatch.packageName)
            if (launchIntent != null && tryStartActivity(launchIntent)) {
                return AppLaunchOutcome(
                    launched = true,
                    spokenResponse = "Opening ${exactMatch.appName}.",
                    detailMessage = "Launched ${exactMatch.appName} (${exactMatch.packageName})",
                    packageName = exactMatch.packageName
                )
            }
        }

        // 2. Check known package candidates
        val candidatePackages = commonAppPackages[lowerQuery] ?: emptyList()
        for (pkg in candidatePackages) {
            val launchIntent = pm.getLaunchIntentForPackage(pkg)
            if (launchIntent != null && tryStartActivity(launchIntent)) {
                val prettyName = displayQuery.replaceFirstChar { it.uppercase() }
                return AppLaunchOutcome(
                    launched = true,
                    spokenResponse = "Opening $prettyName.",
                    detailMessage = "Launched $pkg",
                    packageName = pkg
                )
            }
        }

        // 3. Not installed: return clear, honest message per specification
        val cleanName = displayQuery.replaceFirstChar { it.uppercase() }
        return AppLaunchOutcome(
            launched = false,
            spokenResponse = "$cleanName is not installed on this device.",
            detailMessage = "App '$cleanName' was not found among installed launcher packages. You can install it or open its web version."
        )
    }

    fun launchPackageDirectly(packageName: String, appName: String): AppLaunchOutcome {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
        return if (launchIntent != null && tryStartActivity(launchIntent)) {
            AppLaunchOutcome(
                launched = true,
                spokenResponse = "Opening $appName.",
                detailMessage = "Launched $appName ($packageName)",
                packageName = packageName
            )
        } else {
            AppLaunchOutcome(
                launched = false,
                spokenResponse = "Unable to launch $appName.",
                detailMessage = "No launchable activity found for $packageName."
            )
        }
    }

    fun openUrl(rawUrl: String): AppLaunchOutcome {
        val normalized = if (rawUrl.startsWith("http://") || rawUrl.startsWith("https://")) {
            rawUrl
        } else {
            "https://$rawUrl"
        }
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(normalized))
        return launchIntentSafely(
            intent,
            spokenSuccess = "Opening link.",
            detail = "Opened $normalized"
        )
    }

    private fun launchIntentSafely(intent: Intent, spokenSuccess: String, detail: String): AppLaunchOutcome {
        return if (tryStartActivity(intent)) {
            AppLaunchOutcome(true, spokenSuccess, detail)
        } else {
            AppLaunchOutcome(
                launched = false,
                spokenResponse = "That system screen is unavailable on this device.",
                detailMessage = "Could not resolve intent ${intent.action}"
            )
        }
    }

    private fun tryStartActivity(intent: Intent): Boolean {
        return try {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            false
        }
    }
}
