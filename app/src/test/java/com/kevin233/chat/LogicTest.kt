package com.kevin233.chat

import com.kevin233.chat.data.ChatSearch
import com.kevin233.chat.data.Conversation
import com.kevin233.chat.data.Message
import com.kevin233.chat.data.Presets
import com.kevin233.chat.util.PromptDoctor
import com.kevin233.chat.util.Tokens
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LogicTest {

    private val convCache = Conversation(
        id = "a",
        title = "缓存怎么用",
        messages = listOf(
            Message(id = "m1", role = "user", content = "帮我解释一下提示词缓存"),
            Message(id = "m2", role = "assistant", content = "缓存就是把重复的开头存起来，下次直接复用，命中后更便宜。")
        ),
        updatedAt = 3000L
    )

    private val convTranslate = Conversation(
        id = "b",
        title = "翻译",
        messages = listOf(Message(id = "m3", role = "user", content = "把今天天气很好翻译成英文")),
        updatedAt = 1000L
    )

    @Test
    fun searchFindsMessagesAndTitle() {
        val hits = ChatSearch.search(listOf(convTranslate, convCache), "缓存")
        assertEquals(1, hits.size)
        assertEquals("a", hits[0].conversation.id)
        assertTrue(hits[0].snippets.isNotEmpty())
        assertTrue(hits[0].snippets[0].text.contains("缓存"))
        assertEquals(3, hits[0].matchCount)
    }

    @Test
    fun blankQueryListsByRecent() {
        val hits = ChatSearch.search(listOf(convTranslate, convCache), "   ")
        assertEquals("a", hits[0].conversation.id)
        assertEquals(2, hits.size)
    }

    @Test
    fun searchMissReturnsEmpty() {
        assertTrue(ChatSearch.search(listOf(convCache), "不存在的词").isEmpty())
    }

    @Test
    fun promptDoctorMovesDynamicLines() {
        val prompt = "你是助手。\n今天是 2026-11-22。\n回答要简洁。"
        val profile = Presets.cacheProfile(Presets.toConfig(Presets.all.first()))
        val audit = PromptDoctor.audit(prompt, profile)
        assertTrue(audit.dynamicLines.isNotEmpty())
        val moved = PromptDoctor.moveDynamicToEnd(prompt)
        assertTrue(moved.lines().last().contains("今天是"))
        assertTrue(moved.startsWith("你是助手。"))
    }

    @Test
    fun tokenEstimateIsReasonable() {
        assertTrue(Tokens.estimate("你好世界") in 2..6)
        assertTrue(Tokens.estimate("hello world") in 2..5)
        assertEquals(0, Tokens.estimate(""))
    }

    @Test
    fun cacheProfilesDifferByProvider() {
        val ds = Presets.cacheProfile(Presets.toConfig(Presets.byId("deepseek")!!))
        val claude = Presets.cacheProfile(Presets.toConfig(Presets.byId("claude")!!))
        val groq = Presets.cacheProfile(Presets.toConfig(Presets.byId("groq")!!))
        assertEquals(64, ds.minPrefix)
        assertEquals(1024, claude.minPrefix)
        assertEquals(0, groq.minPrefix)
    }
}
