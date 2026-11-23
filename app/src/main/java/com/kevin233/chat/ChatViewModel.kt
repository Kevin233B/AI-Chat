package com.kevin233.chat

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kevin233.chat.data.Agent
import com.kevin233.chat.data.ApiConfig
import com.kevin233.chat.data.ChatSearch
import com.kevin233.chat.data.Conversation
import com.kevin233.chat.data.DefaultSystemPrompt
import com.kevin233.chat.data.Message
import com.kevin233.chat.data.Preset
import com.kevin233.chat.data.Presets
import com.kevin233.chat.data.SearchHit
import com.kevin233.chat.data.Settings
import com.kevin233.chat.data.Skill
import com.kevin233.chat.data.Skills
import com.kevin233.chat.data.Store
import com.kevin233.chat.data.cacheFingerprint
import com.kevin233.chat.data.composePrompt
import com.kevin233.chat.net.ChatClient
import com.kevin233.chat.net.SearchClient
import com.kevin233.chat.net.ChatException
import com.kevin233.chat.net.Usage
import com.kevin233.chat.util.Tokens
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.ByteArrayInputStream
import java.io.IOException
import java.util.UUID
import java.util.zip.ZipInputStream

enum class Screen { CHAT, HISTORY, AGENTS, SETTINGS, CONFIG_EDIT }

class ChatViewModel(app: Application) : AndroidViewModel(app) {

    private val store = Store(app)
    private val client = ChatClient()

    var settings by mutableStateOf(
        store.loadSettings().let {
            if (it.systemPrompt.isBlank()) it.copy(systemPrompt = DefaultSystemPrompt) else it
        }
    )
        private set

    var conversations by mutableStateOf(store.loadConversations())
        private set

    var activeId by mutableStateOf(conversations.firstOrNull()?.id ?: "")
        private set

    var screen by mutableStateOf(Screen.CHAT)
    var editingConfig by mutableStateOf<ApiConfig?>(null)
    var streaming by mutableStateOf(false)
        private set
    var searching by mutableStateOf(false)
        private set
    var pendingJumpMessageId by mutableStateOf<String?>(null)
        private set
    var highlightedMessageId by mutableStateOf<String?>(null)
        private set
    var toast by mutableStateOf<String?>(null)
    var testing by mutableStateOf(false)
        private set
    var testMessage by mutableStateOf<String?>(null)
        private set
    var loadingModels by mutableStateOf(false)
        private set
    var availableModels by mutableStateOf<List<String>>(emptyList())
        private set

    private var job: Job? = null
    private var stoppedByUser = false

    init {
        if (conversations.isEmpty()) {
            createConversation(save = true)
        }
    }

    val currentConversation: Conversation?
        get() = conversations.firstOrNull { it.id == activeId } ?: conversations.firstOrNull()

    val activeConfig: ApiConfig?
        get() = settings.configs.firstOrNull { it.id == settings.activeConfigId } ?: settings.configs.firstOrNull()

    val isConfigured: Boolean
        get() = activeConfig?.let { it.apiKey.isNotBlank() && it.baseUrl.isNotBlank() && it.model.isNotBlank() } == true

    val sortedConversations: List<Conversation>
        get() = conversations.sortedByDescending { it.updatedAt }

    fun siblingsFor(conv: Conversation?): List<Conversation> {
        if (conv == null) return emptyList()
        val group = conv.branchGroupId
        if (group.isBlank()) return listOf(conv)
        val list = conversations.filter { it.branchGroupId == group || it.id == group }
        return if (list.isEmpty()) listOf(conv) else list.sortedBy { it.branchOrder }
    }

    fun switchBranch(direction: Int) {
        val conv = currentConversation ?: return
        val siblings = siblingsFor(conv)
        if (siblings.size < 2) return
        val index = siblings.indexOfFirst { it.id == conv.id }.coerceAtLeast(0)
        val next = Math.floorMod(index + direction, siblings.size)
        activeId = siblings[next].id
        screen = Screen.CHAT
    }

    fun branchFrom(messageId: String) {
        val conv = currentConversation ?: return
        val index = conv.messages.indexOfFirst { it.id == messageId }
        if (index < 0) return
        val now = System.currentTimeMillis()
        val group = conv.branchGroupId.ifBlank { conv.id }
        val siblings = siblingsFor(conv)
        val maxOrder = siblings.maxOfOrNull { it.branchOrder } ?: 0
        val oldOrder = if (conv.branchGroupId.isBlank()) 1 else conv.branchOrder
        if (conv.branchGroupId.isBlank()) {
            updateConversationById(conv.id) {
                it.copy(branchGroupId = group, branchOrder = 1, branchCreatedAt = it.createdAt)
            }
        }
        val branch = conv.copy(
            id = UUID.randomUUID().toString(),
            messages = conv.messages.take(index),
            createdAt = now,
            updatedAt = now,
            branchGroupId = group,
            branchOrder = maxOrder.coerceAtLeast(oldOrder) + 1,
            branchCreatedAt = now,
            started = true
        )
        conversations = listOf(branch) + conversations
        activeId = branch.id
        store.saveConversation(branch)
        showToast("已新建分支，旧内容保留")
    }

    val agents: List<Agent>
        get() = settings.customAgents

    val skills: List<Skill>
        get() = Skills.builtin + settings.customSkills

    fun startSkill(skill: Skill) {
        val base = settings.systemPrompt.trim().ifBlank { DefaultSystemPrompt }
        val system = base + "\n\n【技能：${skill.name}】\n" + skill.content
        val conv = Conversation(
            title = skill.name,
            systemPrompt = composePrompt(system, settings.anchor),
            started = true,
            reasoning = settings.reasoning,
            agentId = skill.id,
            agentName = skill.name,
            modelKey = activeConfig?.cacheFingerprint() ?: ""
        )
        conversations = listOf(conv) + conversations
        activeId = conv.id
        screen = Screen.CHAT
        if (!isConfigured) showToast("先配置 API，再使用这个技能")
    }

    fun saveSkill(skill: Skill) {
        val clean = skill.copy(
            name = skill.name.trim().ifBlank { "导入的技能" },
            content = skill.content.trim().ifBlank { DefaultSystemPrompt },
            builtin = false
        )
        val list = settings.customSkills
        val updated = if (list.any { it.id == clean.id }) {
            list.map { if (it.id == clean.id) clean else it }
        } else {
            list + clean
        }
        updateSettings { it.copy(customSkills = updated) }
    }

    fun deleteSkill(id: String) {
        updateSettings { it.copy(customSkills = it.customSkills.filterNot { skill -> skill.id == id }) }
    }

    fun importSkill(uri: Uri) {
        viewModelScope.launch {
            try {
                val resolver = getApplication<Application>().contentResolver
                val text = resolver.openInputStream(uri)
                    ?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
                    ?: throw IllegalStateException("这个文件打不开")
                val fallback = uri.lastPathSegment?.substringAfterLast('/')
                    ?.substringBeforeLast('.')?.takeIf { it.isNotBlank() } ?: "导入的技能"
                val skill = Skills.parse(text, fallback)
                saveSkill(skill)
                showToast("技能「${skill.name}」已安装")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                showToast("导入失败：${e.message ?: "文件格式不对"}")
            }
        }
    }

    fun startAgent(agent: Agent) {
        val conv = Conversation(
            title = agent.name,
            systemPrompt = composePrompt(agent.prompt, settings.anchor),
            started = true,
            webSearch = agent.webSearch,
            reasoning = agent.reasoning,
            agentId = agent.id,
            agentName = agent.name,
            agentAvatar = agent.avatarPath,
            modelKey = activeConfig?.cacheFingerprint() ?: ""
        )
        conversations = listOf(conv) + conversations
        activeId = conv.id
        screen = Screen.CHAT
        if (!isConfigured) showToast("先配置 API，再和它聊")
    }

    fun saveAgent(agent: Agent) {
        val clean = agent.copy(
            name = agent.name.trim().ifBlank { "未命名智能体" },
            emoji = "",
            prompt = agent.prompt.ifBlank { DefaultSystemPrompt },
            builtin = false
        )
        val list = settings.customAgents
        val updated = if (list.any { it.id == clean.id }) {
            list.map { if (it.id == clean.id) clean else it }
        } else {
            list + clean
        }
        updateSettings { it.copy(customAgents = updated) }
    }

    fun deleteAgent(id: String) {
        updateSettings { it.copy(customAgents = it.customAgents.filterNot { agent -> agent.id == id }) }
    }

    private fun createConversation(save: Boolean): Conversation {
        val conv = Conversation(
            systemPrompt = composePrompt(settings.systemPrompt, settings.anchor),
            modelKey = activeConfig?.cacheFingerprint() ?: "",
            reasoning = settings.reasoning
        )
        conversations = listOf(conv) + conversations
        activeId = conv.id
        if (save) store.saveConversation(conv)
        return conv
    }

    fun newChat() {
        val current = currentConversation
        if (current != null && !current.started && current.messages.isEmpty()) {
            screen = Screen.CHAT
            return
        }
        createConversation(save = true)
        screen = Screen.CHAT
    }

    fun selectChat(id: String) {
        activeId = id
        screen = Screen.CHAT
    }

    fun openConversation(id: String, messageId: String? = null) {
        activeId = id
        screen = Screen.CHAT
        if (messageId != null) {
            pendingJumpMessageId = messageId
            highlightedMessageId = messageId
        }
    }

    fun clearJump() {
        pendingJumpMessageId = null
    }

    fun clearHighlight() {
        highlightedMessageId = null
    }

    fun search(rawQuery: String): List<SearchHit> = ChatSearch.search(conversations, rawQuery)

    fun deleteChat(id: String) {
        conversations = conversations.filterNot { it.id == id }
        store.deleteConversation(id)
        if (activeId == id) {
            activeId = conversations.firstOrNull()?.id ?: ""
            if (conversations.isEmpty()) createConversation(save = true)
        }
        showToast("对话已删除")
    }

    fun renameChat(id: String, title: String) {
        updateConversationById(id) { it.copy(title = title.ifBlank { "未命名对话" }) }
    }

    fun updateSettings(block: (Settings) -> Settings) {
        settings = block(settings)
        store.saveSettings(settings)
    }

    fun savePrompts(system: String, anchor: String) {
        updateSettings { it.copy(systemPrompt = system, anchor = anchor) }
        val conv = currentConversation
        if (conv != null && conv.messages.isEmpty()) {
            updateConversationById(conv.id) {
                it.copy(
                    systemPrompt = composePrompt(system, anchor),
                    modelKey = activeConfig?.cacheFingerprint() ?: it.modelKey
                )
            }
        }
    }

    fun saveConfig(config: ApiConfig) {
        val exists = settings.configs.any { it.id == config.id }
        val list = if (exists) settings.configs.map { if (it.id == config.id) config else it }
        else settings.configs + config
        updateSettings {
            it.copy(
                configs = list,
                activeConfigId = if (it.activeConfigId.isBlank() || !exists) config.id else it.activeConfigId
            )
        }
        showToast("已保存")
    }

    fun deleteConfig(id: String) {
        val list = settings.configs.filterNot { it.id == id }
        updateSettings {
            it.copy(
                configs = list,
                activeConfigId = if (it.activeConfigId == id) (list.firstOrNull()?.id ?: "") else it.activeConfigId
            )
        }
    }

    fun setActiveConfig(id: String) {
        updateSettings { it.copy(activeConfigId = id) }
        showToast("已切换到「${settings.configs.firstOrNull { c -> c.id == id }?.name ?: ""}」")
    }

    fun finishOnboarding(preset: Preset, key: String, systemPrompt: String = "") {
        val config = Presets.toConfig(preset, key)
        val basePrompt = systemPrompt.ifBlank {
            settings.systemPrompt.ifBlank { DefaultSystemPrompt }
        }
        val prompt = composePrompt(basePrompt, settings.anchor)
        updateSettings {
            it.copy(
                configs = it.configs.filterNot { c -> c.presetId == preset.id } + config,
                activeConfigId = config.id,
                onboarded = true,
                systemPrompt = basePrompt
            )
        }
        val conv = currentConversation
        if (conv != null && conv.messages.isEmpty()) {
            updateConversationById(conv.id) {
                it.copy(systemPrompt = prompt, modelKey = config.cacheFingerprint())
            }
        }
    }

    fun skipOnboarding() {
        updateSettings { it.copy(onboarded = true, systemPrompt = if (it.systemPrompt.isBlank()) DefaultSystemPrompt else it.systemPrompt) }
    }

    fun testConfig(config: ApiConfig) {
        if (testing) return
        if (streaming) {
            showToast("等这条回复结束再测试")
            return
        }
        testing = true
        testMessage = null
        viewModelScope.launch {
            try {
                val msg = client.test(config)
                testMessage = msg
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                testMessage = friendly(e)
            } finally {
                testing = false
            }
        }
    }

    fun clearTest() {
        testMessage = null
    }

    fun testAndStart(preset: Preset, apiKey: String, onDone: (Boolean, String) -> Unit) {
        if (testing) return
        testing = true
        testMessage = null
        viewModelScope.launch {
            val config = Presets.toConfig(preset, apiKey)
            try {
                val msg = client.test(config)
                finishOnboarding(preset, apiKey)
                onDone(true, msg)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                onDone(false, friendly(e))
            } finally {
                testing = false
            }
        }
    }

    fun loadModels(config: ApiConfig) {
        if (loadingModels) return
        if (streaming) {
            showToast("等这条回复结束再获取模型列表")
            return
        }
        loadingModels = true
        availableModels = emptyList()
        viewModelScope.launch {
            try {
                availableModels = client.listModels(config)
                if (availableModels.isEmpty()) showToast("没有读到模型列表，手动填也可以")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                showToast(friendly(e))
            } finally {
                loadingModels = false
            }
        }
    }

    fun send(text: String, images: List<String> = emptyList()) {
        if ((text.isBlank() && images.isEmpty()) || streaming) return
        val config = activeConfig
        if (config == null || !isConfigured) {
            showToast("还没有配置 API，先花 1 分钟设置一下")
            screen = Screen.SETTINGS
            editingConfig = config ?: Presets.toConfig(Presets.byId("deepseek")!!)
            return
        }
        if (images.isNotEmpty() && !config.vision) {
            showToast("当前模型没开图片支持，先去设置里打开")
            return
        }
        var conv = currentConversation ?: createConversation(save = true)
        val fingerprint = config.cacheFingerprint()
        if (conv.modelKey.isNotBlank() && conv.modelKey != fingerprint) {
            showToast("换了模型或接口，这一轮缓存会重新计算")
        }
        val clean = text.trim()
        val userMessage = Message(
            role = "user",
            content = clean.ifBlank { "请看看这些图片。" },
            images = images,
            timeNote = "发送时间：" + com.kevin233.chat.util.Prefix.nowLabel(),
            usedSearch = conv.webSearch
        )
        conv = conv.copy(
            messages = conv.messages + userMessage,
            title = if (!conv.started) autoTitle(clean.ifBlank { "图片对话" }) else conv.title,
            started = true,
            updatedAt = System.currentTimeMillis(),
            modelKey = fingerprint
        )
        replaceConversation(conv)
        startStream(conv, config)
    }

    fun toggleWebSearch() {
        val conv = currentConversation ?: return
        val native = Presets.supportsWebSearch(activeConfig)
        val next = !conv.webSearch
        if (next && !native && !settings.searchTool) {
            showToast("这个模型不支持原生联网，先去设置里打开搜索工具")
            return
        }
        if (conv.messages.isNotEmpty()) showToast("联网状态变了，这一轮缓存会重新计算")
        updateConversationById(conv.id) { it.copy(webSearch = next) }
        if (next) {
            showToast(if (native) "已开启原生联网" else "已开启搜索工具，模型不支持原生联网")
        }
    }

    fun setReasoning(level: Int) {
        val value = level.coerceIn(0, 5)
        updateSettings { it.copy(reasoning = value) }
        val conv = currentConversation
        if (conv != null && conv.messages.isEmpty()) {
            updateConversationById(conv.id) { it.copy(reasoning = value) }
        } else if (conv != null) {
            showToast("已设为新对话默认，当前对话不变")
        }
    }

    fun setConversationReasoning(level: Int) {
        val conv = currentConversation ?: return
        val value = level.coerceIn(0, 5)
        if (conv.reasoning == value) return
        if (conv.messages.isNotEmpty()) showToast("思考强度变了，这一轮缓存会重新计算")
        updateConversationById(conv.id) { it.copy(reasoning = value) }
    }

    fun stop() {
        stoppedByUser = true
        client.cancel()
        job?.cancel()
    }

    fun regenerate() {
        if (streaming) return
        val config = activeConfig ?: return
        var conv = currentConversation ?: return
        val fingerprint = config.cacheFingerprint()
        if (conv.modelKey.isNotBlank() && conv.modelKey != fingerprint) {
            showToast("换了模型或接口，这一轮缓存会重新计算")
        }
        val msgs = conv.messages.toMutableList()
        while (msgs.isNotEmpty() && !msgs.last().isUser) msgs.removeAt(msgs.size - 1)
        if (msgs.isEmpty()) return
        conv = conv.copy(
            messages = msgs,
            updatedAt = System.currentTimeMillis(),
            modelKey = fingerprint
        )
        replaceConversation(conv)
        startStream(conv, config)
    }

    fun rewindTo(messageId: String) {
        val conv = currentConversation ?: return
        val index = conv.messages.indexOfFirst { it.id == messageId }
        if (index < 0) return
        val kept = conv.messages.take(index)
        replaceConversation(conv.copy(messages = kept, updatedAt = System.currentTimeMillis()))
        showToast("已回到这里，后面的内容已删掉")
    }

    private fun startStream(conv: Conversation, config: ApiConfig) {
        val assistant = Message(role = "assistant", content = "")
        var working = conv.copy(
            messages = conv.messages + assistant,
            updatedAt = System.currentTimeMillis()
        )
        replaceConversation(working)
        streaming = true
        stoppedByUser = false
        job = viewModelScope.launch {
            val text = StringBuilder()
            val reasoning = StringBuilder()
            var usage: Usage? = null
            var failed: String? = null
            var searchingNow = false
            val startedAt = System.currentTimeMillis()
            try {
                val nativeSearch = Presets.supportsWebSearch(config)
                if (working.webSearch && !nativeSearch && settings.searchTool) {
                    val userMessage = working.messages.lastOrNull { it.isUser && it.images.isEmpty() }
                    if (userMessage != null && userMessage.content.isNotBlank()) {
                        searchingNow = true
                        searching = true
                        val results = SearchClient.search(userMessage.content, settings.searchProvider)
                        if (results.isNotEmpty()) {
                            val context = SearchClient.searchContext(userMessage.content, results)
                            patchMessage(working.id, userMessage.id) { it.copy(searchContext = context) }
                            persist(working.id)
                        } else {
                            showToast("搜索没成功，先按普通回答")
                        }
                        searching = false
                        searchingNow = false
                    }
                }
                val current = conversations.firstOrNull { it.id == working.id } ?: working
                val history = current.messages.dropLast(1)
                val system = current.systemPrompt.ifBlank { settings.systemPrompt }
                client.stream(
                    config = config,
                    messages = history,
                    system = system,
                    temperature = settings.temperature,
                    maxTokens = settings.maxTokens,
                    includeUsage = settings.includeUsage,
                    longCache = settings.anthropicLongCache,
                    geminiExplicit = settings.geminiExplicitCache,
                    sessionKey = working.id,
                    useStream = settings.streaming,
                    reasoning = working.reasoning,
                    webSearch = working.webSearch && nativeSearch
                ).collect { delta ->
                    if (delta.text.isNotEmpty()) text.append(delta.text)
                    if (delta.reasoning.isNotEmpty()) reasoning.append(delta.reasoning)
                    if (delta.usage != null) usage = delta.usage
                    patchMessage(working.id, assistant.id) {
                        it.copy(content = text.toString(), reasoning = reasoning.toString())
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (!stoppedByUser && !(e is ChatException && e.message == "已停止")) failed = friendly(e)
            } finally {
                val finalUsage = usage
                patchMessage(working.id, assistant.id) { m ->
                    val content = when {
                        text.isNotEmpty() -> text.toString()
                        stoppedByUser -> "（已停止）"
                        failed == null -> "（没有收到回复，再试一次？）"
                        else -> ""
                    }
                    m.copy(
                        content = content,
                        reasoning = reasoning.toString(),
                        cacheHit = finalUsage?.hit ?: 0,
                        cacheMiss = finalUsage?.miss ?: 0,
                        outTokens = finalUsage?.out ?: Tokens.estimate(text.toString()),
                        durationMs = System.currentTimeMillis() - startedAt,
                        error = failed ?: ""
                    )
                }
                if (searchingNow) searching = false
                streaming = false
                persist(working.id)
                if (finalUsage != null) {
                    updateSettings {
                        it.copy(
                            totalHit = it.totalHit + finalUsage.hit,
                            totalMiss = it.totalMiss + finalUsage.miss,
                            totalOut = it.totalOut + finalUsage.out
                        )
                    }
                }
            }
        }
    }

    private fun patchMessage(convId: String, msgId: String, block: (Message) -> Message) {
        val index = conversations.indexOfFirst { it.id == convId }
        if (index < 0) return
        val conv = conversations[index]
        val msgIndex = conv.messages.indexOfFirst { it.id == msgId }
        if (msgIndex < 0) return
        val msgs = conv.messages.toMutableList()
        msgs[msgIndex] = block(msgs[msgIndex])
        val list = conversations.toMutableList()
        list[index] = conv.copy(messages = msgs, updatedAt = System.currentTimeMillis())
        conversations = list
    }

    private fun replaceConversation(conv: Conversation) {
        val index = conversations.indexOfFirst { it.id == conv.id }
        conversations = if (index < 0) listOf(conv) + conversations
        else conversations.toMutableList().also { it[index] = conv }
        store.saveConversation(conv)
    }

    private fun updateConversationById(id: String, block: (Conversation) -> Conversation) {
        val index = conversations.indexOfFirst { it.id == id }
        if (index < 0) return
        val updated = block(conversations[index])
        conversations = conversations.toMutableList().also { it[index] = updated }
        store.saveConversation(updated)
    }

    private fun persist(id: String) {
        conversations.firstOrNull { it.id == id }?.let { store.saveConversation(it) }
    }

    private fun autoTitle(text: String): String {
        val clean = text.replace("\n", " ").trim()
        return if (clean.length > 18) clean.take(18) + "…" else clean
    }

    private fun friendly(e: Exception): String {
        return when (e) {
            is ChatException -> e.message ?: "请求失败了"
            is IOException -> "网络连不上。检查一下网络，或者稍后再试。\n\n技术信息：${e.message ?: ""}"
            else -> "出了点问题：${e.message ?: e.javaClass.simpleName}"
        }
    }

    fun showToast(message: String) {
        toast = message
    }

    fun clearToast() {
        toast = null
    }

    fun clearAllData() {
        store.deleteAll()
        conversations = emptyList()
        activeId = ""
        createConversation(save = true)
        updateSettings {
            it.copy(totalHit = 0, totalMiss = 0, totalOut = 0)
        }
        showToast("已清空所有对话")
    }

    private fun readImportTexts(bytes: ByteArray): List<String> {
        val zip = bytes.size > 4 &&
            bytes[0] == 0x50.toByte() && bytes[1] == 0x4B.toByte() &&
            (bytes[2] == 0x03.toByte() || bytes[2] == 0x05.toByte() || bytes[2] == 0x07.toByte())
        if (!zip) return listOf(String(bytes, Charsets.UTF_8))
        val texts = mutableListOf<String>()
        ZipInputStream(ByteArrayInputStream(bytes)).use { stream ->
            var entry = stream.nextEntry
            while (entry != null) {
                val name = entry.name.lowercase()
                if (!entry.isDirectory && (name.endsWith(".json") || name.endsWith(".txt") || name.endsWith(".md"))) {
                    val text = stream.readBytes().toString(Charsets.UTF_8)
                    if (text.isNotBlank()) texts.add(text)
                }
                stream.closeEntry()
                entry = stream.nextEntry
            }
        }
        return texts
    }

    fun backupFileName(): String =
        "aichat-backup-" + java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.CHINA)
            .format(java.util.Date()) + ".json"

    fun exportTo(uri: Uri) {
        viewModelScope.launch {
            try {
                val resolver = getApplication<Application>().contentResolver
                val data = conversations.filter { it.messages.isNotEmpty() }
                val json = store.exportConversations(data)
                val stream = resolver.openOutputStream(uri)
                    ?: throw IllegalStateException("这个位置写不进去")
                stream.use { it.write(json.toByteArray(Charsets.UTF_8)) }
                showToast("导出好了，一共 ${data.size} 个对话")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                showToast("导出失败：${e.message ?: "换个保存位置试试"}")
            }
        }
    }

    fun importFrom(uri: Uri) {
        viewModelScope.launch {
            try {
                val resolver = getApplication<Application>().contentResolver
                val bytes = resolver.openInputStream(uri)?.use { it.readBytes() }
                    ?: throw IllegalStateException("这个文件打不开")
                val incoming = readImportTexts(bytes)
                    .flatMap { store.parseConversations(it) }
                    .distinctBy { it.id }
                if (incoming.isEmpty()) {
                    showToast("文件里没有可以导入的对话")
                    return@launch
                }
                val existing = conversations.map { it.id }.toHashSet()
                val fresh = incoming.filter { it.id !in existing }
                val skipped = incoming.size - fresh.size
                fresh.forEach { store.saveConversation(it) }
                conversations = (fresh + conversations).sortedByDescending { it.updatedAt }
                if (conversations.isNotEmpty()) activeId = conversations.first().id
                if (fresh.isNotEmpty()) {
                    val hit = fresh.sumOf { it.hitTokens.toLong() }
                    val miss = fresh.sumOf { it.missTokens.toLong() }
                    val out = fresh.sumOf { it.outTokens.toLong() }
                    updateSettings {
                        it.copy(totalHit = it.totalHit + hit, totalMiss = it.totalMiss + miss, totalOut = it.totalOut + out)
                    }
                }
                showToast(
                    if (skipped > 0) "导入了 ${fresh.size} 个对话，跳过 $skipped 个重复的"
                    else "导入了 ${fresh.size} 个对话"
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                showToast("导入失败：${e.message ?: "文件格式不对"}")
            }
        }
    }
}
