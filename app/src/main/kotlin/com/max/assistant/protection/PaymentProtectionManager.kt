package com.max.assistant.protection

import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import com.max.assistant.tts.VoiceLanguage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * PaymentProtectionManager enforces an unconditional, system-wide ban on MAX initiating,
 * executing, clicking, confirming, or automating any financial payments, UPI transfers,
 * wallet transactions, banking actions, or in-app purchases.
 */
object PaymentProtectionManager {
    private const val TAG = "PaymentProtection"

    // Payment execution is permanently disabled for financial safety.
    private val _isPaymentDisabled = MutableStateFlow(true)
    val isPaymentDisabled: StateFlow<Boolean> = _isPaymentDisabled.asStateFlow()

    private val _lastBlockedEvent = MutableStateFlow<String?>(null)
    val lastBlockedEvent: StateFlow<String?> = _lastBlockedEvent.asStateFlow()

    // Known Indian UPI, Wallet, Banking & Financial Investment Application Packages
    private val PAYMENT_PACKAGES = setOf(
        // UPI & Digital Wallets
        "com.google.android.apps.nbu.paisa.user", // Google Pay (Tez)
        "com.phonepe.app",                        // PhonePe
        "net.one97.paytm",                        // Paytm
        "in.org.npci.upiapp",                     // BHIM
        "com.dreamplug.androidapp",               // CRED
        "com.samsung.android.spay",               // Samsung Wallet / Pay
        "com.samsung.android.spaymini",           // Samsung Pay Mini
        "com.samsung.android.rapmobile",          // Samsung Pay FW
        "com.mobikwik_new",                       // MobiKwik
        "com.freecharge.android",                 // Freecharge
        "com.bharatpe.app",                       // BharatPe
        "com.naviapp",                            // Navi
        "com.tatadigital.tcp",                    // Tata Neu / Tata Pay
        "in.amazon.mShop.android.shopping",       // Amazon (when executing payments)

        // Major Banking Applications
        "com.sbi.lotusintouch",                   // YONO SBI
        "com.money.yono",                         // YONO Lite SBI
        "com.snapwork.hdfc",                      // HDFC MobileBanking
        "com.csam.icici.bank.imobile",            // ICICI iMobile Pay
        "com.axis.mobile",                        // Axis Mobile
        "com.msf.kbank.mobile",                   // Kotak 811
        "com.pnb.one",                            // PNB ONE
        "com.bankofbaroda.mconnect",              // bob World (Bank of Baroda)
        "com.canarabank.mobility",                // Canara ai1
        "com.infrasofttech.uboi",                 // Union Bank Vyom
        "com.indusind",                           // IndusMobile
        "com.yesbank",                            // YES Mobile
        "com.idfcfirstbank.optimus",              // IDFC FIRST Bank
        "com.fedmobile",                          // FedMobile (Federal Bank)
        "money.jupiter",                          // Jupiter Money
        "money.fi",                               // Fi Money
        "org.rajyugsolutions.slice",              // Slice

        // Trading / Brokerage
        "com.zerodha.kite3",                      // Zerodha Kite
        "com.nextbillion.groww",                  // Groww
        "com.msf.angelmobile",                    // Angel One
        "in.upstox.app"                           // Upstox
    )

    // Payment action triggers in natural language voice commands (English & Hinglish)
    private val PAYMENT_QUERY_PATTERNS = listOf(
        // English
        Regex("(?i)\\b(pay|payment|make\\s+payment|do\\s+payment|send\\s+money|transfer\\s+money|transfer\\s+funds|transfer\\s+cash|send\\s+cash|send\\s+(?:rs|inr|rupees?|₹)\\s*\\d+|pay\\s+(?:rs|inr|rupees?|₹)?\\s*\\d+|pay\\s+bill|recharge\\s+mobile|recharge\\s+phone|mobile\\s+recharge|buy\\s+this|purchase\\s+this|proceed\\s+to\\s+pay|complete\\s+payment|confirm\\s+payment)\\b"),
        // Specific UPI app payment intents
        Regex("(?i)\\b(?:gpay|google\\s*pay|phonepe|phone\\s*pe|paytm|bhim|cred|upi)\\b.*\\b(?:pay|send|transfer|bhejo|karo|payment)\\b"),
        Regex("(?i)\\b(?:pay|send|transfer|bhejo|karo|payment)\\b.*\\b(?:gpay|google\\s*pay|phonepe|phone\\s*pe|paytm|bhim|cred|upi)\\b"),
        // Latin Hinglish & Bengali transliteration
        Regex("(?i)\\b(paise\\s+bhejo|paise\\s+bhej|paise\\s+transfer|paise\\s+pay|payment\\s+karo|payment\\s+kardo|payment\\s+kar\\s+do|rupaye\\s+bhejo|rupaye\\s+transfer|bhejo\\s+paise|transfer\\s+karo|bill\\s+bharo|recharge\\s+karo|recharge\\s+kardo)\\b"),
        Regex("(?i)\\b(taka\\s+pathao|taka\\s+transfer|payment\\s+koro|bill\\s+dao)\\b")
    )

    // Hindi (Devanagari) and Bengali Unicode payment keywords (using substring matching to avoid regex word-boundary limitations on Unicode scripts)
    private val UNICODE_PAYMENT_KEYWORDS = listOf(
        "पैसे भेजो", "पैसे भेज", "रुपये भेजो", "भुगतान करो", "भुगतान करें", "भुगतान",
        "पेमेंट करो", "पेमेंट कर दो", "पेमेंट करें", "बिल भरो", "रिचार्ज करो",
        "টাকা পাঠাও", "টাকা পাঠিয়ে দাও", "টাকা পাঠান", "টাকা ট্রান্সফার", "পেমেন্ট করো",
        "পেমেন্ট করুন", "রিচার্জ করো", "বিল দাও"
    )

    // Negative triggers that indicate informational inquiries rather than an execution request
    private val INFORMATIONAL_QUERY_PATTERNS = listOf(
        Regex("(?i)\\b(what\\s+is|who\\s+is|how\\s+to|how\\s+does|explain|meaning|kya\\s+hai|ki\\s+hoy|কি|क्या\\s+है)\\b"),
        Regex("(?i)\\b(history|founder|ceo|definition|tutorial|guide)\\b")
    )

    // Interactive button texts and descriptions that must NEVER be auto-clicked
    private val BLOCKED_INTERACTIVE_ACTIONS = listOf(
        // English
        "pay",
        "pay now",
        "proceed to pay",
        "pay with upi",
        "send money",
        "transfer money",
        "confirm payment",
        "make payment",
        "approve payment",
        "enter upi pin",
        "enter pin",
        "enter mpin",
        "submit payment",
        "buy now",
        "place order",
        "checkout",
        "confirm order",
        "complete purchase",
        "recharge now",
        "tap to pay",

        // Hindi
        "भुगतान करें",
        "भुगतान करो",
        "भुगतान",
        "पेमेंट करें",
        "पैसे भेजें",
        "पैसे भेजो",
        "ऑर्डर दें",
        "खरीदें",
        "यूपीआई पिन डालें",
        "पिन डालें",

        // Bengali
        "টাকা পাঠান",
        "পেমেন্ট করুন",
        "পেমেন্ট করো",
        "নিশ্চিত করুন",
        "অর্ডার দিন",
        "কিনুন",
        "পিন দিন"
    )

    /**
     * Checks if a voice query or natural language prompt is requesting MAX to execute a payment.
     */
    fun isPaymentRequest(query: String): Boolean {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return false

        // Check if it's purely informational (e.g. "What is Google Pay?")
        val isInformational = INFORMATIONAL_QUERY_PATTERNS.any { it.containsMatchIn(trimmed) }
        if (isInformational && !trimmed.contains("karo", ignoreCase = true) && !trimmed.contains("bhejo", ignoreCase = true)) {
            return false
        }

        // Check Unicode Hindi & Bengali keywords
        if (UNICODE_PAYMENT_KEYWORDS.any { trimmed.contains(it) }) {
            _lastBlockedEvent.value = "Blocked Unicode payment request: $trimmed"
            Log.w(TAG, "Payment request BLOCKED (Unicode): '$trimmed'")
            return true
        }

        for (pattern in PAYMENT_QUERY_PATTERNS) {
            if (pattern.containsMatchIn(trimmed)) {
                _lastBlockedEvent.value = "Blocked voice payment request: $trimmed"
                Log.w(TAG, "Payment request BLOCKED: '$trimmed'")
                return true
            }
        }
        return false
    }

    /**
     * Checks if a UI element's text or contentDescription matches a prohibited payment action.
     */
    fun isPaymentAction(targetTextOrDesc: String): Boolean {
        val lower = targetTextOrDesc.trim().lowercase()
        if (lower.isBlank()) return false

        // Exact or strong match against blocked interactive actions
        for (blocked in BLOCKED_INTERACTIVE_ACTIONS) {
            val blockedLower = blocked.lowercase()
            if (lower == blockedLower || lower.startsWith("$blockedLower ") || lower.endsWith(" $blockedLower")) {
                _lastBlockedEvent.value = "Blocked UI payment action: $targetTextOrDesc"
                Log.w(TAG, "Prohibited payment UI interaction detected: '$targetTextOrDesc'")
                return true
            }
        }

        // Currency patterns: e.g. "Pay ₹500", "Pay Rs 200", "Transfer $50"
        if (lower.startsWith("pay ") && (lower.contains("₹") || lower.contains("rs") || lower.contains("$") || lower.any { it.isDigit() })) {
            _lastBlockedEvent.value = "Blocked payment button: $targetTextOrDesc"
            return true
        }

        // UPI PIN prompt detection
        if (lower.contains("upi pin") || lower.contains("mpin") || lower.contains("atm pin")) {
            _lastBlockedEvent.value = "Blocked PIN interaction: $targetTextOrDesc"
            return true
        }

        return false
    }

    /**
     * Checks if a package name belongs to a registered financial, banking, or UPI application.
     */
    fun isPaymentPackage(packageName: String?): Boolean {
        if (packageName == null) return false
        val lower = packageName.lowercase()
        if (PAYMENT_PACKAGES.contains(lower)) return true

        // Generic banking package heuristic
        return (lower.contains(".bank.") || lower.contains(".banking.") || lower.contains(".upi.") || lower.contains("wallet")) &&
                !lower.contains("google.android.apps.walletnfcrel") // Google Wallet non-payment card pass view
    }

    /**
     * Inspects an accessibility node to verify if clicking or interacting with it would trigger a payment.
     */
    fun isPaymentNode(node: AccessibilityNodeInfo?): Boolean {
        if (node == null) return false

        val text = node.text?.toString() ?: ""
        val desc = node.contentDescription?.toString() ?: ""
        val viewId = node.viewIdResourceName ?: ""

        if (isPaymentAction(text) || isPaymentAction(desc)) return true

        val lowerViewId = viewId.lowercase()
        if (lowerViewId.contains("btn_pay") || lowerViewId.contains("btnpay") ||
            lowerViewId.contains("proceed_to_pay") || lowerViewId.contains("send_money") ||
            lowerViewId.contains("upi_pin") || lowerViewId.contains("confirm_payment")
        ) {
            return true
        }

        return false
    }

    /**
     * Returns a respectful, firm spoken refusal explaining that payment execution is disabled.
     */
    fun getBlockedPaymentMessage(language: VoiceLanguage): String {
        return when (language) {
            VoiceLanguage.HINDI -> "सर, आपकी वित्तीय सुरक्षा के लिए MAX द्वारा भुगतान या पैसे भेजना पूरी तरह से अक्षम (disabled) है। कृपया यह लेन-देन अपने फ़ोन से स्वयं पूरा करें।"
            VoiceLanguage.BENGALI -> "স্যার, আপনার আর্থিক নিরাপত্তার জন্য MAX-এর মাধ্যমে পেমেন্ট বা টাকা পাঠানোর সুবিধা সম্পূর্ণ নিষ্ক্রিয় করা হয়েছে। দয়া করে লেনদেনটি নিজে সম্পন্ন করুন।"
            VoiceLanguage.HINGLISH -> "Sir, safety policy ke according MAX ko payments ya money transfers perform karne ki permission nahi hai. Kripya payment manually phone se complete karein."
            else -> "Sir, for your financial safety and security, payment execution and fund transfers are strictly disabled in MAX. Please complete the transaction manually."
        }
    }
}
