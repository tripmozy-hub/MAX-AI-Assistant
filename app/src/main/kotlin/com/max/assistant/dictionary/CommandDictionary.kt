package com.max.assistant.dictionary

import android.content.Context
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * MAX MASTER VOICE COMMAND DICTIONARY - Version 1.0
 * Natural-language command recognition and execution registry.
 * Maps English, Hindi/Hinglish, and Bengali utterances to verified intent IDs.
 */
object CommandDictionary {

    private const val TAG = "CommandDictionary"
    private const val CUSTOM_COMMANDS_FILE = "max_custom_commands.json"

    private val builtinDefinitions = mutableListOf<CommandDefinition>()
    private val dynamicDefinitions = mutableListOf<CommandDefinition>()

    init {
        registerBuiltinCommands()
    }

    fun getAllDefinitions(): List<CommandDefinition> {
        return builtinDefinitions + dynamicDefinitions
    }

    fun findDefinitionById(intentId: String): CommandDefinition? {
        return getAllDefinitions().firstOrNull { it.intentId.equals(intentId, ignoreCase = true) }
    }

    fun registerDynamicCommand(def: CommandDefinition) {
        dynamicDefinitions.removeAll { it.intentId.equals(def.intentId, ignoreCase = true) }
        dynamicDefinitions.add(def)
    }

    fun loadCustomCommandsFromJson(context: Context) {
        try {
            val file = File(context.filesDir, CUSTOM_COMMANDS_FILE)
            if (!file.exists()) return
            val jsonStr = file.readText()
            val arr = JSONArray(jsonStr)
            dynamicDefinitions.clear()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val def = parseCommandFromJson(obj)
                if (def != null) {
                    dynamicDefinitions.add(def)
                }
            }
            Log.i(TAG, "Loaded ${dynamicDefinitions.size} custom commands from JSON.")
        } catch (e: Exception) {
            Log.w(TAG, "Error loading custom commands: ${e.message}")
        }
    }

    fun saveCustomCommandsToJson(context: Context) {
        try {
            val file = File(context.filesDir, CUSTOM_COMMANDS_FILE)
            val arr = JSONArray()
            for (d in dynamicDefinitions) {
                val obj = JSONObject().apply {
                    put("intentId", d.intentId)
                    put("category", d.category.name)
                    put("englishPhrases", JSONArray(d.englishPhrases))
                    put("hindiPhrases", JSONArray(d.hindiPhrases))
                    put("bengaliPhrases", JSONArray(d.bengaliPhrases))
                    put("requiredParameters", JSONArray(d.requiredParameters))
                    put("requiredPermissions", JSONArray(d.requiredPermissions))
                    put("confirmationRequired", d.confirmationRequired)
                    put("description", d.description)
                }
                arr.put(obj)
            }
            file.writeText(arr.toString(2))
        } catch (e: Exception) {
            Log.w(TAG, "Error saving custom commands: ${e.message}")
        }
    }

    private fun parseCommandFromJson(obj: JSONObject): CommandDefinition? {
        return try {
            val intentId = obj.getString("intentId")
            val catStr = obj.optString("category", CommandCategory.GENERAL_AI.name)
            val cat = try { CommandCategory.valueOf(catStr) } catch (_: Exception) { CommandCategory.GENERAL_AI }
            val en = jsonArrayToList(obj.optJSONArray("englishPhrases"))
            val hi = jsonArrayToList(obj.optJSONArray("hindiPhrases"))
            val bn = jsonArrayToList(obj.optJSONArray("bengaliPhrases"))
            val params = jsonArrayToList(obj.optJSONArray("requiredParameters"))
            val perms = jsonArrayToList(obj.optJSONArray("requiredPermissions"))
            val conf = obj.optBoolean("confirmationRequired", false)
            val desc = obj.optString("description", "")
            CommandDefinition(intentId, cat, en, hi, bn, params, perms, conf, desc)
        } catch (_: Exception) {
            null
        }
    }

    private fun jsonArrayToList(arr: JSONArray?): List<String> {
        if (arr == null) return emptyList()
        val list = mutableListOf<String>()
        for (i in 0 until arr.length()) {
            list.add(arr.getString(i))
        }
        return list
    }

    private fun registerBuiltinCommands() {
        builtinDefinitions.clear()

        // ==================== CATEGORY 1 – GENERAL AI CONVERSATION ====================
        builtinDefinitions.add(
            CommandDefinition(
                intentId = "AI001",
                category = CommandCategory.GENERAL_AI,
                englishPhrases = listOf("hey max", "hello max", "hi max", "max", "wake up max"),
                hindiPhrases = listOf("hey max", "नमस्ते मैक्स", "मैक्स सुनो", "मैक्स", "सुनो मैक्स"),
                bengaliPhrases = listOf("হেই ম্যাক্স", "ম্যাক্স", "হ্যালো ম্যাক্স", "ম্যাক্স শোনো", "শোনো ম্যাক্স"),
                description = "Activate assistant"
            )
        )
        builtinDefinitions.add(
            CommandDefinition(
                intentId = "AI002",
                category = CommandCategory.GENERAL_AI,
                englishPhrases = listOf("how are you", "how are you max", "how do you do"),
                hindiPhrases = listOf("kaise ho max", "kaise ho", "kya haal hai", "aap kaise hain"),
                bengaliPhrases = listOf("কেমন আছো ম্যাক্স", "কেমন আছো", "কেমন আছেন"),
                description = "Conversational response"
            )
        )
        builtinDefinitions.add(
            CommandDefinition(
                intentId = "AI003",
                category = CommandCategory.GENERAL_AI,
                englishPhrases = listOf("tell me something interesting", "give me an interesting fact", "tell me a fact", "interesting fact"),
                hindiPhrases = listOf("kuch interesting batao", "kuch dilchasp batao", "koi fact batao", "kuch naya batao"),
                bengaliPhrases = listOf("মজার কিছু বলো", "একটি মজার তথ্য বলো", "মজার ফ্যাক্ট বলো"),
                description = "Generate interesting fact"
            )
        )
        builtinDefinitions.add(
            CommandDefinition(
                intentId = "AI004",
                category = CommandCategory.GENERAL_AI,
                englishPhrases = listOf("what can you do", "what are your features", "what are your capabilities", "tell me your features"),
                hindiPhrases = listOf("tum kya kya kar sakte ho", "tum kya kar sakte ho", "apne features batao", "tumhari capabilities kya hain"),
                bengaliPhrases = listOf("তুমি কী কী করতে পারো", "তুমি কি করতে পারো", "তোমার কাজ কি"),
                description = "Explain capabilities"
            )
        )
        builtinDefinitions.add(
            CommandDefinition(
                intentId = "AI005",
                category = CommandCategory.GENERAL_AI,
                englishPhrases = listOf("help me", "i need help", "assist me", "can you help me"),
                hindiPhrases = listOf("meri help karo", "meri madad karo", "madad chahiye", "help chahiye"),
                bengaliPhrases = listOf("আমাকে সাহায্য করো", "সাহায্য চাই", "আমার সাহায্য দরকার"),
                description = "Ask how to help"
            )
        )
        builtinDefinitions.add(
            CommandDefinition(
                intentId = "AI006",
                category = CommandCategory.GENERAL_AI,
                englishPhrases = listOf("explain this simply", "simplify this", "explain in simple terms", "explain like i am five"),
                hindiPhrases = listOf("aasan bhasha mein samjhao", "saral bhasha mein batao", "aasan shabdo mein samjhao", "aasan karke batao"),
                bengaliPhrases = listOf("সহজ ভাষায় বোঝাও", "সহজ করে বলো", "সহজভাবে বুঝিয়ে দাও"),
                description = "Simplify explanation"
            )
        )

        // ==================== CATEGORY 2 – INDIAN GENERAL KNOWLEDGE ====================
        builtinDefinitions.add(
            CommandDefinition(
                intentId = "GK001",
                category = CommandCategory.INDIAN_GK,
                englishPhrases = listOf("who is the president of india", "president of india", "current president of india"),
                hindiPhrases = listOf("bharat ke rashtrapati kaun hain", "bharat ke vartaman rashtrapati", "rashtrapati kaun hai"),
                bengaliPhrases = listOf("ভারতের রাষ্ট্রপতি কে", "ভারতের বর্তমান রাষ্ট্রপতি কে"),
                description = "Answer with current President of India information"
            )
        )
        builtinDefinitions.add(
            CommandDefinition(
                intentId = "GK002",
                category = CommandCategory.INDIAN_GK,
                englishPhrases = listOf("what is the capital of india", "capital of india", "india capital"),
                hindiPhrases = listOf("bharat ki rajdhani kya hai", "bharat ki rajdhani kaun si hai", "bharat ki rajdhani"),
                bengaliPhrases = listOf("ভারতের রাজধানী কী", "ভারতের রাজধানী কোথায়"),
                description = "Answer factual question: Capital of India"
            )
        )
        builtinDefinitions.add(
            CommandDefinition(
                intentId = "GK003",
                category = CommandCategory.INDIAN_GK,
                englishPhrases = listOf("how many states are in india", "number of states in india", "states in india"),
                hindiPhrases = listOf("bharat mein kitne rajya hain", "bharat ke kul rajya", "kitne state hain"),
                bengaliPhrases = listOf("ভারতে কয়টি রাজ্য আছে", "ভারতের রাজ্য সংখ্যা কত"),
                description = "Answer factual question: 28 States and 8 UTs"
            )
        )
        builtinDefinitions.add(
            CommandDefinition(
                intentId = "GK004",
                category = CommandCategory.INDIAN_GK,
                englishPhrases = listOf("when did india become independent", "india independence year", "when did india get freedom"),
                hindiPhrases = listOf("bharat kab azaad hua", "bharat ko azaadi kab mili", "bharat kab swatantra hua"),
                bengaliPhrases = listOf("ভারত কবে স্বাধীন হয়", "ভারতের স্বাধীনতা দিবস কবে"),
                description = "Answer historical question: August 15, 1947"
            )
        )
        builtinDefinitions.add(
            CommandDefinition(
                intentId = "GK005",
                category = CommandCategory.INDIAN_GK,
                englishPhrases = listOf("tell me about the indian constitution", "indian constitution", "explain indian constitution"),
                hindiPhrases = listOf("bharatiya samvidhan ke baare mein batao", "bharat ka samvidhan samjhao", "samvidhan ke baare mein"),
                bengaliPhrases = listOf("ভারতীয় সংবিধান সম্পর্কে বলো", "ভারতের সংবিধান ব্যাখ্যা করো"),
                description = "Explain Indian Constitution"
            )
        )
        builtinDefinitions.add(
            CommandDefinition(
                intentId = "GK006",
                category = CommandCategory.INDIAN_GK,
                englishPhrases = listOf("tell me today's current affairs", "todays current affairs", "latest current affairs", "current affairs"),
                hindiPhrases = listOf("aaj ke current affairs batao", "current affairs batao", "aaj ki taja khabar current affairs"),
                bengaliPhrases = listOf("আজকের কারেন্ট অ্যাফেয়ার্স বলো", "সাম্প্রতিক ঘটনাবলী বলো"),
                description = "Search latest current affairs"
            )
        )

        // ==================== CATEGORY 3 – STUDY AND EDUCATION ====================
        builtinDefinitions.add(
            CommandDefinition(
                intentId = "EDU001",
                category = CommandCategory.EDUCATION,
                englishPhrases = listOf("teach me mathematics", "teach me maths", "maths learning mode", "math shikhao"),
                hindiPhrases = listOf("mujhe maths sikhao", "mujhe ganit sikhao", "math padhao", "ganit padhao"),
                bengaliPhrases = listOf("আমাকে অঙ্ক শেখাও", "আমাকে গণিত শেখাও", "অঙ্ক পড়াও"),
                description = "Enter mathematics learning mode"
            )
        )
        builtinDefinitions.add(
            CommandDefinition(
                intentId = "EDU002",
                category = CommandCategory.EDUCATION,
                englishPhrases = listOf("explain photosynthesis", "what is photosynthesis", "photosynthesis process"),
                hindiPhrases = listOf("photosynthesis samjhao", "prakash sanshleshan samjhao", "photosynthesis kya hai"),
                bengaliPhrases = listOf("সালোকসংশ্লেষণ বোঝাও", "সালোকসংশ্লেষণ কী", "সালোকসংশ্লেষণ প্রক্রিয়া"),
                description = "Explain science topic: Photosynthesis"
            )
        )
        builtinDefinitions.add(
            CommandDefinition(
                intentId = "EDU003",
                category = CommandCategory.EDUCATION,
                englishPhrases = listOf("make notes on history", "create notes on history", "history notes"),
                hindiPhrases = listOf("history ke notes banao", "itihas ke notes banao", "history pe notes"),
                bengaliPhrases = listOf("ইতিহাসের নোট তৈরি করো", "ইতিহাসের নোটস দাও"),
                requiredParameters = listOf("topic"),
                description = "Generate study notes"
            )
        )
        builtinDefinitions.add(
            CommandDefinition(
                intentId = "EDU004",
                category = CommandCategory.EDUCATION,
                englishPhrases = listOf("give me important questions", "important questions", "exam questions"),
                hindiPhrases = listOf("important questions do", "zaroori prashna do", "pariksha ke important sawal"),
                bengaliPhrases = listOf("গুরুত্বপূর্ণ প্রশ্ন দাও", "পরীক্ষার প্রশ্ন দাও"),
                description = "Generate important exam questions"
            )
        )
        builtinDefinitions.add(
            CommandDefinition(
                intentId = "EDU005",
                category = CommandCategory.EDUCATION,
                englishPhrases = listOf("start my study quiz", "start revision test", "start quiz", "begin quiz"),
                hindiPhrases = listOf("mera study quiz shuru karo", "quiz shuru karo", "revision test shuru karo", "test start karo"),
                bengaliPhrases = listOf("আমার পড়ার কুইজ শুরু করো", "কুইজ শুরু করো", "টেস্ট শুরু করো"),
                description = "Start interactive quiz"
            )
        )
        builtinDefinitions.add(
            CommandDefinition(
                intentId = "EDU006",
                category = CommandCategory.EDUCATION,
                englishPhrases = listOf("explain this chapter", "explain chapter", "teach this chapter"),
                hindiPhrases = listOf("ye chapter samjhao", "is chapter ko samjhao", "chapter padhao"),
                bengaliPhrases = listOf("এই অধ্যায়টি বোঝাও", "অধ্যায়টি বুঝিয়ে দাও"),
                requiredParameters = listOf("chapter"),
                description = "Explain chapter"
            )
        )
        builtinDefinitions.add(
            CommandDefinition(
                intentId = "EDU007",
                category = CommandCategory.EDUCATION,
                englishPhrases = listOf("give me 20 mcqs", "create 20 mcqs", "20 mcq questions", "generate mcqs"),
                hindiPhrases = listOf("20 mcq do", "20 objective prashna do", "20 mcq sawal banao"),
                bengaliPhrases = listOf("২০টি MCQ দাও", "২০টি বহু নির্বাচনী প্রশ্ন দাও"),
                description = "Generate 20 MCQs"
            )
        )
        builtinDefinitions.add(
            CommandDefinition(
                intentId = "EDU008",
                category = CommandCategory.EDUCATION,
                englishPhrases = listOf("check my answer", "evaluate my answer", "is this answer correct", "verify my answer"),
                hindiPhrases = listOf("mera answer check karo", "mera uttar check karo", "kya mera answer sahi hai"),
                bengaliPhrases = listOf("আমার উত্তর যাচাই করো", "আমার উত্তর ঠিক আছে কি"),
                description = "Evaluate answer"
            )
        )

        // ==================== CATEGORY 4 – NOTES AND DOCUMENTS ====================
        builtinDefinitions.add(
            CommandDefinition(
                intentId = "NOTE001",
                category = CommandCategory.NOTES,
                englishPhrases = listOf("create a new note", "new note", "take a note", "write a note"),
                hindiPhrases = listOf("naya note banao", "ek naya note likho", "note likho"),
                bengaliPhrases = listOf("নতুন নোট তৈরি করো", "নোট লেখো", "একটি নতুন নোট তৈরি করো"),
                requiredParameters = listOf("content"),
                description = "Create local note"
            )
        )
        builtinDefinitions.add(
            CommandDefinition(
                intentId = "NOTE002",
                category = CommandCategory.NOTES,
                englishPhrases = listOf("save this note", "save note", "save current note"),
                hindiPhrases = listOf("is note ko save karo", "note save karo", "ye note bacha ke rakho"),
                bengaliPhrases = listOf("এই নোটটি সেভ করো", "নোট সংরক্ষণ করো"),
                description = "Save current note"
            )
        )
        builtinDefinitions.add(
            CommandDefinition(
                intentId = "NOTE003",
                category = CommandCategory.NOTES,
                englishPhrases = listOf("read my notes", "show my notes", "view my notes", "list notes"),
                hindiPhrases = listOf("mere notes padho", "mere notes dikhao", "notes padh ke sunao"),
                bengaliPhrases = listOf("আমার নোটগুলো পড়ো", "আমার নোট দেখাও"),
                description = "List and read saved notes"
            )
        )
        builtinDefinitions.add(
            CommandDefinition(
                intentId = "NOTE004",
                category = CommandCategory.NOTES,
                englishPhrases = listOf("find my history notes", "search notes", "find note"),
                hindiPhrases = listOf("mere history notes dhundo", "history ke notes dhundo", "note khojo"),
                bengaliPhrases = listOf("আমার ইতিহাসের নোট খোঁজো", "নোট খোঁজো"),
                requiredParameters = listOf("query"),
                description = "Search notes"
            )
        )
        builtinDefinitions.add(
            CommandDefinition(
                intentId = "NOTE005",
                category = CommandCategory.NOTES,
                englishPhrases = listOf("delete this note", "delete note", "remove note"),
                hindiPhrases = listOf("ye note delete karo", "note delete karo", "note mitao"),
                bengaliPhrases = listOf("এই নোটটি মুছে দাও", "নোট মুছে ফেলো"),
                confirmationRequired = true,
                description = "Confirm and delete note"
            )
        )
        builtinDefinitions.add(
            CommandDefinition(
                intentId = "NOTE006",
                category = CommandCategory.NOTES,
                englishPhrases = listOf("summarize this document", "document summary", "summarize pdf", "summarize text"),
                hindiPhrases = listOf("is document ka summary banao", "document ka saraansh banao", "is document ko summarize karo"),
                bengaliPhrases = listOf("এই ডকুমেন্টের সারাংশ তৈরি করো", "ডকুমেন্ট সংক্ষেপ করো"),
                description = "Summarize document"
            )
        )

        // ==================== CATEGORY 5 – REMINDERS AND TASKS ====================
        builtinDefinitions.add(
            CommandDefinition(
                intentId = "REM001",
                category = CommandCategory.REMINDERS,
                englishPhrases = listOf("remind me tomorrow at 7 am", "set reminder tomorrow at 7", "remind me at 7 am tomorrow"),
                hindiPhrases = listOf("kal subah 7 baje yaad dilana", "kal 7 baje reminder lagao", "subah saat baje yaad dilana"),
                bengaliPhrases = listOf("কাল সকাল ৭টায় মনে করিয়ে দিও", "কাল ৭টায় রিমাইন্ডার দাও"),
                requiredParameters = listOf("time"),
                description = "Schedule reminder"
            )
        )
        builtinDefinitions.add(
            CommandDefinition(
                intentId = "REM002",
                category = CommandCategory.REMINDERS,
                englishPhrases = listOf("remind me to call rahul", "reminder to call rahul", "remind to call"),
                hindiPhrases = listOf("rahul ko call karna yaad dilana", "rahul ko call karne ka reminder lagao"),
                bengaliPhrases = listOf("রাহুলকে ফোন করতে মনে করিয়ে দিও", "রাহুলকে কল করার রিমাইন্ডার দাও"),
                requiredParameters = listOf("task"),
                description = "Create reminder with task"
            )
        )
        builtinDefinitions.add(
            CommandDefinition(
                intentId = "REM003",
                category = CommandCategory.REMINDERS,
                englishPhrases = listOf("what are my reminders", "show my reminders", "list reminders", "upcoming reminders"),
                hindiPhrases = listOf("mere reminders kya hain", "mere reminders dikhao", "kya kya reminder hai"),
                bengaliPhrases = listOf("আমার রিমাইন্ডারগুলো কী কী", "আমার রিমাইন্ডার দেখাও"),
                description = "Read reminders"
            )
        )
        builtinDefinitions.add(
            CommandDefinition(
                intentId = "REM004",
                category = CommandCategory.REMINDERS,
                englishPhrases = listOf("delete my reminder", "delete reminder", "cancel reminder"),
                hindiPhrases = listOf("mera reminder delete karo", "reminder hatao", "reminder cancel karo"),
                bengaliPhrases = listOf("আমার রিমাইন্ডার মুছে দাও", "রিমাইন্ডার বাতিল করো"),
                confirmationRequired = true,
                description = "Identify and delete reminder"
            )
        )
        builtinDefinitions.add(
            CommandDefinition(
                intentId = "REM005",
                category = CommandCategory.REMINDERS,
                englishPhrases = listOf("remind me every sunday", "weekly reminder every sunday", "repeat every sunday"),
                hindiPhrases = listOf("har sunday yaad dilana", "har ravivar yaad dilana", "har sunday ka reminder lagao"),
                bengaliPhrases = listOf("প্রতি রবিবার মনে করিয়ে দিও", "প্রতি রবিবারের রিমাইন্ডার দাও"),
                requiredParameters = listOf("repeat"),
                description = "Create recurring reminder"
            )
        )
        builtinDefinitions.add(
            CommandDefinition(
                intentId = "REM006",
                category = CommandCategory.REMINDERS,
                englishPhrases = listOf("snooze this reminder", "snooze reminder", "remind me later"),
                hindiPhrases = listOf("is reminder ko baad ke liye rakho", "reminder snooze karo", "baad mein yaad dilana"),
                bengaliPhrases = listOf("এই রিমাইন্ডারটি পরে মনে করিও", "রিমাইন্ডার স্নুজ করো"),
                description = "Snooze reminder"
            )
        )

        // ==================== CATEGORY 6 – ALARM, CLOCK AND CALENDAR ====================
        builtinDefinitions.add(
            CommandDefinition(
                intentId = "CLK001",
                category = CommandCategory.CLOCK_CALENDAR,
                englishPhrases = listOf("set an alarm for 6 am", "set alarm at 6 am", "alarm 6 am"),
                hindiPhrases = listOf("subah 6 baje alarm lagao", "6 baje ka alarm lagao", "che baje alarm lagao"),
                bengaliPhrases = listOf("সকাল ৬টায় অ্যালার্ম দাও", "৬টায় অ্যালার্ম লাগাও"),
                requiredParameters = listOf("time"),
                description = "Create alarm"
            )
        )
        builtinDefinitions.add(
            CommandDefinition(
                intentId = "CLK002",
                category = CommandCategory.CLOCK_CALENDAR,
                englishPhrases = listOf("stop the alarm", "turn off alarm", "dismiss alarm", "cancel alarm"),
                hindiPhrases = listOf("alarm band karo", "alarm roko", "alarm off karo"),
                bengaliPhrases = listOf("অ্যালার্ম বন্ধ করো", "অ্যালার্ম থামাও"),
                description = "Dismiss active alarm"
            )
        )
        builtinDefinitions.add(
            CommandDefinition(
                intentId = "CLK003",
                category = CommandCategory.CLOCK_CALENDAR,
                englishPhrases = listOf("what time is it", "what is the time", "current time", "tell me time"),
                hindiPhrases = listOf("kitne baje hain", "samay kya hua hai", "time kya hai", "kitna time hua"),
                bengaliPhrases = listOf("এখন কটা বাজে", "সময় কত", "কটা বাজে"),
                description = "Speak current time"
            )
        )
        builtinDefinitions.add(
            CommandDefinition(
                intentId = "CLK004",
                category = CommandCategory.CLOCK_CALENDAR,
                englishPhrases = listOf("set a timer for 10 minutes", "timer 10 minutes", "10 minute timer"),
                hindiPhrases = listOf("10 minute ka timer lagao", "das minute timer lagao", "10 minute timer"),
                bengaliPhrases = listOf("১০ মিনিটের টাইমার দাও", "১০ মিনিট টাইমার সেট করো"),
                requiredParameters = listOf("duration"),
                description = "Start timer"
            )
        )
        builtinDefinitions.add(
            CommandDefinition(
                intentId = "CLK005",
                category = CommandCategory.CLOCK_CALENDAR,
                englishPhrases = listOf("what's on my calendar today", "my calendar today", "today schedule", "today's events"),
                hindiPhrases = listOf("aaj mere calendar mein kya hai", "aaj ka schedule kya hai", "aaj ki meetings kya hain"),
                bengaliPhrases = listOf("আজ আমার ক্যালেন্ডারে কী আছে", "আজকের সূচি কী"),
                description = "Read today's events"
            )
        )
        builtinDefinitions.add(
            CommandDefinition(
                intentId = "CLK006",
                category = CommandCategory.CLOCK_CALENDAR,
                englishPhrases = listOf("create a meeting tomorrow", "schedule a meeting tomorrow", "calendar meeting tomorrow"),
                hindiPhrases = listOf("kal ek meeting schedule karo", "kal meeting add karo", "kal ki meeting lagao"),
                bengaliPhrases = listOf("কাল একটি মিটিং শিডিউল করো", "কালকের মিটিং যোগ করো"),
                requiredParameters = listOf("title", "time"),
                description = "Ask details and create event"
            )
        )

        // ==================== CATEGORY 7 – SMS AND WHATSAPP ====================
        builtinDefinitions.add(
            CommandDefinition(
                intentId = "MSG001",
                category = CommandCategory.MESSAGING,
                englishPhrases = listOf("send rahul a whatsapp message", "whatsapp message to rahul", "send whatsapp to rahul"),
                hindiPhrases = listOf("rahul ko whatsapp message bhejo", "rahul ko whatsapp karo", "whatsapp bhejo rahul ko"),
                bengaliPhrases = listOf("রাহুলকে whatsapp মেসেজ পাঠাও", "রাহুলকে হোয়াটসঅ্যাপ করো"),
                requiredParameters = listOf("contact", "message"),
                confirmationRequired = true,
                description = "Identify contact and compose WhatsApp message"
            )
        )
        builtinDefinitions.add(
            CommandDefinition(
                intentId = "MSG002",
                category = CommandCategory.MESSAGING,
                englishPhrases = listOf("send an sms to papa", "sms to papa", "send message to papa"),
                hindiPhrases = listOf("papa ko sms bhejo", "papa ko message bhejo", "papa ko text karo"),
                bengaliPhrases = listOf("বাবাকে sms পাঠাও", "বাবাকে মেসেজ পাঠাও"),
                requiredParameters = listOf("contact", "message"),
                confirmationRequired = true,
                description = "Compose SMS"
            )
        )
        builtinDefinitions.add(
            CommandDefinition(
                intentId = "MSG003",
                category = CommandCategory.MESSAGING,
                englishPhrases = listOf("read my latest messages", "read messages", "check my messages", "read sms"),
                hindiPhrases = listOf("mere latest messages padho", "nayi message padho", "messages padh ke sunao"),
                bengaliPhrases = listOf("আমার সাম্প্রতিক মেসেজ পড়ো", "মেসেজগুলো পড়ে শোনাও"),
                description = "Read authorized messages"
            )
        )
        builtinDefinitions.add(
            CommandDefinition(
                intentId = "MSG004",
                category = CommandCategory.MESSAGING,
                englishPhrases = listOf("reply to this message", "reply to message", "send a reply"),
                hindiPhrases = listOf("is message ka reply do", "reply bhejo", "uttar do"),
                bengaliPhrases = listOf("এই মেসেজের উত্তর দাও", "রিপ্লাই দাও"),
                requiredParameters = listOf("message"),
                confirmationRequired = true,
                description = "Draft contextual reply"
            )
        )
        builtinDefinitions.add(
            CommandDefinition(
                intentId = "MSG005",
                category = CommandCategory.MESSAGING,
                englishPhrases = listOf("open my whatsapp", "open whatsapp", "launch whatsapp"),
                hindiPhrases = listOf("mera whatsapp kholo", "whatsapp kholo", "whatsapp open karo"),
                bengaliPhrases = listOf("আমার whatsapp খোলো", "হোয়াটসঅ্যাপ খোলো"),
                description = "Launch WhatsApp"
            )
        )
        builtinDefinitions.add(
            CommandDefinition(
                intentId = "MSG006",
                category = CommandCategory.MESSAGING,
                englishPhrases = listOf("translate this message into bengali", "translate message to bengali"),
                hindiPhrases = listOf("is message ko bengali mein translate karo", "bengali mein anuvad karo"),
                bengaliPhrases = listOf("এই মেসেজটি বাংলায় অনুবাদ করো", "মেসেজটি বাংলায় ট্রান্সলেট করো"),
                requiredParameters = listOf("targetLang"),
                description = "Translate message"
            )
        )
        builtinDefinitions.add(
            CommandDefinition(
                intentId = "MSG007",
                category = CommandCategory.MESSAGING,
                englishPhrases = listOf("send this message", "send message", "confirm send"),
                hindiPhrases = listOf("ye message bhej do", "message bhej do", "send karo"),
                bengaliPhrases = listOf("এই মেসেজটি পাঠিয়ে দাও", "মেসেজটি পাঠিয়ে দাও"),
                confirmationRequired = true,
                description = "Confirm recipient and send"
            )
        )

        // ==================== CATEGORY 8 – PHONE CALLS AND CONTACTS ====================
        builtinDefinitions.add(
            CommandDefinition(
                intentId = "CALL001",
                category = CommandCategory.CALLS_CONTACTS,
                englishPhrases = listOf("call papa", "phone papa", "dial papa"),
                hindiPhrases = listOf("papa ko call karo", "papa ko phone lagao", "papa se baat karwao"),
                bengaliPhrases = listOf("বাবাকে ফোন করো", "বাবাকে কল করো"),
                requiredParameters = listOf("contact"),
                confirmationRequired = true,
                description = "Initiate call with confirmation"
            )
        )
        builtinDefinitions.add(
            CommandDefinition(
                intentId = "CALL002",
                category = CommandCategory.CALLS_CONTACTS,
                englishPhrases = listOf("call rahul on whatsapp", "whatsapp call rahul"),
                hindiPhrases = listOf("rahul ko whatsapp call karo", "rahul ko whatsapp call lagao"),
                bengaliPhrases = listOf("রাহুলকে whatsapp কল করো", "হোয়াটসঅ্যাপে রাহুলকে কল করো"),
                requiredParameters = listOf("contact"),
                confirmationRequired = true,
                description = "Start WhatsApp call"
            )
        )
        builtinDefinitions.add(
            CommandDefinition(
                intentId = "CALL003",
                category = CommandCategory.CALLS_CONTACTS,
                englishPhrases = listOf("open my contacts", "open contacts", "show contacts"),
                hindiPhrases = listOf("mere contacts kholo", "contacts kholo", "phonebook kholo"),
                bengaliPhrases = listOf("আমার কন্ট্যাক্ট খোলো", "কন্ট্যাক্টস খোলো"),
                description = "Open contacts app"
            )
        )
        builtinDefinitions.add(
            CommandDefinition(
                intentId = "CALL004",
                category = CommandCategory.CALLS_CONTACTS,
                englishPhrases = listOf("find amit's number", "find contact amit", "search amit number"),
                hindiPhrases = listOf("amit ka number dhundo", "amit ka contact dhundo", "amit ka phone number"),
                bengaliPhrases = listOf("অমিতের নম্বর খোঁজো", "অমিতের কন্ট্যাক্ট খোঁজো"),
                requiredParameters = listOf("contact"),
                description = "Find authorized contact"
            )
        )
        builtinDefinitions.add(
            CommandDefinition(
                intentId = "CALL005",
                category = CommandCategory.CALLS_CONTACTS,
                englishPhrases = listOf("end the call", "cut the call", "hang up", "disconnect call"),
                hindiPhrases = listOf("call cut karo", "phone kaato", "call band karo"),
                bengaliPhrases = listOf("ফোন কেটে দাও", "কল শেষ করো"),
                description = "End call where permitted"
            )
        )
        builtinDefinitions.add(
            CommandDefinition(
                intentId = "CALL006",
                category = CommandCategory.CALLS_CONTACTS,
                englishPhrases = listOf("redial the last number", "redial", "call again"),
                hindiPhrases = listOf("last number par dobara call karo", "dobara call lagao", "redial karo"),
                bengaliPhrases = listOf("শেষ নম্বরে আবার ফোন করো", "রিডায়াল করো"),
                confirmationRequired = true,
                description = "Redial with confirmation"
            )
        )

        // ==================== CATEGORY 9 – SCREEN VISION AND IMAGE UNDERSTANDING ====================
        builtinDefinitions.add(
            CommandDefinition(
                intentId = "VIS001",
                category = CommandCategory.SCREEN_VISION,
                englishPhrases = listOf("max can you see this", "can you see this", "can you see my screen"),
                hindiPhrases = listOf("max kya tum ye dekh rahe ho", "kya tum ye dekh rahe ho", "kya tum screen dekh sakte ho"),
                bengaliPhrases = listOf("ম্যাক্স তুমি কি এটা দেখতে পাচ্ছো", "তুমি কি এটা দেখতে পাচ্ছো", "স্ক্রিন দেখতে পাচ্ছো"),
                description = "Activate authorized screen vision"
            )
        )
        builtinDefinitions.add(
            CommandDefinition(
                intentId = "VIS002",
                category = CommandCategory.SCREEN_VISION,
                englishPhrases = listOf("read what's on my screen", "read my screen", "read text on screen"),
                hindiPhrases = listOf("screen par kya likha hai padho", "screen padho", "screen ka text padho"),
                bengaliPhrases = listOf("স্ক্রিনে কী লেখা আছে পড়ো", "স্ক্রিন পড়ো"),
                description = "Capture and extract text from screen"
            )
        )
        builtinDefinitions.add(
            CommandDefinition(
                intentId = "VIS003",
                category = CommandCategory.SCREEN_VISION,
                englishPhrases = listOf("explain this image", "analyze this image", "what is in this picture"),
                hindiPhrases = listOf("is image ko samjhao", "ye photo samjhao", "image ka vishleshan karo"),
                bengaliPhrases = listOf("এই ছবিটি বোঝাও", "ছবিটি বিশ্লেষণ করো"),
                description = "Analyse visible image"
            )
        )
        builtinDefinitions.add(
            CommandDefinition(
                intentId = "VIS004",
                category = CommandCategory.SCREEN_VISION,
                englishPhrases = listOf("solve this question", "solve question on screen", "help solve this question"),
                hindiPhrases = listOf("is question ko solve karo", "screen ka sawal solve karo", "ye prashna hal karo"),
                bengaliPhrases = listOf("এই প্রশ্নটি সমাধান করো", "স্ক্রিনের প্রশ্নটি সমাধান করো"),
                description = "Analyse visible question"
            )
        )
        builtinDefinitions.add(
            CommandDefinition(
                intentId = "VIS005",
                category = CommandCategory.SCREEN_VISION,
                englishPhrases = listOf("what is wrong with this screen", "find the error on screen", "find the error"),
                hindiPhrases = listOf("screen mein kya problem hai", "screen par error dhundo", "kya galti hai"),
                bengaliPhrases = listOf("স্ক্রিনে কী সমস্যা আছে", "স্ক্রিনের ভুল খুঁজে বের করো"),
                description = "Analyse visible error"
            )
        )
        builtinDefinitions.add(
            CommandDefinition(
                intentId = "VIS006",
                category = CommandCategory.SCREEN_VISION,
                englishPhrases = listOf("stop watching my screen", "stop screen vision", "stop screen capture"),
                hindiPhrases = listOf("screen dekhna band karo", "screen vision band karo", "screen recording band karo"),
                bengaliPhrases = listOf("স্ক্রিন দেখা বন্ধ করো", "স্ক্রিন ভিশন বন্ধ করো"),
                description = "Stop screen capture"
            )
        )
        builtinDefinitions.add(
            CommandDefinition(
                intentId = "VIS007",
                category = CommandCategory.SCREEN_VISION,
                englishPhrases = listOf("translate this page", "translate screen", "translate visible page"),
                hindiPhrases = listOf("is page ka translation karo", "screen translate karo", "is page ka anuvad karo"),
                bengaliPhrases = listOf("এই পৃষ্ঠাটি অনুবাদ করো", "স্ক্রিন অনুবাদ করো"),
                description = "OCR and translate screen page"
            )
        )

        // ==================== CATEGORY 10 – GOOGLE, INTERNET AND CHROME ====================
        builtinDefinitions.add(
            CommandDefinition(
                intentId = "WEB001",
                category = CommandCategory.WEB_CHROME,
                englishPhrases = listOf("search google for this", "search on google", "google search"),
                hindiPhrases = listOf("isko google par search karo", "google par dhoondo", "google search karo"),
                bengaliPhrases = listOf("এটি google-এ সার্চ করো", "গুগলে সার্চ করো"),
                requiredParameters = listOf("query"),
                description = "Perform Google web search"
            )
        )
        builtinDefinitions.add(
            CommandDefinition(
                intentId = "WEB002",
                category = CommandCategory.WEB_CHROME,
                englishPhrases = listOf("open chrome", "launch chrome", "open browser"),
                hindiPhrases = listOf("chrome kholo", "google chrome kholo", "browser kholo"),
                bengaliPhrases = listOf("chrome খোলো", "ক্রোম খোলো", "ব্রাউজার খোলো"),
                description = "Launch Chrome browser"
            )
        )
        builtinDefinitions.add(
            CommandDefinition(
                intentId = "WEB003",
                category = CommandCategory.WEB_CHROME,
                englishPhrases = listOf("search for government jobs", "sarkari job search", "latest government jobs"),
                hindiPhrases = listOf("sarkari naukri search karo", "sarkari vacancy dhundo", "government job dekho"),
                bengaliPhrases = listOf("সরকারি চাকরি খোঁজো", "সরকারি চাকরির খবর খোঁজো"),
                description = "Search latest government job listings"
            )
        )
        builtinDefinitions.add(
            CommandDefinition(
                intentId = "WEB004",
                category = CommandCategory.WEB_CHROME,
                englishPhrases = listOf("find the official website", "official website", "official portal"),
                hindiPhrases = listOf("official website dhundo", "asli website khojo", "sarkari portal dhundo"),
                bengaliPhrases = listOf("অফিসিয়াল ওয়েবসাইট খোঁজো", "অফিসিয়াল পোর্টাল খোঁজো"),
                requiredParameters = listOf("query"),
                description = "Find official domain"
            )
        )
        builtinDefinitions.add(
            CommandDefinition(
                intentId = "WEB005",
                category = CommandCategory.WEB_CHROME,
                englishPhrases = listOf("open this website", "open url", "visit website"),
                hindiPhrases = listOf("ye website kholo", "website open karo", "link kholo"),
                bengaliPhrases = listOf("এই ওয়েবসাইটটি খোলো", "লিঙ্কটি খোলো"),
                requiredParameters = listOf("url"),
                description = "Open URL after validation"
            )
        )
        builtinDefinitions.add(
            CommandDefinition(
                intentId = "WEB006",
                category = CommandCategory.WEB_CHROME,
                englishPhrases = listOf("summarize this webpage", "webpage summary", "summarize article"),
                hindiPhrases = listOf("is webpage ka summary do", "webpage ka saraansh batao", "is page ko summarize karo"),
                bengaliPhrases = listOf("এই ওয়েবপেজের সারাংশ বলো", "ওয়েবপেজের সারসংক্ষেপ দাও"),
                description = "Summarize accessible webpage"
            )
        )
        builtinDefinitions.add(
            CommandDefinition(
                intentId = "WEB007",
                category = CommandCategory.WEB_CHROME,
                englishPhrases = listOf("compare these results", "compare search results", "compare information"),
                hindiPhrases = listOf("in results ko compare karo", "tulanatmak vishleshan karo", "in dono ko compare karo"),
                bengaliPhrases = listOf("এই ফলাফলগুলো তুলনা করো", "ফলাফল তুলনা করো"),
                description = "Compare search results"
            )
        )

        // ==================== CATEGORY 11 – PHONE AND APP CONTROL ====================
        builtinDefinitions.add(
            CommandDefinition(
                intentId = "PHN001",
                category = CommandCategory.PHONE_CONTROL,
                englishPhrases = listOf("open youtube", "launch youtube", "youtube"),
                hindiPhrases = listOf("youtube kholo", "youtube chalao", "youtube open karo"),
                bengaliPhrases = listOf("youtube খোলো", "ইউটিউব খোলো", "ইউটিউব চালাও"),
                description = "Launch YouTube app"
            )
        )
        builtinDefinitions.add(
            CommandDefinition(
                intentId = "PHN002",
                category = CommandCategory.PHONE_CONTROL,
                englishPhrases = listOf("open camera", "launch camera", "take a picture"),
                hindiPhrases = listOf("camera kholo", "camera chalao", "photo khicho"),
                bengaliPhrases = listOf("ক্যামেরা খোলো", "ক্যামেরা চালু করো", "ছবি তোলো"),
                description = "Launch camera"
            )
        )
        builtinDefinitions.add(
            CommandDefinition(
                intentId = "PHN003",
                category = CommandCategory.PHONE_CONTROL,
                englishPhrases = listOf("turn on flashlight", "flashlight on", "torch on"),
                hindiPhrases = listOf("torch on karo", "flashlight jalao", "torch chalu karo"),
                bengaliPhrases = listOf("টর্চ জ্বালাও", "টর্চ অন করো", "ফ্ল্যাশলাইট জ্বালাও"),
                description = "Enable flashlight"
            )
        )
        builtinDefinitions.add(
            CommandDefinition(
                intentId = "PHN004",
                category = CommandCategory.PHONE_CONTROL,
                englishPhrases = listOf("turn off flashlight", "flashlight off", "torch off"),
                hindiPhrases = listOf("torch off karo", "flashlight band karo", "torch bujhao"),
                bengaliPhrases = listOf("টর্চ বন্ধ করো", "টর্চ অফ করো", "ফ্ল্যাশলাইট নিভিয়ে দাও"),
                description = "Disable flashlight"
            )
        )
        builtinDefinitions.add(
            CommandDefinition(
                intentId = "PHN005",
                category = CommandCategory.PHONE_CONTROL,
                englishPhrases = listOf("open settings", "launch settings", "phone settings"),
                hindiPhrases = listOf("settings kholo", "phone ki settings", "settings open karo"),
                bengaliPhrases = listOf("সেটিংস খোলো", "ফোনের সেটিংস খোলো"),
                description = "Launch settings"
            )
        )
        builtinDefinitions.add(
            CommandDefinition(
                intentId = "PHN006",
                category = CommandCategory.PHONE_CONTROL,
                englishPhrases = listOf("increase volume", "volume up", "louder"),
                hindiPhrases = listOf("volume badhao", "awaz badhao", "awaz tez karo"),
                bengaliPhrases = listOf("ভলিউম বাড়াও", "শব্দ বাড়াও", "আওয়াজ বাড়াও"),
                description = "Increase media volume"
            )
        )
        builtinDefinitions.add(
            CommandDefinition(
                intentId = "PHN007",
                category = CommandCategory.PHONE_CONTROL,
                englishPhrases = listOf("decrease brightness", "lower brightness", "dim screen"),
                hindiPhrases = listOf("brightness kam karo", "screen ki roshni kam karo", "brightness ghatao"),
                bengaliPhrases = listOf("ব্রাইটনেস কমাও", "স্ক্রিনের আলো কমাও"),
                description = "Lower brightness where permitted"
            )
        )

        // ==================== CATEGORY 12 – TRANSLATION AND LANGUAGE ====================
        builtinDefinitions.add(
            CommandDefinition(
                intentId = "LAN001",
                category = CommandCategory.TRANSLATION,
                englishPhrases = listOf("translate this into hindi", "translate to hindi", "in hindi"),
                hindiPhrases = listOf("isko hindi mein translate karo", "hindi mein anuvad karo", "hindi me batao"),
                bengaliPhrases = listOf("এটি হিন্দিতে অনুবাদ করো", "হিন্দিতে অনুবাদ করো"),
                description = "Translate to Hindi"
            )
        )
        builtinDefinitions.add(
            CommandDefinition(
                intentId = "LAN002",
                category = CommandCategory.TRANSLATION,
                englishPhrases = listOf("translate this into bengali", "translate to bengali", "in bengali"),
                hindiPhrases = listOf("isko bengali mein translate karo", "bangla mein anuvad karo", "bangla me batao"),
                bengaliPhrases = listOf("এটি বাংলায় অনুবাদ করো", "বাংলায় অনুবাদ করো"),
                description = "Translate to Bengali"
            )
        )
        builtinDefinitions.add(
            CommandDefinition(
                intentId = "LAN003",
                category = CommandCategory.TRANSLATION,
                englishPhrases = listOf("speak in english", "english language", "switch to english"),
                hindiPhrases = listOf("english mein baat karo", "english bolo", "angrezi mein bolo"),
                bengaliPhrases = listOf("ইংরেজিতে কথা বলো", "ইংলিশে বলো"),
                description = "Switch response language to English"
            )
        )
        builtinDefinitions.add(
            CommandDefinition(
                intentId = "LAN004",
                category = CommandCategory.TRANSLATION,
                englishPhrases = listOf("speak in hindi", "hindi language", "switch to hindi"),
                hindiPhrases = listOf("hindi mein baat karo", "hindi bolo", "hindi mein bolo"),
                bengaliPhrases = listOf("হিন্দিতে কথা বলো", "হিন্দি বলো"),
                description = "Switch response language to Hindi"
            )
        )
        builtinDefinitions.add(
            CommandDefinition(
                intentId = "LAN005",
                category = CommandCategory.TRANSLATION,
                englishPhrases = listOf("speak in bengali", "bengali language", "switch to bengali"),
                hindiPhrases = listOf("bengali mein baat karo", "bangla bolo", "bangla mein bolo"),
                bengaliPhrases = listOf("বাংলায় কথা বলো", "বাংলা বলো"),
                description = "Switch response language to Bengali"
            )
        )
        builtinDefinitions.add(
            CommandDefinition(
                intentId = "LAN006",
                category = CommandCategory.TRANSLATION,
                englishPhrases = listOf("what does this word mean", "word meaning", "define this word"),
                hindiPhrases = listOf("is word ka meaning kya hai", "is shabd ka arth kya hai", "ka matlab kya hai"),
                bengaliPhrases = listOf("এই শব্দের অর্থ কী", "অর্থ কী"),
                requiredParameters = listOf("word"),
                description = "Explain word meaning"
            )
        )

        // ==================== CATEGORY 13 – CALCULATIONS AND FINANCE BASICS ====================
        builtinDefinitions.add(
            CommandDefinition(
                intentId = "CAL001",
                category = CommandCategory.CALCULATIONS,
                englishPhrases = listOf("calculate 25 percent of 1000", "25 percent of 1000", "percentage calculate"),
                hindiPhrases = listOf("1000 ka 25 percent nikalo", "1000 ka 25 pratishat kya hoga"),
                bengaliPhrases = listOf("১০০০-এর ২৫ শতাংশ বের করো", "১০০০ এর ২৫ শতাংশ"),
                description = "Calculate percentage"
            )
        )
        builtinDefinitions.add(
            CommandDefinition(
                intentId = "CAL002",
                category = CommandCategory.CALCULATIONS,
                englishPhrases = listOf("convert rupees into dollars", "rupees to dollars", "usd in inr"),
                hindiPhrases = listOf("rupees ko dollars mein convert karo", "dollar ka rate kya hai", "rupaye ko dollar me badlo"),
                bengaliPhrases = listOf("টাকা ডলারে রূপান্তর করো", "ডলার রূপান্তর করো"),
                description = "Currency conversion using live rate"
            )
        )
        builtinDefinitions.add(
            CommandDefinition(
                intentId = "CAL003",
                category = CommandCategory.CALCULATIONS,
                englishPhrases = listOf("calculate my monthly expenses", "monthly expenses", "calculate expenses"),
                hindiPhrases = listOf("mere monthly expenses calculate karo", "mahine ka kharcha jodo", "kharch ka hisab"),
                bengaliPhrases = listOf("আমার মাসিক খরচ হিসাব করো", "মাসিক খরচের হিসাব"),
                description = "Calculate supplied expenses"
            )
        )
        builtinDefinitions.add(
            CommandDefinition(
                intentId = "CAL004",
                category = CommandCategory.CALCULATIONS,
                englishPhrases = listOf("add these numbers", "add numbers", "sum of numbers", "plus"),
                hindiPhrases = listOf("in numbers ko jodo", "jodo", "plus karo"),
                bengaliPhrases = listOf("এই সংখ্যাগুলো যোগ করো", "যোগ করো"),
                requiredParameters = listOf("expression"),
                description = "Addition"
            )
        )
        builtinDefinitions.add(
            CommandDefinition(
                intentId = "CAL005",
                category = CommandCategory.CALCULATIONS,
                englishPhrases = listOf("divide these numbers", "divide numbers", "division"),
                hindiPhrases = listOf("in numbers ko divide karo", "bhag karo", "divide"),
                bengaliPhrases = listOf("এই সংখ্যাগুলো ভাগ করো", "ভাগ করো"),
                requiredParameters = listOf("expression"),
                description = "Division"
            )
        )
        builtinDefinitions.add(
            CommandDefinition(
                intentId = "CAL006",
                category = CommandCategory.CALCULATIONS,
                englishPhrases = listOf("calculate gst at 18 percent", "18 percent gst", "gst calculate"),
                hindiPhrases = listOf("18 percent gst nikalo", "18 pratishat gst kitna hoga", "gst jodo"),
                bengaliPhrases = listOf("১৮ শতাংশ gst হিসাব করো", "জিএসটি হিসাব করো"),
                description = "GST calculation"
            )
        )

        // ==================== CATEGORY 14 – TRAVEL AND DAILY INFORMATION ====================
        builtinDefinitions.add(
            CommandDefinition(
                intentId = "TRV001",
                category = CommandCategory.TRAVEL_DAILY,
                englishPhrases = listOf("find hotels in darjeeling", "darjeeling hotels", "hotels near me"),
                hindiPhrases = listOf("darjeeling mein hotel dhundo", "hotel khojo darjeeling", "paas ke hotel"),
                bengaliPhrases = listOf("দার্জিলিংয়ে হোটেল খোঁজো", "দার্জিলিং হোটেল"),
                requiredParameters = listOf("location"),
                description = "Search hotels"
            )
        )
        builtinDefinitions.add(
            CommandDefinition(
                intentId = "TRV002",
                category = CommandCategory.TRAVEL_DAILY,
                englishPhrases = listOf("find trains to kolkata", "trains to kolkata", "kolkata train timings"),
                hindiPhrases = listOf("kolkata jaane wali train dhundo", "kolkata ke liye train", "kolkata train"),
                bengaliPhrases = listOf("কলকাতা যাওয়ার ট্রেন খোঁজো", "কলকাতা ট্রেন"),
                requiredParameters = listOf("destination"),
                description = "Search train information"
            )
        )
        builtinDefinitions.add(
            CommandDefinition(
                intentId = "TRV003",
                category = CommandCategory.TRAVEL_DAILY,
                englishPhrases = listOf("show directions to my destination", "directions to destination", "navigate to destination", "show directions"),
                hindiPhrases = listOf("destination ka rasta dikhao", "rasta dikhao", "kaise jaye"),
                bengaliPhrases = listOf("গন্তব্যে যাওয়ার রাস্তা দেখাও", "রাস্তা দেখাও"),
                requiredParameters = listOf("destination"),
                description = "Open navigation"
            )
        )
        builtinDefinitions.add(
            CommandDefinition(
                intentId = "TRV004",
                category = CommandCategory.TRAVEL_DAILY,
                englishPhrases = listOf("what's the weather today", "weather today", "how is the weather"),
                hindiPhrases = listOf("aaj mausam kaisa hai", "aaj ka mausam", "mausam kaisa hai"),
                bengaliPhrases = listOf("আজ আবহাওয়া কেমন", "আজকের আবহাওয়া কেমন", "আবহাওয়া"),
                description = "Retrieve current weather"
            )
        )
        builtinDefinitions.add(
            CommandDefinition(
                intentId = "TRV005",
                category = CommandCategory.TRAVEL_DAILY,
                englishPhrases = listOf("plan a trip for me", "trip plan", "itinerary plan"),
                hindiPhrases = listOf("mere liye trip plan karo", "ghoomne ka plan banao", "itinerary banao"),
                bengaliPhrases = listOf("আমার জন্য ভ্রমণ পরিকল্পনা করো", "ভ্রমণ পরিকল্পনা"),
                description = "Start trip planner"
            )
        )
        builtinDefinitions.add(
            CommandDefinition(
                intentId = "TRV006",
                category = CommandCategory.TRAVEL_DAILY,
                englishPhrases = listOf("find nearby restaurants", "nearby restaurants", "food places near me"),
                hindiPhrases = listOf("paas mein restaurant dhundo", "paas ke restaurant", "nazdeeki hotel"),
                bengaliPhrases = listOf("কাছাকাছি রেস্টুরেন্ট খোঁজো", "কাছের খাবারের দোকান"),
                description = "Search nearby restaurants"
            )
        )

        // ==================== CATEGORY 15 – MAX PERSONAL CONTROL AND PRIVACY ====================
        builtinDefinitions.add(
            CommandDefinition(
                intentId = "SYS001",
                category = CommandCategory.SYS_PRIVACY,
                englishPhrases = listOf("max wake up", "wake up max", "wake up"),
                hindiPhrases = listOf("max jaag jao", "jaag jao max", "jaago max", "jaag jao", "jaago", "uth jao"),
                bengaliPhrases = listOf("ম্যাক্স জেগে ওঠো", "জেগে ওঠো ম্যাক্স", "জেগে ওঠো"),
                description = "Exit standby"
            )
        )
        builtinDefinitions.add(
            CommandDefinition(
                intentId = "SYS002",
                category = CommandCategory.SYS_PRIVACY,
                englishPhrases = listOf("max go to standby", "go to standby", "standby mode", "standby"),
                hindiPhrases = listOf("max standby ho jao", "standby ho jao", "so jao", "standby mode", "standby"),
                bengaliPhrases = listOf("ম্যাক্স স্ট্যান্ডবাই হও", "স্ট্যান্ডবাই হও", "ঘুমিয়ে পড়ো", "স্ট্যান্ডবাই"),
                description = "Enter standby"
            )
        )
        builtinDefinitions.add(
            CommandDefinition(
                intentId = "SYS003",
                category = CommandCategory.SYS_PRIVACY,
                englishPhrases = listOf("max stop everything", "stop everything", "stop all actions", "emergency stop", "stop"),
                hindiPhrases = listOf("max sab kuch band karo", "sab band karo", "sab kuch rok do", "sab kuch band"),
                bengaliPhrases = listOf("ম্যাক্স সব বন্ধ করো", "সব বন্ধ করো", "সব থামাও"),
                description = "Stop MAX operations"
            )
        )
        builtinDefinitions.add(
            CommandDefinition(
                intentId = "SYS004",
                category = CommandCategory.SYS_PRIVACY,
                englishPhrases = listOf("max shut down completely", "shut down completely", "full shutdown", "power off max", "completely shut down"),
                hindiPhrases = listOf("max completely band ho jao", "completely band ho jao", "pura band ho jao", "shutdown karo", "completely band"),
                bengaliPhrases = listOf("ম্যাক্স পুরোপুরি বন্ধ হয়ে যাও", "পুরোপুরি বন্ধ হয়ে যাও", "সম্পূর্ণ বন্ধ করো", "পুরোপুরি বন্ধ"),
                description = "Full shutdown"
            )
        )
        builtinDefinitions.add(
            CommandDefinition(
                intentId = "SYS005",
                category = CommandCategory.SYS_PRIVACY,
                englishPhrases = listOf("max stop listening", "stop listening", "mute microphone"),
                hindiPhrases = listOf("max sunna band karo", "sunna band karo", "awaz band karo", "mute karo"),
                bengaliPhrases = listOf("ম্যাক্স শোনা বন্ধ করো", "শোনা বন্ধ করো", "মাইক্রোফোন মিউট করো"),
                description = "Stop microphone listener"
            )
        )
        builtinDefinitions.add(
            CommandDefinition(
                intentId = "SYS006",
                category = CommandCategory.SYS_PRIVACY,
                englishPhrases = listOf("max cancel that operation", "cancel that operation", "cancel operation", "cancel that", "abort"),
                hindiPhrases = listOf("max woh operation cancel karo", "woh operation cancel karo", "operation cancel karo", "radd karo", "rok do"),
                bengaliPhrases = listOf("ম্যাক্স কাজটি বাতিল করো", "কাজটি বাতিল করো", "বাতিল করো"),
                description = "Cancel current operation"
            )
        )
        builtinDefinitions.add(
            CommandDefinition(
                intentId = "SYS007",
                category = CommandCategory.SYS_PRIVACY,
                englishPhrases = listOf("max are you active", "are you active", "are you online", "are you there"),
                hindiPhrases = listOf("max kya tum active ho", "kya tum active ho", "kya tum online ho", "kya tum sun rahe ho"),
                bengaliPhrases = listOf("ম্যাক্স তুমি কি সক্রিয় আছো", "তুমি কি সক্রিয় আছো", "তুমি কি শুনছো"),
                description = "Report service state"
            )
        )

        // ==================== CATEGORY 16 – BABY ENTERTAINMENT MODE ====================
        builtinDefinitions.add(
            CommandDefinition(
                intentId = "BABY001",
                category = CommandCategory.BABY_ENTERTAINMENT,
                englishPhrases = listOf("the baby is crying", "baby is crying", "distract the baby", "comfort the baby", "baby crying"),
                hindiPhrases = listOf("baccha ro raha hai", "bacha ro raha hai", "iska dhyan bhatkao", "baby ro raha hai", "iska dhyan divert karo"),
                bengaliPhrases = listOf("বাচ্চা কাঁদছে", "বাচ্চার মনোযোগ অন্যদিকে ঘোরাও", "বাচ্চা কান্নাকাটি করছে"),
                description = "Activate baby mode for crying child"
            )
        )
        builtinDefinitions.add(
            CommandDefinition(
                intentId = "BABY002",
                category = CommandCategory.BABY_ENTERTAINMENT,
                englishPhrases = listOf("entertain the baby", "play something for the baby", "start baby mode", "baby mode"),
                hindiPhrases = listOf("baby ko entertain karo", "bacche ko entertain karo", "baby ke liye kuch chalao", "baby mode chalu karo"),
                bengaliPhrases = listOf("বাচ্চাকে আনন্দ দাও", "বাচ্চার জন্য কিছু চালাও", "বেবি মোড চালু করো"),
                description = "Entertain baby with playful activities"
            )
        )
        builtinDefinitions.add(
            CommandDefinition(
                intentId = "BABY003",
                category = CommandCategory.BABY_ENTERTAINMENT,
                englishPhrases = listOf("make the baby laugh", "do something funny for the baby", "make baby smile"),
                hindiPhrases = listOf("bachche ko hansaao", "bachhe ko hasao", "baby ko hasao", "kuch funny karo"),
                bengaliPhrases = listOf("বাচ্চাকে হাসাও", "মজার কিছু করো"),
                description = "Make baby laugh with funny sounds"
            )
        )
        builtinDefinitions.add(
            CommandDefinition(
                intentId = "BABY004",
                category = CommandCategory.BABY_ENTERTAINMENT,
                englishPhrases = listOf("help the baby sleep", "play a lullaby", "sing a lullaby", "put the baby to sleep"),
                hindiPhrases = listOf("help the baby sleep", "lori sunao", "baby ko sulao", "pyari lori sunao", "meethi lori sunao"),
                bengaliPhrases = listOf("ঘুম পাড়ানোর গান শোনাও", "বাচ্চাকে ঘুম পাড়াও", "লোরি শোনাও"),
                description = "Play gentle soothing lullaby"
            )
        )
        builtinDefinitions.add(
            CommandDefinition(
                intentId = "BABY005",
                category = CommandCategory.BABY_ENTERTAINMENT,
                englishPhrases = listOf("tell the baby a story", "tell a story", "tell baby story"),
                hindiPhrases = listOf("baby ko kahani sunao", "bachche ko kahani sunao", "kahani sunao"),
                bengaliPhrases = listOf("একটা গল্প বলো", "বাচ্চাকে গল্প শোনাও", "গল্প বলো"),
                description = "Tell a gentle animated story"
            )
        )
        builtinDefinitions.add(
            CommandDefinition(
                intentId = "BABY006",
                category = CommandCategory.BABY_ENTERTAINMENT,
                englishPhrases = listOf("recite a rhyme", "play a rhyme", "nursery rhyme"),
                hindiPhrases = listOf("rhyme sunao", "kavita sunao", "bal geet sunao"),
                bengaliPhrases = listOf("একটা ছড়া শোনাও", "বাচ্চাদের ছড়া শোনাও", "ছড়া শোনাও"),
                description = "Recite playful nursery rhymes"
            )
        )
        builtinDefinitions.add(
            CommandDefinition(
                intentId = "BABY007",
                category = CommandCategory.BABY_ENTERTAINMENT,
                englishPhrases = listOf("animal sounds", "play animal sounds", "next game", "next activity"),
                hindiPhrases = listOf("animal sounds", "janwaron ki awaz", "next game", "next activity", "khel badlo"),
                bengaliPhrases = listOf("পশুর ডাক", "পরের খেলা", "পরের অ্যাক্টিভিটি"),
                description = "Play animal sounds and games"
            )
        )
        builtinDefinitions.add(
            CommandDefinition(
                intentId = "BABY008",
                category = CommandCategory.BABY_ENTERTAINMENT,
                englishPhrases = listOf("stop baby mode", "close baby mode", "exit baby mode"),
                hindiPhrases = listOf("stop baby mode", "baby mode band karo", "baby entertainment band karo"),
                bengaliPhrases = listOf("বেবি মোড বন্ধ করো", "বেবি মোড থামাও"),
                description = "Stop baby entertainment mode"
            )
        )
    }
}
