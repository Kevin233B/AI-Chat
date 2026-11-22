package com.kevin233.chat.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kevin233.chat.ChatViewModel
import com.kevin233.chat.Screen
import com.kevin233.chat.data.Conversation
import com.kevin233.chat.data.SearchHit
import com.kevin233.chat.util.Tokens
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HistoryScreen(vm: ChatViewModel) {
    BackHandler { vm.screen = Screen.CHAT }
    val palette = pal
    var query by rememberSaveable { mutableStateOf("") }
    var renameTarget by remember { mutableStateOf<Conversation?>(null) }
    var deleteTarget by remember { mutableStateOf<Conversation?>(null) }
    var editingTitle by remember { mutableStateOf("") }
    val hits = remember(query, vm.conversations) { vm.search(query) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri -> uri?.let { vm.exportTo(it) } }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let { vm.importFrom(it) } }

    Column(
        Modifier
            .fillMaxSize()
            .background(palette.bg)
            .statusBarsPadding()
    ) {
        Row(
            Modifier.fillMaxWidth().height(58.dp).padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { vm.screen = Screen.CHAT }) {
                Icon(Icons.Rounded.ArrowBack, "返回", tint = palette.text, modifier = Modifier.size(22.dp))
            }
            Column(Modifier.weight(1f)) {
                Text("历史记录", style = MaterialTheme.typography.titleLarge, color = palette.text)
            }
            IconButton(onClick = {
                importLauncher.launch(arrayOf("application/json", "text/plain", "application/octet-stream", "application/zip", "application/x-zip-compressed"))
            }) {
                Icon(Icons.Rounded.FileUpload, "导入", tint = palette.textSub, modifier = Modifier.size(20.dp))
            }
            IconButton(onClick = {
                exportLauncher.launch(vm.backupFileName())
            }) {
                Icon(Icons.Rounded.FileDownload, "导出", tint = palette.textSub, modifier = Modifier.size(20.dp))
            }
        }

        Surface(
            color = palette.surfaceAlt,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            Row(
                Modifier.padding(horizontal = 12.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Rounded.Search, null, tint = palette.textSub, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(9.dp))
                Box(Modifier.weight(1f)) {
                    if (query.isEmpty()) {
                        Text(
                            "搜索聊天内容、关键词…",
                            style = MaterialTheme.typography.bodyMedium,
                            color = palette.textSub
                        )
                    }
                    BasicTextField(
                        value = query,
                        onValueChange = { query = it },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium.copy(color = palette.text),
                        cursorBrush = SolidColor(palette.accent),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (query.isNotEmpty()) {
                    IconButton(onClick = { query = "" }, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Rounded.Close, "清空", tint = palette.textSub, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        if (hits.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                    Icon(
                        if (query.isBlank()) Icons.Rounded.Search else Icons.Rounded.SearchOff,
                        null,
                        tint = palette.textSub.copy(alpha = 0.5f),
                        modifier = Modifier.size(44.dp)
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        if (query.isBlank()) "还没有聊天记录" else "没找到「$query」",
                        style = MaterialTheme.typography.titleMedium,
                        color = palette.text
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        if (query.isBlank()) "去聊两句，记录会自动存在这台手机上" else "换个词试试，标题和内容都会搜",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.textSub
                    )
                }
            }
        } else if (query.isBlank()) {
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                var lastLabel = ""
                hits.forEach { hit ->
                    val label = dayLabel(hit.conversation.updatedAt)
                    if (label != lastLabel) {
                        lastLabel = label
                        item(key = "header-$label") {
                            Text(
                                label,
                                style = MaterialTheme.typography.labelMedium,
                                color = palette.textSub,
                                modifier = Modifier.padding(start = 4.dp, top = 14.dp, bottom = 2.dp)
                            )
                        }
                    }
                    item(key = hit.conversation.id) {
                        HistoryRow(
                            conv = hit.conversation,
                            onClick = { vm.openConversation(hit.conversation.id) },
                            onRename = {
                                renameTarget = hit.conversation
                                editingTitle = hit.conversation.title
                            },
                            onDelete = { deleteTarget = hit.conversation }
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Text(
                        "找到 ${hits.sumOf { it.matchCount }} 处，来自 ${hits.size} 个对话",
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.textSub,
                        modifier = Modifier.padding(start = 4.dp, top = 4.dp, bottom = 4.dp)
                    )
                }
                items(hits, key = { it.conversation.id }) { hit ->
                    SearchHitCard(
                        hit = hit,
                        query = query,
                        onOpenConversation = { vm.openConversation(hit.conversation.id) },
                        onOpenMessage = { msgId -> vm.openConversation(hit.conversation.id, msgId) }
                    )
                }
            }
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
            text = { Text("删掉就找不回来了。") },
            confirmButton = {
                TextButton(onClick = {
                    deleteTarget?.let { vm.deleteChat(it.id) }
                    deleteTarget = null
                }) { Text("删除", color = palette.danger) }
            },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("取消") } }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HistoryRow(
    conv: Conversation,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    val palette = pal
    var menu by remember { mutableStateOf(false) }
    val last = conv.messages.lastOrNull()
    Surface(
        color = palette.surface,
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, palette.border),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier
                .combinedClickable(onClick = onClick, onLongClick = { menu = true })
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    conv.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = palette.text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    last?.content?.replace("\n", " ")?.take(90) ?: "还没有消息",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textSub,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(5.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "${dayLabel(conv.updatedAt)} ${clockLabel(conv.updatedAt)}，${conv.messages.size} 条消息",
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.textSub.copy(alpha = 0.85f)
                    )
                    if (conv.hitTokens > 0) {
                        Spacer(Modifier.width(8.dp))
                        Icon(CacheStack, null, tint = palette.ok, modifier = Modifier.size(11.dp))
                        Text(
                            "省 ${Tokens.format(conv.hitTokens)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.ok
                        )
                    }
                }
            }
            Box {
                IconButton(onClick = { menu = true }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Rounded.MoreHoriz, "更多", tint = palette.textSub, modifier = Modifier.size(18.dp))
                }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(
                        text = { Text("重命名") },
                        leadingIcon = { Icon(Icons.Rounded.Edit, null, Modifier.size(18.dp)) },
                        onClick = {
                            menu = false
                            onRename()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("删除") },
                        leadingIcon = { Icon(Icons.Rounded.Delete, null, Modifier.size(18.dp)) },
                        onClick = {
                            menu = false
                            onDelete()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchHitCard(
    hit: SearchHit,
    query: String,
    onOpenConversation: () -> Unit,
    onOpenMessage: (String) -> Unit
) {
    val palette = pal
    Surface(
        color = palette.surface,
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, palette.border),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { onOpenConversation() }
                    .padding(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    hit.conversation.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = palette.text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    "${hit.matchCount} 处",
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.accent,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(palette.accentSoft)
                        .padding(horizontal = 7.dp, vertical = 2.dp)
                )
            }
            if (hit.snippets.isEmpty()) {
                Text(
                    "标题命中了，点开看看",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textSub,
                    modifier = Modifier.padding(start = 14.dp, end = 14.dp, bottom = 12.dp)
                )
            } else {
                hit.snippets.forEach { snippet ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { onOpenMessage(snippet.messageId) }
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            if (snippet.role == "user") "你" else "AI",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (snippet.role == "user") palette.accent else palette.textSub,
                            modifier = Modifier.width(26.dp)
                        )
                        Text(
                            text = highlightMatches(snippet.text, query),
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.textSub,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
            }
        }
    }
}

@Composable
private fun highlightMatches(text: String, query: String): AnnotatedString {
    val palette = pal
    if (query.isEmpty()) return AnnotatedString(text)
    return buildAnnotatedString {
        var start = 0
        val lowerText = text.lowercase()
        val lowerQuery = query.lowercase()
        while (true) {
            val index = lowerText.indexOf(lowerQuery, start)
            if (index < 0) {
                append(text.substring(start))
                break
            }
            append(text.substring(start, index))
            withStyle(
                SpanStyle(
                    color = palette.accentText,
                    background = palette.accentSoft,
                    fontWeight = FontWeight.Medium
                )
            ) { append(text.substring(index, index + query.length)) }
            start = index + query.length
        }
    }
}

private fun dayLabel(ts: Long): String {
    val now = Calendar.getInstance()
    val target = Calendar.getInstance().apply { timeInMillis = ts }
    val startToday = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val diffDays = ((startToday.timeInMillis - target.timeInMillis) / 86_400_000L).toInt()
    return when {
        target.timeInMillis >= startToday.timeInMillis -> "今天"
        diffDays < 2 -> "昨天"
        now.get(Calendar.YEAR) == target.get(Calendar.YEAR) && now.get(Calendar.DAY_OF_YEAR) - target.get(Calendar.DAY_OF_YEAR) < 7 -> "本周"
        else -> SimpleDateFormat("M月d日", Locale.CHINA).format(Date(ts))
    }
}

private fun clockLabel(ts: Long): String = SimpleDateFormat("HH:mm", Locale.CHINA).format(Date(ts))
