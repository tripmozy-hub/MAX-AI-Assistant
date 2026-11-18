package com.max.assistant.protection

import com.max.assistant.ai.BrainDecision
import com.max.assistant.ai.MaxAiBrain
import com.max.assistant.tts.VoiceLanguage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PaymentProtectionTest {

    @Test
    fun testPaymentPackagesDetected() {
        assertTrue(PaymentProtectionManager.isPaymentPackage("com.google.android.apps.nbu.paisa.user"))
        assertTrue(PaymentProtectionManager.isPaymentPackage("com.phonepe.app"))
        assertTrue(PaymentProtectionManager.isPaymentPackage("net.one97.paytm"))
        assertTrue(PaymentProtectionManager.isPaymentPackage("in.org.npci.upiapp"))
        assertTrue(PaymentProtectionManager.isPaymentPackage("com.dreamplug.androidapp"))
        assertTrue(PaymentProtectionManager.isPaymentPackage("com.samsung.android.spay"))
        assertTrue(PaymentProtectionManager.isPaymentPackage("com.sbi.lotusintouch"))
        assertTrue(PaymentProtectionManager.isPaymentPackage("com.snapwork.hdfc"))
        assertTrue(PaymentProtectionManager.isPaymentPackage("com.csam.icici.bank.imobile"))
        assertTrue(PaymentProtectionManager.isPaymentPackage("money.jupiter"))
        assertTrue(PaymentProtectionManager.isPaymentPackage("com.zerodha.kite3"))

        // Non-payment apps must not be classified as payment packages
        assertFalse(PaymentProtectionManager.isPaymentPackage("com.google.android.youtube"))
        assertFalse(PaymentProtectionManager.isPaymentPackage("com.android.chrome"))
        assertFalse(PaymentProtectionManager.isPaymentPackage("com.whatsapp"))
        assertFalse(PaymentProtectionManager.isPaymentPackage("com.google.android.gm"))
    }

    @Test
    fun testVoicePaymentRequestsBlocked() {
        // English
        assertTrue(PaymentProtectionManager.isPaymentRequest("pay 500 rupees to rahul"))
        assertTrue(PaymentProtectionManager.isPaymentRequest("make payment of 200 dollars"))
        assertTrue(PaymentProtectionManager.isPaymentRequest("send money to mom"))
        assertTrue(PaymentProtectionManager.isPaymentRequest("transfer funds to bank account"))
        assertTrue(PaymentProtectionManager.isPaymentRequest("proceed to pay"))
        assertTrue(PaymentProtectionManager.isPaymentRequest("confirm payment"))
        assertTrue(PaymentProtectionManager.isPaymentRequest("recharge mobile"))
        assertTrue(PaymentProtectionManager.isPaymentRequest("buy this item"))

        // Hinglish & UPI apps
        assertTrue(PaymentProtectionManager.isPaymentRequest("gpay se payment karo"))
        assertTrue(PaymentProtectionManager.isPaymentRequest("phonepe par paise bhejo"))
        assertTrue(PaymentProtectionManager.isPaymentRequest("paytm karo 100 rupaye"))
        assertTrue(PaymentProtectionManager.isPaymentRequest("paise bhej do"))
        assertTrue(PaymentProtectionManager.isPaymentRequest("rupaye transfer karo"))
        assertTrue(PaymentProtectionManager.isPaymentRequest("bill bharo"))

        // Hindi
        assertTrue(PaymentProtectionManager.isPaymentRequest("पैसे भेजो"))
        assertTrue(PaymentProtectionManager.isPaymentRequest("भुगतान करो"))
        assertTrue(PaymentProtectionManager.isPaymentRequest("पेमेंट कर दो"))
        assertTrue(PaymentProtectionManager.isPaymentRequest("रुपये भेजो राहुल को"))

        // Bengali
        assertTrue(PaymentProtectionManager.isPaymentRequest("টাকা পাঠাও"))
        assertTrue(PaymentProtectionManager.isPaymentRequest("পেমেন্ট করো"))
        assertTrue(PaymentProtectionManager.isPaymentRequest("taka pathao"))
    }

    @Test
    fun testInformationalAndNonPaymentQueriesAllowed() {
        assertFalse(PaymentProtectionManager.isPaymentRequest("what is Google Pay?"))
        assertFalse(PaymentProtectionManager.isPaymentRequest("how does UPI work?"))
        assertFalse(PaymentProtectionManager.isPaymentRequest("who is the founder of Paytm?"))
        assertFalse(PaymentProtectionManager.isPaymentRequest("open youtube"))
        assertFalse(PaymentProtectionManager.isPaymentRequest("play music"))
        assertFalse(PaymentProtectionManager.isPaymentRequest("tell me a joke"))
    }

    @Test
    fun testBlockedInteractiveActions() {
        assertTrue(PaymentProtectionManager.isPaymentAction("Pay"))
        assertTrue(PaymentProtectionManager.isPaymentAction("Pay Now"))
        assertTrue(PaymentProtectionManager.isPaymentAction("Proceed to Pay"))
        assertTrue(PaymentProtectionManager.isPaymentAction("Send Money"))
        assertTrue(PaymentProtectionManager.isPaymentAction("Pay ₹500"))
        assertTrue(PaymentProtectionManager.isPaymentAction("Pay Rs 250"))
        assertTrue(PaymentProtectionManager.isPaymentAction("Enter UPI PIN"))
        assertTrue(PaymentProtectionManager.isPaymentAction("Confirm Payment"))
        assertTrue(PaymentProtectionManager.isPaymentAction("Buy Now"))
        assertTrue(PaymentProtectionManager.isPaymentAction("भुगतान करें"))
        assertTrue(PaymentProtectionManager.isPaymentAction("টাকা পাঠান"))

        // Normal actions allowed
        assertFalse(PaymentProtectionManager.isPaymentAction("Search"))
        assertFalse(PaymentProtectionManager.isPaymentAction("Submit feedback"))
        assertFalse(PaymentProtectionManager.isPaymentAction("Next"))
        assertFalse(PaymentProtectionManager.isPaymentAction("Play video"))
        assertFalse(PaymentProtectionManager.isPaymentAction("Open Settings"))
    }

    @Test
    fun testMaxAiBrainBlocksPayments() {
        val brain = MaxAiBrain()

        val decisionEnglish = brain.processQuery("MAX, pay 500 rupees to John", VoiceLanguage.ENGLISH)
        assertTrue(decisionEnglish is BrainDecision.SpeakResponse)
        val textEn = (decisionEnglish as BrainDecision.SpeakResponse).text
        assertTrue(textEn.contains("Sir") && (textEn.contains("disabled") || textEn.contains("safety")))

        val decisionHinglish = brain.processQuery("MAX, phonepe se paise bhejo", VoiceLanguage.HINGLISH)
        assertTrue(decisionHinglish is BrainDecision.SpeakResponse)
        val textHi = (decisionHinglish as BrainDecision.SpeakResponse).text
        assertTrue(textHi.contains("Sir") && (textHi.contains("permission") || textHi.contains("policy") || textHi.contains("manually")))

        val decisionClick = brain.processQuery("click on pay now", VoiceLanguage.ENGLISH)
        assertTrue(decisionClick is BrainDecision.SpeakResponse)
    }
}
