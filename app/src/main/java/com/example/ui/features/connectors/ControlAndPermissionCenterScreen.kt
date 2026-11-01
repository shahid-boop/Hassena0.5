package com.example.ui.features.connectors

import android.view.KeyEvent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Launch
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.models.CapturedNotification
import com.example.models.DeviceTelemetrySnapshot
import com.example.models.InstalledAppInfo
import com.example.models.PermissionStatusItem
import com.example.services.android.AndroidControlService
import com.example.ui.theme.AmberThinking
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.CoralAlert
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.EmeraldReady
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceElevated
import com.example.ui.theme.QuantumViolet
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlin.math.roundToInt

@Composable
fun ControlAndPermissionCenterScreen(
    telemetry: DeviceTelemetrySnapshot,
    permissions: List<PermissionStatusItem>,
    installedApps: List<InstalledAppInfo>,
    activeNotifications: List<CapturedNotification>,
    isAccessibilityActive: Boolean,
    isFlashlightOn: Boolean,
    onRefreshStatus: () -> Unit,
    onRequestRuntimePermission: (String) -> Unit,
    onOpenSpecialSettings: (String, Boolean) -> Unit,
    onToggleFlashlight: (Boolean) -> Unit,
    onSetVolumePercent: (Int) -> Unit,
    onSetBrightnessPercent: (Int) -> Unit,
    onMediaKey: (Int, String) -> Unit,
    onOpenWifiPanel: () -> Unit,
    onOpenBluetoothSettings: () -> Unit,
    onSetTimerSeconds: (Int) -> Unit,
    onSetAlarm: (Int, Int) -> Unit,
    onCreateCalendarEvent: (String) -> Unit,
    onSubmitCommandWithConfirmation: (String) -> Unit,
    onLaunchInstalledApp: (InstalledAppInfo) -> Unit,
    onAccessibilityCommand: (String) -> Unit,
    onNavigateBackHome: () -> Unit
) {
    BackHandler { onNavigateBackHome() }

    var activeSection by rememberSaveable { mutableIntStateOf(0) } // 0=Controls, 1=Permissions, 2=Apps & Alerts
    var volumeSlider by remember(telemetry.musicVolumePercent) {
        mutableFloatStateOf(telemetry.musicVolumePercent.toFloat())
    }
    var brightnessSlider by remember(telemetry.screenBrightnessPercent) {
        mutableFloatStateOf(telemetry.screenBrightnessPercent.toFloat())
    }
    var appSearchQuery by rememberSaveable { mutableStateOf("") }
    var callRecipientInput by rememberSaveable { mutableStateOf("") }
    var smsRecipientInput by rememberSaveable { mutableStateOf("") }
    var smsBodyInput by rememberSaveable { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Header Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Control & Permission Center",
                    style = MaterialTheme.typography.headlineMedium,
                    color = TextPrimary
                )
                Text(
                    text = "Android hardware tools, Accessibility, App Launcher & Permissions",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
            IconButton(
                onClick = onRefreshStatus,
                modifier = Modifier.testTag("refresh_control_center_button")
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh status", tint = CyberCyan)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Section Switcher Tabs
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val tabs = listOf(
                Triple(0, "Device Control", Icons.Default.Tune),
                Triple(1, "Permissions", Icons.Default.Security),
                Triple(2, "Apps & Alerts", Icons.Default.Apps)
            )
            tabs.forEach { (idx, title, icon) ->
                val selected = activeSection == idx
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (selected) CyberCyan.copy(alpha = 0.2f) else ObsidianSurfaceElevated,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (selected) CyberCyan else BorderSubtle
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable {
                            activeSection = idx
                            onRefreshStatus()
                        }
                        .testTag("control_tab_$idx")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (selected) CyberCyan else TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = title,
                            style = MaterialTheme.typography.labelLarge,
                            color = if (selected) CyberCyan else TextPrimary
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        when (activeSection) {
            // =================================================================
            // TAB 0: ANDROID CONTROL SERVICE TOOLS
            // =================================================================
            0 -> {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(top = 4.dp, bottom = 64.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Telemetry overview
                    item {
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = ObsidianSurface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "LIVE DEVICE TELEMETRY (${telemetry.deviceModel})",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = CyberCyan
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "${telemetry.androidVersion} • Battery: ${telemetry.batteryPercent}% (${telemetry.batteryTempCelsius}°C)\n" +
                                        "RAM Free: ${telemetry.availableRamMb} / ${telemetry.totalRamMb} MB • Storage Free: ${telemetry.freeStorageGb} GB\n" +
                                        "Network: ${telemetry.networkTypeLabel}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )
                            }
                        }
                    }

                    // Flashlight, Wi-Fi, Bluetooth, System Settings
                    item {
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = ObsidianSurface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "HARDWARE & CONNECTIVITY",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = CyberCyan
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = { onToggleFlashlight(!isFlashlightOn) },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isFlashlightOn) CyberCyan else ObsidianSurfaceElevated,
                                            contentColor = if (isFlashlightOn) Color.Black else TextPrimary
                                        ),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.FlashlightOn, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(if (isFlashlightOn) "Torch ON" else "Torch OFF")
                                    }

                                    OutlinedButton(
                                        onClick = onOpenWifiPanel,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.Wifi, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Wi-Fi Panel", color = TextPrimary)
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = onOpenBluetoothSettings,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.Bluetooth, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Bluetooth", color = TextPrimary)
                                    }
                                    OutlinedButton(
                                        onClick = { onOpenSpecialSettings(android.provider.Settings.ACTION_SETTINGS, false) },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.Settings, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("OS Settings", color = TextPrimary)
                                    }
                                }
                            }
                        }
                    }

                    // Volume & Brightness Sliders
                    item {
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = ObsidianSurface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("MEDIA VOLUME", style = MaterialTheme.typography.labelMedium, color = CyberCyan)
                                    Text("${volumeSlider.roundToInt()}%", style = MaterialTheme.typography.labelMedium, color = EmeraldReady)
                                }
                                Slider(
                                    value = volumeSlider,
                                    onValueChange = { volumeSlider = it },
                                    onValueChangeFinished = { onSetVolumePercent(volumeSlider.roundToInt()) },
                                    valueRange = 0f..100f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = CyberCyan,
                                        activeTrackColor = CyberCyan
                                    )
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("SCREEN BRIGHTNESS", style = MaterialTheme.typography.labelMedium, color = CyberCyan)
                                    Text("${brightnessSlider.roundToInt()}%", style = MaterialTheme.typography.labelMedium, color = EmeraldReady)
                                }
                                Slider(
                                    value = brightnessSlider,
                                    onValueChange = { brightnessSlider = it },
                                    onValueChangeFinished = { onSetBrightnessPercent(brightnessSlider.roundToInt()) },
                                    valueRange = 5f..100f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = QuantumViolet,
                                        activeTrackColor = QuantumViolet
                                    )
                                )
                            }
                        }
                    }

                    // Media, Alarm, Timer & Calendar Integration
                    item {
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = ObsidianSurface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "MEDIA, ALARMS, TIMERS & CALENDAR",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = CyberCyan
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    OutlinedButton(onClick = { onMediaKey(KeyEvent.KEYCODE_MEDIA_PREVIOUS, "Previous Track") }) {
                                        Icon(Icons.Default.SkipPrevious, contentDescription = "Previous", tint = TextPrimary)
                                    }
                                    Button(
                                        onClick = { onMediaKey(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, "Play / Pause") },
                                        colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = Color.Black)
                                    ) {
                                        Icon(Icons.Default.PlayArrow, contentDescription = "Play/Pause")
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Play / Pause")
                                    }
                                    OutlinedButton(onClick = { onMediaKey(KeyEvent.KEYCODE_MEDIA_NEXT, "Next Track") }) {
                                        Icon(Icons.Default.SkipNext, contentDescription = "Next", tint = TextPrimary)
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = { onSetTimerSeconds(300) },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.Timer, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("5m Timer", color = TextPrimary)
                                    }
                                    OutlinedButton(
                                        onClick = { onSetAlarm(7, 30) },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.Alarm, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("7:30 Alarm", color = TextPrimary)
                                    }
                                    OutlinedButton(
                                        onClick = { onCreateCalendarEvent("JARVIS AI Briefing") },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.Event, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("+ Event", color = TextPrimary)
                                    }
                                }
                            }
                        }
                    }

                    // Accessibility Assisted UI Navigation Card
                    item {
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = ObsidianSurface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.AccessibilityNew, contentDescription = null, tint = CyberCyan)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("ACCESSIBILITY UI AUTOMATION", style = MaterialTheme.typography.labelMedium, color = CyberCyan)
                                    }
                                    Text(
                                        text = if (isAccessibilityActive) "ACTIVE" else "DISABLED",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isAccessibilityActive) EmeraldReady else AmberThinking
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = { onAccessibilityCommand("Read screen") },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Read Screen", color = TextPrimary)
                                    }
                                    OutlinedButton(
                                        onClick = { onAccessibilityCommand("Take screenshot") },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Screenshot", color = TextPrimary)
                                    }
                                    OutlinedButton(
                                        onClick = { onAccessibilityCommand("Go home") },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Go Home", color = TextPrimary)
                                    }
                                }
                            }
                        }
                    }

                    // Communication Actions (With Confirmation!)
                    item {
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = ObsidianSurface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "COMMUNICATION ACTIONS (WITH CONFIRMATION)",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = CyberCyan
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = callRecipientInput,
                                        onValueChange = { callRecipientInput = it },
                                        placeholder = { Text("Name or phone number...", color = TextMuted) },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = CyberCyan,
                                            unfocusedBorderColor = BorderSubtle,
                                            focusedTextColor = TextPrimary,
                                            unfocusedTextColor = TextPrimary
                                        )
                                    )
                                    Button(
                                        onClick = {
                                            if (callRecipientInput.isNotBlank()) {
                                                onSubmitCommandWithConfirmation("Call $callRecipientInput")
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldReady, contentColor = Color.Black),
                                        modifier = Modifier.testTag("initiate_call_button")
                                    ) {
                                        Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Call")
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                OutlinedTextField(
                                    value = smsRecipientInput,
                                    onValueChange = { smsRecipientInput = it },
                                    placeholder = { Text("SMS Recipient name or number...", color = TextMuted) },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = CyberCyan,
                                        unfocusedBorderColor = BorderSubtle,
                                        focusedTextColor = TextPrimary,
                                        unfocusedTextColor = TextPrimary
                                    )
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = smsBodyInput,
                                        onValueChange = { smsBodyInput = it },
                                        placeholder = { Text("Message text...", color = TextMuted) },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = CyberCyan,
                                            unfocusedBorderColor = BorderSubtle,
                                            focusedTextColor = TextPrimary,
                                            unfocusedTextColor = TextPrimary
                                        )
                                    )
                                    Button(
                                        onClick = {
                                            if (smsRecipientInput.isNotBlank() && smsBodyInput.isNotBlank()) {
                                                onSubmitCommandWithConfirmation("Send message to $smsRecipientInput saying $smsBodyInput")
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = Color.Black),
                                        modifier = Modifier.testTag("initiate_sms_button")
                                    ) {
                                        Icon(Icons.Default.Sms, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("SMS")
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // =================================================================
            // TAB 1: HASSEENA PERMISSION CENTER
            // =================================================================
            1 -> {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .testTag("permissions_center_list"),
                    contentPadding = PaddingValues(top = 4.dp, bottom = 64.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = ObsidianSurfaceElevated,
                            border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "JARVIS never requests permissions blindly on first launch. Grant only the capabilities you want JARVIS to use.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    items(permissions, key = { it.id }) { perm ->
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = ObsidianSurface,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (perm.isGranted) EmeraldReady.copy(alpha = 0.45f) else BorderSubtle
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = if (perm.isGranted) Icons.Default.CheckCircle else Icons.Default.WarningAmber,
                                            contentDescription = null,
                                            tint = if (perm.isGranted) EmeraldReady else AmberThinking,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = perm.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = TextPrimary
                                        )
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = if (perm.isGranted) EmeraldReady.copy(alpha = 0.15f) else CoralAlert.copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = if (perm.isGranted) "GRANTED" else "NOT GRANTED",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (perm.isGranted) EmeraldReady else CoralAlert,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Why JARVIS needs it: ${perm.whyNeeded}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )

                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (!perm.isGranted && perm.manifestPermission != null && !perm.isSpecialAccess) {
                                        Button(
                                            onClick = { onRequestRuntimePermission(perm.manifestPermission) },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = CyberCyan,
                                                contentColor = Color.Black
                                            ),
                                            modifier = Modifier.testTag("grant_perm_${perm.id}")
                                        ) {
                                            Text("Grant Permission", style = MaterialTheme.typography.labelLarge)
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            val action = perm.settingsIntentAction
                                                ?: android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                                            val pkgSpecific = perm.settingsIntentAction == android.provider.Settings.ACTION_MANAGE_WRITE_SETTINGS ||
                                                perm.settingsIntentAction == null
                                            onOpenSpecialSettings(action, pkgSpecific)
                                        },
                                        modifier = Modifier.testTag("settings_perm_${perm.id}")
                                    ) {
                                        Text("Open Settings", color = CyberCyan, style = MaterialTheme.typography.labelLarge)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // =================================================================
            // TAB 2: INSTALLED APP LAUNCHER & NOTIFICATIONS READER
            // =================================================================
            2 -> {
                val filteredApps = remember(installedApps, appSearchQuery) {
                    if (appSearchQuery.isBlank()) installedApps
                    else installedApps.filter {
                        it.appName.contains(appSearchQuery, ignoreCase = true) ||
                            it.packageName.contains(appSearchQuery, ignoreCase = true)
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(top = 4.dp, bottom = 64.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Active Notifications Section
                    item {
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = ObsidianSurface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = CyberCyan)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "ACTIVE NOTIFICATIONS (${activeNotifications.size})",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = CyberCyan
                                        )
                                    }
                                    Text(
                                        text = "Manage Access →",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = QuantumViolet,
                                        modifier = Modifier.clickable {
                                            onOpenSpecialSettings(
                                                android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS,
                                                false
                                            )
                                        }
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                if (activeNotifications.isEmpty()) {
                                    Text(
                                        text = "No captured notifications (or enable Notification Access in the Permissions tab).",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextSecondary
                                    )
                                } else {
                                    activeNotifications.take(5).forEach { notif ->
                                        Text(
                                            text = "• [${notif.appName}] ${notif.title}: ${notif.text}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = TextPrimary,
                                            modifier = Modifier.padding(vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Installed Apps Search
                    item {
                        OutlinedTextField(
                            value = appSearchQuery,
                            onValueChange = { appSearchQuery = it },
                            placeholder = { Text("Search installed apps (${installedApps.size})...", color = TextMuted) },
                            singleLine = true,
                            shape = RoundedCornerShape(18.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CyberCyan,
                                unfocusedBorderColor = BorderSubtle,
                                focusedContainerColor = ObsidianSurface,
                                unfocusedContainerColor = ObsidianSurface,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("installed_apps_search_input")
                        )
                    }

                    items(filteredApps, key = { it.packageName }) { app ->
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = ObsidianSurface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { onLaunchInstalledApp(app) }
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = app.appName,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = app.packageName,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondary
                                    )
                                }
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Launch,
                                    contentDescription = "Launch ${app.appName}",
                                    tint = CyberCyan
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
