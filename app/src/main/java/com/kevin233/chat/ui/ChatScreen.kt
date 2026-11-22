package com.kevin233.chat.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.platform.LocalContext
import com.kevin233.chat.util.Images
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kevin233.chat.ChatViewModel
import com.kevin233.chat.Screen
import com.kevin233.chat.data.Conversation
import com.kevin233.chat.data.Message
import com.kevin233.chat.data.Presets
import com.kevin233.chat.util.Tokens
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ChatScreen(vm: ChatViewModel) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val wide = maxWidth >= 840.dp
        if (wide) {
            Row(Modifier.fillMaxSize().background(pal.bg)) {
                Sidebar(vm, Modifier.width(300.dp).fillMaxHeight(), wide = true, onPicked = {})
                Box(Modifier.width(1.dp).fillMaxHeight().background(pal.border))
                ChatPane(vm, wide = true, onMenu = {}, Modifier.weight(1f))
            }
        } else {
            ModalNavigationDrawer(
                drawerState = drawerState,
                scrimColor = Color.Black.copy(alpha = 0.35f),
                drawerContent = {
                    ModalDrawerSheet(
                        drawerContainerColor = pal.bg,
                        drawerShape = RoundedCornerShape(topEnd = 26.dp, bottomEnd = 26.dp),
                        modifier = Modifier.width(300.dp)
                    ) {
                        Sidebar(vm, Modifier.fillMaxSize(), wide = false) {
                            scope.launch { drawerState.close() }
                        }
                    }
                }
            ) {
                ChatPane(vm, wide = false, onMenu = { scope.launch { drawerState.open() } })
            }
        }
    }
    BackHandler(enabled = vm.screen == Screen.SETTINGS || vm.screen == Screen.CONFIG_EDIT) {
        vm.screen = Screen.CHAT
    }
}

@Composable
private fun ChatPane(
    vm: ChatViewModel,
    wide: Boolean,
    onMenu: () -> Unit,
    modifier: Modifier = Modifier
) {
    val conv = vm.currentConversation
    val palette = pal
    val config = vm.activeConfig
    var input by rememberSaveable(conv?.id) { mutableStateOf("") }
    val listState = rememberLazyListState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var attachments by remember(conv?.id) { mutableStateOf<List<String>>(emptyList()) }
    var attaching by remember { mutableStateOf(false) }
    var previewImage by remember { mutableStateOf<String?>(null) }
    val pickImages = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        if (uris.isNotEmpty()) {
            scope.launch {
                attaching = true
                val room = (4 - attachments.size).coerceAtLeast(0)
                val data = withContext(Dispatchers.IO) {
                    uris.take(room).mapNotNull { uri -> Images.toDataUrl(context, uri) }
                }
                attachments = (attachments + data).take(4)
                attaching = false
                if (data.isEmpty()) vm.showToast("图片读不出来，换一张试试")
            }
        }
    }

    var lastCount by remember(conv?.id) { mutableIntStateOf(conv?.messages?.size ?: 0) }
    LaunchedEffect(conv?.messages?.size) {
        val size = conv?.messages?.size ?: 0
        if (size > lastCount) listState.animateScrollToItem(0)
        lastCount = size
    }
    LaunchedEffect(vm.pendingJumpMessageId, conv?.id) {
        val target = vm.pendingJumpMessageId
        if (target != null && conv != null) {
            val index = conv.messages.indexOfFirst { it.id == target }
            if (index >= 0) {
                listState.scrollToItem((conv.messages.size - 1 - index).coerceAtLeast(0))
            }
            vm.clearJump()
        }
    }
    LaunchedEffect(vm.highlightedMessageId) {
        if (vm.highlightedMessageId != null) {
            kotlinx.coroutines.delay(2600)
            vm.clearHighlight()
        }
    }

    Column(
        modifier
            .fillMaxSize()
            .background(palette.bg)
            .statusBarsPadding()
    ) {
        val topTitle = conv?.title ?: "AI Chat"
        TopBar(
            title = topTitle,
            subtitle = config?.let { "${it.name}，${it.model}" } ?: "还没配置模型",
            onMenu = onMenu,
            wide = wide,
            onNew = { vm.newChat() },
            onSearch = { vm.screen = Screen.HISTORY },
            onSettings = { vm.screen = Screen.SETTINGS }
        )
        if (!vm.isConfigured) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Surface(
                    color = palette.accentSoft,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().clickable { vm.screen = Screen.SETTINGS }
                ) {
                    Row(
                        Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.Key, null, tint = palette.accent, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "还差一步：填上 API Key 就能聊天了",
                            style = MaterialTheme.typography.bodyMedium,
                            color = palette.accentText,
                            modifier = Modifier.weight(1f)
                        )
                        Text("去设置", style = MaterialTheme.typography.labelLarge, color = palette.accent)
                        Icon(Icons.Rounded.ChevronRight, null, tint = palette.accent, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
        Box(Modifier.weight(1f), contentAlignment = Alignment.TopCenter) {
            if (conv == null || conv.messages.isEmpty()) {
                EmptyState(vm, Modifier.fillMaxSize())
            } else {
                val messages = conv.messages
                LazyColumn(
                    state = listState,
                    reverseLayout = true,
                    modifier = if (wide) Modifier.fillMaxHeight().widthIn(max = 860.dp).fillMaxWidth()
                    else Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        start = if (wide) 32.dp else 16.dp,
                        end = if (wide) 32.dp else 16.dp,
                        top = 12.dp,
                        bottom = 8.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    items(messages.asReversed(), key = { it.id }) { msg ->
                        MessageRow(
                            vm = vm,
                            msg = msg,
                            streaming = vm.streaming && msg.id == messages.lastOrNull()?.id,
                            wide = wide,
                            highlight = msg.id == vm.highlightedMessageId,
                            agentName = conv?.agentName.orEmpty(),
                            searching = vm.searching,
                            onImageClick = { previewImage = it }
                        )
                    }
                }
            }
        }
        if (!vm.streaming) {
            val last = conv?.messages?.lastOrNull()
            if (last != null && last.isUser) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { vm.regenerate() }
                        .padding(horizontal = 20.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.Refresh, null, tint = palette.accent, modifier = Modifier.size(13.dp))
                    Spacer(Modifier.width(5.dp))
                    Text(
                        "上一条还没有回复，点这里重试",
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.accent
                    )
                }
            }
        }
        Composer(
            value = input,
            onValueChange = { input = it },
            streaming = vm.streaming,
            conversation = conv,
            configured = vm.isConfigured,
            reasoning = conv?.reasoning ?: vm.settings.reasoning,
            reasoningSupported = Presets.supportsReasoning(config),
            onReasoningChange = { vm.setConversationReasoning(it) },
            webSearch = conv?.webSearch == true,
            onToggleWebSearch = { vm.toggleWebSearch() },
            vision = config?.vision == true,
            images = attachments,
            attaching = attaching,
            onPickImages = { pickImages.launch("image/*") },
            onRemoveImage = { index -> attachments = attachments.filterIndexed { i, _ -> i != index } },
            onSend = {
                val text = input.trim()
                if (text.isNotEmpty() || attachments.isNotEmpty()) {
                    val sending = attachments
                    input = ""
                    attachments = emptyList()
                    vm.send(text, sending)
                }
            },
            onStop = { vm.stop() }
        )
    }
    if (previewImage != null) {
        ImagePreviewDialog(previewImage!!) { previewImage = null }
    }
}

@Composable
private fun TopBar(
    title: String,
    subtitle: String,
    onMenu: () -> Unit,
    wide: Boolean,
    onNew: () -> Unit,
    onSearch: () -> Unit,
    onSettings: () -> Unit
) {
    val palette = pal
    Row(
        Modifier
            .fillMaxWidth()
            .height(58.dp)
            .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (!wide) {
            IconButton(onClick = onMenu) {
                Icon(Icons.Rounded.Menu, "菜单", tint = palette.text, modifier = Modifier.size(23.dp))
            }
        } else {
            Spacer(Modifier.width(14.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                color = palette.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = palette.textSub,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        IconButton(onClick = onSearch) {
            Icon(Icons.Rounded.Search, "搜索历史", tint = palette.textSub, modifier = Modifier.size(20.dp))
        }
        IconButton(onClick = onNew) {
            Icon(Icons.Rounded.Edit, "新对话", tint = palette.textSub, modifier = Modifier.size(20.dp))
        }
        IconButton(onClick = onSettings) {
            Icon(Icons.Rounded.Settings, "设置", tint = palette.textSub, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(2.dp))
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun Sidebar(
    vm: ChatViewModel,
    modifier: Modifier,
    wide: Boolean,
    onPicked: () -> Unit
) {
    val palette = pal
    var renameTarget by remember { mutableStateOf<Conversation?>(null) }
    var deleteTarget by remember { mutableStateOf<Conversation?>(null) }
    var editingTitle by remember { mutableStateOf("") }

    Column(
        modifier
            .background(if (wide) palette.bg else palette.bg)
            .statusBarsPadding()
    ) {
        Row(
            Modifier.padding(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppLogo(34.dp, 10.dp)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text("AI Chat", style = MaterialTheme.typography.titleLarge, color = palette.text)
            }
        }
        Box(
            Modifier
                .padding(horizontal = 14.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(palette.accent)
                .clickable {
                    vm.newChat()
                    onPicked()
                }
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Add, null, tint = Color.White, modifier = Modifier.size(19.dp))
                Spacer(Modifier.width(6.dp))
                Text("新建对话", color = Color.White, style = MaterialTheme.typography.labelLarge)
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(
            Modifier
                .padding(horizontal = 14.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable {
                    vm.screen = Screen.AGENTS
                    onPicked()
                }
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Rounded.Groups, null, tint = palette.textSub, modifier = Modifier.size(17.dp))
            Spacer(Modifier.width(8.dp))
            Text("智能体与技能", style = MaterialTheme.typography.bodySmall, color = palette.text, modifier = Modifier.weight(1f))
            Text("${vm.agents.size + vm.skills.size}", style = MaterialTheme.typography.labelSmall, color = palette.textSub)
        }
        Spacer(Modifier.height(6.dp))
        Row(
            Modifier
                .padding(horizontal = 14.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(palette.surfaceAlt)
                .clickable {
                    vm.screen = Screen.HISTORY
                    onPicked()
                }
                .padding(horizontal = 12.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Rounded.Search, null, tint = palette.textSub, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text("搜索聊天记录", style = MaterialTheme.typography.bodySmall, color = palette.textSub, modifier = Modifier.weight(1f))
            Text("历史", style = MaterialTheme.typography.labelSmall, color = palette.textSub.copy(alpha = 0.6f))
        }
        Spacer(Modifier.height(10.dp))
        CacheSummary(vm, Modifier.padding(start = 14.dp, end = 14.dp, top = 2.dp, bottom = 8.dp))
        Row(
            Modifier.padding(start = 20.dp, end = 16.dp, top = 10.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("最近对话", style = MaterialTheme.typography.labelMedium, color = palette.textSub, modifier = Modifier.weight(1f))
            Text(
                "全部",
                style = MaterialTheme.typography.labelSmall,
                color = palette.accent,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable {
                        vm.screen = Screen.HISTORY
                        onPicked()
                    }
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            )
        }
        LazyColumn(Modifier.weight(1f)) {
            items(vm.sortedConversations, key = { it.id }) { conv ->
                val selected = conv.id == vm.activeId
                Row(
                    Modifier
                        .padding(horizontal = 10.dp, vertical = 2.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (selected) palette.surfaceAlt else Color.Transparent)
                        .combinedClickable(
                            onClick = {
                                vm.selectChat(conv.id)
                                onPicked()
                            },
                            onLongClick = {
                                renameTarget = conv
                                editingTitle = conv.title
                            }
                        )
                        .padding(horizontal = 10.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            conv.title,
                            style = MaterialTheme.typography.bodyMedium,
                            fontSize = 14.sp,
                            color = palette.text,
                            fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            "${timeLabel(conv.updatedAt)}，${conv.messages.count { it.isUser }} 条提问",
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.textSub,
                            maxLines = 1
                        )
                    }
                    Box {
                        var menu by remember { mutableStateOf(false) }
                        IconButton(onClick = { menu = true }, modifier = Modifier.size(30.dp)) {
                            Icon(Icons.Rounded.MoreHoriz, "更多", tint = palette.textSub, modifier = Modifier.size(17.dp))
                        }
                        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                            DropdownMenuItem(
                                text = { Text("重命名") },
                                leadingIcon = { Icon(Icons.Rounded.Edit, null, Modifier.size(18.dp)) },
                                onClick = {
                                    menu = false
                                    renameTarget = conv
                                    editingTitle = conv.title
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("删除") },
                                leadingIcon = { Icon(Icons.Rounded.Delete, null, Modifier.size(18.dp)) },
                                onClick = {
                                    menu = false
                                    deleteTarget = conv
                                }
                            )
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(8.dp)) }
        }
        HorizontalDivider(color = palette.border)
        Column(Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
            SettingRow(
                icon = Icons.Rounded.Settings,
                title = "设置与 API",
                subtitle = vm.activeConfig?.let { "${it.name}，${it.model}" } ?: "还没配置",
                onClick = {
                    vm.screen = Screen.SETTINGS
                    onPicked()
                }
            )
        }
    }

    if (renameTarget != null) {
        AlertDialog(
            onDismissRequest = { renameTarget = null },
            title = { Text("改个名字") },
            text = {
                androidx.compose.material3.OutlinedTextField(
                    value = editingTitle,
                    onValueChange = { editingTitle = it },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    renameTarget?.let { vm.renameChat(it.id, editingTitle) }
                    renameTarget = null
                }) { Text("保存") }
            },
            dismissButton = { TextButton(onClick = { renameTarget = null }) { Text("取消") } }
        )
    }
    if (deleteTarget != null) {
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("删除这个对话？") },
            text = { Text("删掉就找不回来了，确定吗？") },
            confirmButton = {
                TextButton(onClick = {
                    deleteTarget?.let { vm.deleteChat(it.id) }
                    deleteTarget = null
                }) { Text("删除", color = pal.danger) }
            },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("取消") } }
        )
    }
}

@Composable
private fun CacheSummary(vm: ChatViewModel, modifier: Modifier = Modifier) {
    val palette = pal
    val conv = vm.currentConversation
    val profile = Presets.cacheProfile(vm.activeConfig)
    val total = (conv?.hitTokens ?: 0) + (conv?.missTokens ?: 0)
    val rate = if (total > 0) (conv?.hitTokens ?: 0).toFloat() / total else 0f
    val prefix = conv?.let { Tokens.estimate(it.systemPrompt) + Tokens.estimateMessages(it.messages) } ?: 0
    val supported = profile.mode != com.kevin233.chat.data.CacheMode.NONE
    val ready = profile.minPrefix == 0 || prefix >= profile.minPrefix
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Icon(
            CacheStack,
            null,
            tint = if (total > 0 && rate > 0f) palette.ok else palette.textSub,
            modifier = Modifier.size(13.dp)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            when {
                !supported -> "这个模型没有缓存"
                total > 0 -> "缓存命中 ${(rate * 100).toInt()}%，已省 ${Tokens.format(conv?.hitTokens ?: 0)} token"
                ready -> "缓存已就绪，继续聊更省"
                else -> "缓存预热中，还差 ${profile.minPrefix - prefix} token"
            },
            style = MaterialTheme.typography.labelSmall,
            color = palette.textSub,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun EmptyState(vm: ChatViewModel, modifier: Modifier = Modifier) {
    val palette = pal
    val conv = vm.currentConversation
    val agentName = conv?.agentName.orEmpty()
    val suggestions = listOf(
        "帮我写一段自我介绍",
        "用大白话解释一下什么是缓存",
        "帮我列一个今天的学习计划",
        "把这段话翻译成英文：今天天气很好"
    )
    Box(modifier, contentAlignment = Alignment.Center) {
        Column(
            Modifier
                .widthIn(max = 640.dp)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (agentName.isNotBlank()) {
                InitialBadge(agentName, 66.dp)
            }
            Spacer(Modifier.height(18.dp))
            Text(
                if (agentName.isNotBlank()) "我是$agentName" else "你好，我是 AI Chat",
                style = MaterialTheme.typography.headlineSmall,
                color = palette.text,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(22.dp))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                suggestions.chunked(2).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        row.forEach { s ->
                            Surface(
                                color = palette.surface,
                                shape = RoundedCornerShape(14.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, palette.border),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { vm.send(s) }
                            ) {
                                Text(
                                    s,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = palette.text,
                                    maxLines = 2,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp)
                                )
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(18.dp))
            if (agentName.isBlank()) {
                TextButton(onClick = { vm.screen = Screen.AGENTS }) {
                    Text("或者，试试智能体", color = palette.accent, style = MaterialTheme.typography.labelMedium)
                }
            }
            Spacer(Modifier.height(14.dp))
            val config = vm.activeConfig
            val profile = Presets.cacheProfile(config)
            Surface(color = palette.accentSoft, shape = RoundedCornerShape(14.dp)) {
                Row(
                    Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(CacheStack, null, tint = palette.accent, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(7.dp))
                    Text(
                        if (vm.isConfigured) "缓存助手已开启，${profile.label}"
                        else "先花 1 分钟填个 API Key，就能开始",
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.accentText
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MessageRow(
    vm: ChatViewModel,
    msg: Message,
    streaming: Boolean,
    wide: Boolean,
    highlight: Boolean = false,
    agentName: String = "",
    searching: Boolean = false,
    onImageClick: (String) -> Unit = {}
) {
    val palette = pal
    val clipboard = LocalClipboardManager.current
    val maxBubble = if (wide) 560.dp else 300.dp
    var confirmRewind by remember { mutableStateOf(false) }
    if (msg.isUser) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            Column(horizontalAlignment = Alignment.End) {
                if (highlight) JumpHint()
                Box {
                var menu by remember { mutableStateOf(false) }
                Box(
                    Modifier
                        .widthIn(max = maxBubble)
                        .clip(RoundedCornerShape(20.dp, 20.dp, 5.dp, 20.dp))
                        .background(palette.userBubble)
                        .combinedClickable(onClick = { menu = true }, onLongClick = { menu = true })
                        .padding(horizontal = 15.dp, vertical = 11.dp)
                ) {
                    Column {
                        if (msg.images.isNotEmpty()) {
                            Row(
                                Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                msg.images.forEach { image ->
                                    DataImage(
                                        dataUrl = image,
                                        modifier = Modifier.size(118.dp),
                                        onClick = { onImageClick(image) }
                                    )
                                }
                            }
                            if (msg.content.isNotBlank()) Spacer(Modifier.height(7.dp))
                        }
                        if (msg.content.isNotBlank()) {
                            Text(
                                text = msg.content,
                                style = MaterialTheme.typography.bodyLarge,
                                color = palette.userBubbleText
                            )
                        }
                        if (msg.searchContext.isNotBlank()) {
                            Spacer(Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Rounded.Public,
                                    null,
                                    tint = palette.accent,
                                    modifier = Modifier.size(11.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    "联网搜索",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = palette.accent
                                )
                            }
                        }
                    }
                }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(
                        text = { Text("复制") },
                        leadingIcon = { Icon(Icons.Rounded.ContentCopy, null, Modifier.size(18.dp)) },
                        onClick = {
                            clipboard.setText(AnnotatedString(msg.content))
                            menu = false
                            vm.showToast("已复制")
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("回到这里重新说") },
                        leadingIcon = { Icon(Icons.Rounded.Refresh, null, Modifier.size(18.dp)) },
                        onClick = {
                            menu = false
                            confirmRewind = true
                        }
                    )
                }
            }
            }
        }
    } else {

    Column(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (agentName.isNotBlank()) {
                InitialBadge(agentName, 26.dp)
                Spacer(Modifier.width(9.dp))
            }
            Text(agentName.ifBlank { "AI Chat" }, style = MaterialTheme.typography.labelMedium, color = palette.textSub)
            Spacer(Modifier.weight(1f))
            var menu by remember { mutableStateOf(false) }
            IconButton(onClick = { menu = true }, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Rounded.MoreHoriz, null, tint = palette.textSub, modifier = Modifier.size(16.dp))
            }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(
                    text = { Text("复制回复") },
                    leadingIcon = { Icon(Icons.Rounded.ContentCopy, null, Modifier.size(18.dp)) },
                    onClick = {
                        clipboard.setText(AnnotatedString(msg.content))
                        menu = false
                        vm.showToast("已复制")
                    }
                )
                if (!streaming) {
                    DropdownMenuItem(
                        text = { Text("重新回答") },
                        leadingIcon = { Icon(Icons.Rounded.Refresh, null, Modifier.size(18.dp)) },
                        onClick = {
                            menu = false
                            vm.regenerate()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("回到这里") },
                        leadingIcon = { Icon(Icons.Rounded.Edit, null, Modifier.size(18.dp)) },
                        onClick = {
                            menu = false
                            confirmRewind = true
                        }
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        if (highlight) {
            JumpHint()
            Spacer(Modifier.height(6.dp))
        }
        if (msg.reasoning.isNotBlank()) {
            ReasoningBlock(msg.reasoning, streaming)
            Spacer(Modifier.height(8.dp))
        }
        if (msg.content.isBlank() && streaming) {
            Row(Modifier.padding(start = 2.dp, top = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                DotsLoading()
                Spacer(Modifier.width(8.dp))
                Text(
                    if (searching) "正在联网搜索…" else "正在思考…",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textSub
                )
            }
        } else if (msg.error.isNotBlank()) {
            Surface(color = palette.danger.copy(alpha = 0.07f), shape = RoundedCornerShape(14.dp)) {
                Column(Modifier.padding(14.dp)) {
                    Text("没成功", style = MaterialTheme.typography.titleSmall, color = palette.danger)
                    Spacer(Modifier.height(4.dp))
                    Text(msg.error, style = MaterialTheme.typography.bodySmall, color = palette.textSub)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "重试一次",
                        style = MaterialTheme.typography.labelLarge,
                        color = palette.danger,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { vm.regenerate() }
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            }
        } else {
            androidx.compose.foundation.text.selection.SelectionContainer {
                MarkdownText(msg.content, onImageClick = onImageClick)
            }
        }
        if (!streaming && (msg.cacheHit + msg.cacheMiss) > 0) {
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                CacheBadge(msg.cacheHit, msg.cacheMiss)
                Spacer(Modifier.weight(1f))
                Row {
                    IconButton(
                        onClick = {
                            clipboard.setText(AnnotatedString(msg.content))
                            vm.showToast("已复制")
                        },
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            Icons.Rounded.ContentCopy,
                            "复制",
                            tint = palette.textSub.copy(alpha = 0.8f),
                            modifier = Modifier.size(15.dp)
                        )
                    }
                    IconButton(onClick = { vm.regenerate() }, modifier = Modifier.size(30.dp)) {
                        Icon(
                            Icons.Rounded.Refresh,
                            "重新回答",
                            tint = palette.textSub.copy(alpha = 0.8f),
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }
        }
    }
    }

    if (confirmRewind) {
        AlertDialog(
            onDismissRequest = { confirmRewind = false },
            title = { Text("回到这里？") },
            text = { Text("这里之后的消息会被删掉。删改历史会让缓存重新计算，下一轮会贵一点点。") },
            confirmButton = {
                TextButton(onClick = {
                    confirmRewind = false
                    vm.rewindTo(msg.id)
                }) { Text("确定") }
            },
            dismissButton = { TextButton(onClick = { confirmRewind = false }) { Text("取消") } }
        )
    }
}

@Composable
private fun JumpHint() {
    val palette = pal
    Row(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(palette.accentSoft)
            .padding(horizontal = 9.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Rounded.Search, null, tint = palette.accent, modifier = Modifier.size(11.dp))
        Spacer(Modifier.width(4.dp))
        Text("从搜索定位到这里", style = MaterialTheme.typography.labelSmall, color = palette.accentText)
    }
}

@Composable
private fun ReasoningBlock(text: String, streaming: Boolean) {
    val palette = pal
    var expanded by rememberSaveable { mutableStateOf(streaming) }
    val rotate by animateFloatAsState(if (expanded) 180f else 0f, label = "chevron")
    Surface(color = palette.surfaceAlt, shape = RoundedCornerShape(14.dp)) {
        Column(Modifier.fillMaxWidth()) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(horizontal = 12.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Rounded.Psychology, null, tint = palette.textSub, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    if (streaming) "正在深入思考…" else "已深度思考",
                    style = MaterialTheme.typography.labelMedium,
                    color = palette.textSub,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    Icons.Rounded.ExpandMore,
                    null,
                    tint = palette.textSub,
                    modifier = Modifier.size(17.dp).rotate(rotate)
                )
            }
            if (expanded) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textSub,
                    modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp)
                )
            }
        }
    }
}

@Composable
private fun Composer(
    value: String,
    onValueChange: (String) -> Unit,
    streaming: Boolean,
    conversation: Conversation?,
    configured: Boolean,
    reasoning: Int,
    reasoningSupported: Boolean,
    onReasoningChange: (Int) -> Unit,
    webSearch: Boolean,
    onToggleWebSearch: () -> Unit,
    vision: Boolean,
    images: List<String>,
    attaching: Boolean,
    onPickImages: () -> Unit,
    onRemoveImage: (Int) -> Unit,
    onSend: () -> Unit,
    onStop: () -> Unit
) {
    val palette = pal
    val haptic = LocalHapticFeedback.current
    val canSend = (value.isNotBlank() || images.isNotEmpty()) && !streaming && !attaching
    var reasoningMenu by remember { mutableStateOf(false) }
    val levelLabel = when (reasoning) {
        1 -> "低"
        2 -> "中"
        3 -> "高"
        4 -> "极高"
        5 -> "最高"
        else -> "自动"
    }
    Column(
        Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Surface(
            color = palette.surface,
            shape = RoundedCornerShape(24.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, palette.border),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(start = 10.dp, end = 10.dp, top = 8.dp, bottom = 6.dp)) {
                if (images.isNotEmpty() || attaching) {
                    Row(
                        Modifier.horizontalScroll(rememberScrollState()).padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        images.forEachIndexed { index, image ->
                            Box(Modifier.size(64.dp)) {
                                DataImage(image, Modifier.fillMaxSize())
                                Box(
                                    Modifier.align(Alignment.TopEnd).padding(3.dp).size(22.dp)
                                        .clip(CircleShape).background(Color.Black.copy(alpha = 0.55f))
                                        .clickable { onRemoveImage(index) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Rounded.Close, "移除", tint = Color.White, modifier = Modifier.size(13.dp))
                                }
                            }
                        }
                        if (attaching) {
                            CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                        }
                    }
                }
                Row(verticalAlignment = Alignment.Bottom) {
                    Box(Modifier.weight(1f).padding(vertical = 6.dp)) {
                        if (value.isEmpty()) {
                            Text(
                                when {
                                    !configured -> "先去设置里填 API Key…"
                                    images.isNotEmpty() -> "想问这张图的什么？"
                                    else -> "给 AI 发消息…"
                                },
                                style = MaterialTheme.typography.bodyLarge,
                                color = palette.textSub.copy(alpha = 0.75f)
                            )
                        }
                        BasicTextField(
                            value = value,
                            onValueChange = onValueChange,
                            textStyle = MaterialTheme.typography.bodyLarge.copy(color = palette.text),
                            cursorBrush = SolidColor(palette.accent),
                            maxLines = 6,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 26.dp)
                        )
                    }
                    Spacer(Modifier.width(6.dp))
                    Box(
                        Modifier
                            .padding(bottom = 2.dp)
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    streaming -> palette.danger.copy(alpha = 0.12f)
                                    canSend -> palette.accent
                                    else -> palette.surfaceAlt
                                }
                            )
                            .clickable {
                                if (streaming) {
                                    onStop()
                                } else if (canSend) {
                                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                    onSend()
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (streaming) Icons.Rounded.Stop else Icons.AutoMirrored.Rounded.Send,
                            contentDescription = if (streaming) "停止" else "发送",
                            tint = when {
                                streaming -> palette.danger
                                canSend -> Color.White
                                else -> palette.textSub.copy(alpha = 0.6f)
                            },
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Box {
                        ToolChip(Icons.Rounded.Psychology, "思考 $levelLabel", reasoning > 0, dropdown = true) {
                            reasoningMenu = true
                        }
                        DropdownMenu(expanded = reasoningMenu, onDismissRequest = { reasoningMenu = false }) {
                            listOf("自动", "低", "中", "高", "极高 xhigh", "最高 max").forEachIndexed { index, label ->
                                DropdownMenuItem(
                                    text = { Text(label) },
                                    trailingIcon = {
                                        if (reasoning == index) {
                                            Icon(Icons.Rounded.Check, null, tint = palette.accent, modifier = Modifier.size(16.dp))
                                        }
                                    },
                                    onClick = {
                                        reasoningMenu = false
                                        onReasoningChange(index)
                                    }
                                )
                            }
                            if (!reasoningSupported) {
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            "当前模型不支持调节，会按默认方式思考",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = palette.textSub
                                        )
                                    },
                                    enabled = false,
                                    onClick = {}
                                )
                            }
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    ToolChip(Icons.Rounded.Public, if (webSearch) "联网中" else "联网", webSearch, onClick = onToggleWebSearch)
                    Spacer(Modifier.weight(1f))
                    if (vision) {
                        Box(
                            Modifier.size(40.dp).clip(RoundedCornerShape(12.dp))
                                .background(palette.surfaceAlt)
                                .border(1.dp, palette.border, RoundedCornerShape(12.dp))
                                .clickable(enabled = images.size < 4) { onPickImages() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Rounded.Image,
                                "添加图片",
                                tint = if (images.size < 4) palette.textSub else palette.textSub.copy(alpha = 0.4f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
                val hit = conversation?.hitTokens ?: 0
                val miss = conversation?.missTokens ?: 0
                val total = hit + miss
                if (configured && total > 0) {
                    Row(Modifier.padding(start = 6.dp, bottom = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(CacheStack, null, tint = palette.ok, modifier = Modifier.size(11.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "本次命中 ${hit * 100 / total}%，省 ${Tokens.format(hit)} token",
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.textSub
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            "AI 也会出错，重要内容请自己核对",
            style = MaterialTheme.typography.labelSmall,
            color = palette.textSub.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun ToolChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    active: Boolean,
    dropdown: Boolean = false,
    onClick: () -> Unit
) {
    val palette = pal
    Row(
        Modifier.height(36.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (active) palette.accentSoft else palette.surfaceAlt)
            .border(
                1.dp,
                if (active) palette.accent.copy(alpha = 0.35f) else palette.border,
                RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            null,
            tint = if (active) palette.accent else palette.textSub,
            modifier = Modifier.size(15.dp)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            label,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Medium,
            color = if (active) palette.accentText else palette.text,
            maxLines = 1
        )
        if (dropdown) {
            Spacer(Modifier.width(3.dp))
            Icon(
                Icons.Rounded.ExpandMore,
                null,
                tint = if (active) palette.accent else palette.textSub,
                modifier = Modifier.size(15.dp)
            )
        }
    }
}

private fun timeLabel(ts: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - ts
    return when {
        diff < 60_000 -> "刚刚"
        diff < 3_600_000 -> "${diff / 60_000} 分钟前"
        diff < 86_400_000 -> "${diff / 3_600_000} 小时前"
        diff < 172_800_000 -> "昨天"
        else -> SimpleDateFormat("M月d日", Locale.CHINA).format(Date(ts))
    }
}
