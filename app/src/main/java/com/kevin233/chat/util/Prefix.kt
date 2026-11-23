package com.kevin233.chat.util

import com.kevin233.chat.data.Message
import com.kevin233.chat.data.composePrompt
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Prefix {

    fun stableHash(system: String, messages: List<Message>): String {
        val sb = StringBuilder()
        sb.append(system).append('\u0000')
        messages.forEach { m ->
            sb.append(m.role).append('\u0001')
            sb.append(m.content).append('\u0001')
            sb.append(m.timeNote).append('\u0001')
            sb.append(m.searchContext).append('\u0001')
            sb.append(m.images.size).append('\u0001')
        }
        return sha256(sb.toString()).take(16)
    }

    fun sha256(text: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(text.toByteArray(Charsets.UTF_8))
        val sb = StringBuilder(bytes.size * 2)
        for (b in bytes) {
            val v = b.toInt() and 0xFF
            if (v < 16) sb.append('0')
            sb.append(Integer.toHexString(v))
        }
        return sb.toString()
    }

    fun nowLabel(): String =
        SimpleDateFormat("yyyy年M月d日 HH:mm", Locale.CHINA).format(Date())

    fun buildSystem(systemPrompt: String, anchor: String): String =
        composePrompt(systemPrompt, anchor)

    fun isPrefixBroken(previous: String, current: String): Boolean =
        previous.isNotBlank() && current.isNotBlank() && previous != current
}
