package com.spcode.tnpscprep.data.news

import com.spcode.tnpscprep.data.db.NewsItemEntity
import com.spcode.tnpscprep.domain.AffairCategory
import com.spcode.tnpscprep.domain.NewsRelevance
import java.net.URI
import java.security.MessageDigest
import java.time.Duration
import kotlinx.serialization.Serializable

@Serializable
data class FeedConfig(
    val id: String, val name: String, val url: String, val language: String = "en",
    val categoryHint: String = "INDIA", val minScore: Int = 2, val maxItems: Int = 60,
)

@Serializable
data class FeedListDto(val feeds: List<FeedConfig>)

/** Pure: raw parsed items -> the rows we are willing to store. No Android, no network, no database. */
object NewsPipeline {
    const val RETENTION_DAYS = 183L
    private const val MAX_TITLE = 220
    private val tracking = Regex("^(utm_.*|fbclid|gclid|ref|ref_src|cmpid|_ga)$", RegexOption.IGNORE_CASE)
    private val tags = Regex("<[^>]*>")
    private val spaces = Regex("\\s+")

    fun cleanTitle(raw: String): String = tags.replace(raw, " ").replace(spaces, " ").trim().take(MAX_TITLE)

    /** Strips tracking parameters and the #fragment so the same article from two feeds is stored once. */
    fun normalizeUrl(url: String): String? = try {
        val u = URI(url.trim())
        if (!u.scheme.equals("https", ignoreCase = true) || u.host.isNullOrBlank()) null
        else {
            val q = u.rawQuery?.split("&")?.filter { it.isNotEmpty() && !tracking.matches(it.substringBefore('=')) }?.joinToString("&")
            URI("https", u.host.lowercase(), u.rawPath, q?.ifEmpty { null }, null).toString()
        }
    } catch (_: Exception) { null }

    fun idFor(normalizedUrl: String): String =
        MessageDigest.getInstance("SHA-256").digest(normalizedUrl.toByteArray()).take(8).joinToString("") { "%02x".format(it) }

    fun toEntities(feed: FeedConfig, raw: List<RawFeedItem>, nowMs: Long): List<NewsItemEntity> {
        val hint = AffairCategory.fromName(feed.categoryHint) ?: AffairCategory.INDIA
        val cutoff = nowMs - Duration.ofDays(RETENTION_DAYS).toMillis()
        val seen = HashSet<String>()
        return raw.asSequence()
            .mapNotNull { r ->
                val url = normalizeUrl(r.link) ?: return@mapNotNull null
                val title = cleanTitle(r.title).ifEmpty { return@mapNotNull null }
                // Some feeds (e.g. government ones) have no dates: treat as "first seen now". Clamp dates from the future.
                val published = (r.published?.toEpochMilli() ?: nowMs).coerceAtMost(nowMs)
                if (published < cutoff) return@mapNotNull null
                if (NewsRelevance.score(title, hint) < feed.minScore) return@mapNotNull null
                NewsItemEntity(
                    id = idFor(url), feedId = feed.id, source = feed.name, title = title, url = url,
                    publishedAt = published, category = NewsRelevance.category(title, hint).name,
                    language = feed.language, fetchedAt = nowMs,
                )
            }
            .filter { seen.add(it.id) }
            .sortedByDescending { it.publishedAt }
            .take(feed.maxItems)
            .toList()
    }
}
