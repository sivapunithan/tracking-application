package com.spcode.tnpscprep.ui.affairs

import com.spcode.tnpscprep.data.db.AffairStateEntity
import com.spcode.tnpscprep.data.db.CurrentAffairEntity
import com.spcode.tnpscprep.data.db.UserAffairEntity
import com.spcode.tnpscprep.domain.AffairCategory
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AffairListTest {
    private fun p(id: String, date: String, cat: String = "INDIA", title: String = id) =
        CurrentAffairEntity(id, date, cat, title, title, "s", "s", "src", "https://x.org", false)

    private val pack = listOf(p("a", "2026-03-10", "SPORTS"), p("b", "2026-05-02", "TN"), p("c", "2026-05-20", "TN"))
    private val user = listOf(UserAffairEntity(7, "2026-05-25", "POLITY", "my note", "mine", 0L))

    @Test fun newest_first_and_user_items_are_merged() {
        val r = buildAffairList(pack, user, emptyList(), AffairFilter())
        assertEquals(listOf("user-7", "c", "b", "a"), r.items.map { it.id })
        assertTrue(r.items.first().isUser)
        assertEquals(7L, r.items.first().userId)
    }

    @Test fun same_day_items_are_ordered_by_title() {
        val r = buildAffairList(listOf(p("z", "2026-01-01", title = "Zed"), p("y", "2026-01-01", title = "Alpha")), emptyList(), emptyList(), AffairFilter())
        assertEquals(listOf("y", "z"), r.items.map { it.id })
    }

    @Test fun month_and_category_filters_combine() {
        val may = YearMonth.of(2026, 5)
        assertEquals(listOf("user-7", "c", "b"), buildAffairList(pack, user, emptyList(), AffairFilter(month = may)).items.map { it.id })
        assertEquals(listOf("c", "b"), buildAffairList(pack, user, emptyList(), AffairFilter(month = may, category = AffairCategory.TN)).items.map { it.id })
    }

    @Test fun month_chips_list_every_month_even_while_filtering() {
        val r = buildAffairList(pack, user, emptyList(), AffairFilter(category = AffairCategory.SPORTS))
        assertEquals(listOf(YearMonth.of(2026, 5), YearMonth.of(2026, 3)), r.months)
        assertEquals(listOf("a"), r.items.map { it.id })
    }

    @Test fun bookmark_and_level_come_from_state_and_can_filter() {
        val states = listOf(AffairStateEntity("b", true, 2, 0L), AffairStateEntity("user-7", true, 0, 0L))
        val all = buildAffairList(pack, user, states, AffairFilter())
        assertEquals(2, all.items.first { it.id == "b" }.level)
        assertFalse(all.items.first { it.id == "c" }.bookmarked)
        assertEquals(listOf("user-7", "b"), buildAffairList(pack, user, states, AffairFilter(bookmarkedOnly = true)).items.map { it.id })
    }

    @Test fun unknown_category_text_falls_back_instead_of_crashing() {
        val r = buildAffairList(listOf(p("x", "2026-01-01", cat = "NOT_A_CATEGORY")), emptyList(), emptyList(), AffairFilter())
        assertEquals(AffairCategory.INDIA, r.items.single().category)
    }
}
