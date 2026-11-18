package com.max.assistant.ai

import org.junit.Assert.*
import org.junit.Test

class GeminiAiProviderTest {

    @Test
    fun testDefaultApiKeyConfiguration() {
        val provider = GeminiAiProvider(context = null)
        assertTrue("Gemini provider should be configured out-of-the-box with default key", provider.isConfigured)
        assertEquals("AQ.Ab8RN6KrQ3f8RmpYIpfNFS2EoFABCZ0v_EMi_7aEbB6XzTBwrg", provider.getApiKey())
        assertEquals("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", provider.getBackupApiKey())
    }

    @Test
    fun testProviderName() {
        val provider = GeminiAiProvider(context = null)
        assertTrue(provider.providerName.contains("Gemini"))
    }
}
