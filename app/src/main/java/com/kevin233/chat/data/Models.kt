package com.kevin233.chat.data

import java.util.UUID

data class ApiConfig(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val type: String = "openai",
    val baseUrl: String = "",
    val apiKey: String = "",
    val model: String = "",
    val presetId: String = "",
    val vision: Boolean = false
)

data class Message(
    val id: String = UUID.randomUUID().toString(),
    val role: String = "user",
    val content: String = "",
    val reasoning: String = "",
    val images: List<String> = emptyList(),
    val searchContext: String = "",
    val cacheHit: Int = 0,
    val cacheMiss: Int = 0,
    val outTokens: Int = 0,
    val error: String = "",
    val ts: Long = System.currentTimeMillis()
) {
    val isUser: Boolean get() = role == "user"
}

data class Conversation(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "新的对话",
    val messages: List<Message> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val systemPrompt: String = "",
    val started: Boolean = false,
    val modelKey: String = "",
    val webSearch: Boolean = false,
    val reasoning: Int = 0,
    val agentId: String = "",
    val agentName: String = "",
    val agentEmoji: String = "",
    val agentAvatar: String = ""
) {
    val hitTokens: Int get() = messages.sumOf { it.cacheHit }
    val missTokens: Int get() = messages.sumOf { it.cacheMiss }
    val outTokens: Int get() = messages.sumOf { it.outTokens }
}

data class Agent(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val emoji: String = "",
    val tagline: String = "",
    val prompt: String = "",
    val webSearch: Boolean = false,
    val reasoning: Int = 0,
    val builtin: Boolean = false,
    val avatarPath: String = ""
)

data class Skill(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val description: String = "",
    val content: String = "",
    val builtin: Boolean = false
)

data class Settings(
    val configs: List<ApiConfig> = emptyList(),
    val activeConfigId: String = "",
    val systemPrompt: String = "",
    val anchor: String = "",
    val customAgents: List<Agent> = emptyList(),
    val temperature: Float = 0.7f,
    val maxTokens: Int = 8192,
    val reasoning: Int = 0,
    val themeMode: Int = 0,
    val wallpaper: String = "none",
    val wallpaperPath: String = "",
    val glassAlpha: Float = 0.82f,
    val searchProvider: String = "duckduckgo",
    val searchTool: Boolean = true,
    val customSkills: List<Skill> = emptyList(),
    val streaming: Boolean = true,
    val includeUsage: Boolean = true,
    val anthropicLongCache: Boolean = false,
    val geminiExplicitCache: Boolean = false,
    val onboarded: Boolean = false,
    val totalHit: Long = 0,
    val totalMiss: Long = 0,
    val totalOut: Long = 0,
    val sendOnEnter: Boolean = false
)

val DefaultSystemPrompt = "你是一个乐于助人的 AI 助手。回答准确、简洁、有条理；不知道就直说不知道。"

fun composePrompt(system: String, anchor: String): String {
    val stable = anchor.trim()
    if (stable.isEmpty()) return system
    val base = system.trimEnd()
    if (base.isEmpty()) return "【固定资料，请始终作为背景参考】\n$stable"
    return base + "\n\n【固定资料，请始终作为背景参考】\n" + stable
}

fun ApiConfig.cacheFingerprint(): String =
    listOf(type, baseUrl.trim().trimEnd('/').lowercase(), model.trim().lowercase()).joinToString("|")

object ApiTypes {
    const val OPENAI = "openai"
    const val ANTHROPIC = "anthropic"
    const val GEMINI = "gemini"

    fun label(t: String): String = when (t) {
        ANTHROPIC -> "Claude"
        GEMINI -> "Gemini"
        else -> "OpenAI 兼容"
    }
}
