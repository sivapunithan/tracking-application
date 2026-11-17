package com.spcode.tnpscprep.domain

import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import kotlin.math.min

/** The app lock is a PRIVACY convenience (keeps casual snoopers out), not bank-grade security: a 4-digit PIN has only 10,000 combinations. */
object PinPolicy {
    const val LENGTH = 4
    fun isValid(pin: String): Boolean = pin.length == LENGTH && pin.all { it in '0'..'9' }
}

/** Salted PBKDF2: the PIN itself is never stored. */
object PinHasher {
    private const val ITERATIONS = 120_000
    private const val KEY_BITS = 256

    fun newSalt(): ByteArray = ByteArray(16).also { SecureRandom().nextBytes(it) }

    fun hash(pin: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(pin.toCharArray(), salt, ITERATIONS, KEY_BITS)
        return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
    }

    /** Constant-time comparison. */
    fun verify(pin: String, salt: ByteArray, expected: ByteArray): Boolean = MessageDigest.isEqual(hash(pin, salt), expected)
}

/** After 5 wrong PINs the app makes you wait, and the wait doubles each further miss (cap 15 minutes). */
object PinLockout {
    const val FREE_ATTEMPTS = 5
    fun lockoutMs(failedAttempts: Int): Long =
        if (failedAttempts < FREE_ATTEMPTS) 0L else min(30_000L shl (failedAttempts - FREE_ATTEMPTS).coerceAtMost(10), 15 * 60_000L)
}

object AccountForm {
    const val MAX_NAME = 30
    fun cleanName(raw: String): String = raw.trim().replace(Regex("\\s+"), " ").take(MAX_NAME)
    fun isValidName(raw: String): Boolean = cleanName(raw).isNotEmpty()
}
