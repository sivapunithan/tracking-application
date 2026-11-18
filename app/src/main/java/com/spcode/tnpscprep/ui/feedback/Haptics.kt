package com.spcode.tnpscprep.ui.feedback

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import com.spcode.tnpscprep.TnpscApp
import com.spcode.tnpscprep.domain.HapticKind
import com.spcode.tnpscprep.domain.HapticRules
import com.spcode.tnpscprep.domain.HapticStrength

/**
 * The ONE place the app vibrates. It obeys her "Vibration" switch and strength (Profile > Look and language) and does nothing on a phone
 * without a vibrator. Light by design: short pulses, never a long buzz.
 */
object Haptics {
    private fun vibrator(ctx: Context): Vibrator? =
        if (Build.VERSION.SDK_INT >= 31) (ctx.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
        else @Suppress("DEPRECATION") ctx.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator

    /** Vibrates once if her switch is on and the phone has a vibrator. [strengthOverride] is unused for now (kept for tests). */
    fun fire(context: Context, kind: HapticKind?, strengthOverride: HapticStrength? = null) {
        if (kind == null) return
        val ctx = context.applicationContext
        val settings = (ctx as TnpscApp).container.settingsStore
        if (!settings.vibration.value) return
        val v = vibrator(ctx)?.takeIf { it.hasVibrator() } ?: return
        val strength = strengthOverride ?: HapticStrength.of(settings.vibrationStrength.value)
        runCatching {
            val effect = if (v.hasAmplitudeControl()) HapticRules.scaled(kind, strength).let { VibrationEffect.createWaveform(it.timings, it.amps, -1) }
            else HapticRules.scaledByLength(kind, strength).let { VibrationEffect.createWaveform(it.timings, -1) }   // no amplitude control: strength = pulse length
            v.vibrate(effect)
        }
    }
}

/** Replaces Compose's own haptics: every existing `haptic.performHapticFeedback(...)` call now goes through [Haptics] (and her switch). */
private class AppHaptics(private val ctx: Context) : HapticFeedback {
    override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) {
        val kind = when (hapticFeedbackType) {
            HapticFeedbackType.LongPress -> HapticKind.STRONG
            HapticFeedbackType.TextHandleMove, HapticFeedbackType.SegmentTick, HapticFeedbackType.SegmentFrequentTick -> HapticKind.TICK
            HapticFeedbackType.Confirm, HapticFeedbackType.ToggleOn -> HapticKind.SUCCESS
            HapticFeedbackType.Reject -> HapticKind.ERROR
            else -> HapticKind.CLICK
        }
        Haptics.fire(ctx, kind)
    }
}

@Composable
fun rememberAppHaptics(): HapticFeedback { val c = LocalContext.current.applicationContext; return remember(c) { AppHaptics(c) } }
