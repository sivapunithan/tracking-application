package com.spcode.tnpscprep.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.spcode.tnpscprep.R
import com.spcode.tnpscprep.ui.design.PopSheet
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Shown on launch when the account has a PIN. [onSubmit] returns the verdict; the gate unlocks on Ok. */
@Composable
fun LoginScreen(
    name: String,
    lockedUntilMs: () -> Long,
    onSubmit: suspend (String) -> PinResult,
    onErase: suspend () -> Unit,
) {
    var err by rememberSaveable { mutableIntStateOf(0) }
    var triesLeft by rememberSaveable { mutableIntStateOf(-1) }
    var lockedUntil by rememberSaveable { mutableLongStateOf(lockedUntilMs()) }
    var remaining by rememberSaveable { mutableLongStateOf(0L) }
    var busy by rememberSaveable { mutableStateOf(false) }
    var showForgot by rememberSaveable { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(lockedUntil) {
        while (System.currentTimeMillis() < lockedUntil) {
            remaining = lockedUntil - System.currentTimeMillis()
            delay(500)
        }
        remaining = 0
    }
    val locked = remaining > 0
    val message = when {
        locked -> stringResource(R.string.pin_locked, "%d:%02d".format(remaining / 60_000, (remaining / 1000) % 60 + if (remaining < 1000) 0 else 0))
        triesLeft >= 0 -> stringResource(R.string.pin_wrong, triesLeft)
        else -> null
    }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).verticalScroll(rememberScrollState()).navigationBarsPadding()) {
        AuthHero(listOf("mood_proud", "login_wave"), height = 280.dp)
        Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(stringResource(R.string.welcome_back), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(com.spcode.tnpscprep.ui.properName(name).ifBlank { stringResource(R.string.app_name) }, style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold), textAlign = TextAlign.Center)
            Spacer(Modifier.height(18.dp))
            PinInput(
                prompt = stringResource(R.string.pin_enter), message = message, errorTick = err, busy = busy, enabled = !locked,
                onComplete = { pin ->
                    scope.launch {
                        busy = true
                        when (val r = onSubmit(pin)) {
                            PinResult.Ok -> Unit
                            is PinResult.Wrong -> { triesLeft = r.triesLeftBeforeWait; err += 1 }
                            is PinResult.Locked -> { lockedUntil = r.untilMs; triesLeft = -1; err += 1 }
                        }
                        busy = false
                    }
                },
            )
            TextButton(onClick = { showForgot = true }) { Text(stringResource(R.string.forgot_pin)) }
        }
    }

    if (showForgot) {
        PopSheet(onDismiss = { showForgot = false }) { dismiss ->
            Text(stringResource(R.string.forgot_title), style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold))
            Spacer(Modifier.height(10.dp))
            Text(stringResource(R.string.forgot_body), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(20.dp))
            Button(onClick = dismiss, modifier = Modifier.fillMaxWidth().height(54.dp)) { Text(stringResource(R.string.keep_my_data)) }
            TextButton(onClick = { scope.launch { onErase() } }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.erase_start_over), color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
