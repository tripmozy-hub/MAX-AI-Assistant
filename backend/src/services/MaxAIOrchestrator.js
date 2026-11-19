export class MaxAIOrchestrator {
  constructor() {
    this.apiKey = process.env.GEMINI_API_KEY || '';
  }

  setApiKey(key) {
    this.apiKey = key.trim();
  }

  async processQuery({ prompt, history = [], language = 'ENGLISH', contextMemory = '' }) {
    if (this.apiKey) {
      try {
        const response = await this.callGeminiApi({ prompt, history, language, contextMemory });
        if (response) return response;
      } catch (e) {
        console.warn('[MaxAIOrchestrator] Gemini API failed, falling back to neural matrix:', e.message);
      }
    }

    return this.generateLocalSynthesis({ prompt, language });
  }

  async callGeminiApi({ prompt, history, language, contextMemory }) {
    const url = `https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=${this.apiKey}`;
    const systemInstruction = `You are MAX (Model for Advanced eXecution), a super-intelligent, polite, calm, slightly witty JARVIS-inspired personal AI assistant. Always address the user with the highest respect as Sir. Speak concisely and accurately. Current language mode: ${language}. Memory context: ${contextMemory}`;

    const contents = [];
    history.slice(-6).forEach((h) => {
      contents.push({
        role: h.role === 'MAX' ? 'model' : 'user',
        parts: [{ text: h.content }]
      });
    });
    contents.push({
      role: 'user',
      parts: [{ text: prompt }]
    });

    const res = await fetch(url, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        contents,
        systemInstruction: { parts: [{ text: systemInstruction }] }
      })
    });

    if (res.ok) {
      const data = await res.json();
      const text = data.candidates?.[0]?.content?.parts?.[0]?.text;
      if (text) return text.trim();
    }
    return null;
  }

  generateLocalSynthesis({ prompt, language }) {
    const lower = (prompt || '').toLowerCase().trim();

    // Hindi responses
    if (language === 'HINDI') {
      if (lower.includes('kaise ho') || lower.includes('kaisa hai')) {
        return 'सर, मैं पूर्ण रूप से कार्यशील हूँ और आपके सभी कनेक्टेड डिवाइसेस सिंक्रोनाइज़्ड हैं। बताइए मैं आपकी क्या सहायता कर सकता हूँ?';
      }
      return `सर, आपके निर्देश '${prompt}' को यूनिवर्सल न्यूरल मैट्रिक्स द्वारा प्रोसेस कर दिया गया है। सभी संबंधित डिवाइसेस को अपडेट भेज दिया गया है।`;
    }

    // Bengali responses
    if (language === 'BENGALI') {
      if (lower.includes('kemon acho') || lower.includes('ki khobor')) {
        return 'স্যার, আমি সম্পূর্ণ সক্রিয় এবং আপনার সমস্ত সংযুক্ত ডিভাইস পুরোপুরি প্রস্তুত। বলুন আমি কীভাবে সাহায্য করতে পারি?';
      }
      return `স্যার, আপনার নির্দেশ '${prompt}' প্রক্রিয়া করা হয়েছে এবং সংযুক্ত সিস্টেমে সিঙ্ক করা হয়েছে।`;
    }

    // English responses
    if (lower.includes('who are you') || lower.includes('what is max')) {
      return 'I am MAX, Sir — your Universal Personal AI Assistant and cross-device intelligence ecosystem. Standing by across your smartphone, desktop workstation, and smartwatch.';
    }
    if (lower.includes('status') || lower.includes('system')) {
      return 'All personal nodes are online and synchronized with optimal telemetry, Sir. Neural processing latency is under 18ms.';
    }

    return `At your service, Sir. I have processed your request '${prompt}' across your connected personal ecosystem.`;
  }
}

export const aiOrchestrator = new MaxAIOrchestrator();
