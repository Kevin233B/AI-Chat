package com.kevin233.chat.net

import android.text.Html
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLDecoder
import java.net.URLEncoder

object SearchClient {

    private data class Hit(val title: String, val url: String, val snippet: String)

    suspend fun search(query: String, provider: String): String? = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext null
        val primary = if (provider == "bing") bing(query) else duck(query)
        val results = if (primary.isNotEmpty()) primary else {
            if (provider == "bing") duck(query) else bing(query)
        }
        if (results.isEmpty()) null else build(results)
    }

    private fun duck(query: String): List<Hit> {
        return try {
            val html = fetch("https://html.duckduckgo.com/html/?q=${encode(query)}")
            val anchors = Regex("<a([^>]*class=\"result__a\"[^>]*)>(.*?)</a>", RegexOption.DOT_MATCHES_ALL)
                .findAll(html).take(5).toList()
            val snippets = Regex("<a[^>]*class=\"result__snippet\"[^>]*>(.*?)</a>", RegexOption.DOT_MATCHES_ALL)
                .findAll(html).map { clean(it.groupValues[1]) }.toList()
            anchors.mapIndexedNotNull { index, match ->
                val href = Regex("href=\"([^\"]+)\"").find(match.groupValues[1])?.groupValues?.get(1) ?: return@mapIndexedNotNull null
                val url = decodeDuckUrl(href)
                val title = clean(match.groupValues[2])
                if (title.isBlank() || url.isBlank()) return@mapIndexedNotNull null
                Hit(title.take(120), url, snippets.getOrElse(index) { "" }.take(220))
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun bing(query: String): List<Hit> {
        return try {
            val html = fetch("https://www.bing.com/search?q=${encode(query)}&setlang=zh-Hans")
            Regex("<li class=\"b_algo\".*?</li>", RegexOption.DOT_MATCHES_ALL)
                .findAll(html).take(5).mapNotNull { match ->
                    val block = match.value
                    val link = Regex("<h2[^>]*>\\s*<a[^>]*href=\"([^\"]+)\"[^>]*>(.*?)</a>", RegexOption.DOT_MATCHES_ALL)
                        .find(block) ?: return@mapNotNull null
                    val url = link.groupValues[1]
                    val title = clean(link.groupValues[2])
                    val snippet = Regex("<p[^>]*>(.*?)</p>", RegexOption.DOT_MATCHES_ALL)
                        .find(block)?.groupValues?.get(1)?.let { clean(it) } ?: ""
                    if (title.isBlank() || url.isBlank()) return@mapNotNull null
                    Hit(title.take(120), url, snippet.take(220))
                }.toList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun fetch(url: String): String {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = 10000
        connection.readTimeout = 15000
        connection.requestMethod = "GET"
        connection.setRequestProperty(
            "User-Agent",
            "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Mobile Safari/537.36"
        )
        connection.setRequestProperty("Accept", "text/html,application/xhtml+xml")
        connection.setRequestProperty("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
        connection.setRequestProperty("Accept-Encoding", "identity")
        return try {
            if (connection.responseCode !in 200..299) "" else {
                connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun build(hits: List<Hit>): String {
        val sb = StringBuilder()
        sb.append("【联网搜索结果，来自公开搜索，可能不完整】\n")
        hits.forEachIndexed { index, hit ->
            sb.append(index + 1).append(". ").append(hit.title).append('\n')
            if (hit.url.isNotBlank()) sb.append(hit.url).append('\n')
            if (hit.snippet.isNotBlank()) sb.append(hit.snippet).append('\n')
            sb.append('\n')
        }
        sb.append("请结合以上结果回答用户的问题，并在结果不足时明确说明。")
        return sb.toString()
    }

    private fun encode(value: String): String = URLEncoder.encode(value, "UTF-8")

    private fun decodeDuckUrl(href: String): String {
        val raw = if (href.contains("uddg=")) {
            try {
                URLDecoder.decode(href.substringAfter("uddg=").substringBefore("&"), "UTF-8")
            } catch (e: Exception) {
                href
            }
        } else {
            href
        }
        return when {
            raw.startsWith("//") -> "https:$raw"
            raw.startsWith("http://") || raw.startsWith("https://") -> raw
            else -> ""
        }
    }

    private fun clean(value: String): String {
        val text = try {
            Html.fromHtml(value, Html.FROM_HTML_MODE_LEGACY).toString()
        } catch (e: Exception) {
            value
        }
        return text.replace(Regex("\\s+"), " ").trim()
    }
}
