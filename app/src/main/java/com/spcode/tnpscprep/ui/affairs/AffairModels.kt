package com.spcode.tnpscprep.ui.affairs

import com.spcode.tnpscprep.data.AffairRepository
import com.spcode.tnpscprep.data.db.AffairStateEntity
import com.spcode.tnpscprep.data.db.CurrentAffairEntity
import com.spcode.tnpscprep.data.db.NewsItemEntity
import com.spcode.tnpscprep.data.db.UserAffairEntity
import com.spcode.tnpscprep.domain.AffairCategory
import java.time.LocalDate
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId

data class AffairUi(
    val id: String,
    val date: LocalDate,
    val category: AffairCategory,
    val titleEn: String, val titleTa: String,
    val summaryEn: String, val summaryTa: String,
    val sourceName: String,
    val url: String,
    val verified: Boolean,
    val isUser: Boolean,
    val userId: Long?,
    val bookmarked: Boolean,
    val level: Int,
    val lastUpdated: Long = 0L,
    /** Auto-fetched headline (not one of ours, not verified). */
    val isNews: Boolean = false,
)

data class AffairFilter(
    val month: YearMonth? = null,
    val category: AffairCategory? = null,
    val bookmarkedOnly: Boolean = false,
)

data class AffairListUi(val items: List<AffairUi>, val months: List<YearMonth>)

/**
 * Pure function (no Android). Merges shipped items with her own entries and her bookmark/revision state,
 * newest first. [months] lists every month that has ANY item (so the month chips don't vanish while filtering).
 */
fun buildAffairList(
    pack: List<CurrentAffairEntity>,
    user: List<UserAffairEntity>,
    states: List<AffairStateEntity>,
    filter: AffairFilter,
): AffairListUi {
    val stateById = states.associateBy { it.itemId }
    val all = pack.map { p ->
        val st = stateById[p.id]
        AffairUi(
            p.id, LocalDate.parse(p.date), AffairCategory.fromName(p.category) ?: AffairCategory.INDIA,
            p.titleEn, p.titleTa, p.summaryEn, p.summaryTa, p.sourceName, p.url, p.verified,
            isUser = false, userId = null, bookmarked = st?.bookmarked ?: false, level = st?.level ?: 0,
            lastUpdated = st?.lastUpdated ?: 0L,
        )
    } + user.map { u ->
        val id = AffairRepository.USER_PREFIX + u.id
        val st = stateById[id]
        AffairUi(
            id, LocalDate.parse(u.date), AffairCategory.fromName(u.category) ?: AffairCategory.INDIA,
            u.title, u.title, u.summary, u.summary, "", "", verified = false,
            isUser = true, userId = u.id, bookmarked = st?.bookmarked ?: false, level = st?.level ?: 0,
            lastUpdated = st?.lastUpdated ?: 0L,
        )
    }
    val months = all.map { YearMonth.from(it.date) }.distinct().sortedDescending()
    val shown = all.filter { a ->
        (filter.month == null || YearMonth.from(a.date) == filter.month) &&
            (filter.category == null || a.category == filter.category) &&
            (!filter.bookmarkedOnly || a.bookmarked)
    }.sortedWith(compareByDescending<AffairUi> { it.date }.thenBy { it.titleEn })
    return AffairListUi(shown, months)
}

/** A fetched headline shown with the same card (and the same star / revised state) as curated items. */
fun newsToUi(n: NewsItemEntity, st: AffairStateEntity?, zone: ZoneId): AffairUi = AffairUi(
    id = "news-" + n.id,
    date = Instant.ofEpochMilli(n.publishedAt).atZone(zone).toLocalDate(),
    category = AffairCategory.fromName(n.category) ?: AffairCategory.INDIA,
    titleEn = n.title, titleTa = n.title, summaryEn = "", summaryTa = "",
    sourceName = n.source, url = n.url, verified = false, isUser = false, userId = null,
    bookmarked = st?.bookmarked ?: false, level = st?.level ?: 0, lastUpdated = st?.lastUpdated ?: 0L, isNews = true,
)

enum class HeadlineRange(val days: Int?) { WEEK(7), MONTH(30), ALL(null) }

data class HeadlineFilter(
    val range: HeadlineRange = HeadlineRange.WEEK,
    val category: AffairCategory? = null,
    val starredOnly: Boolean = false,
)

/** Pure: the date range is applied by the database query; category and starred are applied here. Newest first. */
fun buildHeadlineList(news: List<NewsItemEntity>, states: List<AffairStateEntity>, filter: HeadlineFilter, zone: ZoneId): List<AffairUi> {
    val stateById = states.associateBy { it.itemId }
    return news.map { newsToUi(it, stateById["news-" + it.id], zone) }
        .filter { (filter.category == null || it.category == filter.category) && (!filter.starredOnly || it.bookmarked) }
        .sortedByDescending { it.date }
}
