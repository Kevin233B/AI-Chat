package com.kevin233.chat.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
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
import com.kevin233.chat.util.Images
import com.kevin233.chat.util.Tokens
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ChatScreen(vm: ChatViewModel) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val wide = maxWidth >= 860.dp
        if (wide) {
            Row(Modifier.fillMaxSize().background(pal.bg)) {
                IndexRail(vm, Modifier.width(288.dp).fillMaxHeight(), true, onPicked = {})
                Box(Modifier.width(1.dp).fillMaxHeight().background(pal.border))
                ChatPane(vm, true, {}, Modifier.weight(1f))
            }
        } else {
            ModalNavigationDrawer(
                drawerState = drawerState,
                scrimColor = Color.Black.copy(alpha = 0.4f),
                drawerContent = {
                    ModalDrawerSheet(
                        drawerContainerColor = pal.bg,
                        drawerShape = RoundedCornerShape(topEnd = 4.dp, bottomEnd = 4.dp),
                        modifier = Modifier.width(300.dp)
                    ) {
                        IndexRail(vm, Modifier.fillMaxSize(), false) {
                            scope.launch { drawerState.close() }
                        }
                    }
                }
            ) {
                ChatPane(vm, false, { scope.launch { drawerState.open() } })
            }
        }
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun IndexRail(
    vm: ChatViewModel,
    modifier: Modifier,
    wide: Boolean,
    onPicked: () -> Unit
) {
    val palette = pal
    var renameTarget by remember { mutableStateOf<Conversation?>(null) }
    var deleteTarget by remember { mutableStateOf<Conversation?>(null) }
    var editingTitle by remember { mutableStateOf("") }
    val list = vm.sortedConversations
    val conv = vm.currentConversation

    Column(modifier.background(if (wide) palette.surfaceAlt else palette.bg).statusBarsPadding()) {
        Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 16.dp)) {
            Text(
                "AI CHAT",
                style = MaterialTheme.typography.titleLarge,
                fontSize = 21.sp,
                color = palette.text,
                letterSpacing = 1.5.sp
            )
            Spacer(Modifier.height(5.dp))
            Kicker("NOTEBOOK / 对话索引")
        }
        Hairline()
        RailAction("新建对话", "NEW", Icons.Rounded.Add, accent = true) {
            vm.newChat()
            onPicked()
        }
        Hairline()
        RailAction("智能体与技能", "${vm.agents.size + vm.skills.size}", Icons.Rounded.Groups) {
            vm.screen = Screen.AGENTS
            onPicked()
        }
        RailAction("搜索聊天记录", "FIND", Icons.Rounded.Search) {
            vm.screen = Screen.HISTORY
            onPicked()
        }
        Hairline()
        CacheLedger(vm, Modifier.padding(horizontal = 20.dp, vertical = 12.dp))
        Hairline()
        Row(
            Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Kicker("RECENT / 最近", palette.textSub, Modifier.weight(1f))
            Text(
                list.size.toString().padStart(2, '0'),
                style = MaterialTheme.typography.labelSmall,
                color = palette.textSub
            )
        }
        LazyColumn(Modifier.weight(1f)) {
            items(list, key = { it.id }) { item ->
                val selected = item.id == conv?.id
                Row(
                    Modifier
                        .fillMaxWidth()
                        .combinedClickable(
                            onClick = {
                                vm.selectChat(item.id)
                                onPicked()
                            },
                            onLongClick = {
                                renameTarget = item
                                editingTitle = item.title
                            }
                        )
                        .padding(start = 16.dp, end = 12.dp, top = 10.dp, bottom = 10.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        Modifier
                            .padding(top = 4.dp)
                            .width(2.dp)
                            .height(26.dp)
                            .background(if (selected) palette.accent else Color.Transparent)
                    )
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            item.title,
                            style = MaterialTheme.typography.bodyMedium,
                            color = palette.text,
                            fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(3.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (item.branchOrder > 0) {
                                Text(
                                    "BRANCH ${item.branchOrder.toString().padStart(2, '0')}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = palette.accent
                                )
                                Spacer(Modifier.width(7.dp))
                            }
                            Text(
                                Clock.ago(item.updatedAt),
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.textSub
                            )
                        }
                    }
                    Box {
                        var menu by remember { mutableStateOf(false) }
                        IconButton(onClick = { menu = true }, modifier = Modifier.size(26.dp)) {
                            Icon(Icons.Rounded.MoreHoriz, "更多", tint = palette.textSub, modifier = Modifier.size(15.dp))
                        }
                        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                            DropdownMenuItem(
                                text = { Text("重命名") },
                                onClick = {
                                    menu = false
                                    renameTarget = item
                                    editingTitle = item.title
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("删除") },
                                onClick = {
                                    menu = false
                                    deleteTarget = item
                                }
                            )
                        }
                    }
                }
                Hairline(Modifier.padding(start = 28.dp))
            }
            item { Spacer(Modifier.height(8.dp)) }
        }
        Hairline()
        RailAction("设置", "CONF", Icons.Rounded.Settings) {
            vm.screen = Screen.SETTINGS
            onPicked()
        }
        Text(
            "LOCAL ONLY / 只存本机",
            style = MaterialTheme.typography.labelSmall,
            color = palette.textSub.copy(alpha = 0.7f),
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
        )
    }

    if (renameTarget != null) {
        AlertDialog(
            onDismissRequest = { renameTarget = null },
            title = { Text("重命名对话") },
            text = {
                androidx.compose.material3.OutlinedTextField(
                    value = editingTitle,
                    onValueChange = { editingTitle = it },
                    singleLine = true,
                    shape = RoundedCornerShape(3.dp)
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
            text = { Text("删掉就找不回来了。") },
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
private fun RailAction(
    title: String,
    meta: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accent: Boolean = false,
    onClick: () -> Unit
) {
    val palette = pal
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (accent) {
            Box(
                Modifier.size(22.dp).background(palette.accent),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = Color.White, modifier = Modifier.size(14.dp))
            }
        } else {
            Icon(icon, null, tint = palette.textSub, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(12.dp))
        Text(
            title,
            style = MaterialTheme.typography.bodyMedium,
            color = if (accent) palette.accent else palette.text,
            modifier = Modifier.weight(1f)
        )
        Text(meta, style = MaterialTheme.typography.labelSmall, color = palette.textSub.copy(alpha = 0.7f))
    }
}

@Composable
private fun CacheLedger(vm: ChatViewModel, modifier: Modifier = Modifier) {
    val palette = pal
    val conv = vm.currentConversation
    val profile = Presets.cacheProfile(vm.activeConfig)
    val total = (conv?.hitTokens ?: 0) + (conv?.missTokens ?: 0)
    val rate = if (total > 0) ((conv?.hitTokens ?: 0).toFloat() / total) else 0f
    val prefix = conv?.let { Tokens.estimate(it.systemPrompt) + Tokens.estimateMessages(it.messages) } ?: 0
    val ready = profile.minPrefix == 0 || prefix >= profile.minPrefix
    Column(modifier.fillMaxWidth()) {
        Text("CACHE / 缓存", style = MaterialTheme.typography.labelSmall, color = palette.textSub)
        Spacer(Modifier.height(4.dp))
        Text(
            if (total > 0) "${(rate * 100).toInt()}%" else if (ready) "READY" else "WARMING",
            style = MaterialTheme.typography.titleLarge,
            fontSize = 19.sp,
            color = if (total > 0 && rate > 0f) palette.ok else palette.text
        )
        Spacer(Modifier.height(3.dp))
        Text(
            if (total > 0) "READ ${Tokens.format(conv?.hitTokens ?: 0)} TOKENS" else if (profile.minPrefix > 0) "${profile.minPrefix - prefix} TOKENS TO GO" else "NO CACHE FOR THIS MODEL",
            style = MaterialTheme.typography.labelSmall,
            color = palette.textSub
        )
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
                    uris.take(room).mapNotNull { Images.toDataUrl(context, it) }
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
            if (index >= 0) listState.scrollToItem((conv.messages.size - 1 - index).coerceAtLeast(0))
            vm.clearJump()
        }
    }
    LaunchedEffect(vm.highlightedMessageId) {
        if (vm.highlightedMessageId != null) {
            kotlinx.coroutines.delay(2600)
            vm.clearHighlight()
        }
    }
    val siblings = vm.siblingsFor(conv)
    val branchIndex = siblings.indexOfFirst { it.id == conv?.id }.coerceAtLeast(0)

    Column(
        modifier.fillMaxSize().background(palette.bg).statusBarsPadding()
    ) {
        Row(
            Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!wide) {
                IconButton(onClick = onMenu) {
                    Icon(Icons.Rounded.Menu, "菜单", tint = palette.text, modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.width(4.dp))
            } else {
                Spacer(Modifier.width(10.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(
                    conv?.title ?: "AI Chat",
                    style = MaterialTheme.typography.titleLarge,
                    color = palette.text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    config?.let { "${it.name} / ${it.model}" } ?: "NO MODEL / 未配置",
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.textSub,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            HeaderAction(Icons.Rounded.Search, "搜索") { vm.screen = Screen.HISTORY }
            HeaderAction(Icons.Rounded.Add, "新对话") { vm.newChat() }
            HeaderAction(Icons.Rounded.Settings, "设置") { vm.screen = Screen.SETTINGS }
        }
        Hairline()
        if (siblings.size > 1) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { vm.switchBranch(-1) }, modifier = Modifier.size(26.dp)) {
                    Icon(Icons.Rounded.ChevronLeft, "上一条分支", tint = palette.textSub, modifier = Modifier.size(16.dp))
                }
                Text(
                    "BRANCH ${(branchIndex + 1).toString().padStart(2, '0')} / ${siblings.size.toString().padStart(2, '0')}",
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.accent
                )
                IconButton(onClick = { vm.switchBranch(1) }, modifier = Modifier.size(26.dp)) {
                    Icon(Icons.Rounded.ChevronRight, "下一条分支", tint = palette.textSub, modifier = Modifier.size(16.dp))
                }
                Spacer(Modifier.weight(1f))
                if ((conv?.branchCreatedAt ?: 0L) > 0L) {
                    Text(
                        Clock.full(conv?.branchCreatedAt ?: 0L),
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.textSub
                    )
                }
            }
            Hairline()
        }
        if (!vm.isConfigured) {
            Row(
                Modifier.fillMaxWidth().background(palette.accentSoft).clickable { vm.screen = Screen.SETTINGS }
                    .padding(horizontal = 18.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.size(4.dp).background(palette.accent))
                Spacer(Modifier.width(8.dp))
                Kicker("SETUP / 还差一个 API Key", palette.accentText, Modifier.weight(1f))
                Text("去设置 →", style = MaterialTheme.typography.labelSmall, color = palette.accent)
            }
            Hairline()
        }
        Box(Modifier.weight(1f), contentAlignment = Alignment.TopCenter) {
            if (conv == null || conv.messages.isEmpty()) {
                EmptyState(vm, Modifier.fillMaxSize())
            } else {
                LazyColumn(
                    state = listState,
                    reverseLayout = true,
                    modifier = if (wide) Modifier.fillMaxHeight().widthIn(max = 820.dp).fillMaxWidth() else Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        start = if (wide) 32.dp else 18.dp,
                        end = if (wide) 32.dp else 18.dp,
                        top = 14.dp,
                        bottom = 10.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    val messages = conv.messages
                    items(messages.asReversed(), key = { it.id }) { msg ->
                        MessageItem(
                            vm = vm,
                            msg = msg,
                            conv = conv,
                            streaming = vm.streaming && msg.id == messages.lastOrNull()?.id,
                            searching = vm.searching,
                            onImageClick = { previewImage = it }
                        )
                    }
                }
            }
        }
        Composer(
            value = input,
            onValueChange = { input = it },
            streaming = vm.streaming,
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
private fun HeaderAction(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(40.dp)) {
        Icon(icon, label, tint = pal.textSub, modifier = Modifier.size(19.dp))
    }
}

@Composable
private fun EmptyState(vm: ChatViewModel, modifier: Modifier = Modifier) {
    val palette = pal
    val conv = vm.currentConversation
    val agentName = conv?.agentName.orEmpty()
    val suggestions = listOf(
        "写一段自我介绍，语气自然一点",
        "用大白话解释一下什么是缓存",
        "帮我列一个今天的学习计划",
        "把这段话翻译成英文：今天天气很好"
    )
    Column(
        modifier.verticalScroll(rememberScrollState()).padding(horizontal = 22.dp, vertical = 30.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(if (agentName.isBlank()) 26.dp else 10.dp))
        if (agentName.isNotBlank()) {
            AgentAvatar(conv?.agentAvatar.orEmpty(), agentName, 64.dp)
            Spacer(Modifier.height(16.dp))
        }
        Kicker("START / 新对话", palette.accent)
        Spacer(Modifier.height(8.dp))
        Text(
            if (agentName.isNotBlank()) "我是$agentName" else "今天想聊什么？",
            style = MaterialTheme.typography.displaySmall,
            color = palette.text,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(12.dp))
        Box(Modifier.width(44.dp).height(2.dp).background(palette.accent))
        Spacer(Modifier.height(26.dp))
        Column(Modifier.fillMaxWidth().widthIn(max = 560.dp)) {
            suggestions.forEachIndexed { index, text ->
                Row(
                    Modifier.fillMaxWidth().clickable { vm.send(text) }.padding(vertical = 13.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NumberLabel(index + 1)
                    Text(
                        text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.text,
                        modifier = Modifier.weight(1f)
                    )
                    Text("↗", style = MaterialTheme.typography.labelMedium, color = palette.textSub)
                }
                Hairline(Modifier.padding(start = 26.dp))
            }
        }
        Spacer(Modifier.height(24.dp))
        val config = vm.activeConfig
        val profile = Presets.cacheProfile(config)
        Row(
            Modifier.fillMaxWidth().widthIn(max = 560.dp).padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "CACHE / ${profile.label}",
                style = MaterialTheme.typography.labelSmall,
                color = palette.textSub,
                modifier = Modifier.weight(1f)
            )
            Text(
                if (config == null) "NO KEY" else "READY",
                style = MaterialTheme.typography.labelSmall,
                color = if (config == null) palette.warn else palette.ok
            )
        }
        Hairline(Modifier.widthIn(max = 560.dp))
        if (agentName.isBlank()) {
            TextButton(onClick = { vm.screen = Screen.AGENTS }) {
                Text("智能体与技能 →", color = palette.accent, style = MaterialTheme.typography.labelMedium)
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun MessageItem(
    vm: ChatViewModel,
    msg: Message,
    conv: Conversation,
    streaming: Boolean,
    searching: Boolean,
    onImageClick: (String) -> Unit
) {
    val palette = pal
    val clipboard = LocalClipboardManager.current
    var menu by remember { mutableStateOf(false) }
    if (msg.isUser) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.End) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (msg.usedSearch || msg.searchContext.isNotBlank()) {
                    Tag("WEB")
                    Spacer(Modifier.width(6.dp))
                }
                Text(Clock.clock(msg.ts), style = MaterialTheme.typography.labelSmall, color = palette.textSub)
            }
            Spacer(Modifier.height(6.dp))
            Box {
                Row(
                    Modifier
                        .widthIn(max = 620.dp)
                        .height(IntrinsicSize.Min)
                        .background(palette.surface, RoundedCornerShape(3.dp))
                        .border(1.dp, palette.border, RoundedCornerShape(3.dp))
                        .combinedClickable(onClick = { menu = true }, onLongClick = { menu = true })
                ) {
                    Box(Modifier.width(2.dp).fillMaxHeight().background(palette.accent))
                    Column(Modifier.padding(horizontal = 14.dp, vertical = 11.dp)) {
                        if (msg.images.isNotEmpty()) {
                            Row(
                                Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                msg.images.forEach { image ->
                                    DataImage(
                                        dataUrl = image,
                                        modifier = Modifier.size(116.dp),
                                        onClick = { onImageClick(image) }
                                    )
                                }
                            }
                            if (msg.content.isNotBlank()) Spacer(Modifier.height(8.dp))
                        }
                        if (msg.content.isNotBlank()) {
                            Text(
                                msg.content,
                                style = MaterialTheme.typography.bodyLarge,
                                color = palette.text
                            )
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
                        text = { Text("从这里新建分支") },
                        leadingIcon = { Icon(Icons.Rounded.Edit, null, Modifier.size(18.dp)) },
                        onClick = {
                            menu = false
                            vm.branchFrom(msg.id)
                        }
                    )
                }
            }
        }
        return
    }

    Column(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (conv.agentName.isNotBlank()) {
                AgentAvatar(conv.agentAvatar, conv.agentName, 20.dp)
                Spacer(Modifier.width(7.dp))
                Kicker(conv.agentName, palette.text)
            } else {
                Box(Modifier.size(4.dp).background(palette.accent))
                Spacer(Modifier.width(7.dp))
                Kicker("AI", palette.text)
            }
            Spacer(Modifier.width(9.dp))
            Text(Clock.clock(msg.ts), style = MaterialTheme.typography.labelSmall, color = palette.textSub)
            if (msg.durationMs > 0) {
                Spacer(Modifier.width(7.dp))
                Text(Clock.duration(msg.durationMs), style = MaterialTheme.typography.labelSmall, color = palette.textSub)
            }
            Spacer(Modifier.weight(1f))
            IconButton(onClick = { menu = true }, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Rounded.MoreHoriz, null, tint = palette.textSub, modifier = Modifier.size(16.dp))
            }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(
                    text = { Text("复制回复") },
                    onClick = {
                        clipboard.setText(AnnotatedString(msg.content))
                        menu = false
                        vm.showToast("已复制")
                    }
                )
                if (!streaming) {
                    DropdownMenuItem(
                        text = { Text("重新回答") },
                        onClick = {
                            menu = false
                            vm.regenerate()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("从这里新建分支") },
                        onClick = {
                            menu = false
                            vm.branchFrom(msg.id)
                        }
                    )
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            Box(
                Modifier
                    .width(2.dp)
                    .fillMaxHeight()
                    .background(if (streaming) palette.accent else palette.border)
            )
            Spacer(Modifier.width(13.dp))
            Column(Modifier.weight(1f)) {
                if (msg.reasoning.isNotBlank()) {
                    ReasoningPanel(msg.reasoning, streaming)
                    Spacer(Modifier.height(10.dp))
                }
                when {
                    msg.content.isBlank() && streaming -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            DotsLoading()
                            Spacer(Modifier.width(9.dp))
                            Kicker(if (searching) "SEARCHING / 正在联网" else "THINKING / 正在思考")
                        }
                    }
                    msg.error.isNotBlank() -> {
                        Column(
                            Modifier.fillMaxWidth().background(palette.danger.copy(alpha = 0.06f))
                                .border(1.dp, palette.danger.copy(alpha = 0.3f), RoundedCornerShape(3.dp))
                                .padding(14.dp)
                        ) {
                            Kicker("ERROR", palette.danger)
                            Spacer(Modifier.height(6.dp))
                            Text(msg.error, style = MaterialTheme.typography.bodySmall, color = palette.textSub)
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "重试 →",
                                style = MaterialTheme.typography.labelMedium,
                                color = palette.danger,
                                modifier = Modifier.clickable { vm.regenerate() }
                            )
                        }
                    }
                    else -> {
                        SelectionContainer {
                            MarkdownText(msg.content, onImageClick = onImageClick)
                        }
                    }
                }
                if (!streaming && (msg.cacheHit + msg.cacheMiss) > 0) {
                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CacheBadge(msg.cacheHit, msg.cacheMiss)
                        Spacer(Modifier.weight(1f))
                        TinyAction(Icons.Rounded.ContentCopy, "复制") {
                            clipboard.setText(AnnotatedString(msg.content))
                            vm.showToast("已复制")
                        }
                        TinyAction(Icons.Rounded.Refresh, "重新回答") { vm.regenerate() }
                    }
                }
            }
        }
    }
}

@Composable
private fun TinyAction(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(30.dp)) {
        Icon(icon, label, tint = pal.textSub, modifier = Modifier.size(14.dp))
    }
}

@Composable
private fun Tag(text: String) {
    val palette = pal
    Row(
        Modifier.background(palette.accentSoft, RoundedCornerShape(2.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text, style = MaterialTheme.typography.labelSmall, color = palette.accentText)
    }
}

@Composable
private fun ReasoningPanel(text: String, streaming: Boolean) {
    val palette = pal
    var expanded by rememberSaveable { mutableStateOf(streaming) }
    Column(
        Modifier.fillMaxWidth().background(palette.surfaceAlt, RoundedCornerShape(3.dp))
            .border(1.dp, palette.border, RoundedCornerShape(3.dp))
    ) {
        Row(
            Modifier.fillMaxWidth().clickable { expanded = !expanded }.padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Rounded.Psychology, null, tint = palette.textSub, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(8.dp))
            Kicker(if (streaming) "THINKING / 正在深入" else "THINKING / 已深度思考", palette.textSub, Modifier.weight(1f))
            Icon(
                if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                null,
                tint = palette.textSub,
                modifier = Modifier.size(15.dp)
            )
        }
        if (expanded) {
            Hairline()
            Text(
                text,
                style = MaterialTheme.typography.bodySmall,
                color = palette.textSub,
                modifier = Modifier.padding(12.dp)
            )
        }
    }
}

@Composable
private fun Composer(
    value: String,
    onValueChange: (String) -> Unit,
    streaming: Boolean,
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
    val level = when (reasoning) {
        1 -> "LOW"
        2 -> "MID"
        3 -> "HIGH"
        4 -> "XHIGH"
        5 -> "MAX"
        else -> "AUTO"
    }
    Column(
        Modifier.fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime))
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Column(
            Modifier.fillMaxWidth()
                .background(palette.surface, RoundedCornerShape(4.dp))
                .border(1.dp, palette.border, RoundedCornerShape(4.dp))
                .padding(10.dp)
        ) {
            if (images.isNotEmpty() || attaching) {
                Row(
                    Modifier.horizontalScroll(rememberScrollState()).padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    images.forEachIndexed { index, image ->
                        Box(Modifier.size(58.dp)) {
                            DataImage(image, Modifier.fillMaxSize())
                            Box(
                                Modifier.align(Alignment.TopEnd).padding(2.dp).size(18.dp)
                                    .background(palette.text, RoundedCornerShape(2.dp))
                                    .clickable { onRemoveImage(index) },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Rounded.Close, "移除", tint = palette.bg, modifier = Modifier.size(11.dp))
                            }
                        }
                    }
                    if (attaching) DotsLoading()
                }
            }
            Box(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 6.dp)) {
                if (value.isEmpty()) {
                    Text(
                        if (configured) "写下你的问题…" else "先在设置里填入 API Key",
                        style = MaterialTheme.typography.bodyLarge,
                        color = palette.textSub.copy(alpha = 0.65f)
                    )
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = palette.text),
                    cursorBrush = SolidColor(palette.accent),
                    maxLines = 7,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 24.dp)
                )
            }
            Spacer(Modifier.height(6.dp))
            Hairline()
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box {
                    ToolTag(Icons.Rounded.Psychology, "思考 $level", reasoning > 0) { reasoningMenu = true }
                    DropdownMenu(expanded = reasoningMenu, onDismissRequest = { reasoningMenu = false }) {
                        listOf("自动" to "AUTO", "低" to "LOW", "中" to "MID", "高" to "HIGH", "极高" to "XHIGH", "最高" to "MAX")
                            .forEachIndexed { index, pair ->
                                DropdownMenuItem(
                                    text = { Text(pair.first, style = MaterialTheme.typography.bodyMedium) },
                                    trailingIcon = {
                                        Text(pair.second, style = MaterialTheme.typography.labelSmall, color = palette.textSub)
                                    },
                                    onClick = {
                                        reasoningMenu = false
                                        onReasoningChange(index)
                                    }
                                )
                            }
                        if (!reasoningSupported) {
                            DropdownMenuItem(
                                text = { Text("当前模型不支持调节", style = MaterialTheme.typography.labelSmall, color = palette.textSub) },
                                enabled = false,
                                onClick = {}
                            )
                        }
                    }
                }
                Spacer(Modifier.width(6.dp))
                ToolTag(Icons.Rounded.Public, if (webSearch) "联网 ON" else "联网", webSearch) { onToggleWebSearch() }
                Spacer(Modifier.weight(1f))
                if (vision) {
                    Box(
                        Modifier.size(34.dp).border(1.dp, palette.border, RoundedCornerShape(3.dp))
                            .clickable(enabled = images.size < 4) { onPickImages() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.Image, "图片", tint = palette.textSub, modifier = Modifier.size(17.dp))
                    }
                    Spacer(Modifier.width(6.dp))
                }
                Box(
                    Modifier.size(38.dp)
                        .background(
                            when {
                                streaming -> palette.accentSoft
                                canSend -> palette.accent
                                else -> palette.surfaceAlt
                            },
                            RoundedCornerShape(3.dp)
                        )
                        .clickable {
                            if (streaming) onStop()
                            else if (canSend) {
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                onSend()
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (streaming) Icons.Rounded.Stop else Icons.AutoMirrored.Rounded.Send,
                        if (streaming) "停止" else "发送",
                        tint = when {
                            streaming -> palette.accent
                            canSend -> Color.White
                            else -> palette.textSub.copy(alpha = 0.5f)
                        },
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
        Spacer(Modifier.height(7.dp))
        Row(Modifier.fillMaxWidth().padding(horizontal = 2.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "LOCAL / 只存本机",
                style = MaterialTheme.typography.labelSmall,
                color = palette.textSub.copy(alpha = 0.6f)
            )
            Spacer(Modifier.weight(1f))
            Text(
                "AI 也会出错，请核对",
                style = MaterialTheme.typography.labelSmall,
                color = palette.textSub.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
private fun ToolTag(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    active: Boolean,
    onClick: () -> Unit
) {
    val palette = pal
    Row(
        Modifier.height(32.dp)
            .background(if (active) palette.accentSoft else Color.Transparent, RoundedCornerShape(3.dp))
            .border(
                1.dp,
                if (active) palette.accent.copy(alpha = 0.4f) else palette.border,
                RoundedCornerShape(3.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = if (active) palette.accent else palette.textSub, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(6.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = if (active) palette.accentText else palette.text
        )
    }
}
