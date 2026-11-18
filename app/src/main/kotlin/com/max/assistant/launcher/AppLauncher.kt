package com.max.assistant.launcher

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import android.util.Log
import com.max.assistant.protection.GalleryProtectionManager
import com.max.assistant.protection.PaymentProtectionManager

sealed class LaunchResult {
    data class Success(val appName: String, val packageName: String) : LaunchResult()
    data class Blocked(val reason: String) : LaunchResult()
    data class NotFound(val query: String) : LaunchResult()
    data class Error(val message: String) : LaunchResult()
}

class AppLauncher(private val context: Context) {

    companion object {
        private const val TAG = "AppLauncher"

        // Disallowed stop words that should NEVER trigger fuzzy application launch
        private val STOPWORDS = setOf(
            "app", "apps", "application", "the", "a", "an", "kholo", "chalao", "open", "launch",
            "start", "run", "please", "sir", "karo", "do", "ek", "my", "me", "to", "in", "what",
            "whats", "dikhao", "chalun", "khulun", "bhejo", "kar", "wa", "ऐप", "ऐप्स", "खोलो", "चलाओ"
        )

        // Comprehensive alias dictionary with English, Hindi & Bengali app names
        private val KNOWN_APPS = mapOf(
            // YouTube
            "youtube" to listOf("com.google.android.youtube"),
            "yt" to listOf("com.google.android.youtube"),
            "यूट्यूब" to listOf("com.google.android.youtube"),
            "ইউটিউব" to listOf("com.google.android.youtube"),
            "youtube music" to listOf("com.google.android.apps.youtube.music"),
            "yt music" to listOf("com.google.android.apps.youtube.music"),
            
            // WhatsApp (ONLY explicit WhatsApp aliases - NO short "wa" or generic "app")
            "whatsapp" to listOf("com.whatsapp", "com.whatsapp.w4b"),
            "whats app" to listOf("com.whatsapp", "com.whatsapp.w4b"),
            "व्हाट्सएप" to listOf("com.whatsapp", "com.whatsapp.w4b"),
            "व्हाट्स एप" to listOf("com.whatsapp", "com.whatsapp.w4b"),
            "হোয়াটসঅ্যাপ" to listOf("com.whatsapp", "com.whatsapp.w4b"),
            "whatsapp business" to listOf("com.whatsapp.w4b", "com.whatsapp"),

            // Instagram
            "instagram" to listOf("com.instagram.android"),
            "insta" to listOf("com.instagram.android"),
            "इंस्टाग्राम" to listOf("com.instagram.android"),
            "ইনস্টাগ্রাম" to listOf("com.instagram.android"),

            // Facebook
            "facebook" to listOf("com.facebook.katana"),
            "fb" to listOf("com.facebook.katana"),
            "फेसबुक" to listOf("com.facebook.katana"),
            "ফেসবুক" to listOf("com.facebook.katana"),

            // Chrome & Browser
            "chrome" to listOf("com.android.chrome"),
            "क्रोम" to listOf("com.android.chrome"),
            "ক্রোম" to listOf("com.android.chrome"),
            "browser" to listOf("com.android.chrome", "com.sec.android.app.sbrowser"),
            "ब्राउज़र" to listOf("com.android.chrome", "com.sec.android.app.sbrowser"),
            "इंटरनेट" to listOf("com.sec.android.app.sbrowser", "com.android.chrome"),
            "ইন্টারনেট" to listOf("com.android.chrome"),

            // Camera
            "camera" to listOf("com.sec.android.app.camera", "com.google.android.GoogleCamera"),
            "cam" to listOf("com.sec.android.app.camera"),
            "कैमरा" to listOf("com.sec.android.app.camera"),
            "ক্যামেরা" to listOf("com.sec.android.app.camera"),
            "फोटो" to listOf("com.sec.android.app.camera"),

            // Settings
            "settings" to listOf("com.android.settings"),
            "setting" to listOf("com.android.settings"),
            "सेटिंग्स" to listOf("com.android.settings"),
            "सेटिंग" to listOf("com.android.settings"),
            "সেটিংস" to listOf("com.android.settings"),

            // Calculator
            "calculator" to listOf("com.sec.android.app.popupcalculator", "com.google.android.calculator"),
            "calc" to listOf("com.sec.android.app.popupcalculator"),
            "कैलकुलेटर" to listOf("com.sec.android.app.popupcalculator"),
            "ক্যালকুলেটর" to listOf("com.sec.android.app.popupcalculator"),

            // Clock & Alarms
            "clock" to listOf("com.sec.android.app.clockpackage", "com.google.android.deskclock"),
            "alarm" to listOf("com.sec.android.app.clockpackage"),
            "घड़ी" to listOf("com.sec.android.app.clockpackage"),
            "ঘড়ি" to listOf("com.sec.android.app.clockpackage"),

            // Contacts
            "contacts" to listOf("com.samsung.android.app.contacts", "com.google.android.contacts"),
            "phonebook" to listOf("com.samsung.android.app.contacts"),
            "संपर्क" to listOf("com.samsung.android.app.contacts"),
            "কনট্যাক্টস" to listOf("com.samsung.android.app.contacts"),

            // Phone Dialer
            "phone" to listOf("com.samsung.android.dialer", "com.google.android.dialer"),
            "dialer" to listOf("com.samsung.android.dialer"),
            "फोन" to listOf("com.samsung.android.dialer"),
            "डायलर" to listOf("com.samsung.android.dialer"),
            "ফোন" to listOf("com.samsung.android.dialer"),

            // Messages
            "messages" to listOf("com.google.android.apps.messaging", "com.samsung.android.messaging"),
            "message" to listOf("com.google.android.apps.messaging", "com.samsung.android.messaging"),
            "sms" to listOf("com.google.android.apps.messaging"),
            "मैसेज" to listOf("com.google.android.apps.messaging"),
            "মেসেজ" to listOf("com.google.android.apps.messaging"),

            // Spotify & Music
            "spotify" to listOf("com.spotify.music"),
            "स्पॉटीफाई" to listOf("com.spotify.music"),
            "স্পটিফাই" to listOf("com.spotify.music"),
            "music" to listOf("com.spotify.music", "com.google.android.apps.youtube.music", "com.sec.android.app.music"),
            "गाने" to listOf("com.spotify.music", "com.google.android.apps.youtube.music"),
            "गाना" to listOf("com.spotify.music", "com.google.android.apps.youtube.music"),
            "গান" to listOf("com.spotify.music", "com.google.android.apps.youtube.music"),

            // Maps
            "maps" to listOf("com.google.android.apps.maps"),
            "map" to listOf("com.google.android.apps.maps"),
            "google maps" to listOf("com.google.android.apps.maps"),
            "नक्शा" to listOf("com.google.android.apps.maps"),
            "মানচিত্র" to listOf("com.google.android.apps.maps"),

            // Gmail
            "gmail" to listOf("com.google.android.gm"),
            "email" to listOf("com.google.android.gm"),
            "mail" to listOf("com.google.android.gm"),
            "ईमेल" to listOf("com.google.android.gm"),

            // Telegram
            "telegram" to listOf("org.telegram.messenger"),
            "टेलीग्राम" to listOf("org.telegram.messenger"),

            // Snapchat
            "snapchat" to listOf("com.snapchat.android"),
            "snap" to listOf("com.snapchat.android"),

            // Netflix
            "netflix" to listOf("com.netflix.mediaclient"),

            // Google Play Store
            "play store" to listOf("com.android.vending"),
            "google play" to listOf("com.android.vending")
        )
    }

    fun launchAppByVoice(query: String): LaunchResult {
        val trimmed = query.trim().lowercase()

        // 1. Strict Gallery Protection check (English, Hindi, Bengali)
        if (GalleryProtectionManager.shouldBlockVoiceLaunch(trimmed)) {
            Log.w(TAG, "Access to gallery/photos denied, Sir: $query")
            return LaunchResult.Blocked("माफ़ कीजिए सर, गैलरी और तस्वीरों तक पहुंच सुरक्षा नीति द्वारा प्रतिबंधित है।")
        }

        // 1b. Strict Payment Protection check
        if (PaymentProtectionManager.isPaymentRequest(trimmed)) {
            Log.w(TAG, "Payment execution launch denied, Sir: $query")
            return LaunchResult.Blocked(PaymentProtectionManager.getBlockedPaymentMessage(com.max.assistant.tts.VoiceLanguage.HINGLISH))
        }

        // Clean wake words and prefixes repeatedly
        var targetName = trimmed
            .replace(Regex("^(hey\\s+max|ok\\s+max|okay\\s+max|hello\\s+max|namaste\\s+max|max|मैक्स|ম্যাক্স|হেই\\s+ম্যাক্স|হে\\s+ম্যাক্স|ম্যাক্স\\s+শোনো)[,\\s:]*"), "")
            .trimStart(',', ' ', ':', '-', ';', '.')
            .trimEnd(',', ' ', ':', '-', ';', '.', '?')
            .trim()

        val prefixes = listOf(
            "open ", "launch ", "start ", "run ", "chala do ", "khol do ", "kholo ", "chalao ",
            "खोलो ", "चलाओ ", "चालू करो ", "খোলো ", "চালু করো ", "please open ", "please start "
        )
        var changed = true
        while (changed) {
            changed = false
            for (prefix in prefixes) {
                if (targetName.startsWith(prefix)) {
                    targetName = targetName.removePrefix(prefix).trim()
                    changed = true
                    break
                }
            }
        }

        val suffixes = listOf(
            " kholo", " chalao", " dikhao", " start karo", " open karo",
            " chala do", " khol do", " khulun", " open", " launch", " application", " app",
            " खोलो", " चलाओ", " चालू करो", " ऐप", " ऐप खोलो", " অ্যাপ", " অ্যাপ খোলো",
            " please", " sir", " please kholo"
        )
        changed = true
        while (changed) {
            changed = false
            for (suffix in suffixes) {
                if (targetName.endsWith(suffix)) {
                    targetName = targetName.removeSuffix(suffix).trim()
                    changed = true
                    break
                }
            }
        }

        targetName = targetName.trimStart(',', ' ', ':', '-', ';', '.').trimEnd(',', ' ', ':', '-', ';', '.', '?').trim()

        if (targetName.isEmpty() || STOPWORDS.contains(targetName) || targetName.length < 2) {
            return LaunchResult.NotFound("कोई मान्य एप्लिकेशन का नाम नहीं पहचाना गया, सर।")
        }

        // 2. Settings shortcut
        if (targetName in setOf("settings", "setting", "सेटिंग्स", "सेटिंग", "সেটিংস")) {
            return try {
                val intent = Intent(Settings.ACTION_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                safeStartActivity(intent)
                LaunchResult.Success("Settings", "com.android.settings")
            } catch (e: Exception) {
                LaunchResult.Error("सेटिंग्स खोलने में त्रुटि, सर: ${e.message}")
            }
        }

        // 3. Known App Dictionary Lookup (Exact & Alias)
        val candidates = KNOWN_APPS[targetName]
        if (candidates != null) {
            for (pkg in candidates) {
                if (isPackageInstalled(pkg)) {
                    val displayName = targetName.replaceFirstChar { it.uppercase() }
                    val res = launchPackage(pkg, displayName)
                    if (res is LaunchResult.Success) return res
                }
            }
        }

        // 4. Dynamic Package Manager Query: Search installed launcher activities
        val pm = context.packageManager
        val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val resolveInfos = pm.queryIntentActivities(mainIntent, 0)

        // Exact label match
        for (info in resolveInfos) {
            val label = info.loadLabel(pm).toString().lowercase().trim()
            val packageName = info.activityInfo.packageName

            if (GalleryProtectionManager.isGalleryPackage(packageName)) continue

            if (label == targetName) {
                return launchPackage(packageName, info.loadLabel(pm).toString())
            }
        }

        // Guard against generic words triggering WhatsApp or random apps
        if (targetName.length < 3 || STOPWORDS.contains(targetName)) {
            return LaunchResult.NotFound("माफ़ कीजिए सर, एप्लिकेशन '$targetName' फोन में नहीं मिला।")
        }

        // Safe prefix match: only if targetName is a distinct prefix of the app's name
        for (info in resolveInfos) {
            val label = info.loadLabel(pm).toString().lowercase().trim()
            val packageName = info.activityInfo.packageName

            if (GalleryProtectionManager.isGalleryPackage(packageName)) continue

            // WhatsApp safety guard: WhatsApp should NEVER match unless targetName contains whatsapp
            if (packageName.contains("whatsapp") && !targetName.contains("whatsapp") && !targetName.contains("व्हाट्सएप") && !targetName.contains("হোয়াটসঅ্যাপ")) {
                continue
            }

            // Word-boundary prefix match (e.g. "youtube" matches "YouTube Music", "chrome" matches "Google Chrome")
            if (label.startsWith(targetName) || label.endsWith(" $targetName") || label.contains(" $targetName ")) {
                return launchPackage(packageName, info.loadLabel(pm).toString())
            }
        }

        return LaunchResult.NotFound("माफ़ कीजिए सर, एप्लिकेशन '$targetName' फोन में नहीं मिला।")
    }

    private fun launchPackage(packageName: String, displayName: String): LaunchResult {
        if (GalleryProtectionManager.isGalleryPackage(packageName)) {
            return LaunchResult.Blocked("माफ़ कीजिए सर, गैलरी और तस्वीरों तक पहुंच सुरक्षा नीति द्वारा प्रतिबंधित है।")
        }

        return try {
            val intent = context.packageManager.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                val launched = safeStartActivity(intent)
                if (launched) {
                    Log.i(TAG, "Launched $displayName ($packageName) for Sir")
                    LaunchResult.Success(displayName, packageName)
                } else {
                    LaunchResult.Error("एप्लिकेशन $displayName खोलने में असमर्थ, सर।")
                }
            } else {
                LaunchResult.Error("एप्लिकेशन $displayName खोलने में असमर्थ, सर।")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch $packageName: ${e.message}")
            LaunchResult.Error("एप्लिकेशन खोलने में त्रुटि, सर: ${e.message}")
        }
    }

    fun safeStartActivity(intent: Intent): Boolean {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val activeActivity = com.max.assistant.MainActivity.activeInstance
        if (activeActivity != null && !activeActivity.isFinishing && !activeActivity.isDestroyed) {
            try {
                activeActivity.startActivity(intent)
                return true
            } catch (e: Exception) {
                Log.w(TAG, "activeActivity.startActivity failed: ${e.message}")
            }
        }

        try {
            val pendingIntent = android.app.PendingIntent.getActivity(
                context,
                (System.currentTimeMillis() % 10000).toInt(),
                intent,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
            )
            pendingIntent.send()
            return true
        } catch (e: Exception) {
            Log.w(TAG, "pendingIntent.send() failed: ${e.message}")
        }

        return try {
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Log.e(TAG, "context.startActivity failed: ${e.message}")
            false
        }
    }

    fun isPackageInstalled(packageName: String): Boolean {
        return try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(packageName, 0)
            }
            true
        } catch (e: Exception) {
            context.packageManager.getLaunchIntentForPackage(packageName) != null
        }
    }
}
