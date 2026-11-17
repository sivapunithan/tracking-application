package com.spcode.tnpscprep.ui.affairs

import com.spcode.tnpscprep.data.AffairRepository
import com.spcode.tnpscprep.data.db.AffairStateEntity
import com.spcode.tnpscprep.data.db.CurrentAffairEntity
import com.spcode.tnpscprep.data.db.UserAffairEntity
import com.spcode.tnpscprep.domain.AffairCategory
import java.time.LocalDate
import java.time.YearMonth

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
        )
    } + user.map { u ->
        val id = AffairRepository.USER_PREFIX + u.id
        val st = stateById[id]
        AffairUi(
            id, LocalDate.parse(u.date), AffairCategory.fromName(u.category) ?: AffairCategory.INDIA,
            u.title, u.title, u.summary, u.summary, "", "", verified = false,
            isUser = true, userId = u.id, bookmarked = st?.bookmarked ?: false, level = st?.level ?: 0,
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
