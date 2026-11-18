package com.spcode.tnpscprep.ui.auth

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Decides what she sees: the welcome flow (no account yet), the login screen (account with a PIN), or the app.
 * The app itself is not even composed while locked, so nothing of hers is loaded or visible behind the lock.
 */
@Composable
fun AuthGate(auth: AuthViewModel, content: @Composable () -> Unit) {
    val state by auth.state.collectAsStateWithLifecycle()
    val name by auth.userName.collectAsStateWithLifecycle()
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { auth.onStopped() }
    LifecycleEventEffect(Lifecycle.Event.ON_START) { auth.onStarted() }

    AnimatedContent(
        targetState = state,
        transitionSpec = {
            (fadeIn(tween(380, delayMillis = 60)) + scaleIn(spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow), initialScale = 0.96f)) togetherWith fadeOut(tween(180))
        },
        label = "auth",
    ) { s ->
        when (s) {
            AuthState.NEEDS_ACCOUNT -> {
                val ctx = androidx.compose.ui.platform.LocalContext.current
                WelcomeFlow(
                    onCreate = { n, exam, day, goal, pin -> auth.createAccount(n, exam, day, goal, pin) },
                    // returns null = not a Mathi backup; true = restored and she is in; false = restored but a name is still needed
                    onRestore = { uri ->
                        val repo = (ctx.applicationContext as com.spcode.tnpscprep.TnpscApp).container.backupRepository
                        repo.restoreFromUri(ctx.contentResolver, uri).getOrNull()?.let { auth.adoptRestoredAccount() }
                    },
                )
            }
            AuthState.LOCKED -> LoginScreen(
                name = name,
                lockedUntilMs = auth::lockedUntilMs,
                onSubmit = { pin -> auth.checkPin(pin).also { if (it is PinResult.Ok) auth.unlock() } },
                onErase = { auth.eraseAccount() },
            )
            AuthState.UNLOCKED -> content()
        }
    }
}
