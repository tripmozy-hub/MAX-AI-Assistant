package com.max.assistant.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Path
import android.graphics.Rect
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import com.max.assistant.service.MaxAccessibilityGuardService

data class UiElement(
    val text: String,
    val contentDescription: String,
    val viewId: String,
    val className: String,
    val isClickable: Boolean,
    val isEditable: Boolean,
    val bounds: Rect,
    val nodeInfo: AccessibilityNodeInfo
)

/**
 * AndroidAccessibilityController is the central bridge between MAX's AI engines
 * and real on-screen Android application UI components.
 */
class AndroidAccessibilityController(private val context: Context) {

    companion object {
        private const val TAG = "AccessibilityController"
    }

    val isServiceActive: Boolean
        get() = MaxAccessibilityGuardService.isServiceActive && MaxAccessibilityGuardService.instance != null

    private val serviceInstance: MaxAccessibilityGuardService?
        get() = MaxAccessibilityGuardService.instance

    fun openAccessibilitySettings() {
        try {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open accessibility settings: ${e.message}")
        }
    }

    fun getRootNode(): AccessibilityNodeInfo? {
        val service = serviceInstance ?: return null
        return try {
            service.rootInActiveWindow
        } catch (e: Exception) {
            Log.w(TAG, "Error getting root node: ${e.message}")
            null
        }
    }

    fun clickByText(targetText: String, exact: Boolean = false): Boolean {
        val root = getRootNode() ?: return false
        try {
            val matches = root.findAccessibilityNodeInfosByText(targetText)
            for (node in matches) {
                val nodeText = node.text?.toString() ?: ""
                val nodeDesc = node.contentDescription?.toString() ?: ""
                val matchesCondition = if (exact) {
                    nodeText.equals(targetText, ignoreCase = true) || nodeDesc.equals(targetText, ignoreCase = true)
                } else {
                    nodeText.contains(targetText, ignoreCase = true) || nodeDesc.contains(targetText, ignoreCase = true)
                }

                if (matchesCondition) {
                    if (clickNode(node)) {
                        Log.i(TAG, "Successfully clicked element matching text: '$targetText'")
                        return true
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error clicking by text '$targetText': ${e.message}")
        }
        return false
    }

    fun clickByDescription(description: String, exact: Boolean = false): Boolean {
        val root = getRootNode() ?: return false
        val allElements = mutableListOf<UiElement>()
        collectAllElements(root, allElements)
        for (el in allElements) {
            val matches = if (exact) {
                el.contentDescription.equals(description, ignoreCase = true)
            } else {
                el.contentDescription.contains(description, ignoreCase = true)
            }
            if (matches) {
                if (clickNode(el.nodeInfo)) {
                    Log.i(TAG, "Successfully clicked element matching description: '$description'")
                    return true
                }
            }
        }
        return false
    }

    fun clickByViewId(viewId: String): Boolean {
        val root = getRootNode() ?: return false
        try {
            val matches = root.findAccessibilityNodeInfosByViewId(viewId)
            for (node in matches) {
                if (clickNode(node)) {
                    Log.i(TAG, "Successfully clicked element matching viewId: '$viewId'")
                    return true
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error clicking by viewId '$viewId': ${e.message}")
        }
        return false
    }

    fun clickNthOption(index1Based: Int): Boolean {
        val root = getRootNode() ?: return false
        val clickable = mutableListOf<UiElement>()
        collectAllElements(root, clickable, clickableOnly = true)
        val targetIndex = index1Based - 1
        if (targetIndex in clickable.indices) {
            val target = clickable[targetIndex]
            if (clickNode(target.nodeInfo)) {
                Log.i(TAG, "Successfully clicked option #$index1Based: '${target.text.ifBlank { target.contentDescription }}'")
                return true
            }
        }
        return false
    }

    fun clickNode(node: AccessibilityNodeInfo): Boolean {
        var curr: AccessibilityNodeInfo? = node
        while (curr != null) {
            if (curr.isClickable) {
                val ok = curr.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                if (ok) return true
            }
            curr = curr.parent
        }
        // Fallback: Dispatch tap at the center coordinates of the node bounds if clickable flag was false on parent
        val rect = Rect()
        node.getBoundsInScreen(rect)
        if (!rect.isEmpty && rect.centerX() > 0 && rect.centerY() > 0) {
            return tapCoordinates(rect.centerX().toFloat(), rect.centerY().toFloat())
        }
        return false
    }

    fun typeText(text: String, targetField: AccessibilityNodeInfo? = null, append: Boolean = false): Boolean {
        val root = getRootNode() ?: return false
        val field = targetField ?: findFirstEditableNode(root)
        if (field != null) {
            try {
                field.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
                val arguments = Bundle().apply {
                    val currentText = if (append) (field.text?.toString() ?: "") else ""
                    putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, currentText + text)
                }
                val setSuccess = field.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
                if (setSuccess) {
                    Log.i(TAG, "Successfully set text into input field: '$text'")
                    return true
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error setting text into node: ${e.message}")
            }
        }
        return false
    }

    private fun findFirstEditableNode(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        if (node == null) return null
        if (node.isEditable) return node
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val found = findFirstEditableNode(child)
            if (found != null) return found
        }
        return null
    }

    fun scrollForward(): Boolean {
        val root = getRootNode() ?: return false
        val scrollable = findScrollableNode(root)
        if (scrollable != null) {
            val res = scrollable.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)
            if (res) return true
        }
        // Fallback: Gesture swipe up
        val displayMetrics = context.resources.displayMetrics
        val width = displayMetrics.widthPixels.toFloat()
        val height = displayMetrics.heightPixels.toFloat()
        return swipe(width * 0.5f, height * 0.75f, width * 0.5f, height * 0.25f, 350L)
    }

    fun scrollBackward(): Boolean {
        val root = getRootNode() ?: return false
        val scrollable = findScrollableNode(root)
        if (scrollable != null) {
            val res = scrollable.performAction(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD)
            if (res) return true
        }
        // Fallback: Gesture swipe down
        val displayMetrics = context.resources.displayMetrics
        val width = displayMetrics.widthPixels.toFloat()
        val height = displayMetrics.heightPixels.toFloat()
        return swipe(width * 0.5f, height * 0.25f, width * 0.5f, height * 0.75f, 350L)
    }

    private fun findScrollableNode(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        if (node == null) return null
        if (node.isScrollable) return node
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val found = findScrollableNode(child)
            if (found != null) return found
        }
        return null
    }

    fun tapCoordinates(x: Float, y: Float): Boolean {
        val service = serviceInstance ?: return false
        val path = Path().apply { moveTo(x, y) }
        val stroke = GestureDescription.StrokeDescription(path, 0, 50)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        return service.dispatchGesture(gesture, null, null)
    }

    fun swipe(startX: Float, startY: Float, endX: Float, endY: Float, durationMs: Long = 300L): Boolean {
        val service = serviceInstance ?: return false
        val path = Path().apply {
            moveTo(startX, startY)
            lineTo(endX, endY)
        }
        val stroke = GestureDescription.StrokeDescription(path, 0, durationMs)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        return service.dispatchGesture(gesture, null, null)
    }

    fun pressBack(): Boolean {
        val service = serviceInstance ?: return false
        return service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
    }

    fun pressHome(): Boolean {
        val service = serviceInstance ?: return false
        return service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME)
    }

    fun pressRecents(): Boolean {
        val service = serviceInstance ?: return false
        return service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_RECENTS)
    }

    fun openNotifications(): Boolean {
        val service = serviceInstance ?: return false
        return service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS)
    }

    fun openQuickSettings(): Boolean {
        val service = serviceInstance ?: return false
        return service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_QUICK_SETTINGS)
    }

    fun extractVisibleText(): String {
        val root = getRootNode() ?: return ""
        val sb = StringBuilder()
        collectTextRecursive(root, sb)
        return sb.toString().trim()
    }

    private fun collectTextRecursive(node: AccessibilityNodeInfo?, sb: StringBuilder) {
        if (node == null || !node.isVisibleToUser) return

        val text = node.text?.toString()?.trim()
        val desc = node.contentDescription?.toString()?.trim()

        if (!text.isNullOrBlank()) {
            sb.append(text).append("\n")
        } else if (!desc.isNullOrBlank()) {
            sb.append(desc).append("\n")
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            collectTextRecursive(child, sb)
        }
    }

    fun findInteractiveElements(): List<UiElement> {
        val root = getRootNode() ?: return emptyList()
        val list = mutableListOf<UiElement>()
        collectAllElements(root, list, clickableOnly = true)
        return list
    }

    private fun collectAllElements(
        node: AccessibilityNodeInfo?,
        outList: MutableList<UiElement>,
        clickableOnly: Boolean = false
    ) {
        if (node == null || !node.isVisibleToUser) return

        val text = node.text?.toString()?.trim() ?: ""
        val desc = node.contentDescription?.toString()?.trim() ?: ""
        val viewId = node.viewIdResourceName?.toString() ?: ""
        val className = node.className?.toString() ?: ""
        val isClickable = node.isClickable
        val isEditable = node.isEditable

        val hasContent = text.isNotBlank() || desc.isNotBlank() || isClickable || isEditable
        if (hasContent && (!clickableOnly || (isClickable || isEditable))) {
            val rect = Rect()
            node.getBoundsInScreen(rect)
            outList.add(
                UiElement(
                    text = text,
                    contentDescription = desc,
                    viewId = viewId,
                    className = className,
                    isClickable = isClickable,
                    isEditable = isEditable,
                    bounds = rect,
                    nodeInfo = node
                )
            )
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            collectAllElements(child, outList, clickableOnly)
        }
    }

    fun detectLoading(): Boolean {
        val root = getRootNode() ?: return false
        val text = extractVisibleText().lowercase()
        if (text.contains("loading...") || text.contains("please wait") || text.contains("connecting...")) return true

        val all = mutableListOf<UiElement>()
        collectAllElements(root, all)
        return all.any { it.className.contains("ProgressBar", ignoreCase = true) }
    }

    fun detectError(): String? {
        val text = extractVisibleText()
        val lower = text.lowercase()
        val errorKeywords = listOf("error", "failed", "connection timed out", "try again", "crash", "something went wrong", "ত্রুটি", "गड़बड़")
        for (kw in errorKeywords) {
            if (lower.contains(kw)) {
                // Find line with keyword
                val line = text.lines().firstOrNull { it.contains(kw, ignoreCase = true) }
                return line ?: "An error message is displayed on screen."
            }
        }
        return null
    }

    fun takeScreenshot(callback: (Bitmap?) -> Unit) {
        val service = serviceInstance
        if (service != null) {
            service.takeScreenshotCompat(callback)
        } else {
            callback(null)
        }
    }
}
