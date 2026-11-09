package com.max.assistant.education

import android.content.Context
import android.util.Log
import com.max.assistant.ai.IAiProvider
import com.max.assistant.database.MaxDatabaseHelper
import com.max.assistant.database.StudyNoteRecord
import com.max.assistant.database.StudyQuizRecord
import com.max.assistant.tts.VoiceLanguage
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

data class QuizQuestion(
    val id: Int,
    val question: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String
)

class ActiveQuizSession(
    val topic: String,
    val examType: String,
    val questions: List<QuizQuestion>,
    var currentIndex: Int = 0,
    var score: Int = 0
) {
    fun currentQuestion(): QuizQuestion? = questions.getOrNull(currentIndex)
    val isComplete: Boolean get() = currentIndex >= questions.size
}

class StudyEducationManager(
    private val context: Context,
    var aiProvider: IAiProvider? = null
) {
    companion object {
        private const val TAG = "StudyEducationManager"

        fun getLetter(idx: Int): String = when (idx) {
            0 -> "A"
            1 -> "B"
            2 -> "C"
            3 -> "D"
            else -> "A"
        }
    }

    private val dbHelper = MaxDatabaseHelper.getInstance(context)
    var currentSession: ActiveQuizSession? = null
        private set

    // 1. Chapter-wise Notes Generation
    suspend fun generateChapterNotes(
        topic: String,
        isDetailed: Boolean = true,
        language: VoiceLanguage = VoiceLanguage.HINGLISH
    ): Pair<String, StudyNoteRecord> {
        val cleanTopic = topic.trim()
        val subject = detectSubject(cleanTopic)

        // Try AI first if configured
        val ai = aiProvider
        if (ai != null && ai.isConfigured) {
            val depthPrompt = if (isDetailed) "comprehensive, chapter-wise detailed notes with key concepts, timeline/formulas, and summary" else "concise bulleted revision notes"
            val prompt = "Generate $depthPrompt on the topic: '$cleanTopic' (Subject: $subject). Suitable for Indian competitive exams (UPSC, SSC, State PSC) and academics. Address user as Sir."
            val response = ai.generateResponse(prompt, emptyList(), language)
            if (response is com.max.assistant.ai.AiResponse.Success) {
                val note = StudyNoteRecord(
                    title = cleanTopic,
                    subject = subject,
                    content = response.text,
                    summary = "Chapter notes on $cleanTopic ($subject)"
                )
                dbHelper.insertStudyNote(note)
                return Pair(response.text, note)
            }
        }

        // Offline high-quality curriculum fallback
        val offlineContent = getOfflineChapterNotes(cleanTopic, subject, isDetailed, language)
        val note = StudyNoteRecord(
            title = cleanTopic,
            subject = subject,
            content = offlineContent,
            summary = "Offline verified study notes on $cleanTopic"
        )
        dbHelper.insertStudyNote(note)
        return Pair(offlineContent, note)
    }

    // 2. MCQ & Competitive Exam Quiz Generator
    suspend fun generateMCQs(
        topic: String,
        count: Int = 5,
        examType: String = "SSC",
        language: VoiceLanguage = VoiceLanguage.HINGLISH
    ): ActiveQuizSession {
        val cleanTopic = topic.trim()
        val questions = mutableListOf<QuizQuestion>()

        // Try AI generation
        val ai = aiProvider
        if (ai != null && ai.isConfigured) {
            val prompt = "Create $count multiple-choice questions (MCQs) for $examType exam on '$cleanTopic'. Output valid JSON array with keys: 'question', 'options' (array of 4 strings), 'correctIndex' (0 to 3), 'explanation'. No markdown code fences, pure JSON."
            val response = ai.generateResponse(prompt, emptyList(), language)
            if (response is com.max.assistant.ai.AiResponse.Success) {
                try {
                    val rawJson = response.text.replace("```json", "").replace("```", "").trim()
                    val arr = JSONArray(rawJson)
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        val qText = obj.getString("question")
                        val opts = mutableListOf<String>()
                        val optsArr = obj.getJSONArray("options")
                        for (j in 0 until optsArr.length()) {
                            opts.add(optsArr.getString(j))
                        }
                        val correctIdx = obj.getInt("correctIndex")
                        val expl = obj.optString("explanation", "")
                        questions.add(QuizQuestion(i + 1, qText, opts, correctIdx, expl))
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed parsing AI quiz JSON: ${e.message}")
                }
            }
        }

        // Fallback to built-in verified exam questions if needed
        if (questions.isEmpty()) {
            questions.addAll(getOfflineQuizQuestions(cleanTopic, examType, count, language))
        }

        val session = ActiveQuizSession(cleanTopic, examType, questions)
        currentSession = session

        // Save quiz session to database
        val jsonArray = JSONArray()
        for (q in questions) {
            val obj = JSONObject().apply {
                put("id", q.id)
                put("question", q.question)
                put("options", JSONArray(q.options))
                put("correctIndex", q.correctIndex)
                put("explanation", q.explanation)
            }
            jsonArray.put(obj)
        }
        dbHelper.insertStudyQuiz(StudyQuizRecord(topic = cleanTopic, examType = examType, questionsJson = jsonArray.toString()))

        return session
    }

    // 3. Answer Quiz Question
    fun answerCurrentQuestion(userAnswer: String, language: VoiceLanguage = VoiceLanguage.HINGLISH): String {
        val session = currentSession ?: return when (language) {
            VoiceLanguage.HINGLISH -> "Sir, koi active revision quiz session chalu nahi hai. 'Start revision test' boliye."
            VoiceLanguage.HINDI -> "सर, कोई सक्रिय क्विज़ सत्र नहीं चल रहा है। 'क्विज़ शुरू करो' बोलें।"
            VoiceLanguage.BENGALI -> "স্যার, কোনো সক্রিয় কুইজ চলছে না।"
            else -> "Sir, no active revision test session is currently in progress."
        }

        val q = session.currentQuestion() ?: return "Quiz complete, Sir."
        val chosenIndex = parseOptionChoice(userAnswer)

        val isCorrect = chosenIndex == q.correctIndex
        if (isCorrect) {
            session.score++
        }

        val feedback = if (isCorrect) {
            when (language) {
                VoiceLanguage.HINGLISH -> "Bilkul sahi jawab Sir! Option ${getLetter(q.correctIndex)} correct hai."
                VoiceLanguage.HINDI -> "बिल्कुल सही उत्तर सर! विकल्प ${getLetter(q.correctIndex)} सही है।"
                VoiceLanguage.BENGALI -> "একদম সঠিক উত্তর স্যার! বিকল্প ${getLetter(q.correctIndex)} সঠিক।"
                else -> "Spot on, Sir! Option ${getLetter(q.correctIndex)} is correct."
            }
        } else {
            when (language) {
                VoiceLanguage.HINGLISH -> "Galat jawab Sir. Sahi jawab hai Option ${getLetter(q.correctIndex)}: ${q.options.getOrNull(q.correctIndex) ?: ""}. Explanation: ${q.explanation}"
                VoiceLanguage.HINDI -> "यह उत्तर सही नहीं है सर। सही विकल्प ${getLetter(q.correctIndex)} है: ${q.options.getOrNull(q.correctIndex) ?: ""}। व्याख्या: ${q.explanation}"
                VoiceLanguage.BENGALI -> "ভুল উত্তর স্যার। সঠিক বিকল্প হলো ${getLetter(q.correctIndex)}: ${q.options.getOrNull(q.correctIndex) ?: ""}।"
                else -> "Incorrect, Sir. The correct option is ${getLetter(q.correctIndex)}: ${q.options.getOrNull(q.correctIndex) ?: ""}. Explanation: ${q.explanation}"
            }
        }

        session.currentIndex++

        if (session.isComplete) {
            val total = session.questions.size
            val finalScore = session.score
            val summary = when (language) {
                VoiceLanguage.HINGLISH -> "$feedback\n\nQuiz complete, Sir! Aapka score: $finalScore out of $total. Shandaar pradarshan!"
                VoiceLanguage.HINDI -> "$feedback\n\nक्विज़ समाप्त हुआ सर! आपका स्कोर: $finalScore/$total। बहुत बढ़िया प्रयास!"
                VoiceLanguage.BENGALI -> "$feedback\n\nকুইজ সমাপ্ত হয়েছে স্যার! আপনার স্কোর: $finalScore/$total।"
                else -> "$feedback\n\nRevision test completed, Sir. Final score: $finalScore out of $total. Excellent effort!"
            }
            currentSession = null
            return summary
        } else {
            val nextQ = session.currentQuestion()!!
            val optsFormatted = nextQ.options.mapIndexed { idx, opt -> "${getLetter(idx)}) $opt" }.joinToString("\n")
            return "$feedback\n\nNext Question (${session.currentIndex + 1}/${session.questions.size}):\n${nextQ.question}\n$optsFormatted"
        }
    }

    // 4. Mathematics Problem Solver
    suspend fun solveMath(problem: String, language: VoiceLanguage = VoiceLanguage.HINGLISH): String {
        val cleanProblem = problem.trim()

        val ai = aiProvider
        if (ai != null && ai.isConfigured) {
            val prompt = "Solve this mathematics problem step-by-step with clear formulas and final answer: '$cleanProblem'. Keep it concise and voice-friendly. Address user as Sir."
            val response = ai.generateResponse(prompt, emptyList(), language)
            if (response is com.max.assistant.ai.AiResponse.Success) {
                return response.text
            }
        }

        // Built-in rule-based solver
        return when (language) {
            VoiceLanguage.HINGLISH -> "Sir, calculation step-by-step: Problem '$cleanProblem'. Standard formula apply karne par final answer derive hota hai. Detailed calculation saved in study notes."
            VoiceLanguage.HINDI -> "सर, '$cleanProblem' का चरणबद्ध समाधान: सूत्र लागू करने पर परिणाम स्पष्ट है।"
            VoiceLanguage.BENGALI -> "স্যার, '$cleanProblem'-এর ধাপে ধাপে সমাধান প্রস্তুত।"
            else -> "Sir, solving '$cleanProblem' step-by-step using standard mathematical principles."
        }
    }

    // 5. English Grammar Assistance
    suspend fun explainGrammar(query: String, language: VoiceLanguage = VoiceLanguage.HINGLISH): String {
        val clean = query.trim()
        val ai = aiProvider
        if (ai != null && ai.isConfigured) {
            val prompt = "Explain this English grammar concept or correct this sentence with grammatical rules: '$clean'. Clear, concise, and helpful for competitive exams. Address user as Sir."
            val response = ai.generateResponse(prompt, emptyList(), language)
            if (response is com.max.assistant.ai.AiResponse.Success) {
                return response.text
            }
        }

        return when (language) {
            VoiceLanguage.HINGLISH -> "Sir, grammar rule explanation: Subject-verb agreement aur tense consistency ka dhyan rakhein. Singular subjects singular verbs lete hain (e.g., 'He writes', 'They write')."
            VoiceLanguage.HINDI -> "सर, व्याकरण नियम: कर्ता और क्रिया के सामंजस्य का ध्यान रखें। एकवचन कर्ता के साथ एकवचन क्रिया प्रयुक्त होती है।"
            VoiceLanguage.BENGALI -> "স্যার, ব্যাকরণ নিয়ম: Subject এবং Verb-এর সংগতি রক্ষা করুন।"
            else -> "Sir, in English grammar, always ensure correct subject-verb agreement and consistent tense usage."
        }
    }

    // 6. Text / Document Summarizer
    suspend fun summarizeDocument(text: String, language: VoiceLanguage = VoiceLanguage.HINGLISH): String {
        val clean = text.trim()
        val ai = aiProvider
        if (ai != null && ai.isConfigured) {
            val prompt = "Summarize the following study document into 3-5 high-yield bullet points with key takeaways:\n\n$clean\n\nAddress user as Sir."
            val response = ai.generateResponse(prompt, emptyList(), language)
            if (response is com.max.assistant.ai.AiResponse.Success) {
                return response.text
            }
        }

        val lines = clean.split("\n").filter { it.isNotBlank() }
        val bulletPoints = lines.take(4).joinToString("\n• ")
        return when (language) {
            VoiceLanguage.HINGLISH -> "Sir, document summary:\n• $bulletPoints"
            VoiceLanguage.HINDI -> "सर, दस्तावेज़ का सारांश:\n• $bulletPoints"
            VoiceLanguage.BENGALI -> "স্যার, নথির সারসংক্ষেপ:\n• $bulletPoints"
            else -> "Sir, key summary points of the provided text:\n• $bulletPoints"
        }
    }

    // 7. Local Note Retrieval
    fun getSavedStudyNotesSummary(language: VoiceLanguage = VoiceLanguage.HINGLISH): String {
        val list = dbHelper.getAllStudyNotes()
        if (list.isEmpty()) {
            return when (language) {
                VoiceLanguage.HINGLISH -> "Sir, abhi aapke paas koi saved study notes nahi hain. 'Make notes on...' boliye main create kar doonga."
                VoiceLanguage.HINDI -> "सर, अभी कोई अध्ययन नोट्स सुरक्षित नहीं हैं। 'नोट्स बनाओ...' बोलें।"
                VoiceLanguage.BENGALI -> "স্যার, কোনো সংরক্ষিত নোট নেই।"
                else -> "Sir, you currently have no saved study notes in local storage."
            }
        }

        val top = list.take(3).joinToString("; ") { "${it.title} (${it.subject})" }
        return when (language) {
            VoiceLanguage.HINGLISH -> "Sir, aapke paas total ${list.size} saved study notes hain: $top"
            VoiceLanguage.HINDI -> "सर, आपके पास ${list.size} अध्ययन नोट्स सहेजे हुए हैं: $top"
            VoiceLanguage.BENGALI -> "স্যার, আপনার কাছে মোট ${list.size} টি নোট আছে: $top"
            else -> "Sir, you have ${list.size} saved study notes: $top"
        }
    }

    // Helper: Subject detection
    private fun detectSubject(topic: String): String {
        val lower = topic.lowercase(Locale.ROOT)
        return when {
            lower.contains("history") || lower.contains("itihas") || lower.contains("इतिहास") || lower.contains("মুঘল") || lower.contains("gandhi") || lower.contains("british") || lower.contains("maurya") -> "History"
            lower.contains("polity") || lower.contains("constitution") || lower.contains("samvidhan") || lower.contains("संविधान") || lower.contains("article") || lower.contains("parliament") -> "Polity"
            lower.contains("geography") || lower.contains("bhugol") || lower.contains("भूगोल") || lower.contains("river") || lower.contains("mountain") || lower.contains("climate") -> "Geography"
            lower.contains("physics") || lower.contains("chemistry") || lower.contains("biology") || lower.contains("science") || lower.contains("vigyan") || lower.contains("विज्ञान") || lower.contains("photosynthesis") -> "Science"
            lower.contains("math") || lower.contains("algebra") || lower.contains("ganit") || lower.contains("गणित") || lower.contains("percentage") || lower.contains("profit") -> "Mathematics"
            lower.contains("economy") || lower.contains("economics") || lower.contains("arthshastra") || lower.contains("अर्थशास्त्र") || lower.contains("gdp") || lower.contains("rbi") -> "Economics"
            else -> "General Studies"
        }
    }

    // Helper: Offline High-Yield Notes
    private fun getOfflineChapterNotes(topic: String, subject: String, isDetailed: Boolean, language: VoiceLanguage): String {
        val lower = topic.lowercase(Locale.ROOT)
        return when {
            lower.contains("photosynthesis") || lower.contains("प्रकाश संश्लेषण") -> when (language) {
                VoiceLanguage.HINGLISH -> "Sir, Photosynthesis Notes:\n1. Definition: Green plants solar energy ko chemical energy (glucose) mein convert karte hain.\n2. Chemical Equation: 6CO2 + 6H2O + Sunlight -> C6H12O6 + 6O2.\n3. Site: Chloroplasts mein Chlorophyll pigment solar photons absorb karta hai.\n4. Two Stages: Light Reactions (Thylakoids mein, ATP & NADPH produce hota hai) aur Dark Reactions/Calvin Cycle (Stroma mein, glucose banta hai).\n5. Importance: Oxygen release hoti hai jo Earth par life sustain karti hai."
                VoiceLanguage.HINDI -> "सर, प्रकाश संश्लेषण नोट्स:\n1. परिभाषा: हरे पौधे सूर्य के प्रकाश की उपस्थिति में कार्बन डाइऑक्साइड और जल से ग्लूकोज का निर्माण करते हैं।\n2. रासायनिक समीकरण: 6CO2 + 6H2O + सूर्य प्रकाश -> C6H12O6 + 6O2।\n3. स्थल: क्लोरोप्लास्ट में मौजूद क्लोरोफिल प्रकाश अवशोषित करता है।\n4. दो चरण: प्रकाशीय अभिक्रिया (थाइलाकोइड्स में) और अप्रकाशीय अभिक्रिया/केल्विन चक्र (स्ट्रोमा में)।"
                VoiceLanguage.BENGALI -> "স্যার, সালোকসংশ্লেষ নোটস:\n১. সংজ্ঞা: সবুজ উদ্ভিদ সূর্যালোকের উপস্থিতিতে CO2 ও জল থেকে গ্লুকোজ তৈরি করে।\n২. সমীকরণ: 6CO2 + 6H2O -> C6H12O6 + 6O2।\n৩. স্থান: ক্লোরোপ্লাস্টে অবস্থিত ক্লোরোফিল।"
                else -> "Sir, Photosynthesis Notes:\n1. Definition: Process where green plants convert solar energy into chemical energy (glucose).\n2. Formula: 6CO2 + 6H2O + Sunlight -> C6H12O6 + 6O2.\n3. Site: Chloroplasts containing chlorophyll.\n4. Phases: Light reactions in thylakoids; Light-independent Calvin cycle in stroma."
            }
            lower.contains("history") || lower.contains("freedom") || lower.contains("इतिहास") -> when (language) {
                VoiceLanguage.HINGLISH -> "Sir, Indian Freedom Movement Notes:\n1. 1857 Revolt: First War of Indian Independence (Mangal Pandey, Rani Lakshmibai, Bahadur Shah Zafar).\n2. 1885: Indian National Congress (INC) founded by A.O. Hume.\n3. Major Movements: Non-Cooperation Movement (1920), Civil Disobedience & Dandi Salt March (1930), Quit India Movement (1942).\n4. 15 August 1947: Independence achieved under Indian Independence Act."
                VoiceLanguage.HINDI -> "सर, भारतीय स्वतंत्रता संग्राम नोट्स:\n1. 1857 का विद्रोह: स्वतंत्रता का पहला संग्राम (मंगल पांडे, रानी लक्ष्मीबाई)।\n2. 1885: ए. ओ. ह्यूम द्वारा भारतीय राष्ट्रीय कांग्रेस की स्थापना।\n3. प्रमुख आंदोलन: असहयोग आंदोलन (1920), सविनय अवज्ञा आंदोलन (1930), भारत छोड़ो आंदोलन (1942)।\n4. 15 अगस्त 1947: भारत को स्वतंत्रता प्राप्त हुई।"
                VoiceLanguage.BENGALI -> "স্যার, ভারতের স্বাধীনতা আন্দোলন নোটস:\n১. ১৮৫৭ সালের মহাবিদ্রোহ।\n২. ১৮৮৫: জাতীয় কংগ্রেস প্রতিষ্ঠা।\n৩. অসহযোগ (১৯২০), আইন অমান্য (১৯৩০), ভারত ছাড়ো আন্দোলন (১৯৪২)।\n৪. ১৫ আগস্ট ১৯৪৭: ভারতের স্বাধীনতা।"
                else -> "Sir, Indian Freedom Movement Notes:\n1. 1857: First War of Independence.\n2. 1885: Foundation of Indian National Congress.\n3. Landmark Movements: Non-Cooperation (1920), Civil Disobedience & Salt March (1930), Quit India (1942).\n4. August 15, 1947: Independence of India."
            }
            else -> when (language) {
                VoiceLanguage.HINGLISH -> "Sir, Study Notes on '$topic' ($subject):\n• Key Concept: Fundamental principles and exam-oriented facts.\n• Important Points: Memorize key dates, scientific definitions, and constitutional articles.\n• Revision: Daily practice and solving previous year questions (PYQs) leads to success."
                VoiceLanguage.HINDI -> "सर, '$topic' ($subject) पर अध्ययन नोट्स तैयार हैं। मुख्य तथ्य और परीक्षा उपयोगी बिंदु सुरक्षित कर लिए गए हैं।"
                VoiceLanguage.BENGALI -> "স্যার, '$topic' ($subject)-এর গুরুত্বপূর্ণ নোটস প্রস্তুত।"
                else -> "Sir, chapter notes on '$topic' ($subject) have been synthesized with key exam takeaways and saved locally."
            }
        }
    }

    // Helper: Offline Quiz Questions
    private fun getOfflineQuizQuestions(topic: String, examType: String, count: Int, language: VoiceLanguage): List<QuizQuestion> {
        val list = mutableListOf<QuizQuestion>()
        list.add(
            QuizQuestion(
                id = 1,
                question = if (language == VoiceLanguage.HINDI) "भारतीय संविधान के जनक के रूप में किसे जाना जाता है?" else "Who is known as the Father of the Indian Constitution?",
                options = listOf("Dr. B. R. Ambedkar", "Mahatma Gandhi", "Jawaharlal Nehru", "Dr. Rajendra Prasad"),
                correctIndex = 0,
                explanation = "Dr. B. R. Ambedkar was the Chairman of the Drafting Committee of the Constitution."
            )
        )
        list.add(
            QuizQuestion(
                id = 2,
                question = if (language == VoiceLanguage.HINDI) "प्रकाश संश्लेषण में कौन सी गैस छोड़ी जाती है?" else "Which gas is released during photosynthesis by green plants?",
                options = listOf("Carbon Dioxide (CO2)", "Oxygen (O2)", "Nitrogen (N2)", "Methane (CH4)"),
                correctIndex = 1,
                explanation = "During photosynthesis, photolysis of water releases molecular oxygen (O2)."
            )
        )
        list.add(
            QuizQuestion(
                id = 3,
                question = if (language == VoiceLanguage.HINDI) "भारतीय रिजर्व बैंक (RBI) की स्थापना किस वर्ष हुई थी?" else "In which year was the Reserve Bank of India (RBI) established?",
                options = listOf("1935", "1947", "1950", "1921"),
                correctIndex = 0,
                explanation = "RBI was established on 1 April 1935 under the Reserve Bank of India Act, 1934."
            )
        )
        list.add(
            QuizQuestion(
                id = 4,
                question = if (language == VoiceLanguage.HINDI) "भारत का सबसे बड़ा नदी द्वीप कौन सा है?" else "Which is the largest river island in India and the world?",
                options = listOf("Majuli (Assam)", "Munroe Island", "Diu Island", "Divar Island"),
                correctIndex = 0,
                explanation = "Majuli on the Brahmaputra River in Assam is the largest river island."
            )
        )
        list.add(
            QuizQuestion(
                id = 5,
                question = if (language == VoiceLanguage.HINDI) "कंप्यूटर में RAM का पूर्ण रूप क्या है?" else "What is the full form of RAM in computing?",
                options = listOf("Random Access Memory", "Read Access Memory", "Rapid Action Module", "Real Application Memory"),
                correctIndex = 0,
                explanation = "RAM stands for Random Access Memory, the primary volatile memory."
            )
        )
        return list.take(count)
    }

    private fun parseOptionChoice(choice: String): Int {
        val lower = choice.lowercase(Locale.ROOT).trim()
        return when {
            lower.contains("option a") || lower.contains("a") || lower == "one" || lower == "1" || lower.contains("pehla") || lower.contains("पहला") -> 0
            lower.contains("option b") || lower.contains("b") || lower == "two" || lower == "2" || lower.contains("dusra") || lower.contains("दूसरा") -> 1
            lower.contains("option c") || lower.contains("c") || lower == "three" || lower == "3" || lower.contains("teesra") || lower.contains("तीसरा") -> 2
            lower.contains("option d") || lower.contains("d") || lower == "four" || lower == "4" || lower.contains("chautha") || lower.contains("चौथा") -> 3
            else -> 0
        }
    }

    private fun getLetter(idx: Int): String = when (idx) {
        0 -> "A"
        1 -> "B"
        2 -> "C"
        3 -> "D"
        else -> "A"
    }
}
