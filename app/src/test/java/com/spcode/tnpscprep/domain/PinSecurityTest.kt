package com.spcode.tnpscprep.domain

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PinSecurityTest {
    @Test fun a_pin_is_exactly_four_digits() {
        assertTrue(PinPolicy.isValid("0420"))
        listOf("", "123", "12345", "12a4", "١٢٣٤", " 123", "12 4").forEach { assertFalse("'$it'", PinPolicy.isValid(it)) }
    }

    @Test fun hashing_is_deterministic_for_the_same_salt_and_differs_per_salt_or_pin() {
        val salt = PinHasher.newSalt()
        assertArrayEquals(PinHasher.hash("1234", salt), PinHasher.hash("1234", salt))
        assertFalse(PinHasher.hash("1234", salt).contentEquals(PinHasher.hash("1235", salt)))
        assertFalse(PinHasher.hash("1234", salt).contentEquals(PinHasher.hash("1234", PinHasher.newSalt())))
        assertEquals(32, PinHasher.hash("1234", salt).size)
    }

    @Test fun verify_accepts_the_right_pin_and_rejects_the_wrong_one() {
        val salt = PinHasher.newSalt(); val stored = PinHasher.hash("2468", salt)
        assertTrue(PinHasher.verify("2468", salt, stored))
        assertFalse(PinHasher.verify("2469", salt, stored))
        assertFalse(PinHasher.verify("0000", salt, stored))
    }

    @Test fun salts_are_random() = assertNotEquals(PinHasher.newSalt().toList(), PinHasher.newSalt().toList())

    @Test fun the_stored_value_never_contains_the_pin() {
        val h = PinHasher.hash("1234", PinHasher.newSalt())
        assertFalse(String(h, Charsets.ISO_8859_1).contains("1234"))
    }

    @Test fun lockout_starts_after_five_misses_and_doubles_up_to_a_cap() {
        assertEquals(listOf(0L, 0L, 0L, 0L), (0..3).map { PinLockout.lockoutMs(it) }.take(4))
        assertEquals(0L, PinLockout.lockoutMs(4))
        assertEquals(30_000L, PinLockout.lockoutMs(5))
        assertEquals(60_000L, PinLockout.lockoutMs(6))
        assertEquals(120_000L, PinLockout.lockoutMs(7))
        assertEquals(15 * 60_000L, PinLockout.lockoutMs(30))
    }

    @Test fun names_are_trimmed_collapsed_and_limited() {
        assertEquals("Anu Priya", AccountForm.cleanName("  Anu   Priya  "))
        assertEquals(30, AccountForm.cleanName("x".repeat(80)).length)
        assertFalse(AccountForm.isValidName("   "))
        assertTrue(AccountForm.isValidName("மாலதி"))
    }
}
