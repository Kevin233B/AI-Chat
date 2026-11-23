package com.kevin233.chat.ui

import android.graphics.Bitmap
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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kevin233.chat.ChatViewModel
import com.kevin233.chat.Screen
import com.kevin233.chat.data.Agent
import com.kevin233.chat.data.Skill
import com.kevin233.chat.data.Skills
import com.kevin233.chat.util.Images
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.kevin233.chat.data.DefaultSystemPrompt

@Composable
fun AgentsScreen(vm: ChatViewModel) {
    var creating by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Agent?>(null) }
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var deleteSkill by remember { mutableStateOf<Skill?>(null) }
    val skillPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) vm.importSkill(uri)
    }
    if (creating || editing != null) {
        AgentEditorScreen(vm, if (creating) null else editing) {
            creating = false
            editing = null
        }
        return
    }
    BackHandler { vm.screen = Screen.CHAT }
    val palette = pal
    Column(Modifier.fillMaxSize().background(palette.bg).statusBarsPadding()) {
        Row(
            Modifier.fillMaxWidth().height(66.dp).padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { vm.screen = Screen.CHAT }) {
                Icon(Icons.Rounded.ArrowBack, "返回", tint = palette.text, modifier = Modifier.size(21.dp))
            }
            Spacer(Modifier.width(4.dp))
            Column(Modifier.weight(1f)) {
                Kicker("CATALOG / 角色目录")
                Spacer(Modifier.height(2.dp))
                Text("智能体与技能", style = MaterialTheme.typography.titleLarge, color = palette.text)
            }
            if (tab == 0) {
                TextButton(onClick = { creating = true }) {
                    Text("创建", color = palette.accent, style = MaterialTheme.typography.labelMedium)
                }
            } else {
                TextButton(onClick = {
                    skillPicker.launch(arrayOf("text/*", "application/json", "application/octet-stream"))
                }) {
                    Text("导入", color = palette.accent, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
        Hairline()
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
            CatalogTab("智能体", tab == 0) { tab = 0 }
            Spacer(Modifier.width(22.dp))
            CatalogTab("技能", tab == 1) { tab = 1 }
        }
        Hairline()
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp).navigationBarsPadding()
        ) {
            Spacer(Modifier.height(16.dp))
            if (tab == 0) {
                Kicker("CUSTOM / 全部由你创建")
                Spacer(Modifier.height(5.dp))
                Text(
                    "没有自带角色。名字、人设、头像和默认开关都由你决定。",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textSub
                )
                Spacer(Modifier.height(16.dp))
                if (vm.settings.customAgents.isEmpty()) {
                    Column(
                        Modifier.fillMaxWidth().background(palette.surface, RoundedCornerShape(3.dp))
                            .border(1.dp, palette.border, RoundedCornerShape(3.dp))
                            .padding(22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Kicker("EMPTY / 空", palette.accent)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "还没有智能体",
                            style = MaterialTheme.typography.headlineSmall,
                            color = palette.text
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "创建一个属于你的角色，之后点一下就能开始聊。",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.textSub,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(Modifier.height(14.dp))
                        Text(
                            "创建第一个 →",
                            style = MaterialTheme.typography.labelMedium,
                            color = palette.accent,
                            modifier = Modifier.clickable { creating = true }.padding(6.dp)
                        )
                    }
                } else {
                    Kicker("MY AGENTS / 我的智能体")
                    Spacer(Modifier.height(4.dp))
                    AgentCatalog(vm.settings.customAgents, onStart = vm::startAgent, onEdit = { editing = it })
                    AddCatalogRow("创建智能体") { creating = true }
                }
            } else {
                Kicker("SKILLS / 固定工作说明")
                Spacer(Modifier.height(5.dp))
                Text(
                    "技能会固定成新对话的系统提示词。提示词稳定，缓存也稳。",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textSub
                )
                Spacer(Modifier.height(16.dp))
                if (vm.settings.customSkills.isNotEmpty()) {
                    Kicker("MY SKILLS / 我的技能")
                    Spacer(Modifier.height(4.dp))
                    SkillCatalog(vm.settings.customSkills, vm::startSkill) { deleteSkill = it }
                    Spacer(Modifier.height(10.dp))
                }
                AddCatalogRow("导入技能文件") {
                    skillPicker.launch(arrayOf("text/*", "application/json", "application/octet-stream"))
                }
                Spacer(Modifier.height(22.dp))
                Kicker("BUILT IN / 内置技能")
                Spacer(Modifier.height(4.dp))
                SkillCatalog(Skills.builtin, vm::startSkill, null)
                Spacer(Modifier.height(16.dp))
                Text(
                    "支持 SKILL.md、Markdown 和纯文本。带 YAML 头的文件会自动读取名字和说明。",
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.textSub
                )
            }
            Spacer(Modifier.height(30.dp))
            Text(
                "LOCAL ONLY / 不额外收费，不离开这台手机",
                style = MaterialTheme.typography.labelSmall,
                color = palette.textSub.copy(alpha = 0.75f),
                modifier = Modifier.fillMaxWidth().padding(bottom = 26.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
    val target = deleteSkill
    if (target != null) {
        AlertDialog(
            onDismissRequest = { deleteSkill = null },
            title = { Text("删除这个技能？") },
            text = { Text("已经开始的对话会保留，但这里就找不到了。") },
            confirmButton = {
                TextButton(onClick = {
                    deleteSkill = null
                    vm.deleteSkill(target.id)
                    vm.showToast("已删除")
                }) { Text("删除", color = palette.danger) }
            },
            dismissButton = { TextButton(onClick = { deleteSkill = null }) { Text("取消") } }
        )
    }
}

@Composable
private fun CatalogTab(text: String, selected: Boolean, onClick: () -> Unit) {
    val palette = pal
    Column(
        Modifier.clickable(onClick = onClick).padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) palette.accent else palette.textSub
        )
        Spacer(Modifier.height(7.dp))
        Box(
            Modifier.width(22.dp).height(2.dp)
                .background(if (selected) palette.accent else Color.Transparent)
        )
    }
}

@Composable
private fun AgentCatalog(agents: List<Agent>, onStart: (Agent) -> Unit, onEdit: (Agent) -> Unit) {
    val palette = pal
    Column(Modifier.fillMaxWidth()) {
        agents.forEachIndexed { index, agent ->
            Row(
                Modifier.fillMaxWidth().clickable { onStart(agent) }.padding(vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                NumberLabel(index + 1)
                AgentAvatar(agent.avatarPath, agent.name, 40.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(agent.name, style = MaterialTheme.typography.titleSmall, color = palette.text, maxLines = 1)
                    Spacer(Modifier.height(2.dp))
                    Text(
                        agent.tagline.ifBlank { "自定义角色" },
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.textSub,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (agent.webSearch) Kicker("WEB", palette.accent)
                        if (agent.reasoning > 0) Kicker("THINK ${reasoningShort(agent.reasoning)}", palette.textSub)
                    }
                }
                IconButton(onClick = { onEdit(agent) }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Rounded.Edit, "编辑", tint = palette.textSub, modifier = Modifier.size(16.dp))
                }
            }
            Hairline()
        }
    }
}

@Composable
private fun SkillCatalog(skills: List<Skill>, onUse: (Skill) -> Unit, onDelete: ((Skill) -> Unit)?) {
    val palette = pal
    Column(Modifier.fillMaxWidth()) {
        skills.forEachIndexed { index, skill ->
            Row(
                Modifier.fillMaxWidth().clickable { onUse(skill) }.padding(vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                NumberLabel(index + 1)
                AgentAvatar("", skill.name, 36.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(skill.name, style = MaterialTheme.typography.titleSmall, color = palette.text, maxLines = 1)
                    if (skill.description.isNotBlank()) {
                        Spacer(Modifier.height(2.dp))
                        Text(
                            skill.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.textSub,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                val remove = onDelete
                if (remove != null) {
                    IconButton(onClick = { remove(skill) }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Rounded.Delete, "删除", tint = palette.textSub, modifier = Modifier.size(16.dp))
                    }
                } else {
                    Text("↗", style = MaterialTheme.typography.labelMedium, color = palette.textSub)
                }
            }
            Hairline()
        }
    }
}

@Composable
private fun AddCatalogRow(text: String, onClick: () -> Unit) {
    val palette = pal
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(26.dp).border(1.dp, palette.border, RoundedCornerShape(2.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.Add, null, tint = palette.accent, modifier = Modifier.size(15.dp))
        }
        Spacer(Modifier.width(12.dp))
        Text(text, style = MaterialTheme.typography.labelMedium, color = palette.accent)
        Spacer(Modifier.weight(1f))
        Text("＋", style = MaterialTheme.typography.labelMedium, color = palette.textSub)
    }
    Hairline()
}

@Composable
private fun AgentEditorScreen(vm: ChatViewModel, existing: Agent?, onBack: () -> Unit) {
    val palette = pal
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var draft by remember(existing?.id) { mutableStateOf(existing ?: Agent()) }
    var confirmDelete by remember { mutableStateOf(false) }
    var cropSource by remember { mutableStateOf<Bitmap?>(null) }
    val avatarPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            scope.launch {
                val bitmap = withContext(Dispatchers.IO) { Images.loadForCrop(context, uri) }
                if (bitmap != null) {
                    cropSource = bitmap
                } else {
                    vm.showToast("图片读不出来，换一张试试")
                }
            }
        }
    }
    BackHandler { onBack() }
    Column(Modifier.fillMaxSize().background(palette.bg).statusBarsPadding().imePadding()) {
        Row(
            Modifier.fillMaxWidth().height(66.dp).padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Rounded.ArrowBack, "返回", tint = palette.text, modifier = Modifier.size(21.dp))
            }
            Spacer(Modifier.width(4.dp))
            Column(Modifier.weight(1f)) {
                Kicker("AGENT / 角色设定")
                Spacer(Modifier.height(2.dp))
                Text(
                    if (existing == null) "创建智能体" else "编辑智能体",
                    style = MaterialTheme.typography.titleLarge,
                    color = palette.text
                )
            }
            TextButton(onClick = {
                vm.saveAgent(draft)
                vm.showToast("已保存")
                onBack()
            }) { Text("保存", color = palette.accent, style = MaterialTheme.typography.labelMedium) }
        }
        Hairline()
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp).navigationBarsPadding()
        ) {
            Spacer(Modifier.height(16.dp))
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(contentAlignment = Alignment.BottomEnd) {
                    AgentAvatar(draft.avatarPath, draft.name, 78.dp)
                    Box(
                        Modifier.padding(end = 2.dp, bottom = 2.dp).size(26.dp)
                            .clip(CircleShape).background(palette.accent)
                            .clickable { avatarPicker.launch("image/*") },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.Edit, "更换头像", tint = Color.White, modifier = Modifier.size(14.dp))
                    }
                }
                Column(Modifier.weight(1f)) {
                    Text("头像", style = MaterialTheme.typography.titleSmall, color = palette.text)
                    Spacer(Modifier.height(3.dp))
                    Text(
                        "点圆圈选图，双指缩放，拖动调整位置，最后裁成圆形。",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.textSub
                    )
                    TextButton(onClick = { avatarPicker.launch("image/*") }) {
                        Text("选择图片", color = palette.accent)
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            OutlinedTextField(
                value = draft.name,
                onValueChange = { draft = draft.copy(name = it) },
                label = { Text("名字") },
                placeholder = { Text("比如：小红书文案助手") },
                singleLine = true,
                shape = RoundedCornerShape(3.dp),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = draft.tagline,
                onValueChange = { draft = draft.copy(tagline = it) },
                label = { Text("一句话介绍") },
                placeholder = { Text("它擅长什么，适合什么时候找它") },
                singleLine = true,
                shape = RoundedCornerShape(3.dp),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("人设提示词", style = MaterialTheme.typography.titleSmall, color = palette.text)
                Spacer(Modifier.weight(1f))
                Text(
                    "${draft.prompt.length} 字",
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.textSub
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                "写清楚它是谁、怎么说话、擅长什么、不要做什么。把固定的内容放前面，不要写日期这类会变的信息。",
                style = MaterialTheme.typography.bodySmall,
                color = palette.textSub
            )
            Spacer(Modifier.height(8.dp))
            Surface(
                color = palette.surface,
                shape = RoundedCornerShape(3.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, palette.border)
            ) {
                Box(Modifier.fillMaxWidth().height(230.dp).padding(14.dp)) {
                    if (draft.prompt.isEmpty()) {
                        Text(
                            "例如：你是一名小红书文案助手。语气轻松，句子短，先给三个标题，再给正文，最后给标签。",
                            style = MaterialTheme.typography.bodyMedium,
                            color = palette.textSub.copy(alpha = 0.7f)
                        )
                    }
                    BasicTextField(
                        value = draft.prompt,
                        onValueChange = { draft = draft.copy(prompt = it) },
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = palette.text),
                        cursorBrush = SolidColor(palette.accent),
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            Surface(
                color = palette.surface,
                shape = RoundedCornerShape(3.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, palette.border)
            ) {
                Column {
                    Row(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Public, null, tint = palette.accent, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text("默认打开联网", style = MaterialTheme.typography.bodyLarge, color = palette.text)
                            Text("只有 Claude、Gemini、OpenRouter 和通义支持", style = MaterialTheme.typography.labelSmall, color = palette.textSub)
                        }
                        Switch(
                            checked = draft.webSearch,
                            onCheckedChange = { on -> draft = draft.copy(webSearch = on) }
                        )
                    }
                    HorizontalDivider(color = palette.border, modifier = Modifier.padding(start = 39.dp))
                    Column(Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Psychology, null, tint = palette.accent, modifier = Modifier.size(17.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("默认思考强度", style = MaterialTheme.typography.bodyLarge, color = palette.text)
                        }
                        Spacer(Modifier.height(10.dp))
                        Row(
                            Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                "自动" to 0, "低" to 1, "中" to 2, "高" to 3,
                                "极高 xhigh" to 4, "最高 max" to 5
                            ).forEach { (label, level) ->
                                PillChip(label, draft.reasoning == level) {
                                    draft = draft.copy(reasoning = level)
                                }
                            }
                        }
                    }
                }
            }
            if (existing != null) {
                Spacer(Modifier.height(16.dp))
                Text(
                    "删除这个智能体",
                    style = MaterialTheme.typography.labelLarge,
                    color = palette.danger,
                    modifier = Modifier.clip(RoundedCornerShape(3.dp))
                        .clickable { confirmDelete = true }.padding(8.dp)
                )
            }
            Spacer(Modifier.height(30.dp))
        }
    }
    val source = cropSource
    if (source != null) {
        AvatarCropDialog(
            bitmap = source,
            onCancel = { cropSource = null },
            onDone = { path ->
                draft = draft.copy(avatarPath = path)
                cropSource = null
            }
        )
    }
    val deleteTarget = existing
    if (confirmDelete && deleteTarget != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("删除这个智能体？") },
            text = { Text("已经开始的对话会保留，但这里就找不到了。") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    vm.deleteAgent(deleteTarget.id)
                    vm.showToast("已删除")
                    onBack()
                }) { Text("删除", color = palette.danger) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("取消") } }
        )
    }
}

private fun reasoningShort(level: Int): String = when (level) {
    1 -> "低"
    2 -> "中"
    3 -> "高"
    4 -> "极高"
    5 -> "最高"
    else -> "自动"
}
