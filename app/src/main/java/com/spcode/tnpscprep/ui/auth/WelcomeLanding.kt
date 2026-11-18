package com.spcode.tnpscprep.ui.auth

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Newspaper
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.spcode.tnpscprep.R
import com.spcode.tnpscprep.ui.design.PopIn
import com.spcode.tnpscprep.ui.settings.AppLanguage
import com.spcode.tnpscprep.ui.settings.AppLocale
import kotlinx.coroutines.launch

/**
 * First screen: the picture on top (with an English / Tamil switch), what the app gives her in three short rows, and the buttons
 * pinned at the bottom: "Get started", "I already have a backup" and the privacy line. Everything fits one screen: no empty bottom half.
 */
@Composable
fun WelcomeLanding(onStart: () -> Unit, onRestore: suspend (android.net.Uri) -> Boolean?, onNeedName: (String) -> Unit) {
    val tamil = LocalConfiguration.current.locales[0].language == "ta"
    val screenH = LocalConfiguration.current.screenHeightDp
    val scope = rememberCoroutineScope()
    val wctx = androidx.compose.ui.platform.LocalContext.current
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val bad = stringResource(R.string.welcome_restore_bad)
    val noName = stringResource(R.string.welcome_restore_noname)
    val pick = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) { busy = true; error = null; scope.launch {
            when (onRestore(uri)) { null -> error = bad; false -> { com.spcode.tnpscprep.ui.mood.MoodSounds.play(wctx, com.spcode.tnpscprep.domain.MoodSoundRules.RESTORED); onNeedName(noName) }; true -> com.spcode.tnpscprep.ui.mood.MoodSounds.play(wctx, com.spcode.tnpscprep.domain.MoodSoundRules.RESTORED) }
            busy = false
        } }
    }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).navigationBarsPadding()) {
        Box {
            AuthHero(
                if (tamil) listOf("login_vanakkam", "login_wave", "mood_proud") else listOf("login_wave", "login_vanakkam", "mood_proud"),
                height = (screenH * 0.40f).coerceIn(260f, 380f).dp,
            )
            // English / Tamil switch (the page is re-made in the chosen language at once)
            Row(Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                LangPill("English", selected = !tamil) { if (tamil) AppLocale.apply(AppLanguage.ENGLISH) }
                LangPill("தமிழ்", selected = tamil) { if (!tamil) AppLocale.apply(AppLanguage.TAMIL) }
            }
        }
        Column(Modifier.weight(1f).padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(18.dp))
            Text(stringResource(R.string.welcome_title), style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold), textAlign = TextAlign.Center)
            Spacer(Modifier.height(6.dp))
            Text(stringResource(R.string.welcome_sub), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            Spacer(Modifier.height(18.dp))
            PopIn(1) { Feature(Icons.Rounded.Replay, R.string.welcome_f1_title, R.string.welcome_f1_body) }
            Spacer(Modifier.height(12.dp))
            PopIn(2) { Feature(Icons.Rounded.Newspaper, R.string.welcome_f2_title, R.string.welcome_f2_body) }
            Spacer(Modifier.height(12.dp))
            PopIn(3) { Feature(Icons.Rounded.EditNote, R.string.welcome_f3_title, R.string.welcome_f3_body) }
            Spacer(Modifier.weight(1f))
        }
        Column(Modifier.padding(horizontal = 24.dp).padding(bottom = 14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Button(onClick = onStart, enabled = !busy, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                Text(stringResource(R.string.get_started), style = MaterialTheme.typography.titleMedium)
            }
            TextButton(onClick = { error = null; pick.launch(arrayOf("*/*")) }, enabled = !busy) {
                Text(stringResource(if (busy) R.string.welcome_restoring else R.string.welcome_restore))
            }
            error?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center) }
            Column(Modifier.padding(top = 4.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Icon(Icons.Rounded.Lock, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(stringResource(R.string.welcome_private), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
private fun LangPill(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.clip(RoundedCornerShape(50)).background(if (selected) Color.White else Color.White.copy(alpha = 0.18f)).clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp),
    ) { Text(label, style = MaterialTheme.typography.labelLarge, color = if (selected) Color(0xFF7A0019) else Color.White) }
}

@Composable
private fun Feature(icon: ImageVector, title: Int, body: Int) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(Modifier.size(42.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
            Icon(icon, null, Modifier.size(22.dp), tint = MaterialTheme.colorScheme.primary)
        }
        Column(Modifier.weight(1f)) {
            Text(stringResource(title), style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
            Text(stringResource(body), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
