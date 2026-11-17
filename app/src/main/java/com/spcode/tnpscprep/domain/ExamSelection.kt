package com.spcode.tnpscprep.domain

/**
 * "My exams": the ONE global exam choice (several allowed). Stored as text in SettingsStore, as a bitmask in use.
 * Old installs stored a single value ("ALL", "G1", "G2", "G4"): those parse unchanged, so no conversion step is needed.
 * An empty or unreadable value means ALL, so the lists can never end up empty because of a setting.
 */
object ExamSelection {
    fun parse(stored: String?): Int {
        if (stored.isNullOrBlank()) return Exam.ALL_MASK
        var mask = 0
        for (part in stored.split(',')) {
            val code = part.trim()
            if (code == "ALL") return Exam.ALL_MASK
            Exam.entries.firstOrNull { it.code == code }?.let { mask = mask or it.bit }
        }
        return if (mask == 0) Exam.ALL_MASK else mask
    }

    fun format(mask: Int): String {
        val m = mask and Exam.ALL_MASK
        if (m == 0 || m == Exam.ALL_MASK) return "ALL"
        return Exam.entries.filter { m and it.bit != 0 }.joinToString(",") { it.code }
    }

    /** Flip one exam; unticking the last one falls back to all exams instead of an empty selection. */
    fun toggle(mask: Int, exam: Exam): Int {
        val next = (mask and Exam.ALL_MASK) xor exam.bit
        return if (next == 0) Exam.ALL_MASK else next
    }

    fun has(mask: Int, exam: Exam) = mask and exam.bit != 0
    fun codes(mask: Int): List<String> = Exam.entries.filter { mask and it.bit != 0 }.map { it.code }
}
