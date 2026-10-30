package com.example.services.accessibility

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.os.Build
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class HasseenaAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        _isRunning.value = true
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        val pkg = event.packageName?.toString()
        if (!pkg.isNullOrBlank()) {
            _activePackageName.value = pkg
        }
    }

    override fun onInterrupt() {
        // No covert monitoring; interrupt handled cleanly
    }

    override fun onDestroy() {
        super.onDestroy()
        if (instance === this) {
            instance = null
            _isRunning.value = false
        }
    }

    /**
     * Reads visible text and interactive elements from the active window only when explicitly
     * requested by the user ("What is on my screen?", "Read this page").
     */
    fun readVisibleScreenContent(): ScreenReadSnapshot {
        val root = rootInActiveWindow
            ?: return ScreenReadSnapshot(
                available = false,
                packageName = _activePackageName.value,
                visibleTexts = emptyList(),
                clickableLabels = emptyList(),
                summary = "No active window content available right now."
            )

        val visibleTexts = mutableListOf<String>()
        val clickableLabels = mutableListOf<String>()
        collectNodes(root, visibleTexts, clickableLabels, depth = 0)

        val distinctTexts = visibleTexts.distinct().take(40)
        val distinctClickables = clickableLabels.distinct().take(25)

        val summary = if (distinctTexts.isEmpty()) {
            "The current screen (${_activePackageName.value.ifBlank { "Active App" }}) has no readable text elements."
        } else {
            distinctTexts.joinToString(separator = " • ")
        }

        return ScreenReadSnapshot(
            available = true,
            packageName = _activePackageName.value,
            visibleTexts = distinctTexts,
            clickableLabels = distinctClickables,
            summary = summary
        )
    }

    private fun collectNodes(
        node: AccessibilityNodeInfo?,
        textsOut: MutableList<String>,
        clickablesOut: MutableList<String>,
        depth: Int
    ) {
        if (node == null || depth > 18) return
        // Never read password fields for user security
        if (node.isPassword) return

        val text = node.text?.toString()?.trim()
        val desc = node.contentDescription?.toString()?.trim()

        if (!text.isNullOrBlank() && text.length >= 2) {
            textsOut.add(text)
        } else if (!desc.isNullOrBlank() && desc.length >= 2) {
            textsOut.add(desc)
        }

        if (node.isClickable) {
            val label = text?.takeIf { it.isNotBlank() } ?: desc?.takeIf { it.isNotBlank() }
            if (label != null) {
                clickablesOut.add(label)
            }
        }

        val childCount = node.childCount
        for (i in 0 until childCount) {
            collectNodes(node.getChild(i), textsOut, clickablesOut, depth + 1)
        }
    }

    fun navigateBack(): Boolean = performGlobalAction(GLOBAL_ACTION_BACK)

    fun navigateHome(): Boolean = performGlobalAction(GLOBAL_ACTION_HOME)

    fun openRecents(): Boolean = performGlobalAction(GLOBAL_ACTION_RECENTS)

    fun openNotificationsShade(): Boolean = performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)

    fun takeScreenshotAction(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            performGlobalAction(GLOBAL_ACTION_TAKE_SCREENSHOT)
        } else {
            false
        }
    }

    fun scrollScreen(forward: Boolean): Boolean {
        val root = rootInActiveWindow ?: return false
        val scrollable = findScrollableNode(root) ?: return false
        val action = if (forward) {
            AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_FORWARD.id
        } else {
            AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_BACKWARD.id
        }
        return scrollable.performAction(action)
    }

    private fun findScrollableNode(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        if (node == null) return null
        if (node.isScrollable) return node
        for (i in 0 until node.childCount) {
            val found = findScrollableNode(node.getChild(i))
            if (found != null) return found
        }
        return null
    }

    fun clickControlByLabel(targetLabel: String): Boolean {
        val root = rootInActiveWindow ?: return false
        val normalized = targetLabel.trim().lowercase(Locale.US)
        val targetNode = findMatchingClickableNode(root, normalized) ?: return false
        return targetNode.performAction(AccessibilityNodeInfo.ACTION_CLICK)
    }

    private fun findMatchingClickableNode(
        node: AccessibilityNodeInfo?,
        targetLower: String
    ): AccessibilityNodeInfo? {
        if (node == null || node.isPassword) return null
        val text = node.text?.toString()?.trim()?.lowercase(Locale.US) ?: ""
        val desc = node.contentDescription?.toString()?.trim()?.lowercase(Locale.US) ?: ""
        if (text.contains(targetLower) || desc.contains(targetLower)) {
            var current: AccessibilityNodeInfo? = node
            while (current != null) {
                if (current.isClickable) return current
                current = current.parent
            }
        }
        for (i in 0 until node.childCount) {
            val found = findMatchingClickableNode(node.getChild(i), targetLower)
            if (found != null) return found
        }
        return null
    }

    fun dispatchTapGesture(x: Float, y: Float, onComplete: ((Boolean) -> Unit)? = null): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return false
        val path = android.graphics.Path().apply {
            moveTo(x, y)
        }
        val stroke = android.accessibilityservice.GestureDescription.StrokeDescription(path, 0, 50)
        val gesture = android.accessibilityservice.GestureDescription.Builder()
            .addStroke(stroke)
            .build()
        return dispatchGesture(
            gesture,
            object : AccessibilityService.GestureResultCallback() {
                override fun onCompleted(gestureDescription: android.accessibilityservice.GestureDescription?) {
                    onComplete?.invoke(true)
                }

                override fun onCancelled(gestureDescription: android.accessibilityservice.GestureDescription?) {
                    onComplete?.invoke(false)
                }
            },
            null
        )
    }

    fun dispatchSwipeGesture(
        startX: Float,
        startY: Float,
        endX: Float,
        endY: Float,
        durationMs: Long = 300,
        onComplete: ((Boolean) -> Unit)? = null
    ): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return false
        val path = android.graphics.Path().apply {
            moveTo(startX, startY)
            lineTo(endX, endY)
        }
        val stroke = android.accessibilityservice.GestureDescription.StrokeDescription(path, 0, durationMs)
        val gesture = android.accessibilityservice.GestureDescription.Builder()
            .addStroke(stroke)
            .build()
        return dispatchGesture(
            gesture,
            object : AccessibilityService.GestureResultCallback() {
                override fun onCompleted(gestureDescription: android.accessibilityservice.GestureDescription?) {
                    onComplete?.invoke(true)
                }

                override fun onCancelled(gestureDescription: android.accessibilityservice.GestureDescription?) {
                    onComplete?.invoke(false)
                }
            },
            null
        )
    }

    data class ScreenReadSnapshot(
        val available: Boolean,
        val packageName: String,
        val visibleTexts: List<String>,
        val clickableLabels: List<String>,
        val summary: String
    )

    companion object {
        @Volatile
        var instance: HasseenaAccessibilityService? = null
            private set

        private val _isRunning = MutableStateFlow(false)
        val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

        private val _activePackageName = MutableStateFlow("")
        val activePackageName: StateFlow<String> = _activePackageName.asStateFlow()

        fun isAccessibilityServiceEnabled(context: Context): Boolean {
            if (_isRunning.value && instance != null) return true
            val enabledServices = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false
            return enabledServices.contains(context.packageName, ignoreCase = true)
        }
    }
}
