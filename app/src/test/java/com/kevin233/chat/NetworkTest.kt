package com.kevin233.chat

import com.kevin233.chat.data.ApiConfig
import com.kevin233.chat.data.ApiTypes
import com.kevin233.chat.data.Message
import com.kevin233.chat.net.ChatClient
import com.kevin233.chat.net.ChatException
import com.kevin233.chat.net.Delta
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import java.io.InputStream
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.Collections
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread

class NetworkTest {

    private lateinit var serverSocket: ServerSocket
    private lateinit var serverThread: Thread
    private var port: Int = 0
    private val requests = Collections.synchronizedList(mutableListOf<String>())
    private val hits = AtomicInteger(0)
    @Volatile
    private var failCacheCreation = false

    private fun config(type: String = ApiTypes.OPENAI): ApiConfig = ApiConfig(
        name = "test",
        type = type,
        baseUrl = "http://127.0.0.1:$port/v1",
        apiKey = "sk-test",
        model = "test-model"
    )

    @Before
    fun setUp() {
        requests.clear()
        hits.set(0)
        failCacheCreation = false
        serverSocket = ServerSocket(0, 50, InetAddress.getByName("127.0.0.1"))
        port = serverSocket.localPort
        serverThread = thread(isDaemon = true, name = "mock-api") {
            while (!serverSocket.isClosed) {
                try {
                    serverSocket.accept().use { socket -> handle(socket) }
                } catch (_: Exception) {
                    if (serverSocket.isClosed) break
                }
            }
        }
    }

    @After
    fun tearDown() {
        serverSocket.close()
    }

    private fun readLine(input: InputStream): String {
        val sb = StringBuilder()
        while (true) {
            val b = input.read()
            if (b < 0 || b == '\n'.code) break
            if (b != '\r'.code) sb.append(b.toChar())
        }
        return sb.toString()
    }

    private fun handle(socket: Socket) {
        val input = socket.getInputStream()
        val requestLine = readLine(input)
        val headers = mutableMapOf<String, String>()
        while (true) {
            val line = readLine(input)
            if (line.isEmpty()) break
            val idx = line.indexOf(':')
            if (idx > 0) headers[line.substring(0, idx).trim().lowercase()] = line.substring(idx + 1).trim()
        }
        val length = headers["content-length"]?.toIntOrNull() ?: 0
        val buf = ByteArray(length)
        var read = 0
        while (read < length) {
            val n = input.read(buf, read, length - read)
            if (n < 0) break
            read += n
        }
        val body = String(buf, 0, read, Charsets.UTF_8)
        val path = requestLine.split(" ").getOrElse(1) { "" }
        requests.add(body)
        hits.incrementAndGet()
        route(socket, path, body)
    }

    private fun route(socket: Socket, path: String, body: String) {
        if (path.endsWith("/cachedContents")) {
            if (failCacheCreation) {
                json(socket, 400, """{"error":{"message":"explicit cache not supported"}}""")
            } else {
                json(socket, 200, """{"name":"cachedContents/abc","expireTime":"2099-01-01T00:00:00Z"}""")
            }
            return
        }
        if (path.endsWith("/models")) {
            json(socket, 200, """{"data":[{"id":"m1"},{"id":"m2"}]}""")
            return
        }
        if (body.contains("__error__")) {
            json(socket, 401, """{"error":{"message":"invalid key"}}""")
            return
        }
        if (body.contains("__retry__") && hits.get() == 1) {
            json(socket, 400, """{"error":{"message":"unknown field stream_options"}}""")
            return
        }
        if (path.contains("/messages")) {
            sse(
                socket,
                "event: message_start\n" +
                    "data: {\"type\":\"message_start\",\"message\":{\"usage\":{\"input_tokens\":10,\"cache_read_input_tokens\":50,\"cache_creation_input_tokens\":5,\"output_tokens\":0}}}\n\n" +
                    "event: content_block_delta\n" +
                    "data: {\"type\":\"content_block_delta\",\"delta\":{\"type\":\"text_delta\",\"text\":\"嗨\"}}\n\n" +
                    "event: message_delta\n" +
                    "data: {\"type\":\"message_delta\",\"usage\":{\"output_tokens\":3}}\n\n"
            )
            return
        }
        if (path.contains(":streamGenerateContent")) {
            sse(
                socket,
                "data: {\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"你好\"}]}}]}\n\n" +
                    "data: {\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"呀\"}]}}],\"usageMetadata\":{\"promptTokenCount\":100,\"cachedContentTokenCount\":40,\"candidatesTokenCount\":2}}\n\n"
            )
            return
        }
        sse(
            socket,
            "data: {\"choices\":[{\"delta\":{\"reasoning_content\":\"想一想\"}}]}\n\n" +
                "data: {\"choices\":[{\"delta\":{\"content\":\"你好\"}}]}\n\n" +
                "data: {\"choices\":[],\"usage\":{\"prompt_tokens\":100,\"prompt_tokens_details\":{\"cached_tokens\":80},\"completion_tokens\":5}}\n\n" +
                "data: [DONE]\n\n"
        )
    }

    private fun write(socket: Socket, code: Int, type: String, body: String) {
        val bytes = body.toByteArray(Charsets.UTF_8)
        val header = "HTTP/1.1 $code OK\r\nContent-Type: $type\r\nContent-Length: ${bytes.size}\r\nConnection: close\r\n\r\n"
        socket.getOutputStream().apply {
            write(header.toByteArray(Charsets.UTF_8))
            write(bytes)
            flush()
        }
    }

    private fun sse(socket: Socket, body: String) {
        write(socket, 200, "text/event-stream", body)
    }

    private fun json(socket: Socket, code: Int, body: String) {
        write(socket, code, "application/json", body)
    }

    private fun collect(
        client: ChatClient,
        cfg: ApiConfig,
        system: String = "你是助手",
        explicit: Boolean = false,
        sessionKey: String = "test-session",
        messages: List<Message> = listOf(Message(role = "user", content = "你好")),
        reasoning: Int = 0,
        webSearch: Boolean = false
    ): List<Delta> =
        runBlocking {
            client.stream(
                config = cfg,
                messages = messages,
                system = system,
                temperature = 0.7f,
                maxTokens = 128,
                includeUsage = true,
                longCache = false,
                geminiExplicit = explicit,
                sessionKey = sessionKey,
                useStream = true,
                reasoning = reasoning,
                webSearch = webSearch
            ).toList()
        }

    @Test
    fun openAiStreamParsesTextAndCacheUsage() {
        val deltas = collect(ChatClient(), config())
        assertEquals("想一想", deltas.joinToString("") { it.reasoning })
        assertEquals("你好", deltas.joinToString("") { it.text })
        val usage = deltas.last().usage
        assertEquals(80, usage?.hit)
        assertEquals(20, usage?.miss)
        assertEquals(5, usage?.out)
        assertEquals("test-model", JSONObject(requests.first()).optString("model"))
        assertTrue(JSONObject(requests.first()).has("stream_options"))
    }

    @Test
    fun anthropicStreamParsesUsageAndInjectsCache() {
        val longSystem = "规则".repeat(1500)
        val deltas = collect(ChatClient(), config(ApiTypes.ANTHROPIC), longSystem)
        assertEquals("嗨", deltas.joinToString("") { it.text })
        val usage = deltas.last().usage
        assertEquals(50, usage?.hit)
        assertEquals(15, usage?.miss)
        assertEquals(3, usage?.out)
        val body = JSONObject(requests.first())
        assertTrue(body.has("cache_control"))
        assertTrue(body.toString().contains("ephemeral"))
        val systemBlock = body.getJSONArray("system").getJSONObject(0)
        assertTrue(systemBlock.has("cache_control"))
    }

    @Test
    fun anthropicSkipsCacheForShortPrompt() {
        collect(ChatClient(), config(ApiTypes.ANTHROPIC), "短提示词")
        assertTrue(!requests.first().contains("cache_control"))
    }

    @Test
    fun openAiSendsStablePromptCacheKey() {
        val cfg = config().copy(baseUrl = "http://127.0.0.1:$port/api.openai.com/v1")
        val client = ChatClient()
        collect(client, cfg, sessionKey = "conv-a")
        collect(client, cfg, sessionKey = "conv-b")
        val key1 = JSONObject(requests[0]).optString("prompt_cache_key")
        val key2 = JSONObject(requests[1]).optString("prompt_cache_key")
        assertTrue(key1.startsWith("aichat-"))
        assertEquals(key1, key2)
    }

    @Test
    fun openRouterSendsSessionAndAnthropicCacheControl() {
        val cfg = config().copy(
            baseUrl = "http://127.0.0.1:$port/openrouter.ai/v1",
            model = "anthropic/claude-sonnet-4.5"
        )
        collect(ChatClient(), cfg, sessionKey = "conv-456")
        val body = JSONObject(requests.first())
        assertEquals("conv-456", body.optString("session_id"))
        assertTrue(body.optString("prompt_cache_key").startsWith("aichat-"))
        assertTrue(body.has("cache_control"))
    }

    @Test
    fun geminiExplicitCacheIsCreatedAndUsed() {
        val cfg = config(ApiTypes.GEMINI)
        val longSystem = "资料".repeat(2500)
        collect(ChatClient(), cfg, longSystem, explicit = true)
        assertEquals(2, requests.size)
        val create = JSONObject(requests[0])
        assertEquals("models/test-model", create.optString("model"))
        val generate = JSONObject(requests[1])
        assertEquals("cachedContents/abc", generate.optString("cachedContent"))
        assertTrue(!generate.has("systemInstruction"))
    }

    @Test
    fun geminiExplicitCacheFallsBack() {
        failCacheCreation = true
        val cfg = config(ApiTypes.GEMINI)
        val longSystem = "资料".repeat(2500)
        collect(ChatClient(), cfg, longSystem, explicit = true)
        assertEquals(2, requests.size)
        val generate = JSONObject(requests[1])
        assertTrue(!generate.has("cachedContent"))
        assertTrue(generate.has("systemInstruction"))
    }

    @Test
    fun geminiStreamParsesTextAndUsage() {
        val deltas = collect(ChatClient(), config(ApiTypes.GEMINI))
        assertEquals("你好呀", deltas.joinToString("") { it.text })
        val usage = deltas.last().usage
        assertEquals(40, usage?.hit)
        assertEquals(60, usage?.miss)
        assertEquals(2, usage?.out)
    }

    @Test
    fun errorMessageIsFriendly() {
        try {
            collect(ChatClient(), config().copy(model = "__error__"))
            fail("should fail")
        } catch (e: ChatException) {
            assertTrue(e.message.orEmpty().contains("Key 不对"))
            assertEquals(401, e.code)
        }
    }

    @Test
    fun retriesWithoutStreamOptions() {
        val cfg = config().copy(model = "__retry__")
        val deltas = collect(ChatClient(), cfg)
        assertEquals("你好", deltas.joinToString("") { it.text })
        assertEquals(2, hits.get())
    }

    @Test
    fun testConnectionListsModels() {
        runBlocking {
            val msg = ChatClient().test(config())
            assertTrue(msg.contains("连接成功"))
        }
    }

    @Test
    fun modelListReturnsNames() {
        runBlocking {
            val models = ChatClient().listModels(config())
            assertEquals(listOf("m1", "m2"), models)
        }
    }

    private val image = "data:image/png;base64,aGVsbG8="

    @Test
    fun openAiVisionSendsImageParts() {
        val cfg = config().copy(model = "gpt-4o")
        collect(
            ChatClient(),
            cfg,
            messages = listOf(Message(role = "user", content = "看图", images = listOf(image)))
        )
        val messages = JSONObject(requests.first()).getJSONArray("messages")
        val content = messages.getJSONObject(messages.length() - 1).getJSONArray("content")
        var found = false
        for (i in 0 until content.length()) {
            val part = content.getJSONObject(i)
            if (part.optString("type") == "image_url") {
                found = part.getJSONObject("image_url").optString("url") == image
            }
        }
        assertTrue(found)
    }

    @Test
    fun anthropicVisionSendsImageBlock() {
        val cfg = config(ApiTypes.ANTHROPIC).copy(model = "claude-sonnet-4-5")
        collect(
            ChatClient(),
            cfg,
            messages = listOf(Message(role = "user", content = "看图", images = listOf(image)))
        )
        val content = JSONObject(requests.first()).getJSONArray("messages").getJSONObject(0).getJSONArray("content")
        var found = false
        for (i in 0 until content.length()) {
            val part = content.getJSONObject(i)
            if (part.optString("type") == "image") {
                found = part.getJSONObject("source").optString("data") == "aGVsbG8="
            }
        }
        assertTrue(found)
    }

    @Test
    fun geminiVisionSendsInlineData() {
        val cfg = config(ApiTypes.GEMINI).copy(model = "gemini-2.5-flash")
        collect(
            ChatClient(),
            cfg,
            messages = listOf(Message(role = "user", content = "看图", images = listOf(image)))
        )
        val parts = JSONObject(requests.first()).getJSONArray("contents").getJSONObject(0).getJSONArray("parts")
        var found = false
        for (i in 0 until parts.length()) {
            val part = parts.getJSONObject(i)
            if (part.has("inlineData")) {
                found = part.getJSONObject("inlineData").optString("data") == "aGVsbG8="
            }
        }
        assertTrue(found)
    }

    @Test
    fun openAiReasoningLevelsFollowXhighThenMax() {
        val cfg = config().copy(baseUrl = "http://127.0.0.1:$port/api.openai.com/v1", model = "gpt-6-sol")
        val client = ChatClient()
        collect(client, cfg, reasoning = 4)
        collect(client, cfg, reasoning = 5)
        assertEquals("xhigh", JSONObject(requests[0]).optString("reasoning_effort"))
        assertEquals("max", JSONObject(requests[1]).optString("reasoning_effort"))
    }

    @Test
    fun anthropicThinkingBudgetUsesLevel() {
        val cfg = config(ApiTypes.ANTHROPIC).copy(model = "claude-sonnet-4-5")
        collect(ChatClient(), cfg, reasoning = 1)
        val body = JSONObject(requests.first())
        assertEquals(1024, body.getJSONObject("thinking").optInt("budget_tokens"))
        assertEquals(1.0, body.optDouble("temperature"), 0.001)
        assertTrue(body.optInt("max_tokens") > 1024)
    }

    @Test
    fun geminiThinkingLevelSupportsXhigh() {
        val cfg = config(ApiTypes.GEMINI).copy(model = "gemini-4-hydrogen")
        collect(ChatClient(), cfg, reasoning = 4)
        val thinking = JSONObject(requests.first()).getJSONObject("generationConfig").getJSONObject("thinkingConfig")
        assertEquals("xhigh", thinking.optString("thinkingLevel"))
        assertTrue(thinking.optBoolean("includeThoughts"))
    }

    @Test
    fun webSearchUsesVendorNativeShape() {
        collect(ChatClient(), config(ApiTypes.ANTHROPIC), webSearch = true)
        val anthropic = JSONObject(requests[0]).getJSONArray("tools").getJSONObject(0)
        assertEquals("web_search_20250305", anthropic.optString("type"))
        collect(ChatClient(), config(ApiTypes.GEMINI), webSearch = true)
        assertTrue(JSONObject(requests[1]).getJSONArray("tools").getJSONObject(0).has("google_search"))
        val cfg = config().copy(baseUrl = "http://127.0.0.1:$port/openrouter.ai/v1", model = "deepseek/deepseek-chat")
        collect(ChatClient(), cfg, webSearch = true)
        assertTrue(JSONObject(requests[2]).optString("model").endsWith(":online"))
    }
}
