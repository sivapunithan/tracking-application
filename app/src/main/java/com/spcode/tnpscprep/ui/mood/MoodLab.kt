package com.spcode.tnpscprep.ui.mood

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.spcode.tnpscprep.R
import com.spcode.tnpscprep.domain.Mood
import com.spcode.tnpscprep.ui.design.PopSheet

fun moodLabelRes(m: Mood): Int = when (m) {
    Mood.LAUGH -> R.string.mood_laugh
    Mood.CHEER -> R.string.mood_cheer
    Mood.PROUD -> R.string.mood_proud
    Mood.WORRIED -> R.string.mood_worried
    Mood.CRY -> R.string.mood_cry
    Mood.ANGRY -> R.string.mood_angry
    Mood.CELEBRATE -> R.string.mood_celebrate
}

/** Try every reaction. A mood whose own picture is not made yet shows its effect over the proud face and says so. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MoodLabSheet(onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    var picked by rememberSaveable { mutableStateOf(Mood.CRY.name) }
    val mood = Mood.entries.first { it.name == picked }
    PopSheet(onDismiss) { _ ->
        Text(stringResource(R.string.mood_lab_title), style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold))
        Spacer(Modifier.height(8.dp))
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            MoodAvatar(mood, size = 260.dp, previewWithFallback = true)
        }
        // Always the same height, so the chips below do not jump when the note appears or disappears.
        Box(Modifier.fillMaxWidth().height(40.dp)) {
            if (!hasMoodImage(ctx, mood)) {
                Text(stringResource(R.string.mood_picture_pending), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Mood.entries.forEach { m ->
                FilterChip(selected = m == mood, onClick = { picked = m.name }, label = { Text(stringResource(moodLabelRes(m))) })
            }
        }
    }
}
