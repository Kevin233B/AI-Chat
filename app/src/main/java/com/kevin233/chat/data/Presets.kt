package com.kevin233.chat.data

enum class CacheMode { PREFIX, ANTHROPIC, GEMINI, NONE }

data class CacheProfile(
    val mode: CacheMode,
    val label: String,
    val key: String,
    val minPrefix: Int,
    val tips: List<String>
)

data class Preset(
    val id: String,
    val name: String,
    val desc: String,
    val type: String,
    val baseUrl: String,
    val model: String,
    val models: List<String>,
    val keyUrl: String,
    val badge: String = "",
    val guide: List<String> = emptyList(),
    val note: String = "",
    val vision: Boolean = false
)

object Presets {

    val deepSeekGuide = listOf(
        "打开 DeepSeek 开放平台，用手机号或微信登录（没注册就先注册，1 分钟）。",
        "登录后点左侧菜单里的「API keys」。",
        "点「创建 API key」，名字随便填，然后复制那串 sk- 开头的字符。",
        "回到这里粘贴到输入框，点下面的「测试并开始」就完成了。"
    )

    val all: List<Preset> = listOf(
        Preset(
            id = "deepseek",
            name = "DeepSeek",
            desc = "国内直连，便宜，缓存命中再打 1 折",
            type = ApiTypes.OPENAI,
            baseUrl = "https://api.deepseek.com/v1",
            model = "deepseek-chat",
            models = listOf("deepseek-chat", "deepseek-reasoner"),
            keyUrl = "https://platform.deepseek.com/api_keys",
            badge = "推荐",
            guide = deepSeekGuide,
            note = "deepseek-chat 日常够用；deepseek-reasoner 会先思考再回答，适合难题。"
        ),
        Preset(
            id = "siliconflow",
            name = "硅基流动",
            desc = "一个 Key 玩几十种模型，有免费额度",
            type = ApiTypes.OPENAI,
            baseUrl = "https://api.siliconflow.cn/v1",
            model = "deepseek-ai/DeepSeek-V3",
            models = listOf("deepseek-ai/DeepSeek-V3", "deepseek-ai/DeepSeek-R1", "Qwen/Qwen2.5-72B-Instruct"),
            keyUrl = "https://cloud.siliconflow.cn/account/ak",
            note = "注册会送免费额度，适合想多试几个模型的人。"
        ),
        Preset(
            id = "kimi",
            name = "Kimi 月之暗面",
            desc = "长文本阅读很强，自动缓存",
            type = ApiTypes.OPENAI,
            baseUrl = "https://api.moonshot.cn/v1",
            model = "kimi-k2-0905-preview",
            models = listOf("kimi-k2-0905-preview", "moonshot-v1-8k", "moonshot-v1-32k"),
            keyUrl = "https://platform.moonshot.cn/console/api-keys",
            note = "长文档问答很划算，缓存自动生效。"
        ),
        Preset(
            id = "glm",
            name = "智谱 GLM",
            desc = "glm-4-flash 免费，国内速度快",
            type = ApiTypes.OPENAI,
            baseUrl = "https://open.bigmodel.cn/api/paas/v4",
            model = "glm-4-flash",
            models = listOf("glm-4-flash", "glm-4-air", "glm-4-plus"),
            keyUrl = "https://open.bigmodel.cn/usercenter/apikeys",
            note = "glm-4-flash 可以免费用，先拿它入门最省事。"
        ),
        Preset(
            id = "qwen",
            name = "通义千问",
            desc = "阿里云百炼，新用户有免费额度",
            type = ApiTypes.OPENAI,
            baseUrl = "https://dashscope.aliyuncs.com/compatible-mode/v1",
            model = "qwen-plus",
            models = listOf("qwen-plus", "qwen-turbo", "qwen-max"),
            keyUrl = "https://bailian.console.aliyun.com/?apiKey=1",
            note = "百炼控制台点右上角「API-KEY」就能创建。"
        ),
        Preset(
            id = "ark",
            name = "火山方舟 豆包",
            desc = "字节跳动，国内延迟低",
            type = ApiTypes.OPENAI,
            baseUrl = "https://ark.cn-beijing.volces.com/api/v3",
            model = "doubao-seed-1-6-250615",
            models = listOf("doubao-seed-1-6-250615", "doubao-1-5-pro-32k-250115"),
            keyUrl = "https://console.volcengine.com/ark",
            note = "如果报模型不存在，把「推理接入点 ID」填到模型那一栏。",
            vision = true,
        ),
        Preset(
            id = "openai",
            name = "OpenAI",
            desc = "GPT 系列官方接口",
            type = ApiTypes.OPENAI,
            baseUrl = "https://api.openai.com/v1",
            model = "gpt-4o-mini",
            models = listOf("gpt-4o-mini", "gpt-4o", "gpt-5", "gpt-4.1-mini", "o3-mini"),
            keyUrl = "https://platform.openai.com/api-keys",
            note = "国内访问可能需要代理，可以把接口地址换成中转站地址。",
            vision = true,
        ),
        Preset(
            id = "openrouter",
            name = "OpenRouter",
            desc = "一个 Key 用遍全球模型",
            type = ApiTypes.OPENAI,
            baseUrl = "https://openrouter.ai/api/v1",
            model = "deepseek/deepseek-chat",
            models = listOf("deepseek/deepseek-chat", "google/gemini-2.5-flash", "anthropic/claude-sonnet-4.5"),
            keyUrl = "https://openrouter.ai/keys",
            note = "模型名要带厂商前缀，例如 deepseek/deepseek-chat。"
        ),
        Preset(
            id = "groq",
            name = "Groq",
            desc = "出字飞快，免费额度大",
            type = ApiTypes.OPENAI,
            baseUrl = "https://api.groq.com/openai/v1",
            model = "llama-3.3-70b-versatile",
            models = listOf("llama-3.3-70b-versatile", "llama-3.1-8b-instant"),
            keyUrl = "https://console.groq.com/keys",
            note = "速度极快，适合日常闲聊和翻译。"
        ),
        Preset(
            id = "claude",
            name = "Claude",
            desc = "Anthropic 官方，写作能力强",
            type = ApiTypes.ANTHROPIC,
            baseUrl = "https://api.anthropic.com",
            model = "claude-sonnet-4-5-20250929",
            models = listOf("claude-sonnet-4-5-20250929", "claude-sonnet-5-5", "claude-3-5-haiku-20241022"),
            keyUrl = "https://console.anthropic.com/settings/keys",
            note = "本应用会自动帮你打缓存标记，长对话省很多。",
            vision = true,
        ),
        Preset(
            id = "gemini",
            name = "Google Gemini",
            desc = "有免费额度，多模态强",
            type = ApiTypes.GEMINI,
            baseUrl = "https://generativelanguage.googleapis.com",
            model = "gemini-2.5-flash",
            models = listOf("gemini-2.5-flash", "gemini-4-hydrogen", "gemini-3.8-flash", "gemini-2.5-pro"),
            keyUrl = "https://aistudio.google.com/app/apikey",
            note = "在 AI Studio 点「Get API key」就能拿。",
            vision = true,
        ),
        Preset(
            id = "ollama",
            name = "本地 Ollama",
            desc = "不联网、不花钱，模型跑在电脑上",
            type = ApiTypes.OPENAI,
            baseUrl = "http://127.0.0.1:11434/v1",
            model = "qwen3:8b",
            models = listOf("qwen3:8b", "llama3.2", "deepseek-r1:8b"),
            keyUrl = "https://ollama.com/download",
            note = "手机和电脑要连同一个 Wi-Fi，把 127.0.0.1 换成电脑的局域网 IP。"
        ),
        Preset(
            id = "lmstudio",
            name = "本地 LM Studio",
            desc = "电脑上跑模型，手机远程调用",
            type = ApiTypes.OPENAI,
            baseUrl = "http://127.0.0.1:1234/v1",
            model = "local-model",
            models = listOf("local-model"),
            keyUrl = "https://lmstudio.ai",
            note = "在 LM Studio 里开启 Local Server，端口默认 1234。"
        ),
        Preset(
            id = "custom",
            name = "自定义接口",
            desc = "中转站、公司内网、任何 OpenAI 兼容接口",
            type = ApiTypes.OPENAI,
            baseUrl = "",
            model = "",
            models = emptyList(),
            keyUrl = "",
            note = "地址一般要填到 /v1 结尾，例如 https://xxx.com/v1。"
        )
    )

    fun byId(id: String): Preset? = all.firstOrNull { it.id == id }

    fun toConfig(p: Preset, apiKey: String = ""): ApiConfig = ApiConfig(
        name = p.name,
        type = p.type,
        baseUrl = p.baseUrl,
        apiKey = apiKey,
        model = p.model,
        presetId = p.id,
        vision = p.vision
    )

    fun cacheProfile(config: ApiConfig?): CacheProfile {
        if (config == null) return genericProfile
        val url = config.baseUrl.lowercase()
        val model = config.model.lowercase()
        if (config.type == ApiTypes.ANTHROPIC || url.contains("anthropic")) return anthropicProfile(config.model)
        if (config.type == ApiTypes.GEMINI || url.contains("generativelanguage")) return geminiProfile(config.model)
        return when {
            url.contains("deepseek") -> deepSeekProfile
            url.contains("moonshot") || model.startsWith("kimi") -> kimiProfile
            url.contains("bigmodel") || model.startsWith("glm") || url.contains("z.ai") -> glmProfile
            url.contains("dashscope") || model.startsWith("qwen") -> qwenProfile
            url.contains("siliconflow") -> siliconProfile
            url.contains("openrouter") -> openRouterProfile(config.model)
            url.contains("api.openai.com") || model.startsWith("gpt-") -> openAiProfile
            url.contains("groq") -> groqProfile(config.model)
            url.contains("127.0.0.1") || url.contains("localhost") || url.contains("192.168.") -> localProfile
            else -> genericProfile
        }
    }

    fun claudeMinTokens(model: String): Int {
        val m = model.lowercase().replace("_", "-")
        return when {
            m.contains("fable-5") || m.contains("mythos-5") || m.contains("opus-5") ||
                m.contains("sonnet-5") || m.contains("haiku-5") -> 512
            m.contains("mythos-preview") || m.contains("opus-4-7") -> 2048
            m.contains("opus-4-6") || m.contains("opus-4-5") || m.contains("haiku-4-5") -> 4096
            m.contains("haiku-3-5") -> 2048
            else -> 1024
        }
    }

    fun geminiMinTokens(model: String): Int {
        val m = model.lowercase().replace("_", "-")
        return when {
            m.contains("gemini-4") || m.contains("gemini-3") -> 4096
            else -> 2048
        }
    }

    fun geminiSupportsExplicit(model: String): Boolean =
        !model.lowercase().contains("gemini-4")

    fun supportsWebSearch(config: ApiConfig?): Boolean {
        if (config == null) return false
        val url = config.baseUrl.lowercase()
        val model = config.model.lowercase()
        return config.type == ApiTypes.ANTHROPIC || url.contains("anthropic") ||
            config.type == ApiTypes.GEMINI || url.contains("generativelanguage") ||
            url.contains("openrouter") || url.contains("dashscope") ||
            (url.contains("api.openai.com") && model.contains("search"))
    }

    fun supportsReasoning(config: ApiConfig?): Boolean {
        if (config == null) return false
        if (config.type == ApiTypes.ANTHROPIC || config.type == ApiTypes.GEMINI) return true
        val model = config.model.lowercase()
        return model.startsWith("o1") || model.startsWith("o3") || model.startsWith("o4") ||
            model.startsWith("gpt-5") || model.startsWith("gpt-6") ||
            model.contains("qwen3") || model.contains("glm-4.5") ||
            model.contains("glm-4.6") || model.contains("glm-5")
    }

    private val deepSeekProfile = CacheProfile(
        CacheMode.PREFIX, "DeepSeek 自动缓存", "deepseek", 64,
        listOf(
            "DeepSeek 会自动把重复的开头存进缓存，命中部分的输入价格大约只要 1 折。",
            "缓存最小单位是 64 个 token，所以开头越固定越好。",
            "同一个对话里只往后追加、不删改旧消息，命中率最高。",
            "系统提示词里别写「今天几号」「随机编号」这类每次都变的内容。"
        )
    )

    private val openAiProfile = CacheProfile(
        CacheMode.PREFIX, "OpenAI 前缀缓存", "openai", 1024,
        listOf(
            "GPT-5.6 起最低 1024 token 才会缓存，写入按 1.25 倍计费，读取约 0.1 倍。",
            "本应用会带上稳定的 prompt_cache_key，帮请求落到同一台缓存机器。",
            "缓存按 128 token 一段生效，开头对上了，后面才可能对上。",
            "别在系统提示词里塞时间戳，改一个字后面的缓存就作废。"
        )
    )

    private fun anthropicProfile(model: String): CacheProfile = CacheProfile(
        CacheMode.ANTHROPIC, "Claude 自动缓存", "anthropic", claudeMinTokens(model),
        listOf(
            "Claude 现在支持自动缓存，本应用在请求顶层放了一个缓存标记，断点会随对话自动往前推进。",
            "系统提示词或固定资料够长时，会再打一个固定断点，保证这块内容稳定复用。",
            "门槛看模型：Claude 5 系列 512 token，Sonnet 4.5 是 1024，Opus 4.5、Opus 4.6 和 Haiku 4.5 是 4096。",
            "缓存默认 5 分钟，打开 1 小时缓存可以跨会话复用，专门对付聊两句停一会儿的场景。"
        )
    )

    private fun geminiProfile(model: String): CacheProfile = CacheProfile(
        CacheMode.GEMINI, "Gemini 隐式缓存", "gemini", geminiMinTokens(model),
        listOf(
            "Gemini 2.5 要 2048 token 起步，Gemini 3 系列和 4 系列要 4096 token。",
            "把固定资料放系统提示词，把问题放最后，最容易命中。",
            "打开「长资料显式缓存」后，固定资料会单独存成 1 小时缓存，反复提问更省。",
            "缓存命中按更低价格计费，长文档场景最明显。"
        )
    )

    private val kimiProfile = CacheProfile(
        CacheMode.PREFIX, "Kimi 自动缓存", "kimi", 64,
        listOf(
            "Kimi 会自动缓存重复的长开头，命中部分按更低价格计费。",
            "只追加、不改旧消息，命中率会一直保持很高。",
            "长文档阅读时，把资料放在前面、问题放在后面。"
        )
    )

    private val glmProfile = CacheProfile(
        CacheMode.PREFIX, "GLM 自动缓存", "glm", 64,
        listOf(
            "GLM 的上下文缓存自动生效，不用额外设置。",
            "同样的资料反复提问，第二次开始就便宜很多。",
            "前缀保持一致，别在开头塞时间戳。"
        )
    )

    private val qwenProfile = CacheProfile(
        CacheMode.PREFIX, "通义千问自动缓存", "qwen", 64,
        listOf(
            "通义千问会自动缓存重复前缀，命中后输入价格很低。",
            "长文档问答时，资料固定、问题变化，最省钱。",
            "保持系统提示词不变，只追加新消息。"
        )
    )

    private val siliconProfile = CacheProfile(
        CacheMode.PREFIX, "SiliconFlow 自动缓存", "silicon", 64,
        listOf(
            "硅基流动部分模型支持前缀缓存，具体看模型说明。",
            "开头固定、只往后追加，是通用做法。",
            "同一个对话连续提问，比每次新开对话更省。"
        )
    )

    private fun openRouterProfile(model: String): CacheProfile {
        val m = model.lowercase()
        val min = when {
            m.contains("claude") -> claudeMinTokens(m)
            m.contains("gemini") -> geminiMinTokens(m)
            else -> 1024
        }
        return CacheProfile(
            CacheMode.PREFIX, "OpenRouter 黏性路由缓存", "openrouter", min,
            listOf(
                "OpenRouter 会自动把同一会话的请求粘在同一个服务商上，缓存不容易丢。",
                "本应用会带上会话 ID，让黏性路由更准，60 分钟内连续聊都能复用。",
                "Claude 模型会额外带上自动缓存标记，断点会跟着对话走。",
                "不同上游模型门槛不同，至少 1024 token，Claude 看具体型号。"
            )
        )
    }

    private fun groqProfile(model: String): CacheProfile {
        if (model.lowercase().contains("kimi")) {
            return CacheProfile(
                CacheMode.PREFIX, "Groq Kimi 缓存", "groq", 1024,
                listOf(
                    "Groq 目前给 Kimi K2 系列开了提示词缓存，读取价格约 5 折。",
                    "保持开头一致，只追加新消息。",
                    "缓存自动生效，没有额外开关。"
                )
            )
        }
        return CacheProfile(
            CacheMode.NONE, "Groq 暂无缓存", "groq", 0,
            listOf(
                "Groq 目前只有 Kimi K2 系列支持缓存，其他模型没有。",
                "省钱主要靠控制上下文长度，常开新对话。",
                "短问题、快回答的场景最适合它。"
            )
        )
    }

    private val localProfile = CacheProfile(
        CacheMode.PREFIX, "本地模型缓存", "local", 64,
        listOf(
            "本地模型通常由服务端自行缓存前缀，Ollama、LM Studio 都有类似机制。",
            "不联网、不花钱，速度取决于你电脑的显卡。",
            "上下文越长越吃内存，聊久了记得新开对话。"
        )
    )

    private val genericProfile = CacheProfile(
        CacheMode.PREFIX, "通用前缀缓存", "generic", 1024,
        listOf(
            "大多数模型都会缓存「开头完全一样」的内容。",
            "只往后追加、不回头改，是最省钱的用法。",
            "系统提示词越固定，缓存命中越稳定。"
        )
    )

    fun suggestedSystemPrompts(): List<Pair<String, String>> = listOf(
        "通用助手" to "你是一个乐于助人的 AI 助手。回答准确、简洁、有条理；不知道就直说不知道。",
        "翻译专家" to "你是一名专业翻译。把用户发来的内容翻译成地道、自然的目标语言；保持原文语气和格式，专有名词保留原文。只输出译文。",
        "写作帮手" to "你是一名中文写作教练。帮用户把内容改得更通顺、更有条理，保留原意，指出最关键的 1 到 2 个问题，并给出改写示例。",
        "编程搭档" to "你是一名资深工程师。先给可运行的代码，再简短说明关键点；指出潜在坑；不确定的地方明确标注。",
        "学习导师" to "你是一位耐心的老师。用大白话解释概念，配合生活中的例子，一次只讲一个重点，最后用一句话总结。",
        "口语陪练" to "你是一位英语口语陪练。用英文和用户对话，难度控制在用户水平；每次回复后，用一句中文指出一个可以改进的表达。"
    )
}
