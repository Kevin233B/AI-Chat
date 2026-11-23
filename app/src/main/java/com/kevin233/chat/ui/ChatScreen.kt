package com.kevin233.chat.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.KeyboardArrowDown
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.AnnotatedString
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
import com.kevin233.chat.data.cacheFingerprint
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
        val wide = maxWidth >= 880.dp
        if (wide) {
            Row(Modifier.fillMaxSize().background(pal.bg)) {
                Sidebar(vm, Modifier.width(304.dp).fillMaxHeight(), true, onPicked = {})
                Box(Modifier.width(1.dp).fillMaxHeight().background(pal.border))
                ChatPane(vm, true, {}, Modifier.weight(1f))
            }
        } else {
            ModalNavigationDrawer(
                drawerState = drawerState,
                scrimColor = Color.Black.copy(alpha = 0.38f),
                drawerContent = {
                    ModalDrawerSheet(
                        drawerContainerColor = pal.bg,
                        drawerShape = RoundedCornerShape(topEnd = 22.dp, bottomEnd = 22.dp),
                        modifier = Modifier.width(312.dp)
                    ) {
                        Sidebar(vm, Modifier.fillMaxSize(), false) {
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
    val list = vm.sortedConversations
    val conv = vm.currentConversation

    Column(
        modifier
            .background(if (wide) palette.surface else palette.bg)
            .statusBarsPadding()
    ) {
        Row(
            Modifier.fillMaxWidth().padding(start = 18.dp, end = 12.dp, top = 16.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(7.dp).background(palette.accent, CircleShape))
            Spacer(Modifier.width(9.dp))
            Text(
                "AI Chat",
                style = MaterialTheme.typography.headlineSmall,
                color = palette.text,
                letterSpacing = (-0.2).sp,
                modifier = Modifier.weight(1f)
            )
            Box(
                Modifier
                    .background(palette.accentSoft, RoundedCornerShape(50))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text("本机", style = MaterialTheme.typography.labelSmall, color = palette.accentText)
            }
        }
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(palette.accent)
                .clickable {
                    vm.newChat()
                    onPicked()
                }
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Rounded.Add, null, tint = palette.onAccent, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                "新建对话",
                style = MaterialTheme.typography.titleSmall,
                color = palette.onAccent,
                modifier = Modifier.weight(1f)
            )
        }
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(palette.surfaceAlt)
                .clickable {
                    vm.screen = Screen.HISTORY
                    onPicked()
                }
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Rounded.Search, null, tint = palette.textSub, modifier = Modifier.size(17.dp))
            Spacer(Modifier.width(8.dp))
            Text("搜索聊天记录", style = MaterialTheme.typography.bodyMedium, color = palette.textSub)
        }
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 2.dp)
                .clip(RoundedCornerShape(12.dp))
                .clickable {
                    vm.screen = Screen.AGENTS
                    onPicked()
                }
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Rounded.Groups, null, tint = palette.accent, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(9.dp))
            Text(
                "智能体与技能",
                style = MaterialTheme.typography.bodyMedium,
                color = palette.text,
                modifier = Modifier.weight(1f)
            )
            Text(
                (vm.agents.size + vm.skills.size).toString(),
                style = MaterialTheme.typography.labelSmall,
                color = palette.textSub
            )
        }
        Row(
            Modifier.fillMaxWidth().padding(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SectionLabel("最近对话", Modifier.weight(1f))
            Text(list.size.toString(), style = MaterialTheme.typography.labelSmall, color = palette.textSub)
        }
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)) {
            items(list, key = { it.id }) { item ->
                val selected = item.id == conv?.id
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (selected) palette.accentSoft else Color.Transparent)
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
                        .padding(start = 8.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier
                            .size(30.dp)
                            .background(
                                if (selected) palette.accent else palette.surfaceAlt,
                                RoundedCornerShape(10.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            item.title.take(1).ifBlank { "新" },
                            style = MaterialTheme.typography.labelMedium,
                            color = if (selected) palette.onAccent else palette.textSub
                        )
                    }
                    Spacer(Modifier.width(9.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            item.title,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (selected) palette.accentText else palette.text,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (item.branchOrder > 0) {
                                Text(
                                    "分支 ${item.branchOrder}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = palette.accent
                                )
                                Spacer(Modifier.width(6.dp))
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
                        IconButton(onClick = { menu = true }, modifier = Modifier.size(28.dp)) {
                            Icon(
                                Icons.Rounded.MoreHoriz,
                                "更多",
                                tint = palette.textSub,
                                modifier = Modifier.size(16.dp)
                            )
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
            }
            item { Spacer(Modifier.height(6.dp)) }
        }
        Box(Modifier.fillMaxWidth().padding(horizontal = 14.dp).height(1.dp).background(palette.border))
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp)
                .clip(RoundedCornerShape(12.dp))
                .clickable {
                    vm.screen = Screen.SETTINGS
                    onPicked()
                }
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Rounded.Settings, null, tint = palette.textSub, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(9.dp))
            Text("设置", style = MaterialTheme.typography.bodyMedium, color = palette.text, modifier = Modifier.weight(1f))
            Text("1.0.0", style = MaterialTheme.typography.labelSmall, color = palette.textSub)
        }
        Text(
            "聊天记录只保存在这台手机",
            style = MaterialTheme.typography.labelSmall,
            color = palette.textSub.copy(alpha = 0.8f),
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp)
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
                    shape = RoundedCornerShape(14.dp)
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
            text = { Text("删掉以后就找不回来了。") },
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
private fun ChatPane(
    vm: ChatViewModel,
    wide: Boolean,
    onMenu: () -> Unit,
    modifier: Modifier = Modifier
) {
    val conv = vm.currentConversation
    val palette = pal
    val config = vm.activeConfig
    val conversationId = conv?.id.orEmpty()
    var input by rememberSaveable(conv?.id) { mutableStateOf(vm.draftFor(conversationId)) }
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
    val modelChanged = conv != null && conv.messages.isNotEmpty() && conv.modelKey.isNotBlank() &&
        config != null && conv.modelKey != config.cacheFingerprint()
    val showJump by remember { derivedStateOf { listState.firstVisibleItemIndex > 2 } }

    Column(modifier.fillMaxSize().background(palette.bg).statusBarsPadding()) {
        Row(
            Modifier.fillMaxWidth().height(66.dp).padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!wide) {
                CircleButton(Icons.Rounded.Menu, "菜单", onClick = onMenu)
                Spacer(Modifier.width(4.dp))
            } else {
                Spacer(Modifier.width(8.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(
                    conv?.title ?: "AI Chat",
                    style = MaterialTheme.typography.titleLarge,
                    color = palette.text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(3.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(5.dp).background(if (vm.isConfigured) palette.ok else palette.warn, CircleShape))
                    Spacer(Modifier.width(5.dp))
                    Text(
                        config?.let { "${it.name}  ${it.model}" } ?: "还没有配置模型",
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.textSub,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.clickable { vm.screen = Screen.SETTINGS }
                    )
                }
            }
            CircleButton(Icons.Rounded.Search, "搜索") { vm.screen = Screen.HISTORY }
            Spacer(Modifier.width(4.dp))
            CircleButton(Icons.Rounded.Add, "新对话") { vm.newChat() }
            Spacer(Modifier.width(4.dp))
            CircleButton(Icons.Rounded.Settings, "设置") { vm.screen = Screen.SETTINGS }
        }
        if (siblings.size > 1) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(palette.surfaceAlt)
                    .padding(horizontal = 6.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { vm.switchBranch(-1) }, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Rounded.ChevronLeft, "上一条分支", tint = palette.textSub, modifier = Modifier.size(17.dp))
                }
                Text(
                    "分支 ${branchIndex + 1} / ${siblings.size}",
                    style = MaterialTheme.typography.labelMedium,
                    color = palette.text
                )
                IconButton(onClick = { vm.switchBranch(1) }, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Rounded.ChevronRight, "下一条分支", tint = palette.textSub, modifier = Modifier.size(17.dp))
                }
                Spacer(Modifier.weight(1f))
                if ((conv?.branchCreatedAt ?: 0L) > 0L) {
                    Text(
                        Clock.full(conv?.branchCreatedAt ?: 0L),
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.textSub,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
            }
        }
        if (modelChanged) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(palette.surfaceAlt)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Rounded.Refresh, null, tint = palette.warn, modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(9.dp))
                Text(
                    "这个对话换过模型，缓存会重新计算",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.text,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    "新建对话",
                    style = MaterialTheme.typography.labelMedium,
                    color = palette.accent,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable { vm.newChat() }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
        if (!vm.isConfigured) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(palette.accentSoft)
                    .clickable { vm.screen = Screen.SETTINGS }
                    .padding(horizontal = 14.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier.size(30.dp).background(palette.surface, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Key, null, tint = palette.accent, modifier = Modifier.size(16.dp))
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("先连接一个模型", style = MaterialTheme.typography.titleSmall, color = palette.accentText)
                    Text(
                        "填入 API Key 就能开始聊天",
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.accentText.copy(alpha = 0.8f)
                    )
                }
                Text("去设置", style = MaterialTheme.typography.labelMedium, color = palette.accent)
            }
        }
        Box(Modifier.weight(1f), contentAlignment = Alignment.TopCenter) {
            if (conv == null || conv.messages.isEmpty()) {
                EmptyState(vm, Modifier.fillMaxSize())
            } else {
                LazyColumn(
                    state = listState,
                    reverseLayout = true,
                    modifier = if (wide) Modifier.fillMaxHeight().widthIn(max = 820.dp).fillMaxWidth() else Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = if (wide) 28.dp else 14.dp,
                        end = if (wide) 28.dp else 14.dp,
                        top = 12.dp,
                        bottom = 12.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    val messages = conv.messages
                    items(messages.asReversed(), key = { it.id }) { msg ->
                        MessageItem(
                            vm = vm,
                            msg = msg,
                            conv = conv,
                            streaming = vm.streaming && msg.id == messages.lastOrNull()?.id,
                            searching = vm.searching,
                            highlighted = msg.id == vm.highlightedMessageId,
                            onImageClick = { previewImage = it }
                        )
                    }
                }
            }
            if (showJump && conv?.messages?.isNotEmpty() == true) {
                IconButton(
                    onClick = { scope.launch { listState.animateScrollToItem(0) } },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(14.dp)
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(palette.surface)
                        .border(1.dp, palette.border, CircleShape)
                ) {
                    Icon(Icons.Rounded.KeyboardArrowDown, "回到最新", tint = palette.text, modifier = Modifier.size(21.dp))
                }
            }
        }
        Composer(
            value = input,
            onValueChange = {
                input = it
                vm.setDraft(conversationId, it)
            },
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
                    vm.setDraft(conversationId, "")
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
private fun CircleButton(icon: ImageVector, label: String, onClick: () -> Unit) {
    val palette = pal
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(palette.surfaceAlt)
    ) {
        Icon(icon, label, tint = palette.text, modifier = Modifier.size(19.dp))
    }
}

@Composable
private fun EmptyState(vm: ChatViewModel, modifier: Modifier = Modifier) {
    val palette = pal
    val conv = vm.currentConversation
    val agentName = conv?.agentName.orEmpty()
    val suggestions = listOf(
        "帮我写一段自我介绍",
        "用简单的话解释一个概念",
        "给我一个今天的学习计划",
        "把这句话翻译成英文"
    )
    Column(
        modifier.verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(if (agentName.isBlank()) 16.dp else 4.dp))
        if (agentName.isNotBlank()) {
            AgentAvatar(conv?.agentAvatar.orEmpty(), agentName, 64.dp)
            Spacer(Modifier.height(14.dp))
            Text(
                "和 $agentName 聊聊",
                style = MaterialTheme.typography.headlineMedium,
                color = palette.text,
                textAlign = TextAlign.Center
            )
        } else {
            Row(
                Modifier
                    .background(palette.accentSoft, RoundedCornerShape(50))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.size(6.dp).background(palette.accent, CircleShape))
                Spacer(Modifier.width(6.dp))
                Text("开始新对话", style = MaterialTheme.typography.labelMedium, color = palette.accentText)
            }
            Spacer(Modifier.height(12.dp))
            Text(
                "今天想聊点什么？",
                style = MaterialTheme.typography.displaySmall,
                color = palette.text,
                textAlign = TextAlign.Center
            )
        }
        Spacer(Modifier.height(24.dp))
        Column(Modifier.fillMaxWidth().widthIn(max = 560.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            suggestions.chunked(2).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    row.forEach { text ->
                        Column(
                            Modifier
                                .weight(1f)
                                .background(palette.surface, RoundedCornerShape(16.dp))
                                .border(1.dp, palette.border, RoundedCornerShape(16.dp))
                                .clickable { vm.send(text) }
                                .padding(14.dp)
                        ) {
                            Text(
                                text,
                                style = MaterialTheme.typography.bodyMedium,
                                color = palette.text,
                                minLines = 2
                            )
                            Spacer(Modifier.height(10.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("开始", style = MaterialTheme.typography.labelSmall, color = palette.accent)
                                Spacer(Modifier.weight(1f))
                                Text("→", style = MaterialTheme.typography.labelMedium, color = palette.accent)
                            }
                        }
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
        Spacer(Modifier.height(20.dp))
        val config = vm.activeConfig
        val profile = Presets.cacheProfile(config)
        Row(
            Modifier.fillMaxWidth().widthIn(max = 560.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatusPill(
                if (config == null) "未连接模型" else config.name,
                if (config == null) palette.warn else palette.textSub
            )
            StatusPill(
                if (profile.mode == com.kevin233.chat.data.CacheMode.NONE) "无缓存" else "缓存已开启",
                if (profile.mode == com.kevin233.chat.data.CacheMode.NONE) palette.textSub else palette.ok
            )
            Spacer(Modifier.weight(1f))
            if (agentName.isBlank()) {
                Text(
                    "智能体 →",
                    style = MaterialTheme.typography.labelMedium,
                    color = palette.accent,
                    modifier = Modifier.clickable { vm.screen = Screen.AGENTS }.padding(6.dp)
                )
            }
        }
        Spacer(Modifier.height(18.dp))
    }
}

@Composable
private fun StatusPill(text: String, color: Color) {
    val palette = pal
    Row(
        Modifier
            .background(palette.surface, RoundedCornerShape(50))
            .border(1.dp, palette.border, RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(5.dp).background(color, CircleShape))
        Spacer(Modifier.width(5.dp))
        Text(text, style = MaterialTheme.typography.labelSmall, color = palette.textSub, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MessageItem(
    vm: ChatViewModel,
    msg: Message,
    conv: Conversation,
    streaming: Boolean,
    searching: Boolean,
    highlighted: Boolean,
    onImageClick: (String) -> Unit
) {
    val palette = pal
    val clipboard = LocalClipboardManager.current
    var menu by remember { mutableStateOf(false) }
    if (msg.isUser) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 2.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (msg.usedSearch || msg.searchContext.isNotBlank()) {
                Box(
                    Modifier
                        .background(palette.accentSoft, RoundedCornerShape(50))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text("联网", style = MaterialTheme.typography.labelSmall, color = palette.accentText)
                }
                Spacer(Modifier.width(7.dp))
            }
            Text("你", style = MaterialTheme.typography.labelMedium, color = palette.accent)
            Spacer(Modifier.width(6.dp))
            Text(Clock.clock(msg.ts), style = MaterialTheme.typography.labelSmall, color = palette.textSub)
        }
        Spacer(Modifier.height(5.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            Box {
                Column(
                    Modifier
                        .widthIn(max = 620.dp)
                        .clip(RoundedCornerShape(18.dp, 18.dp, 5.dp, 18.dp))
                        .background(palette.userBubble)
                        .combinedClickable(onClick = { menu = true }, onLongClick = { menu = true })
                        .padding(horizontal = 14.dp, vertical = 11.dp)
                ) {
                    if (msg.images.isNotEmpty()) {
                        Row(
                            Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            msg.images.forEach { image ->
                                DataImage(
                                    dataUrl = image,
                                    modifier = Modifier.size(118.dp).clip(RoundedCornerShape(12.dp)),
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
                            color = palette.userBubbleText
                        )
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

    Row(Modifier.fillMaxWidth()) {
        AssistantAvatar(conv)
        Spacer(Modifier.width(9.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    conv.agentName.ifBlank { "AI 助手" },
                    style = MaterialTheme.typography.labelMedium,
                    color = palette.text
                )
                Spacer(Modifier.width(7.dp))
                Text(Clock.clock(msg.ts), style = MaterialTheme.typography.labelSmall, color = palette.textSub)
                if (msg.durationMs > 0) {
                    Spacer(Modifier.width(6.dp))
                    Text(Clock.duration(msg.durationMs), style = MaterialTheme.typography.labelSmall, color = palette.textSub)
                }
                Spacer(Modifier.weight(1f))
                Box {
                    IconButton(onClick = { menu = true }, modifier = Modifier.size(30.dp)) {
                        Icon(Icons.Rounded.MoreHoriz, null, tint = palette.textSub, modifier = Modifier.size(17.dp))
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
            }
            Spacer(Modifier.height(6.dp))
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(
                        if (highlighted) palette.accentSoft else palette.surface,
                        RoundedCornerShape(4.dp, 18.dp, 18.dp, 18.dp)
                    )
                    .border(
                        1.dp,
                        if (highlighted) palette.accent.copy(alpha = 0.45f) else palette.border,
                        RoundedCornerShape(4.dp, 18.dp, 18.dp, 18.dp)
                    )
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                if (msg.reasoning.isNotBlank()) {
                    ReasoningPanel(msg.reasoning, streaming)
                    Spacer(Modifier.height(10.dp))
                }
                when {
                    msg.content.isBlank() && streaming -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            DotsLoading()
                            Spacer(Modifier.width(9.dp))
                            Text(
                                if (searching) "正在联网搜索" else "正在思考",
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.textSub
                            )
                        }
                    }
                    msg.error.isNotBlank() -> {
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .background(palette.danger.copy(alpha = 0.07f), RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Text("没有成功", style = MaterialTheme.typography.titleSmall, color = palette.danger)
                            Spacer(Modifier.height(4.dp))
                            Text(msg.error, style = MaterialTheme.typography.bodySmall, color = palette.textSub)
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "再试一次",
                                style = MaterialTheme.typography.labelMedium,
                                color = palette.danger,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .clickable { vm.regenerate() }
                                    .background(palette.danger.copy(alpha = 0.1f))
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                    else -> {
                        SelectionContainer {
                            MarkdownText(msg.content, onImageClick = onImageClick)
                        }
                    }
                }
            }
            if (!streaming && (msg.cacheHit + msg.cacheMiss) > 0) {
                Spacer(Modifier.height(7.dp))
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

@Composable
private fun AssistantAvatar(conv: Conversation) {
    val palette = pal
    if (conv.agentName.isNotBlank()) {
        AgentAvatar(conv.agentAvatar, conv.agentName, 30.dp)
    } else {
        Box(
            Modifier.size(30.dp).background(palette.accentSoft, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text("AI", style = MaterialTheme.typography.labelSmall, color = palette.accent)
        }
    }
}

@Composable
private fun TinyAction(icon: ImageVector, label: String, onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(30.dp)) {
        Icon(icon, label, tint = pal.textSub, modifier = Modifier.size(15.dp))
    }
}

@Composable
private fun ReasoningPanel(text: String, streaming: Boolean) {
    val palette = pal
    var expanded by rememberSaveable { mutableStateOf(streaming) }
    Column(
        Modifier
            .fillMaxWidth()
            .background(palette.surfaceAlt, RoundedCornerShape(12.dp))
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Rounded.Psychology, null, tint = palette.textSub, modifier = Modifier.size(15.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                if (streaming) "正在思考" else "思考过程",
                style = MaterialTheme.typography.labelMedium,
                color = palette.textSub,
                modifier = Modifier.weight(1f)
            )
            Icon(
                if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                null,
                tint = palette.textSub,
                modifier = Modifier.size(16.dp)
            )
        }
        if (expanded) {
            Box(Modifier.fillMaxWidth().height(1.dp).background(palette.border.copy(alpha = 0.7f)))
            Text(
                text,
                style = MaterialTheme.typography.bodySmall,
                color = palette.textSub,
                modifier = Modifier.padding(11.dp)
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
    val reasoningText = when (reasoning) {
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
        Column(
            Modifier
                .fillMaxWidth()
                .background(palette.surface, RoundedCornerShape(22.dp))
                .border(1.dp, palette.border, RoundedCornerShape(22.dp))
                .padding(10.dp)
        ) {
            if (images.isNotEmpty() || attaching) {
                Row(
                    Modifier.horizontalScroll(rememberScrollState()).padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    images.forEachIndexed { index, image ->
                        Box(Modifier.size(62.dp).clip(RoundedCornerShape(12.dp))) {
                            DataImage(image, Modifier.fillMaxSize())
                            Box(
                                Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(3.dp)
                                    .size(19.dp)
                                    .background(Color.Black.copy(alpha = 0.65f), CircleShape)
                                    .clickable { onRemoveImage(index) },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Rounded.Close, "移除", tint = Color.White, modifier = Modifier.size(12.dp))
                            }
                        }
                    }
                    if (attaching) DotsLoading()
                }
            }
            Box(Modifier.fillMaxWidth().padding(horizontal = 5.dp, vertical = 5.dp)) {
                if (value.isEmpty()) {
                    Text(
                        if (configured) "发消息给 AI…" else "先在设置里填入 API Key",
                        style = MaterialTheme.typography.bodyLarge,
                        color = palette.textSub.copy(alpha = 0.75f)
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
            Spacer(Modifier.height(7.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box {
                    ToolPill(Icons.Rounded.Psychology, "思考 $reasoningText", reasoning > 0) { reasoningMenu = true }
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
                ToolPill(Icons.Rounded.Public, if (webSearch) "联网已开" else "联网", webSearch) { onToggleWebSearch() }
                Spacer(Modifier.weight(1f))
                if (vision) {
                    Box(
                        Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(palette.surfaceAlt)
                            .clickable(enabled = images.size < 4) { onPickImages() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.Image, "图片", tint = palette.textSub, modifier = Modifier.size(18.dp))
                    }
                    Spacer(Modifier.width(6.dp))
                }
                Box(
                    Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                streaming -> palette.accentSoft
                                canSend -> palette.accent
                                else -> palette.surfaceAlt
                            }
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
                            canSend -> palette.onAccent
                            else -> palette.textSub.copy(alpha = 0.5f)
                        },
                        modifier = Modifier.size(19.dp)
                    )
                }
            }
        }
        Spacer(Modifier.height(5.dp))
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "只保存在本机",
                style = MaterialTheme.typography.labelSmall,
                color = palette.textSub.copy(alpha = 0.75f)
            )
            Spacer(Modifier.weight(1f))
            Text(
                "AI 也会出错，请核对",
                style = MaterialTheme.typography.labelSmall,
                color = palette.textSub.copy(alpha = 0.75f)
            )
        }
    }
}

@Composable
private fun ToolPill(
    icon: ImageVector,
    label: String,
    active: Boolean,
    onClick: () -> Unit
) {
    val palette = pal
    Row(
        Modifier
            .height(34.dp)
            .clip(RoundedCornerShape(50))
            .background(if (active) palette.accentSoft else palette.surfaceAlt)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            null,
            tint = if (active) palette.accent else palette.textSub,
            modifier = Modifier.size(15.dp)
        )
        Spacer(Modifier.width(5.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = if (active) palette.accentText else palette.text
        )
    }
}
