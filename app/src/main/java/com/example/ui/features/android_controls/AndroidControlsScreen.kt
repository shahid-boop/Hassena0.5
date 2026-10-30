package com.example.ui.features.android_controls

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.models.DeviceTelemetrySnapshot
import com.example.models.InstalledAppInfo
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.CoralAlert
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberCyanDim
import com.example.ui.theme.EmeraldReady
import com.example.ui.theme.ObsidianBackground
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceElevated
import com.example.ui.theme.ObsidianSurfaceGlass
import com.example.ui.theme.SolarGold
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun AndroidControlsScreen(
    telemetry: DeviceTelemetrySnapshot,
    installedApps: List<InstalledAppInfo>,
    isFlashlightOn: Boolean,
    onToggleFlashlight: (Boolean) -> Unit = {},
    onSetBrightness: (Int) -> Unit = {},
    onSetVolume: (Int) -> Unit = {},
    onLaunchApp: (InstalledAppInfo) -> Unit = {},
    onMakeCall: (String) -> Unit = {},
    onSendMessage: (recipient: String, message: String) -> Unit = { _, _ -> },
    onOpenSearch: (String) -> Unit = {},
    onOpenSystemSettings: (String) -> Unit = {},
    onNavigateBack: () -> Unit = {}
) {
    var showAppPicker by remember { mutableStateOf(false) }
    var appSearchQuery by remember { mutableStateOf("") }
    var showCallDialog by remember { mutableStateOf(false) }
    var callNumber by remember { mutableStateOf("") }
    var showSmsDialog by remember { mutableStateOf(false) }
    var smsRecipient by remember { mutableStateOf("") }
    var smsBody by remember { mutableStateOf("") }
    var showInfoDialog by remember { mutableStateOf(false) }
    var brightnessSlider by remember { mutableFloatStateOf(telemetry.screenBrightnessPercent.toFloat()) }

    // App Picker Dialog
    if (showAppPicker) {
        val filteredApps = remember(installedApps, appSearchQuery) {
            if (appSearchQuery.isBlank()) installedApps else {
                installedApps.filter { it.appName.contains(appSearchQuery, ignoreCase = true) }
            }
        }
        AlertDialog(
            onDismissRequest = { showAppPicker = false },
            containerColor = ObsidianSurfaceElevated,
            title = { Text("Launch Installed Application", fontWeight = FontWeight.Bold, color = TextPrimary) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = appSearchQuery,
                        onValueChange = { appSearchQuery = it },
                        placeholder = { Text("Search apps...", color = TextMuted) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = CyberCyan,
                            unfocusedBorderColor = BorderSubtle
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    LazyColumn(modifier = Modifier.height(260.dp)) {
                        items(filteredApps) { app ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onLaunchApp(app)
                                        showAppPicker = false
                                    }
                                    .padding(vertical = 8.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(ObsidianSurface, CircleShape)
                                        .border(1.dp, BorderSubtle, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Apps, null, tint = CyberCyan, modifier = Modifier.size(16.dp))
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(app.appName, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                    Text(app.packageName, color = TextMuted, fontSize = 10.sp)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                OutlinedButton(onClick = { showAppPicker = false }) { Text("Close", color = TextSecondary) }
            }
        )
    }

    // Call Dialog
    if (showCallDialog) {
        AlertDialog(
            onDismissRequest = { showCallDialog = false },
            containerColor = ObsidianSurfaceElevated,
            title = { Text("Make Phone Call", fontWeight = FontWeight.Bold, color = TextPrimary) },
            text = {
                Column {
                    Text("Enter phone number or contact name to start call flow.", fontSize = 12.sp, color = TextSecondary)
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = callNumber,
                        onValueChange = { callNumber = it },
                        placeholder = { Text("e.g. 555-0199", color = TextMuted) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = CyberCyan,
                            unfocusedBorderColor = BorderSubtle
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (callNumber.isNotBlank()) {
                            onMakeCall(callNumber)
                            showCallDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = Color.Black)
                ) { Text("Start Call", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                OutlinedButton(onClick = { showCallDialog = false }) { Text("Cancel", color = TextSecondary) }
            }
        )
    }

    // SMS Dialog
    if (showSmsDialog) {
        AlertDialog(
            onDismissRequest = { showSmsDialog = false },
            containerColor = ObsidianSurfaceElevated,
            title = { Text("Compose Message", fontWeight = FontWeight.Bold, color = TextPrimary) },
            text = {
                Column {
                    Text("Prepare message with recipient and content.", fontSize = 12.sp, color = TextSecondary)
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = smsRecipient,
                        onValueChange = { smsRecipient = it },
                        label = { Text("Recipient Number / Contact") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = CyberCyan,
                            unfocusedBorderColor = BorderSubtle
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = smsBody,
                        onValueChange = { smsBody = it },
                        label = { Text("Message Body") },
                        minLines = 2,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = CyberCyan,
                            unfocusedBorderColor = BorderSubtle
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (smsRecipient.isNotBlank()) {
                            onSendMessage(smsRecipient, smsBody)
                            showSmsDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = Color.Black)
                ) { Text("Compose", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                OutlinedButton(onClick = { showSmsDialog = false }) { Text("Cancel", color = TextSecondary) }
            }
        )
    }

    // Device Info Dialog
    if (showInfoDialog) {
        AlertDialog(
            onDismissRequest = { showInfoDialog = false },
            containerColor = ObsidianSurfaceElevated,
            title = { Text("JARVIS Device Telemetry", fontWeight = FontWeight.Bold, color = TextPrimary) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    InfoRow("Model", telemetry.deviceModel)
                    InfoRow("Android Version", telemetry.androidVersion)
                    InfoRow("Battery Level", "${telemetry.batteryPercent}% (${if (telemetry.isCharging) "Charging" else "Discharging"})")
                    InfoRow("Battery Temp", "${telemetry.batteryTempCelsius}°C")
                    InfoRow("RAM Memory", "${telemetry.availableRamMb} MB / ${telemetry.totalRamMb} MB")
                    InfoRow("Free Storage", "${telemetry.freeStorageGb} GB")
                    InfoRow("Network State", telemetry.networkTypeLabel)
                    InfoRow("Screen Brightness", "${telemetry.screenBrightnessPercent}%")
                }
            },
            confirmButton = {
                Button(onClick = { showInfoDialog = false }) { Text("Close") }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBackground)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
            .testTag("android_controls_screen")
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier
                    .size(40.dp)
                    .background(ObsidianSurfaceGlass, CircleShape)
                    .border(1.dp, BorderSubtle, CircleShape)
                    .testTag("android_controls_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = TextPrimary
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(ObsidianSurfaceGlass, CircleShape)
                    .border(1.dp, CyberCyan.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PhoneAndroid,
                    contentDescription = null,
                    tint = CyberCyan,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "JARVIS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = CyberCyan,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Android Controls",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Hardware Controls Card (Flashlight, Brightness, Volume)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text("HARDWARE QUICK CONTROLS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SolarGold, letterSpacing = 1.sp)
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.FlashlightOn, null, tint = if (isFlashlightOn) SolarGold else TextMuted, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Flashlight", fontSize = 13.sp, color = TextPrimary)
                    }

                    Button(
                        onClick = { onToggleFlashlight(!isFlashlightOn) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isFlashlightOn) SolarGold else ObsidianSurfaceElevated,
                            contentColor = if (isFlashlightOn) Color.Black else TextPrimary
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text(if (isFlashlightOn) "ON" else "OFF", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Brightness slider
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Brightness: ${brightnessSlider.toInt()}%", fontSize = 12.sp, color = TextSecondary)
                }
                Slider(
                    value = brightnessSlider,
                    onValueChange = {
                        brightnessSlider = it
                        onSetBrightness(it.toInt())
                    },
                    valueRange = 5f..100f,
                    colors = SliderDefaults.colors(
                        thumbColor = CyberCyan,
                        activeTrackColor = CyberCyan,
                        inactiveTrackColor = ObsidianSurfaceGlass
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Actions List (Matching screen 10 of mockup)
        Text("SYSTEM ACTION HUBS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMuted, letterSpacing = 1.sp)
        Spacer(modifier = Modifier.height(8.dp))

        ActionHubRow(
            icon = Icons.Default.Apps,
            title = "Open App",
            subtitle = "Launch any installed app",
            onClick = { showAppPicker = true }
        )

        ActionHubRow(
            icon = Icons.Default.Call,
            title = "Make Call",
            subtitle = "Call a contact or number",
            onClick = { showCallDialog = true }
        )

        ActionHubRow(
            icon = Icons.AutoMirrored.Filled.Message,
            title = "Send Message",
            subtitle = "Compose or send SMS",
            onClick = { showSmsDialog = true }
        )

        ActionHubRow(
            icon = Icons.Default.Search,
            title = "Search",
            subtitle = "Search the web or device",
            onClick = { onOpenSearch("JARVIS Android") }
        )

        ActionHubRow(
            icon = Icons.Default.Settings,
            title = "System Settings",
            subtitle = "Open Android settings",
            onClick = { onOpenSystemSettings(android.provider.Settings.ACTION_SETTINGS) }
        )

        ActionHubRow(
            icon = Icons.Default.Info,
            title = "Device Info",
            subtitle = "Check device information",
            onClick = { showInfoDialog = true }
        )
    }
}

@Composable
private fun ActionHubRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onClick() }
            .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(ObsidianSurfaceElevated, RoundedCornerShape(10.dp))
                    .border(1.dp, BorderSubtle, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(20.dp))
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text(subtitle, fontSize = 11.sp, color = TextSecondary)
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = TextMuted,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = TextSecondary, fontSize = 12.sp)
        Text(value, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}
