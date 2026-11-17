package com.spcode.tnpscprep.ui.wrapup

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.spcode.tnpscprep.R
import com.spcode.tnpscprep.domain.WrapupCardState
import com.spcode.tnpscprep.ui.design.SoftCard

/** Evening card on Today (and the same wording on the Journal button): invites her, or says it is done and opens it for editing. */
@Composable
fun WrapupCardView(state: WrapupCardState, onOpen: () -> Unit, lastSeven: Int = 0, tomorrow: List<String> = emptyList()) {
    if (state == WrapupCardState.HIDDEN) return
    val done = state == WrapupCardState.DONE
    SoftCard(Modifier.fillMaxWidth(), onClick = onOpen) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
                Icon(if (done) Icons.Rounded.CheckCircle else Icons.Rounded.NightsStay, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(if (done) R.string.wrap_card_done_title else R.string.wrap_title), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                Text(stringResource(if (done) R.string.wrap_card_done_body else R.string.wrap_card_invite_body), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (tomorrow.isNotEmpty()) Text(stringResource(R.string.wrap_card_tomorrow, tomorrow.joinToString(" · ")), style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                // forgiving: shown only when there is something to celebrate; never "0 of 7"
                if (lastSeven >= 1) Text(stringResource(R.string.wrap_consistency, lastSeven), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            }
            OutlinedButton(onClick = onOpen) { Text(stringResource(if (done) R.string.wrap_card_open else R.string.wrap_card_start)) }
        }
    }
}
