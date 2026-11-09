package com.max.assistant.dictionary

import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.provider.Settings
import android.util.Log

/**
 * AndroidDeviceController manages hardware features: flashlight, audio volume, brightness, and settings pages.
 */
class AndroidDeviceController(private val context: Context) {

    companion object {
        private const val TAG = "DeviceController"
    }

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager

    fun setFlashlight(enabled: Boolean): Boolean {
        val cm = cameraManager ?: return false
        return try {
            val cameraId = cm.cameraIdList.firstOrNull() ?: return false
            cm.setTorchMode(cameraId, enabled)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed setting flashlight: ${e.message}")
            false
        }
    }

    fun adjustVolume(direction: Int): Boolean { // +1 = up, -1 = down, 0 = mute
        val am = audioManager ?: return false
        return try {
            val dir = when {
                direction > 0 -> AudioManager.ADJUST_RAISE
                direction < 0 -> AudioManager.ADJUST_LOWER
                else -> AudioManager.ADJUST_MUTE
            }
            am.adjustStreamVolume(AudioManager.STREAM_MUSIC, dir, AudioManager.FLAG_SHOW_UI)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed adjusting volume: ${e.message}")
            false
        }
    }

    fun openSettings(subpage: String = ""): Boolean {
        val action = when (subpage.lowercase().trim()) {
            "wifi" -> Settings.ACTION_WIFI_SETTINGS
            "bluetooth" -> Settings.ACTION_BLUETOOTH_SETTINGS
            "display", "brightness" -> Settings.ACTION_DISPLAY_SETTINGS
            "sound", "volume" -> Settings.ACTION_SOUND_SETTINGS
            "accessibility" -> Settings.ACTION_ACCESSIBILITY_SETTINGS
            "location" -> Settings.ACTION_LOCATION_SOURCE_SETTINGS
            else -> Settings.ACTION_SETTINGS
        }
        return try {
            val intent = Intent(action).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            false
        }
    }
}
