package com.max.assistant.appcontrol

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.util.Log
import com.max.assistant.accessibility.AndroidAccessibilityController
import com.max.assistant.accessibility.UiElement
import com.max.assistant.launcher.AppLauncher
import com.max.assistant.protection.GalleryProtectionManager
import com.max.assistant.tts.VoiceLanguage
import kotlinx.coroutines.delay

class AppInteractionManager(
    private val context: Context,
    val accessibilityController: AndroidAccessibilityController = AndroidAccessibilityController(context),
    val appLauncher: AppLauncher = AppLauncher(context)
) {

    companion object {
        private const val TAG = "AppInteractionManager"
    }

    // --- 1. WhatsApp Operations ---

    suspend fun openWhatsAppChat(contactName: String): Boolean {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("whatsapp://send?text=")
                `package` = "com.whatsapp"
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            appLauncher.launchAppByVoice("whatsapp")
        }

        delay(1200L) // Wait for WhatsApp main window

        val searchClicked = accessibilityController.clickByDescription("Search") ||
                accessibilityController.clickByDescription("खोजें") ||
                accessibilityController.clickByDescription("অনুসন্ধান") ||
                accessibilityController.clickByViewId("com.whatsapp:id/search_holder") ||
                accessibilityController.clickByViewId("com.whatsapp:id/menuitem_search")

        if (searchClicked) {
            delay(500L)
            accessibilityController.typeText(contactName)
            delay(800L)
            val clicked = accessibilityController.clickByText(contactName, exact = false)
            Log.i(TAG, "WhatsApp open chat '$contactName': $clicked")
            return clicked
        }
        return false
    }

    suspend fun typeWhatsAppMessage(message: String): Boolean {
        val typed = accessibilityController.typeText(message)
        Log.i(TAG, "WhatsApp typed message: '$message', success: $typed")
        return typed
    }

    fun findUnreadWhatsAppMessages(): String {
        val text = accessibilityController.extractVisibleText()
        val lines = text.lines().filter { it.isNotBlank() }
        val unreadLines = lines.filter { line ->
            line.contains("unread", ignoreCase = true) ||
                    line.contains("बिना पढ़े", ignoreCase = true) ||
                    line.contains("না পড়া", ignoreCase = true)
        }
        return if (unreadLines.isNotEmpty()) {
            "You have unread WhatsApp messages: ${unreadLines.joinToString("; ")}"
        } else {
            "No obvious unread message indicators visible on current WhatsApp screen."
        }
    }

    // --- 2. Chrome Operations ---

    suspend fun searchInChrome(query: String): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_WEB_SEARCH).apply {
                putExtra("query", query)
                `package` = "com.android.chrome"
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Log.i(TAG, "Launched Chrome web search for '$query'")
            true
        } catch (_: Exception) {
            val url = "https://www.google.com/search?q=" + Uri.encode(query)
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                `package` = "com.android.chrome"
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        }
    }

    suspend fun openFirstChromeResult(): Boolean {
        delay(1500L)
        val clickable = accessibilityController.findInteractiveElements()
        for (el in clickable) {
            if (el.text.length > 10 && !el.text.contains("google", ignoreCase = true) && !el.text.contains("search", ignoreCase = true)) {
                val ok = accessibilityController.clickNode(el.nodeInfo)
                if (ok) {
                    Log.i(TAG, "Clicked first search result: '${el.text}'")
                    return true
                }
            }
        }
        return accessibilityController.clickNthOption(2)
    }

    fun scrollChrome(down: Boolean = true): Boolean {
        return if (down) {
            accessibilityController.scrollForward()
        } else {
            accessibilityController.scrollBackward()
        }
    }

    // --- 3. YouTube Operations ---

    suspend fun searchAndPlayYouTube(query: String): Boolean {
        try {
            val intent = Intent(Intent.ACTION_SEARCH).apply {
                setPackage("com.google.android.youtube")
                putExtra("query", query)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/results?search_query=" + Uri.encode(query))).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        }

        delay(1800L)
        val elements = accessibilityController.findInteractiveElements()
        val videoResult = elements.firstOrNull {
            it.contentDescription.contains("video", ignoreCase = true) ||
                    it.contentDescription.contains("views", ignoreCase = true) ||
                    it.contentDescription.contains("ago", ignoreCase = true) ||
                    it.text.contains(query.split(" ").firstOrNull() ?: query, ignoreCase = true)
        }

        if (videoResult != null) {
            val clicked = accessibilityController.clickNode(videoResult.nodeInfo)
            Log.i(TAG, "Clicked first YouTube video result: '${videoResult.text.ifBlank { videoResult.contentDescription }}'")
            return clicked
        }
        return accessibilityController.clickNthOption(3)
    }

    fun controlYouTubePlayback(action: String): Boolean {
        val isPause = action.contains("pause", ignoreCase = true) || action.contains("roko", ignoreCase = true) || action.contains("band", ignoreCase = true)
        return if (isPause) {
            accessibilityController.clickByDescription("Pause video") ||
                    accessibilityController.clickByDescription("रोकें") ||
                    accessibilityController.clickByDescription("থামান")
        } else {
            accessibilityController.clickByDescription("Play video") ||
                    accessibilityController.clickByDescription("चलाएं") ||
                    accessibilityController.clickByDescription("চালান")
        }
    }

    // --- 4. Gmail Operations ---

    fun openGmail(): Boolean {
        return try {
            val intent = context.packageManager.getLaunchIntentForPackage("com.google.android.gm")?.apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (intent != null) {
                context.startActivity(intent)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open Gmail: ${e.message}")
            false
        }
    }

    suspend fun composeGmail(recipient: String, subject: String, body: String): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:" + Uri.encode(recipient))
                putExtra(Intent.EXTRA_SUBJECT, subject)
                putExtra(Intent.EXTRA_TEXT, body)
                `package` = "com.google.android.gm"
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            val fallback = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$recipient")).apply {
                putExtra(Intent.EXTRA_SUBJECT, subject)
                putExtra(Intent.EXTRA_TEXT, body)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(fallback)
            true
        }
    }

    suspend fun searchGmail(query: String): Boolean {
        openGmail()
        delay(1200L)
        val searchClicked = accessibilityController.clickByDescription("Search in mail") ||
                accessibilityController.clickByText("Search in mail") ||
                accessibilityController.clickByText("ईमेल में खोजें") ||
                accessibilityController.clickByViewId("com.google.android.gm:id/open_search_bar")
        if (searchClicked) {
            delay(500L)
            return accessibilityController.typeText(query)
        }
        return false
    }

    // --- 5. Phone / Contacts Operations ---

    fun dialNumber(phoneNumber: String): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phoneNumber")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to dial number: ${e.message}")
            false
        }
    }

    suspend fun searchContactInDialer(contactName: String): Boolean {
        val launched = appLauncher.launchAppByVoice("phone")
        delay(1000L)
        val searchClicked = accessibilityController.clickByDescription("Search contacts") ||
                accessibilityController.clickByText("Search contacts") ||
                accessibilityController.clickByDescription("खोजें") ||
                accessibilityController.clickByText("संपर्क खोजें")
        if (searchClicked) {
            delay(400L)
            return accessibilityController.typeText(contactName)
        }
        return false
    }

    // --- 6. Maps Operations ---

    fun openMapsNavigation(destination: String): Boolean {
        return try {
            val uri = Uri.parse("google.navigation:q=" + Uri.encode(destination))
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                `package` = "com.google.android.apps.maps"
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open navigation for $destination: ${e.message}")
            false
        }
    }

    suspend fun searchNearbyPlaces(placeType: String): Boolean {
        return try {
            val uri = Uri.parse("geo:0,0?q=" + Uri.encode(placeType))
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                `package` = "com.google.android.apps.maps"
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            false
        }
    }

    // --- 7. Settings Operations ---

    fun openBluetoothSettings(): Boolean {
        return try {
            val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open bluetooth settings: ${e.message}")
            false
        }
    }

    suspend fun toggleBluetoothOn(): Boolean {
        openBluetoothSettings()
        delay(1000L)
        val switchClicked = accessibilityController.clickByText("Off") ||
                accessibilityController.clickByText("बंद") ||
                accessibilityController.clickByDescription("Bluetooth") ||
                accessibilityController.clickByText("Bluetooth")
        Log.i(TAG, "Toggled Bluetooth switch: $switchClicked")
        return switchClicked
    }

    fun openWifiSettings(): Boolean {
        return try {
            val intent = Intent(Settings.ACTION_WIFI_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open wifi settings: ${e.message}")
            false
        }
    }

    fun openDisplaySettings(): Boolean {
        return try {
            val intent = Intent(Settings.ACTION_DISPLAY_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun openSoundSettings(): Boolean {
        return try {
            val intent = Intent(Settings.ACTION_SOUND_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun openBatterySettings(): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_POWER_USAGE_SUMMARY).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
    }

    // --- 8. UI Verification & Error Recovery ---

    suspend fun verifyActionOutcome(expectedKeyword: String, timeoutMs: Long = 2000L): Boolean {
        val startTime = System.currentTimeMillis()
        while (System.currentTimeMillis() - startTime < timeoutMs) {
            val visibleText = accessibilityController.extractVisibleText()
            if (visibleText.contains(expectedKeyword, ignoreCase = true)) {
                return true
            }
            delay(300L)
        }
        return false
    }

    // --- 9. Form Filling Operations ---

    fun inspectVisibleFormFields(): List<UiElement> {
        val root = accessibilityController.getRootNode() ?: return emptyList()
        val all = mutableListOf<UiElement>()
        collectFormFields(root, all)
        return all
    }

    private fun collectFormFields(node: android.view.accessibility.AccessibilityNodeInfo?, outList: MutableList<UiElement>) {
        if (node == null || !node.isVisibleToUser) return
        if (node.isEditable || node.isCheckable || node.className.contains("EditText", ignoreCase = true) || node.className.contains("CheckBox", ignoreCase = true)) {
            val rect = android.graphics.Rect()
            node.getBoundsInScreen(rect)
            outList.add(
                UiElement(
                    text = node.text?.toString() ?: "",
                    contentDescription = node.contentDescription?.toString() ?: "",
                    viewId = node.viewIdResourceName?.toString() ?: "",
                    className = node.className?.toString() ?: "",
                    isClickable = node.isClickable,
                    isEditable = node.isEditable,
                    bounds = rect,
                    nodeInfo = node
                )
            )
        }
        for (i in 0 until node.childCount) {
            collectFormFields(node.getChild(i), outList)
        }
    }

    fun fillFormField(labelOrHint: String, value: String): Boolean {
        val fields = inspectVisibleFormFields()
        val target = fields.firstOrNull {
            it.text.contains(labelOrHint, ignoreCase = true) ||
                    it.contentDescription.contains(labelOrHint, ignoreCase = true) ||
                    it.viewId.contains(labelOrHint, ignoreCase = true)
        } ?: fields.firstOrNull { it.isEditable }

        if (target != null) {
            return accessibilityController.typeText(value, target.nodeInfo)
        }
        return false
    }
}
