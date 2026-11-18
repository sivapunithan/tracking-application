package com.spcode.tnpscprep.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HapticRulesTest {
    @Test fun everyPatternIsWellFormedAndShort() {
        HapticKind.entries.forEach { k ->
            val p = HapticRules.base(k)
            assertEquals(p.timings.size, p.amps.size)
            assertTrue("$k total", p.timings.sum() <= 300)                 // never a long buzz
            assertTrue(p.amps.all { it in 0..255 })
        }
    }
    @Test fun strengthScalesAmplitudeNotTiming() {
        val full = HapticRules.scaled(HapticKind.SUCCESS, HapticStrength.STRONG); val light = HapticRules.scaled(HapticKind.SUCCESS, HapticStrength.LIGHT)
        assertEquals(full.timings.toList(), light.timings.toList())
        assertTrue(light.amps.zip(full.amps).all { (l, f) -> if (f == 0) l == 0 else l in 1 until f })
    }
    @Test fun withoutAmplitudeControlStrengthChangesLength() {
        val l = HapticRules.scaledByLength(HapticKind.CLICK, HapticStrength.LIGHT); val n = HapticRules.scaledByLength(HapticKind.CLICK, HapticStrength.NORMAL); val st = HapticRules.scaledByLength(HapticKind.CLICK, HapticStrength.STRONG)
        assertTrue(l.timings.sum() < n.timings.sum() && n.timings.sum() < st.timings.sum()); assertTrue(st.timings.sum() <= 300)
        HapticKind.entries.forEach { assertTrue(HapticRules.scaledByLength(it, HapticStrength.STRONG).timings.sum() <= 300) }
    }
    @Test fun clipsMapToABuzz() {
        assertEquals(HapticKind.SUCCESS, HapticRules.forClip("celebrate")); assertEquals(HapticKind.ERROR, HapticRules.forClip("slap"))
        assertEquals(HapticKind.SOFT_BUZZ, HapticRules.forClip("fail")); assertNull(HapticRules.forClip("nope"))
        listOf("cry", "angry", "worried", "laugh", "cheer", "proud", "celebrate", "boom", "photo", "slap", "fail", "later").forEach { assertTrue(it, HapticRules.forClip(it) != null) }
    }
    @Test fun strengthIndexFallsBackToNormal() { assertEquals(HapticStrength.NORMAL, HapticStrength.of(9)); assertEquals(HapticStrength.LIGHT, HapticStrength.of(0)) }
}
