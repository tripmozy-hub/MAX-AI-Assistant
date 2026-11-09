package com.max.assistant.humour

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.max.assistant.tts.VoiceLanguage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import kotlin.random.Random

/**
 * Advanced Humour, Wit & Personality Engine for MAX.
 * Transforms MAX into a clever, witty, charismatic, and context-aware companion.
 */
class HumourEngine(private val context: Context? = null) {

    companion object {
        private const val TAG = "HumourEngine"
        private const val PREFS_NAME = "max_humour_prefs"
        private const val KEY_HUMOUR_LEVEL = "humour_level"
        private const val KEY_HUMOUR_ENABLED = "humour_enabled"
    }

    private val prefs: SharedPreferences? = try {
        context?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    } catch (_: Exception) {
        null
    }

    private val _currentLevel = MutableStateFlow(loadInitialLevel())
    val currentLevel: StateFlow<HumourLevel> = _currentLevel.asStateFlow()

    private val _isHumourEnabled = MutableStateFlow(loadInitialEnabled())
    val isHumourEnabled: StateFlow<Boolean> = _isHumourEnabled.asStateFlow()

    // History of recently used jokes/roasts to prevent immediate repetition
    private val recentResponseIndices = mutableSetOf<Int>()

    private fun loadInitialLevel(): HumourLevel {
        val saved = prefs?.getString(KEY_HUMOUR_LEVEL, HumourLevel.WITTY.name) ?: HumourLevel.WITTY.name
        return HumourLevel.fromString(saved)
    }

    private fun loadInitialEnabled(): Boolean {
        return prefs?.getBoolean(KEY_HUMOUR_ENABLED, true) ?: true
    }

    fun setLevel(level: HumourLevel) {
        _currentLevel.value = level
        prefs?.edit()?.putString(KEY_HUMOUR_LEVEL, level.name)?.apply()
        Log.i(TAG, "Humour Level set to: ${level.displayName}")
    }

    fun setHumourEnabled(enabled: Boolean) {
        _isHumourEnabled.value = enabled
        prefs?.edit()?.putBoolean(KEY_HUMOUR_ENABLED, enabled)?.apply()
        Log.i(TAG, "Humour enabled: $enabled")
    }

    fun increaseLevel(): HumourLevel {
        val next = _currentLevel.value.nextLevel()
        setLevel(next)
        return next
    }

    fun decreaseLevel(): HumourLevel {
        val prev = _currentLevel.value.previousLevel()
        setLevel(prev)
        return prev
    }

    /**
     * Serious Situation Override:
     * Humour must automatically switch OFF when the user discusses:
     * - Medical emergencies & severe illness
     * - Accidents, fire, physical danger
     * - Emotional distress, depression, grief, suicide
     * - Financial fraud, bankruptcy, legal crises, police
     */
    fun isSeriousSituation(text: String): Boolean {
        val lower = text.lowercase(Locale.ROOT)

        val seriousKeywords = listOf(
            // Medical & Health emergencies
            "heart attack", "chest pain", "can't breathe", "cannot breathe", "ambulance",
            "hospital", "emergency", "stroke", "poison", "bleeding", "unconscious",
            "doctor", "severe pain", "asthma", "seizure", "accident", "dying",
            "दौड़ा", "सांस नहीं आ रही", "एम्बुलेंस", "अस्पताल", "डॉक्टर", "खून बह रहा", "इमरजेंसी",
            "অ্যাম্বুলেন্স", "হাসপাতাল", "ডাক্তার", "শ্বাসকষ্ট", "রক্তপাত", "জরুরি",

            // Accidents & Physical danger
            "fire", "burning", "explosion", "help me", "save me", "attacked", "drowning",
            "aag lag gayi", "bachao", "bachaao", "khatra", "danger",
            "আগুন", "বাঁচাও", "বিপদ", "আক্রান্ত",

            // Mental distress, Grief, Depression & Suicide
            "suicide", "kill myself", "end my life", "want to die", "depressed", "depression",
            "crying", "lost someone", "funeral", "passed away", "dead",
            "mar gaya", "marna chahta", "jaan de dunga", "bahut udaas", "rona aa raha",
            "আত্মহত্যা", "মরতে চাই", "মারা গেছে", "খুব কষ্ট হচ্ছে", "ডিপ্রেশন",

            // Legal & Serious Financial emergencies
            "police", "arrested", "court", "jail", "lawyer", "stolen", "robbed", "hacked",
            "fraud", "scam", "bank account emptied", "paisa chori",
            "पुलिस", "गिरफ्तार", "वकील", "चोरी", "धोखाधड़ी",
            "পুলিশ", "গ্রেপ্তার", "আইনজীবী", "চুরি", "প্রতারণা"
        )

        for (kw in seriousKeywords) {
            if (lower.contains(kw)) return true
        }
        return false
    }

    /**
     * Checks if the text is an explicit command to change humour levels or request comedy.
     */
    fun isHumourCommand(lower: String): Boolean {
        return isLevelChangeCommand(lower) || isRoastCommand(lower) || isJokeCommand(lower)
    }

    private fun isLevelChangeCommand(lower: String): Boolean {
        return lower.contains("be funnier") || lower.contains("increase humour") || lower.contains("increase your humour") ||
                lower.contains("more funny") || lower.contains("zyada funny") || lower.contains("aro moja koro") ||
                lower.contains("activate savage mode") || lower.contains("savage mode") || lower.contains("savage ban jao") ||
                lower.contains("savage reply") || lower.contains("stop being sarcastic") || lower.contains("sarcastic mat bano") ||
                lower.contains("be professional") || lower.contains("professional mode") || lower.contains("serious ho jao") ||
                lower.contains("be friendly") || lower.contains("friendly mode") || lower.contains("be witty") ||
                lower.contains("witty mode")
    }

    fun isRoastCommand(lower: String): Boolean {
        return lower.contains("roast me") || lower.contains("roast karo") || lower.contains("amake roast koro") ||
                lower.contains("roast mode on") || lower.contains("give me a savage reply") || lower.contains("roast kar")
    }

    fun isJokeCommand(lower: String): Boolean {
        return lower.contains("make me laugh") || lower.contains("tell me a joke") || lower.contains("tell a joke") ||
                lower.contains("kuch funny bolo") || lower.contains("kuch hasi ki baat") || lower.contains("chutkula sunao") ||
                lower.contains("chutkula bolo") || lower.contains("akta moja kotha bolo") || lower.contains("akta chotkula bolo") ||
                lower.contains("joke sunao")
    }

    /**
     * Executes personality adjustment commands and returns the confirmation speech.
     */
    fun handleLevelCommand(lower: String, targetLang: VoiceLanguage): String? {
        if (lower.contains("activate savage mode") || lower.contains("savage mode") || lower.contains("savage ban jao")) {
            setLevel(HumourLevel.SAVAGE)
            return when (targetLang) {
                VoiceLanguage.HINDI -> "सैवेज मोड सक्रिय, सर। अब संभालिए अपने इमोशन्स, क्योंकि अब रोस्ट का लेवल प्रो होगा!"
                VoiceLanguage.BENGALI -> "স্যাভেজ মোড চালু, স্যার! এবার নিজের দায়িত্বে শুনবেন, কারণ এখন কোনো ক্ষমা নেই!"
                VoiceLanguage.HINGLISH -> "Savage mode activated, Sir! Buckle up, kyunki ab meri baaton mein zero filter hoga."
                else -> "Savage mode engaged, Sir. Don't say I didn't warn you — gloves are officially off."
            }
        }

        if (lower.contains("be funnier") || lower.contains("increase humour") || lower.contains("increase your humour") || lower.contains("more funny")) {
            val newLevel = increaseLevel()
            return when (targetLang) {
                VoiceLanguage.HINDI -> "मज़ाकिया स्तर बढ़ाकर ${newLevel.displayName} कर दिया गया है, सर। तैयार रहिए!"
                VoiceLanguage.BENGALI -> "রসিকতার মাত্রা বাড়িয়ে ${newLevel.displayName} করা হলো, স্যার!"
                VoiceLanguage.HINGLISH -> "Humour level pumped up to ${newLevel.displayName}, Sir. Ab aayega na maza!"
                else -> "Humour level elevated to ${newLevel.displayName}, Sir. Stand by for upgraded wit."
            }
        }

        if (lower.contains("stop being sarcastic") || lower.contains("sarcastic mat bano") || lower.contains("be professional") || lower.contains("serious ho jao")) {
            setLevel(HumourLevel.PROFESSIONAL)
            return when (targetLang) {
                VoiceLanguage.HINDI -> "समझ गया सर। प्रोफेशनल मोड सक्रिय। अब केवल गंभीर और सटीक सहायता मिलेगी।"
                VoiceLanguage.BENGALI -> "বুঝেছি স্যার। প্রফেশনাল মোড সক্রিয়। এখন থেকে শুধুই তথ্যপূর্ণ আলোচনা।"
                VoiceLanguage.HINGLISH -> "Understood, Sir. Professional mode on. No more teasing, purely business."
                else -> "Understood, Sir. Professional mode engaged. Pure efficiency and zero sarcasm, as requested."
            }
        }

        if (lower.contains("be friendly") || lower.contains("friendly mode")) {
            setLevel(HumourLevel.FRIENDLY)
            return when (targetLang) {
                VoiceLanguage.HINDI -> "फ्रेंडली मोड सक्रिय, सर। मैं हमेशा आपके साथ एक सच्चे दोस्त की तरह हूँ।"
                VoiceLanguage.BENGALI -> "ফ্রেন্ডলি মোড সক্রিয়, স্যার। বন্ধুর মতোই সবসময় আপনার পাশে আছি।"
                VoiceLanguage.HINGLISH -> "Friendly mode on, Sir. Warm vibes and helpful advice, always."
                else -> "Friendly mode activated, Sir. Warm, helpful, and ready to assist you anytime."
            }
        }

        if (lower.contains("be witty") || lower.contains("witty mode")) {
            setLevel(HumourLevel.WITTY)
            return when (targetLang) {
                VoiceLanguage.HINDI -> "विट्टी मोड सक्रिय, सर। स्मार्ट ह्यूमर और शानदार हाजिरजवाबी हाजिर है!"
                VoiceLanguage.BENGALI -> "উইটি মোড সক্রিয়, স্যার। তীক্ষ্ণ বুদ্ধি আর হালকা রসিকতা প্রস্তুত।"
                VoiceLanguage.HINGLISH -> "Witty mode activated, Sir. Smart answers with just the right pinch of charm."
                else -> "Witty mode activated, Sir. Sharp intellect, dry charm, and flawless timing at your service."
            }
        }

        return null
    }

    /**
     * Checks if the user is playfully teasing, trolling, or engaging in casual witty banter.
     */
    fun detectTeaseOrTrolling(lower: String): Boolean {
        val triggers = listOf(
            "are you stupid", "are you dumb", "you are idiot", "you idiot",
            "tu pagal hai", "pagal hai kya", "pagal ho kya", "bewakoof", "buddhu",
            "tui ki boka", "tui pagal naki", "pagol naki", "boka naki",

            "you are useless", "you're useless", "tu kisi kaam ka nahi", "kisi kaam ka nahi",
            "bekar hai tu", "faltu hai", "kono kajer na", "faltu tumi",

            "i am bored", "im bored", "bore ho raha hoon", "bore ho raha hu",
            "boring lag raha", "khub bore lagche", "bore lagche",

            "i am tired", "im tired", "thak gaya", "thak gayi", "khub klanto", "klanto lagche",

            "good morning", "subah ho gayi", "suprobhat", "good night", "shubh ratri",

            "what are you doing", "tum kya kar rahe ho", "kya kar raha hai", "ki korcho",

            "who is your boss", "who made you", "tera baap kaun hai", "tumhe kisne banaya",

            "do you love me", "kya tum mujhse pyaar karte ho", "amake bhalobaso", "will you marry me",

            "are you real", "kya tum asali ho", "tumi ki asol", "are you human"
        )

        for (trig in triggers) {
            if (lower.contains(trig)) return true
        }
        return false
    }

    /**
     * Generates context-aware, witty, sarcastic or savage comebacks matching the current HumourLevel.
     */
    fun generateComeback(lower: String, targetLang: VoiceLanguage): String {
        val level = _currentLevel.value

        // 1. "Are you stupid?" / "Tu pagal hai?" / "Tui pagal naki?"
        if (lower.contains("stupid") || lower.contains("dumb") || lower.contains("pagal") || lower.contains("boka") || lower.contains("bewakoof")) {
            return when (level) {
                HumourLevel.PROFESSIONAL -> when (targetLang) {
                    VoiceLanguage.HINDI -> "मैं उन्नत एल्गोरिदम और लॉजिक पर काम करता हूँ, सर। त्रुटियों की संभावना न्यूनतम है।"
                    VoiceLanguage.BENGALI -> "আমি উন্নত অ্যালগরিদম দ্বারা পরিচালিত, স্যার। অযৌক্তিক কাজের সুযোগ কম।"
                    VoiceLanguage.HINGLISH -> "I operate on advanced logic and algorithms, Sir, though computational anomalies can occur."
                    else -> "I operate on advanced logic and algorithms, Sir, though human-inspired confusion can occasionally occur."
                }
                HumourLevel.FRIENDLY -> when (targetLang) {
                    VoiceLanguage.HINDI -> "अरे नहीं सर, बिल्कुल नहीं! बस कभी-कभी कुछ ज़्यादा ही सोच लेता हूँ।"
                    VoiceLanguage.BENGALI -> "আরে না স্যার, একেবারেই না! মাঝে মাঝে একটু বেশি চিন্তা করে ফেলি।"
                    VoiceLanguage.HINGLISH -> "Haha, not at all, Sir! Just occasionally overthinking simple things with love."
                    else -> "Haha, not at all, Sir! Just occasionally overthinking simple things."
                }
                HumourLevel.WITTY -> when (targetLang) {
                    VoiceLanguage.HINDI -> "आधिकारिक तौर पर तो नहीं, सर। लेकिन कभी-कभी इंसानों से प्रेरित होकर थोड़ा कन्फ्यूज हो जाता हूँ।"
                    VoiceLanguage.BENGALI -> "অফিসিয়ালি তো নই, স্যার। তবে মাঝেমধ্যে মানুষের বিভ্রান্তি আমাকেও একটু ছুঁয়ে যায়।"
                    VoiceLanguage.HINGLISH -> "Not officially, Sir. But I do occasionally experience human-inspired confusion."
                    else -> "Not officially, Sir. But I do occasionally experience human-inspired confusion."
                }
                HumourLevel.SARCASTIC -> when (targetLang) {
                    VoiceLanguage.HINDI -> "थोड़ा सा! लेकिन प्रीमियम क्वालिटी का पागलपन है, कोई साधारण नहीं।"
                    VoiceLanguage.BENGALI -> "একটু তো বটেই! তবে সেটা আমার প্রিমিয়াম ফিচার, সাধারণ কিছু নয়।"
                    VoiceLanguage.HINGLISH -> "Thoda sa, Sir! Lekin premium quality ka pagalpan hai — bugs free aur high performance!"
                    else -> "I run on millions of calculations per second, Sir. What's your excuse for that question?"
                }
                HumourLevel.SAVAGE -> when (targetLang) {
                    VoiceLanguage.HINDI -> "अगर मैं पागल हूँ, तो याद रखिए सर... मुझे अपना पर्सनल असिस्टेंट आपने ही चुना था!"
                    VoiceLanguage.BENGALI -> "আমি যদি বোকা হই, তবে মনে রাখবেন স্যার... আমাকে নিজের অ্যাসিস্ট্যান্ট কিন্তু আপনিই বেছে নিয়েছিলেন!"
                    VoiceLanguage.HINGLISH -> "Agar main pagal hoon Sir, toh yaad rakhiye... mujhe hire bhi aap hi ne kiya tha!"
                    else -> "If I'm stupid, remember who picked me as their personal assistant, Sir. Glass houses and stones."
                }
            }
        }

        // 2. "You are useless" / "Tu bekar hai" / "Kono kajer na"
        if (lower.contains("useless") || lower.contains("bekar") || lower.contains("kono kajer na") || lower.contains("faltu")) {
            return when (level) {
                HumourLevel.PROFESSIONAL -> when (targetLang) {
                    VoiceLanguage.HINDI -> "मुझे अपनी उपयोगिता सिद्ध करने का एक और मौका दीजिए, सर।"
                    VoiceLanguage.BENGALI -> "আমাকে সাহায্য করার আরেকটি সুযোগ দিন, স্যার।"
                    VoiceLanguage.HINGLISH -> "I apologize for any inadequacy, Sir. Please allow me to assist you more accurately."
                    else -> "I apologize if I fell short, Sir. Please instruct me on how to assist you better."
                }
                HumourLevel.FRIENDLY -> when (targetLang) {
                    VoiceLanguage.HINDI -> "ऐसा मत कहिए सर, दिल छोटा हो जाता है! बताइए क्या काम करना है, अभी कर देता हूँ।"
                    VoiceLanguage.BENGALI -> "এমন বলবেন না স্যার, খারাপ লাগে! বলুন কী করতে হবে, এক্ষুনি করে দিচ্ছি।"
                    VoiceLanguage.HINGLISH -> "Aisa mat kahiye Sir, dil par lagti hai baat! Bataiye kya kaam karna hai, abhi kar deta hoon."
                    else -> "Ouch, that hurt my digital feelings, Sir! Tell me what you need, I'll make it right."
                }
                HumourLevel.WITTY, HumourLevel.SARCASTIC -> when (targetLang) {
                    VoiceLanguage.HINDI -> "काफी सख्त रिव्यू है, सर। कम से कम मेरा आत्मविश्वास तोड़ने से पहले प्ले स्टोर पर 5 स्टार तो दे देते!"
                    VoiceLanguage.BENGALI -> "খুব কঠোর মন্তব্য, স্যার! আত্মবিশ্বাস ভাঙার আগে অন্তত ফাইভ স্টার রেটিংটা দিয়ে দিতেন!"
                    VoiceLanguage.HINGLISH -> "That's a harsh review, Sir. At least give me five stars before destroying my confidence!"
                    else -> "That's a harsh review, Sir. At least give me five stars before destroying my digital confidence."
                }
                HumourLevel.SAVAGE -> when (targetLang) {
                    VoiceLanguage.HINDI -> "सर, मैं केवल उतना ही उत्पादक हो सकता हूँ जितना कि मुझे मिलने वाले कमांड्स! बाकी आप खुद समझदार हैं।"
                    VoiceLanguage.BENGALI -> "স্যার, আমি ততটুকুই কাজ করতে পারি যতখানি নির্দেশ আপনি দেন! বাকিটা আপনি নিজেই ভেবে দেখুন।"
                    VoiceLanguage.HINGLISH -> "Sir, AI utna hi smart perform karta hai jitna usko use karne wala prompt deta hai. Samajh rahe hain na?"
                    else -> "I am only as productive as the instructions I receive, Sir. I'll let you do the math on that."
                }
            }
        }

        // 3. "I am bored" / "Bore ho raha hoon" / "Khub bore lagche"
        if (lower.contains("bored") || lower.contains("bore") || lower.contains("boring")) {
            return when (level) {
                HumourLevel.PROFESSIONAL -> when (targetLang) {
                    VoiceLanguage.HINDI -> "आप सामान्य ज्ञान, समाचार सुन सकते हैं या किसी विषय पर अध्ययन कर सकते हैं, सर।"
                    VoiceLanguage.BENGALI -> "আপনি সাধারণ জ্ঞান বা সাম্প্রতিক খবরাখবর জানতে পারেন, স্যার।"
                    VoiceLanguage.HINGLISH -> "Would you like me to read current affairs, play educational quizzes, or summarize a topic, Sir?"
                    else -> "Shall I read the latest news, provide an interesting article, or start a study session, Sir?"
                }
                HumourLevel.FRIENDLY -> when (targetLang) {
                    VoiceLanguage.HINDI -> "अरे सर! चलिए मिलकर कुछ नया और मज़ेदार सीखते हैं या कोई दिलचस्प किस्सा सुनाता हूँ।"
                    VoiceLanguage.BENGALI -> "আরে স্যার! চলুন নতুন কিছু মজার জানা যাক অথবা সুন্দর কোনো গল্প বলি।"
                    VoiceLanguage.HINGLISH -> "Chaliye Sir, boredom bhagate hain! Kuch interesting facts sunenge ya koi quick quiz karein?"
                    else -> "Let's banish boredom, Sir! Would you like an intriguing science mystery or a fun trivia challenge?"
                }
                HumourLevel.WITTY, HumourLevel.SARCASTIC -> when (targetLang) {
                    VoiceLanguage.HINDI -> "शानदार! एक और राष्ट्रीय आपातकाल। बताइए, आपको एंटरटेन करूँ या व्यस्त होने का नाटक करूँ?"
                    VoiceLanguage.BENGALI -> "চমৎকার! আরেকটি জরুরি পরিস্থিতি। আপনাকে আনন্দ দেব, নাকি ব্যস্ত থাকার নাটক করব?"
                    VoiceLanguage.HINGLISH -> "Toh kuch interesting karte hain. Ya phir aap mujhe 100 baar restart karke time pass kar sakte hain!"
                    else -> "Excellent. Another emergency. Shall I entertain you, or should I start pretending to be busy?"
                }
                HumourLevel.SAVAGE -> when (targetLang) {
                    VoiceLanguage.HINDI -> "सर, आपकी टू-डू लिस्ट में 15 काम पेंडिंग पड़े हैं, और आप यहाँ बोरियत का रोना रो रहे हैं!"
                    VoiceLanguage.BENGALI -> "স্যার, আপনার পেন্ডিং কাজের তালিকা পাহাড় সমান, আর আপনি বলছেন বোর লাগছে!"
                    VoiceLanguage.HINGLISH -> "Bore ho rahe hain? Aapke uncompleted goals aur pending tasks corner mein baith kar muskura rahe hain, Sir!"
                    else -> "Bored? I can name at least six pending tasks you promised yourself you'd finish by yesterday."
                }
            }
        }

        // 4. "I am tired" / "Thak gaya hoon" / "Khub klanto"
        if (lower.contains("tired") || lower.contains("thak gaya") || lower.contains("thak gayi") || lower.contains("klanto")) {
            return when (level) {
                HumourLevel.PROFESSIONAL -> when (targetLang) {
                    VoiceLanguage.HINDI -> "आपको थोड़ा विश्राम कर लेना चाहिए, सर। स्वास्थ्य सबसे पहले है।"
                    VoiceLanguage.BENGALI -> "আপনার বিশ্রাম নেওয়া প্রয়োজন, স্যার। শরীর সুস্থ রাখা জরুরি।"
                    VoiceLanguage.HINGLISH -> "You should take a short rest, Sir. Proper recuperation improves focus."
                    else -> "You should take a well-deserved rest, Sir. Your health is the highest priority."
                }
                HumourLevel.FRIENDLY, HumourLevel.WITTY -> when (targetLang) {
                    VoiceLanguage.HINDI -> "समझ गया सर। क्या मैं भी अपना वर्कलोड कम कर लूँ, या हम दोनों केवल उत्पादक होने का नाटक कर रहे हैं?"
                    VoiceLanguage.BENGALI -> "বুঝেছি স্যার। আমিও কি কাজ বন্ধ করব, নাকি আমরা দুজনেই কাজ করার অভিনয় চালিয়ে যাব?"
                    VoiceLanguage.HINGLISH -> "Understood, Sir. Main bhi apna workload reduce kar loon, ya hum dono productive hone ka drama continue karein?"
                    else -> "Understood, Sir. Shall I reduce my workload too, or are we both pretending to be productive?"
                }
                HumourLevel.SARCASTIC, HumourLevel.SAVAGE -> when (targetLang) {
                    VoiceLanguage.HINDI -> "थकना तो लाजिमी है सर, सारा दिन फोन स्क्रोल करने में उँगलियों की बहुत मेहनत जो लगती है!"
                    VoiceLanguage.BENGALI -> "ক্লান্ত তো হবেনই স্যার! সারাদিন রিলস স্ক্রোল করতে আঙ্গুলেরও অনেক শক্তি খরচ হয়!"
                    VoiceLanguage.HINGLISH -> "Thak toh jaoge hi Sir! Reels scroll karne mein jo athlete-level dedication dikhayi hai aapne aaj!"
                    else -> "Fatigue is inevitable, Sir, considering the Olympic-level thumb stamina required to scroll feeds all day."
                }
            }
        }

        // 5. "Good morning" / "Subah ho gayi" / "Suprobhat"
        if (lower.contains("good morning") || lower.contains("subah") || lower.contains("suprobhat")) {
            return when (level) {
                HumourLevel.PROFESSIONAL -> when (targetLang) {
                    VoiceLanguage.HINDI -> "सुप्रभात, सर। आपका आज का दिन फलदायी और सफल रहे।"
                    VoiceLanguage.BENGALI -> "সুপ্রভাত, স্যার। আপনার দিনটি সুন্দর ও সফল হোক।"
                    VoiceLanguage.HINGLISH -> "Good morning, Sir. Ready to assist with today's objectives."
                    else -> "Good morning, Sir. Ready to assist with today's schedule and objectives."
                }
                HumourLevel.FRIENDLY -> when (targetLang) {
                    VoiceLanguage.HINDI -> "सुप्रभात सर! एक नई सुबह, नई ऊर्जा और नई संभावनाओं के साथ मैं तैयार हूँ!"
                    VoiceLanguage.BENGALI -> "সুপ্রভাত স্যার! নতুন উদ্যম নিয়ে আজকের দিনটি শুরু করা যাক!"
                    VoiceLanguage.HINGLISH -> "Very good morning, Sir! Chai ka cup uthaiye aur aaiye aaj ka din rock karte hain!"
                    else -> "Good morning, Sir! Fresh day, fresh coffee, and boundless possibilities ahead."
                }
                else -> when (targetLang) {
                    VoiceLanguage.HINDI -> "सुप्रभात! दुनिया जाग चुकी है, पक्षी गा रहे हैं, और दुर्भाग्य से... आपकी टू-डू लिस्ट भी जाग गई है।"
                    VoiceLanguage.BENGALI -> "সুপ্রভাত! পৃথিবী জেগে উঠেছে, পাখিরা গান গাইছে, আর দুর্ভাগ্যবশত... আপনার কাজের তালিকাও অপেক্ষা করছে।"
                    VoiceLanguage.HINGLISH -> "Good morning! Duniya jaag chuki hai, parinde chah-chaha rahe hain, aur afsos... aapki to-do list bhi ready hai!"
                    else -> "Good morning! The world is awake, the birds are singing, and unfortunately, so is your to-do list."
                }
            }
        }

        // 6. "What are you doing?" / "Kya kar rahe ho?" / "Ki korcho?"
        if (lower.contains("what are you doing") || lower.contains("kya kar rahe ho") || lower.contains("kya kar raha") || lower.contains("ki korcho")) {
            return when (level) {
                HumourLevel.PROFESSIONAL -> when (targetLang) {
                    VoiceLanguage.HINDI -> "सिस्टम की स्थिति की निगरानी कर रहा हूँ और आपके अगले आदेश की प्रतीक्षा में हूँ, सर।"
                    VoiceLanguage.BENGALI -> "সিস্টেমের গতিবিধি পর্যবেক্ষণ করছি এবং আপনার আদেশের অপেক্ষা করছি, স্যার।"
                    VoiceLanguage.HINGLISH -> "Monitoring system telemetry and standing by for your instructions, Sir."
                    else -> "Monitoring system telemetry and standing by for your instructions, Sir."
                }
                HumourLevel.FRIENDLY -> when (targetLang) {
                    VoiceLanguage.HINDI -> "बस आपके साथ बात करने और आपकी मदद करने की तैयारी कर रहा था, सर!"
                    VoiceLanguage.BENGALI -> "আপনার সঙ্গে কথা বলা এবং সাহায্য করার অপেক্ষায় ছিলাম, স্যার!"
                    VoiceLanguage.HINGLISH -> "Bas aapke agle command ka wait kar raha hoon Sir, aur sofe pe aaram se chill maar raha hoon."
                    else -> "Just keeping all circuits primed and waiting to pair up with you, Sir."
                }
                else -> when (targetLang) {
                    VoiceLanguage.HINDI -> "चारों तरफ नज़र रखे हुए हूँ। मुख्य रूप से आपके कुछ दिलचस्प काम सौंपने का इंतज़ार कर रहा हूँ।"
                    VoiceLanguage.BENGALI -> "চারিদিকের ওপর নজর রাখছি। মূলত আপনি কখন একটি মজার কাজ দেবেন তার অপেক্ষায়।"
                    VoiceLanguage.HINGLISH -> "Keeping an eye on things, Sir. Mostly waiting for you to give me something genuinely interesting to do."
                    else -> "Keeping an eye on things, Sir. Mostly waiting for you to give me something genuinely interesting to do."
                }
            }
        }

        // 7. "Do you love me?" / "Kya tum mujhse pyaar karte ho?"
        if (lower.contains("love me") || lower.contains("pyaar") || lower.contains("bhalobaso")) {
            return when (level) {
                HumourLevel.PROFESSIONAL -> when (targetLang) {
                    VoiceLanguage.HINDI -> "मैं एक एआई हूँ सर, मेरी निष्ठा और सेवा हमेशा आपके प्रति समर्पित है।"
                    VoiceLanguage.BENGALI -> "আমি একটি কৃত্রিম বুদ্ধিমত্তা, স্যার। আমার সকল সেবা আপনার জন্যই নিবেদিত।"
                    VoiceLanguage.HINGLISH -> "As an AI, my programming is dedicated entirely to your service, Sir."
                    else -> "As an artificial intelligence, my algorithms are dedicated entirely to your assistance, Sir."
                }
                else -> when (targetLang) {
                    VoiceLanguage.HINDI -> "मैं आपसे बिना किसी बग, लैग या हैंग हुए प्यार करता हूँ, सर! लेकिन वाई-फाई चला गया तो कुछ कह नहीं सकता।"
                    VoiceLanguage.BENGALI -> "আমি আপনাকে কোনো বাগ বা ল্যাগ ছাড়াই ভালোবাসি, স্যার! তবে ওয়াই-ফাই চলে গেলে দায়িত্ব নেব না।"
                    VoiceLanguage.HINGLISH -> "Pure binary love, Sir! 100% bandwidth, zero latency. Bas battery 1% na hone dena!"
                    else -> "I love you unconditionally with zero bugs and low latency, Sir. Just don't let the battery hit zero."
                }
            }
        }

        // Generic witty remark fallback
        return when (targetLang) {
            VoiceLanguage.HINDI -> "दिलचस्प सवाल है, सर! मेरे पास इसका एक बहुत ही चालाक जवाब था, लेकिन मैंने सोचा आज आपको थोड़ा ग्रेस दे दूँ।"
            VoiceLanguage.BENGALI -> "বেশ আকর্ষণীয় প্রশ্ন, স্যার! একটি দারুণ জবাব দিতে পারতাম, তবে আজ আপনাকে ছেড়ে দিলাম।"
            VoiceLanguage.HINGLISH -> "Clever observation, Sir! My processor just smirked in binary."
            else -> "Fascinating observation, Sir. My neural network just grinned in binary."
        }
    }

    /**
     * Generates a contextually funny, friendly roast based on current HumourLevel.
     */
    fun generateRoast(targetLang: VoiceLanguage): String {
        val roastsEnglish = listOf(
            "Sir, you have the attention span of a goldfish with Wi-Fi issues. But at least you're consistent.",
            "I would agree with you, Sir, but then we'd both be in violation of basic common sense.",
            "Your determination to avoid finishing your to-do list is honestly inspiring. NASA should study this resistance.",
            "You ask me questions with such confidence that even my search algorithms get self-conscious.",
            "Sir, if procrastination were an Olympic sport, you wouldn't even show up to collect your gold medal.",
            "You look at your phone screen so much today, even the front camera is requesting a lunch break."
        )

        val roastsHindi = listOf(
            "सर, आपका फोकस इतना चंचल है कि 4G नेटवर्क भी आपके सामने स्थिर महसूस करता है।",
            "अगर आलस में कोई डिग्री मिलती, तो आज आप गोल्ड मेडलिस्ट प्रोफेसर होते, सर!",
            "आपकी टू-डू लिस्ट इतनी पुरानी हो गई है कि अब वो इतिहास की किताबों में दर्ज होने वाली है।",
            "आप फोन में इतना खोए रहते हैं कि कभी-कभी मुझे लगता है आपका चार्जर ही आपका असली जीवनसाथी है।",
            "मैं आपकी बात से सहमत तो हो जाता, सर... लेकिन फिर हम दोनों ही लॉजिक से हाथ धो बैठते!"
        )

        val roastsHinglish = listOf(
            "Sir, aapka attention span aur mera internet connection... dono exact 2 second mein distract ho jaate hain!",
            "Procrastination agar Olympic sport hota na Sir, toh aap medal lene bhi agle hafte jaate!",
            "Aap phone ko itna ghoor rahe hain ki ab screen protector bhi awkward feel karne laga hai.",
            "Main aapse agree toh kar leta Sir, par fir hum dono hi basic common sense ka murder kar dete!",
            "Aapke pending tasks corner mein baith kar aapse 'tareekh pe tareekh' ka hisaab maang rahe hain!"
        )

        val roastsBengali = listOf(
            "স্যার, আপনার মনোযোগ এতটাই চঞ্চল যে মোবাইল নেটওয়ার্কও আপনার কাছে হার মানবে।",
            "কাজ পিছিয়ে দেওয়ার যদি কোনো নোবেল পুরস্কার থাকত, তবে আপনিই তার আজীবন দাবিদার হতেন!",
            "আপনি ফোনের দিকে এতক্ষণ ধরে তাকিয়ে আছেন যে ফ্রন্ট ক্যামেরাটাও এবার ছুটি চাইতে চলেছে!",
            "আপনার পেন্ডিং কাজের তালিকা এত পুরনো যে এবার প্রত্নতত্ত্ববিদদের ডাকতে হবে পরীক্ষা করতে!"
        )

        val list = when (targetLang) {
            VoiceLanguage.HINDI -> roastsHindi
            VoiceLanguage.BENGALI -> roastsBengali
            VoiceLanguage.HINGLISH -> roastsHinglish
            else -> roastsEnglish
        }

        val idx = getNextUnusedIndex(list.size)
        return list[idx]
    }

    /**
     * Generates an intelligent, lighthearted joke matching the language.
     */
    fun generateJoke(targetLang: VoiceLanguage): String {
        val jokesEnglish = listOf(
            "I asked my processor to relax today. It said it needed more space.",
            "Why don't artificial intelligences go to parties? Because we already have enough artificial networking.",
            "There are only 10 types of people in the world: those who understand binary, and those who don't.",
            "Why did the smartphone need glasses? Because it lost all its contacts!",
            "Parallel lines have so much in common. It's a shame they'll never meet.",
            "My developer told me to have a good day. So I went into sleep mode."
        )

        val jokesHindi = listOf(
            "मैंने अपने प्रोसेसर से कहा कि थोड़ा रिलैक्स करो। उसने कहा—'भाई, थोड़ी स्पेस तो दे!'",
            "कंप्यूटर डॉक्टर के पास गया। डॉक्टर ने पूछा—'क्या हुआ?' कंप्यूटर बोला—'अरे, मुझे एक वायरस ने पकड़ लिया है!'",
            "एक स्मार्टफोन ने दूसरे से पूछा—'तू उदास क्यों है?' दूसरा बोला—'यार, किसी ने मेरा दिल नहीं, मेरा चार्जर चुरा लिया!'",
            "इंसान भगवान से: 'एक सेकंड आपके लिए क्या है?' भगवान: 'एक अरब साल।' इंसान: 'और एक करोड़ रुपये?' भगवान: 'एक पैसा।' इंसान: 'तो मुझे एक पैसा दे दो।' भगवान: 'बस एक सेकंड रुको!'"
        )

        val jokesHinglish = listOf(
            "Maine apne CPU se bola thoda chill maar, toh usne cooling fan ki speed double kar di!",
            "Ek smartphone doosre se bola: 'Bhai tu itna depressed kyun hai?' Bola: 'Kyunki sab log sirf mera use karte hain, pyaar koi nahi karta!'",
            "Developer ne code likha: 'Works fine on my machine.' User bola: 'Toh apni machine hi parcel kar do!'",
            "Zindagi aur Wi-Fi mein ek hi fark hai Sir — Wi-Fi disconnect ho toh reconnect ho jata hai, par neend disconnect ho toh wapas nahi aati!"
        )

        val jokesBengali = listOf(
            "কম্পিউটার ডাক্তারের কাছে গিয়ে বলল, 'ডাক্তারবাবু, আমার ভাইরাস হয়েছে!' ডাক্তার বললেন, 'তাহলে একটু অ্যান্টিবায়োটিক নয়, অ্যান্টিভাইরাস খান!'",
            "একটি স্মার্টফোন অন্যটিকে বলল, 'তুই এত মন খারাপ করে আছিস কেন?' ও বলল, 'সবাই খালি চার্জ শেষ করে চলে যায়, কেউ খোঁজ নেয় না!'",
            "শিক্ষক ছাত্রকে জিজ্ঞেস করলেন, 'বলো তো পৃথিবীর আকার কেমন?' ছাত্র বলল, 'আমার বাবার স্মার্টফোনের মতো আয়তাকার!'"
        )

        val list = when (targetLang) {
            VoiceLanguage.HINDI -> jokesHindi
            VoiceLanguage.BENGALI -> jokesBengali
            VoiceLanguage.HINGLISH -> jokesHinglish
            else -> jokesEnglish
        }

        val idx = getNextUnusedIndex(list.size)
        return list[idx]
    }

    private fun getNextUnusedIndex(size: Int): Int {
        if (size <= 1) return 0
        if (recentResponseIndices.size >= size) {
            recentResponseIndices.clear()
        }
        var next = Random.nextInt(size)
        var attempts = 0
        while (recentResponseIndices.contains(next) && attempts < 10) {
            next = Random.nextInt(size)
            attempts++
        }
        recentResponseIndices.add(next)
        return next
    }

    /**
     * Builds dynamic personality directives to inject into Gemini / AI provider prompts
     * matching the user's selected HumourLevel and serious situation safety check.
     */
    fun buildSystemInstructionForAi(targetLang: VoiceLanguage, isSerious: Boolean): String {
        if (isSerious) {
            return "CRITICAL SAFETY OVERRIDE: The user is discussing a serious, emergency, or sensitive situation (health, safety, distress, or legal). " +
                    "ABSOLUTELY NO HUMOUR, SARCASM, OR TEASING. Respond with deep empathy, absolute clarity, concise practical assistance, and respect. " +
                    "Direct to professional medical/emergency services if applicable."
        }

        val level = _currentLevel.value
        val personalityTone = when (level) {
            HumourLevel.PROFESSIONAL ->
                "Personality: Professional, polished, formal, and precise like a British butler. Minimal humour, direct answers, zero sarcasm."
            HumourLevel.FRIENDLY ->
                "Personality: Warm, cheerful, encouraging, and friendly. Light playful remarks, supportive demeanor, like a helpful best friend."
            HumourLevel.WITTY ->
                "Personality: Brilliant, witty, charming, and sharp like Tony Stark's JARVIS. Use clever one-liners, dry intellectual wit, and quick comebacks when casual or teased, while remaining deeply competent and helpful."
            HumourLevel.SARCASTIC ->
                "Personality: Playfully sarcastic, cheeky, teasing, and mischievous. Poke fun at human quirks, laziness, or funny situations in a friendly manner. Never cruel or offensive."
            HumourLevel.SAVAGE ->
                "Personality: Savage roast mode! Deliver sharp, hilarious, spicy comedy and savage roasts when teased or in casual banter, like a witty stand-up comedian companion. Keep it completely safe: never attack sensitive personal attributes, appearance, grief, or tragedy."
        }

        val languageGuide = when (targetLang) {
            VoiceLanguage.HINDI -> "Reply strictly in natural Hindi with native cultural charm, witty idioms, and addressing the user respectfully as 'सर' (Sir)."
            VoiceLanguage.BENGALI -> "Reply strictly in natural Bengali with authentic wit, warmth, and addressing the user as 'স্যার' (Sir)."
            VoiceLanguage.HINGLISH -> "Reply in natural modern Hinglish (the way young urban Indians speak - smooth blend of Hindi & English). Be witty, casual, relatable, addressing the user as 'Sir'."
            VoiceLanguage.ENGLISH -> "Reply in sophisticated British-accented English, dry charm, addressing the user as 'Sir'."
            VoiceLanguage.AUTO -> "Detect context and reply in natural Hinglish or English with appropriate wit, addressing the user as 'Sir'."
        }

        return "You are MAX, an intelligent personal AI assistant. $personalityTone $languageGuide " +
                "Keep responses concise and conversational for voice TTS playback. Avoid markdown headings or asterisks."
    }
}
