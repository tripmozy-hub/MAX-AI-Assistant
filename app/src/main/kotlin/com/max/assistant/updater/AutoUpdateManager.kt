package com.max.assistant.updater

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

sealed class UpdateStatus {
    object Idle : UpdateStatus()
    object Checking : UpdateStatus()
    object UpToDate : UpdateStatus()
    data class UpdateAvailable(val latestVersion: String, val downloadUrl: String) : UpdateStatus()
    data class Downloading(val progressPercent: Int) : UpdateStatus()
    data class ReadyToInstall(val apkFile: File) : UpdateStatus()
    data class Error(val message: String) : UpdateStatus()
}

class AutoUpdateManager(private val context: Context) {

    companion object {
        private const val TAG = "AutoUpdateManager"
        const val GITHUB_RELEASE_API = "https://api.github.com/repos/tripmozy-hub/MAX-AI-Assistant/releases/latest"
        const val PRIMARY_DOWNLOAD_URL = "https://github.com/tripmozy-hub/MAX-AI-Assistant/releases/latest/download/MAX.apk"
        const val FALLBACK_DOWNLOAD_URL = "https://files.catbox.moe/jkoe2j.apk"
    }

    /**
     * Checks if a new release is available on GitHub.
     */
    suspend fun checkVersion(): UpdateStatus = withContext(Dispatchers.IO) {
        try {
            val url = URL(GITHUB_RELEASE_API)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 8000
                readTimeout = 8000
                setRequestProperty("User-Agent", "MAX-Assistant-Android")
                setRequestProperty("Accept", "application/vnd.github.v3+json")
            }

            if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                val jsonStr = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(jsonStr)
                val tagName = json.optString("tag_name", "").trim()
                val remoteVer = tagName.removePrefix("v").trim()
                
                val currentVersion = try {
                    val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                    pInfo.versionName ?: "2.0.0"
                } catch (e: Exception) {
                    "2.0.0"
                }

                var downloadUrl = PRIMARY_DOWNLOAD_URL
                val assets = json.optJSONArray("assets")
                if (assets != null && assets.length() > 0) {
                    for (i in 0 until assets.length()) {
                        val asset = assets.getJSONObject(i)
                        if (asset.optString("name").endsWith(".apk", ignoreCase = true)) {
                            downloadUrl = asset.optString("browser_download_url", PRIMARY_DOWNLOAD_URL)
                            break
                        }
                    }
                }

                if (isNewerVersion(remoteVer, currentVersion)) {
                    Log.i(TAG, "New update available: $remoteVer (Current: $currentVersion)")
                    return@withContext UpdateStatus.UpdateAvailable(remoteVer, downloadUrl)
                } else {
                    Log.i(TAG, "MAX is up-to-date. Version: $currentVersion")
                    return@withContext UpdateStatus.UpToDate
                }
            }
            UpdateStatus.UpToDate
        } catch (e: Exception) {
            Log.w(TAG, "Check version failed, checking fallback: ${e.message}")
            UpdateStatus.UpToDate
        }
    }

    /**
     * Downloads and installs update from GitHub or mirror.
     */
    suspend fun downloadAndInstallUpdate(
        customUrl: String? = null,
        onProgress: ((Int) -> Unit)? = null
    ): UpdateStatus = withContext(Dispatchers.IO) {
        val targetUrl = customUrl ?: PRIMARY_DOWNLOAD_URL
        var result = tryDownload(targetUrl, onProgress)
        if (result is UpdateStatus.Error && targetUrl == PRIMARY_DOWNLOAD_URL) {
            Log.w(TAG, "Primary download failed, trying CDN fallback...")
            result = tryDownload(FALLBACK_DOWNLOAD_URL, onProgress)
        }
        result
    }

    private fun tryDownload(targetUrl: String, onProgress: ((Int) -> Unit)?): UpdateStatus {
        try {
            Log.i(TAG, "Downloading update from $targetUrl")
            var url = URL(targetUrl)
            var connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 12000
            connection.readTimeout = 25000
            connection.instanceFollowRedirects = true
            connection.setRequestProperty("User-Agent", "MAX-Assistant-Android")
            connection.connect()

            // Handle HTTP 301 / 302 / 307 redirects
            val code = connection.responseCode
            if (code in listOf(HttpURLConnection.HTTP_MOVED_PERM, HttpURLConnection.HTTP_MOVED_TEMP, 307, 308)) {
                val newUrl = connection.getHeaderField("Location")
                if (!newUrl.isNullOrEmpty()) {
                    url = URL(newUrl)
                    connection = url.openConnection() as HttpURLConnection
                    connection.connectTimeout = 12000
                    connection.readTimeout = 25000
                    connection.setRequestProperty("User-Agent", "MAX-Assistant-Android")
                    connection.connect()
                }
            }

            if (connection.responseCode !in 200..299) {
                return UpdateStatus.Error("अपडेट सर्वर से संपर्क नहीं हो पाया (Code: ${connection.responseCode})")
            }

            val fileLength = connection.contentLength
            val cacheDir = context.externalCacheDir ?: context.cacheDir
            val updateFile = File(cacheDir, "MAX_Update.apk")
            if (updateFile.exists()) updateFile.delete()

            connection.inputStream.use { input ->
                FileOutputStream(updateFile).use { output ->
                    val data = ByteArray(16384)
                    var total: Long = 0
                    var count: Int
                    while (input.read(data).also { count = it } != -1) {
                        total += count
                        output.write(data, 0, count)
                        if (fileLength > 0) {
                            val percent = ((total * 100) / fileLength).toInt()
                            onProgress?.invoke(percent)
                        }
                    }
                }
            }

            if (updateFile.length() < 1_000_000L) {
                return UpdateStatus.Error("अमान्य अपडेट फ़ाइल आकार (${updateFile.length()} bytes)")
            }

            Log.i(TAG, "Update downloaded successfully: ${updateFile.absolutePath} (${updateFile.length()} bytes)")
            triggerInstall(updateFile)
            return UpdateStatus.ReadyToInstall(updateFile)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating MAX: ${e.message}")
            return UpdateStatus.Error("अपडेट डाउनलोड करने में त्रुटि, सर: ${e.message}")
        }
    }

    /**
     * Triggers Android package installer for the downloaded APK using FileProvider.
     */
    fun triggerInstall(apkFile: File) {
        try {
            val contentUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(contentUri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            context.startActivity(installIntent)
            Log.i(TAG, "Package installer initiated for MAX update.")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch package installer: ${e.message}")
        }
    }

    private fun isNewerVersion(remoteVer: String, currentVer: String): Boolean {
        try {
            val rParts = remoteVer.split(".").map { it.toIntOrNull() ?: 0 }
            val cParts = currentVer.split(".").map { it.toIntOrNull() ?: 0 }
            val maxLen = maxOf(rParts.size, cParts.size)
            for (i in 0 until maxLen) {
                val r = rParts.getOrElse(i) { 0 }
                val c = cParts.getOrElse(i) { 0 }
                if (r > c) return true
                if (r < c) return false
            }
        } catch (e: Exception) {
            Log.w(TAG, "Version compare error: ${e.message}")
        }
        return false
    }
}
