package com.spcode.tnpscprep.domain

/**
 * Photos inside a diary entry. The entry text stays plain text; a photo is the token `[[photo:<12 hex>]]` alone on its own line.
 * Pure Kotlin: the editor splits the text into blocks with [parse] and saves it back with [join].
 */
sealed interface DiaryBlock {
    data class Text(val text: String) : DiaryBlock
    data class Photo(val name: String) : DiaryBlock
}

object DiaryPhotos {
    const val MAX_PER_ENTRY = 10
    private val NAME = Regex("[0-9a-f]{12}")
    private val LINE = Regex("\\[\\[photo:([0-9a-f]{12})]]")

    fun token(name: String) = "[[photo:$name]]"
    fun newName(random: java.util.Random = java.security.SecureRandom()): String =
        buildString { repeat(12) { append("0123456789abcdef"[random.nextInt(16)]) } }
    fun isName(s: String) = NAME.matches(s)

    /** Splits [text] into text blocks and photo blocks (a photo = a line that is exactly the token). join(parse(t)) == t. */
    fun parse(text: String): List<DiaryBlock> {
        val out = ArrayList<DiaryBlock>()
        val buf = ArrayList<String>()
        fun flush() { if (buf.isNotEmpty()) { out += DiaryBlock.Text(buf.joinToString("\n")); buf.clear() } }
        for (line in text.split("\n")) {
            val m = LINE.matchEntire(line)
            if (m != null) { flush(); out += DiaryBlock.Photo(m.groupValues[1]) } else buf += line
        }
        flush()
        if (out.isEmpty()) out += DiaryBlock.Text("")
        return out
    }

    fun join(blocks: List<DiaryBlock>): String =
        blocks.joinToString("\n") { when (it) { is DiaryBlock.Text -> it.text; is DiaryBlock.Photo -> token(it.name) } }

    fun names(text: String): List<String> = parse(text).filterIsInstance<DiaryBlock.Photo>().map { it.name }.distinct()

    /** Timeline/search preview: every token becomes [label] (e.g. "📷"), so raw tokens are never shown. */
    fun preview(text: String, label: String): String = LINE.replace(text, label)

    /** Count of letters only (a photo token is not "written" text): used so a photo-only entry still counts as non-blank. */
    fun hasWords(text: String): Boolean = LINE.replace(text, "").isNotBlank()
}

/** Size maths for storing photos small. Pure. */
object PhotoSizing {
    const val MAX_FULL = 1600
    const val MAX_THUMB = 320

    /** The size after shrinking so the long edge is at most [max]; never enlarges. */
    fun fit(w: Int, h: Int, max: Int): Pair<Int, Int> {
        if (w <= 0 || h <= 0) return 1 to 1
        val long = maxOf(w, h)
        if (long <= max) return w to h
        val f = max.toDouble() / long
        return maxOf(1, Math.round(w * f).toInt()) to maxOf(1, Math.round(h * f).toInt())
    }

    /** Power-of-two BitmapFactory sample size so the decoded bitmap is still >= [max] on the long edge (decode small, then scale exactly). */
    fun sampleSize(w: Int, h: Int, max: Int): Int {
        var s = 1
        val long = maxOf(w, h)
        while (long / (s * 2) >= max) s *= 2
        return s
    }
}
