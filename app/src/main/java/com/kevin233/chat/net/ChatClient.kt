package com.kevin233.chat.net

import com.kevin233.chat.data.ApiConfig
import com.kevin233.chat.data.ApiTypes
import com.kevin233.chat.data.CacheMode
import com.kevin233.chat.data.Message
import com.kevin233.chat.data.Presets
import com.kevin233.chat.util.Tokens
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

data class Usage(val hit: Int = 0, val miss: Int = 0, val out: Int = 0) {
    val rate: Float
        get() {
            val total = hit + miss
            return if (total <= 0) 0f else hit.toFloat() / total
        }
}

data class Delta(
    val text: String = "",
    val reasoning: String = "",
    val usage: Usage? = null,
    val toolQuery: String = "",
    val toolName: String = ""
)

class ChatException(message: String, val code: Int = 0) : Exception(message)

object Prompts {
    fun buildSystem(system: String, config: ApiConfig): String = system
}

class ChatClient {

    private data class GeminiCache(val name: String, val expireAt: Long)

    private val geminiCaches = mutableMapOf<String, GeminiCache>()

    private val geminiCacheFailures = mutableMapOf<String, Long>()

    @Volatile private var conn: HttpURLConnection? = null
    @Volatile private var canceled = false

    fun cancel() {
        canceled = true
        try {
            conn?.disconnect()
        } catch (_: Exception) {
        }
    }

    fun stream(
        config: ApiConfig,
        messages: List<Message>,
        system: String,
        temperature: Float,
        maxTokens: Int,
        includeUsage: Boolean,
        longCache: Boolean,
        geminiExplicit: Boolean,
        sessionKey: String,
        useStream: Boolean,
        reasoning: Int = 0,
        webSearch: Boolean = false,
        toolSearch: Boolean = false,
        searchProvider: String = "duckduckgo"
    ): Flow<Delta> = flow {
        canceled = false
        val profile = Presets.cacheProfile(config)
        var usage = includeUsage
        var features = reasoning > 0 || webSearch
        var long = longCache
        var anthropicCache = true
        var attempts = 0
        while (true) {
            attempts++
            val level = if (features) reasoning else 0
            val search = features && webSearch
            val attempt = when (config.type) {
                ApiTypes.ANTHROPIC -> streamAnthropic(
                    config, messages, system, temperature, maxTokens, long, useStream, profile.minPrefix, level, search,
                    anthropicCache
                )
                ApiTypes.GEMINI -> streamGemini(
                    config, messages, system, temperature, maxTokens, geminiExplicit, useStream, level, search
                )
                else -> streamOpenAi(
                    config, messages, system, temperature, maxTokens, usage, long, sessionKey, useStream,
                    level, if (toolSearch) false else search, toolSearch, searchProvider
                )
            }
            try {
                emitAll(attempt)
                break
            } catch (e: ChatException) {
                val detail = e.message.orEmpty()
                if (attempts >= 6) throw e
                if (usage && includeUsage &&
                    (detail.contains("stream_options", true) || detail.contains("include_usage", true))
                ) {
                    usage = false
                    continue
                }
                if (features && looksLikeFeatureError(detail)) {
                    features = false
                    continue
                }
                if (config.type == ApiTypes.ANTHROPIC && long &&
                    (detail.contains("ttl", true) || detail.contains("beta", true))
                ) {
                    long = false
                    continue
                }
                if (config.type == ApiTypes.ANTHROPIC && anthropicCache &&
                    (detail.contains("cache_control", true) || detail.contains("prompt caching", true) ||
                        detail.contains("cache creation", true))
                ) {
                    anthropicCache = false
                    continue
                }
                throw e
            }
        }
    }.flowOn(Dispatchers.IO)

    private fun streamOpenAi(
        config: ApiConfig,
        messages: List<Message>,
        system: String,
        temperature: Float,
        maxTokens: Int,
        includeUsage: Boolean,
        longCache: Boolean,
        sessionKey: String,
        useStream: Boolean,
        reasoning: Int,
        webSearch: Boolean,
        toolSearch: Boolean,
        searchProvider: String
    ): Flow<Delta> = flow {
        val base = normalizeBase(config.baseUrl)
        val url = "$base/chat/completions"
        val baseLower = base.lowercase()
        val isOpenRouter = baseLower.contains("openrouter.ai")
        val isOpenAi = baseLower.contains("api.openai.com")
        val modelLower = config.model.lowercase()
        var modelName = config.model
        if (webSearch && isOpenRouter && !modelName.endsWith(":online")) modelName += ":online"
        val body = JSONObject()
        body.put("model", modelName)
        if (isOpenAi || isOpenRouter) {
            body.put("prompt_cache_key", stableCacheKey(config.model, system))
        }
        if (isOpenRouter && sessionKey.isNotBlank()) {
            body.put("session_id", sessionKey.take(256))
        }
        if (isOpenRouter && modelLower.contains("claude")) {
            val cacheControl = JSONObject().put("type", "ephemeral")
            if (longCache) cacheControl.put("ttl", "1h")
            body.put("cache_control", cacheControl)
        }
        if (reasoning > 0 && isOpenAiReasoningModel(modelLower)) {
            val effort = when (reasoning) {
                1 -> "low"
                2 -> "medium"
                3 -> "high"
                4 -> "xhigh"
                else -> "max"
            }
            val capped = if (reasoning >= 4 && !supportsDeepEffort(modelLower)) "high" else effort
            body.put("reasoning_effort", capped)
        }
        if (reasoning > 0 && modelLower.contains("qwen3")) {
            body.put("enable_thinking", true)
            body.put("thinking_budget", when (reasoning) {
                1 -> 1024
                2 -> 4096
                3 -> 8192
                4 -> 16384
                else -> 24576
            })
        }
        if (reasoning > 0 && isGlmReasoningModel(modelLower)) {
            body.put("thinking", JSONObject().put("type", "enabled"))
        }
        if (webSearch && isOpenAi && modelLower.contains("search")) {
            body.put("web_search_options", JSONObject())
        }
        if (webSearch && baseLower.contains("dashscope")) {
            body.put("enable_search", true)
        }
        if (toolSearch) {
            body.put("tools", JSONArray().put(SearchClient.toolSpec(searchProvider)))
            body.put("tool_choice", "auto")
        }
        val arr = JSONArray()
        if (system.isNotBlank()) arr.put(JSONObject().put("role", "system").put("content", system))
        messages.forEach { m ->
            val images = if (m.isUser) m.images else emptyList()
            if (images.isNotEmpty()) {
                val parts = JSONArray()
                if (m.content.isNotBlank() || m.searchContext.isNotBlank()) {
                    parts.put(JSONObject().put("type", "text").put("text", cleanForApi(apiText(m))))
                }
                images.forEach { image ->
                    parts.put(
                        JSONObject()
                            .put("type", "image_url")
                            .put("image_url", JSONObject().put("url", image))
                    )
                }
                arr.put(JSONObject().put("role", "user").put("content", parts))
            } else if (m.content.isNotBlank() || m.searchContext.isNotBlank()) {
                arr.put(JSONObject().put("role", m.role).put("content", cleanForApi(apiText(m))))
            }
        }
        body.put("messages", arr)
        if (!isReasoningOnlyModel(config.model)) body.put("temperature", temperature.toDouble())
        if (maxTokens > 0) {
            if (isNewOpenAiModel(config.model)) body.put("max_completion_tokens", maxTokens)
            else body.put("max_tokens", maxTokens)
        }
        body.put("stream", useStream)
        if (useStream && includeUsage) {
            body.put("stream_options", JSONObject().put("include_usage", true))
        }
        val headers = mapOf(
            "Content-Type" to "application/json",
            "Accept" to if (useStream) "text/event-stream" else "application/json",
            "Authorization" to "Bearer ${config.apiKey.trim()}"
        )
        if (!useStream) {
            val text = request(url, body.toString(), headers)
            val obj = JSONObject(text)
            val choice = obj.optJSONArray("choices")?.optJSONObject(0)
            val msg = choice?.optJSONObject("message")
            val content = msg?.let { str(it, "content") } ?: ""
            val reasoning = msg?.let { str(it, "reasoning_content") } ?: ""
            val usage = parseOpenAiUsage(obj.optJSONObject("usage"))
            val toolQuery = parseToolCall(msg?.optJSONArray("tool_calls"))
            emit(Delta(content, reasoning, usage, toolQuery, SearchClient.TOOL_NAME))
            return@flow
        }
        streamSse(url, body.toString(), headers) { data ->
            if (data == "[DONE]") return@streamSse true
            val obj = try {
                JSONObject(data)
            } catch (e: Exception) {
                return@streamSse false
            }
            val err = jo(obj, "error")
            if (err != null) throw ChatException(str(err, "message").ifBlank { "请求被拒绝" })
            val usage = parseOpenAiUsage(jo(obj, "usage"))
            val choice = obj.optJSONArray("choices")?.optJSONObject(0)
            val delta = choice?.let { jo(it, "delta") }
            val content = delta?.let { str(it, "content") } ?: ""
            val reasoning = delta?.let { str(it, "reasoning_content") } ?: delta?.let { str(it, "reasoning") } ?: ""
            val toolQuery = parseToolCall(delta?.optJSONArray("tool_calls"))
            if (content.isNotEmpty() || reasoning.isNotEmpty() || usage != null || toolQuery.isNotBlank()) {
                emit(Delta(content, reasoning, usage, toolQuery, SearchClient.TOOL_NAME))
            }
            false
        }
    }

    private fun streamAnthropic(
        config: ApiConfig,
        messages: List<Message>,
        system: String,
        temperature: Float,
        maxTokens: Int,
        longCache: Boolean,
        useStream: Boolean,
        minPrefix: Int,
        reasoning: Int,
        webSearch: Boolean,
        cacheEnabled: Boolean
    ): Flow<Delta> = flow {
        val base = normalizeBase(config.baseUrl)
        val url = if (base.endsWith("/v1")) "$base/messages" else "$base/v1/messages"
        val thinkingOn = reasoning > 0 && supportsAnthropicThinking(config.model)
        val thinkingBudget = when (reasoning) {
            1 -> 1024
            2 -> 4096
            3 -> 8192
            4 -> 16384
            else -> 28000
        }
        var outputTokens = if (maxTokens > 0) maxTokens else 4096
        if (thinkingOn) outputTokens = maxOf(outputTokens, thinkingBudget + 2048)
        val body = JSONObject()
        body.put("model", config.model)
        body.put("max_tokens", outputTokens)
        body.put("temperature", if (thinkingOn) 1.0 else temperature.toDouble())
        body.put("stream", useStream)
        if (thinkingOn) {
            body.put(
                "thinking",
                JSONObject().put("type", "enabled").put("budget_tokens", thinkingBudget)
            )
        }
        if (webSearch) {
            body.put(
                "tools",
                JSONArray().put(
                    JSONObject()
                        .put("type", "web_search_20250305")
                        .put("name", "web_search")
                        .put("max_uses", 5)
                )
            )
        }

        val cache = JSONObject().put("type", "ephemeral")
        if (longCache) cache.put("ttl", "1h")

        val history = mergeRoles(messages)
        var running = Tokens.estimate(system) + 8
        history.forEach { running += Tokens.estimate(it.content) + 4 }
        val canCache = running >= minPrefix && cacheEnabled
        if (canCache) body.put("cache_control", cache)
        if (system.isNotBlank()) {
            val sysBlock = JSONObject().put("type", "text").put("text", system)
            if (canCache) sysBlock.put("cache_control", cache)
            body.put("system", JSONArray().put(sysBlock))
        }
        val arr = JSONArray()
        history.forEach { m ->
            val images = if (m.isUser) m.images else emptyList()
            val content: Any = if (images.isNotEmpty()) {
                val blocks = JSONArray()
                images.forEach { image ->
                    val parsed = parseDataUrl(image)
                    if (parsed != null) {
                        blocks.put(
                            JSONObject()
                                .put("type", "image")
                                .put(
                                    "source",
                                    JSONObject()
                                        .put("type", "base64")
                                        .put("media_type", parsed.first)
                                        .put("data", parsed.second)
                                )
                        )
                    }
                }
                if (m.content.isNotBlank() || m.searchContext.isNotBlank()) {
                    blocks.put(JSONObject().put("type", "text").put("text", cleanForApi(apiText(m))))
                }
                blocks
            } else {
                cleanForApi(apiText(m))
            }
            arr.put(
                JSONObject()
                    .put("role", if (m.isUser) "user" else "assistant")
                    .put("content", content)
            )
        }
        body.put("messages", arr)

        val headers = mutableMapOf(
            "Content-Type" to "application/json",
            "Accept" to if (useStream) "text/event-stream" else "application/json",
            "x-api-key" to config.apiKey.trim(),
            "anthropic-version" to "2023-06-01"
        )

        if (!useStream) {
            val text = request(url, body.toString(), headers)
            val obj = JSONObject(text)
            val contentArr = obj.optJSONArray("content")
            var out = ""
            var think = ""
            for (i in 0 until (contentArr?.length() ?: 0)) {
                val b = contentArr?.optJSONObject(i) ?: continue
                when (str(b, "type")) {
                    "text" -> out += str(b, "text")
                    "thinking" -> think += str(b, "thinking")
                }
            }
            emit(Delta(out, think, parseAnthropicUsage(jo(obj, "usage"))))
            return@flow
        }

        var inputTokens = 0
        var cacheRead = 0
        var cacheWrite = 0
        var usageOut = 0
        streamSse(url, body.toString(), headers) { data ->
            val obj = try {
                JSONObject(data)
            } catch (e: Exception) {
                return@streamSse false
            }
            when (str(obj, "type")) {
                "error" -> {
                    val e = jo(obj, "error")
                    throw ChatException(e?.let { str(it, "message") } ?: "请求被拒绝")
                }
                "message_start" -> {
                    val u = jo(jo(obj, "message"), "usage")
                    if (u != null) {
                        inputTokens = u.optInt("input_tokens")
                        cacheRead = u.optInt("cache_read_input_tokens")
                        cacheWrite = u.optInt("cache_creation_input_tokens")
                        usageOut = u.optInt("output_tokens")
                    }
                }
                "content_block_delta" -> {
                    val d = jo(obj, "delta") ?: return@streamSse false
                    when (str(d, "type")) {
                        "text_delta" -> emit(Delta(text = str(d, "text")))
                        "thinking_delta" -> emit(Delta(reasoning = str(d, "thinking")))
                    }
                }
                "message_delta" -> {
                    val u = jo(obj, "usage")
                    if (u != null) usageOut = u.optInt("output_tokens")
                }
            }
            false
        }
        if (inputTokens + cacheRead + cacheWrite + usageOut > 0) {
            emit(
                Delta(
                    usage = Usage(
                        hit = cacheRead,
                        miss = inputTokens + cacheWrite,
                        out = usageOut
                    )
                )
            )
        }
    }

    private fun streamGemini(
        config: ApiConfig,
        messages: List<Message>,
        system: String,
        temperature: Float,
        maxTokens: Int,
        explicitCache: Boolean,
        useStream: Boolean,
        reasoning: Int,
        webSearch: Boolean
    ): Flow<Delta> = flow {
        val base = normalizeBase(config.baseUrl)
        val prefix = if (base.endsWith("/v1beta")) base else "$base/v1beta"
        val model = config.model.removePrefix("models/")
        val minTokens = Presets.geminiMinTokens(model)
        var cacheName: String? = null
        var cacheKey: String? = null
        if (explicitCache && system.isNotBlank() && Presets.geminiSupportsExplicit(model) &&
            Tokens.estimate(system) >= minTokens
        ) {
            val key = sha256("${config.apiKey}|$model|$system")
            cacheKey = key
            cacheName = ensureGeminiCache(config, prefix, model, system, key)
        }
        try {
            emitAll(
                geminiStream(
                    config, prefix, model, messages, system, temperature, maxTokens, useStream, cacheName,
                    reasoning, webSearch
                )
            )
        } catch (e: ChatException) {
            if (cacheName != null && (e.code == 400 || e.code == 404)) {
                cacheKey?.let { geminiCaches.remove(it) }
                emitAll(
                    geminiStream(
                        config, prefix, model, messages, system, temperature, maxTokens, useStream, null,
                        reasoning, webSearch
                    )
                )
            } else {
                throw e
            }
        }
    }

    private fun geminiStream(
        config: ApiConfig,
        prefix: String,
        model: String,
        messages: List<Message>,
        system: String,
        temperature: Float,
        maxTokens: Int,
        useStream: Boolean,
        cacheName: String?,
        reasoning: Int,
        webSearch: Boolean
    ): Flow<Delta> = flow {
        val action = if (useStream) "streamGenerateContent?alt=sse&key=" else "generateContent?key="
        val url = "$prefix/models/$model:$action${config.apiKey.trim()}"
        val modelLower = model.lowercase()
        val thinkingOn = reasoning > 0 &&
            (modelLower.startsWith("gemini-2.5") || modelLower.startsWith("gemini-3") || modelLower.startsWith("gemini-4"))
        val thinkingBudget = when (reasoning) {
            1 -> 1024
            2 -> 4096
            3 -> 8192
            4 -> 16384
            else -> 24576
        }

        val body = JSONObject()
        if (cacheName != null) {
            body.put("cachedContent", cacheName)
        } else if (system.isNotBlank()) {
            body.put(
                "systemInstruction",
                JSONObject().put("parts", JSONArray().put(JSONObject().put("text", system)))
            )
        }
        val contents = JSONArray()
        mergeRoles(messages).forEach { m ->
            val parts = JSONArray()
            val images = if (m.isUser) m.images else emptyList()
            images.forEach { image ->
                val parsed = parseDataUrl(image)
                if (parsed != null) {
                    parts.put(
                        JSONObject().put(
                            "inlineData",
                            JSONObject().put("mimeType", parsed.first).put("data", parsed.second)
                        )
                    )
                }
            }
            if (m.content.isNotBlank() || m.searchContext.isNotBlank()) {
                parts.put(JSONObject().put("text", cleanForApi(apiText(m))))
            }
            contents.put(
                JSONObject()
                    .put("role", if (m.isUser) "user" else "model")
                    .put("parts", parts)
            )
        }
        body.put("contents", contents)
        val generation = JSONObject().put("temperature", temperature.toDouble())
        val outputLimit = if (thinkingOn) {
            maxOf(maxTokens, when (reasoning) {
                5 -> 32768
                4 -> 24576
                else -> 16384
            })
        } else {
            maxTokens
        }
        if (outputLimit > 0) generation.put("maxOutputTokens", outputLimit)
        if (thinkingOn) {
            val thinking = JSONObject().put("includeThoughts", true)
            if (modelLower.startsWith("gemini-2.5")) {
                thinking.put("thinkingBudget", thinkingBudget)
            } else {
                val xhigh = modelLower.contains("neon") || modelLower.contains("hydrogen")
                thinking.put("thinkingLevel", when (reasoning) {
                    1 -> "low"
                    2 -> "medium"
                    3 -> "high"
                    4 -> if (xhigh) "xhigh" else "high"
                    else -> if (xhigh) "xhigh" else "high"
                })
            }
            generation.put("thinkingConfig", thinking)
        }
        body.put("generationConfig", generation)
        if (webSearch) {
            body.put(
                "tools",
                JSONArray().put(JSONObject().put("google_search", JSONObject()))
            )
        }
        val headers = mapOf("Content-Type" to "application/json", "Accept" to "text/event-stream")

        if (!useStream) {
            val text = request(url, body.toString(), headers)
            val obj = JSONObject(text)
            var out = ""
            var think = ""
            val parts = obj.optJSONArray("candidates")?.optJSONObject(0)?.let { jo(it, "content") }?.optJSONArray("parts")
            for (i in 0 until (parts?.length() ?: 0)) {
                val part = parts?.optJSONObject(i) ?: continue
                val value = str(part, "text")
                if (part.optBoolean("thought")) {
                    think += value
                } else if (value.isNotBlank()) {
                    out += value
                }
                val generated = geminiImagePart(part)
                if (generated != null) out += "\n\n![生成的图片]($generated)\n\n"
            }
            emit(Delta(out, think, parseGeminiUsage(jo(obj, "usageMetadata"))))
            return@flow
        }

        var lastUsage: Usage? = null
        streamSse(url, body.toString(), headers) { data ->
            val obj = try {
                JSONObject(data)
            } catch (e: Exception) {
                return@streamSse false
            }
            val err = jo(obj, "error")
            if (err != null) throw ChatException(str(err, "message"))
            val usage = parseGeminiUsage(jo(obj, "usageMetadata"))
            if (usage != null) lastUsage = usage
            val parts = obj.optJSONArray("candidates")?.optJSONObject(0)?.let { jo(it, "content") }?.optJSONArray("parts")
            for (i in 0 until (parts?.length() ?: 0)) {
                val part = parts?.optJSONObject(i) ?: continue
                val value = str(part, "text")
                if (part.optBoolean("thought")) {
                    if (value.isNotEmpty()) emit(Delta(reasoning = value))
                } else if (value.isNotEmpty()) {
                    emit(Delta(text = value))
                }
                val generated = geminiImagePart(part)
                if (generated != null) emit(Delta(text = "\n\n![生成的图片]($generated)\n\n"))
            }
            false
        }
        lastUsage?.let { emit(Delta(usage = it)) }
    }

    private fun ensureGeminiCache(
        config: ApiConfig,
        prefix: String,
        model: String,
        system: String,
        key: String
    ): String? {
        val now = System.currentTimeMillis()
        geminiCaches[key]?.let { if (it.expireAt > now + 60_000) return it.name }
        geminiCacheFailures[key]?.let { if (it > now) return null }
        return try {
            val body = JSONObject()
                .put("model", "models/$model")
                .put(
                    "systemInstruction",
                    JSONObject().put("parts", JSONArray().put(JSONObject().put("text", system)))
                )
                .put("ttl", "3600s")
                .put("displayName", "aichat-anchor")
            val headers = mapOf(
                "Content-Type" to "application/json",
                "x-goog-api-key" to config.apiKey.trim()
            )
            val text = request("$prefix/cachedContents", body.toString(), headers)
            val obj = JSONObject(text)
            val name = obj.optString("name")
            if (name.isBlank()) return null
            geminiCaches[key] = GeminiCache(name, now + 3_500_000)
            geminiCacheFailures.remove(key)
            name
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (_: Exception) {
            geminiCacheFailures[key] = now + 60_000
            null
        }
    }

    private fun stableCacheKey(model: String, system: String): String =
        "aichat-" + sha256(model + "|" + system).take(24)

    private fun sha256(text: String): String {
        val bytes = java.security.MessageDigest.getInstance("SHA-256").digest(text.toByteArray(Charsets.UTF_8))
        val sb = StringBuilder(bytes.size * 2)
        for (b in bytes) {
            val v = b.toInt() and 0xFF
            if (v < 16) sb.append('0')
            sb.append(Integer.toHexString(v))
        }
        return sb.toString()
    }

    suspend fun test(config: ApiConfig): String {
        canceled = false
        return try {
            testModels(config)
        } catch (e: ChatException) {
            if (config.type == ApiTypes.OPENAI && (e.code == 404 || e.code == 405 || e.code == 400)) {
                tinyChat(config)
                "连接成功。这个平台不提供模型列表，但聊天是通的"
            } else {
                throw e
            }
        }
    }

    private suspend fun testModels(config: ApiConfig): String {
        val base = normalizeBase(config.baseUrl)
        val headers = mutableMapOf("Content-Type" to "application/json")
        val url = when (config.type) {
            ApiTypes.ANTHROPIC -> {
                headers["x-api-key"] = config.apiKey.trim()
                headers["anthropic-version"] = "2023-06-01"
                if (base.endsWith("/v1")) "$base/models" else "$base/v1/models"
            }
            ApiTypes.GEMINI -> {
                val prefix = if (base.endsWith("/v1beta")) base else "$base/v1beta"
                "$prefix/models?key=${config.apiKey.trim()}"
            }
            else -> {
                headers["Authorization"] = "Bearer ${config.apiKey.trim()}"
                "$base/models"
            }
        }
        val text = request(url, null, headers, "GET")
        val count = try {
            val obj = JSONObject(text)
            obj.optJSONArray("data")?.length() ?: obj.optJSONArray("models")?.length() ?: 0
        } catch (e: Exception) {
            0
        }
        return if (count > 0) "连接成功，这个 Key 可以用（识别到 $count 个模型）" else "连接成功，这个 Key 可以用"
    }

    private suspend fun tinyChat(config: ApiConfig) {
        val base = normalizeBase(config.baseUrl)
        val body = JSONObject()
            .put("model", config.model)
            .put("stream", false)
            .put("max_tokens", 16)
            .put(
                "messages",
                JSONArray().put(JSONObject().put("role", "user").put("content", "hi"))
            )
        val headers = mapOf(
            "Content-Type" to "application/json",
            "Authorization" to "Bearer ${config.apiKey.trim()}"
        )
        request("$base/chat/completions", body.toString(), headers)
    }

    suspend fun listModels(config: ApiConfig): List<String> {
        canceled = false
        val base = normalizeBase(config.baseUrl)
        val headers = mutableMapOf<String, String>()
        val url = when (config.type) {
            ApiTypes.ANTHROPIC -> {
                headers["x-api-key"] = config.apiKey.trim()
                headers["anthropic-version"] = "2023-06-01"
                if (base.endsWith("/v1")) "$base/models?limit=100" else "$base/v1/models?limit=100"
            }
            ApiTypes.GEMINI -> {
                val prefix = if (base.endsWith("/v1beta")) base else "$base/v1beta"
                "$prefix/models?key=${config.apiKey.trim()}&pageSize=100"
            }
            else -> {
                headers["Authorization"] = "Bearer ${config.apiKey.trim()}"
                "$base/models"
            }
        }
        val text = request(url, null, headers, "GET")
        val result = mutableListOf<String>()
        val obj = JSONObject(text)
        if (config.type == ApiTypes.GEMINI) {
            val arr = obj.optJSONArray("models")
            for (i in 0 until (arr?.length() ?: 0)) {
                val m = arr?.optJSONObject(i) ?: continue
                val methods = m.optJSONArray("supportedGenerationMethods")
                var ok = methods == null
                if (methods != null) {
                    for (j in 0 until methods.length()) if (methods.optString(j) == "generateContent") ok = true
                }
                if (ok) result.add(str(m, "name").removePrefix("models/"))
            }
        } else {
            val arr = obj.optJSONArray("data") ?: obj.optJSONArray("models")
            for (i in 0 until (arr?.length() ?: 0)) {
                val m = arr?.optJSONObject(i) ?: continue
                val id = str(m, "id").ifBlank { str(m, "name") }
                if (id.isNotBlank()) result.add(id)
            }
        }
        return result.sorted()
    }

    private suspend fun streamSse(
        url: String,
        body: String,
        headers: Map<String, String>,
        onData: suspend (String) -> Boolean
    ) {
        if (canceled) throw ChatException("已停止")
        val connection = open(url, body, headers, "POST")
        conn = connection
        try {
            val code = connection.responseCode
            if (code !in 200..299) {
                throw ChatException(readError(connection, code), code)
            }
            val reader = BufferedReader(InputStreamReader(connection.inputStream, Charsets.UTF_8))
            var line: String?
            while (true) {
                currentCoroutineContext().ensureActive()
                if (canceled) throw ChatException("已停止")
                line = reader.readLine() ?: break
                val trimmed = line.trim()
                if (trimmed.isEmpty()) continue
                if (!trimmed.startsWith("data:")) continue
                val data = trimmed.removePrefix("data:").trim()
                if (data.isEmpty()) continue
                if (onData(data)) break
            }
        } finally {
            try {
                connection.disconnect()
            } catch (_: Exception) {
            }
            conn = null
        }
    }

    private fun request(
        url: String,
        body: String?,
        headers: Map<String, String>,
        method: String = "POST"
    ): String {
        if (canceled) throw ChatException("已停止")
        val connection = open(url, body, headers, method)
        conn = connection
        try {
            val code = connection.responseCode
            if (code !in 200..299) throw ChatException(readError(connection, code), code)
            return connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        } finally {
            try {
                connection.disconnect()
            } catch (_: Exception) {
            }
            conn = null
        }
    }

    private fun open(url: String, body: String?, headers: Map<String, String>, method: String): HttpURLConnection {
        val u = try {
            URL(url)
        } catch (e: Exception) {
            throw ChatException("接口地址写得不对，检查一下是不是少了 https://")
        }
        val connection = u.openConnection() as HttpURLConnection
        connection.connectTimeout = 20000
        connection.readTimeout = 300000
        connection.requestMethod = method
        headers.forEach { (k, v) -> connection.setRequestProperty(k, v) }
        connection.setRequestProperty("User-Agent", "AIChat/1.0 Android")
        if (body != null && method == "POST") {
            connection.doOutput = true
            connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
        }
        return connection
    }

    private fun readError(connection: HttpURLConnection, code: Int): String {
        val raw = try {
            connection.errorStream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } ?: ""
        } catch (e: Exception) {
            ""
        }
        var detail = ""
        try {
            val obj = JSONObject(raw)
            val err = jo(obj, "error")
            detail = when {
                err != null -> str(err, "message").ifBlank { err.toString() }
                obj.has("message") -> str(obj, "message")
                obj.has("msg") -> str(obj, "msg")
                obj.has("detail") -> str(obj, "detail")
                else -> ""
            }
        } catch (_: Exception) {
        }
        if (detail.isEmpty()) detail = raw.take(200)
        val hint = when (code) {
            401 -> "Key 不对或已经失效，重新复制一次试试。"
            402 -> "账户余额不够了，去官网充一点再试。"
            403 -> "这个 Key 没有权限，或者所在地区不支持。"
            404 -> "接口地址或模型名不对。地址一般要填到 /v1 结尾。"
            429 -> "请求太频繁或额度用完了，等一会儿再试。"
            in 500..599 -> "对方服务器忙不过来，过一会儿再试。"
            else -> "请求被拒绝了，看看下面的原始提示。"
        }
        return "$hint\n\n原始信息：$detail"
    }

    private fun normalizeBase(raw: String): String {
        var base = raw.trim()
        if (base.isEmpty()) throw ChatException("还没填接口地址")
        if (!base.startsWith("http")) base = "https://$base"
        base = base.trimEnd('/')
        if (base.endsWith("/chat/completions")) base = base.removeSuffix("/chat/completions")
        return base
    }

    private fun mergeRoles(messages: List<Message>): List<Message> {
        val cleaned = messages.filter { it.content.isNotBlank() }
        if (cleaned.isEmpty()) return cleaned
        val out = mutableListOf<Message>()
        var firstUser = false
        for (m in cleaned) {
            if (out.isEmpty() && !m.isUser) continue
            firstUser = true
            val last = out.lastOrNull()
            if (last != null && last.isUser == m.isUser) {
                out[out.size - 1] = last.copy(
                    content = last.content + "\n\n" + m.content,
                    images = last.images + m.images,
                    searchContext = listOf(last.searchContext, m.searchContext)
                        .filter { it.isNotBlank() }
                        .joinToString("\n\n")
                )
            } else {
                out.add(m)
            }
        }
        return if (firstUser) out else emptyList()
    }

    private fun isOpenAiReasoningModel(model: String): Boolean =
        model.startsWith("o1") || model.startsWith("o3") || model.startsWith("o4") ||
            model.startsWith("gpt-5") || model.startsWith("gpt-6") || model.contains("reasoning")

    private fun supportsDeepEffort(model: String): Boolean =
        model.contains("gpt-6") || model.contains("gpt-5.6") || model.contains("gpt-5-6") ||
            model.contains("sol") || model.contains("luna") || model.contains("astra")

    private fun isGlmReasoningModel(model: String): Boolean =
        model.contains("glm-4.5") || model.contains("glm-4.6") || model.contains("glm-5")

    private fun supportsAnthropicThinking(model: String): Boolean {
        val m = model.lowercase()
        return m.contains("claude-3-7") || m.contains("claude-4") || m.contains("claude-5") ||
            m.contains("sonnet-4") || m.contains("sonnet-5") || m.contains("opus-4") ||
            m.contains("opus-5") || m.contains("haiku-4") || m.contains("haiku-5") ||
            m.contains("fable") || m.contains("mythos")
    }

    private fun apiText(message: Message): String {
        val sb = StringBuilder()
        if (message.searchContext.isNotBlank()) {
            sb.append(message.searchContext).append("\n\n")
        }
        sb.append(message.content)
        if (message.timeNote.isNotBlank()) {
            sb.append("\n\n").append(message.timeNote)
        }
        return sb.toString()
    }

    private fun cleanForApi(text: String): String =
        text.replace(Regex("!\\[[^\\]]*]\\(data:[^)]+\\)"), "[图片]")

    private fun geminiImagePart(part: JSONObject): String? {
        val inline = jo(part, "inlineData") ?: jo(part, "inline_data") ?: return null
        val mime = str(inline, "mimeType").ifBlank { str(inline, "mime_type") }
        val data = str(inline, "data")
        if (!mime.startsWith("image/") || data.isBlank()) return null
        return "data:$mime;base64,$data"
    }

    private fun parseDataUrl(dataUrl: String): Pair<String, String>? {
        if (!dataUrl.startsWith("data:")) return null
        val comma = dataUrl.indexOf(',')
        if (comma < 0) return null
        val header = dataUrl.substring(5, comma)
        val mime = header.substringBefore(';').ifBlank { "image/jpeg" }
        val data = dataUrl.substring(comma + 1)
        if (data.isBlank()) return null
        return mime to data
    }

    private fun looksLikeFeatureError(detail: String): Boolean {
        val d = detail.lowercase()
        return d.contains("reasoning") || d.contains("thinking") || d.contains("web_search") ||
            d.contains("web search") || d.contains("enable_search") || d.contains("google_search") ||
            d.contains("web_search_options") || d.contains(":online") || d.contains("tool")
    }

    private fun isNewOpenAiModel(model: String): Boolean {
        val m = model.lowercase()
        return m.startsWith("o1") || m.startsWith("o3") || m.startsWith("o4") ||
            m.startsWith("gpt-5") || m.startsWith("gpt-6")
    }

    private fun isReasoningOnlyModel(model: String): Boolean = isNewOpenAiModel(model)

    private fun parseToolCall(calls: JSONArray?): String {
        if (calls == null) return ""
        for (i in 0 until calls.length()) {
            val call = calls.optJSONObject(i) ?: continue
            val function = jo(call, "function") ?: continue
            if (str(function, "name") != SearchClient.TOOL_NAME) continue
            val args = str(function, "arguments")
            val query = try {
                JSONObject(args).optString("query")
            } catch (e: Exception) {
                Regex("\"query\"\\s*:\\s*\"([^\"]+)").find(args)?.groupValues?.get(1).orEmpty()
            }
            if (query.isNotBlank()) return query
        }
        return ""
    }

    private fun parseOpenAiUsage(usage: JSONObject?): Usage? {
        if (usage == null) return null
        var cached = 0
        jo(usage, "prompt_tokens_details")?.let {
            if (it.has("cached_tokens")) cached = it.optInt("cached_tokens")
            if (it.has("cache_read_input_tokens")) cached = it.optInt("cache_read_input_tokens")
        }
        if (usage.has("prompt_cache_hit_tokens")) cached = usage.optInt("prompt_cache_hit_tokens")
        else if (usage.has("cached_tokens")) cached = usage.optInt("cached_tokens")
        if (usage.has("cache_read_input_tokens")) cached = usage.optInt("cache_read_input_tokens")
        val creation = if (usage.has("cache_creation_input_tokens")) usage.optInt("cache_creation_input_tokens") else 0
        val prompt = if (usage.has("prompt_tokens")) {
            usage.optInt("prompt_tokens")
        } else {
            usage.optInt("input_tokens") + cached + creation
        }
        val miss = when {
            usage.has("prompt_cache_miss_tokens") -> usage.optInt("prompt_cache_miss_tokens")
            else -> (prompt - cached).coerceAtLeast(0)
        }
        val out = when {
            usage.has("completion_tokens") -> usage.optInt("completion_tokens")
            else -> usage.optInt("output_tokens")
        }
        if (prompt == 0 && out == 0 && cached == 0 && miss == 0) return null
        return Usage(cached, miss, out)
    }

    private fun parseAnthropicUsage(usage: JSONObject?): Usage? {
        if (usage == null) return null
        val hit = usage.optInt("cache_read_input_tokens")
        val miss = usage.optInt("input_tokens") + usage.optInt("cache_creation_input_tokens")
        val out = usage.optInt("output_tokens")
        if (hit == 0 && miss == 0 && out == 0) return null
        return Usage(hit, miss, out)
    }

    private fun parseGeminiUsage(usage: JSONObject?): Usage? {
        if (usage == null) return null
        val hit = usage.optInt("cachedContentTokenCount")
        val prompt = usage.optInt("promptTokenCount")
        val out = usage.optInt("candidatesTokenCount")
        if (prompt == 0 && out == 0 && hit == 0) return null
        return Usage(hit, (prompt - hit).coerceAtLeast(0), out)
    }

    private fun str(o: JSONObject?, key: String): String {
        if (o == null || !o.has(key) || o.isNull(key)) return ""
        return o.optString(key, "")
    }

    private fun jo(o: JSONObject?, key: String): JSONObject? {
        if (o == null || !o.has(key) || o.isNull(key)) return null
        return o.optJSONObject(key)
    }
}
