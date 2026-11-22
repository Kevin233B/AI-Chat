package com.kevin233.chat.data

data class SearchSnippet(
    val messageId: String,
    val role: String,
    val text: String
)

data class SearchHit(
    val conversation: Conversation,
    val matchCount: Int,
    val snippets: List<SearchSnippet>
)

object ChatSearch {

    fun search(conversations: List<Conversation>, rawQuery: String): List<SearchHit> {
        val query = rawQuery.trim()
        if (query.isEmpty()) {
            return conversations.sortedByDescending { it.updatedAt }.map {
                SearchHit(it, 0, emptyList())
            }
        }
        val lower = query.lowercase()
        val hits = mutableListOf<SearchHit>()
        for (conv in conversations) {
            var count = 0
            val snippets = mutableListOf<SearchSnippet>()
            if (conv.title.lowercase().contains(lower)) count++
            for (m in conv.messages) {
                val content = m.content
                val index = content.lowercase().indexOf(lower)
                if (index >= 0) {
                    count++
                    if (snippets.size < 3) {
                        snippets.add(SearchSnippet(m.id, m.role, snippet(content, query)))
                    }
                }
            }
            if (count > 0) {
                hits.add(SearchHit(conv, count, snippets))
            }
        }
        return hits.sortedWith(
            compareByDescending<SearchHit> { it.matchCount }
                .thenByDescending { it.conversation.updatedAt }
        )
    }

    fun snippet(text: String, query: String, radius: Int = 34): String {
        val flat = text.replace("\n", " ").replace(Regex("\\s+"), " ").trim()
        val lower = flat.lowercase()
        val index = lower.indexOf(query.lowercase())
        if (index < 0) return flat.take(80)
        val start = (index - radius).coerceAtLeast(0)
        val end = (index + query.length + radius).coerceAtMost(flat.length)
        val prefix = if (start > 0) "…" else ""
        val suffix = if (end < flat.length) "…" else ""
        return prefix + flat.substring(start, end) + suffix
    }
}
