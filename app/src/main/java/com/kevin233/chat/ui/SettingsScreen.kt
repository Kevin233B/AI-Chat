package com.kevin233.chat.ui

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.HelpOutline
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kevin233.chat.ChatViewModel
import com.kevin233.chat.Screen
import com.kevin233.chat.data.ApiConfig
import com.kevin233.chat.data.ApiTypes
import com.kevin233.chat.data.Presets
import com.kevin233.chat.data.Settings
import com.kevin233.chat.data.composePrompt
import com.kevin233.chat.util.PromptDoctor
import com.kevin233.chat.util.Images
import com.kevin233.chat.util.Tokens
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class SettingsPage { HOME, API, PROMPT, CACHE, GENERATION, APPEARANCE, DATA, HELP }

@Composable
fun SettingsScreen(vm: ChatViewModel) {
    var pageName by rememberSaveable { mutableStateOf(SettingsPage.HOME.name) }
    if (vm.editingConfig != null) {
        ConfigEditorScreen(vm)
        return
    }
    val page = SettingsPage.entries.firstOrNull { it.name == pageName } ?: SettingsPage.HOME
    BackHandler {
        if (page == SettingsPage.HOME) vm.screen = Screen.CHAT else pageName = SettingsPage.HOME.name
    }
    when (page) {
        SettingsPage.HOME -> SettingsHome(vm) { pageName = it.name }
        SettingsPage.API -> ApiSettingsPage(vm) { pageName = SettingsPage.HOME.name }
        SettingsPage.PROMPT -> PromptEditorScreen(vm) { pageName = SettingsPage.HOME.name }
        SettingsPage.CACHE -> CacheSettingsPage(vm, { pageName = SettingsPage.HOME.name }) {
            pageName = SettingsPage.PROMPT.name
        }
        SettingsPage.GENERATION -> GenerationSettingsPage(vm) { pageName = SettingsPage.HOME.name }
        SettingsPage.APPEARANCE -> AppearanceSettingsPage(vm) { pageName = SettingsPage.HOME.name }
        SettingsPage.DATA -> DataSettingsPage(vm) { pageName = SettingsPage.HOME.name }
        SettingsPage.HELP -> HelpSettingsPage(vm) { pageName = SettingsPage.HOME.name }
    }
}

@Composable
private fun SettingsHome(vm: ChatViewModel, onOpen: (SettingsPage) -> Unit) {
    val palette = pal
    val config = vm.activeConfig
    val conv = vm.currentConversation
    val settings = vm.settings
    val profile = Presets.cacheProfile(config)
    val convHit = conv?.hitTokens ?: 0
    val convMiss = conv?.missTokens ?: 0
    val convTotal = convHit + convMiss
    val totalAll = settings.totalHit + settings.totalMiss
    val totalRate = if (totalAll > 0) settings.totalHit.toFloat() / totalAll else 0f
    val promptTokens = Tokens.estimate(conv?.systemPrompt ?: composePrompt(settings.systemPrompt, settings.anchor))
    val lastAnswer = conv?.messages?.lastOrNull { !it.isUser }
    val lastTotal = (lastAnswer?.cacheHit ?: 0) + (lastAnswer?.cacheMiss ?: 0)

    SettingsPageFrame("设置", onBack = { vm.screen = Screen.CHAT }) {
        PageBody {
            item {
                Column(Modifier.fillMaxWidth().padding(top = 6.dp)) {
                    Kicker("SETTINGS / 本地配置")
                    Spacer(Modifier.height(6.dp))
                    Text("设置", style = MaterialTheme.typography.displaySmall, color = palette.text)
                    Spacer(Modifier.height(16.dp))
                    Hairline()
                    Spacer(Modifier.height(16.dp))
                    Row(
                        Modifier.fillMaxWidth().clickable { onOpen(SettingsPage.API) },
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(Modifier.weight(1f)) {
                            Kicker("MODEL / 当前模型")
                            Spacer(Modifier.height(5.dp))
                            Text(
                                config?.name?.ifBlank { "自定义模型" } ?: "还没有连接模型",
                                style = MaterialTheme.typography.titleLarge,
                                fontSize = 20.sp,
                                color = palette.text
                            )
                            Spacer(Modifier.height(3.dp))
                            Text(
                                config?.model ?: "去添加一个 API，1 分钟就能用",
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.textSub,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(Modifier.height(7.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                StatusDot(if (vm.isConfigured) "已连接" else "未配置", vm.isConfigured)
                                if (config?.vision == true) StatusDot("支持图片", true)
                                if (Presets.supportsWebSearch(config)) StatusDot("原生联网", true)
                            }
                        }
                        Text("更改 ↗", style = MaterialTheme.typography.labelMedium, color = palette.accent)
                    }
                    Spacer(Modifier.height(16.dp))
                    Hairline()
                    Spacer(Modifier.height(18.dp))
                    Row(Modifier.fillMaxWidth().clickable { onOpen(SettingsPage.CACHE) }, verticalAlignment = Alignment.Bottom) {
                        Text(
                            "${(totalRate * 100).toInt()}%",
                            style = MaterialTheme.typography.displaySmall,
                            fontSize = 42.sp,
                            color = if (totalRate >= 0.3f) palette.ok else palette.text
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.padding(bottom = 4.dp)) {
                            Kicker("CACHE / 输入命中")
                            Spacer(Modifier.height(3.dp))
                            Text(
                                "READ ${Tokens.format(settings.totalHit.toInt())} TOKENS",
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.textSub
                            )
                        }
                        Spacer(Modifier.weight(1f))
                        Text("查看 →", style = MaterialTheme.typography.labelMedium, color = palette.accent)
                    }
                    Spacer(Modifier.height(10.dp))
                    val progress = if (profile.minPrefix <= 0) 1f else (promptTokens.toFloat() / profile.minPrefix).coerceIn(0f, 1f)
                    Box(Modifier.fillMaxWidth().height(2.dp).background(palette.border)) {
                        Box(Modifier.fillMaxWidth(progress).height(2.dp).background(if (progress >= 1f) palette.ok else palette.accent))
                    }
                    Spacer(Modifier.height(7.dp))
                    Text(
                        when {
                            convTotal > 0 && convHit > 0 -> "本次对话命中 ${convHit * 100 / convTotal}%"
                            lastAnswer != null && lastTotal > 0 -> "上一轮未命中，点开查看原因"
                            profile.minPrefix > 0 && promptTokens < profile.minPrefix -> "前缀还差 ${profile.minPrefix - promptTokens} token"
                            else -> "前缀已稳定，继续聊会更省"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.textSub
                    )
                    Spacer(Modifier.height(24.dp))
                }
            }

            item { GroupLabel("01 / 角色与资料") }
            item {
                GroupCard {
                    NavRow(Icons.Rounded.Groups, "智能体与技能", "${vm.agents.size} 个角色 / ${vm.skills.size} 个技能") {
                        vm.screen = Screen.AGENTS
                    }
                    RowDivider()
                    NavRow(Icons.Rounded.Description, "个性与固定资料", "约 $promptTokens TOKENS") {
                        onOpen(SettingsPage.PROMPT)
                    }
                }
            }

            item { GroupLabel("02 / 对话") }
            item {
                GroupCard {
                    NavRow(Icons.Rounded.Tune, "生成设置", "TEMP ${settings.temperature} / MAX ${settings.maxTokens}") {
                        onOpen(SettingsPage.GENERATION)
                    }
                    RowDivider()
                    NavRow(CacheStack, "缓存优化", profile.label) {
                        onOpen(SettingsPage.CACHE)
                    }
                }
            }

            item { GroupLabel("03 / 界面") }
            item {
                GroupCard {
                    NavRow(Icons.Rounded.Palette, "外观", "${themeLabel(settings.themeMode)} / ${accentLabel(settings.accent)}") {
                        onOpen(SettingsPage.APPEARANCE)
                    }
                }
            }

            item { GroupLabel("04 / 数据") }
            item {
                GroupCard {
                    NavRow(Icons.Rounded.Archive, "数据与备份", "${vm.conversations.size} 个对话，只存本机") {
                        onOpen(SettingsPage.DATA)
                    }
                    RowDivider()
                    NavRow(Icons.Rounded.HelpOutline, "帮助与关于", "FAQ / 新手引导 / LICENSE") {
                        onOpen(SettingsPage.HELP)
                    }
                }
            }

            item {
                Text(
                    "LOCAL ONLY / 聊天与 Key 只保存在本机",
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.textSub,
                    modifier = Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 28.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun ApiSettingsPage(vm: ChatViewModel, onBack: () -> Unit) {
    val palette = pal
    val active = vm.activeConfig
    SettingsPageFrame("模型与 API", "一套配置就是一个模型入口", onBack) {
        PageBody {
            if (active == null) {
                item {
                    Surface(color = palette.surface, shape = RoundedCornerShape(3.dp), border = androidx.compose.foundation.BorderStroke(1.dp, palette.border)) {
                        Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            InitialBadge("AI", 52.dp)
                            Spacer(Modifier.height(12.dp))
                            Text("还没有配置模型", style = MaterialTheme.typography.titleMedium, color = palette.text)
                            Text("先选一个平台，跟着提示填 Key 就行", style = MaterialTheme.typography.bodySmall, color = palette.textSub)
                            Spacer(Modifier.height(14.dp))
                            PrimaryAction("去配置") { vm.editingConfig = ApiConfig(name = "", type = ApiTypes.OPENAI) }
                        }
                    }
                }
            } else {
                item {
                    Surface(
                        color = palette.surface,
                        shape = RoundedCornerShape(3.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, palette.border),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(18.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                InitialBadge(active.name, 46.dp)
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            active.name.ifBlank { "自定义模型" },
                                            style = MaterialTheme.typography.titleMedium,
                                            color = palette.text
                                        )
                                        Spacer(Modifier.width(7.dp))
                                        StatusDot(if (vm.isConfigured) "使用中" else "缺 Key", vm.isConfigured)
                                    }
                                    Text(active.model, style = MaterialTheme.typography.bodySmall, color = palette.textSub, maxLines = 1)
                                    Text(maskKey(active.apiKey), style = MaterialTheme.typography.labelSmall, color = palette.textSub)
                                }
                                IconButton(onClick = { vm.editingConfig = active }) {
                                    Icon(Icons.Rounded.Edit, "编辑", tint = palette.accent, modifier = Modifier.size(19.dp))
                                }
                            }
                            if (active.vision || Presets.supportsWebSearch(active)) {
                                Spacer(Modifier.height(10.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    if (active.vision) StatusDot("支持图片", true)
                                    if (Presets.supportsWebSearch(active)) StatusDot("支持联网", true)
                                    if (Presets.supportsReasoning(active)) StatusDot("支持思考控制", true)
                                }
                            }
                        }
                    }
                }
            }

            val others = vm.settings.configs.filter { it.id != active?.id }
            if (others.isNotEmpty()) {
                item { GroupLabel("其他配置") }
                item {
                    GroupCard {
                        others.forEachIndexed { index, cfg ->
                            Row(
                                Modifier.fillMaxWidth().clickable { vm.setActiveConfig(cfg.id) }.padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    Modifier.size(34.dp).clip(RoundedCornerShape(3.dp)).background(palette.surfaceAlt),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Rounded.Memory, null, tint = palette.textSub, modifier = Modifier.size(18.dp))
                                }
                                Spacer(Modifier.width(11.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(cfg.name.ifBlank { "自定义模型" }, style = MaterialTheme.typography.bodyMedium, color = palette.text)
                                    Text(cfg.model, style = MaterialTheme.typography.labelSmall, color = palette.textSub, maxLines = 1)
                                }
                                Text("切换", style = MaterialTheme.typography.labelMedium, color = palette.accent)
                            }
                            if (index < others.lastIndex) RowDivider()
                        }
                    }
                }
            }

            item {
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(3.dp))
                        .border(1.dp, palette.border, RoundedCornerShape(3.dp))
                        .clickable { vm.editingConfig = ApiConfig(name = "", type = ApiTypes.OPENAI) }
                        .padding(vertical = 14.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.Add, null, tint = palette.accent, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("添加模型或接口", style = MaterialTheme.typography.labelLarge, color = palette.accent)
                }
            }

            item {
                Surface(color = palette.surface, shape = RoundedCornerShape(3.dp), border = androidx.compose.foundation.BorderStroke(1.dp, palette.border)) {
                    Column(Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(CacheStack, null, tint = palette.accent, modifier = Modifier.size(17.dp))
                            Spacer(Modifier.width(7.dp))
                            Text("Key 到底怎么拿？", style = MaterialTheme.typography.titleSmall, color = palette.text)
                        }
                        Spacer(Modifier.height(5.dp))
                        Text(
                            "点下面的按钮重新看一遍新手引导，四步就能拿到 DeepSeek 的 Key。",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.textSub
                        )
                        TextButton(onClick = { vm.updateSettings { it.copy(onboarded = false) } }) {
                            Text("重新看新手引导", color = palette.accent)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CacheSettingsPage(vm: ChatViewModel, onBack: () -> Unit, onEditPrompt: () -> Unit) {
    val palette = pal
    val settings = vm.settings
    val conv = vm.currentConversation
    val active = vm.activeConfig
    val profile = Presets.cacheProfile(active)
    val convHit = conv?.hitTokens ?: 0
    val convMiss = conv?.missTokens ?: 0
    val convTotal = convHit + convMiss
    val totalAll = settings.totalHit + settings.totalMiss
    val totalRate = if (totalAll > 0) settings.totalHit.toFloat() / totalAll else 0f
    val promptTokens = Tokens.estimate(conv?.systemPrompt ?: composePrompt(settings.systemPrompt, settings.anchor))
    val lastAnswer = conv?.messages?.lastOrNull { !it.isUser }
    val lastTotal = (lastAnswer?.cacheHit ?: 0) + (lastAnswer?.cacheMiss ?: 0)

    SettingsPageFrame("缓存优化", "命中越高，花得越少", onBack) {
        PageBody {
            item {
                Surface(color = palette.surface, shape = RoundedCornerShape(3.dp), border = androidx.compose.foundation.BorderStroke(1.dp, palette.border)) {
                    Column(Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "${(totalRate * 100).toInt()}%",
                                style = MaterialTheme.typography.headlineMedium,
                                color = if (totalRate >= 0.3f) palette.ok else palette.text
                            )
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text("累计缓存命中率", style = MaterialTheme.typography.labelMedium, color = palette.text)
                                Text("省下 ${Tokens.format(settings.totalHit.toInt())} token", style = MaterialTheme.typography.labelSmall, color = palette.textSub)
                            }
                            Spacer(Modifier.weight(1f))
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    if (convTotal > 0) "${convHit * 100 / convTotal}%" else "未开始",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = palette.accent
                                )
                                Text("本次对话", style = MaterialTheme.typography.labelSmall, color = palette.textSub)
                            }
                        }
                        Spacer(Modifier.height(14.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(CacheStack, null, tint = palette.accent, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(profile.label, style = MaterialTheme.typography.labelMedium, color = palette.text, modifier = Modifier.weight(1f))
                            Text(
                                if (profile.minPrefix > 0) "门槛 ${profile.minPrefix} token" else "无缓存",
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.textSub
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        val progress = if (profile.minPrefix <= 0) 1f else (promptTokens.toFloat() / profile.minPrefix).coerceIn(0f, 1f)
                        Box(Modifier.fillMaxWidth().height(6.dp).clip(CircleShape).background(palette.border)) {
                            Box(
                                Modifier.fillMaxWidth(progress).height(6.dp).clip(CircleShape)
                                    .background(if (progress >= 1f) palette.ok else palette.accent)
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "当前前缀约 $promptTokens token，继续聊会越来越容易命中",
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.textSub
                        )
                    }
                }
            }

            if (lastAnswer != null && lastTotal > 0) {
                item {
                    Surface(color = palette.accentSoft, shape = RoundedCornerShape(3.dp)) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
                            Icon(CacheStack, null, tint = palette.accent, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                if (lastAnswer.cacheHit > 0) {
                                    "上一轮命中 ${lastAnswer.cacheHit * 100 / lastTotal}%，保持现在的设置继续聊。"
                                } else {
                                    "上一轮没命中。常见原因：刚换模型、改过提示词、服务商路由变了，或者还没到门槛。"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.accentText
                            )
                        }
                    }
                }
            }

            item { GroupLabel("这个模型的方案") }
            item {
                Surface(color = palette.surface, shape = RoundedCornerShape(3.dp), border = androidx.compose.foundation.BorderStroke(1.dp, palette.border)) {
                    Column(Modifier.padding(14.dp)) {
                        profile.tips.forEach { tip ->
                            Row(Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.Top) {
                                BulletDot(Modifier.padding(top = 7.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(tip, style = MaterialTheme.typography.bodySmall, color = palette.textSub)
                            }
                        }
                    }
                }
            }

            item { GroupLabel("开关") }
            item {
                GroupCard {
                    SettingRow(
                        icon = CacheStack,
                        title = "Claude 使用 1 小时缓存",
                        subtitle = "Claude 和 OpenRouter 上的 Claude 都能用",
                        tint = palette.ok,
                        trailing = {
                            Switch(
                                checked = settings.anthropicLongCache,
                                onCheckedChange = { on -> vm.updateSettings { it.copy(anthropicLongCache = on) } }
                            )
                        }
                    )
                    if (active?.type == ApiTypes.GEMINI) {
                        RowDivider()
                        SettingRow(
                            icon = CacheStack,
                            title = "长资料显式缓存",
                            subtitle = if (Presets.geminiSupportsExplicit(active.model)) {
                                "固定资料够长时单独存 1 小时，反复提问更省"
                            } else {
                                "Gemini 4 只支持自动缓存，这个开关不生效"
                            },
                            tint = palette.ok,
                            trailing = {
                                Switch(
                                    checked = settings.geminiExplicitCache,
                                    onCheckedChange = { on -> vm.updateSettings { it.copy(geminiExplicitCache = on) } }
                                )
                            }
                        )
                        Text(
                            "显式缓存按小时收存储费，只有反复用同一份长资料才划算。失败会自动退回普通缓存。",
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.textSub,
                            modifier = Modifier.padding(start = 48.dp, end = 8.dp, bottom = 6.dp)
                        )
                    }
                    RowDivider()
                    SettingRow(
                        icon = Icons.Rounded.Info,
                        title = "向 API 索取用量统计",
                        subtitle = "用来显示命中率，几乎不影响速度",
                        trailing = {
                            Switch(
                                checked = settings.includeUsage,
                                onCheckedChange = { on -> vm.updateSettings { it.copy(includeUsage = on) } }
                            )
                        }
                    )
                }
            }

            item {
                Surface(color = palette.surfaceAlt, shape = RoundedCornerShape(3.dp)) {
                    Column(Modifier.padding(14.dp)) {
                        Text(
                            "想再省一点？把长期不变的资料放进固定资料，前缀一大，命中率就稳了。",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.textSub
                        )
                        TextButton(onClick = onEditPrompt) { Text("去编辑固定资料", color = palette.accent) }
                    }
                }
            }
        }
    }
}

@Composable
private fun GenerationSettingsPage(vm: ChatViewModel, onBack: () -> Unit) {
    val palette = pal
    val settings = vm.settings
    val supportsReasoning = Presets.supportsReasoning(vm.activeConfig)
    var temperature by remember(settings.temperature) { mutableStateOf(settings.temperature) }
    var maxTokens by remember(settings.maxTokens) { mutableStateOf(settings.maxTokens) }

    SettingsPageFrame("生成设置", "回答的聪明程度和长短", onBack) {
        PageBody {
            item {
                Surface(color = palette.surface, shape = RoundedCornerShape(3.dp), border = androidx.compose.foundation.BorderStroke(1.dp, palette.border)) {
                    Column(Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Psychology, null, tint = palette.accent, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(7.dp))
                            Text("思考强度", style = MaterialTheme.typography.titleSmall, color = palette.text)
                            Spacer(Modifier.weight(1f))
                            Text(
                                if (supportsReasoning) "当前模型支持" else "当前模型不生效",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (supportsReasoning) palette.ok else palette.textSub
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "这里是新对话的默认值。当前对话可以直接在输入框上方调，越深越聪明，也越慢越费。",
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.textSub
                        )
                        Spacer(Modifier.height(12.dp))
                        Row(
                            Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                "自动" to 0, "低" to 1, "中" to 2, "高" to 3,
                                "极高 xhigh" to 4, "最高 max" to 5
                            ).forEach { (label, level) ->
                                PillChip(label, settings.reasoning == level) { vm.setReasoning(level) }
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "不同模型支持的档位不同。Gemini 没有 max，最高按 xhigh 处理；不支持的档位会降到最接近的一档。",
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.textSub
                        )
                    }
                }
            }

            item {
                Surface(color = palette.surface, shape = RoundedCornerShape(3.dp), border = androidx.compose.foundation.BorderStroke(1.dp, palette.border)) {
                    Column(Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("回答风格", style = MaterialTheme.typography.bodyLarge, color = palette.text, modifier = Modifier.weight(1f))
                            Text(temperatureLabel(temperature), style = MaterialTheme.typography.labelMedium, color = palette.accent)
                        }
                        Slider(
                            value = temperature,
                            onValueChange = { v -> temperature = v },
                            onValueChangeFinished = {
                                vm.updateSettings { it.copy(temperature = (temperature * 10).toInt() / 10f) }
                            },
                            valueRange = 0f..1.5f
                        )
                        Row {
                            Text("精准", style = MaterialTheme.typography.labelSmall, color = palette.textSub)
                            Spacer(Modifier.weight(1f))
                            Text("发散", style = MaterialTheme.typography.labelSmall, color = palette.textSub)
                        }
                    }
                }
            }

            item {
                Surface(color = palette.surface, shape = RoundedCornerShape(3.dp), border = androidx.compose.foundation.BorderStroke(1.dp, palette.border)) {
                    Column(Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("最长回复", style = MaterialTheme.typography.bodyLarge, color = palette.text, modifier = Modifier.weight(1f))
                            Text("$maxTokens token", style = MaterialTheme.typography.labelMedium, color = palette.accent)
                        }
                        Slider(
                            value = maxTokens.toFloat(),
                            onValueChange = { v -> maxTokens = (v / 512).toInt() * 512 },
                            onValueChangeFinished = { vm.updateSettings { it.copy(maxTokens = maxTokens) } },
                            valueRange = 512f..32768f
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(2048, 8192, 16384, 32768).forEach { size ->
                                PillChip("${size / 1024}K", maxTokens == size) {
                                    maxTokens = size
                                    vm.updateSettings { it.copy(maxTokens = size) }
                                }
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "越大越能写长文，也越费 token。超过模型上限时接口会拒绝或截断。",
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.textSub
                        )
                    }
                }
            }

            item {
                GroupCard {
                    SettingRow(
                        icon = Icons.Rounded.Restore,
                        title = "边生成边显示",
                        subtitle = "关掉就是等全部写完再出现",
                        trailing = {
                            Switch(
                                checked = settings.streaming,
                                onCheckedChange = { on -> vm.updateSettings { it.copy(streaming = on) } }
                            )
                        }
                    )
                    RowDivider()
                    SettingRow(
                        icon = Icons.Rounded.Public,
                        title = "联网搜索",
                        subtitle = "在聊天输入框上方按对话开关，支持 Claude、Gemini、OpenRouter 和通义",
                        trailing = { Text("去聊天", color = palette.accent, style = MaterialTheme.typography.labelMedium) },
                        onClick = { vm.screen = Screen.CHAT }
                    )
                }
            }

            item {
                Surface(color = palette.surface, shape = RoundedCornerShape(3.dp), border = androidx.compose.foundation.BorderStroke(1.dp, palette.border)) {
                    Column(Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Public, null, tint = palette.accent, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("搜索工具", style = MaterialTheme.typography.titleSmall, color = palette.text)
                            Spacer(Modifier.weight(1f))
                            Switch(
                                checked = settings.searchTool,
                                onCheckedChange = { on -> vm.updateSettings { it.copy(searchTool = on) } }
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "模型没有原生联网时，应用用公开搜索代查。结果只附在当前提问后面，前面的缓存不动。",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.textSub
                        )
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            PillChip("DuckDuckGo", settings.searchProvider == "duckduckgo") {
                                vm.updateSettings { it.copy(searchProvider = "duckduckgo") }
                            }
                            PillChip("Bing 网页", settings.searchProvider == "bing") {
                                vm.updateSettings { it.copy(searchProvider = "bing") }
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "搜索词会发送给所选搜索服务。公开网页搜索可能被限流，失败时会自动按普通回答继续。",
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.textSub
                        )
                    }
                }
            }

            item {
                TextButton(onClick = {
                    vm.updateSettings {
                        it.copy(temperature = 0.7f, maxTokens = 8192, reasoning = 0, streaming = true)
                    }
                }) { Text("恢复默认设置", color = palette.textSub) }
            }
        }
    }
}

@Composable
private fun AppearanceSettingsPage(vm: ChatViewModel, onBack: () -> Unit) {
    val palette = pal
    val settings = vm.settings
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var savingWallpaper by remember { mutableStateOf(false) }
    var glass by remember(settings.glassAlpha) { mutableStateOf(settings.glassAlpha) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            scope.launch {
                savingWallpaper = true
                val path = withContext(Dispatchers.IO) { Images.saveWallpaper(ctx, uri) }
                savingWallpaper = false
                if (path != null) {
                    vm.updateSettings { it.copy(wallpaper = "custom", wallpaperPath = path) }
                } else {
                    vm.showToast("这张图存不下来，换一张试试")
                }
            }
        }
    }

    SettingsPageFrame("外观", "壁纸、玻璃和主题", onBack) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp).navigationBarsPadding()) {
            Text("壁纸", style = MaterialTheme.typography.titleSmall, color = palette.text)
            Spacer(Modifier.height(3.dp))
            Text(
                "选一张底色，卡片会变成半透明玻璃。选“无”回到纯色背景。",
                style = MaterialTheme.typography.bodySmall,
                color = palette.textSub
            )
            Spacer(Modifier.height(10.dp))
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                WallpaperSwatch("none", "无", settings.wallpaper == "none") {
                    vm.updateSettings { it.copy(wallpaper = "none") }
                }
                WallpaperPresets.all.forEach { preset ->
                    WallpaperSwatch(preset.id, preset.name, settings.wallpaper == preset.id) {
                        vm.updateSettings { it.copy(wallpaper = preset.id) }
                    }
                }
                WallpaperSwatch("custom", if (savingWallpaper) "处理中" else "自定义", settings.wallpaper == "custom", custom = true) {
                    if (!savingWallpaper) picker.launch("image/*")
                }
            }
            Spacer(Modifier.height(20.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("玻璃透明度", style = MaterialTheme.typography.titleSmall, color = palette.text, modifier = Modifier.weight(1f))
                Text(
                    if (settings.wallpaper == "none") "先选壁纸" else "${(glass * 100).toInt()}%",
                    style = MaterialTheme.typography.labelMedium,
                    color = palette.accent
                )
            }
            Slider(
                value = glass,
                onValueChange = { v -> glass = v },
                onValueChangeFinished = { vm.updateSettings { it.copy(glassAlpha = glass) } },
                valueRange = 0.45f..0.95f
            )
            Row {
                Text("更透", style = MaterialTheme.typography.labelSmall, color = palette.textSub)
                Spacer(Modifier.weight(1f))
                Text("更实", style = MaterialTheme.typography.labelSmall, color = palette.textSub)
            }
            if (settings.wallpaper == "none") {
                Text(
                    "玻璃效果只在有壁纸时看得出来。",
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.textSub
                )
            }
            Spacer(Modifier.height(20.dp))
            Kicker("ACCENT / 强调色")
            Spacer(Modifier.height(8.dp))
            val accents = listOf(
                Color(0xFFE4502A), Color(0xFF2F5BEA), Color(0xFF0E7A5F), Color(0xFF8A3FFC), Color(0xFFB07A12)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                accents.forEachIndexed { index, color ->
                    Box(
                        Modifier.size(30.dp)
                            .background(color, RoundedCornerShape(2.dp))
                            .border(
                                if (settings.accent == index) 2.dp else 1.dp,
                                if (settings.accent == index) palette.text else palette.border,
                                RoundedCornerShape(2.dp)
                            )
                            .clickable { vm.updateSettings { it.copy(accent = index) } },
                        contentAlignment = Alignment.Center
                    ) {
                        if (settings.accent == index) {
                            Icon(Icons.Rounded.Check, null, tint = Color.White, modifier = Modifier.size(15.dp))
                        }
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
            Kicker("THEME / 明暗")
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("跟随系统" to 0, "浅色" to 1, "深色" to 2).forEach { (label, mode) ->
                    PillChip(label, settings.themeMode == mode) {
                        vm.updateSettings { it.copy(themeMode = mode) }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Text("预览", style = MaterialTheme.typography.labelMedium, color = palette.textSub)
            Spacer(Modifier.height(8.dp))
            Surface(
                color = palette.bg,
                shape = RoundedCornerShape(3.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, palette.border)
            ) {
                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                    Row(horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Text("YOU / 14:20", style = MaterialTheme.typography.labelSmall, color = palette.textSub)
                        Spacer(Modifier.width(8.dp))
                    }
                    Spacer(Modifier.height(6.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        Row(
                            Modifier.background(palette.surface, RoundedCornerShape(2.dp))
                                .border(1.dp, palette.border, RoundedCornerShape(2.dp))
                        ) {
                            Box(Modifier.width(2.dp).height(38.dp).background(palette.accent))
                            Text(
                                "今天适合出门吗？",
                                style = MaterialTheme.typography.bodyMedium,
                                color = palette.text,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(4.dp).background(palette.accent))
                        Spacer(Modifier.width(6.dp))
                        Text("AI / 14:20", style = MaterialTheme.typography.labelSmall, color = palette.text)
                    }
                    Spacer(Modifier.height(7.dp))
                    Row(Modifier.fillMaxWidth().height(44.dp)) {
                        Box(Modifier.width(2.dp).fillMaxHeight().background(palette.border))
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "我帮你看看天气，顺便给你一条路线。",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.text
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(
                "跟随系统时会自动跟着手机的深色模式切换。深色模式会把壁纸压暗一点，保证文字看得清。",
                style = MaterialTheme.typography.bodySmall,
                color = palette.textSub
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun WallpaperSwatch(
    id: String,
    label: String,
    selected: Boolean,
    custom: Boolean = false,
    onClick: () -> Unit
) {
    val palette = pal
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        val fill = if (custom || id == "none") {
            Modifier.background(palette.surfaceAlt)
        } else {
            Modifier.background(WallpaperPresets.brush(id))
        }
        Box(
            Modifier.size(width = 64.dp, height = 82.dp)
                .clip(RoundedCornerShape(3.dp))
                .then(fill)
                .border(
                    if (selected) 2.dp else 1.dp,
                    if (selected) palette.accent else palette.border,
                    RoundedCornerShape(3.dp)
                )
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            when {
                id == "none" -> Text("无", style = MaterialTheme.typography.titleMedium, color = palette.textSub)
                custom -> Icon(Icons.Rounded.Add, null, tint = palette.textSub, modifier = Modifier.size(24.dp))
                else -> if (selected) {
                    Box(
                        Modifier.size(24.dp).clip(CircleShape).background(palette.accent),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.Check, null, tint = Color.White, modifier = Modifier.size(15.dp))
                    }
                }
            }
        }
        Spacer(Modifier.height(5.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) palette.accent else palette.textSub
        )
    }
}

@Composable
private fun DataSettingsPage(vm: ChatViewModel, onBack: () -> Unit) {
    val palette = pal
    var confirmClear by remember { mutableStateOf(false) }
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri -> uri?.let { vm.exportTo(it) } }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let { vm.importFrom(it) } }
    val messageCount = vm.conversations.sumOf { it.messages.size }

    SettingsPageFrame("数据与备份", "换手机、清理、恢复都在这", onBack) {
        PageBody {
            item {
                Surface(color = palette.surface, shape = RoundedCornerShape(3.dp), border = androidx.compose.foundation.BorderStroke(1.dp, palette.border)) {
                    Row(Modifier.padding(18.dp)) {
                        Column(Modifier.weight(1f)) {
                            Text("${vm.conversations.size}", style = MaterialTheme.typography.headlineMedium, color = palette.text)
                            Text("个对话", style = MaterialTheme.typography.labelSmall, color = palette.textSub)
                        }
                        Column(Modifier.weight(1f)) {
                            Text(Tokens.format(messageCount), style = MaterialTheme.typography.headlineMedium, color = palette.text)
                            Text("条消息", style = MaterialTheme.typography.labelSmall, color = palette.textSub)
                        }
                        Column(Modifier.weight(1f)) {
                            Text(Tokens.format(vm.settings.totalHit.toInt()), style = MaterialTheme.typography.headlineMedium, color = palette.ok)
                            Text("节省 token", style = MaterialTheme.typography.labelSmall, color = palette.textSub)
                        }
                    }
                }
            }

            item {
                GroupCard {
                    SettingRow(
                        icon = Icons.Rounded.FileDownload,
                        title = "导出对话记录",
                        subtitle = "存成 JSON 文件，只含聊天内容，不含 Key",
                        onClick = { exportLauncher.launch(vm.backupFileName()) }
                    )
                    RowDivider()
                    SettingRow(
                        icon = Icons.Rounded.FileUpload,
                        title = "导入对话记录",
                        subtitle = "支持 AI Chat、Claude、DeepSeek 的 JSON，重复对话自动跳过",
                        onClick = {
                            importLauncher.launch(arrayOf("application/json", "text/plain", "application/octet-stream", "application/zip", "application/x-zip-compressed"))
                        }
                    )
                    RowDivider()
                    SettingRow(
                        icon = Icons.Rounded.Delete,
                        title = "清空所有对话",
                        subtitle = "API 配置和提示词会保留",
                        tint = palette.danger,
                        onClick = { confirmClear = true }
                    )
                }
            }

            item {
                Text(
                    "所有数据都在本机。系统云备份已经关闭，换手机请用上面的导出和导入。",
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.textSub,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("清空全部对话？") },
            text = { Text("聊天记录会全没，但 API 配置、提示词和固定资料还在。") },
            confirmButton = {
                TextButton(onClick = {
                    confirmClear = false
                    vm.clearAllData()
                }) { Text("清空", color = palette.danger) }
            },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("算了") } }
        )
    }
}

@Composable
private fun HelpSettingsPage(vm: ChatViewModel, onBack: () -> Unit) {
    val palette = pal
    val context = LocalContext.current
    var expanded by remember { mutableStateOf(-1) }
    val faqs = listOf(
        "API Key 是什么，要花钱吗？" to "Key 就像只属于你的钥匙，AI 公司靠它认出是你。多数平台注册会送一点免费额度，用完按量计费。这个应用本身不收费。",
        "为什么缓存命中率是 0？" to "常见原因有三个：开头太短没到门槛，刚改过提示词或换了模型，或者服务商把请求分到了另一台机器。连续聊几轮通常就会上来。",
        "支持哪些模型？" to "OpenAI 兼容接口都支持，另外单独支持 Claude 和 Gemini 原生接口。图片和联网搜索要看具体模型是否支持。",
        "会不会泄露聊天内容？" to "不会。聊天记录、图片和 Key 只存在这台手机。只有你点发送时，应用才会直连你填的 API 地址。",
        "换手机会丢数据吗？" to "直接换机会丢，先用数据与备份里的导出功能备份，再在新手机上导入。API Key、提示词和固定资料需要重新填一次。",
        "提示 401 或余额不足怎么办？" to "401 一般是 Key 复制错了或失效了，回官网重新复制。余额不足就去平台充值，或者换一个有免费额度的模型。",
        "手机上能连电脑上的模型吗？" to "可以。手机和电脑连同一个 Wi-Fi，把地址里的 127.0.0.1 换成电脑的局域网 IP，例如 http://192.168.1.5:11434/v1。"
    )

    SettingsPageFrame("帮助与关于", "不懂的地方先看这里", onBack) {
        PageBody {
            item {
                GroupCard {
                    SettingRow(
                        icon = Icons.Rounded.Restore,
                        title = "重新看一遍新手引导",
                        subtitle = "忘了怎么配 API 的时候点这里",
                        onClick = { vm.updateSettings { it.copy(onboarded = false) } }
                    )
                    RowDivider()
                    SettingRow(
                        icon = Icons.Rounded.Key,
                        title = "打开 DeepSeek 官网",
                        subtitle = "去拿 API Key",
                        onClick = {
                            runCatching {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://platform.deepseek.com/api_keys")))
                            }
                        }
                    )
                }
            }

            item { GroupLabel("常见问题") }
            item {
                Surface(color = palette.surface, shape = RoundedCornerShape(3.dp), border = androidx.compose.foundation.BorderStroke(1.dp, palette.border)) {
                    Column {
                        faqs.forEachIndexed { index, pair ->
                            Column(Modifier.fillMaxWidth().clickable { expanded = if (expanded == index) -1 else index }.padding(horizontal = 14.dp, vertical = 12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        pair.first,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = palette.text,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Icon(
                                        if (expanded == index) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                                        null,
                                        tint = palette.textSub,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                if (expanded == index) {
                                    Spacer(Modifier.height(5.dp))
                                    Text(pair.second, style = MaterialTheme.typography.bodySmall, color = palette.textSub)
                                }
                            }
                            if (index < faqs.lastIndex) RowDivider()
                        }
                    }
                }
            }

            item {
                Surface(color = palette.surface, shape = RoundedCornerShape(3.dp), border = androidx.compose.foundation.BorderStroke(1.dp, palette.border)) {
                    Column(Modifier.padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        AppLogo(50.dp, 15.dp)
                        Spacer(Modifier.height(10.dp))
                        Text("AI Chat 1.0.0", style = MaterialTheme.typography.titleMedium, color = palette.text)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "轻量、直接、只存在本机",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.textSub
                        )
                        Spacer(Modifier.height(10.dp))
                        Text(
                            "没有账号，没有广告，没有第三方统计。自带自己的 Key，直连模型。",
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.textSub
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsPageFrame(
    title: String,
    subtitle: String? = null,
    onBack: () -> Unit,
    action: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val palette = pal
    Column(Modifier.fillMaxSize().background(palette.bg).statusBarsPadding()) {
        Row(
            Modifier.fillMaxWidth().height(66.dp).padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Rounded.ArrowBack, "返回", tint = palette.text, modifier = Modifier.size(21.dp))
            }
            Spacer(Modifier.width(4.dp))
            Column(Modifier.weight(1f)) {
                Kicker("SETTINGS / 本地配置")
                Spacer(Modifier.height(2.dp))
                Text(
                    title,
                    style = MaterialTheme.typography.titleLarge,
                    color = palette.text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (action != null) {
                Box(Modifier.padding(end = 6.dp)) { action() }
            }
        }
        Hairline()
        Box(Modifier.fillMaxWidth().weight(1f)) {
            content()
        }
    }
}

@Composable
private fun PageBody(content: LazyListScope.() -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier.fillMaxHeight().widthIn(max = 760.dp).fillMaxWidth(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 48.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
            content = content
        )
    }
}

@Composable
private fun GroupLabel(text: String) {
    Row(
        Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 0.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(3.dp).height(14.dp).background(pal.accent))
        Spacer(Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.labelMedium, color = pal.text)
    }
}

@Composable
private fun GroupCard(content: @Composable ColumnScope.() -> Unit) {
    val palette = pal
    Column(
        Modifier.fillMaxWidth().background(palette.surface, RoundedCornerShape(3.dp))
            .border(1.dp, palette.border, RoundedCornerShape(3.dp)),
        content = content
    )
}

@Composable
private fun RowDivider() {
    HorizontalDivider(color = pal.border, modifier = Modifier.padding(start = 48.dp))
}

@Composable
private fun NavRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    val palette = pal
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = palette.accent, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontSize = 15.sp, color = palette.text)
            Spacer(Modifier.height(2.dp))
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = palette.textSub, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text("↗", style = MaterialTheme.typography.labelMedium, color = palette.textSub)
    }
}

@Composable
private fun StatusDot(text: String, ok: Boolean) {
    val palette = pal
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(4.dp).background(if (ok) palette.ok else palette.warn))
        Spacer(Modifier.width(5.dp))
        Text(text, style = MaterialTheme.typography.labelSmall, color = if (ok) palette.ok else palette.warn)
    }
}

@Composable
private fun PrimaryAction(text: String, onClick: () -> Unit) {
    Box(
        Modifier.fillMaxWidth().background(pal.accent, RoundedCornerShape(3.dp)).clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = Color.White, style = MaterialTheme.typography.labelMedium)
    }
}

private fun reasoningLabel(level: Int): String = when (level) {
    1 -> "低"
    2 -> "中"
    3 -> "高"
    4 -> "极高"
    5 -> "最高"
    else -> "自动"
}

private fun temperatureLabel(t: Float): String = when {
    t <= 0.3f -> "很严谨"
    t <= 0.7f -> "比较稳"
    t <= 1.0f -> "正常"
    else -> "很发散"
}

private fun themeLabel(mode: Int): String = when (mode) {
    1 -> "浅色"
    2 -> "深色"
    else -> "跟随系统"
}

private fun accentLabel(index: Int): String = listOf("EMBER", "COBALT", "PINE", "PLUM", "OCHRE")
    .getOrElse(index) { "EMBER" }

private fun maskKey(key: String): String {
    if (key.isBlank()) return "还没填 Key"
    if (key.length <= 10) return key.take(2) + "****"
    return key.take(6) + "******" + key.takeLast(4)
}

@Composable
private fun ConfigEditorScreen(vm: ChatViewModel) {
    val palette = pal
    val original = vm.editingConfig ?: return
    var draft by remember(original.id) { mutableStateOf(original) }
    var showKey by remember { mutableStateOf(false) }
    var showModels by remember { mutableStateOf(false) }
    val isNew = vm.settings.configs.none { it.id == original.id }
    val ctx = LocalContext.current

    LaunchedEffect(original.id) { vm.clearTest() }
    BackHandler { vm.editingConfig = null }

    SettingsPageFrame(
        title = if (isNew) "添加模型" else "编辑模型",
        subtitle = "填一次，之后随便聊",
        onBack = { vm.editingConfig = null },
        action = {
            TextButton(onClick = {
                if (draft.name.isBlank()) draft = draft.copy(name = "自定义")
                if (draft.baseUrl.isBlank() || draft.model.isBlank()) {
                    vm.showToast("接口地址和模型名要填上")
                } else {
                    vm.saveConfig(draft)
                    vm.editingConfig = null
                }
            }) { Text("保存", color = palette.accent) }
        }
    ) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)
                .imePadding().navigationBarsPadding()
        ) {
            Text("快速填充", style = MaterialTheme.typography.labelMedium, color = palette.textSub)
            Spacer(Modifier.height(8.dp))
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Presets.all.forEach { preset ->
                    PillChip(preset.name, draft.presetId == preset.id) {
                        draft = Presets.toConfig(preset, draft.apiKey).copy(id = draft.id)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = draft.name,
                onValueChange = { draft = draft.copy(name = it) },
                label = { Text("起个名字") },
                singleLine = true,
                shape = RoundedCornerShape(3.dp),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))
            Text("接口类型", style = MaterialTheme.typography.labelMedium, color = palette.textSub)
            Spacer(Modifier.height(7.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PillChip("OpenAI 兼容", draft.type == ApiTypes.OPENAI) { draft = draft.copy(type = ApiTypes.OPENAI) }
                PillChip("Claude", draft.type == ApiTypes.ANTHROPIC) { draft = draft.copy(type = ApiTypes.ANTHROPIC) }
                PillChip("Gemini", draft.type == ApiTypes.GEMINI) { draft = draft.copy(type = ApiTypes.GEMINI) }
            }
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = draft.baseUrl,
                onValueChange = { draft = draft.copy(baseUrl = it) },
                label = { Text("接口地址") },
                placeholder = { Text("https://api.deepseek.com/v1") },
                singleLine = true,
                shape = RoundedCornerShape(3.dp),
                supportingText = { Text("大多数填到 /v1 结尾，中转站按它给的地址填") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = draft.apiKey,
                onValueChange = { draft = draft.copy(apiKey = it.trim()) },
                label = { Text("API Key") },
                placeholder = { Text("sk-...") },
                singleLine = true,
                shape = RoundedCornerShape(3.dp),
                visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { showKey = !showKey }) {
                        Icon(
                            if (showKey) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                            "显示/隐藏",
                            tint = palette.textSub,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = draft.model,
                onValueChange = { draft = draft.copy(model = it) },
                label = { Text("模型名") },
                placeholder = { Text("deepseek-chat") },
                singleLine = true,
                shape = RoundedCornerShape(3.dp),
                trailingIcon = {
                    TextButton(onClick = {
                        vm.loadModels(draft)
                        showModels = true
                    }) {
                        if (vm.loadingModels) {
                            CircularProgressIndicator(Modifier.size(14.dp), strokeWidth = 2.dp)
                        } else {
                            Text("获取", color = palette.accent)
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            Surface(color = palette.surface, shape = RoundedCornerShape(3.dp), border = androidx.compose.foundation.BorderStroke(1.dp, palette.border)) {
                Column {
                    SettingRow(
                        icon = Icons.Rounded.Image,
                        title = "这个模型支持图片",
                        subtitle = "打开后聊天框会出现图片按钮，不确定就关着",
                        trailing = {
                            Switch(
                                checked = draft.vision,
                                onCheckedChange = { on -> draft = draft.copy(vision = on) }
                            )
                        }
                    )
                    if (Presets.supportsReasoning(draft) || Presets.supportsWebSearch(draft)) {
                        RowDivider()
                        Row(Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (Presets.supportsReasoning(draft)) StatusDot("支持思考控制", true)
                            if (Presets.supportsWebSearch(draft)) StatusDot("支持联网", true)
                        }
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    Modifier.weight(1f).clip(RoundedCornerShape(3.dp)).border(1.dp, palette.border, RoundedCornerShape(3.dp))
                        .clickable { vm.testConfig(draft) }.padding(vertical = 13.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (vm.testing) {
                            CircularProgressIndicator(Modifier.size(15.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(7.dp))
                        }
                        Text(if (vm.testing) "正在测试" else "测试连接", color = palette.text, style = MaterialTheme.typography.labelLarge)
                    }
                }
                Box(
                    Modifier.weight(1f).clip(RoundedCornerShape(3.dp)).background(palette.accent).clickable {
                        if (draft.name.isBlank()) draft = draft.copy(name = "自定义")
                        if (draft.baseUrl.isBlank() || draft.model.isBlank()) {
                            vm.showToast("接口地址和模型名要填上")
                        } else {
                            vm.saveConfig(draft)
                            if (isNew) vm.setActiveConfig(draft.id)
                            vm.editingConfig = null
                            vm.showToast("已保存，可以开始聊天了")
                        }
                    }.padding(vertical = 13.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("保存并使用", color = Color.White, style = MaterialTheme.typography.labelLarge)
                }
            }
            vm.testMessage?.let { msg ->
                Spacer(Modifier.height(10.dp))
                val ok = msg.startsWith("连接成功")
                Surface(
                    color = if (ok) palette.ok.copy(alpha = 0.08f) else palette.danger.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(3.dp)
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
                        Icon(
                            if (ok) Icons.Rounded.Check else Icons.Rounded.ErrorOutline,
                            null,
                            tint = if (ok) palette.ok else palette.danger,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(msg, style = MaterialTheme.typography.bodySmall, color = if (ok) palette.ok else palette.danger)
                    }
                }
            }
            val preset = Presets.byId(draft.presetId)
            if (preset != null) {
                Spacer(Modifier.height(12.dp))
                Surface(color = palette.surfaceAlt, shape = RoundedCornerShape(3.dp)) {
                    Column(Modifier.padding(12.dp)) {
                        Text(preset.name, style = MaterialTheme.typography.labelLarge, color = palette.text)
                        if (preset.note.isNotBlank()) {
                            Spacer(Modifier.height(3.dp))
                            Text(preset.note, style = MaterialTheme.typography.bodySmall, color = palette.textSub)
                        }
                        if (preset.keyUrl.isNotBlank()) {
                            Spacer(Modifier.height(5.dp))
                            Text(
                                "去这里拿 Key：${preset.keyUrl}",
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.accent,
                                modifier = Modifier.clickable {
                                    runCatching {
                                        ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(preset.keyUrl)))
                                    }
                                }
                            )
                        }
                    }
                }
            }
            if (!isNew) {
                Spacer(Modifier.height(12.dp))
                Text(
                    "删除这个配置",
                    style = MaterialTheme.typography.labelLarge,
                    color = palette.danger,
                    modifier = Modifier.clip(RoundedCornerShape(3.dp)).clickable {
                        vm.deleteConfig(draft.id)
                        vm.editingConfig = null
                        vm.showToast("已删除")
                    }.padding(8.dp)
                )
            }
            Spacer(Modifier.height(30.dp))
        }
    }

    if (showModels) {
        AlertDialog(
            onDismissRequest = { showModels = false },
            title = { Text(if (vm.availableModels.isEmpty()) "模型列表" else "选一个模型") },
            text = {
                if (vm.loadingModels) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(10.dp))
                        Text("正在向服务器要列表")
                    }
                } else if (vm.availableModels.isEmpty()) {
                    Text("没拿到列表。可以直接手动填模型名，或检查 Key 和地址。")
                } else {
                    LazyColumn(Modifier.height(360.dp)) {
                        items(vm.availableModels) { model ->
                            Text(
                                model,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (model == draft.model) palette.accent else palette.text,
                                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        draft = draft.copy(model = model)
                                        showModels = false
                                    }
                                    .padding(vertical = 10.dp, horizontal = 6.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showModels = false }) { Text("关闭") } }
        )
    }
}

@Composable
private fun PromptEditorScreen(vm: ChatViewModel, onBack: () -> Unit) {
    val palette = pal
    val settings = vm.settings
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var text by rememberSaveable(settings.systemPrompt) { mutableStateOf(settings.systemPrompt) }
    var anchorText by rememberSaveable(settings.anchor) { mutableStateOf(settings.anchor) }
    val profile = Presets.cacheProfile(vm.activeConfig)
    val audit = PromptDoctor.audit(text, profile)
    val anchorAudit = PromptDoctor.audit(anchorText, profile)
    val totalTokens = Tokens.estimate(composePrompt(text, anchorText))
    BackHandler {
        vm.savePrompts(text, anchorText)
        onBack()
    }

    SettingsPageFrame(
        title = "个性与固定资料",
        subtitle = "共约 $totalTokens token，只影响新对话",
        onBack = { vm.savePrompts(text, anchorText); onBack() },
        action = {
            TextButton(onClick = {
                vm.savePrompts(text, anchorText)
                vm.showToast("已保存，新对话会用它")
                onBack()
            }) { Text("保存", color = palette.accent, fontWeight = FontWeight.Medium) }
        }
    ) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).imePadding().navigationBarsPadding()) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PillChip("系统提示词", tab == 0) { tab = 0 }
                PillChip("固定资料", tab == 1) { tab = 1 }
            }
            Spacer(Modifier.height(12.dp))
            if (tab == 0) {
                Text(
                    "决定 AI 怎么说话、做什么、不做什么。所有新对话共用。",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textSub
                )
            } else {
                Text(
                    "放手册、写作规范、常用代码、长期资料。所有新对话共用，越固定越容易命中缓存。",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textSub
                )
            }
            Spacer(Modifier.height(10.dp))
            Surface(color = palette.surface, shape = RoundedCornerShape(3.dp), border = androidx.compose.foundation.BorderStroke(1.dp, palette.border)) {
                BasicTextField(
                    value = if (tab == 0) text else anchorText,
                    onValueChange = { if (tab == 0) text = it else anchorText = it },
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = palette.text),
                    cursorBrush = SolidColor(palette.accent),
                    modifier = Modifier.fillMaxWidth().height(260.dp).padding(14.dp)
                )
            }
            Spacer(Modifier.height(7.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "约 ${Tokens.estimate(if (tab == 0) text else anchorText)} token",
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.textSub
                )
                Spacer(Modifier.weight(1f))
                if (tab == 0) {
                    Text("保存后只影响新对话", style = MaterialTheme.typography.labelSmall, color = palette.textSub)
                } else {
                    Text("已计入缓存前缀", style = MaterialTheme.typography.labelSmall, color = palette.ok)
                }
            }

            if (tab == 0) {
                Spacer(Modifier.height(16.dp))
                Text("常用模板", style = MaterialTheme.typography.labelMedium, color = palette.textSub)
                Spacer(Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Presets.suggestedSystemPrompts().chunked(2).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            row.forEach { (name, prompt) ->
                                Surface(
                                    color = palette.surface,
                                    shape = RoundedCornerShape(3.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, palette.border),
                                    modifier = Modifier.weight(1f).clickable { text = prompt }
                                ) {
                                    Text(name, style = MaterialTheme.typography.bodySmall, color = palette.text, modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp))
                                }
                            }
                        }
                    }
                }
                if (audit.hasIssue) {
                    Spacer(Modifier.height(14.dp))
                    Surface(color = palette.warn.copy(alpha = 0.09f), shape = RoundedCornerShape(3.dp)) {
                        Column(Modifier.padding(14.dp)) {
                            Text("缓存体检", style = MaterialTheme.typography.labelLarge, color = palette.warn)
                            Spacer(Modifier.height(5.dp))
                            audit.warnings.forEach { warning ->
                                Row(Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.Top) {
                                    BulletDot(Modifier.padding(top = 6.dp), palette.warn)
                                    Spacer(Modifier.width(7.dp))
                                    Text(warning, style = MaterialTheme.typography.bodySmall, color = palette.textSub)
                                }
                            }
                            if (audit.dynamicLines.isNotEmpty()) {
                                Text(
                                    "一键把会变的内容挪到最后",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = palette.warn,
                                    modifier = Modifier.clickable { text = PromptDoctor.moveDynamicToEnd(text) }.padding(vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            } else if (anchorAudit.dynamicLines.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Surface(color = palette.warn.copy(alpha = 0.09f), shape = RoundedCornerShape(3.dp)) {
                    Text(
                        "固定资料里发现了会变的内容，建议删掉，否则后面的缓存每次都会失效。",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.warn,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }

            Spacer(Modifier.height(14.dp))
            Surface(color = palette.surfaceAlt, shape = RoundedCornerShape(3.dp)) {
                Column(Modifier.padding(14.dp)) {
                    Text("为什么会影响省钱？", style = MaterialTheme.typography.labelLarge, color = palette.text)
                    Spacer(Modifier.height(5.dp))
                    Text(
                        "这块内容在每次对话最前面。它只要不变，服务器就能直接复用缓存。一旦中间改了字，后面的缓存就全部作废。",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.textSub
                    )
                }
            }
            Spacer(Modifier.height(30.dp))
        }
    }
}
