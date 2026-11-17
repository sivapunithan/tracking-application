package com.spcode.tnpscprep.domain

/**
 * The exams a topic can belong to. Stored in Room as ONE int bitmask column on `topic`
 * (G1=1, G2=2, G4=4) so a topic shared by several exams is a single row; progress is keyed by
 * topic id, so it can never be duplicated per exam. Filter in SQL: `(exams & :mask) != 0`.
 */
enum class Exam(val code: String, val bit: Int) {
    G1("G1", 1), G2("G2", 2), G4("G4", 4);

    companion object {
        /** Matches every exam: the "All" filter. */
        val ALL_MASK: Int = entries.fold(0) { acc, e -> acc or e.bit }

        fun maskOf(codes: Collection<String>): Int = codes.fold(0) { acc, code ->
            val exam = entries.firstOrNull { it.code == code }
                ?: throw IllegalArgumentException("Unknown exam code '$code' (allowed: ${entries.joinToString { it.code }})")
            acc or exam.bit
        }
    }
}
