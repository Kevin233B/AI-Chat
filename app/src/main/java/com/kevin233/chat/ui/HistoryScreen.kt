package com.kevin233.chat.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
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

    Column(Modifier.fillMaxSize().background(palette.bg).statusBarsPadding()) {
        Row(
            Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HistoryCircleButton(Icons.Rounded.ArrowBack, "返回") { vm.screen = Screen.CHAT }
            Spacer(Modifier.width(10.dp))
            Text(
                "历史记录",
                style = MaterialTheme.typography.headlineSmall,
                color = palette.text,
                modifier = Modifier.weight(1f)
            )
            HistoryCircleButton(Icons.Rounded.FileUpload, "导入") {
                importLauncher.launch(
                    arrayOf(
                        "application/json", "text/plain", "application/octet-stream",
                        "application/zip", "application/x-zip-compressed"
                    )
                )
            }
            Spacer(Modifier.width(6.dp))
            HistoryCircleButton(Icons.Rounded.FileDownload, "导出") {
                exportLauncher.launch(vm.backupFileName())
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)
                .background(palette.surfaceAlt, RoundedCornerShape(14.dp))
                .padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Rounded.Search, null, tint = palette.textSub, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(9.dp))
            Box(Modifier.weight(1f).padding(vertical = 8.dp)) {
                if (query.isEmpty()) {
                    Text(
                        "搜索标题和聊天内容",
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.textSub.copy(alpha = 0.78f)
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
                IconButton(onClick = { query = "" }, modifier = Modifier.size(30.dp)) {
                    Icon(Icons.Rounded.Close, "清空", tint = palette.textSub, modifier = Modifier.size(16.dp))
                }
            }
        }

        if (hits.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                    Box(
                        Modifier.size(58.dp).background(palette.accentSoft, RoundedCornerShape(18.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (query.isBlank()) Icons.Rounded.SearchOff else Icons.Rounded.Search,
                            null,
                            tint = palette.accent,
                            modifier = Modifier.size(27.dp)
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    Text(
                        if (query.isBlank()) "还没有对话" else "没有找到",
                        style = MaterialTheme.typography.headlineSmall,
                        color = palette.text
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        if (query.isBlank()) "聊两句，记录会自动保存在手机上" else "换个关键词试试，标题和内容都会搜",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.textSub,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else if (query.isBlank()) {
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 10.dp,
                    bottom = 32.dp
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "${vm.conversations.size} 个对话，${vm.conversations.sumOf { it.messages.size }} 条消息",
                            style = MaterialTheme.typography.labelMedium,
                            color = palette.textSub,
                            modifier = Modifier.weight(1f)
                        )
                        Text("最近优先", style = MaterialTheme.typography.labelSmall, color = palette.textSub)
                    }
                }
                var lastLabel = ""
                hits.forEach { hit ->
                    val label = Clock.groupTitle(hit.conversation.updatedAt)
                    if (label != lastLabel) {
                        lastLabel = label
                        item(key = "header-$label") {
                            Text(
                                label,
                                style = MaterialTheme.typography.labelMedium,
                                color = palette.textSub,
                                modifier = Modifier.padding(start = 4.dp, top = 12.dp, bottom = 2.dp)
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
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 10.dp,
                    bottom = 32.dp
                ),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Text(
                        "找到 ${hits.size} 个对话",
                        style = MaterialTheme.typography.labelMedium,
                        color = palette.textSub,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
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
                }) { Text("删除", color = palette.danger) }
            },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("取消") } }
        )
    }
}

@Composable
private fun HistoryCircleButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    val palette = pal
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(38.dp).clip(CircleShape).background(palette.surfaceAlt)
    ) {
        Icon(icon, label, tint = palette.text, modifier = Modifier.size(19.dp))
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
    Row(
        Modifier
            .fillMaxWidth()
            .background(palette.surface, RoundedCornerShape(18.dp))
            .border(1.dp, palette.border, RoundedCornerShape(18.dp))
            .combinedClickable(onClick = onClick, onLongClick = { menu = true })
            .padding(start = 14.dp, end = 4.dp, top = 13.dp, bottom = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(40.dp).background(palette.accentSoft, RoundedCornerShape(13.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                conv.title.take(1).ifBlank { "新" },
                style = MaterialTheme.typography.titleMedium,
                color = palette.accent
            )
        }
        Spacer(Modifier.width(11.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    conv.title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = palette.text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (conv.branchOrder > 0) {
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "分支 ${conv.branchOrder}",
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.accent,
                        modifier = Modifier
                            .background(palette.accentSoft, RoundedCornerShape(50))
                            .padding(horizontal = 7.dp, vertical = 2.dp)
                    )
                }
            }
            Spacer(Modifier.height(3.dp))
            Text(
                last?.content?.replace("\n", " ")?.take(110) ?: "还没有消息",
                style = MaterialTheme.typography.bodySmall,
                color = palette.textSub,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(7.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(Clock.ago(conv.updatedAt), style = MaterialTheme.typography.labelSmall, color = palette.textSub)
                Spacer(Modifier.width(8.dp))
                Text("${conv.messages.size} 条消息", style = MaterialTheme.typography.labelSmall, color = palette.textSub)
                if (conv.hitTokens > 0) {
                    Spacer(Modifier.width(8.dp))
                    CacheBadge(conv.hitTokens, conv.missTokens, compact = true)
                }
            }
        }
        Box {
            IconButton(onClick = { menu = true }, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Rounded.MoreHoriz, "更多", tint = palette.textSub, modifier = Modifier.size(17.dp))
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

@Composable
private fun SearchHitCard(
    hit: SearchHit,
    query: String,
    onOpenConversation: () -> Unit,
    onOpenMessage: (String) -> Unit
) {
    val palette = pal
    Column(
        Modifier.fillMaxWidth().background(palette.surface, RoundedCornerShape(18.dp))
            .border(1.dp, palette.border, RoundedCornerShape(18.dp))
    ) {
        Row(
            Modifier.fillMaxWidth().clickable { onOpenConversation() }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    "匹配 ${hit.matchCount} 处",
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.accent
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    hit.conversation.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = palette.text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text("↗", style = MaterialTheme.typography.labelMedium, color = palette.accent)
        }
        if (hit.snippets.isEmpty()) {
            Hairline()
            Text(
                "标题命中，点开查看",
                style = MaterialTheme.typography.bodySmall,
                color = palette.textSub,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
            )
        } else {
            hit.snippets.forEach { snippet ->
                Hairline(Modifier.padding(start = 16.dp))
                Row(
                    Modifier.fillMaxWidth().clickable { onOpenMessage(snippet.messageId) }
                        .padding(horizontal = 16.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        if (snippet.role == "user") "你" else "AI",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (snippet.role == "user") palette.accent else palette.textSub,
                        modifier = Modifier.width(26.dp).padding(top = 2.dp)
                    )
                    Text(
                        text = highlightMatches(snippet.text, query),
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.textSub,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
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
