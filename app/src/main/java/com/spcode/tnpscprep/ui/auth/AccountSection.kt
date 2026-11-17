package com.spcode.tnpscprep.ui.auth

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.spcode.tnpscprep.R
import com.spcode.tnpscprep.ui.design.PopSheet
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Settings > Account: set / change / remove the PIN, lock now, or delete the account and all its data. */
@Composable
fun AccountSection(auth: AuthViewModel, showSecurity: Boolean = true, showErase: Boolean = true) {
    var sheet by rememberSaveable { mutableStateOf<String?>(null) }
    var hasPin by remember { mutableStateOf(auth.hasPin()) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (showSecurity) Text(stringResource(R.string.section_security), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
        if (!showSecurity) Unit else if (hasPin) {
            OutlinedButton(onClick = { sheet = "change" }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.change_pin)) }
            OutlinedButton(onClick = { sheet = "remove" }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.remove_pin)) }
            Button(onClick = { auth.lockNow() }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.lock_now)) }
        } else {
            OutlinedButton(onClick = { sheet = "set" }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.set_pin)) }
        }
        if (showErase) TextButton(onClick = { sheet = "delete" }, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.delete_account), color = MaterialTheme.colorScheme.error)
        }
    }

    sheet?.let { mode ->
        PopSheet(onDismiss = { sheet = null }) { dismiss ->
            if (mode == "delete") DeleteContent(auth, dismiss) else PinSheetContent(mode, auth, dismiss) { hasPin = auth.hasPin() }
        }
    }
}

@Composable
private fun SuccessTick() {
    val tick by animateFloatAsState(1f, spring(dampingRatio = 0.35f, stiffness = Spring.StiffnessMedium), label = "tick")
    Box(Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) {
        Icon(Icons.Rounded.CheckCircle, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(88.dp).graphicsLayer { scaleX = tick; scaleY = tick })
    }
}

/** mode: "set" (new -> confirm), "change" (current -> new -> confirm), "remove" (current). */
@Composable
private fun PinSheetContent(mode: String, auth: AuthViewModel, dismiss: () -> Unit, onChanged: () -> Unit) {
    var stage by remember { mutableStateOf(if (mode == "set") "new" else "current") }
    var first by remember { mutableStateOf("") }
    var err by remember { mutableIntStateOf(0) }
    var msg by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var done by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val mismatch = stringResource(R.string.pin_mismatch)
    val wrongTpl = stringResource(R.string.pin_wrong)
    val lockedMsg = stringResource(R.string.pin_locked, "")

    if (done) {
        SuccessTick()
        LaunchedEffect(Unit) { delay(800); dismiss() }
        return
    }
    Text(
        stringResource(when (mode) { "set" -> R.string.set_pin; "change" -> R.string.change_pin; else -> R.string.remove_pin }),
        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
    )
    Spacer(Modifier.height(16.dp))
    PinInput(
        prompt = stringResource(when (stage) { "current" -> R.string.pin_current; "new" -> R.string.pin_choose; else -> R.string.pin_confirm }),
        message = msg, errorTick = err, busy = busy,
        onComplete = { pin ->
            scope.launch {
                when (stage) {
                    "current" -> {
                        busy = true
                        when (val r = auth.checkPin(pin)) {
                            PinResult.Ok -> if (mode == "remove") { auth.removePin(); onChanged(); done = true } else { stage = "new"; msg = null }
                            is PinResult.Wrong -> { err += 1; msg = wrongTpl.replace("%1\$d", r.triesLeftBeforeWait.toString()) }
                            is PinResult.Locked -> { err += 1; msg = lockedMsg }
                        }
                        busy = false
                    }
                    "new" -> { first = pin; stage = "confirm"; msg = null }
                    else -> if (pin == first) { auth.setPin(pin); onChanged(); done = true } else { err += 1; msg = mismatch; stage = "new" }
                }
            }
        },
    )
    TextButton(onClick = dismiss, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.cancel)) }
}

@Composable
private fun DeleteContent(auth: AuthViewModel, dismiss: () -> Unit) {
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    Text(stringResource(R.string.delete_title), style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold))
    Spacer(Modifier.height(10.dp))
    Text(stringResource(R.string.delete_body), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Spacer(Modifier.height(20.dp))
    Button(onClick = dismiss, modifier = Modifier.fillMaxWidth().height(54.dp)) { Text(stringResource(R.string.keep_my_data)) }
    TextButton(
        onClick = { if (!busy) { busy = true; scope.launch { auth.eraseAccount() } } },
        modifier = Modifier.fillMaxWidth(),
    ) { Text(stringResource(R.string.delete_confirm), color = MaterialTheme.colorScheme.error) }
}
