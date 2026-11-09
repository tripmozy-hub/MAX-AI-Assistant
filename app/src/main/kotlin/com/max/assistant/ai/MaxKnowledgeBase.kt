package com.max.assistant.ai

import com.max.assistant.tts.VoiceLanguage
import java.util.Locale

class MaxKnowledgeBase {

    companion object {
        private const val TAG = "MaxKnowledgeBase"
    }

    // Keep track of the last topic discussed for conversational context and follow-up queries
    var activeContextTopic: String? = null

    /**
     * Answers general knowledge, definitions, science, tech, history, and follow-up inquiries.
     * Returns null if no built-in knowledge matches.
     */
    fun queryKnowledge(input: String, language: VoiceLanguage, history: List<ChatMessage> = emptyList()): String? {
        val lower = input.lowercase(Locale.ROOT).trim()

        // 1. Check for context-sensitive follow-up questions (e.g. "when was he born?", "who is that?", "tell me more")
        val followUpAnswer = checkContextFollowUp(lower, language, history)
        if (followUpAnswer != null) {
            return followUpAnswer
        }

        // 2. Explanations & Summarization ("explain quantum computing simply", "what is photosynthesis", "explain AI")
        if (lower.contains("explain ") || lower.contains("samjhao") || lower.contains("ki hoy") || lower.contains("kya hai") || lower.contains("what is ")) {
            val explained = explainTopic(lower, language)
            if (explained != null) return explained
        }

        // 3. World Facts & General Knowledge
        // Capital cities
        if (lower.contains("capital of india") || lower.contains("bharat ki rajdhani") || lower.contains("bharater rajdhani")) {
            activeContextTopic = "New Delhi"
            return when (language) {
                VoiceLanguage.HINDI -> "सर, भारत की राजधानी नई दिल्ली है।"
                VoiceLanguage.BENGALI -> "স্যার, ভারতের রাজধানী হলো নতুন দিল্লি।"
                else -> "Sir, the capital of India is New Delhi."
            }
        }
        if (lower.contains("capital of usa") || lower.contains("capital of america") || lower.contains("america ki rajdhani")) {
            activeContextTopic = "Washington, D.C."
            return when (language) {
                VoiceLanguage.HINDI -> "सर, संयुक्त राज्य अमेरिका की राजधानी वाशिंगटन, डी.सी. है।"
                VoiceLanguage.BENGALI -> "স্যার, মার্কিন যুক্তরাষ্ট্রের রাজধানী ওয়াশিংটন, ডি.সি.।"
                else -> "Sir, the capital of the United States is Washington, D.C."
            }
        }
        if (lower.contains("capital of france") || lower.contains("france ki rajdhani")) {
            activeContextTopic = "Paris"
            return when (language) {
                VoiceLanguage.HINDI -> "सर, फ्रांस की राजधानी पेरिस है।"
                VoiceLanguage.BENGALI -> "স্যার, ফ্রান্সের রাজধানী প্যারিস।"
                else -> "Sir, the capital of France is Paris."
            }
        }

        // Science & Space
        if (lower.contains("speed of light") || lower.contains("prakash ki gati") || lower.contains("alor goti")) {
            activeContextTopic = "Speed of Light"
            return when (language) {
                VoiceLanguage.HINDI -> "सर, निर्वात में प्रकाश की गति लगभग २,९९,७९२ किलोमीटर प्रति सेकंड (लगभग ३ लाख किमी/सेकंड) होती है।"
                VoiceLanguage.BENGALI -> "স্যার, আলোর গতি প্রায় ২,৯৯,৭৯২ কিলোমিটার প্রতি সেকেন্ড।"
                else -> "Sir, the speed of light in a vacuum is approximately 299,792 kilometers per second, or roughly 186,282 miles per second."
            }
        }
        if (lower.contains("largest planet") || lower.contains("sabse bada grah") || lower.contains("shobcheye boro groho")) {
            activeContextTopic = "Jupiter"
            return when (language) {
                VoiceLanguage.HINDI -> "सर, हमारे सौरमंडल का सबसे बड़ा ग्रह बृहस्पति (Jupiter) है।"
                VoiceLanguage.BENGALI -> "স্যার, আমাদের সৌরজগতের বৃহত্তম গ্রহ হলো বৃহস্পতি (Jupiter)।"
                else -> "Sir, the largest planet in our solar system is Jupiter."
            }
        }
        if (lower.contains("closest planet to sun") || lower.contains("suraj ke sabse pass")) {
            activeContextTopic = "Mercury"
            return when (language) {
                VoiceLanguage.HINDI -> "सर, सूर्य के सबसे निकटतम ग्रह बुध (Mercury) है।"
                VoiceLanguage.BENGALI -> "স্যার, সূর্যের নিকটতম গ্রহ হলো বুধ।"
                else -> "Sir, Mercury is the closest planet to the Sun."
            }
        }
        if (lower.contains("highest mountain") || lower.contains("mount everest") || lower.contains("sabse unchi choti")) {
            activeContextTopic = "Mount Everest"
            return when (language) {
                VoiceLanguage.HINDI -> "सर, विश्व का सबसे ऊँचा पर्वत शिखर माउंट एवरेस्ट है, जिसकी ऊँचाई ८,८४८.८६ मीटर है।"
                VoiceLanguage.BENGALI -> "স্যার, পৃথিবীর সর্বোচ্চ পর্বতশৃঙ্গ হলো মাউন্ট এভারেস্ট, যার উচ্চতা ৮,৮৪৮.৮৬ মিটার।"
                else -> "Sir, Mount Everest is the highest mountain in the world, standing at 8,848.86 meters above sea level."
            }
        }

        // Historical & Influential Figures
        if (lower.contains("elon musk") || lower.contains("musk")) {
            activeContextTopic = "Elon Musk"
            return when (language) {
                VoiceLanguage.HINDI -> "सर, एलोन मस्क टेस्ला, स्पेसएक्स, न्यूरालिंक और एक्स (पूर्व ट्विटर) के संस्थापक और प्रमुख हैं।"
                VoiceLanguage.BENGALI -> "স্যার, ইলন মাস্ক হলেন টেসলা, স্পেসএক্স এবং নিউরালিংকের প্রধান কর্ণধার।"
                else -> "Sir, Elon Musk is the CEO and Chief Engineer of SpaceX, CEO and product architect of Tesla, and owner of X."
            }
        }
        if (lower.contains("steve jobs") || lower.contains("apple founder")) {
            activeContextTopic = "Steve Jobs"
            return when (language) {
                VoiceLanguage.HINDI -> "सर, स्टीव जॉब्स एप्पल इंक के सह-संस्थापक और पूर्व सीईओ थे, जिन्होंने आईफोन और मैक कंप्यूटर की शुरुआत की।"
                VoiceLanguage.BENGALI -> "স্যার, স্টিভ জবস অ্যাপলের সহ-প্রতিষ্ঠাতা ছিলেন।"
                else -> "Sir, Steve Jobs was the visionary co-founder and former CEO of Apple Inc."
            }
        }
        if (lower.contains("albert einstein") || lower.contains("einstein")) {
            activeContextTopic = "Albert Einstein"
            return when (language) {
                VoiceLanguage.HINDI -> "सर, अल्बर्ट आइंस्टीन महान भौतिक विज्ञानी थे जिन्होंने सापेक्षता के सिद्धांत (Theory of Relativity) और E = mc² का प्रतिपादन किया।"
                VoiceLanguage.BENGALI -> "স্যার, আলবার্ট আইনস্টাইন ছিলেন আপেক্ষিকতা তত্ত্বের উদ্ভাবক প্রখ্যাত পদার্থবিজ্ঞানী।"
                else -> "Sir, Albert Einstein was a renowned theoretical physicist best known for developing the theory of relativity."
            }
        }
        if (lower.contains("first president of india") || lower.contains("bharat ke pehle rashtrapati")) {
            activeContextTopic = "Dr. Rajendra Prasad"
            return when (language) {
                VoiceLanguage.HINDI -> "सर, स्वतंत्र भारत के पहले राष्ट्रपति डॉ. राजेंद्र प्रसाद थे, जिन्होंने १९५० से १९६२ तक सेवा की।"
                VoiceLanguage.BENGALI -> "স্যার, স্বাধীন ভারতের প্রথম রাষ্ট্রপতি ছিলেন ডঃ রাজেন্দ্র প্রসাদ।"
                else -> "Sir, Dr. Rajendra Prasad was the first President of independent India, serving from 1950 to 1962."
            }
        }

        return null
    }

    private fun checkContextFollowUp(lower: String, language: VoiceLanguage, history: List<ChatMessage>): String? {
        // If user says "when was he born?", "where is it?", "tell me more", etc.
        val lastTopic = activeContextTopic ?: history.takeLast(4).firstOrNull { it.role == MessageRole.MAX }?.text

        if (lower.contains("when was he born") || lower.contains("unka janam") || lower.contains("tar jonmo")) {
            if (activeContextTopic == "Albert Einstein" || lastTopic?.contains("आइंस्टीन") == true) {
                return when (language) {
                    VoiceLanguage.HINDI -> "सर, अल्बर्ट आइंस्टीन का जन्म १४ मार्च १८७९ को जर्मनी में हुआ था।"
                    VoiceLanguage.BENGALI -> "স্যার, আলবার্ট আইনস্টাইন ১৪ই মার্চ ১৮৭৯ সালে জন্মগ্রহণ করেন।"
                    else -> "Sir, Albert Einstein was born on March 14, 1879, in Germany."
                }
            }
            if (activeContextTopic == "Dr. Rajendra Prasad" || lastTopic?.contains("राजेंद्र प्रसाद") == true) {
                return when (language) {
                    VoiceLanguage.HINDI -> "सर, डॉ. राजेंद्र प्रसाद का जन्म ३ दिसंबर १८८४ को बिहार के जीरादेई में हुआ था।"
                    VoiceLanguage.BENGALI -> "স্যার, ডঃ রাজেন্দ্র প্রসাদ ৩রা ডিসেম্বর ১৮৮৪ সালে জন্মগ্রহণ করেছিলেন।"
                    else -> "Sir, Dr. Rajendra Prasad was born on December 3, 1884, in Bihar."
                }
            }
            if (activeContextTopic == "Steve Jobs" || lastTopic?.contains("स्टीव जॉब्स") == true) {
                return when (language) {
                    VoiceLanguage.HINDI -> "सर, स्टीव जॉब्स का जन्म २४ फरवरी १९५५ को कैलिफोर्निया में हुआ था।"
                    VoiceLanguage.BENGALI -> "স্যার, স্টিভ জবস ২৪শে ফেব্রুয়ারি ১৯৫৫ সালে জন্মগ্রহণ করেন।"
                    else -> "Sir, Steve Jobs was born on February 24, 1955, in San Francisco, California."
                }
            }
        }

        if (lower.contains("tell me more") || lower.contains("aur batao") || lower.contains("aro bolo")) {
            if (activeContextTopic != null) {
                return when (language) {
                    VoiceLanguage.HINDI -> "सर, $activeContextTopic के संबंध में आगे का विवरण यह है कि यह विज्ञान और प्रौद्योगिकी का एक अत्यंत महत्वपूर्ण आधार है।"
                    VoiceLanguage.BENGALI -> "স্যার, $activeContextTopic সম্পর্কে আরো বিস্তারিত তথ্য সংরক্ষিত রয়েছে।"
                    else -> "Sir, regarding $activeContextTopic, it remains one of the foundational subjects in modern science and innovation."
                }
            }
        }

        return null
    }

    private fun explainTopic(lower: String, language: VoiceLanguage): String? {
        if (lower.contains("quantum computing") || lower.contains("quantum")) {
            activeContextTopic = "Quantum Computing"
            return when (language) {
                VoiceLanguage.HINDI -> "सर, क्वांटम कंप्यूटिंग पारंपरिक बिट्स (० और १) के बजाय क्वांटम बिट्स (क्यूबिट्स) का उपयोग करती है, जिससे यह सुपरपोजिशन के कारण जटिल गणनाएँ पल भर में कर सकती है।"
                VoiceLanguage.BENGALI -> "স্যার, কোয়ান্টাম কম্পিউটিং কিউবিট ব্যবহার করে অত্যন্ত জটিল গাণিতিক সমস্যার দ্রুত সমাধান করতে পারে।"
                else -> "Sir, quantum computing utilizes qubits that can exist as both 0 and 1 simultaneously via superposition, allowing exponential computational speed."
            }
        }
        if (lower.contains("photosynthesis") || lower.contains("prakash sanshleshan")) {
            activeContextTopic = "Photosynthesis"
            return when (language) {
                VoiceLanguage.HINDI -> "सर, प्रकाश संश्लेषण वह प्रक्रिया है जिससे पौधे सूर्य के प्रकाश, जल और कार्बन डाइऑक्साइड का उपयोग करके ऑक्सीजन और ग्लूकोज बनाते हैं।"
                VoiceLanguage.BENGALI -> "স্যার, সালোকসংশ্লেষণ প্রক্রিয়ায় উদ্ভিদ সূর্যের আলো ও জলের সাহায্যে নিজের খাদ্য প্রস্তুত করে।"
                else -> "Sir, photosynthesis is the biological process by which green plants convert sunlight, water, and carbon dioxide into oxygen and chemical energy."
            }
        }
        if (lower.contains("artificial intelligence") || lower.contains(" ai ") || lower.endsWith(" ai") || lower.contains("एआई")) {
            activeContextTopic = "Artificial Intelligence"
            return when (language) {
                VoiceLanguage.HINDI -> "सर, आर्टिफिशियल इंटेलिजेंस मशीनों में मानव जैसी बुद्धिमत्ता, सीखने और निर्णय लेने की क्षमता का निर्माण है, ठीक वैसे ही जैसे मैं आपकी सहायता करता हूँ।"
                VoiceLanguage.BENGALI -> "স্যার, কৃত্রিম বুদ্ধিমত্তা হলো কম্পিউটারের মাধ্যমে মানুষের মতো চিন্তা ও সিদ্ধান্ত নেওয়ার প্রযুক্তি।"
                else -> "Sir, Artificial Intelligence is the capability of computing systems to perform cognitive tasks normally requiring human intelligence, such as perception and reasoning."
            }
        }
        return null
    }
}
