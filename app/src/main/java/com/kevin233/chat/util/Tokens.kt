package com.kevin233.chat.util

import com.kevin233.chat.data.CacheMode
import com.kevin233.chat.data.CacheProfile
import com.kevin233.chat.data.Conversation
import com.kevin233.chat.data.Message

object Tokens {

    fun estimate(text: String): Int {
        if (text.isEmpty()) return 0
        var cjk = 0
        var other = 0
        for (ch in text) {
            val c = ch.code
            when {
                c in 0x2E80..0x9FFF -> cjk++
                c in 0xAC00..0xD7AF -> cjk++
                c in 0x3000..0x303F -> cjk++
                c in 0xFF00..0xFFEF -> cjk++
                else -> other++
            }
        }
        return (cjk * 0.65 + other / 3.6).toInt() + 1
    }

    fun estimateMessages(messages: List<Message>): Int {
        var total = 0
        for (m in messages) {
            total += estimate(m.content) + estimate(m.reasoning) + 4 + m.images.size * 800
        }
        return total
    }

    fun estimateConversation(conv: Conversation, fallbackSystem: String): Int {
        val sys = conv.systemPrompt.ifBlank { fallbackSystem }
        return estimate(sys) + 8 + estimateMessages(conv.messages)
    }

    fun format(n: Int): String = when {
        n >= 1_000_000 -> String.format("%.1fM", n / 1_000_000.0)
        n >= 10_000 -> String.format("%.1fK", n / 1_000.0)
        n >= 1_000 -> String.format("%.2fK", n / 1_000.0)
        else -> n.toString()
    }
}

data class PromptAudit(
    val dynamicLines: List<String>,
    val warnings: List<String>
) {
    val hasIssue: Boolean get() = dynamicLines.isNotEmpty() || warnings.isNotEmpty()
}

object PromptDoctor {

    private val dynamicPatterns = listOf(
        Regex("(?i)今天是|当前时间|现在时间|此刻|实时时间"),
        Regex("(?i)时间[:：]|日期[:：]|date[:：]|time[:：]"),
        Regex("\\d{4}\\s*[-/年]\\s*\\d{1,2}\\s*[-/月]\\s*\\d{1,2}"),
        Regex("\\{\\{\\s*[\\w.\\u4e00-\\u9fa5]+\\s*\\}\\}"),
        Regex("\\$\\{[^}]*\\}"),
        Regex("(?i)session[_ ]?id|随机数|random|uuid"),
        Regex("(?i)用户(名|昵称)[:：]|用户名[:：]|姓名[:：]"),
        Regex("(?i)第\\s*\\d+\\s*轮|本轮|当前问题")
    )

    fun audit(prompt: String, profile: CacheProfile): PromptAudit {
        if (prompt.isBlank()) return PromptAudit(emptyList(), emptyList())
        val lines = prompt.lines()
        val dynamic = lines.filter { line ->
            line.isNotBlank() && dynamicPatterns.any { it.containsMatchIn(line) }
        }
        val warnings = mutableListOf<String>()
        val tokens = Tokens.estimate(prompt)
        if (profile.mode == CacheMode.NONE) {
            warnings.add("当前模型基本不做提示词缓存，缩短上下文比优化提示词更有效。")
        } else if (profile.minPrefix > 0 && tokens < profile.minPrefix) {
            warnings.add("系统提示词偏短（约 $tokens token），还没到 ${profile.minPrefix} token 的缓存门槛，可以固定补充一些常用背景资料。")
        }
        if (dynamic.isNotEmpty()) {
            warnings.add("发现 ${dynamic.size} 行会变的内容，放在开头会让后面的缓存全部作废。")
        }
        if (prompt.contains("祝你") || prompt.contains("根据对话内容")) {
            warnings.add("提示词里有和对话无关的客套话，删掉能省一点 token。")
        }
        return PromptAudit(dynamic, warnings)
    }

    fun moveDynamicToEnd(prompt: String): String {
        val lines = prompt.lines()
        val dynamic = mutableListOf<String>()
        val stable = mutableListOf<String>()
        for (line in lines) {
            if (line.isNotBlank() && dynamicPatterns.any { it.containsMatchIn(line) }) dynamic.add(line.trim()) else stable.add(line)
        }
        if (dynamic.isEmpty()) return prompt
        val base = stable.joinToString("\n").trim()
        val tail = dynamic.joinToString("\n")
        return if (base.isEmpty()) tail else "$base\n\n【以下内容可能随时变化，放在最后，不影响前面的缓存】\n$tail"
    }
}
