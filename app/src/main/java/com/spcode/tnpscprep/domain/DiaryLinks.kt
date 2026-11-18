package com.spcode.tnpscprep.domain

/**
 * Hyperlinks inside a diary entry, stored in the plain text as `[label](https://address)`. Only http/https. Pure Kotlin.
 * The editor SHOWS just the label (styled); [display] gives the shown text plus the offset maps between the stored and the shown text.
 */
object DiaryLinks {
    private val RX = Regex("\\[([^\\]\\n\\[]{1,80})]\\((https?://[^)\\s]{1,500})\\)")

    /** [start, end) = the whole `[label](url)` in the stored text. */
    data class Link(val start: Int, val end: Int, val label: String, val url: String)

    fun find(text: String): List<Link> = RX.findAll(text).map { Link(it.range.first, it.range.last + 1, it.groupValues[1], it.groupValues[2]) }.toList()

    /** What she typed -> a safe address, or null. Adds https:// to "example.com"; refuses anything that is not http/https (javascript:, intent:, file: ...). */
    fun normalizeUrl(input: String): String? {
        val s = input.trim()
        if (s.isEmpty() || s.any { it.isWhitespace() }) return null
        val withScheme = when {
            s.startsWith("http://", true) || s.startsWith("https://", true) -> s
            s.contains("://") || s.startsWith("javascript:", true) || s.startsWith("intent:", true) || s.startsWith("file:", true) || s.startsWith("data:", true) || s.contains(":") && !s.substringBefore('/').contains('.') -> return null
            s.contains('.') -> "https://$s"
            else -> return null
        }
        val host = withScheme.substringAfter("://").substringBefore('/').substringBefore('?').substringBefore('#')
        if (host.isEmpty() || !host.contains('.') && !host.equals("localhost", true)) return null
        return withScheme.replace(")", "%29")
    }

    /** The stored form. Characters that would break the pattern are removed from the label. */
    fun make(label: String, url: String): String? {
        val l = label.replace('[', '(').replace(']', ')').replace("\n", " ").trim().take(80)
        val u = normalizeUrl(url) ?: return null
        return if (l.isEmpty()) null else "[$l]($u)"
    }

    /** A link in the SHOWN text: [tStart, tEnd) covers the label. */
    data class Shown(val tStart: Int, val tEnd: Int, val link: Link)

    class Display(val text: String, private val toShown: IntArray, private val toStored: IntArray, val links: List<Shown>) {
        fun shownOffset(stored: Int) = toShown[stored.coerceIn(0, toShown.size - 1)]
        fun storedOffset(shown: Int) = toStored[shown.coerceIn(0, toStored.size - 1)]
    }

    fun display(text: String): Display {
        val links = find(text)
        val sb = StringBuilder()
        val toShown = IntArray(text.length + 1)
        val toStored = ArrayList<Int>()      // index = shown offset
        val shown = ArrayList<Shown>()
        var i = 0
        var li = 0
        var endTaken = false                 // the shown offset right after a link already maps to "before ]" (so typing there extends the label)
        fun addStored(t: Int, stored: Int) { if (t == toStored.size) toStored.add(stored) }
        while (i < text.length) {
            val l = links.getOrNull(li)
            if (l != null && i == l.start) {
                val tStart = sb.length
                toShown[l.start] = tStart
                for (k in l.label.indices) {
                    toShown[l.start + 1 + k] = tStart + k
                    addStored(sb.length, if (k == 0) l.start else l.start + 1 + k)   // the cursor before the first label letter = before the "["
                    sb.append(l.label[k])
                }
                val tEnd = sb.length
                for (p in (l.start + 1 + l.label.length)..minOf(l.end, text.length)) toShown[p] = tEnd
                addStored(tEnd, l.start + 1 + l.label.length)
                endTaken = true
                shown += Shown(tStart, tEnd, l)
                i = l.end; li++
                continue
            }
            toShown[i] = sb.length
            if (endTaken && sb.length == toStored.size - 1) { /* this shown offset is already mapped to the label end */ } else addStored(sb.length, i)
            endTaken = false
            sb.append(text[i]); i++
        }
        toShown[text.length] = sb.length
        if (sb.length == toStored.size) toStored.add(text.length)
        return Display(sb.toString(), toShown, toStored.toIntArray(), shown)
    }

    /** Preview / word count: every link becomes just its label. */
    fun preview(text: String): String = RX.replace(text) { it.groupValues[1] }
}
