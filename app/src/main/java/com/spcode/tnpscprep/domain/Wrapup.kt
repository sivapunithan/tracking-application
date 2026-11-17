package com.spcode.tnpscprep.domain

import java.time.LocalDate
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** The parts of an Evening Wrap-up. Stored as JSON in `diary_entry.wrapup`; the diary `text` is rendered from it (so search works). */
@Serializable
data class WrapupContent(
    val v: Int = 1,
    val well: String = "",
    val hard: String = "",
    /** 1..5, optional (0 = not chosen). */
    val intensity: Int = 0,
    val tomorrow: List<String> = emptyList(),
)

/** Headings are passed in (localised by the UI) so the pure code does not depend on Android resources. */
data class WrapupHeadings(val well: String, val hard: String, val tomorrow: String)

object Wrapup {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    fun encode(c: WrapupContent): String = json.encodeToString(WrapupContent.serializer(), c)
    fun decode(s: String?): WrapupContent? = if (s.isNullOrBlank()) null else runCatching { json.decodeFromString(WrapupContent.serializer(), s) }.getOrNull()

    /** The readable diary text: a section appears only when she wrote something in it. */
    fun render(c: WrapupContent, h: WrapupHeadings): String {
        val parts = ArrayList<String>()
        if (c.well.isNotBlank()) parts += h.well + "\n" + c.well.trim()
        if (c.hard.isNotBlank()) parts += h.hard + "\n" + c.hard.trim()
        val plan = c.tomorrow.map { it.trim() }.filter { it.isNotEmpty() }
        if (plan.isNotEmpty()) parts += h.tomorrow + "\n" + plan.mapIndexed { i, t -> "${i + 1}. $t" }.joinToString("\n")
        return parts.joinToString("\n\n")
    }

    fun isBlank(c: WrapupContent) = c.well.isBlank() && c.hard.isBlank() && c.tomorrow.none { it.isNotBlank() } && c.intensity == 0
}

/** A saved wrap-up row, as the saver sees it. */
data class WrapupRow(val id: Long, val date: LocalDate, val text: String, val mood: String?, val json: String)

/** Persistence the saver needs (a Room DAO implements it; a fake does in tests). */
interface WrapupStore {
    suspend fun find(date: LocalDate): WrapupRow?
    suspend fun insert(date: LocalDate, text: String, mood: String?, json: String, now: Long): Long
    suspend fun update(id: Long, text: String, mood: String?, json: String, now: Long)
}

object WrapupSaver {
    /** Saves today's wrap-up: re-opening and saving EDITS the same entry, so there is never a duplicate. Returns the entry id. */
    suspend fun save(store: WrapupStore, date: LocalDate, content: WrapupContent, mood: String?, headings: WrapupHeadings, now: Long): Long {
        val text = Wrapup.render(content, headings)
        val js = Wrapup.encode(content)
        val existing = store.find(date)
        return if (existing == null) store.insert(date, text, mood, js, now) else { store.update(existing.id, text, mood, js, now); existing.id }
    }
}

/** Unfinished wrap-up (autosaved while she goes through the steps): kept as JSON per day outside the diary, so a half-written wrap-up is never a diary entry. */
@Serializable
data class WrapupDraft(val date: String, val step: Int, val mood: String? = null, val content: WrapupContent = WrapupContent())

object WrapupDrafts {
    private val json = Json { ignoreUnknownKeys = true }
    fun encode(d: WrapupDraft): String = json.encodeToString(WrapupDraft.serializer(), d)
    /** A draft belongs to ONE day: a draft from another day is ignored (it would be stale). */
    fun decodeFor(day: LocalDate, s: String?): WrapupDraft? =
        if (s.isNullOrBlank()) null else runCatching { json.decodeFromString(WrapupDraft.serializer(), s) }.getOrNull()?.takeIf { it.date == day.toString() }
}

/** Reordering for the "tomorrow's top 3" list. */
object PlanOrder {
    fun <T> move(list: List<T>, from: Int, to: Int): List<T> {
        if (from !in list.indices || to !in list.indices || from == to) return list
        return list.toMutableList().also { val x = it.removeAt(from); it.add(to, x) }
    }
}
