package com.example.ui.features.visual_lens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.content.BroadcastReceiver
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ImageDecoder
import android.graphics.Paint
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.ScreenSearchDesktop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.ui.theme.AmberThinking
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.EmeraldReady
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VisualLensScreen(
    capturedBitmap: Bitmap?,
    analysisText: String,
    isAnalyzing: Boolean,
    onBitmapCaptured: (Bitmap?) -> Unit,
    onAnalyzePrompt: (String) -> Unit,
    onAnalyzeActiveScreen: () -> Unit,
    onSpeakText: (String) -> Unit,
    onNavigateBackHome: () -> Unit
) {
    BackHandler { onNavigateBackHome() }

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var showLiveCamera by rememberSaveable { mutableStateOf(false) }
    var selectedVideoUri by rememberSaveable { mutableStateOf<Uri?>(null) }
    var customVisionQuestion by rememberSaveable { mutableStateOf("") }
    var previewViewRef by remember { mutableStateOf<PreviewView?>(null) }
    var selected3DScene by rememberSaveable { mutableStateOf(0) }
    var selectedLensEffect by rememberSaveable { mutableStateOf(0) }
    var importedModelMesh by remember { mutableStateOf<FloatArray?>(null) }
    var importedModelName by rememberSaveable { mutableStateOf("") }
    var modelLoadMessage by rememberSaveable { mutableStateOf("Built-in Hologram scene") }
    val depthCapabilityMessage = remember(context) { LensDepthCapabilities.describe(context) }
    val modelPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            try {
                val model = GltfModelLoader.load(context.contentResolver, uri)
                importedModelMesh = model.interleaved
                importedModelName = model.sourceName
                modelLoadMessage = "Loaded ${model.triangleCount} triangles • ${model.sourceName}"
                selected3DScene = 0
            } catch (error: Exception) {
                importedModelMesh = null
                importedModelName = ""
                modelLoadMessage = "Model not loaded: ${error.message ?: "unsupported GLTF"}"
            }
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        if (granted) {
            showLiveCamera = true
        }
    }

    // Zero-permission Android Photo Picker (Google Play Policy compliant)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            showLiveCamera = false
            val mimeType = context.contentResolver.getType(uri).orEmpty()
            if (mimeType.startsWith("video/")) {
                selectedVideoUri = uri
            } else {
                selectedVideoUri = null
                val bmp = decodeBitmapFromUri(context, uri)
                if (bmp != null) onBitmapCaptured(bmp)
            }
        }
    }

    // Video preview is deliberately muted and looping. It is paused whenever the
    // screen turns off, the app leaves the foreground, or this screen is removed.
    val videoPlayer = remember(selectedVideoUri) {
        selectedVideoUri?.let { uri ->
            ExoPlayer.Builder(context).build().apply {
                setMediaItem(MediaItem.fromUri(uri))
                volume = 0f
                repeatMode = Player.REPEAT_MODE_ONE
                playWhenReady = true
                prepare()
            }
        }
    }

    DisposableEffect(videoPlayer, lifecycleOwner, context) {
        val lifecycleObserver = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_STOP) {
                videoPlayer?.pause()
            }
            if (event == androidx.lifecycle.Lifecycle.Event.ON_START && selectedVideoUri != null) {
                // Do not autoplay after returning; user can tap the preview to resume.
                videoPlayer?.pause()
            }
        }
        lifecycleOwner.lifecycle.addObserver(lifecycleObserver)
        val screenOffReceiver = object : BroadcastReceiver() {
            override fun onReceive(receiverContext: Context?, intent: Intent?) {
                if (intent?.action == Intent.ACTION_SCREEN_OFF) videoPlayer?.pause()
            }
        }
        context.registerReceiver(screenOffReceiver, IntentFilter(Intent.ACTION_SCREEN_OFF))
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(lifecycleObserver)
            try { context.unregisterReceiver(screenOffReceiver) } catch (_: IllegalArgumentException) {}
            videoPlayer?.release()
        }
    }

    val quickVisionPrompts = listOf(
        "What is this?" to "Identify the primary object or scene in this image and explain it clearly.",
        "Read this page (OCR)" to "Extract and transcribe all visible text from this image accurately, preserving structure.",
        "Explain this image" to "Provide a detailed breakdown of the components, text, and meaning in this image.",
        "Summarize key points" to "Summarize the key takeaways or action items visible in this image."
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Text(
            text = "JARVIS Visual Lens",
            style = MaterialTheme.typography.headlineMedium,
            color = TextPrimary
        )
        Text(
            text = "Camera capture, Photo Picker, OCR page reader & Accessibility Screen analysis",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Action Buttons Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    selectedVideoUri = null
                    if (hasCameraPermission) {
                        showLiveCamera = !showLiveCamera
                    } else {
                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = CyberCyan,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag("lens_camera_button")
            ) {
                Icon(Icons.Default.CameraAlt, contentDescription = "Camera", modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (showLiveCamera) "Capture" else "Camera", style = MaterialTheme.typography.labelLarge)
            }

            OutlinedButton(
                onClick = {
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                    )
                },
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                modifier = Modifier
                    .weight(1f)
                    .testTag("lens_photo_picker_button")
            ) {
                Icon(Icons.Default.PhotoLibrary, contentDescription = "Pick Photo", tint = CyberCyan, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Gallery", color = TextPrimary, style = MaterialTheme.typography.labelLarge)
            }

            OutlinedButton(
                onClick = onAnalyzeActiveScreen,
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                modifier = Modifier
                    .weight(1f)
                    .testTag("lens_read_screen_button")
            ) {
                Icon(Icons.Default.ScreenSearchDesktop, contentDescription = "Read Screen", tint = EmeraldReady, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Screen", color = TextPrimary, style = MaterialTheme.typography.labelLarge)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 3D scene and lens effect controls. GLB and embedded-buffer glTF models can be imported.
        Text("3D SCENE", style = MaterialTheme.typography.labelLarge, color = CyberCyan)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("Hologram", "Chrome", "Emerald", "Solar Flare").forEachIndexed { index, title ->
                if (selected3DScene == index) Button(onClick = { selected3DScene = index; importedModelMesh = null; importedModelName = "" }, contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 5.dp)) { Text(title) }
                else OutlinedButton(onClick = { selected3DScene = index; importedModelMesh = null; importedModelName = "" }, contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 5.dp)) { Text(title) }
            }
        }
        Text("LENS EFFECT", style = MaterialTheme.typography.labelLarge, color = CyberCyan, modifier = Modifier.padding(top = 8.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("Balanced", "Reflective", "Scanlines", "Bloom Glow").forEachIndexed { index, title ->
                if (selectedLensEffect == index) Button(onClick = { selectedLensEffect = index }, contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 5.dp)) { Text(title) }
                else OutlinedButton(onClick = { selectedLensEffect = index }, contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 5.dp)) { Text(title) }
            }
        }
        OutlinedButton(onClick = { modelPicker.launch(arrayOf("model/gltf-binary", "model/gltf+json", "application/octet-stream", "*/*")) }, modifier = Modifier.fillMaxWidth()) {
            Text("Import 3D model (.glb / .gltf)")
        }
        Text(modelLoadMessage, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        OpenGlLensPreview(selectedScene = selected3DScene, selectedEffect = selectedLensEffect, modelMesh = importedModelMesh)
        Text(depthCapabilityMessage, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        Text(
            "Enhanced mobile shader: metallic/roughness presets, procedural studio reflections and bloom-style glow. True HDR image-based PBR, shadow-map rendering and camera-composited AR still require a dedicated rendering pipeline.",
            style = MaterialTheme.typography.bodySmall, color = TextMuted
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Viewfinder or Captured Image Box
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = ObsidianSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
            modifier = Modifier
                .fillMaxWidth()
                .height(230.dp)
        ) {
            when {
                selectedVideoUri != null && videoPlayer != null -> {
                    Box(modifier = Modifier.fillMaxSize()) {
                        AndroidView(
                            factory = { ctx ->
                                PlayerView(ctx).apply {
                                    player = videoPlayer
                                    useController = true
                                    setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                                }
                            },
                            update = { view -> view.player = videoPlayer },
                            modifier = Modifier.fillMaxSize()
                        )
                        Text(
                            text = "Muted • Loop",
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
                                .background(Color.Black.copy(alpha = 0.65f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 5.dp)
                        )
                    }
                }
                showLiveCamera && hasCameraPermission -> {
                    Box(modifier = Modifier.fillMaxSize()) {
                        AndroidView(
                            factory = { ctx ->
                                val previewView = PreviewView(ctx)
                                previewViewRef = previewView
                                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                                cameraProviderFuture.addListener({
                                    try {
                                        val cameraProvider = cameraProviderFuture.get()
                                        val preview = Preview.Builder().build().also {
                                            it.setSurfaceProvider(previewView.surfaceProvider)
                                        }
                                        cameraProvider.unbindAll()
                                        cameraProvider.bindToLifecycle(
                                            lifecycleOwner,
                                            CameraSelector.DEFAULT_BACK_CAMERA,
                                            preview
                                        )
                                    } catch (_: Exception) {
                                    }
                                }, ContextCompat.getMainExecutor(ctx))
                                previewView
                            },
                            modifier = Modifier.fillMaxSize()
                        )

                        Button(
                            onClick = {
                                val frame = previewViewRef?.bitmap ?: createSampleDiagnosticPageBitmap()
                                showLiveCamera = false
                                onBitmapCaptured(frame)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldReady, contentColor = Color.Black),
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(12.dp)
                                .testTag("capture_camera_frame_button")
                        ) {
                            Text("Snap Frame for Analysis", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
                capturedBitmap != null -> {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Image(
                            bitmap = capturedBitmap.asImageBitmap(),
                            contentDescription = "Captured Visual Lens image",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = ObsidianSurface.copy(alpha = 0.85f),
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(10.dp)
                                .clickable { onBitmapCaptured(createSampleDiagnosticPageBitmap()) }
                        ) {
                            Text(
                                text = "Load Sample OCR Page",
                                style = MaterialTheme.typography.labelSmall,
                                color = CyberCyan,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
                else -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = "Visual Lens Ready",
                            tint = CyberCyan,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No Image Loaded",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Use Camera, pick a photo/video, or load a sample page for OCR & Vision.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = { selectedVideoUri = null; onBitmapCaptured(createSampleDiagnosticPageBitmap()) },
                            border = androidx.compose.foundation.BorderStroke(1.dp, CyberCyan),
                            modifier = Modifier.testTag("load_sample_ocr_card_button")
                        ) {
                            Text("Load Sample Technical Page for OCR", color = CyberCyan)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Quick Vision Action Chips
        Text(
            text = "VISION & OCR COMMANDS",
            style = MaterialTheme.typography.labelMedium,
            color = TextMuted
        )
        Spacer(modifier = Modifier.height(6.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            quickVisionPrompts.forEach { (label, fullPrompt) ->
                Surface(
                    shape = RoundedCornerShape(50),
                    color = ObsidianSurfaceElevated,
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable {
                            if (capturedBitmap == null) {
                                onBitmapCaptured(createSampleDiagnosticPageBitmap())
                            }
                            onAnalyzePrompt(fullPrompt)
                        }
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge,
                        color = CyberCyan,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Custom question about the image
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = customVisionQuestion,
                onValueChange = { customVisionQuestion = it },
                placeholder = { Text("Ask about this image...", color = TextMuted) },
                singleLine = true,
                shape = RoundedCornerShape(20.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CyberCyan,
                    unfocusedBorderColor = BorderSubtle,
                    focusedContainerColor = ObsidianSurface,
                    unfocusedContainerColor = ObsidianSurface,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                modifier = Modifier
                    .weight(1f)
                    .testTag("lens_custom_prompt_input")
            )
            IconButton(
                onClick = {
                    if (customVisionQuestion.isNotBlank()) {
                        if (capturedBitmap == null) {
                            onBitmapCaptured(createSampleDiagnosticPageBitmap())
                        }
                        onAnalyzePrompt(customVisionQuestion)
                        customVisionQuestion = ""
                    }
                },
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(CyberCyan)
                    .testTag("lens_analyze_send_button")
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Analyze", tint = Color.Black)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Streaming Vision Output Card
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = ObsidianSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, if (isAnalyzing) CyberCyan else BorderSubtle),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isAnalyzing) "STREAMING MULTIMODAL VISION..." else "VISION & OCR RESULT",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isAnalyzing) AmberThinking else CyberCyan
                    )
                    if (analysisText.isNotBlank()) {
                        IconButton(
                            onClick = { onSpeakText(analysisText) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = "Read vision analysis aloud",
                                tint = TextSecondary
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = analysisText.ifBlank {
                        "Capture or pick an image, or tap 'Screen' to inspect the active Android UI via JARVIS Accessibility Service."
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextPrimary
                )
            }
        }
    }
}

private fun decodeBitmapFromUri(context: Context, uri: Uri): Bitmap? {
    return try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                decoder.isMutableRequired = true
            }
        } else {
            @Suppress("DEPRECATION")
            MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
        }
    } catch (_: Exception) {
        null
    }
}

/**
 * Generates a crisp high-contrast technical specification & invoice card bitmap
 * so users on emulators without physical camera scenes can immediately test real Gemini Vision OCR!
 */
private fun createSampleDiagnosticPageBitmap(): Bitmap {
    val bmp = Bitmap.createBitmap(720, 460, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    canvas.drawColor(android.graphics.Color.parseColor("#0D1528"))

    val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.parseColor("#00E5FF")
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }
    canvas.drawRect(20f, 20f, 700f, 440f, borderPaint)

    val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.parseColor("#00E5FF")
        textSize = 30f
        isFakeBoldText = true
    }
    canvas.drawText("HASSEENA AI - NEURAL TELEMETRY SHEET", 45f, 75f, titlePaint)

    val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        textSize = 23f
    }
    canvas.drawText("Module: Fast Voice-First Response Pipeline v2.4", 45f, 130f, bodyPaint)
    canvas.drawText("Local Command Latency: 3.2 ms (Zero-Cloud Route)", 45f, 175f, bodyPaint)
    canvas.drawText("Streaming TTFT Target: < 280 ms via HTTP/2 SSE", 45f, 220f, bodyPaint)
    canvas.drawText("Action Item 1: Verify Torch & Audio Volume APIs", 45f, 280f, bodyPaint)
    canvas.drawText("Action Item 2: Enable Barge-In Voice Interruption", 45f, 325f, bodyPaint)
    canvas.drawText("Serial ID: HSN-2026-9941-QUANTUM", 45f, 390f, bodyPaint)

    return bmp
}
