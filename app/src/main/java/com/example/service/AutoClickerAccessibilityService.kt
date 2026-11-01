package com.example.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Context
import android.graphics.Path
import android.provider.Settings
import android.text.TextUtils
import android.view.accessibility.AccessibilityEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

class AutoClickerAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        _isServiceActive.value = true
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Dedicated solely to user-authorized automation gestures; no passive telemetry collection
    }

    override fun onInterrupt() {
        _isServiceActive.value = false
        if (instance == this) {
            instance = null
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _isServiceActive.value = false
        if (instance == this) {
            instance = null
        }
    }

    suspend fun performClick(x: Float, y: Float, durationMs: Long = 50L): Boolean {
        return withTimeoutOrNull(2500L) {
            suspendCancellableCoroutine { continuation ->
                val path = Path().apply {
                    moveTo(x.coerceAtLeast(0f), y.coerceAtLeast(0f))
                }
                val stroke = GestureDescription.StrokeDescription(path, 0L, durationMs.coerceIn(10L, 5000L))
                val gesture = GestureDescription.Builder().addStroke(stroke).build()

                val dispatched = dispatchGesture(
                    gesture,
                    object : GestureResultCallback() {
                        override fun onCompleted(gestureDescription: GestureDescription?) {
                            if (continuation.isActive) continuation.resume(true)
                        }

                        override fun onCancelled(gestureDescription: GestureDescription?) {
                            if (continuation.isActive) continuation.resume(false)
                        }
                    },
                    null
                )
                if (!dispatched && continuation.isActive) {
                    continuation.resume(false)
                }
            }
        } ?: false
    }

    suspend fun performSwipe(
        startX: Float,
        startY: Float,
        endX: Float,
        endY: Float,
        durationMs: Long = 300L
    ): Boolean {
        return withTimeoutOrNull(3000L) {
            suspendCancellableCoroutine { continuation ->
                val path = Path().apply {
                    moveTo(startX.coerceAtLeast(0f), startY.coerceAtLeast(0f))
                    lineTo(endX.coerceAtLeast(0f), endY.coerceAtLeast(0f))
                }
                val stroke = GestureDescription.StrokeDescription(path, 0L, durationMs.coerceIn(50L, 5000L))
                val gesture = GestureDescription.Builder().addStroke(stroke).build()

                val dispatched = dispatchGesture(
                    gesture,
                    object : GestureResultCallback() {
                        override fun onCompleted(gestureDescription: GestureDescription?) {
                            if (continuation.isActive) continuation.resume(true)
                        }

                        override fun onCancelled(gestureDescription: GestureDescription?) {
                            if (continuation.isActive) continuation.resume(false)
                        }
                    },
                    null
                )
                if (!dispatched && continuation.isActive) {
                    continuation.resume(false)
                }
            }
        } ?: false
    }

    companion object {
        var instance: AutoClickerAccessibilityService? = null
            private set

        private val _isServiceActive = MutableStateFlow(false)
        val isServiceActive: StateFlow<Boolean> = _isServiceActive.asStateFlow()

        val isConnected: Boolean
            get() = instance != null && _isServiceActive.value

        suspend fun testServiceConnection(): Boolean {
            val service = instance ?: return false
            return try {
                service.performClick(10f, 10f, 25L)
            } catch (e: Exception) {
                false
            }
        }

        fun isServiceEnabledInSettings(context: Context): Boolean {
            val expectedServiceName = "${context.packageName}/${AutoClickerAccessibilityService::class.java.canonicalName}"
            val enabledServices = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false

            val colonSplitter = TextUtils.SimpleStringSplitter(':')
            colonSplitter.setString(enabledServices)
            while (colonSplitter.hasNext()) {
                val componentName = colonSplitter.next()
                if (componentName.equals(expectedServiceName, ignoreCase = true) ||
                    componentName.contains("AutoClickerAccessibilityService", ignoreCase = true)
                ) {
                    return true
                }
            }
            return false
        }
    }
}
