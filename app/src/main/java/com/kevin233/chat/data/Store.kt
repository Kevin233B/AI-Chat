package com.kevin233.chat.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID

class Store(context: Context) {

    private val prefs = context.getSharedPreferences("aichat_prefs", Context.MODE_PRIVATE)
    private val dir = File(context.filesDir, "chats").apply { mkdirs() }

    fun loadSettings(): Settings {
        val raw = prefs.getString("settings", null) ?: return Settings()
        return try {
            fromJson(JSONObject(raw))
        } catch (e: Exception) {
            Settings()
        }
    }

    fun saveSettings(s: Settings) {
        prefs.edit().putString("settings", toJson(s).toString()).apply()
    }

    fun loadConversations(): List<Conversation> {
        val list = mutableListOf<Conversation>()
        dir.listFiles()?.forEach { f ->
            try {
                if (f.isFile && f.name.endsWith(".json")) list.add(sanitize(convFromJson(JSONObject(f.readText()))))
            } catch (_: Exception) {
            }
        }
        return list.sortedByDescending { it.updatedAt }
    }

    fun saveConversation(c: Conversation) {
        try {
            File(dir, "${c.id}.json").writeText(convToJson(c).toString())
        } catch (_: Exception) {
        }
    }

    fun deleteConversation(id: String) {
        File(dir, "$id.json").delete()
    }

    fun deleteAll() {
        dir.listFiles()?.forEach { it.delete() }
    }

    private fun toJson(s: Settings): JSONObject {
        val o = JSONObject()
        val arr = JSONArray()
        s.configs.forEach { c ->
            arr.put(JSONObject().apply {
                put("id", c.id)
                put("name", c.name)
                put("type", c.type)
                put("baseUrl", c.baseUrl)
                put("apiKey", c.apiKey)
                put("model", c.model)
                put("presetId", c.presetId)
                put("vision", c.vision)
            })
        }
        o.put("configs", arr)
        o.put("activeConfigId", s.activeConfigId)
        o.put("systemPrompt", s.systemPrompt)
        o.put("anchor", s.anchor)
        val agents = JSONArray()
        s.customAgents.forEach { agent ->
            agents.put(JSONObject().apply {
                put("id", agent.id)
                put("name", agent.name)
                put("emoji", agent.emoji)
                put("tagline", agent.tagline)
                put("prompt", agent.prompt)
                put("webSearch", agent.webSearch)
                put("reasoning", agent.reasoning)
                put("avatarPath", agent.avatarPath)
            })
        }
        o.put("customAgents", agents)
        o.put("wallpaper", s.wallpaper)
        o.put("wallpaperPath", s.wallpaperPath)
        o.put("glassAlpha", s.glassAlpha.toDouble())
        o.put("searchProvider", s.searchProvider)
        o.put("searchTool", s.searchTool)
        val skills = JSONArray()
        s.customSkills.forEach { skill ->
            skills.put(JSONObject().apply {
                put("id", skill.id)
                put("name", skill.name)
                put("description", skill.description)
                put("content", skill.content)
            })
        }
        o.put("customSkills", skills)
        o.put("temperature", s.temperature.toDouble())
        o.put("maxTokens", s.maxTokens)
        o.put("reasoning", s.reasoning)
        o.put("themeMode", s.themeMode)
        o.put("streaming", s.streaming)
        o.put("includeUsage", s.includeUsage)
        o.put("anthropicLongCache", s.anthropicLongCache)
        o.put("geminiExplicitCache", s.geminiExplicitCache)
        o.put("onboarded", s.onboarded)
        o.put("totalHit", s.totalHit)
        o.put("totalMiss", s.totalMiss)
        o.put("totalOut", s.totalOut)
        o.put("sendOnEnter", s.sendOnEnter)
        return o
    }

    private fun fromJson(o: JSONObject): Settings {
        val configs = mutableListOf<ApiConfig>()
        val arr = o.optJSONArray("configs") ?: JSONArray()
        for (i in 0 until arr.length()) {
            val c = arr.optJSONObject(i) ?: continue
            configs.add(
                ApiConfig(
                    id = c.optString("id"),
                    name = c.optString("name"),
                    type = c.optString("type", "openai"),
                    baseUrl = c.optString("baseUrl"),
                    apiKey = c.optString("apiKey"),
                    model = c.optString("model"),
                    presetId = c.optString("presetId"),
                    vision = c.optBoolean("vision", false)
                )
            )
        }
        return Settings(
            configs = configs,
            activeConfigId = o.optString("activeConfigId"),
            systemPrompt = o.optString("systemPrompt"),
            anchor = o.optString("anchor"),
            customAgents = run {
                val list = mutableListOf<Agent>()
                val agents = o.optJSONArray("customAgents") ?: JSONArray()
                for (i in 0 until agents.length()) {
                    val a = agents.optJSONObject(i) ?: continue
                    if (a.optString("name").isBlank()) continue
                    list.add(
                        Agent(
                            id = a.optString("id").ifBlank { java.util.UUID.randomUUID().toString() },
                            name = a.optString("name"),
                            emoji = a.optString("emoji", "🤖"),
                            tagline = a.optString("tagline"),
                            prompt = a.optString("prompt"),
                            webSearch = a.optBoolean("webSearch", false),
                            reasoning = a.optInt("reasoning", 0),
                            avatarPath = a.optString("avatarPath")
                        )
                    )
                }
                list
            },
            wallpaper = o.optString("wallpaper", "none"),
            wallpaperPath = o.optString("wallpaperPath"),
            glassAlpha = o.optDouble("glassAlpha", 0.82).toFloat(),
            searchProvider = o.optString("searchProvider", "duckduckgo"),
            searchTool = o.optBoolean("searchTool", true),
            customSkills = run {
                val list = mutableListOf<Skill>()
                val skills = o.optJSONArray("customSkills") ?: JSONArray()
                for (i in 0 until skills.length()) {
                    val skill = skills.optJSONObject(i) ?: continue
                    if (skill.optString("name").isBlank() || skill.optString("content").isBlank()) continue
                    list.add(
                        Skill(
                            id = skill.optString("id").ifBlank { java.util.UUID.randomUUID().toString() },
                            name = skill.optString("name"),
                            description = skill.optString("description"),
                            content = skill.optString("content")
                        )
                    )
                }
                list
            },
            temperature = o.optDouble("temperature", 0.7).toFloat(),
            maxTokens = o.optInt("maxTokens", 8192),
            reasoning = o.optInt("reasoning", 0),
            themeMode = o.optInt("themeMode", 0),
            streaming = o.optBoolean("streaming", true),
            includeUsage = o.optBoolean("includeUsage", true),
            anthropicLongCache = o.optBoolean("anthropicLongCache", false),
            geminiExplicitCache = o.optBoolean("geminiExplicitCache", false),
            onboarded = o.optBoolean("onboarded", false),
            totalHit = o.optLong("totalHit"),
            totalMiss = o.optLong("totalMiss"),
            totalOut = o.optLong("totalOut"),
            sendOnEnter = o.optBoolean("sendOnEnter", false)
        )
    }

    private fun sanitize(conv: Conversation): Conversation {
        if (conv.messages.none { it.role == "assistant" && it.content.isBlank() && it.reasoning.isBlank() && it.error.isBlank() }) {
            return conv
        }
        return conv.copy(
            messages = conv.messages.filterNot {
                it.role == "assistant" && it.content.isBlank() && it.reasoning.isBlank() && it.error.isBlank()
            }
        )
    }

    fun exportConversations(conversations: List<Conversation>): String {
        val root = JSONObject()
        root.put("app", "AI Chat")
        root.put("version", 1)
        root.put("exportedAt", System.currentTimeMillis())
        val arr = JSONArray()
        conversations.sortedByDescending { it.updatedAt }.forEach { arr.put(convToJson(it)) }
        root.put("conversations", arr)
        return root.toString(2)
    }

    fun parseConversations(text: String): List<Conversation> {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return emptyList()
        return try {
            if (trimmed.startsWith("[")) {
                val array = JSONArray(trimmed)
                val own = parseOwnArray(array)
                if (own.isNotEmpty()) own else parseForeignArray(array)
            } else {
                val obj = JSONObject(trimmed)
                val own = parseOwnArray(obj.optJSONArray("conversations"))
                if (own.isNotEmpty()) {
                    own
                } else {
                    val array = obj.optJSONArray("conversations")
                        ?: obj.optJSONArray("data")
                        ?: obj.optJSONArray("chats")
                    val foreign = if (array != null) parseForeignArray(array) else emptyList()
                    if (foreign.isNotEmpty()) foreign else {
                        val single = parseForeignConversation(obj)
                        if (single != null) listOf(single) else emptyList()
                    }
                }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun parseOwnArray(array: JSONArray?): List<Conversation> {
        if (array == null) return emptyList()
        val list = mutableListOf<Conversation>()
        for (i in 0 until array.length()) {
            val o = array.optJSONObject(i) ?: continue
            if (!o.has("systemPrompt") && !o.has("modelKey") && !o.has("started")) continue
            try {
                val conv = sanitize(convFromJson(o))
                if (conv.id.isNotBlank() && conv.messages.isNotEmpty()) list.add(conv)
            } catch (_: Exception) {
            }
        }
        return list
    }

    private fun parseForeignArray(array: JSONArray): List<Conversation> {
        val list = mutableListOf<Conversation>()
        for (i in 0 until array.length()) {
            val o = array.optJSONObject(i) ?: continue
            val conv = parseForeignConversation(o) ?: continue
            list.add(conv)
        }
        return list
    }

    private fun parseForeignConversation(o: JSONObject): Conversation? {
        val rawMessages = o.optJSONArray("chat_messages")
            ?: o.optJSONArray("messages")
            ?: o.optJSONArray("chat_history")
            ?: o.optJSONArray("turns")
            ?: o.optJSONArray("dialogue")
            ?: return null
        val messages = mutableListOf<Message>()
        for (i in 0 until rawMessages.length()) {
            val raw = rawMessages.optJSONObject(i) ?: continue
            val message = parseForeignMessage(raw) ?: continue
            messages.add(message)
        }
        if (messages.isEmpty()) return null
        val createdAt = parseTime(
            o.opt("created_at") ?: o.opt("createdAt") ?: o.opt("create_time") ?: o.opt("timestamp")
        )
        val updatedAt = parseTime(
            o.opt("updated_at") ?: o.opt("updatedAt") ?: o.opt("update_time") ?: o.opt("timestamp")
        ).takeIf { it > 0 } ?: messages.lastOrNull()?.ts ?: createdAt
        val sourceId = o.optString("uuid").ifBlank { o.optString("id") }
        val title = o.optString("name").ifBlank { o.optString("title") }
            .ifBlank { o.optString("subject") }
            .ifBlank { messages.firstOrNull { it.isUser }?.content?.take(24)?.replace("\n", " ") ?: "导入的对话" }
        val safeId = sourceId.filter { it.isLetterOrDigit() || it == '-' || it == '_' }.take(80)
        return Conversation(
            id = "imported-" + safeId.ifBlank { UUID.randomUUID().toString() },
            title = title,
            messages = messages,
            createdAt = createdAt.takeIf { it > 0 } ?: messages.first().ts,
            updatedAt = updatedAt,
            started = true
        )
    }

    private fun parseForeignMessage(o: JSONObject): Message? {
        val roleRaw = o.optString("role").ifBlank { o.optString("sender") }
            .ifBlank { o.optString("author") }.lowercase()
        val role = when (roleRaw) {
            "user", "human", "customer" -> "user"
            "assistant", "ai", "bot", "model", "claude" -> "assistant"
            else -> return null
        }
        val content = extractText(o.opt("content") ?: o.opt("text") ?: o.opt("message") ?: o.opt("value"))
        if (content.isBlank()) return null
        val ts = parseTime(
            o.opt("created_at") ?: o.opt("createdAt") ?: o.opt("create_time")
                ?: o.opt("timestamp") ?: o.opt("ts")
        )
        return Message(
            id = o.optString("uuid").ifBlank { o.optString("id") }.ifBlank { UUID.randomUUID().toString() },
            role = role,
            content = content,
            ts = ts.takeIf { it > 0 } ?: System.currentTimeMillis()
        )
    }

    private fun extractText(value: Any?): String {
        return when (value) {
            null -> ""
            is String -> value
            is JSONArray -> {
                val parts = mutableListOf<String>()
                for (i in 0 until value.length()) {
                    val item = value.opt(i)
                    val text = when (item) {
                        is String -> item
                        is JSONObject -> item.optString("text").ifBlank { item.optString("content") }
                        else -> ""
                    }
                    if (text.isNotBlank()) parts.add(text)
                }
                parts.joinToString("\n")
            }
            is JSONObject -> value.optString("text").ifBlank { value.optString("content") }
            else -> ""
        }.trim()
    }

    private fun parseTime(value: Any?): Long {
        if (value == null) return 0L
        if (value is Number) {
            val n = value.toLong()
            return if (n in 1..9_999_999_999L) n * 1000L else n
        }
        val raw = value.toString().trim()
        if (raw.isBlank()) return 0L
        raw.toLongOrNull()?.let { n -> return if (n in 1..9_999_999_999L) n * 1000L else n }
        val patterns = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
            "yyyy-MM-dd'T'HH:mm:ssXXX",
            "yyyy-MM-dd HH:mm:ss",
            "yyyy/MM/dd HH:mm:ss"
        )
        for (pattern in patterns) {
            try {
                val format = SimpleDateFormat(pattern, Locale.US)
                format.timeZone = TimeZone.getTimeZone("UTC")
                val date = format.parse(raw) ?: continue
                return date.time
            } catch (_: Exception) {
            }
        }
        return 0L
    }

    private fun msgToJson(m: Message): JSONObject = JSONObject().apply {
        put("id", m.id)
        put("role", m.role)
        put("content", m.content)
        put("reasoning", m.reasoning)
        val imageArr = JSONArray()
        m.images.forEach { imageArr.put(it) }
        put("images", imageArr)
        put("searchContext", m.searchContext)
        put("cacheHit", m.cacheHit)
        put("cacheMiss", m.cacheMiss)
        put("outTokens", m.outTokens)
        put("error", m.error)
        put("ts", m.ts)
    }

    private fun msgFromJson(o: JSONObject): Message = Message(
        id = o.optString("id"),
        role = o.optString("role", "user"),
        content = o.optString("content"),
        reasoning = o.optString("reasoning"),
        images = run {
            val arr = o.optJSONArray("images") ?: JSONArray()
            (0 until arr.length()).mapNotNull { arr.optString(it).takeIf { v -> v.isNotBlank() } }
        },
        searchContext = o.optString("searchContext"),
        cacheHit = o.optInt("cacheHit"),
        cacheMiss = o.optInt("cacheMiss"),
        outTokens = o.optInt("outTokens"),
        error = o.optString("error"),
        ts = o.optLong("ts", System.currentTimeMillis())
    )

    private fun convToJson(c: Conversation): JSONObject = JSONObject().apply {
        put("id", c.id)
        put("title", c.title)
        put("createdAt", c.createdAt)
        put("updatedAt", c.updatedAt)
        put("systemPrompt", c.systemPrompt)
        put("started", c.started)
        put("modelKey", c.modelKey)
        put("webSearch", c.webSearch)
        put("reasoning", c.reasoning)
        put("agentId", c.agentId)
        put("agentName", c.agentName)
        put("agentEmoji", c.agentEmoji)
        put("agentAvatar", c.agentAvatar)
        val arr = JSONArray()
        c.messages.forEach { arr.put(msgToJson(it)) }
        put("messages", arr)
    }

    private fun convFromJson(o: JSONObject): Conversation {
        val messages = mutableListOf<Message>()
        val arr = o.optJSONArray("messages") ?: JSONArray()
        for (i in 0 until arr.length()) {
            arr.optJSONObject(i)?.let { messages.add(msgFromJson(it)) }
        }
        return Conversation(
            id = o.optString("id"),
            title = o.optString("title", "新的对话"),
            messages = messages,
            createdAt = o.optLong("createdAt", System.currentTimeMillis()),
            updatedAt = o.optLong("updatedAt", System.currentTimeMillis()),
            systemPrompt = o.optString("systemPrompt"),
            started = o.optBoolean("started"),
            modelKey = o.optString("modelKey"),
            webSearch = o.optBoolean("webSearch", false),
            reasoning = o.optInt("reasoning", 0),
            agentId = o.optString("agentId"),
            agentName = o.optString("agentName"),
            agentEmoji = o.optString("agentEmoji"),
            agentAvatar = o.optString("agentAvatar")
        )
    }
}
