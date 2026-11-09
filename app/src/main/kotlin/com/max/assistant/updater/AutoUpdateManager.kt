package com.max.assistant.updater

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

sealed class UpdateStatus {
    object Idle : UpdateStatus()
    object Checking : UpdateStatus()
    data class Downloading(val progressPercent: Int) : UpdateStatus()
    data class ReadyToInstall(val apkFile: File) : UpdateStatus()
    data class Error(val message: String) : UpdateStatus()
}

class AutoUpdateManager(private val context: Context) {

    companion object {
        private const val TAG = "AutoUpdateManager"
        // Local Wi-Fi server serving latest build
        const val UPDATE_SERVER_URL = "http://192.168.1.4:8080/MAX.apk"
    }

    /**
     * Checks Wi-Fi server and downloads the latest MAX APK update.
     */
    suspend fun downloadAndInstallUpdate(
        updateUrl: String = UPDATE_SERVER_URL,
        onProgress: ((Int) -> Unit)? = null
    ): UpdateStatus = withContext(Dispatchers.IO) {
        try {
            Log.i(TAG, "Checking update from $updateUrl")
            val url = URL(updateUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 8000
            connection.readTimeout = 15000
            connection.requestMethod = "GET"
            connection.connect()

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                return@withContext UpdateStatus.Error("अपडेट सर्वर से संपर्क नहीं हो पाया (Code: ${connection.responseCode})")
            }

            val fileLength = connection.contentLength
            val cacheDir = context.externalCacheDir ?: context.cacheDir
            val updateFile = File(cacheDir, "MAX_Update.apk")

            connection.inputStream.use { input ->
                FileOutputStream(updateFile).use { output ->
                    val data = ByteArray(8192)
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

            Log.i(TAG, "Update downloaded successfully: ${updateFile.absolutePath} (${updateFile.length()} bytes)")
            triggerInstall(updateFile)
            UpdateStatus.ReadyToInstall(updateFile)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating MAX: ${e.message}")
            UpdateStatus.Error("अपडेट डाउनलोड करने में त्रुटि, सर: ${e.message}")
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
}
