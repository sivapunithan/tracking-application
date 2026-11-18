package com.spcode.tnpscprep.data

import com.spcode.tnpscprep.domain.DiaryPhotos
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * The ".mathi" backup = ONE zip file: `backup.json` (the same text as the plain backup, written FIRST) + `photos/<12 hex>.webp`
 * (the display copy of each photo; thumbnails are rebuilt on restore). Plain JVM code so it is unit-tested.
 * Hostile-file rules: entry names are never used as paths (only a strictly matching photo name is accepted), every size is capped,
 * nothing else in the zip is read.
 */
object MathiBundle {
    const val JSON_ENTRY = "backup.json"
    private const val PHOTO_PREFIX = "photos/"
    const val MAX_PHOTO_BYTES = 8_000_000
    const val MAX_TOTAL_BYTES = 400_000_000L
    const val MAX_PHOTOS = 5_000

    /** A zip starts with "PK". */
    fun looksLikeZip(head: ByteArray) = head.size >= 2 && head[0] == 'P'.code.toByte() && head[1] == 'K'.code.toByte()

    fun entryName(photoName: String) = "$PHOTO_PREFIX$photoName.webp"

    /** The photo name in a zip entry name, or null if it is not exactly `photos/<12 hex>.webp`. */
    fun photoNameOf(entry: String): String? {
        if (!entry.startsWith(PHOTO_PREFIX) || !entry.endsWith(".webp")) return null
        val n = entry.substring(PHOTO_PREFIX.length, entry.length - 5)
        return n.takeIf { DiaryPhotos.isName(it) }
    }

    /** [open] gives the stored bytes of a photo (null = file missing: skipped). Returns how many photos were written. */
    fun write(out: OutputStream, json: String, names: List<String>, open: (String) -> InputStream?): Int {
        var n = 0
        ZipOutputStream(out).use { z ->
            z.putNextEntry(ZipEntry(JSON_ENTRY)); z.write(json.toByteArray(Charsets.UTF_8)); z.closeEntry()
            for (name in names.distinct()) {
                val ins = open(name) ?: continue
                z.putNextEntry(ZipEntry(entryName(name)))
                ins.use { it.copyTo(z) }
                z.closeEntry(); n++
            }
        }
        return n
    }

    /** Reads a bundle in order: [json] first, then [nextPhoto] until it returns null. */
    class Reader(input: InputStream) {
        private val z = ZipInputStream(input)
        private var total = 0L
        private var photos = 0

        /** The JSON text, or null if the first entry is not backup.json or it is too big. */
        fun json(maxChars: Int = AppBackup.MAX_FILE_CHARS): String? {
            val e = z.nextEntry ?: return null
            if (e.name != JSON_ENTRY || e.isDirectory) return null
            val bytes = readLimited(maxChars * 2 + 1) ?: return null
            total += bytes.size
            return String(bytes, Charsets.UTF_8)
        }

        /** The next valid photo (name + bytes). Entries with a bad name or an oversize body are skipped. Null at the end or when a limit is hit. */
        fun nextPhoto(): Pair<String, ByteArray>? {
            while (true) {
                val e = z.nextEntry ?: return null
                if (e.isDirectory) continue
                val name = photoNameOf(e.name) ?: continue
                if (photos >= MAX_PHOTOS) return null
                val bytes = readLimited(MAX_PHOTO_BYTES) ?: continue
                total += bytes.size
                if (total > MAX_TOTAL_BYTES) return null
                photos++
                return name to bytes
            }
        }

        /** Reads the current entry; null if it is longer than [max] (the rest is skipped by the next call to nextEntry). */
        private fun readLimited(max: Int): ByteArray? {
            val buf = java.io.ByteArrayOutputStream()
            val chunk = ByteArray(16 * 1024)
            while (true) {
                val r = z.read(chunk)
                if (r < 0) break
                if (buf.size() + r > max) return null
                buf.write(chunk, 0, r)
            }
            return buf.toByteArray()
        }
    }
}
