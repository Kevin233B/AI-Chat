package com.kevin233.chat.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

sealed interface MdBlock {
    data class Code(val lang: String, val code: String) : MdBlock
    data class Heading(val level: Int, val text: String) : MdBlock
    data class Bullet(val index: Int, val text: String, val indent: Int) : MdBlock
    data class Quote(val text: String) : MdBlock
    data class Image(val url: String, val alt: String) : MdBlock
    data class Para(val text: String) : MdBlock
    data class Table(val rows: List<List<String>>) : MdBlock
    data object Rule : MdBlock
}

object Markdown {

    private val bulletRegex = Regex("^(\\s*)[-*+]\\s+(.*)$")
    private val orderedRegex = Regex("^(\\s*)(\\d{1,2})[.)、]\\s*(.*)$")
    private val headingRegex = Regex("^(#{1,4})\\s+(.*)$")
    private val ruleRegex = Regex("^\\s*([-*_])\\1{2,}\\s*$")
    private val imageRegex = Regex("^!\\[([^\\]]*)]\\(([^)\\s]+)\\)$")

    fun parse(source: String): List<MdBlock> {
        val blocks = mutableListOf<MdBlock>()
        val lines = source.replace("\r\n", "\n").split("\n")
        var i = 0
        val para = StringBuilder()
        fun flush() {
            if (para.isNotBlank()) blocks.add(MdBlock.Para(trimTrailing(para.toString())))
            para.clear()
        }
        while (i < lines.size) {
            val line = lines[i]
            val trimmed = line.trimStart()
            if (trimmed.startsWith("```")) {
                flush()
                val lang = trimmed.removePrefix("```").trim()
                val code = StringBuilder()
                i++
                while (i < lines.size && !lines[i].trimStart().startsWith("```")) {
                    if (code.isNotEmpty()) code.append("\n")
                    code.append(lines[i])
                    i++
                }
                blocks.add(MdBlock.Code(lang, code.toString()))
                i++
                continue
            }
            if (trimmed.isEmpty()) {
                flush()
                i++
                continue
            }
            val heading = headingRegex.find(trimmed)
            if (heading != null) {
                flush()
                blocks.add(MdBlock.Heading(heading.groupValues[1].length, heading.groupValues[2].trim()))
                i++
                continue
            }
            if (ruleRegex.matches(line)) {
                flush()
                blocks.add(MdBlock.Rule)
                i++
                continue
            }
            if (trimmed.startsWith("|") && i + 1 < lines.size && isTableSeparator(lines[i + 1])) {
                flush()
                val rows = mutableListOf<List<String>>()
                while (i < lines.size && lines[i].trimStart().startsWith("|")) {
                    val raw = lines[i].trim()
                    if (!isTableSeparator(raw)) {
                        val cells = raw.removePrefix("|").removeSuffix("|").split("|").map { it.trim() }
                        if (cells.any { it.isNotEmpty() }) rows.add(cells)
                    }
                    i++
                }
                if (rows.isNotEmpty()) blocks.add(MdBlock.Table(rows))
                continue
            }
            val image = imageRegex.find(trimmed)
            if (image != null) {
                flush()
                blocks.add(MdBlock.Image(image.groupValues[2], image.groupValues[1]))
                i++
                continue
            }
            if (trimmed.startsWith(">")) {
                flush()
                val quote = StringBuilder()
                while (i < lines.size && lines[i].trimStart().startsWith(">")) {
                    if (quote.isNotEmpty()) quote.append("\n")
                    quote.append(lines[i].trimStart().removePrefix(">").trimStart())
                    i++
                }
                blocks.add(MdBlock.Quote(quote.toString()))
                continue
            }
            val bullet = bulletRegex.find(line)
            if (bullet != null) {
                flush()
                blocks.add(MdBlock.Bullet(0, bullet.groupValues[2].trim(), bullet.groupValues[1].length / 2))
                i++
                continue
            }
            val ordered = orderedRegex.find(line)
            if (ordered != null) {
                flush()
                blocks.add(
                    MdBlock.Bullet(
                        ordered.groupValues[2].toIntOrNull() ?: 1,
                        ordered.groupValues[3].trim(),
                        ordered.groupValues[1].length / 2
                    )
                )
                i++
                continue
            }
            if (para.isNotEmpty()) para.append("\n")
            para.append(line.trim())
            i++
        }
        flush()
        return blocks
    }

    private fun trimTrailing(s: String): String = s.trimEnd()

    private fun isTableSeparator(line: String): Boolean {
        val t = line.trim()
        if (!t.contains('|')) return false
        val cells = t.removePrefix("|").removeSuffix("|").split("|")
        if (cells.isEmpty()) return false
        return cells.all { cell ->
            val c = cell.trim()
            c.isNotEmpty() && c.matches(Regex("^:?-{2,}:?$"))
        }
    }

    private val inlineRegex = Regex(
        "(`[^`\\n]+`|\\*\\*[^*\\n]+\\*\\*|__[^_\\n]+__|~~[^~\\n]+~~|\\[[^\\]]+\\]\\([^)\\s]+\\)|\\*[^*\\n]+\\*|_[^_\\n]+_)"
    )

    @Composable
    fun inline(text: String, color: Color, baseSize: Float = 16f, lineHeight: Float = 26f): AnnotatedString {
        val palette = pal
        val style = SpanStyle(color = color)
        return buildAnnotatedString {
            var last = 0
            for (m in inlineRegex.findAll(text)) {
                if (m.range.first > last) append(text.substring(last, m.range.first))
                val token = m.value
                when {
                    token.startsWith("`") -> withStyle(
                        SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            color = palette.accentText,
                            background = palette.accentSoft,
                            fontSize = (baseSize - 1.5f).sp
                        )
                    ) { append(token.trim('`')) }
                    token.startsWith("**") || token.startsWith("__") -> withStyle(
                        SpanStyle(fontWeight = FontWeight.SemiBold, color = color)
                    ) { append(token.substring(2, token.length - 2)) }
                    token.startsWith("~~") -> withStyle(
                        SpanStyle(textDecoration = TextDecoration.LineThrough, color = palette.textSub)
                    ) { append(token.substring(2, token.length - 2)) }
                    token.startsWith("[") -> {
                        val close = token.indexOf("](")
                        val label = token.substring(1, close)
                        val url = token.substring(close + 2, token.length - 1)
                        withLink(
                            LinkAnnotation.Url(
                                url,
                                TextLinkStyles(
                                    style = SpanStyle(color = palette.accent, textDecoration = TextDecoration.Underline)
                                )
                            )
                        ) { append(label) }
                    }
                    else -> withStyle(SpanStyle(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)) {
                        append(token.trim('*', '_'))
                    }
                }
                last = m.range.last + 1
            }
            if (last < text.length) append(text.substring(last))
            addStyle(style, 0, length)
        }
    }
}

@Composable
fun MarkdownText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = pal.text,
    onImageClick: (String) -> Unit = {}
) {
    val palette = pal
    val blocks = Markdown.parse(text)
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        blocks.forEach { block ->
            when (block) {
                is MdBlock.Code -> {
                    val langLower = block.lang.lowercase()
                    val previewable = langLower == "html" || langLower == "svg" ||
                        block.code.trimStart().startsWith("<!doctype html", true) ||
                        block.code.trimStart().startsWith("<html", true) ||
                        block.code.trimStart().startsWith("<svg", true)
                    var preview by remember(block.code) { mutableStateOf(false) }
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .background(palette.codeBg, RoundedCornerShape(14.dp))
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(start = 14.dp, end = 4.dp, top = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = block.lang.ifBlank { "code" },
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF8B93A5),
                                modifier = Modifier.weight(1f)
                            )
                            if (previewable) {
                                TextButton(
                                    onClick = { preview = true },
                                    contentPadding = PaddingValues(horizontal = 7.dp)
                                ) {
                                    Text("预览", fontSize = 11.sp, color = Color(0xFFB9C0CE))
                                }
                            }
                            CopyButton(block.code, Color(0xFFB9C0CE))
                        }
                        Text(
                            text = block.code,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                            lineHeight = 20.sp,
                            color = palette.codeText,
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(start = 14.dp, end = 14.dp, top = 2.dp, bottom = 12.dp)
                        )
                    }
                    if (preview) {
                        CodePreviewDialog(block.code, block.lang) { preview = false }
                    }
                }
                is MdBlock.Heading -> Text(
                    text = Markdown.inline(block.text, palette.text, 16f + (4 - block.level) * 1.5f, 28f),
                    fontSize = (17 + (4 - block.level) * 1.5f).sp,
                    lineHeight = (26 + (4 - block.level) * 2).sp,
                    fontWeight = FontWeight.SemiBold,
                    color = palette.text
                )
                is MdBlock.Bullet -> Row(
                    Modifier.padding(start = (block.indent * 14).dp),
                    verticalAlignment = Alignment.Top
                ) {
                    if (block.index > 0) {
                        Text(
                            text = "${block.index}.",
                            color = palette.textSub,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    } else {
                        BulletDot(Modifier.padding(top = 10.dp))
                        Spacer(Modifier.width(9.dp))
                    }
                    Text(
                        text = Markdown.inline(block.text, palette.text),
                        style = MaterialTheme.typography.bodyLarge,
                        color = palette.text
                    )
                }
                is MdBlock.Quote -> Row {
                    Surface(
                        color = palette.border,
                        shape = RoundedCornerShape(2.dp),
                        modifier = Modifier.padding(top = 2.dp, bottom = 2.dp).width(3.dp)
                    ) {}
                    Text(
                        text = Markdown.inline(block.text, palette.textSub),
                        style = MaterialTheme.typography.bodyLarge,
                        color = palette.textSub,
                        modifier = Modifier.padding(start = 10.dp)
                    )
                }
                is MdBlock.Image -> Column {
                    RemoteImage(block.url, onClick = { onImageClick(block.url) })
                    if (block.alt.isNotBlank()) {
                        Text(
                            block.alt,
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.textSub,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
                is MdBlock.Rule -> Surface(
                    color = palette.border,
                    modifier = Modifier.fillMaxWidth().height(1.dp)
                ) {}
                is MdBlock.Table -> Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(palette.surfaceAlt)
                ) {
                    block.rows.forEachIndexed { rowIndex, cells ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .background(if (rowIndex == 0) palette.accentSoft.copy(alpha = 0.55f) else Color.Transparent)
                                .padding(vertical = 8.dp)
                        ) {
                            cells.forEach { cell ->
                                Text(
                                    text = Markdown.inline(cell, palette.text, 13f, 20f),
                                    fontSize = 13.sp,
                                    lineHeight = 20.sp,
                                    fontWeight = if (rowIndex == 0) FontWeight.Medium else FontWeight.Normal,
                                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                                )
                            }
                        }
                        if (rowIndex < block.rows.lastIndex) {
                            Box(Modifier.fillMaxWidth().height(1.dp).background(palette.border))
                        }
                    }
                }
                is MdBlock.Para -> Text(
                    text = Markdown.inline(block.text, color),
                    style = MaterialTheme.typography.bodyLarge,
                    color = color
                )
            }
        }
        if (blocks.isEmpty()) {
            Text("", style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
fun CopyButton(text: String, tint: Color) {
    val clipboard = LocalClipboardManager.current
    IconButton(onClick = { clipboard.setText(AnnotatedString(text)) }, modifier = Modifier.size(34.dp)) {
        Icon(
            imageVector = Icons.Rounded.ContentCopy,
            contentDescription = "复制",
            tint = tint,
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
fun MarkdownPreview(text: String, maxLines: Int = 4) {
    val single = text.replace("\n", " ")
    Text(
        text = single,
        style = MaterialTheme.typography.bodyMedium,
        color = pal.textSub,
        maxLines = maxLines,
        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
    )
}
