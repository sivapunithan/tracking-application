package com.spcode.tnpscprep.data.seed

import com.spcode.tnpscprep.data.db.CurrentAffairEntity
import com.spcode.tnpscprep.domain.AffairCategory
import java.time.LocalDate
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Mirrors assets/current_affairs.json. */
@Serializable
data class AffairPackDto(
    val version: Int,
    val dataUpTo: String,
    @SerialName("_notice") val notice: String = "",
    val items: List<AffairItemDto>,
)

@Serializable
data class AffairItemDto(
    val id: String, val date: String, val category: String,
    val titleEn: String, val titleTa: String, val summaryEn: String, val summaryTa: String,
    val sourceName: String = "", val url: String = "", val verified: Boolean = false,
)

data class AffairData(val version: Int, val dataUpTo: LocalDate, val items: List<CurrentAffairEntity>)

/** Same idea as SyllabusStore: the seeder can only touch pack items, never her bookmarks or own entries. */
interface AffairStore {
    suspend fun storedVersion(): Int
    suspend fun replace(data: AffairData)
}

class AffairSeeder(
    private val store: AffairStore,
    private val json: Json = Json { ignoreUnknownKeys = true },
) {
    /** Validate first (a bad pack fails before touching the DB), then apply only if the version is newer. */
    suspend fun seedIfNeeded(jsonText: String): SeedResult {
        val data = toData(json.decodeFromString<AffairPackDto>(jsonText))
        if (data.version <= store.storedVersion()) return SeedResult.UP_TO_DATE
        store.replace(data)
        return SeedResult.SEEDED
    }

    companion object {
        /** A clear message that names the offending field, so a pack editor knows what to fix. */
        private fun parseDate(what: String, text: String): LocalDate =
            try { LocalDate.parse(text) } catch (e: java.time.format.DateTimeParseException) {
                throw IllegalArgumentException("$what: '$text' is not an ISO date (yyyy-MM-dd)", e)
            }

        fun toData(dto: AffairPackDto): AffairData {
            require(dto.version >= 1) { "pack version must be >= 1" }
            val upTo = parseDate("dataUpTo", dto.dataUpTo)
            val ids = HashSet<String>()
            val items = dto.items.map { i ->
                require(i.id.isNotBlank() && !i.id.startsWith("user-")) { "bad id '${i.id}' (must be non-blank, not 'user-...')" }
                require(ids.add(i.id)) { "duplicate item id '${i.id}'" }
                val date = parseDate("item '${i.id}' date", i.date)
                require(!date.isAfter(upTo)) { "item '${i.id}' is dated after dataUpTo" }
                require(AffairCategory.fromName(i.category) != null) { "item '${i.id}': unknown category '${i.category}'" }
                require(i.titleEn.isNotBlank() && i.summaryEn.isNotBlank()) { "item '${i.id}': English title/summary required" }
                require(i.url.isEmpty() || i.url.startsWith("https://")) { "item '${i.id}': url must be https" }
                CurrentAffairEntity(i.id, i.date, i.category, i.titleEn, i.titleTa, i.summaryEn, i.summaryTa, i.sourceName, i.url, i.verified)
            }
            return AffairData(dto.version, upTo, items)
        }
    }
}
