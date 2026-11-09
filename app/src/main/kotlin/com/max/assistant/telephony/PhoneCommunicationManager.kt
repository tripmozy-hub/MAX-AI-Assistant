package com.max.assistant.telephony

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import android.telephony.SmsManager
import android.util.Log
import androidx.core.content.ContextCompat
import java.net.URLEncoder

sealed class PhoneActionResult {
    abstract val message: String
    data class Success(override val message: String) : PhoneActionResult()
    data class Error(override val message: String) : PhoneActionResult()
}

data class ContactInfo(val displayName: String, val phoneNumber: String)

class PhoneCommunicationManager(private val context: Context) {

    companion object {
        private const val TAG = "PhoneCommunication"
    }

    /**
     * Resolves contact name or phone number and executes voice call.
     */
    fun makeCall(target: String): PhoneActionResult {
        val trimmed = target.trim()
        val phoneNumber = if (trimmed.matches(Regex("^[0-9+ ]+$"))) {
            trimmed.replace(" ", "")
        } else {
            lookupContactNumber(trimmed)
        }

        if (phoneNumber.isNullOrEmpty() || !phoneNumber.any { it.isDigit() }) {
            return PhoneActionResult.Error("माफ़ कीजिए सर, संपर्क '$target' का नंबर नहीं मिला।")
        }

        val hasCallPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CALL_PHONE
        ) == PackageManager.PERMISSION_GRANTED

        return try {
            val intent = if (hasCallPermission) {
                Intent(Intent.ACTION_CALL, Uri.parse("tel:$phoneNumber"))
            } else {
                Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phoneNumber"))
            }.apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            safeStartActivity(intent)
            Log.i(TAG, "Initiated call to $target ($phoneNumber)")
            PhoneActionResult.Success("जी सर, $target को कॉल किया जा रहा है।")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initiate call: ${e.message}")
            PhoneActionResult.Error("कॉल नहीं लग पाया, सर: ${e.message}")
        }
    }

    /**
     * Sends SMS message or opens messaging app with pre-filled content.
     */
    fun sendMessage(recipient: String, text: String): PhoneActionResult {
        val trimmed = recipient.trim()
        val phoneNumber = if (trimmed.matches(Regex("^[0-9+ ]+$"))) {
            trimmed.replace(" ", "")
        } else {
            lookupContactNumber(trimmed) ?: trimmed
        }

        val hasSmsPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.SEND_SMS
        ) == PackageManager.PERMISSION_GRANTED

        return try {
            val smsMgr = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                context.getSystemService(SmsManager::class.java)
            } else {
                @Suppress("DEPRECATION")
                SmsManager.getDefault()
            }
            if (hasSmsPermission && phoneNumber.matches(Regex("^[0-9+]+$"))) {
                smsMgr.sendTextMessage(phoneNumber, null, text, null, null)
                Log.i(TAG, "Sent direct SMS to $phoneNumber")
                PhoneActionResult.Success("जी सर, $recipient को संदेश भेज दिया गया है।")
            } else {
                val sendIntent = Intent(Intent.ACTION_SENDTO).apply {
                    data = Uri.parse("smsto:${if (phoneNumber.matches(Regex("^[0-9+]+$"))) phoneNumber else ""}")
                    putExtra("sms_body", text)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                safeStartActivity(sendIntent)
                PhoneActionResult.Success("जी सर, $recipient के लिए संदेश तैयार है।")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send SMS: ${e.message}")
            PhoneActionResult.Error("संदेश नहीं भेजा जा सका, सर: ${e.message}")
        }
    }

    /**
     * Formats phone numbers for WhatsApp deep linking (standardizing Indian 10-digit to 91XXXXXXXXXX).
     */
    fun formatWhatsAppNumber(rawNumber: String): String {
        val digits = rawNumber.filter { it.isDigit() }
        return when {
            digits.length == 10 -> "91$digits"
            digits.length == 11 && digits.startsWith("0") -> "91${digits.substring(1)}"
            digits.length == 12 && digits.startsWith("91") -> digits
            digits.length > 6 -> digits
            else -> ""
        }
    }

    private fun getInstalledWhatsAppPackage(): String? {
        val pm = context.packageManager
        val candidates = listOf("com.whatsapp", "com.whatsapp.w4b")
        for (pkg in candidates) {
            try {
                pm.getPackageInfo(pkg, 0)
                return pkg
            } catch (_: Exception) {}
        }
        return null
    }

    /**
     * Sends WhatsApp message directly to contact.
     */
    fun sendWhatsApp(recipient: String, text: String): PhoneActionResult {
        val trimmed = recipient.trim()
        val phoneNumber = if (trimmed.matches(Regex("^[0-9+ ]+$"))) {
            formatWhatsAppNumber(trimmed)
        } else {
            val num = lookupContactNumber(trimmed)
            if (num != null) formatWhatsAppNumber(num) else ""
        }

        // Signal accessibility service to auto-click send button hands-free
        com.max.assistant.service.MaxAccessibilityGuardService.requestWhatsAppAutoSend()

        val targetPkg = getInstalledWhatsAppPackage() ?: "com.whatsapp"

        return try {
            val encodedText = URLEncoder.encode(text, "UTF-8")
            val uriStr = if (phoneNumber.isNotEmpty()) {
                "https://api.whatsapp.com/send?phone=$phoneNumber&text=$encodedText"
            } else {
                "https://api.whatsapp.com/send?text=$encodedText"
            }
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uriStr)).apply {
                setPackage(targetPkg)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            safeStartActivity(intent)
            PhoneActionResult.Success("जी सर, $recipient को व्हाट्सएप उत्तर भेजा जा रहा है।")
        } catch (e: Exception) {
            // Fallback: regular intent without strict package lock
            try {
                val fallbackIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, text)
                    if (targetPkg.isNotEmpty()) setPackage(targetPkg)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                safeStartActivity(fallbackIntent)
                PhoneActionResult.Success("जी सर, व्हाट्सएप संदेश तैयार है।")
            } catch (ex: Exception) {
                PhoneActionResult.Error("व्हाट्सएप खोलने में असमर्थ, सर।")
            }
        }
    }

    private fun safeStartActivity(intent: Intent): Boolean {
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

    private fun transliterateToLatin(text: String): String {
        val sb = StringBuilder()
        for (ch in text) {
            val latin = when (ch) {
                'अ' -> "a"; 'आ' -> "a"; 'इ' -> "i"; 'ई' -> "ee"; 'उ' -> "u"; 'ऊ' -> "oo"; 'ए' -> "e"; 'ऐ' -> "ai"; 'ओ' -> "o"; 'औ' -> "au"
                'क' -> "k"; 'ख' -> "kh"; 'ग' -> "g"; 'घ' -> "gh"; 'च' -> "ch"; 'छ' -> "chh"; 'ज' -> "j"; 'झ' -> "jh"
                'ट' -> "t"; 'ठ' -> "th"; 'ड' -> "d"; 'ढ' -> "dh"; 'ण' -> "n"; 'त' -> "t"; 'थ' -> "th"; 'द' -> "d"; 'ध' -> "dh"; 'न' -> "n"
                'प' -> "p"; 'फ' -> "ph"; 'ब' -> "b"; 'भ' -> "bh"; 'म' -> "m"; 'य' -> "y"; 'र' -> "r"; 'ल' -> "l"; 'व' -> "v"
                'श' -> "sh"; 'ष' -> "sh"; 'स' -> "s"; 'ह' -> "h"
                'ा' -> "a"; 'ि' -> "i"; 'ी' -> "ee"; 'ु' -> "u"; 'ू' -> "oo"; 'े' -> "e"; 'ै' -> "ai"; 'ो' -> "o"; 'ौ' -> "au"
                'ं' -> "n"; 'ँ' -> "n"; '्' -> ""
                // Bengali
                'অ' -> "a"; 'আ' -> "a"; 'ই' -> "i"; 'ঈ' -> "ee"; 'উ' -> "u"; 'ঊ' -> "oo"; 'এ' -> "e"; 'ঐ' -> "oi"; 'ও' -> "o"; 'ঔ' -> "ou"
                'ক' -> "k"; 'খ' -> "kh"; 'গ' -> "g"; 'ঘ' -> "gh"; 'চ' -> "ch"; 'ছ' -> "chh"; 'জ' -> "j"; 'ঝ' -> "jh"
                'ট' -> "t"; 'ঠ' -> "th"; 'ড' -> "d"; 'ঢ' -> "dh"; 'ণ' -> "n"; 'ত' -> "t"; 'থ' -> "th"; 'দ' -> "d"; 'ধ' -> "dh"; 'ন' -> "n"
                'প' -> "p"; 'ফ' -> "ph"; 'ব' -> "b"; 'ভ' -> "bh"; 'ম' -> "m"; 'য' -> "y"; 'র' -> "r"; 'ল' -> "l"; 'শ' -> "sh"; 'ষ' -> "sh"; 'স' -> "s"; 'হ' -> "h"
                '\u09BE' -> "a"; '\u09BF' -> "i"; '\u09C0' -> "ee"; '\u09C1' -> "u"; '\u09C2' -> "oo"; '\u09C7' -> "e"; '\u09C8' -> "oi"; '\u09CB' -> "o"; '\u09CC' -> "ou"
                'ং' -> "n"; 'ঁ' -> "n"; '্' -> ""
                else -> ch.toString()
            }
            sb.append(latin)
        }
        return sb.toString().lowercase(java.util.Locale.ROOT)
    }

    fun findContact(contactName: String): ContactInfo? {
        val hasContactPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasContactPermission) return null

        val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME
        )

        // 1. Direct search
        try {
            val selection = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?"
            val selectionArgs = arrayOf("%$contactName%")
            context.contentResolver.query(uri, projection, selection, selectionArgs, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                    val nameIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                    if (numberIndex >= 0) {
                        val num = cursor.getString(numberIndex)
                        val name = if (nameIndex >= 0) cursor.getString(nameIndex) ?: contactName else contactName
                        if (!num.isNullOrBlank()) return ContactInfo(name, num)
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Direct lookup error: ${e.message}")
        }

        // 2. Transliterated / Normalized lookup (handles Devanagari/Bengali voice input vs Latin contacts with emojis)
        val latinTarget = transliterateToLatin(contactName)
            .replace(Regex("[^a-zA-Z0-9\\s]"), "")
            .trim()
            .lowercase(java.util.Locale.ROOT)
        val targetTokens = latinTarget.split("\\s+".toRegex()).filter { it.length >= 2 }

        try {
            var bestMatchName: String? = null
            var bestMatchNum: String? = null
            var bestMatchScore = 0

            context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                if (nameIndex >= 0 && numberIndex >= 0) {
                    while (cursor.moveToNext()) {
                        val rawName = cursor.getString(nameIndex) ?: continue
                        val cleanName = rawName.replace(Regex("[^a-zA-Z0-9\\s]"), "").trim().lowercase(java.util.Locale.ROOT)
                        val num = cursor.getString(numberIndex)
                        if (cleanName.isEmpty() || num.isNullOrBlank()) continue

                        val nameTokens = cleanName.split("\\s+".toRegex()).filter { it.length >= 2 }

                        // Priority 1: Exact clean name match (ignoring emojis/punctuation)
                        if (cleanName == latinTarget) {
                            Log.i(TAG, "Exact match for '$contactName' -> '$rawName' ($num)")
                            return ContactInfo(rawName, num)
                        }

                        // Priority 2: cleanName contains entire latinTarget as phrase
                        if (latinTarget.length >= 3 && cleanName.contains(latinTarget)) {
                            if (bestMatchScore < 90) {
                                bestMatchScore = 90
                                bestMatchName = rawName
                                bestMatchNum = num
                                Log.i(TAG, "Phrase match candidate: '$rawName' ($num)")
                            }
                        }

                        // Priority 3: All target tokens match words in cleanName with phonetic/skeleton tolerance
                        if (targetTokens.size >= 2 && targetTokens.all { tok -> nameTokens.any { isFuzzyNameMatch(tok, it) } }) {
                            if (bestMatchScore < 85) {
                                bestMatchScore = 85
                                bestMatchName = rawName
                                bestMatchNum = num
                                Log.i(TAG, "All-tokens fuzzy match candidate: '$rawName' ($num)")
                            }
                        }

                        // Priority 4: First token matches first name
                        if (targetTokens.isNotEmpty() && nameTokens.isNotEmpty() && isFuzzyNameMatch(targetTokens[0], nameTokens[0])) {
                            if (bestMatchScore < 70) {
                                bestMatchScore = 70
                                bestMatchName = rawName
                                bestMatchNum = num
                                Log.i(TAG, "First-name match candidate: '$rawName' ($num)")
                            }
                        }
                    }
                }
            }
            if (bestMatchNum != null) {
                Log.i(TAG, "Selected best matched contact for '$contactName' -> $bestMatchNum")
                return ContactInfo(bestMatchName ?: contactName, bestMatchNum)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Normalized lookup error: ${e.message}")
        }

        return null
    }

    private fun lookupContactNumber(contactName: String): String? {
        return findContact(contactName)?.phoneNumber
    }

    private fun isFuzzyNameMatch(tokenA: String, tokenB: String): Boolean {
        if (tokenA == tokenB) return true
        if (tokenA.contains(tokenB) || tokenB.contains(tokenA)) return true

        // Compare consonant skeletons (removes vowels so 'anjna' matches 'anjana', 'adity' matches 'aditya')
        val skelA = tokenA.replace(Regex("[aeiouy]"), "")
        val skelB = tokenB.replace(Regex("[aeiouy]"), "")
        if (skelA.isNotEmpty() && skelA == skelB) return true

        // Levenshtein distance <= 1 for words length >= 4
        if (tokenA.length >= 4 && tokenB.length >= 4) {
            val dist = levenshteinDistance(tokenA, tokenB)
            if (dist <= 1) return true
        }
        return false
    }

    private fun levenshteinDistance(s1: String, s2: String): Int {
        val dp = Array(s1.length + 1) { IntArray(s2.length + 1) }
        for (i in 0..s1.length) dp[i][0] = i
        for (j in 0..s2.length) dp[0][j] = j
        for (i in 1..s1.length) {
            for (j in 1..s2.length) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = minOf(dp[i - 1][j] + 1, dp[i][j - 1] + 1, dp[i - 1][j - 1] + cost)
            }
        }
        return dp[s1.length][s2.length]
    }
}
