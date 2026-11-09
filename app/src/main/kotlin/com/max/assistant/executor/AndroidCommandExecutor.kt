package com.max.assistant.executor

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.Uri
import android.provider.AlarmClock
import android.provider.CalendarContract
import android.provider.ContactsContract
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.view.KeyEvent
import com.max.assistant.launcher.AppLauncher
import com.max.assistant.launcher.LaunchResult
import com.max.assistant.productivity.NotesAndTasksManager
import com.max.assistant.protection.GalleryProtectionManager
import com.max.assistant.telephony.PhoneCommunicationManager
import com.max.assistant.tts.VoiceLanguage
import com.max.assistant.util.SystemStatusManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

sealed class CommandExecutionResult {
    data class Success(val message: String, val actionType: String) : CommandExecutionResult()
    data class Blocked(val reason: String) : CommandExecutionResult()
    data class RequiresConfirmation(val prompt: String, val actionType: String) : CommandExecutionResult()
    data class Failed(val error: String) : CommandExecutionResult()
}

enum class VolumeDirection {
    UP,
    DOWN,
    MUTE
}

enum class MediaAction {
    PLAY,
    PAUSE,
    PLAY_PAUSE,
    NEXT,
    PREVIOUS,
    STOP
}

/**
 * AndroidCommandExecutor executes real Android system actions, intents, and hardware controls.
 * Adheres strictly to genuine Android APIs and prevents simulated responses.
 */
class AndroidCommandExecutor(
    private val context: Context,
    private val appLauncher: AppLauncher = AppLauncher(context),
    private val phoneManager: PhoneCommunicationManager = PhoneCommunicationManager(context),
    private val systemStatus: SystemStatusManager = SystemStatusManager(context),
    val notesAndTasks: NotesAndTasksManager = NotesAndTasksManager(context)
) {

    companion object {
        private const val TAG = "AndroidCommandExecutor"
    }

    private var isTorchOn = false

    // 1. Open Applications
    fun openApplication(query: String, targetLanguage: VoiceLanguage = VoiceLanguage.HINDI): CommandExecutionResult {
        if (GalleryProtectionManager.shouldBlockVoiceLaunch(query)) {
            val blockMsg = when (targetLanguage) {
                VoiceLanguage.HINDI -> "माफ़ कीजिए सर, गैलरी और तस्वीरों तक पहुंच सुरक्षा नीति द्वारा प्रतिबंधित है।"
                VoiceLanguage.BENGALI -> "ক্ষমা করবেন স্যার, গ্যালারি এবং ব্যক্তিগত ছবি সুরক্ষা নীতি দ্বারা নিষিদ্ধ।"
                else -> "I apologize Sir, access to the photo gallery is strictly restricted by security protocol."
            }
            return CommandExecutionResult.Blocked(blockMsg)
        }

        return when (val res = appLauncher.launchAppByVoice(query)) {
            is LaunchResult.Success -> {
                val msg = when (targetLanguage) {
                    VoiceLanguage.HINGLISH -> "Sir, ${res.appName} open kar raha hoon."
                    VoiceLanguage.HINDI -> "जी सर, ${res.appName} खोला जा रहा है।"
                    VoiceLanguage.BENGALI -> "হ্যাঁ স্যার, ${res.appName} খোলা হচ্ছে।"
                    else -> "Opening ${res.appName} for you, Sir."
                }
                CommandExecutionResult.Success(msg, "OPEN_APP")
            }
            is LaunchResult.Blocked -> {
                CommandExecutionResult.Blocked(res.reason)
            }
            is LaunchResult.NotFound -> {
                val msg = when (targetLanguage) {
                    VoiceLanguage.HINGLISH -> "Sorry Sir, '$query' application phone mein nahi mila."
                    VoiceLanguage.HINDI -> "माफ़ कीजिए सर, '$query' एप्लिकेशन आपके फ़ोन में नहीं मिला।"
                    VoiceLanguage.BENGALI -> "ক্ষমা করবেন স্যার, '$query' অ্যাপ্লিকেশনটি পাওয়া যায়নি।"
                    else -> "Sir, I could not find the '$query' application installed on your device."
                }
                CommandExecutionResult.Failed(msg)
            }
            is LaunchResult.Error -> {
                CommandExecutionResult.Failed(res.message)
            }
        }
    }

    // 2. Open Phone Dialer
    fun openPhoneApp(targetLanguage: VoiceLanguage): CommandExecutionResult {
        return try {
            val intent = Intent(Intent.ACTION_DIAL).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            appLauncher.safeStartActivity(intent)
            val msg = when (targetLanguage) {
                VoiceLanguage.HINGLISH -> "Sir, Phone dialer open kar raha hoon."
                VoiceLanguage.HINDI -> "जी सर, फ़ोन डायलर खोला जा रहा है।"
                VoiceLanguage.BENGALI -> "হ্যাঁ স্যার, ফোন ডায়ালার খোলা হচ্ছে।"
                else -> "Opening Phone dialer, Sir."
            }
            CommandExecutionResult.Success(msg, "OPEN_PHONE")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open phone dialer: ${e.message}")
            openApplication("phone", targetLanguage)
        }
    }

    // 3. Open Contacts
    fun openContactsApp(targetLanguage: VoiceLanguage): CommandExecutionResult {
        return try {
            val intent = Intent(Intent.ACTION_VIEW, ContactsContract.Contacts.CONTENT_URI).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            appLauncher.safeStartActivity(intent)
            val msg = when (targetLanguage) {
                VoiceLanguage.HINGLISH -> "Sir, Contacts open kar raha hoon."
                VoiceLanguage.HINDI -> "जी सर, संपर्क (कॉन्टैक्ट्स) खोले जा रहे हैं।"
                VoiceLanguage.BENGALI -> "হ্যাঁ স্যার, কনট্যাক্টস খোলা হচ্ছে।"
                else -> "Opening Contacts, Sir."
            }
            CommandExecutionResult.Success(msg, "OPEN_CONTACTS")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open contacts: ${e.message}")
            openApplication("contacts", targetLanguage)
        }
    }

    // 4. Open Messages App
    fun openMessagesApp(targetLanguage: VoiceLanguage): CommandExecutionResult {
        return try {
            val intent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_APP_MESSAGING)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            appLauncher.safeStartActivity(intent)
            val msg = when (targetLanguage) {
                VoiceLanguage.HINGLISH -> "Sir, Messages open kar raha hoon."
                VoiceLanguage.HINDI -> "जी सर, संदेश (मैसेज) एप्लिकेशन खोला जा रहा है।"
                VoiceLanguage.BENGALI -> "হ্যাঁ স্যার, মেসেজ অ্যাপ্লিকেশন খোলা হচ্ছে।"
                else -> "Opening Messages application, Sir."
            }
            CommandExecutionResult.Success(msg, "OPEN_MESSAGES")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open messages: ${e.message}")
            openApplication("messages", targetLanguage)
        }
    }

    // 5. Open Camera App
    fun openCameraApp(targetLanguage: VoiceLanguage): CommandExecutionResult {
        return try {
            val intent = Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            appLauncher.safeStartActivity(intent)
            val msg = when (targetLanguage) {
                VoiceLanguage.HINGLISH -> "Sir, Camera open kar raha hoon."
                VoiceLanguage.HINDI -> "जी सर, कैमरा सक्रिय किया जा रहा है।"
                VoiceLanguage.BENGALI -> "হ্যাঁ স্যার, ক্যামেরা সক্রিয় করা হচ্ছে।"
                else -> "Opening Camera for you, Sir."
            }
            CommandExecutionResult.Success(msg, "OPEN_CAMERA")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open camera: ${e.message}")
            openApplication("camera", targetLanguage)
        }
    }

    // 6. Open Settings App or Specific Subpage
    fun openSettingsApp(subpage: String = "", targetLanguage: VoiceLanguage): CommandExecutionResult {
        return try {
            val sub = subpage.lowercase().trim()
            val (action, screenName) = when {
                sub.contains("wifi") || sub.contains("wi-fi") || sub.contains("वाईफाई") || sub.contains("ওয়াইফাই") -> 
                    Pair(Settings.ACTION_WIFI_SETTINGS, "Wi-Fi Settings")
                sub.contains("bluetooth") || sub.contains("ब्लूटूथ") || sub.contains("ব্লুটুথ") -> 
                    Pair(Settings.ACTION_BLUETOOTH_SETTINGS, "Bluetooth Settings")
                sub.contains("location") || sub.contains("gps") || sub.contains("लोकेशन") || sub.contains("स्थान") || sub.contains("অবস্থান") -> 
                    Pair(Settings.ACTION_LOCATION_SOURCE_SETTINGS, "Location Settings")
                sub.contains("accessibility") || sub.contains("एक्सेसिबिलिटी") || sub.contains("অ্যাক্সেসিবিলিটি") -> 
                    Pair(Settings.ACTION_ACCESSIBILITY_SETTINGS, "Accessibility Settings")
                sub.contains("sound") || sub.contains("volume") || sub.contains("ध्वनि") || sub.contains("আওয়াজ") -> 
                    Pair(Settings.ACTION_SOUND_SETTINGS, "Sound Settings")
                sub.contains("display") || sub.contains("brightness") || sub.contains("स्क्रीन") || sub.contains("डिस्प्ले") || sub.contains("ডিসপ্লে") -> 
                    Pair(Settings.ACTION_DISPLAY_SETTINGS, "Display Settings")
                sub.contains("app") || sub.contains("permission") || sub.contains("अनुमति") || sub.contains("एप्लिकेशन") -> 
                    Pair(Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS, "Application Settings")
                else -> 
                    Pair(Settings.ACTION_SETTINGS, "System Settings")
            }
            val intent = Intent(action).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            appLauncher.safeStartActivity(intent)
            val msg = when (targetLanguage) {
                VoiceLanguage.HINGLISH -> "Sir, $screenName open kar raha hoon."
                VoiceLanguage.HINDI -> "सर, $screenName स्क्रीन खोली जा रही है जहां से आप इसे बदल सकते हैं।"
                VoiceLanguage.BENGALI -> "স্যার, $screenName স্ক্রিন খোলা হচ্ছে যেখান থেকে আপনি এটি পরিবর্তন করতে পারবেন।"
                else -> "Opening $screenName for you, Sir."
            }
            CommandExecutionResult.Success(msg, "OPEN_SETTINGS")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open settings: ${e.message}")
            CommandExecutionResult.Failed("सेटिंग्स खोलने में त्रुटि: ${e.message}")
        }
    }

    // 7. Open Chrome or Default Browser
    fun openBrowser(url: String = "https://www.google.com", targetLanguage: VoiceLanguage): CommandExecutionResult {
        return try {
            val uri = Uri.parse(url)
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                setPackage("com.android.chrome")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            if (!appLauncher.safeStartActivity(intent)) {
                val fallback = Intent(Intent.ACTION_VIEW, uri).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                }
                appLauncher.safeStartActivity(fallback)
            }
            val msg = when (targetLanguage) {
                VoiceLanguage.HINGLISH -> "Sir, Chrome browser open kar raha hoon."
                VoiceLanguage.HINDI -> "जी सर, ब्राउज़र खोला जा रहा है।"
                VoiceLanguage.BENGALI -> "হ্যাঁ স্যার, ব্রাউজার খোলা হচ্ছে।"
                else -> "Opening Chrome browser, Sir."
            }
            CommandExecutionResult.Success(msg, "OPEN_BROWSER")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open browser: ${e.message}")
            CommandExecutionResult.Failed("ब्राउज़र खोलने में त्रुटि: ${e.message}")
        }
    }

    // 8. Open Maps
    fun openMaps(query: String = "", targetLanguage: VoiceLanguage): CommandExecutionResult {
        return try {
            val uri = if (query.isNotBlank()) {
                Uri.parse("geo:0,0?q=${Uri.encode(query)}")
            } else {
                Uri.parse("geo:0,0?q=maps")
            }
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                setPackage("com.google.android.apps.maps")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            if (!appLauncher.safeStartActivity(intent)) {
                val fallback = Intent(Intent.ACTION_VIEW, uri).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                }
                appLauncher.safeStartActivity(fallback)
            }
            val msg = when (targetLanguage) {
                VoiceLanguage.HINGLISH -> "Sir, Google Maps open kar raha hoon."
                VoiceLanguage.HINDI -> "जी सर, गूगल मैप्स खोला जा रहा है।"
                VoiceLanguage.BENGALI -> "হ্যাঁ স্যার, গুগল ম্যাপস খোলা হচ্ছে।"
                else -> "Opening Maps, Sir."
            }
            CommandExecutionResult.Success(msg, "OPEN_MAPS")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open maps: ${e.message}")
            CommandExecutionResult.Failed("नक्शा खोलने में त्रुटि: ${e.message}")
        }
    }

    // 8a. Open WhatsApp
    fun openWhatsAppApp(business: Boolean = false, targetLanguage: VoiceLanguage): CommandExecutionResult {
        val targetPkg = if (business) "com.whatsapp.w4b" else "com.whatsapp"
        val fallbackPkg = if (business) "com.whatsapp" else "com.whatsapp.w4b"

        val pkgToLaunch = if (appLauncher.isPackageInstalled(targetPkg)) {
            targetPkg
        } else if (appLauncher.isPackageInstalled(fallbackPkg)) {
            fallbackPkg
        } else {
            null
        }

        if (pkgToLaunch == null) {
            val notFoundMsg = when (targetLanguage) {
                VoiceLanguage.HINDI -> "सर, आपके फोन में व्हाट्सएप इंस्टॉल नहीं मिला।"
                VoiceLanguage.BENGALI -> "স্যার, আপনার ফোনে হোয়াটসঅ্যাপ ইনস্টল করা নেই।"
                else -> "Sir, WhatsApp is not installed on your device."
            }
            return CommandExecutionResult.Failed(notFoundMsg)
        }

        return try {
            val intent = context.packageManager.getLaunchIntentForPackage(pkgToLaunch)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                appLauncher.safeStartActivity(intent)
                val msg = when (targetLanguage) {
                    VoiceLanguage.HINGLISH -> "Sir, WhatsApp open kar raha hoon."
                    VoiceLanguage.HINDI -> "जी सर, व्हाट्सएप खोला जा रहा है।"
                    VoiceLanguage.BENGALI -> "হ্যাঁ স্যার, হোয়াটসঅ্যাপ খোলা হচ্ছে।"
                    else -> "Opening WhatsApp, Sir."
                }
                CommandExecutionResult.Success(msg, "OPEN_WHATSAPP")
            } else {
                CommandExecutionResult.Failed("WhatsApp could not be opened, Sir.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error opening WhatsApp: ${e.message}")
            CommandExecutionResult.Failed("Error opening WhatsApp: ${e.message}")
        }
    }

    // 8b. Web Search
    fun searchWeb(query: String, targetLanguage: VoiceLanguage): CommandExecutionResult {
        return try {
            val cleanQuery = query.trim()
            val uri = Uri.parse("https://www.google.com/search?q=" + Uri.encode(cleanQuery))
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            val msg = when (targetLanguage) {
                VoiceLanguage.HINGLISH -> "Sir, '$cleanQuery' ke liye web search kar raha hoon."
                VoiceLanguage.HINDI -> "सर, '$cleanQuery' के लिए इंटरनेट पर खोज की जा रही है।"
                VoiceLanguage.BENGALI -> "স্যার, '$cleanQuery'-এর জন্য ইন্টারনেটে অনুসন্ধান করা হচ্ছে।"
                else -> "Searching the web for '$cleanQuery', Sir."
            }
            CommandExecutionResult.Success(msg, "SEARCH_WEB")
        } catch (e: Exception) {
            Log.e(TAG, "Web search failed: ${e.message}")
            CommandExecutionResult.Failed("Web search error: ${e.message}")
        }
    }

    // 9. Set Alarm Clock
    fun setAlarm(hour: Int, minute: Int, message: String = "MAX Alarm", targetLanguage: VoiceLanguage): CommandExecutionResult {
        val success = systemStatus.setAlarm(hour, minute, message)
        val timeStr = String.format(Locale.US, "%02d:%02d", hour, minute)
        return if (success) {
            val msg = when (targetLanguage) {
                VoiceLanguage.HINGLISH -> "Sir, $timeStr ka alarm set kar diya hai."
                VoiceLanguage.HINDI -> "सर, $timeStr का अलार्म सफलतापूर्वक लगा दिया गया है।"
                VoiceLanguage.BENGALI -> "স্যার, $timeStr এর এলার্ম সফলভাবে নির্ধারণ করা হয়েছে।"
                else -> "Sir, an alarm has been scheduled for $timeStr."
            }
            CommandExecutionResult.Success(msg, "SET_ALARM")
        } else {
            val err = when (targetLanguage) {
                VoiceLanguage.HINDI -> "माफ़ कीजिए सर, अलार्म सेट करने में असमर्थ रहा।"
                VoiceLanguage.BENGALI -> "ক্ষমা করবেন স্যার, এলার্ম সেট করা যায়নি।"
                else -> "Sir, unable to set alarm on the device clock."
            }
            CommandExecutionResult.Failed(err)
        }
    }

    // 10. Create Calendar Event
    fun createCalendarEvent(title: String = "New Event", targetLanguage: VoiceLanguage): CommandExecutionResult {
        val success = systemStatus.openCalendarNewEvent(title)
        return if (success) {
            val msg = when (targetLanguage) {
                VoiceLanguage.HINDI -> "जी सर, नया कार्यक्रम जोड़ने के लिए कैलेंडर खोला जा रहा है।"
                VoiceLanguage.BENGALI -> "হ্যাঁ স্যার, নতুন ইভেন্ট যুক্ত করতে ক্যালেন্ডার খোলা হচ্ছে।"
                else -> "Opening Calendar to schedule your event, Sir."
            }
            CommandExecutionResult.Success(msg, "CREATE_CALENDAR_EVENT")
        } else {
            CommandExecutionResult.Failed("कैलेंडर खोलने में असमर्थ, सर।")
        }
    }

    // 11. Adjust Volume Settings
    fun adjustVolume(direction: VolumeDirection, targetLanguage: VoiceLanguage): CommandExecutionResult {
        val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            ?: return CommandExecutionResult.Failed("ऑडियो सेवा अनुपलब्ध है।")
        return try {
            when (direction) {
                VolumeDirection.UP -> {
                    am.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI)
                    val msg = when (targetLanguage) {
                        VoiceLanguage.HINGLISH -> "Sir, volume badha diya hai."
                        VoiceLanguage.HINDI -> "सर, आवाज़ बढ़ा दी गई है।"
                        VoiceLanguage.BENGALI -> "স্যার, ভলিউম বাড়ানো হয়েছে।"
                        else -> "Volume increased, Sir."
                    }
                    CommandExecutionResult.Success(msg, "VOLUME_UP")
                }
                VolumeDirection.DOWN -> {
                    am.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI)
                    val msg = when (targetLanguage) {
                        VoiceLanguage.HINGLISH -> "Sir, volume kam kar diya hai."
                        VoiceLanguage.HINDI -> "सर, आवाज़ कम कर दी गई है।"
                        VoiceLanguage.BENGALI -> "স্যার, ভলিউম কমানো হয়েছে।"
                        else -> "Volume decreased, Sir."
                    }
                    CommandExecutionResult.Success(msg, "VOLUME_DOWN")
                }
                VolumeDirection.MUTE -> {
                    am.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_MUTE, AudioManager.FLAG_SHOW_UI)
                    val msg = when (targetLanguage) {
                        VoiceLanguage.HINGLISH -> "Sir, volume mute kar diya hai."
                        VoiceLanguage.HINDI -> "सर, मीडिया आवाज़ म्यूट कर दी गई है।"
                        VoiceLanguage.BENGALI -> "স্যার, মিডিয়া ভলিউম মিউট করা হয়েছে।"
                        else -> "Media volume muted, Sir."
                    }
                    CommandExecutionResult.Success(msg, "VOLUME_MUTE")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to adjust volume: ${e.message}")
            CommandExecutionResult.Failed("आवाज़ नियंत्रित करने में त्रुटि: ${e.message}")
        }
    }

    // 12. Turn Flashlight On / Off
    fun setFlashlight(enable: Boolean, targetLanguage: VoiceLanguage): CommandExecutionResult {
        val cm = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
            ?: return CommandExecutionResult.Failed("कैमरा सेवा अनुपलब्ध है।")
        return try {
            val cameraId = cm.cameraIdList.firstOrNull()
                ?: return CommandExecutionResult.Failed("डिवाइस में फ्लैशलाइट उपलब्ध नहीं है।")
            cm.setTorchMode(cameraId, enable)
            isTorchOn = enable
            val msg = if (enable) {
                when (targetLanguage) {
                    VoiceLanguage.HINGLISH -> "Sir, flashlight on kar di hai."
                    VoiceLanguage.HINDI -> "सर, फ्लैशलाइट चालू कर दी गई है।"
                    VoiceLanguage.BENGALI -> "স্যার, ফ্ল্যাশলাইট চালু করা হয়েছে।"
                    else -> "Flashlight turned on, Sir."
                }
            } else {
                when (targetLanguage) {
                    VoiceLanguage.HINGLISH -> "Sir, flashlight band kar di hai."
                    VoiceLanguage.HINDI -> "सर, फ्लैशलाइट बंद कर दी गई है।"
                    VoiceLanguage.BENGALI -> "স্যার, ফ্ল্যাশলাইট বন্ধ করা হয়েছে।"
                    else -> "Flashlight turned off, Sir."
                }
            }
            CommandExecutionResult.Success(msg, if (enable) "TORCH_ON" else "TORCH_OFF")
        } catch (e: Exception) {
            Log.e(TAG, "Flashlight control error: ${e.message}")
            CommandExecutionResult.Failed("फ्लैशलाइट नियंत्रण में त्रुटि: ${e.message}")
        }
    }

    // 13. Read Time
    fun readCurrentTime(targetLanguage: VoiceLanguage): CommandExecutionResult {
        val statement = systemStatus.getCurrentTime(targetLanguage)
        return CommandExecutionResult.Success(statement, "READ_TIME")
    }

    // 14. Read Date
    fun readCurrentDate(targetLanguage: VoiceLanguage): CommandExecutionResult {
        val statement = systemStatus.getTodayDate(targetLanguage)
        return CommandExecutionResult.Success(statement, "READ_DATE")
    }

    // 15. Report Battery Level
    fun reportBattery(targetLanguage: VoiceLanguage): CommandExecutionResult {
        val statement = systemStatus.getBatteryStatement(targetLanguage)
        return CommandExecutionResult.Success(statement, "REPORT_BATTERY")
    }

    // 16. Set Timer
    fun setTimer(seconds: Int, message: String = "MAX Timer", targetLanguage: VoiceLanguage): CommandExecutionResult {
        val success = systemStatus.setTimer(seconds, message)
        return if (success) {
            val minutes = seconds / 60
            val secs = seconds % 60
            val timeText = if (minutes > 0) "$minutes मिनट $secs सेकंड" else "$seconds सेकंड"
            val msg = when (targetLanguage) {
                VoiceLanguage.HINDI -> "सर, $timeText का टाइमर शुरू कर दिया गया है।"
                VoiceLanguage.BENGALI -> "স্যার, $timeText এর টাইমার চালু করা হয়েছে।"
                else -> "Timer started for $seconds seconds, Sir."
            }
            CommandExecutionResult.Success(msg, "SET_TIMER")
        } else {
            CommandExecutionResult.Failed("माफ़ कीजिए सर, टाइमर सेट करने में असमर्थ रहा।")
        }
    }

    // 17. Media Controls (Play/Pause, Next, Previous, Stop)
    fun controlMedia(action: MediaAction, targetLanguage: VoiceLanguage): CommandExecutionResult {
        val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            ?: return CommandExecutionResult.Failed("ऑडियो सेवा अनुपलब्ध है, सर।")
        val keyCode = when (action) {
            MediaAction.PLAY -> KeyEvent.KEYCODE_MEDIA_PLAY
            MediaAction.PAUSE -> KeyEvent.KEYCODE_MEDIA_PAUSE
            MediaAction.PLAY_PAUSE -> KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
            MediaAction.NEXT -> KeyEvent.KEYCODE_MEDIA_NEXT
            MediaAction.PREVIOUS -> KeyEvent.KEYCODE_MEDIA_PREVIOUS
            MediaAction.STOP -> KeyEvent.KEYCODE_MEDIA_STOP
        }
        return try {
            am.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
            am.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyCode))
            val msg = when (action) {
                MediaAction.PLAY -> when (targetLanguage) {
                    VoiceLanguage.HINDI -> "सर, मीडिया चलाया जा रहा है।"
                    VoiceLanguage.BENGALI -> "স্যার, মিডিয়া চালানো হচ্ছে।"
                    else -> "Media playback started, Sir."
                }
                MediaAction.PAUSE -> when (targetLanguage) {
                    VoiceLanguage.HINDI -> "सर, मीडिया रोक दिया गया है।"
                    VoiceLanguage.BENGALI -> "স্যার, মিডিয়া থামানো হয়েছে।"
                    else -> "Media playback paused, Sir."
                }
                MediaAction.PLAY_PAUSE -> when (targetLanguage) {
                    VoiceLanguage.HINDI -> "सर, मीडिया प्ले/पॉज़ किया गया।"
                    VoiceLanguage.BENGALI -> "স্যার, মিডিয়া প্লে/পজ করা হয়েছে।"
                    else -> "Media playback toggled, Sir."
                }
                MediaAction.NEXT -> when (targetLanguage) {
                    VoiceLanguage.HINDI -> "सर, अगला ट्रैक चलाया जा रहा है।"
                    VoiceLanguage.BENGALI -> "স্যার, পরবর্তী ট্র্যাক চালানো হচ্ছে।"
                    else -> "Playing next track, Sir."
                }
                MediaAction.PREVIOUS -> when (targetLanguage) {
                    VoiceLanguage.HINDI -> "सर, पिछला ट्रैक चलाया जा रहा है।"
                    VoiceLanguage.BENGALI -> "স্যার, পূর্ববর্তী ট্র্যাক চালানো হচ্ছে।"
                    else -> "Playing previous track, Sir."
                }
                MediaAction.STOP -> when (targetLanguage) {
                    VoiceLanguage.HINDI -> "सर, मीडिया बंद कर दिया गया है।"
                    VoiceLanguage.BENGALI -> "স্যার, মিডিয়া বন্ধ করা হয়েছে।"
                    else -> "Media stopped, Sir."
                }
            }
            CommandExecutionResult.Success(msg, "MEDIA_CONTROL")
        } catch (e: Exception) {
            Log.e(TAG, "Media control failed: ${e.message}")
            CommandExecutionResult.Failed("मीडिया नियंत्रण में त्रुटि: ${e.message}")
        }
    }

    // 17b. Play Music from Spotify or available music player
    fun playMusic(query: String = "", preferredApp: String = "spotify", targetLanguage: VoiceLanguage): CommandExecutionResult {
        Log.i(TAG, "playMusic: query='$query', app='$preferredApp'")

        val spotifyPkg = "com.spotify.music"
        val ytMusicPkg = "com.google.android.apps.youtube.music"
        val samsungMusicPkg = "com.sec.android.app.music"

        val isSpotifyInstalled = appLauncher.isPackageInstalled(spotifyPkg)
        val isYtMusicInstalled = appLauncher.isPackageInstalled(ytMusicPkg)
        val isSamsungMusicInstalled = appLauncher.isPackageInstalled(samsungMusicPkg)

        // Case 1: Specific query / song / artist
        if (query.isNotBlank()) {
            val cleanQuery = query.trim()
            if (isSpotifyInstalled) {
                try {
                    val spotifyUri = Uri.parse("spotify:search:" + Uri.encode(cleanQuery))
                    val intent = Intent(Intent.ACTION_VIEW, spotifyUri).apply {
                        setPackage(spotifyPkg)
                    }
                    val launched = appLauncher.safeStartActivity(intent)
                    if (launched) {
                        dispatchPlayKeyEventDelayed()
                        val msg = when (targetLanguage) {
                            VoiceLanguage.HINGLISH -> "Sir, Spotify pe '$cleanQuery' play kar raha hoon."
                            VoiceLanguage.HINDI -> "जी सर, स्पॉटीफ़ाई पर '$cleanQuery' चलाया जा रहा है।"
                            VoiceLanguage.BENGALI -> "হ্যাঁ স্যার, স্পটিফাইতে '$cleanQuery' চালানো হচ্ছে।"
                            else -> "Playing '$cleanQuery' on Spotify for you, Sir."
                        }
                        return CommandExecutionResult.Success(msg, "PLAY_MUSIC")
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Spotify search failed: ${e.message}")
                }
            }

            // Fallback: MediaStore search intent
            try {
                val searchIntent = Intent(MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH).apply {
                    putExtra(android.app.SearchManager.QUERY, cleanQuery)
                    if (isSpotifyInstalled) setPackage(spotifyPkg)
                    else if (isYtMusicInstalled) setPackage(ytMusicPkg)
                }
                val launched = appLauncher.safeStartActivity(searchIntent)
                if (launched) {
                    dispatchPlayKeyEventDelayed()
                    val msg = when (targetLanguage) {
                        VoiceLanguage.HINDI -> "जी सर, '$cleanQuery' संगीत चलाया जा रहा है।"
                        VoiceLanguage.BENGALI -> "হ্যাঁ স্যার, '$cleanQuery' গান চালানো হচ্ছে।"
                        else -> "Playing '$cleanQuery' for you, Sir."
                    }
                    return CommandExecutionResult.Success(msg, "PLAY_MUSIC")
                }
            } catch (e: Exception) {
                Log.w(TAG, "MEDIA_PLAY_FROM_SEARCH failed: ${e.message}")
            }
        }

        // Case 2: General music playback
        if (isSpotifyInstalled) {
            val res = appLauncher.launchAppByVoice("spotify")
            dispatchPlayKeyEventDelayed()
            val msg = when (targetLanguage) {
                VoiceLanguage.HINGLISH -> "Sir, Spotify pe music start kar raha hoon."
                VoiceLanguage.HINDI -> "जी सर, स्पॉटीफ़ाई पर संगीत शुरू किया जा रहा है।"
                VoiceLanguage.BENGALI -> "হ্যাঁ স্যার, স্পটিফাইতে গান শুরু করা হচ্ছে।"
                else -> "Starting Spotify for you, Sir."
            }
            return CommandExecutionResult.Success(msg, "PLAY_MUSIC")
        }

        if (isYtMusicInstalled) {
            val res = appLauncher.launchAppByVoice("youtube music")
            dispatchPlayKeyEventDelayed()
            val msg = when (targetLanguage) {
                VoiceLanguage.HINDI -> "जी सर, यूट्यूब म्यूज़िक पर संगीत शुरू किया जा रहा है।"
                VoiceLanguage.BENGALI -> "হ্যাঁ স্যার, ইউটিউব মিউজিকে গান শুরু করা হচ্ছে।"
                else -> "Starting YouTube Music for you, Sir."
            }
            return CommandExecutionResult.Success(msg, "PLAY_MUSIC")
        }

        if (isSamsungMusicInstalled) {
            val res = appLauncher.launchAppByVoice("music")
            dispatchPlayKeyEventDelayed()
            val msg = when (targetLanguage) {
                VoiceLanguage.HINDI -> "जी सर, संगीत शुरू किया जा रहा है।"
                VoiceLanguage.BENGALI -> "হ্যাঁ স্যার, গান শুরু করা হচ্ছে।"
                else -> "Starting music player for you, Sir."
            }
            return CommandExecutionResult.Success(msg, "PLAY_MUSIC")
        }

        // Direct audio media play key
        val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        if (am != null) {
            try {
                am.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PLAY))
                am.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PLAY))
                val msg = when (targetLanguage) {
                    VoiceLanguage.HINDI -> "जी सर, संगीत चलाया जा रहा है।"
                    VoiceLanguage.BENGALI -> "হ্যাঁ স্যার, গান চালানো হচ্ছে।"
                    else -> "Resuming music playback, Sir."
                }
                return CommandExecutionResult.Success(msg, "PLAY_MUSIC")
            } catch (_: Exception) {}
        }

        return CommandExecutionResult.Failed("माफ़ कीजिए सर, आपके फोन में कोई संगीत एप्लिकेशन नहीं मिला।")
    }

    private fun dispatchPlayKeyEventDelayed() {
        CoroutineScope(Dispatchers.IO).launch {
            delay(800)
            val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return@launch
            try {
                am.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PLAY))
                am.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PLAY))
            } catch (_: Exception) {}
        }
    }

    // 18. Search Nearby (Hospitals, Restaurants, Hotels, Fuel, etc.)
    fun searchNearby(category: String, targetLanguage: VoiceLanguage): CommandExecutionResult {
        val cleanCat = category.trim()
        val query = "$cleanCat near me"
        val uri = Uri.parse("geo:0,0?q=" + Uri.encode(query))
        return try {
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                setPackage("com.google.android.apps.maps")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                context.startActivity(intent)
            } catch (e: Exception) {
                val fallbackIntent = Intent(Intent.ACTION_VIEW, uri).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(fallbackIntent)
            }
            val msg = when (targetLanguage) {
                VoiceLanguage.HINDI -> "सर, पास के $cleanCat खोजे जा रहे हैं।"
                VoiceLanguage.BENGALI -> "স্যার, কাছাকাছি $cleanCat খোঁজা হচ্ছে।"
                else -> "Searching nearby $cleanCat for you, Sir."
            }
            CommandExecutionResult.Success(msg, "SEARCH_NEARBY")
        } catch (e: Exception) {
            CommandExecutionResult.Failed("नक्शा खोलने में त्रुटि: ${e.message}")
        }
    }

    // 19. Open Navigation Directions
    fun openDirections(destination: String, targetLanguage: VoiceLanguage): CommandExecutionResult {
        val dest = destination.trim()
        val uri = Uri.parse("google.navigation:q=" + Uri.encode(dest))
        return try {
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                setPackage("com.google.android.apps.maps")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                context.startActivity(intent)
            } catch (e: Exception) {
                val webUri = Uri.parse("https://www.google.com/maps/dir/?api=1&destination=" + Uri.encode(dest))
                context.startActivity(Intent(Intent.ACTION_VIEW, webUri).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
            }
            val msg = when (targetLanguage) {
                VoiceLanguage.HINDI -> "सर, $dest के लिए दिशा-निर्देश (नेविगेशन) खोले जा रहे हैं।"
                VoiceLanguage.BENGALI -> "স্যার, $dest এর দিকনির্দেশ খোলা হচ্ছে।"
                else -> "Opening navigation directions to $dest, Sir."
            }
            CommandExecutionResult.Success(msg, "OPEN_DIRECTIONS")
        } catch (e: Exception) {
            CommandExecutionResult.Failed("दिशा-निर्देश खोलने में त्रुटि: ${e.message}")
        }
    }

    // 20. Clipboard & Sharing
    fun copyToClipboard(text: String, targetLanguage: VoiceLanguage): CommandExecutionResult {
        return try {
            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                ?: return CommandExecutionResult.Failed("क्लिपबोर्ड सेवा अनुपलब्ध है।")
            val clip = ClipData.newPlainText("MAX Note", text)
            cm.setPrimaryClip(clip)
            val msg = when (targetLanguage) {
                VoiceLanguage.HINDI -> "सर, टेक्स्ट क्लिपबोर्ड पर कॉपी कर दिया गया है।"
                VoiceLanguage.BENGALI -> "স্যার, টেক্সট ক্লিপবোর্ডে কপি করা হয়েছে।"
                else -> "Text copied to clipboard, Sir."
            }
            CommandExecutionResult.Success(msg, "COPY_TO_CLIPBOARD")
        } catch (e: Exception) {
            CommandExecutionResult.Failed("क्लिपबोर्ड त्रुटि: ${e.message}")
        }
    }

    fun shareText(text: String, targetLanguage: VoiceLanguage): CommandExecutionResult {
        return try {
            val sendIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, text)
                type = "text/plain"
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val shareIntent = Intent.createChooser(sendIntent, "Share with MAX").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(shareIntent)
            val msg = when (targetLanguage) {
                VoiceLanguage.HINDI -> "जी सर, शेयर करने के लिए विकल्प खोले जा रहे हैं।"
                VoiceLanguage.BENGALI -> "হ্যাঁ স্যার, শেয়ার করার অপশন খোলা হচ্ছে।"
                else -> "Opening share options, Sir."
            }
            CommandExecutionResult.Success(msg, "SHARE_TEXT")
        } catch (e: Exception) {
            CommandExecutionResult.Failed("शेयर करने में त्रुटि: ${e.message}")
        }
    }

    // 21. Productivity: Notes
    fun createNote(content: String, targetLanguage: VoiceLanguage): CommandExecutionResult {
        val note = notesAndTasks.addNote(content)
        val msg = when (targetLanguage) {
            VoiceLanguage.HINDI -> "सर, नोट सुरक्षित कर लिया गया है: '${note.content}'"
            VoiceLanguage.BENGALI -> "স্যার, নোট সংরক্ষণ করা হয়েছে: '${note.content}'"
            else -> "Note saved successfully, Sir: '${note.content}'"
        }
        return CommandExecutionResult.Success(msg, "CREATE_NOTE")
    }

    fun readNotes(targetLanguage: VoiceLanguage): CommandExecutionResult {
        val summary = notesAndTasks.formatNotesSummary(targetLanguage)
        return CommandExecutionResult.Success(summary, "READ_NOTES")
    }

    // 22. Productivity: Tasks
    fun addTask(title: String, targetLanguage: VoiceLanguage): CommandExecutionResult {
        val task = notesAndTasks.addTask(title)
        val msg = when (targetLanguage) {
            VoiceLanguage.HINDI -> "सर, कार्य सूची में जोड़ा गया: '${task.title}'"
            VoiceLanguage.BENGALI -> "স্যার, কাজের তালিকায় যুক্ত করা হয়েছে: '${task.title}'"
            else -> "Task added to list, Sir: '${task.title}'"
        }
        return CommandExecutionResult.Success(msg, "ADD_TASK")
    }

    fun readTasks(targetLanguage: VoiceLanguage): CommandExecutionResult {
        val summary = notesAndTasks.formatTasksSummary(targetLanguage)
        return CommandExecutionResult.Success(summary, "READ_TASKS")
    }

    // 23. Draft Email
    fun draftEmail(recipient: String, subject: String, body: String, targetLanguage: VoiceLanguage): CommandExecutionResult {
        val success = try {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:" + Uri.encode(recipient.trim()))
                putExtra(Intent.EXTRA_SUBJECT, subject)
                putExtra(Intent.EXTRA_TEXT, body)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
        return if (success) {
            val msg = when (targetLanguage) {
                VoiceLanguage.HINDI -> "जी सर, ईमेल ड्राफ्ट खोला जा रहा है।"
                VoiceLanguage.BENGALI -> "হ্যাঁ স্যার, ইমেল ড্রাফট খোলা হচ্ছে।"
                else -> "Drafting email for you, Sir."
            }
            CommandExecutionResult.Success(msg, "DRAFT_EMAIL")
        } else {
            CommandExecutionResult.Failed("ईमेल ऐप खोलने में असमर्थ, सर।")
        }
    }

    // 24. Find Contact
    fun findContact(name: String, targetLanguage: VoiceLanguage): CommandExecutionResult {
        val contact = phoneManager.findContact(name)
        return if (contact != null) {
            val phone = contact.phoneNumber.ifBlank { "उपलब्ध नहीं" }
            val msg = when (targetLanguage) {
                VoiceLanguage.HINDI -> "सर, संपर्क मिला: ${contact.displayName}, फ़ोन नंबर: $phone"
                VoiceLanguage.BENGALI -> "স্যার, যোগাযোগ পাওয়া গেছে: ${contact.displayName}, নম্বর: $phone"
                else -> "Sir, contact found: ${contact.displayName}, Phone: $phone"
            }
            CommandExecutionResult.Success(msg, "FIND_CONTACT")
        } else {
            val msg = when (targetLanguage) {
                VoiceLanguage.HINDI -> "माफ़ कीजिए सर, '$name' नाम का कोई संपर्क नहीं मिला।"
                VoiceLanguage.BENGALI -> "ক্ষমা করবেন স্যার, '$name' নামে কোনো যোগাযোগ পাওয়া যায়নি।"
                else -> "Sir, no contact named '$name' was found on your device."
            }
            CommandExecutionResult.Failed(msg)
        }
    }
}
