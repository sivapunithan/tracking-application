package com.spcode.tnpscprep.ui.auth

import androidx.lifecycle.ViewModel
import com.spcode.tnpscprep.data.SettingsStore
import com.spcode.tnpscprep.domain.AccountForm
import com.spcode.tnpscprep.domain.PinHasher
import com.spcode.tnpscprep.domain.PinLockout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

enum class AuthState { NEEDS_ACCOUNT, LOCKED, UNLOCKED }

sealed interface PinResult {
    data object Ok : PinResult
    data class Wrong(val triesLeftBeforeWait: Int) : PinResult
    data class Locked(val untilMs: Long) : PinResult
}

/**
 * The LOCAL account: a name (+ optional 4-digit PIN) that lives only on this phone. No server, no password database.
 * Unlock state is in memory: a cold start asks for the PIN again, and so does coming back after [RELOCK_AFTER_MS] away.
 */
class AuthViewModel(
    private val settings: SettingsStore,
    private val eraseData: suspend () -> Unit,
    private val now: () -> Long = System::currentTimeMillis,
) : ViewModel() {
    private val _state = MutableStateFlow(initialState())
    val state: StateFlow<AuthState> = _state.asStateFlow()
    val userName: StateFlow<String> = settings.userName

    private var stoppedAt = 0L

    private fun initialState() = when {
        !settings.accountCreated.value -> AuthState.NEEDS_ACCOUNT
        settings.hasPin() -> AuthState.LOCKED
        else -> AuthState.UNLOCKED
    }

    fun hasPin(): Boolean = settings.hasPin()
    fun lockedUntilMs(): Long = settings.lockedUntilMs()

    suspend fun createAccount(name: String, exam: String, examEpochDay: Long?, goalMinutes: Int, pin: String?) {
        settings.setUserName(AccountForm.cleanName(name))
        settings.setTargetExam(exam)
        settings.setExamEpochDay(examEpochDay ?: SettingsStore.NO_DATE)
        settings.setDailyGoalMinutes(goalMinutes)
        if (pin != null) setPin(pin) else settings.clearPin()
        settings.markAccountCreated()
        _state.value = AuthState.UNLOCKED
    }

    /**
     * After a backup was restored on a fresh install: if the file carried her name the account exists already (no PIN: it is never in a backup),
     * so go straight in. Returns false when there was no name, and the Welcome flow then asks for it.
     */
    fun adoptRestoredAccount(): Boolean {
        if (!AccountForm.isValidName(settings.userName.value)) return false
        settings.clearPin(); settings.markAccountCreated(); _state.value = AuthState.UNLOCKED
        return true
    }

    /** Hashing is deliberately slow, so it never runs on the main thread. Counts wrong tries and enforces the wait. */
    suspend fun checkPin(pin: String): PinResult = withContext(Dispatchers.Default) {
        val t = now()
        val until = settings.lockedUntilMs()
        if (until > t) return@withContext PinResult.Locked(until)
        val record = settings.pinRecord() ?: return@withContext PinResult.Ok // no PIN set: nothing to check
        if (PinHasher.verify(pin, record.first, record.second)) {
            settings.recordSuccess()
            PinResult.Ok
        } else {
            val fails = settings.failedAttempts() + 1
            val wait = PinLockout.lockoutMs(fails)
            settings.recordFailure(if (wait > 0) t + wait else 0L, fails)
            if (wait > 0) PinResult.Locked(t + wait) else PinResult.Wrong(PinLockout.FREE_ATTEMPTS - fails)
        }
    }

    suspend fun setPin(pin: String) = withContext(Dispatchers.Default) {
        val salt = PinHasher.newSalt()
        settings.savePin(salt, PinHasher.hash(pin, salt))
    }

    fun removePin() = settings.clearPin()
    fun unlock() { _state.value = AuthState.UNLOCKED }
    fun lockNow() { if (settings.hasPin()) _state.value = AuthState.LOCKED }

    // Rotation / language change stop and restart the Activity within moments: only a real absence re-locks.
    fun onStopped() { stoppedAt = now() }
    fun onStarted() {
        if (_state.value == AuthState.UNLOCKED && settings.hasPin() && stoppedAt > 0 && now() - stoppedAt > RELOCK_AFTER_MS) {
            _state.value = AuthState.LOCKED
        }
        stoppedAt = 0
    }

    suspend fun eraseAccount() {
        eraseData()
        _state.value = AuthState.NEEDS_ACCOUNT
    }

    companion object { const val RELOCK_AFTER_MS = 5 * 60_000L }
}
