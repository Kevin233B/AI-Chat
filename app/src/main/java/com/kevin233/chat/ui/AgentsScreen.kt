package com.kevin233.chat.ui

import androidx.activity.compose.BackHandler
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kevin233.chat.ChatViewModel
import com.kevin233.chat.Screen
import com.kevin233.chat.data.Agent
import com.kevin233.chat.data.DefaultSystemPrompt

@Composable
fun AgentsScreen(vm: ChatViewModel) {
    var creating by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Agent?>(null) }
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
            Modifier.fillMaxWidth().height(58.dp).padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { vm.screen = Screen.CHAT }) {
                Icon(Icons.Rounded.ArrowBack, "返回", tint = palette.text, modifier = Modifier.size(22.dp))
            }
            Column(Modifier.weight(1f)) {
                Text("智能体", style = MaterialTheme.typography.titleLarge, color = palette.text)
            }
            TextButton(onClick = { creating = true }) {
                Text("创建", color = palette.accent, fontWeight = FontWeight.Medium)
            }
        }
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp).navigationBarsPadding()
        ) {
            Surface(color = palette.accentSoft, shape = RoundedCornerShape(18.dp)) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
                    Icon(CacheStack, null, tint = palette.accent, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(9.dp))
                    Text(
                        "角色设定会固定成新对话的系统提示词。设定越稳定，缓存越容易命中，聊起来也更省钱。",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.accentText
                    )
                }
            }
            Spacer(Modifier.height(18.dp))
            if (vm.settings.customAgents.isNotEmpty()) {
                SectionLabel("我的智能体")
                Spacer(Modifier.height(8.dp))
                AgentRows(vm.settings.customAgents, onStart = vm::startAgent, onEdit = { editing = it })
                Spacer(Modifier.height(12.dp))
            }
            AddAgentRow { creating = true }
            Spacer(Modifier.height(20.dp))
            SectionLabel("推荐")
            Spacer(Modifier.height(8.dp))
            AgentRows(com.kevin233.chat.data.Agents.builtin, onStart = vm::startAgent, onEdit = null)
            Spacer(Modifier.height(30.dp))
            Text(
                "智能体只影响提示词和默认开关，不会额外收费。所有对话仍然只存在这台手机。",
                style = MaterialTheme.typography.labelSmall,
                color = palette.textSub,
                modifier = Modifier.padding(bottom = 24.dp)
            )
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        color = pal.textSub,
        modifier = Modifier.padding(start = 4.dp)
    )
}

@Composable
private fun AgentRows(agents: List<Agent>, onStart: (Agent) -> Unit, onEdit: ((Agent) -> Unit)?) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        agents.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { agent ->
                    AgentCard(agent, Modifier.weight(1f), onStart, onEdit)
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun AgentCard(
    agent: Agent,
    modifier: Modifier,
    onStart: (Agent) -> Unit,
    onEdit: ((Agent) -> Unit)?
) {
    val palette = pal
    Surface(
        color = palette.surface,
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, palette.border),
        modifier = modifier.heightIn(min = 164.dp)
    ) {
        Column(
            Modifier.fillMaxWidth().clickable { onStart(agent) }.padding(14.dp)
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Box(
                    Modifier.size(42.dp).clip(RoundedCornerShape(13.dp)).background(palette.accentSoft),
                    contentAlignment = Alignment.Center
                ) {
                    Text(agent.emoji, fontSize = 22.sp)
                }
                Spacer(Modifier.weight(1f))
                val editAction = onEdit
                if (editAction != null) {
                    IconButton(onClick = { editAction(agent) }, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Rounded.Edit, "编辑", tint = palette.textSub, modifier = Modifier.size(15.dp))
                    }
                }
            }
            Spacer(Modifier.height(9.dp))
            Text(
                agent.name,
                style = MaterialTheme.typography.titleSmall,
                color = palette.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(3.dp))
            Text(
                agent.tagline,
                style = MaterialTheme.typography.bodySmall,
                color = palette.textSub,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.weight(1f))
            if (agent.webSearch || agent.reasoning > 0) {
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    if (agent.webSearch) MiniChip("联网")
                    if (agent.reasoning > 0) MiniChip("思考 ${reasoningShort(agent.reasoning)}")
                }
            }
        }
    }
}

@Composable
private fun MiniChip(text: String) {
    val palette = pal
    Row(
        Modifier.clip(CircleShape).background(palette.accentSoft)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text, style = MaterialTheme.typography.labelSmall, color = palette.accentText)
    }
}

@Composable
private fun AddAgentRow(onClick: () -> Unit) {
    val palette = pal
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
            .border(1.dp, palette.border, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 13.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Rounded.Add, null, tint = palette.accent, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text("创建你自己的智能体", style = MaterialTheme.typography.labelLarge, color = palette.accent)
    }
}

@Composable
private fun AgentEditorScreen(vm: ChatViewModel, existing: Agent?, onBack: () -> Unit) {
    val palette = pal
    var draft by remember(existing?.id) { mutableStateOf(existing ?: Agent(emoji = "🤖")) }
    var confirmDelete by remember { mutableStateOf(false) }
    BackHandler { onBack() }
    Column(Modifier.fillMaxSize().background(palette.bg).statusBarsPadding().imePadding()) {
        Row(
            Modifier.fillMaxWidth().height(58.dp).padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Rounded.ArrowBack, "返回", tint = palette.text, modifier = Modifier.size(22.dp))
            }
            Column(Modifier.weight(1f)) {
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
            }) { Text("保存", color = palette.accent, fontWeight = FontWeight.Medium) }
        }
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp).navigationBarsPadding()
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = draft.emoji,
                    onValueChange = { draft = draft.copy(emoji = it.take(4)) },
                    label = { Text("头像") },
                    singleLine = true,
                    textStyle = TextStyle(fontSize = 22.sp),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.width(92.dp)
                )
                OutlinedTextField(
                    value = draft.name,
                    onValueChange = { draft = draft.copy(name = it) },
                    label = { Text("名字") },
                    placeholder = { Text("比如：小红书文案助手") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = draft.tagline,
                onValueChange = { draft = draft.copy(tagline = it) },
                label = { Text("一句话介绍") },
                placeholder = { Text("它擅长什么，适合什么时候找它") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
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
                shape = RoundedCornerShape(16.dp),
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
                shape = RoundedCornerShape(16.dp),
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
                    modifier = Modifier.clip(RoundedCornerShape(10.dp))
                        .clickable { confirmDelete = true }.padding(8.dp)
                )
            }
            Spacer(Modifier.height(30.dp))
        }
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
