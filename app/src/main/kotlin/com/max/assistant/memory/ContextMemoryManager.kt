package com.max.assistant.memory

import android.content.Context
import android.util.Log
import com.max.assistant.database.ConversationRecord
import com.max.assistant.database.MaxDatabaseHelper
import com.max.assistant.database.ProjectTaskRecord
import com.max.assistant.database.UserMemoryRecord
import com.max.assistant.tts.VoiceLanguage
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class ContextMemoryManager(private val context: Context) {

    companion object {
        private const val TAG = "ContextMemoryManager"

        private val SENSITIVE_PATTERNS = listOf(
            Regex("(?i)\\b(otp|one\\s*time\\s*password|verification\\s*code)\\b"),
            Regex("(?i)\\b(password|passcode|mpin|upi\\s*pin|atm\\s*pin)\\b"),
            Regex("\\b(?:4[0-9]{12}(?:[0-9]{3})?|5[1-5][0-9]{14}|3[47][0-9]{13}|6(?:011|5[0-9]{2})[0-9]{12})\\b"), // Credit card
            Regex("(?i)\\b(cvv|cvc)\\b\\s*[:=]?\\s*\\d{3,4}"),
            Regex("(?i)\\b(account\\s*no|a\\/c\\s*no)\\b\\s*[:=]?\\s*\\d{9,18}")
        )
    }

    private val dbHelper = MaxDatabaseHelper.getInstance(context)

    fun containsSensitiveData(text: String): Boolean {
        for (pattern in SENSITIVE_PATTERNS) {
            if (pattern.containsMatchIn(text)) return true
        }
        return false
    }

    // ==================== USER PREFERENCES & FACTS ====================

    fun saveMemory(key: String, content: String, category: String = "PREFERENCE", tags: String = ""): Boolean {
        if (containsSensitiveData(content)) {
            Log.w(TAG, "Rejected saving memory for '$key' due to sensitive credential detection.")
            return false
        }
        val record = UserMemoryRecord(
            memoryKey = key.trim().lowercase(Locale.ROOT),
            content = content.trim(),
            category = category,
            tags = tags,
            isUserApproved = true,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        val id = dbHelper.upsertUserMemory(record)
        Log.i(TAG, "Saved user memory [$id] '$key': '$content'")
        return id > 0
    }

    fun saveUserMemory(fact: String, language: VoiceLanguage): String {
        val cleanKey = fact.take(25).trim().lowercase(Locale.ROOT).replace(Regex("[^a-z0-9_]"), "_")
        val success = saveMemory(key = "fact_$cleanKey", content = fact)
        return if (success) {
            when (language) {
                VoiceLanguage.HINDI, VoiceLanguage.HINGLISH -> "Sir, maine yeh information yaad rakh li hai: '$fact'"
                VoiceLanguage.BENGALI -> "স্যার, আমি এই তথ্যটি সংরক্ষণ করেছি: '$fact'"
                else -> "Sir, I have stored this information in persistent memory: '$fact'"
            }
        } else {
            when (language) {
                VoiceLanguage.HINDI, VoiceLanguage.HINGLISH -> "Sir, sensitive credentials ya passwords ko MAX memory mein save karna allowed nahi hai."
                VoiceLanguage.BENGALI -> "স্যার, সংবেদনশীল তথ্য বা পাসওয়ার্ড সংরক্ষণ করার অনুমতি নেই।"
                else -> "Sir, saving sensitive credentials or security keys in persistent memory is restricted."
            }
        }
    }

    fun forgetMemory(key: String): Boolean {
        val success = dbHelper.deleteUserMemoryByKey(key)
        Log.i(TAG, "Forget memory '$key': success=$success")
        return success
    }

    fun forgetMemory(query: String, language: VoiceLanguage): String {
        val success = forgetMemory(query.trim().lowercase(Locale.ROOT).replace(Regex("[^a-z0-9_]"), "_"))
        return if (success) {
            when (language) {
                VoiceLanguage.HINDI, VoiceLanguage.HINGLISH -> "Sir, '$query' ko memory se delete kar diya gaya hai."
                VoiceLanguage.BENGALI -> "স্যার, '$query' মেমরি থেকে মুছে ফেলা হয়েছে।"
                else -> "Sir, '$query' has been cleared from persistent memory."
            }
        } else {
            when (language) {
                VoiceLanguage.HINDI, VoiceLanguage.HINGLISH -> "Sir, '$query' se related koi record memory mein nahi mila."
                VoiceLanguage.BENGALI -> "স্যার, '$query' সম্পর্কিত কোনো তথ্য পাওয়া যায়নি।"
                else -> "Sir, I could not find a stored memory matching '$query'."
            }
        }
    }

    fun getAllMemoriesSummary(language: VoiceLanguage): String {
        val memories = dbHelper.getAllUserMemories()
        if (memories.isEmpty()) {
            return when (language) {
                VoiceLanguage.HINDI, VoiceLanguage.HINGLISH -> "Sir, abhi koi saved memory nahi hai."
                VoiceLanguage.BENGALI -> "স্যার, এই মুহূর্তে কোনো সংরক্ষিত মেমরি নেই।"
                else -> "Sir, there are currently no saved memories in your profile."
            }
        }

        val sb = StringBuilder()
        when (language) {
            VoiceLanguage.HINDI, VoiceLanguage.HINGLISH -> sb.append("Sir, aapke saved memories yeh hain:\n")
            VoiceLanguage.BENGALI -> sb.append("স্যার, আপনার সংরক্ষিত মেমরিগুলি হলো:\n")
            else -> sb.append("Sir, here are your saved memories:\n")
        }
        memories.take(8).forEachIndexed { idx, m ->
            sb.append("${idx + 1}. ${m.memoryKey.replace('_', ' ').replaceFirstChar { it.uppercase() }}: ${m.content}\n")
        }
        return sb.toString().trim()
    }

    fun recallUserMemories(language: VoiceLanguage): String = getAllMemoriesSummary(language)

    // ==================== PROJECT TASK TRACKING ====================

    fun saveProjectTask(projectName: String, taskTitle: String, notes: String = ""): Boolean {
        val record = ProjectTaskRecord(
            projectName = projectName.trim(),
            taskTitle = taskTitle.trim(),
            status = "PENDING",
            notes = notes.trim(),
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        val id = dbHelper.insertProjectTask(record)
        Log.i(TAG, "Project task saved [$id] for '$projectName': '$taskTitle'")
        return id > 0
    }

    fun getProjectStatusReport(projectName: String, language: VoiceLanguage): String {
        val tasks = dbHelper.getTasksForProject(projectName)
        if (tasks.isEmpty()) {
            return when (language) {
                VoiceLanguage.HINDI, VoiceLanguage.HINGLISH -> "Sir, $projectName project ke liye koi pending task saved nahi hai."
                VoiceLanguage.BENGALI -> "স্যার, $projectName প্রজেক্টের জন্য কোনো টাস্ক পাওয়া যায়নি।"
                else -> "Sir, I found no registered tasks for project $projectName."
            }
        }

        val pending = tasks.filter { it.status != "COMPLETED" }
        val completed = tasks.filter { it.status == "COMPLETED" }

        val sb = StringBuilder()
        when (language) {
            VoiceLanguage.HINDI, VoiceLanguage.HINGLISH -> {
                sb.append("Sir, aapke saved $projectName project notes ke according:\n")
                if (pending.isNotEmpty()) {
                    sb.append("Pending tasks:\n")
                    pending.forEachIndexed { i, t -> sb.append("${i + 1}. ${t.taskTitle}\n") }
                } else {
                    sb.append("Sabhi tasks completed hain!\n")
                }
                if (completed.isNotEmpty()) {
                    sb.append("Completed (${completed.size} tasks).\n")
                }
            }
            VoiceLanguage.BENGALI -> {
                sb.append("স্যার, $projectName প্রজেক্ট অনুযায়ী:\n")
                pending.forEachIndexed { i, t -> sb.append("${i + 1}. ${t.taskTitle}\n") }
            }
            else -> {
                sb.append("Sir, according to your saved notes for $projectName:\n")
                pending.forEachIndexed { i, t -> sb.append("• ${t.taskTitle}\n") }
                if (completed.isNotEmpty()) {
                    sb.append("(${completed.size} tasks completed)\n")
                }
            }
        }
        return sb.toString().trim()
    }

    fun recallProjectTasks(projectName: String, language: VoiceLanguage): String = getProjectStatusReport(projectName, language)

    // ==================== CONVERSATION RECALL ====================

    fun recordConversationTurn(sessionId: String, userMessage: String, assistantReply: String, language: VoiceLanguage) {
        val isUserSens = containsSensitiveData(userMessage)
        val isReplySens = containsSensitiveData(assistantReply)

        dbHelper.insertConversationRecord(
            ConversationRecord(
                sessionId = sessionId,
                role = "USER",
                message = if (isUserSens) "[REDACTED SENSITIVE DATA]" else userMessage,
                language = language.name,
                timestamp = System.currentTimeMillis(),
                isSensitive = isUserSens
            )
        )

        dbHelper.insertConversationRecord(
            ConversationRecord(
                sessionId = sessionId,
                role = "MAX",
                message = if (isReplySens) "[REDACTED SENSITIVE DATA]" else assistantReply,
                language = language.name,
                timestamp = System.currentTimeMillis() + 10,
                isSensitive = isReplySens
            )
        )
    }

    fun recallPastDiscussion(daysAgo: Int = 1, language: VoiceLanguage): String {
        val cal = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -daysAgo)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startEpoch = cal.timeInMillis
        val endEpoch = startEpoch + (24 * 3600 * 1000L) - 1

        val records = dbHelper.getConversationsForDateRange(startEpoch, endEpoch)
        if (records.isEmpty()) {
            val dateLabel = if (daysAgo == 1) "kal" else "$daysAgo din pehle"
            return when (language) {
                VoiceLanguage.HINDI, VoiceLanguage.HINGLISH -> "Sir, $dateLabel hamari koi baat-cheet record nahi hui thi."
                VoiceLanguage.BENGALI -> "স্যার, ওই দিন আমাদের কোনো কথোপকথন সংরক্ষিত নেই।"
                else -> "Sir, I have no recorded conversation from that day."
            }
        }

        val userTopics = records.filter { it.role == "USER" && it.message.length > 5 }
            .takeLast(4)
            .map { it.message }

        return when (language) {
            VoiceLanguage.HINDI, VoiceLanguage.HINGLISH -> {
                "Sir, kal humne yeh topics discuss kiye the: " + userTopics.joinToString("; ") { "'$it'" }
            }
            VoiceLanguage.BENGALI -> {
                "স্যার, পূর্ববর্তী আলোচনা অনুযায়ী: " + userTopics.joinToString("; ") { "'$it'" }
            }
            else -> {
                "Sir, yesterday we discussed: " + userTopics.joinToString("; ") { "'$it'" }
            }
        }
    }

    fun recallYesterdayDiscussion(language: VoiceLanguage): String = recallPastDiscussion(daysAgo = 1, language = language)

    // ==================== CONTEXT ENRICHMENT FOR GEMINI ====================

    fun getPromptContextEnrichment(): String {
        val memories = dbHelper.getAllUserMemories()
        val projects = dbHelper.getAllProjects()
        if (memories.isEmpty() && projects.isEmpty()) return ""

        val sb = StringBuilder()
        if (memories.isNotEmpty()) {
            sb.append("\n[USER APPROVED MEMORIES & FACTS]:\n")
            memories.take(5).forEach { m ->
                sb.append("- ${m.memoryKey}: ${m.content}\n")
            }
        }
        if (projects.isNotEmpty()) {
            sb.append("\n[USER ACTIVE PROJECTS]:\n")
            for (p in projects.take(3)) {
                val tasks = dbHelper.getTasksForProject(p).filter { it.status != "COMPLETED" }.take(3)
                sb.append("• Project $p: ")
                if (tasks.isNotEmpty()) {
                    sb.append(tasks.joinToString(", ") { it.taskTitle })
                } else {
                    sb.append("All tasks up to date.")
                }
                sb.append("\n")
            }
        }
        return sb.toString().trim()
    }

    fun buildEnrichedContext(input: String): String {
        val lower = input.lowercase(Locale.ROOT)
        val sb = StringBuilder()

        val memories = dbHelper.searchUserMemories(lower).take(3)
        if (memories.isNotEmpty()) {
            sb.append("\n[User Context Memories]:\n")
            memories.forEach { m ->
                sb.append("- ${m.memoryKey}: ${m.content}\n")
            }
        }

        val projects = dbHelper.getAllProjects()
        for (proj in projects) {
            if (lower.contains(proj.lowercase(Locale.ROOT))) {
                val tasks = dbHelper.getTasksForProject(proj).take(5)
                sb.append("\n[Project '${proj}' Tasks]:\n")
                tasks.forEach { t ->
                    sb.append("- [${t.status}] ${t.taskTitle} (Notes: ${t.notes})\n")
                }
            }
        }

        return sb.toString().trim()
    }
}
