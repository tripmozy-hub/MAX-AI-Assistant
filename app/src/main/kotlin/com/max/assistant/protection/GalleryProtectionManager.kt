package com.max.assistant.protection

import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object GalleryProtectionManager {
    private const val TAG = "GalleryProtection"

    private val _isProtectionActive = MutableStateFlow(true)
    val isProtectionActive: StateFlow<Boolean> = _isProtectionActive.asStateFlow()

    private val _lastBlockedEvent = MutableStateFlow<String?>(null)
    val lastBlockedEvent: StateFlow<String?> = _lastBlockedEvent.asStateFlow()

    private val BLOCKED_PACKAGES = setOf(
        "com.sec.android.gallery3d",           // Samsung Gallery
        "com.google.android.apps.photos",      // Google Photos
        "com.android.gallery3d",               // AOSP Gallery
        "com.samsung.android.gallery.app",
        "com.simplemobiletools.gallery",
        "com.simplemobiletools.gallery.pro"
    )

    fun setProtectionActive(active: Boolean) {
        _isProtectionActive.value = active
        Log.i(TAG, "Gallery Protection set to: $active")
    }

    fun isGalleryPackage(packageName: String?): Boolean {
        if (packageName == null) return false
        val lower = packageName.lowercase()
        return BLOCKED_PACKAGES.contains(lower) || 
               (lower.contains("gallery") && !lower.contains("max")) || 
               lower.contains("photos")
    }

    fun shouldBlockVoiceLaunch(appNameOrCommand: String): Boolean {
        if (!_isProtectionActive.value) return false
        val lower = appNameOrCommand.lowercase()
        val isGallery = lower.contains("gallery") || 
                        lower.contains("photo") || 
                        lower.contains("pictures") || 
                        lower.contains("camera roll") ||
                        lower.contains("dcim") ||
                        lower.contains("album") ||
                        lower.contains("tasveer") ||
                        lower.contains("chobi") ||
                        lower.contains("গ্যালারি") ||
                        lower.contains("ছবি") ||
                        lower.contains("ফটো") ||
                        lower.contains("অ্যালবাম") ||
                        lower.contains("गैलरी") ||
                        lower.contains("फोटो") ||
                        lower.contains("तस्वीर")

        if (isGallery) {
            _lastBlockedEvent.value = "Blocked unauthorized gallery request: $appNameOrCommand"
            Log.w(TAG, "Voice launch blocked: $appNameOrCommand")
        }
        return isGallery
    }

    fun interceptAndBlockLaunch(context: Context, detectedPackage: String) {
        if (!_isProtectionActive.value) return

        Log.w(TAG, "CRITICAL: Intercepting gallery launch for package: $detectedPackage")
        _lastBlockedEvent.value = "Gallery intercepted: $detectedPackage"

        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        context.startActivity(homeIntent)
    }
}
