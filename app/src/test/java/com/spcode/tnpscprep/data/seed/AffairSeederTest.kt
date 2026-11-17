package com.spcode.tnpscprep.data.seed

import java.io.File
import java.time.LocalDate
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

private class FakeAffairStore(var version: Int = 0) : AffairStore {
    var data: AffairData? = null
    var calls = 0
    override suspend fun storedVersion() = version
    override suspend fun replace(data: AffairData) { calls++; version = data.version; this.data = data }
}

class AffairSeederTest {
    private fun item(id: String, date: String = "2026-05-01", category: String = "INDIA", url: String = "https://example.org/a") =
        """{"id":"$id","date":"$date","category":"$category","titleEn":"T","titleTa":"த","summaryEn":"S","summaryTa":"ச","sourceName":"src","url":"$url"}"""

    private fun pack(version: Int, vararg items: String, upTo: String = "2026-11-17") =
        """{"version":$version,"dataUpTo":"$upTo","items":[${items.joinToString(",")}]}"""

    private fun rejects(json: String, store: FakeAffairStore = FakeAffairStore()) = runBlocking {
        try { AffairSeeder(store).seedIfNeeded(json); fail("expected IllegalArgumentException") } catch (_: IllegalArgumentException) { }
        assertEquals("nothing may be written when validation fails", 0, store.calls)
    }

    @Test fun first_launch_seeds_and_records_data_up_to() = runBlocking {
        val store = FakeAffairStore()
        assertEquals(SeedResult.SEEDED, AffairSeeder(store).seedIfNeeded(pack(1, item("a"), item("b"))))
        assertEquals(2, store.data!!.items.size)
        assertEquals(LocalDate.of(2026, 11, 17), store.data!!.dataUpTo)
    }

    @Test fun same_or_older_version_is_a_no_op() = runBlocking {
        val store = FakeAffairStore(version = 2)
        assertEquals(SeedResult.UP_TO_DATE, AffairSeeder(store).seedIfNeeded(pack(2, item("a"))))
        assertEquals(SeedResult.UP_TO_DATE, AffairSeeder(store).seedIfNeeded(pack(1, item("a"))))
        assertEquals(0, store.calls)
    }

    @Test fun higher_version_replaces_the_pack() = runBlocking {
        val store = FakeAffairStore()
        val seeder = AffairSeeder(store)
        seeder.seedIfNeeded(pack(1, item("a"), item("b")))
        seeder.seedIfNeeded(pack(2, item("b"), item("c")))
        assertEquals(listOf("b", "c"), store.data!!.items.map { it.id })
    }

    @Test fun unknown_category_is_rejected() = rejects(pack(1, item("a", category = "SPORT")))
    @Test fun duplicate_ids_are_rejected() = rejects(pack(1, item("a"), item("a")))
    @Test fun ids_starting_with_user_are_reserved() = rejects(pack(1, item("user-1")))
    @Test fun item_dated_after_data_up_to_is_rejected() = rejects(pack(1, item("a", date = "2026-12-01")))
    @Test fun bad_date_is_rejected() = rejects(pack(1, item("a", date = "17/11/2026")))
    @Test fun non_https_url_is_rejected() = rejects(pack(1, item("a", url = "http://example.org")))

    /** Guards the real asset, and any future pack someone drops in. */
    @Test fun bundled_pack_is_valid_and_reasonably_complete() = runBlocking {
        val text = File("src/main/assets/current_affairs.json").readText()
        val store = FakeAffairStore()
        assertEquals(SeedResult.SEEDED, AffairSeeder(store).seedIfNeeded(text))
        val items = store.data!!.items
        assertTrue(items.size >= 60)
        assertTrue("covers at least 6 months", items.map { it.date.take(7) }.distinct().size >= 6)
        assertEquals("every category is used", com.spcode.tnpscprep.domain.AffairCategory.entries.map { it.name }.toSet(), items.map { it.category }.toSet())
        assertTrue(items.all { it.titleTa.isNotBlank() && it.summaryTa.isNotBlank() })
        assertTrue("every item links to a source", items.all { it.url.startsWith("https://") })
        assertTrue("nothing claims to be verified without a human check", items.none { it.verified })
    }
}
