package com.kevin233.chat.net

import android.text.Html
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLDecoder
import java.net.URLEncoder

data class SearchResult(
    val title: String,
    val url: String,
    val snippet: String
)

object SearchClient {

    const val TOOL_NAME = "web_search"

    fun toolSpec(provider: String): JSONObject {
        val source = when (provider) {
            "bing" -> "Bing"
            "wikipedia" -> "Wikipedia"
            else -> "DuckDuckGo"
        }
        return JSONObject()
            .put("type", "function")
            .put(
                "function",
                JSONObject()
                    .put("name", TOOL_NAME)
                    .put("description", "Search the public web ($source) for up to date or factual information. Use it when the answer depends on recent events, prices, versions, or facts you are unsure about.")
                    .put(
                        "parameters",
                        JSONObject()
                            .put("type", "object")
                            .put(
                                "properties",
                                JSONObject().put(
                                    "query",
                                    JSONObject()
                                        .put("type", "string")
                                        .put("description", "Short search keywords, same language as the question.")
                                )
                            )
                            .put("required", JSONArray().put("query"))
                    )
            )
    }

    suspend fun search(query: String, provider: String): List<SearchResult> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        val trimmed = query.trim().take(200)
        val results = when (provider) {
            "bing" -> bing(trimmed)
            "wikipedia" -> wikipedia(trimmed)
            else -> duck(trimmed)
        }
        if (results.isNotEmpty()) return@withContext results
        when (provider) {
            "bing" -> duck(trimmed)
            "wikipedia" -> duck(trimmed)
            else -> bing(trimmed)
        }
    }

    fun searchContext(query: String, results: List<SearchResult>): String {
        if (results.isEmpty()) return "【联网搜索：$query】没有取到结果，请基于已有知识回答，并说明信息可能不是最新的。"
        val sb = StringBuilder()
        sb.append("【联网搜索：").append(query).append("】\n")
        results.take(5).forEachIndexed { index, hit ->
            sb.append(index + 1).append(". ").append(hit.title).append('\n')
            if (hit.url.isNotBlank()) sb.append(hit.url).append('\n')
            if (hit.snippet.isNotBlank()) sb.append(hit.snippet).append('\n')
        }
        sb.append("以上是公开搜索结果，可能不完整或过期，回答时请自行判断。")
        return sb.toString()
    }

    private fun duck(query: String): List<SearchResult> {
        return try {
            val html = fetch("https://html.duckduckgo.com/html/?q=${encode(query)}")
            val anchors = Regex("<a([^>]*class=\"result__a\"[^>]*)>(.*?)</a>", RegexOption.DOT_MATCHES_ALL)
                .findAll(html).take(6).toList()
            val snippets = Regex("<a[^>]*class=\"result__snippet\"[^>]*>(.*?)</a>", RegexOption.DOT_MATCHES_ALL)
                .findAll(html).map { clean(it.groupValues[1]) }.toList()
            anchors.mapIndexedNotNull { index, match ->
                val href = Regex("href=\"([^\"]+)\"").find(match.groupValues[1])?.groupValues?.get(1)
                    ?: return@mapIndexedNotNull null
                val url = decodeDuckUrl(href)
                val title = clean(match.groupValues[2])
                if (title.isBlank() || url.isBlank()) return@mapIndexedNotNull null
                SearchResult(title.take(120), url, snippets.getOrElse(index) { "" }.take(240))
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun bing(query: String): List<SearchResult> {
        return try {
            val html = fetch("https://www.bing.com/search?q=${encode(query)}&setlang=zh-Hans")
            Regex("<li class=\"b_algo\".*?</li>", RegexOption.DOT_MATCHES_ALL)
                .findAll(html).take(6).mapNotNull { match ->
                    val block = match.value
                    val link = Regex(
                        "<h2[^>]*>\\s*<a[^>]*href=\"([^\"]+)\"[^>]*>(.*?)</a>",
                        RegexOption.DOT_MATCHES_ALL
                    ).find(block) ?: return@mapNotNull null
                    val url = link.groupValues[1]
                    val title = clean(link.groupValues[2])
                    val snippet = Regex("<p[^>]*>(.*?)</p>", RegexOption.DOT_MATCHES_ALL)
                        .find(block)?.groupValues?.get(1)?.let { clean(it) } ?: ""
                    if (title.isBlank() || url.isBlank()) return@mapNotNull null
                    SearchResult(title.take(120), url, snippet.take(240))
                }.toList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun wikipedia(query: String): List<SearchResult> {
        return try {
            val lang = if (query.any { it.code in 0x4E00..0x9FFF }) "zh" else "en"
            val json = fetchJson(
                "https://$lang.wikipedia.org/w/api.php?action=query&list=search&format=json&utf8=1&srlimit=5&srsearch=${
                    encode(query)
                }"
            )
            val arr = JSONObject(json).optJSONObject("query")?.optJSONArray("search") ?: JSONArray()
            (0 until arr.length()).mapNotNull { i ->
                val item = arr.optJSONObject(i) ?: return@mapNotNull null
                val title = item.optString("title")
                if (title.isBlank()) return@mapNotNull null
                SearchResult(
                    title,
                    "https://$lang.wikipedia.org/wiki/" + encode(title.replace(' ', '_')),
                    clean(item.optString("snippet")).take(240)
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun fetch(url: String): String {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = 12000
        connection.readTimeout = 16000
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

    private fun fetchJson(url: String): String = fetch(url)

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
