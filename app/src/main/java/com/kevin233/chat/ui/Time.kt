package com.kevin233.chat.ui

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object Clock {

    private val hm = SimpleDateFormat("HH:mm", Locale.CHINA)
    private val hmss = SimpleDateFormat("HH:mm:ss", Locale.CHINA)
    private val md = SimpleDateFormat("M月d日", Locale.CHINA)
    private val ymd = SimpleDateFormat("yyyy.MM.dd", Locale.CHINA)
    private val fullFormat = SimpleDateFormat("yyyy.MM.dd HH:mm", Locale.CHINA)

    fun clock(ts: Long): String = hm.format(Date(ts))

    fun precise(ts: Long): String = hmss.format(Date(ts))

    fun day(ts: Long): String = md.format(Date(ts))

    fun stamp(ts: Long): String = ymd.format(Date(ts))

    fun full(ts: Long): String = fullFormat.format(Date(ts))

    fun duration(ms: Long): String = when {
        ms <= 0 -> ""
        ms < 1000 -> "${ms}ms"
        ms < 60_000 -> String.format(Locale.CHINA, "%.1fs", ms / 1000.0)
        else -> "${ms / 60_000}m${(ms % 60_000) / 1000}s"
    }

    fun ago(ts: Long): String {
        val diff = System.currentTimeMillis() - ts
        return when {
            diff < 60_000 -> "刚刚"
            diff < 3_600_000 -> "${diff / 60_000}分钟前"
            diff < 86_400_000 -> "${diff / 3_600_000}小时前"
            diff < 172_800_000 -> "昨天"
            else -> day(ts)
        }
    }

    fun groupTitle(ts: Long): String {
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val target = Calendar.getInstance().apply { timeInMillis = ts }
        val start = today.timeInMillis
        return when {
            ts >= start -> "今天"
            ts >= start - 86_400_000L -> "昨天"
            ts >= start - 6 * 86_400_000L -> "本周"
            target.get(Calendar.YEAR) == today.get(Calendar.YEAR) -> day(ts)
            else -> stamp(ts)
        }
    }
}
