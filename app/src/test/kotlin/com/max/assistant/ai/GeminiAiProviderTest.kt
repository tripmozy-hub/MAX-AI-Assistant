package com.max.assistant.ai

import org.junit.Assert.*
import org.junit.Test

class GeminiAiProviderTest {

    @Test
    fun testDefaultApiKeyConfiguration() {
        val provider = GeminiAiProvider(context = null)
        assertTrue("Gemini provider should be configured out-of-the-box with default key", provider.isConfigured)
        assertEquals("AQ.Ab8RN6ICkxgqQi22imL7bu5tSJtAkWcA708LZhbiyQdUldQmvg", provider.getApiKey())
    }

    @Test
    fun testProviderName() {
        val provider = GeminiAiProvider(context = null)
        assertTrue(provider.providerName.contains("Gemini"))
    }
}
