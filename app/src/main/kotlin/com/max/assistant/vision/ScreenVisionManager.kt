package com.max.assistant.vision

import android.content.Context
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.util.Base64
import android.util.Log
import com.max.assistant.ai.IAiProvider
import com.max.assistant.hud.ScreenVisionUiState
import com.max.assistant.tts.VoiceLanguage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.util.Locale

class ScreenVisionManager(
    private val context: Context,
    var aiProvider: IAiProvider? = null,
    val accessibilityController: com.max.assistant.accessibility.AndroidAccessibilityController = com.max.assistant.accessibility.AndroidAccessibilityController(context),
    val screenContextManager: ScreenContextManager = ScreenContextManager()
) {

    companion object {
        private const val TAG = "ScreenVisionManager"
        private const val VIRTUAL_DISPLAY_NAME = "MAX_ScreenVision_Display"
        const val MAX_CAPTURE_WIDTH = 1080
        const val MAX_CAPTURE_HEIGHT = 1920
    }

    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null

    var isLiveVisionActive: Boolean = false
        private set

    val hasActiveProjection: Boolean get() = mediaProjection != null

    private val _screenVisionUiState = MutableStateFlow(ScreenVisionUiState())
    val screenVisionUiState: StateFlow<ScreenVisionUiState> = _screenVisionUiState.asStateFlow()

    private val projectionCallback = object : MediaProjection.Callback() {
        override fun onStop() {
            Log.i(TAG, "MediaProjection session stopped by system or user.")
            cleanupResources()
        }
    }

    fun setMediaProjection(projection: MediaProjection, width: Int, height: Int, densityDpi: Int) {
        cleanupResources()
        mediaProjection = projection
        projection.registerCallback(projectionCallback, null)

        val targetWidth = if (width > MAX_CAPTURE_WIDTH) MAX_CAPTURE_WIDTH else width
        val targetHeight = if (height > MAX_CAPTURE_HEIGHT) MAX_CAPTURE_HEIGHT else height

        val reader = ImageReader.newInstance(targetWidth, targetHeight, PixelFormat.RGBA_8888, 2)
        imageReader = reader

        val flags = DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR
        virtualDisplay = projection.createVirtualDisplay(
            VIRTUAL_DISPLAY_NAME,
            targetWidth,
            targetHeight,
            densityDpi,
            flags,
            reader.surface,
            null,
            null
        )
        Log.i(TAG, "MediaProjection initialized: ${targetWidth}x${targetHeight} at $densityDpi dpi")
    }

    fun captureFrame(): Bitmap? {
        val reader = imageReader ?: return null
        val image = try {
            reader.acquireLatestImage() ?: reader.acquireNextImage()
        } catch (e: Exception) {
            Log.w(TAG, "Error acquiring image frame: ${e.message}")
            null
        } ?: return null

        return try {
            val planes = image.planes
            val buffer = planes[0].buffer
            val pixelStride = planes[0].pixelStride
            val rowStride = planes[0].rowStride
            val rowPadding = rowStride - pixelStride * image.width

            val bitmap = Bitmap.createBitmap(
                image.width + rowPadding / pixelStride,
                image.height,
                Bitmap.Config.ARGB_8888
            )
            bitmap.copyPixelsFromBuffer(buffer)

            // Crop excess row padding if present
            if (rowPadding > 0) {
                Bitmap.createBitmap(bitmap, 0, 0, image.width, image.height)
            } else {
                bitmap
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting bitmap from image: ${e.message}")
            null
        } finally {
            image.close()
        }
    }

    suspend fun acquireBitmap(): Bitmap? {
        if (mediaProjection != null) {
            val bmp = captureFrame()
            if (bmp != null) return bmp
        }
        if (accessibilityController.isServiceActive) {
            return try {
                kotlinx.coroutines.withTimeoutOrNull(2500L) {
                    kotlinx.coroutines.suspendCancellableCoroutine { cont ->
                        accessibilityController.takeScreenshot { bmp ->
                            if (cont.isActive) {
                                cont.resume(bmp) {}
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Screen capture via Accessibility timed out or failed: ${e.message}")
                null
            }
        }
        return null
    }

    suspend fun analyzeScreenSnapshot(
        prompt: String,
        language: VoiceLanguage = VoiceLanguage.HINGLISH
    ): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        // 1. Identify package and app name
        val rootNode = accessibilityController.getRootNode()
        val pkg = rootNode?.packageName?.toString() ?: ""
        val appName = getFriendlyAppName(pkg)

        val interactiveElements = try {
            accessibilityController.findInteractiveElements().mapNotNull { el ->
                val txt = el.text.ifBlank { el.contentDescription.ifBlank { el.viewId ?: "" } }.trim()
                if (txt.isNotBlank() && txt.length < 40) txt else null
            }.distinct().take(6)
        } catch (_: Exception) {
            emptyList()
        }

        _screenVisionUiState.value = ScreenVisionUiState(
            isActive = true,
            captureStatus = "Analyzing Display...",
            appName = appName,
            extractedTextSummary = "",
            identifiedElementsCount = interactiveElements.size,
            identifiedElements = interactiveElements,
            maxInterpretation = "Processing visible screen elements and visual layout, Sir...",
            suggestedActions = listOf("Explain Error", "Scroll Down", "Take Action")
        )

        // 2. Strict Gallery and Financial App Protection Interception
        if (com.max.assistant.protection.GalleryProtectionManager.isGalleryPackage(pkg)) {
            val blockMsg = "Sorry, I don't have permission to access your Gallery."
            _screenVisionUiState.value = ScreenVisionUiState(
                isActive = false,
                captureStatus = "Privacy Protected",
                appName = appName,
                maxInterpretation = blockMsg
            )
            return@withContext Pair(false, blockMsg)
        }
        if (com.max.assistant.protection.PaymentProtectionManager.isPaymentPackage(pkg)) {
            val blockMsg = "Sir, for your financial security and privacy, screen vision is restricted on banking and payment applications."
            _screenVisionUiState.value = ScreenVisionUiState(
                isActive = false,
                captureStatus = "Financial Shield Active",
                appName = appName,
                maxInterpretation = blockMsg
            )
            return@withContext Pair(false, blockMsg)
        }

        // 3. Extract visible text from active UI hierarchy
        val visibleText = accessibilityController.extractVisibleText()

        // 4. Acquire current frame via MediaProjection or AccessibilityService
        val bitmap = acquireBitmap()

        // Helper to update state on successful explanation
        fun updateSuccessUiState(explanation: String) {
            val dynamicSuggestions = mutableListOf<String>()
            if (interactiveElements.isNotEmpty()) {
                dynamicSuggestions.add("Click ${interactiveElements.first()}")
            }
            if (visibleText.contains("error", ignoreCase = true) || visibleText.contains("failed", ignoreCase = true)) {
                dynamicSuggestions.add("Explain Error")
            }
            dynamicSuggestions.add("Scroll Down")
            dynamicSuggestions.add("Close Vision")

            _screenVisionUiState.value = ScreenVisionUiState(
                isActive = true,
                captureStatus = "Screen Vision Active",
                appName = appName,
                extractedTextSummary = visibleText.take(240).replace("\n", " ").trim(),
                identifiedElementsCount = interactiveElements.size,
                identifiedElements = interactiveElements,
                maxInterpretation = explanation,
                suggestedActions = dynamicSuggestions.distinct().take(4)
            )
        }

        // 5. Try Gemini Multimodal AI Vision if bitmap and provider are available
        if (bitmap != null) {
            val ai = aiProvider
            if (ai != null && ai.isConfigured) {
                try {
                    val base64 = compressBitmapToBase64(bitmap)
                    val contextEnrichedPrompt = if (prompt.isNotBlank()) {
                        if (visibleText.isNotBlank()) {
                            "$prompt\n\n(Extracted text from active window: ${visibleText.take(1200)})"
                        } else {
                            prompt
                        }
                    } else {
                        if (visibleText.isNotBlank()) {
                            "Explain what is currently on the screen, identify any questions, errors, or key elements clearly.\n\n(Visible UI text: ${visibleText.take(1200)})"
                        } else {
                            "Explain what is currently on the screen, identify any questions, errors, or key elements clearly."
                        }
                    }

                    val result = ai.analyzeImage(base64, contextEnrichedPrompt, language)
                    if (result is com.max.assistant.ai.AiResponse.Success && result.text.isNotBlank()) {
                        screenContextManager.updateScreenContext(result.text, visibleText, bitmap)
                        updateSuccessUiState(result.text)
                        return@withContext Pair(true, result.text)
                    } else if (result is com.max.assistant.ai.AiResponse.Error) {
                        Log.w(TAG, "Gemini Vision returned error: ${result.message}. Falling back to local screen vision.")
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Gemini Vision exception: ${e.message}. Falling back to local screen vision.")
                }
            }
        }

        // 6. Intelligent Local Screen Vision Engine (Offline / Instant fallback)
        if (visibleText.isNotBlank() || pkg.isNotBlank()) {
            val localExplanation = generateLocalScreenExplanation(pkg, visibleText, prompt, language)
            screenContextManager.updateScreenContext(localExplanation, visibleText, bitmap)
            updateSuccessUiState(localExplanation)
            return@withContext Pair(true, localExplanation)
        }

        // 7. If both screenshot and accessibility text failed
        if (mediaProjection == null && !accessibilityController.isServiceActive) {
            val reqMsg = when (language) {
                VoiceLanguage.HINGLISH -> "Sir, screen dekhne ke liye Accessibility permission enable karni hogi. Settings open kar raha hoon, wahan MAX Assistant ko turn on karein."
                VoiceLanguage.HINDI -> "सर, स्क्रीन देखने के लिए एक्सेसिबिलिटी अनुमति चालू करें। मैं सेटिंग्स खोल रहा हूँ।"
                VoiceLanguage.BENGALI -> "স্যার, স্ক্রিন দেখার জন্য অ্যাক্সেসিবিলিটি পারমিশন চালু করুন। আমি সেটিংস খুলছি।"
                else -> "Sir, screen vision requires Accessibility permission. Opening Accessibility settings for you now."
            }
            _screenVisionUiState.value = ScreenVisionUiState(
                isActive = false,
                captureStatus = "Permission Required",
                appName = appName,
                maxInterpretation = reqMsg
            )
            return@withContext Pair(false, reqMsg)
        }

        val failMsg = when (language) {
            VoiceLanguage.HINGLISH -> "Sir, abhi screen frame capture nahi ho paya. Ek second baad dobara try karein."
            VoiceLanguage.HINDI -> "सर, स्क्रीन का फ्रेम कैप्चर नहीं हो सका। कृपया दोबारा प्रयास करें।"
            VoiceLanguage.BENGALI -> "স্যার, স্ক্রিন ফ্রেম ক্যাপচার করা সম্ভব হয়নি। অনুগ্রহ করে আবার চেষ্টা করুন।"
            else -> "Sir, failed to acquire the current display frame. Please try again."
        }
        _screenVisionUiState.value = ScreenVisionUiState(
            isActive = false,
            captureStatus = "Capture Inactive",
            appName = appName,
            maxInterpretation = failMsg
        )
        return@withContext Pair(false, failMsg)
    }

    fun getFriendlyAppName(packageName: String): String {
        if (packageName.isBlank()) return "Active Screen"
        val known = mapOf(
            "com.whatsapp" to "WhatsApp",
            "com.whatsapp.w4b" to "WhatsApp Business",
            "com.android.chrome" to "Google Chrome",
            "org.chromium.chrome" to "Google Chrome",
            "com.google.android.youtube" to "YouTube",
            "com.google.android.apps.maps" to "Google Maps",
            "com.android.settings" to "Settings",
            "com.samsung.android.settings" to "Settings",
            "com.google.android.gm" to "Gmail",
            "com.google.android.apps.messaging" to "Messages",
            "com.samsung.android.messaging" to "Messages",
            "com.google.android.dialer" to "Phone",
            "com.samsung.android.dialer" to "Phone",
            "com.sec.android.app.camera" to "Camera",
            "com.google.android.GoogleCamera" to "Camera",
            "com.spotify.music" to "Spotify",
            "com.instagram.android" to "Instagram",
            "com.facebook.katana" to "Facebook",
            "com.twitter.android" to "X (Twitter)",
            "com.amazon.mShop.android.shopping" to "Amazon",
            "com.flipkart.android" to "Flipkart",
            "com.netflix.mediaclient" to "Netflix"
        )
        val direct = known[packageName]
        if (direct != null) return direct

        return try {
            val pm = context.packageManager
            val info = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(info).toString()
        } catch (_: Exception) {
            packageName.substringAfterLast('.').replaceFirstChar { it.uppercase() }
        }
    }

    fun generateLocalScreenExplanation(
        packageName: String,
        visibleText: String,
        userPrompt: String,
        language: VoiceLanguage
    ): String {
        val appName = getFriendlyAppName(packageName)
        val lowerPrompt = userPrompt.lowercase(Locale.ROOT)
        val isReadQuery = lowerPrompt.contains("read") || lowerPrompt.contains("padho") || lowerPrompt.contains("poro") || lowerPrompt.contains("likha")
        val isErrorQuery = lowerPrompt.contains("error") || lowerPrompt.contains("galti") || lowerPrompt.contains("problem")
        val isSolveQuery = lowerPrompt.contains("solve") || lowerPrompt.contains("hal") || lowerPrompt.contains("question") || lowerPrompt.contains("math")

        val lines = visibleText.lines().map { it.trim() }.filter { it.isNotBlank() }

        if (lines.isEmpty()) {
            return when (language) {
                VoiceLanguage.HINDI -> "सर, $appName खुला है, लेकिन इस स्क्रीन पर कोई टेक्स्ट या संदेश नहीं मिला।"
                VoiceLanguage.BENGALI -> "স্যার, $appName খোলা আছে, তবে স্ক্রিনে কোনো লেখা পাওয়া যায়নি।"
                VoiceLanguage.HINGLISH -> "Sir, $appName open hai, lekin screen par abhi koi readable text nahi dikh raha hai."
                else -> "Sir, $appName is open, but no readable text was detected on this screen."
            }
        }

        // Check if there is an error dialog or alert
        val errorLines = lines.filter { line ->
            val l = line.lowercase(Locale.ROOT)
            l.contains("error") || l.contains("failed") || l.contains("alert") || l.contains("warning") ||
            l.contains("unable") || l.contains("try again") || l.contains("invalid") || l.contains("not found")
        }

        if (isErrorQuery && errorLines.isNotEmpty()) {
            val errorSummary = errorLines.take(3).joinToString(". ")
            return when (language) {
                VoiceLanguage.HINDI -> "सर, स्क्रीन पर यह समस्या दिख रही है: $errorSummary। कृपया जांचें।"
                VoiceLanguage.BENGALI -> "স্যার, স্ক্রিনে এই সমস্যা দেখা যাচ্ছে: $errorSummary।"
                VoiceLanguage.HINGLISH -> "Sir, screen par yeh error aa raha hai: $errorSummary. Isko theek karne ke liye screen ke instructions follow karein."
                else -> "Sir, the error on screen indicates: $errorSummary."
            }
        }

        if (isReadQuery) {
            val preview = lines.take(6).joinToString(". ")
            return when (language) {
                VoiceLanguage.HINDI -> "सर, $appName पर लिखा है: $preview"
                VoiceLanguage.BENGALI -> "স্যার, $appName-এ লেখা আছে: $preview"
                VoiceLanguage.HINGLISH -> "Sir, $appName par yeh likha hai: $preview"
                else -> "Sir, reading from $appName: $preview"
            }
        }

        if (isSolveQuery) {
            val problemLines = lines.take(5).joinToString(" ")
            return when (language) {
                VoiceLanguage.HINDI -> "सर, $appName पर यह प्रश्न दिख रहा है: $problemLines"
                VoiceLanguage.BENGALI -> "স্যার, $appName-এ এই প্রশ্নটি দেখা যাচ্ছে: $problemLines"
                VoiceLanguage.HINGLISH -> "Sir, $appName par yeh question dikh raha hai: $problemLines"
                else -> "Sir, identified question on $appName: $problemLines"
            }
        }

        // General "Can you see this?" / "What is on my screen?" / "Explain this"
        val header = lines.firstOrNull() ?: appName
        val contentHighlights = lines.drop(1).take(5).joinToString(", ")
        return when (language) {
            VoiceLanguage.HINDI -> if (contentHighlights.isNotBlank()) {
                "हाँ सर, मैं आपकी स्क्रीन देख रहा हूँ। $appName खुला है, जिसमें मुख्य शीर्षक '$header' है और '$contentHighlights' दिखाई दे रहा है।"
            } else {
                "हाँ सर, मैं आपकी स्क्रीन देख रहा हूँ। स्क्रीन पर $appName खुला हुआ है।"
            }
            VoiceLanguage.BENGALI -> if (contentHighlights.isNotBlank()) {
                "হ্যাঁ স্যার, আমি আপনার স্ক্রিন দেখছি। $appName খোলা আছে, যেখানে '$header' এবং '$contentHighlights' দেখা যাচ্ছে।"
            } else {
                "হ্যাঁ স্যার, আমি আপনার স্ক্রিন দেখছি। স্ক্রিনে $appName খোলা রয়েছে।"
            }
            VoiceLanguage.HINGLISH -> if (contentHighlights.isNotBlank()) {
                "Haan Sir, main aapki screen dekh raha hoon! $appName open hai, jisme '$header' aur '$contentHighlights' dikh raha hai."
            } else {
                "Haan Sir, main aapki screen dekh sakta hoon. Screen par $appName open hai."
            }
            else -> if (contentHighlights.isNotBlank()) {
                "Yes, Sir, I can see your screen. $appName is active, showing '$header' with '$contentHighlights'."
            } else {
                "Yes, Sir, I can see your screen. $appName is currently active."
            }
        }
    }

    fun startLiveVision() {
        isLiveVisionActive = true
        Log.i(TAG, "Live screen vision activated.")
    }

    fun stopLiveVision() {
        isLiveVisionActive = false
        Log.i(TAG, "Live screen vision deactivated.")
    }

    fun compressBitmapToBase64(bitmap: Bitmap, quality: Int = 75): String {
        val stream = ByteArrayOutputStream()
        // Downscale if unusually large
        val scaled = if (bitmap.width > 1080) {
            val aspect = bitmap.height.toFloat() / bitmap.width.toFloat()
            Bitmap.createScaledBitmap(bitmap, 1080, (1080 * aspect).toInt(), true)
        } else {
            bitmap
        }
        scaled.compress(Bitmap.CompressFormat.JPEG, quality, stream)
        val byteArray = stream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.NO_WRAP)
    }

    fun cleanupResources() {
        isLiveVisionActive = false
        _screenVisionUiState.value = ScreenVisionUiState(
            isActive = false,
            captureStatus = "Standby",
            appName = "Home Screen",
            maxInterpretation = "Screen vision is in standby. Ask 'MAX, what is on my screen?'"
        )
        try {
            virtualDisplay?.release()
            virtualDisplay = null
            imageReader?.close()
            imageReader = null
            mediaProjection?.unregisterCallback(projectionCallback)
            mediaProjection?.stop()
            mediaProjection = null
            Log.i(TAG, "Screen vision resources completely released.")
        } catch (e: Exception) {
            Log.w(TAG, "Error cleaning up screen vision resources: ${e.message}")
        }
    }
}
