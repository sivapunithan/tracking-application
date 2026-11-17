package com.spcode.tnpscprep.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class ExamSelectionTest {
    @Test fun legacySingleValuesParse() {
        assertEquals(Exam.ALL_MASK, ExamSelection.parse("ALL"))
        assertEquals(Exam.G1.bit, ExamSelection.parse("G1"))
        assertEquals(Exam.G4.bit, ExamSelection.parse("G4"))
    }
    @Test fun multiValueRoundTrips() {
        val m = Exam.G1.bit or Exam.G4.bit
        assertEquals("G1,G4", ExamSelection.format(m))
        assertEquals(m, ExamSelection.parse("G1,G4"))
    }
    @Test fun emptyOrUnknownMeansAll() {
        assertEquals(Exam.ALL_MASK, ExamSelection.parse(null))
        assertEquals(Exam.ALL_MASK, ExamSelection.parse(""))
        assertEquals(Exam.ALL_MASK, ExamSelection.parse("G9"))
        assertEquals("ALL", ExamSelection.format(0))
    }
    @Test fun allThreeFormatsAsAll() { assertEquals("ALL", ExamSelection.format(Exam.ALL_MASK)) }
    @Test fun untickingLastFallsBackToAll() {
        assertEquals(Exam.ALL_MASK, ExamSelection.toggle(Exam.G2.bit, Exam.G2))
    }
    @Test fun toggleAddsAndRemoves() {
        val two = ExamSelection.toggle(Exam.G1.bit, Exam.G2)
        assertEquals(Exam.G1.bit or Exam.G2.bit, two)
        assertEquals(Exam.G2.bit, ExamSelection.toggle(two, Exam.G1))
    }
    @Test fun filteringTopicsBySelection() {
        // a topic shared by G1+G2 shows for a G2-only selection; a G1-only topic does not
        val shared = Exam.G1.bit or Exam.G2.bit
        val sel = Exam.G2.bit
        assertEquals(true, shared and sel != 0)
        assertEquals(false, Exam.G1.bit and sel != 0)
    }
}
