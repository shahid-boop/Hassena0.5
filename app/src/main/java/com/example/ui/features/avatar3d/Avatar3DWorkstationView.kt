package com.example.ui.features.avatar3d

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.View
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.ui.features.visual_lens.OpenGlLensPreview
import org.json.JSONObject

class AvatarJsBridge(
    private val context: Context,
    private val onReady: () -> Unit
) {
    private val mainHandler = Handler(Looper.getMainLooper())

    @JavascriptInterface
    fun onAvatarReady() {
        mainHandler.post { onReady() }
    }

    @JavascriptInterface
    fun getAvatarBase64(): String {
        return try {
            val bytes = context.assets.open("avatar3d/female_avatar.glb").use { it.readBytes() }
            android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
        } catch (_: Exception) {
            ""
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun Avatar3DWorkstationView(
    state: AvatarWorkstationState,
    onReady: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var isLoaded by remember { mutableStateOf(false) }
    var hasError by remember { mutableStateOf(false) }

    val bridge = remember(context) {
        AvatarJsBridge(context) {
            isLoaded = true
            onReady()
        }
    }

    // Reactively send state updates to WebGL 3D scene
    LaunchedEffect(
        state.activity,
        state.currentTaskTitle,
        state.currentTaskProgress,
        state.activeCodeSnippet,
        isLoaded
    ) {
        if (!isLoaded || webViewRef == null) return@LaunchedEffect
        val safeActivity = state.activity.name
        val safeTitle = JSONObject.quote(state.currentTaskTitle)
        val safeCode = JSONObject.quote(state.activeCodeSnippet)
        val progress = state.currentTaskProgress
        val js = "window.HasseenaBridge && window.HasseenaBridge.setActivity('$safeActivity', $safeTitle, $safeCode, $progress);"
        webViewRef?.evaluateJavascript(js, null)
    }

    LaunchedEffect(state.audioLevel, isLoaded) {
        if (!isLoaded || webViewRef == null) return@LaunchedEffect
        val js = "window.HasseenaBridge && window.HasseenaBridge.setSpeechAudioLevel(${state.audioLevel});"
        webViewRef?.evaluateJavascript(js, null)
    }

    LaunchedEffect(state.viseme, state.audioLevel, isLoaded) {
        if (!isLoaded || webViewRef == null) return@LaunchedEffect
        val v = state.viseme
        val js = "window.HasseenaBridge && window.HasseenaBridge.setViseme(${v.id}, ${v.mouthOpen}, ${v.mouthSmile}, ${state.audioLevel}, '${v.phoneme}');"
        webViewRef?.evaluateJavascript(js, null)
    }

    LaunchedEffect(state.cameraPreset, isLoaded) {
        if (!isLoaded || webViewRef == null) return@LaunchedEffect
        val js = "window.HasseenaBridge && window.HasseenaBridge.setCameraAngle('${state.cameraPreset.id}');"
        webViewRef?.evaluateJavascript(js, null)
    }

    LaunchedEffect(state.avatarStyle, isLoaded) {
        if (!isLoaded || webViewRef == null) return@LaunchedEffect
        val style = state.avatarStyle
        val js = "window.HasseenaBridge && window.HasseenaBridge.setCustomization('${style.hairHex}', '${style.blazerHex}', '${style.neonHex}');"
        webViewRef?.evaluateJavascript(js, null)
    }

    LaunchedEffect(state.isAnimationPaused, isLoaded) {
        if (!isLoaded || webViewRef == null) return@LaunchedEffect
        val js = "window.HasseenaBridge && window.HasseenaBridge.setAnimationPaused(${state.isAnimationPaused});"
        webViewRef?.evaluateJavascript(js, null)
    }

    DisposableEffect(lifecycleOwner, webViewRef) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> webViewRef?.onResume()
                Lifecycle.Event.ON_PAUSE -> webViewRef?.onPause()
                Lifecycle.Event.ON_DESTROY -> {
                    webViewRef?.destroy()
                    webViewRef = null
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Box(modifier = modifier.fillMaxSize().background(Color(0xFF050914))) {
        if (hasError) {
            // Hardware/WebGL fallback to native OpenGL ES renderer
            OpenGlLensPreview(modifier = Modifier.fillMaxSize())
        } else {
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        setLayerType(View.LAYER_TYPE_HARDWARE, null)
                        setBackgroundColor(android.graphics.Color.TRANSPARENT)
                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            allowFileAccess = true
                            allowContentAccess = true
                            @Suppress("DEPRECATION")
                            allowFileAccessFromFileURLs = true
                            @Suppress("DEPRECATION")
                            allowUniversalAccessFromFileURLs = true
                            cacheMode = WebSettings.LOAD_DEFAULT
                            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                        }
                        webChromeClient = WebChromeClient()
                        webViewClient = object : WebViewClient() {
                            override fun onReceivedError(
                                view: WebView?,
                                errorCode: Int,
                                description: String?,
                                failingUrl: String?
                            ) {
                                hasError = true
                            }
                        }
                        addJavascriptInterface(bridge, "AndroidBridge")
                        loadUrl("file:///android_asset/avatar3d/workstation.html")
                        webViewRef = this
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            if (!isLoaded) {
                Box(
                    modifier = Modifier.fillMaxSize().background(Color(0xCC050914)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFF00F0FF))
                    Text(
                        text = "INITIALIZING 3D PBR AVATAR...",
                        color = Color(0xFF7DE7FF),
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )
                }
            }
        }
    }
}
