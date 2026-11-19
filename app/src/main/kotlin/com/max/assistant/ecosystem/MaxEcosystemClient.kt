package com.max.assistant.ecosystem

import android.content.Context
import android.util.Log
import com.max.assistant.tts.VoiceLanguage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

data class ConnectedDevice(
    val id: String,
    val name: String,
    val platform: String,
    val status: String,
    val capabilities: List<String>
)

data class RemoteCommandResult(
    val success: Boolean,
    val message: String,
    val commandId: String? = null
)

/**
 * Android Client for the MAX Universal AI Ecosystem.
 * Handles device registration, cross-device conversation/memory sync,
 * remote device command dispatching (e.g. phone to laptop), and command execution.
 */
class MaxEcosystemClient(
    private val context: Context,
    private val scope: CoroutineScope,
    private val onExecuteRemoteCommand: (action: String, params: Map<String, String>) -> String = { _, _ -> "Executed" }
) {

    companion object {
        private const val TAG = "MaxEcosystemClient"
        const val DEVICE_ID = "dev_phone_s25"
        const val DEVICE_NAME = "Samsung Galaxy S25 Ultra"
        const val DEFAULT_BACKEND_URL = "http://10.0.2.2:8085"
        const val LOCALHOST_BACKEND_URL = "http://127.0.0.1:8085"
    }

    private var activeBackendUrl = DEFAULT_BACKEND_URL
    private val _connectedDevices = MutableStateFlow<List<ConnectedDevice>>(emptyList())
    val connectedDevices: StateFlow<List<ConnectedDevice>> = _connectedDevices.asStateFlow()

    private val _isBackendConnected = MutableStateFlow(false)
    val isBackendConnected: StateFlow<Boolean> = _isBackendConnected.asStateFlow()

    init {
        scope.launch(Dispatchers.IO) {
            findReachableBackend()
            startHeartbeatAndPollLoop()
        }
    }

    private suspend fun findReachableBackend() {
        val candidates = listOf("http://127.0.0.1:8085", "http://10.0.2.2:8085", "http://localhost:8085")
        for (url in candidates) {
            try {
                val conn = URL("$url/health").openConnection() as HttpURLConnection
                conn.connectTimeout = 1500
                conn.readTimeout = 1500
                if (conn.responseCode == 200) {
                    activeBackendUrl = url
                    _isBackendConnected.value = true
                    Log.i(TAG, "Connected to Central MAX Backend at: $activeBackendUrl")
                    registerThisDevice()
                    return
                }
            } catch (_: Exception) {}
        }
        // Default to local loopback
        activeBackendUrl = LOCALHOST_BACKEND_URL
    }

    private suspend fun registerThisDevice() = withContext(Dispatchers.IO) {
        try {
            val payload = JSONObject().apply {
                put("id", DEVICE_ID)
                put("name", DEVICE_NAME)
                put("platform", "android")
                put("capabilities", JSONArray(listOf(
                    "screen_vision", "voice_engine", "app_control",
                    "phone_call", "sms", "whatsapp", "flashlight", "notifications"
                )))
            }

            postJson("$activeBackendUrl/api/devices/register", payload)
            refreshDevicesList()
        } catch (e: Exception) {
            Log.w(TAG, "Device registration deferred: ${e.message}")
        }
    }

    private fun startHeartbeatAndPollLoop() {
        scope.launch(Dispatchers.IO) {
            while (isActive) {
                try {
                    refreshDevicesList()
                } catch (_: Exception) {}
                delay(6000L)
            }
        }
    }

    suspend fun refreshDevicesList() = withContext(Dispatchers.IO) {
        try {
            val conn = URL("$activeBackendUrl/api/devices").openConnection() as HttpURLConnection
            conn.connectTimeout = 2000
            conn.readTimeout = 2000
            if (conn.responseCode == 200) {
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                val array = JSONArray(body)
                val list = mutableListOf<ConnectedDevice>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val caps = mutableListOf<String>()
                    val cArray = obj.optJSONArray("capabilities")
                    if (cArray != null) {
                        for (j in 0 until cArray.length()) caps.add(cArray.getString(j))
                    }
                    list.add(
                        ConnectedDevice(
                            id = obj.getString("id"),
                            name = obj.getString("name"),
                            platform = obj.getString("platform"),
                            status = obj.optString("status", "online"),
                            capabilities = caps
                        )
                    )
                }
                _connectedDevices.value = list
                _isBackendConnected.value = true
            }
        } catch (e: Exception) {
            _isBackendConnected.value = false
        }
    }

    suspend fun syncChatMessage(content: String, role: String, language: VoiceLanguage) = withContext(Dispatchers.IO) {
        try {
            val payload = JSONObject().apply {
                put("senderDevice", DEVICE_ID)
                put("role", role)
                put("content", content)
                put("language", language.name)
            }
            postJson("$activeBackendUrl/api/conversations", payload)
        } catch (e: Exception) {
            Log.w(TAG, "Chat sync deferred: ${e.message}")
        }
    }

    suspend fun syncTask(title: String, dueAt: Long = 0) = withContext(Dispatchers.IO) {
        try {
            val payload = JSONObject().apply {
                put("title", title)
                put("dueAt", if (dueAt > 0) dueAt else System.currentTimeMillis() + 3600 * 1000)
                put("originDevice", DEVICE_ID)
                put("assignedDevice", "all")
            }
            postJson("$activeBackendUrl/api/tasks", payload)
        } catch (e: Exception) {
            Log.w(TAG, "Task sync deferred: ${e.message}")
        }
    }

    suspend fun dispatchRemoteCommand(
        targetDeviceId: String,
        action: String,
        params: Map<String, String> = emptyMap()
    ): RemoteCommandResult = withContext(Dispatchers.IO) {
        try {
            val payload = JSONObject().apply {
                put("originDeviceId", DEVICE_ID)
                put("targetDeviceId", targetDeviceId)
                put("action", action)
                val pObj = JSONObject()
                params.forEach { (k, v) -> pObj.put(k, v) }
                put("params", pObj)
            }

            val resp = postJson("$activeBackendUrl/api/commands/dispatch", payload)
            if (resp != null) {
                val success = resp.optBoolean("success", false)
                val message = resp.optString("message", "Dispatched to target device, Sir.")
                val cmdId = resp.optString("commandId")
                return@withContext RemoteCommandResult(success, message, cmdId)
            }
        } catch (e: Exception) {
            return@withContext RemoteCommandResult(false, "Remote command dispatch error: ${e.message}")
        }
        return@withContext RemoteCommandResult(false, "Central backend unreachable, Sir.")
    }

    /**
     * Cross-Device Natural Voice Command Interceptor.
     * Handles commands like:
     * - "MAX, tell my laptop to open Chrome"
     * - "MAX, open Android Studio on my laptop"
     * - "MAX, is my laptop online?"
     * - "MAX, send this task to my phone and laptop"
     */
    fun handleEcosystemVoiceCommand(command: String, language: VoiceLanguage): String? {
        val lower = command.trim().lowercase()

        // 1. Remote App Launch on Laptop: "tell my laptop to open [app]" / "open [app] on my laptop"
        val laptopTriggers = listOf(
            "tell my laptop to open", "open on my laptop", "on my laptop open",
            "laptop pe kholo", "laptop par open karo", "laptop me kholo"
        )
        for (trigger in laptopTriggers) {
            if (lower.contains(trigger)) {
                val app = if (lower.contains("open on my laptop")) {
                    lower.substringBefore("open on my laptop").removePrefix("open").trim()
                } else {
                    lower.substringAfter(trigger).trim()
                }
                val cleanApp = if (app.isNotBlank()) app else "Android Studio"

                scope.launch {
                    dispatchRemoteCommand(
                        targetDeviceId = "dev_laptop_win",
                        action = "open_app",
                        params = mapOf("appName" to cleanApp)
                    )
                }

                return when (language) {
                    VoiceLanguage.HINDI, VoiceLanguage.HINGLISH -> "जी सर, आपके विंडोज़ लैपटॉप पर $cleanApp खोलने का निर्देश भेज दिया गया है।"
                    VoiceLanguage.BENGALI -> "হ্যাঁ স্যার, আপনার ল্যাপটপে $cleanApp খোলার নির্দেশ পাঠানো হয়েছে।"
                    else -> "Remote authorization dispatched: Opening $cleanApp on your Windows workstation, Sir."
                }
            }
        }

        // 2. Query Laptop Status: "is my laptop online?" / "check laptop status"
        if (lower.contains("is my laptop online") || lower.contains("laptop online hai") || lower.contains("laptop status")) {
            val dev = connectedDevices.value.find { it.platform == "windows" }
            val isOnline = dev != null && dev.status == "online"
            return when (language) {
                VoiceLanguage.HINDI, VoiceLanguage.HINGLISH -> if (isOnline) "जी सर, आपका विंडोज़ लैपटॉप ऑनलाइन है और सेंट्रल कोर से जुड़ा हुआ है।" else "सर, आपका लैपटॉप अभी ऑफलाइन दिखाई दे रहा है।"
                VoiceLanguage.BENGALI -> if (isOnline) "হ্যাঁ স্যার, আপনার ল্যাপটপ অনলাইন আছে।" else "স্যার, ল্যাপটপ অফলাইন দেখাচ্ছে।"
                else -> if (isOnline) "Your Windows workstation is online and actively synchronized with the central neural core, Sir." else "Your workstation appears to be offline currently, Sir."
            }
        }

        // 3. Query Smartwatch Status: "is my watch connected?" / "watch status"
        if (lower.contains("watch connected") || lower.contains("watch status") || lower.contains("smartwatch online")) {
            val dev = connectedDevices.value.find { it.platform == "wearos" }
            val isOnline = dev != null && dev.status == "online"
            return if (isOnline) {
                "Your MAX Galaxy Watch node is connected and streaming telemetry, Sir."
            } else {
                "Your smartwatch node is standing by, Sir."
            }
        }

        // 4. Cross-Device Task Sync: "remember that I need to finish my website tonight" / "add task"
        if (lower.startsWith("remember that i need to ") || lower.startsWith("add task ") || lower.contains("remind all devices")) {
            val taskText = lower.removePrefix("remember that i need to ").removePrefix("add task ").trim()
            if (taskText.isNotBlank()) {
                scope.launch {
                    syncTask(taskText)
                }
                return when (language) {
                    VoiceLanguage.HINDI, VoiceLanguage.HINGLISH -> "जी सर, '$taskText' को आपके फोन, लैपटॉप और स्मार्टवॉच पर सिंक्रोनाइज़ कर दिया गया है।"
                    VoiceLanguage.BENGALI -> "হ্যাঁ স্যার, টাস্কটি আপনার সমস্ত ডিভাইসে সিঙ্ক করা হয়েছে।"
                    else -> "Synchronized task '$taskText' across your smartphone, laptop, and smartwatch, Sir."
                }
            }
        }

        return null
    }

    private fun postJson(urlStr: String, json: JSONObject): JSONObject? {
        val url = URL(urlStr)
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 3000
            readTimeout = 3000
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
        }
        OutputStreamWriter(conn.outputStream).use { it.write(json.toString()) }
        if (conn.responseCode in 200..299) {
            val resText = conn.inputStream.bufferedReader().use { it.readText() }
            return JSONObject(resText)
        }
        return null
    }
}
