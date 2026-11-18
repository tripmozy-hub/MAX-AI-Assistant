package com.max.assistant.service

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.max.assistant.protection.GalleryProtectionManager
import com.max.assistant.protection.PaymentProtectionManager

class MaxAccessibilityGuardService : AccessibilityService() {

    companion object {
        private const val TAG = "MaxAccessibilityGuard"

        @Volatile
        var instance: MaxAccessibilityGuardService? = null
            private set

        @Volatile
        var isServiceActive = false
            private set

        @Volatile
        var pendingAutoSendWhatsApp = false

        @Volatile
        var autoSendTimestamp = 0L

        fun requestWhatsAppAutoSend() {
            pendingAutoSendWhatsApp = true
            autoSendTimestamp = System.currentTimeMillis()
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        isServiceActive = true
        Log.i(TAG, "MaxAccessibilityGuardService connected and active.")
    }

    override fun onDestroy() {
        super.onDestroy()
        if (instance == this) {
            instance = null
        }
        isServiceActive = false
        Log.i(TAG, "MaxAccessibilityGuardService destroyed.")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val packageName = event.packageName?.toString() ?: return

        // 1. Strict Gallery Protection Check (Intercept any gallery launch attempts)
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            if (GalleryProtectionManager.isGalleryPackage(packageName)) {
                Log.w(TAG, "INTERCEPTED GALLERY LAUNCH: $packageName")

                // Immediately send user back to home
                performGlobalAction(GLOBAL_ACTION_HOME)

                // Announce security intervention
                GalleryProtectionManager.interceptAndBlockLaunch(this, packageName)
                MaxForegroundService.activeInstance?.let { service ->
                    service.voiceEngine.speak("Security alert. Gallery launch intercepted and blocked.")
                }
                return
            }

            // 1b. Payment & Financial Application Safety Isolation
            if (PaymentProtectionManager.isPaymentPackage(packageName)) {
                if (pendingAutoSendWhatsApp) {
                    pendingAutoSendWhatsApp = false
                    Log.w(TAG, "Security Guard: Suppressed background automation due to active financial app: $packageName")
                }
            }
        }

        // 2. Automated WhatsApp Message Dispatcher
        if (pendingAutoSendWhatsApp) {
            val elapsed = System.currentTimeMillis() - autoSendTimestamp
            if (elapsed > 10000L) {
                // Timeout after 10 seconds to avoid stray clicks
                pendingAutoSendWhatsApp = false
                Log.d(TAG, "WhatsApp auto-send request expired after 10s.")
            } else if (packageName == "com.whatsapp" || packageName == "com.whatsapp.w4b") {
                val clicked = tryAutoClickWhatsAppSend()
                if (clicked) {
                    pendingAutoSendWhatsApp = false
                    Log.i(TAG, "MAX successfully auto-clicked WhatsApp send button!")
                    MaxForegroundService.activeInstance?.addLog("WhatsApp message auto-sent by MAX.")
                }
            }
        }
    }

    private fun tryAutoClickWhatsAppSend(): Boolean {
        val root = rootInActiveWindow ?: return false
        try {
            // Priority 1: Check by exact WhatsApp send view ID
            val byId = root.findAccessibilityNodeInfosByViewId("com.whatsapp:id/send")
            for (node in byId) {
                if (performClick(node)) return true
            }

            // Priority 2: Check for WhatsApp Business view ID
            val byIdW4b = root.findAccessibilityNodeInfosByViewId("com.whatsapp.w4b:id/send")
            for (node in byIdW4b) {
                if (performClick(node)) return true
            }

            // Priority 3: Check generic send view ID
            val byGenericId = root.findAccessibilityNodeInfosByViewId("send")
            for (node in byGenericId) {
                if (performClick(node)) return true
            }

            // Priority 4: Search by standard Send descriptions and texts (multilingual)
            val sendKeywords = listOf("Send", "send", "भेजें", "পাঠান", "Send message", "संदेश भेजें", "বার্তা পাঠান")
            for (kw in sendKeywords) {
                val nodes = root.findAccessibilityNodeInfosByText(kw)
                for (node in nodes) {
                    val desc = node.contentDescription?.toString()
                    val txt = node.text?.toString()
                    if ((desc != null && desc.contains(kw, ignoreCase = true)) ||
                        (txt != null && txt.contains(kw, ignoreCase = true))) {
                        if (performClick(node)) return true
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error while searching for WhatsApp send button: ${e.message}")
        } finally {
            try {
                root.recycle()
            } catch (_: Exception) {}
        }
        return false
    }

    private fun performClick(node: AccessibilityNodeInfo): Boolean {
        var curr: AccessibilityNodeInfo? = node
        while (curr != null) {
            if (curr.isClickable) {
                val result = curr.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                if (result) return true
            }
            curr = curr.parent
        }
        return false
    }

    fun takeScreenshotCompat(callback: (android.graphics.Bitmap?) -> Unit) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            try {
                takeScreenshot(
                    android.view.Display.DEFAULT_DISPLAY,
                    mainExecutor,
                    object : TakeScreenshotCallback {
                        override fun onSuccess(screenshot: ScreenshotResult) {
                            try {
                                val bitmap = android.graphics.Bitmap.wrapHardwareBuffer(
                                    screenshot.hardwareBuffer,
                                    screenshot.colorSpace
                                )
                                val copy = bitmap?.copy(android.graphics.Bitmap.Config.ARGB_8888, false)
                                screenshot.hardwareBuffer.close()
                                callback(copy ?: bitmap)
                            } catch (e: Exception) {
                                Log.e(TAG, "Error processing hardware buffer screenshot: ${e.message}")
                                callback(null)
                            }
                        }

                        override fun onFailure(errorCode: Int) {
                            Log.w(TAG, "Accessibility takeScreenshot failed with code $errorCode")
                            callback(null)
                        }
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Exception calling takeScreenshot: ${e.message}")
                callback(null)
            }
        } else {
            callback(null)
        }
    }

    override fun onInterrupt() {
        Log.w(TAG, "MaxAccessibilityGuardService interrupted.")
    }
}
