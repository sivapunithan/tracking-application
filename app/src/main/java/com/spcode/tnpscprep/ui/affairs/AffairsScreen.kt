package com.spcode.tnpscprep.ui.affairs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.spcode.tnpscprep.R
import com.spcode.tnpscprep.domain.AffairCategory
import com.spcode.tnpscprep.domain.RevisionLevel
import com.spcode.tnpscprep.ui.localized
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.util.Locale

fun categoryLabelRes(c: AffairCategory): Int = when (c) {
    AffairCategory.POLITY -> R.string.cat_polity
    AffairCategory.TN -> R.string.cat_tn
    AffairCategory.INDIA -> R.string.cat_india
    AffairCategory.WORLD -> R.string.cat_world
    AffairCategory.ECONOMY -> R.string.cat_economy
    AffairCategory.SCIENCE -> R.string.cat_science
    AffairCategory.SPORTS -> R.string.cat_sports
    AffairCategory.AWARDS -> R.string.cat_awards
}

@Composable
fun AffairsScreen(vm: AffairsViewModel) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    var showAdd by rememberSaveable { mutableStateOf(false) }
    val uriHandler = LocalUriHandler.current

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ui.dataUpTo?.let {
                    Text(
                        stringResource(R.string.ca_compiled, it.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                Text(
                    stringResource(R.string.ca_disclaimer),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedButton(onClick = { showAdd = true }) { Text(stringResource(R.string.ca_add_own)) }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = ui.filter.month == null,
                            onClick = { vm.setMonth(null) },
                            label = { Text(stringResource(R.string.filter_all)) },
                        )
                    }
                    items(ui.list.months) { m ->
                        FilterChip(
                            selected = ui.filter.month == m,
                            onClick = { vm.setMonth(m) },
                            label = { Text(m.month.getDisplayName(TextStyle.SHORT, Locale.getDefault()) + " " + m.year.toString().takeLast(2)) },
                        )
                    }
                }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = ui.filter.bookmarkedOnly,
                            onClick = vm::toggleBookmarkedOnly,
                            label = { Text("★ " + stringResource(R.string.ca_bookmarked)) },
                        )
                    }
                    item {
                        FilterChip(
                            selected = ui.filter.category == null,
                            onClick = { vm.setCategory(null) },
                            label = { Text(stringResource(R.string.filter_all)) },
                        )
                    }
                    items(AffairCategory.entries) { c ->
                        FilterChip(
                            selected = ui.filter.category == c,
                            onClick = { vm.setCategory(c) },
                            label = { Text(stringResource(categoryLabelRes(c))) },
                        )
                    }
                }
            }
        }
        if (ui.list.items.isEmpty()) {
            item {
                Text(
                    stringResource(R.string.ca_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            items(ui.list.items, key = { it.id }) { a ->
                AffairCard(
                    a,
                    onBookmark = { vm.toggleBookmark(a.id) },
                    onRevise = { vm.revise(a.id) },
                    onOpen = { runCatching { uriHandler.openUri(a.url) } }, // no browser installed: ignore
                    onDelete = { a.userId?.let(vm::deleteOwn) },
                )
            }
        }
    }

    if (showAdd) {
        AddAffairDialog(
            onDismiss = { showAdd = false },
            onSave = { date, cat, title, summary -> vm.addOwn(date, cat, title, summary); showAdd = false },
        )
    }
}

@Composable
private fun AffairCard(a: AffairUi, onBookmark: () -> Unit, onRevise: () -> Unit, onOpen: () -> Unit, onDelete: () -> Unit) {
    val levelName = stringArrayResource(R.array.revision_levels)[RevisionLevel.clamp(a.level)]
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    a.date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)) + "  ·  " + stringResource(categoryLabelRes(a.category)),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                )
                val starLabel = stringResource(if (a.bookmarked) R.string.a11y_unstar else R.string.a11y_star)
                TextButton(
                    onClick = onBookmark, contentPadding = PaddingValues(0.dp),
                    modifier = Modifier.size(44.dp).semantics { contentDescription = starLabel },
                ) { Text(if (a.bookmarked) "★" else "☆", style = MaterialTheme.typography.titleLarge) }
            }
            Text(localized(a.titleEn, a.titleTa), style = MaterialTheme.typography.titleMedium)
            Text(localized(a.summaryEn, a.summaryTa), style = MaterialTheme.typography.bodyMedium)
            if (a.isUser) {
                Text(stringResource(R.string.ca_my_entry), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Text(
                    stringResource(if (a.verified) R.string.ca_verified else R.string.ca_unverified) + "  ·  " + a.sourceName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onRevise) { Text(stringResource(R.string.revised)) }
                Row(Modifier.clearAndSetSemantics {}, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    repeat(RevisionLevel.MAX) { i ->
                        Box(
                            Modifier.size(10.dp).clip(CircleShape).background(
                                if (i < a.level) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                            ),
                        )
                    }
                }
                Text(levelName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                if (a.url.isNotEmpty()) TextButton(onClick = onOpen) { Text(stringResource(R.string.ca_open_source)) }
                if (a.isUser) TextButton(onClick = onDelete) { Text(stringResource(R.string.ca_delete)) }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun AddAffairDialog(onDismiss: () -> Unit, onSave: (LocalDate, AffairCategory, String, String) -> Unit) {
    var title by rememberSaveable { mutableStateOf("") }
    var summary by rememberSaveable { mutableStateOf("") }
    var epochDay by rememberSaveable { mutableStateOf(LocalDate.now().toEpochDay()) }
    var category by rememberSaveable { mutableStateOf(AffairCategory.INDIA.name) }
    var picking by rememberSaveable { mutableStateOf(false) }
    val date = LocalDate.ofEpochDay(epochDay)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.ca_add_own)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { picking = true }) { Text(date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))) }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    AffairCategory.entries.forEach { c ->
                        FilterChip(selected = category == c.name, onClick = { category = c.name }, label = { Text(stringResource(categoryLabelRes(c))) })
                    }
                }
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text(stringResource(R.string.ca_title)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = summary, onValueChange = { summary = it }, label = { Text(stringResource(R.string.ca_summary)) }, minLines = 3, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(
                enabled = title.isNotBlank(),
                onClick = { onSave(date, AffairCategory.fromName(category) ?: AffairCategory.INDIA, title, summary) },
            ) { Text(stringResource(R.string.save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )

    if (picking) {
        val state = rememberDatePickerState(initialSelectedDateMillis = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
        DatePickerDialog(
            onDismissRequest = { picking = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { epochDay = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().toEpochDay() }
                    picking = false
                }) { Text(stringResource(R.string.ok)) }
            },
            dismissButton = { TextButton(onClick = { picking = false }) { Text(stringResource(R.string.cancel)) } },
        ) { DatePicker(state = state) }
    }
}
