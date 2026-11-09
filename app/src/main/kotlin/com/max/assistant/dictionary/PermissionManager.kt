package com.max.assistant.dictionary

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

/**
 * PermissionManager checks and reports runtime permission requirements before command execution.
 */
class PermissionManager(private val context: Context) {

    fun hasPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }

    fun hasRecordAudio(): Boolean = hasPermission(Manifest.permission.RECORD_AUDIO)

    fun hasCallPhone(): Boolean = hasPermission(Manifest.permission.CALL_PHONE)

    fun hasReadContacts(): Boolean = hasPermission(Manifest.permission.READ_CONTACTS)

    fun hasSendSms(): Boolean = hasPermission(Manifest.permission.SEND_SMS)

    fun hasPostNotifications(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            hasPermission(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            true
        }
    }

    fun checkPermissionsForIntent(def: CommandDefinition): Pair<Boolean, String?> {
        for (perm in def.requiredPermissions) {
            if (!hasPermission(perm)) {
                return Pair(false, "Permission required: $perm")
            }
        }
        return Pair(true, null)
    }
}
