package com.spcode.tnpscprep.ui.affairs

import android.text.format.DateUtils
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.spcode.tnpscprep.R
import com.spcode.tnpscprep.domain.AffairCategory
import com.spcode.tnpscprep.ui.design.LocalBottomBarInset
import com.spcode.tnpscprep.ui.design.ScreenTitle

/** The Current affairs tab: our curated pack, or the auto-fetched headlines. */
@Composable
fun AffairsTab(affairsVm: AffairsViewModel, headlinesVm: HeadlinesViewModel) {
    var mode by rememberSaveable { mutableIntStateOf(0) }
    Column(Modifier.fillMaxSize()) {
        ScreenTitle(stringResource(R.string.tab_affairs), Modifier.padding(horizontal = 16.dp))
        Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = mode == 0, onClick = { mode = 0 }, label = { Text(stringResource(R.string.news_tab_curated)) })
            FilterChip(selected = mode == 1, onClick = { mode = 1 }, label = { Text(stringResource(R.string.news_tab_headlines)) })
        }
        if (mode == 0) AffairsScreen(affairsVm) else HeadlinesScreen(headlinesVm)
    }
}

@Composable
fun HeadlinesScreen(vm: HeadlinesViewModel) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val uriHandler = LocalUriHandler.current

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp + LocalBottomBarInset.current),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    stringResource(R.string.news_disclaimer),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (!ui.enabled) {
                    Text(stringResource(R.string.news_off_title), style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(R.string.news_off_body), style = MaterialTheme.typography.bodyMedium)
                    Button(onClick = { vm.setEnabled(true) }) { Text(stringResource(R.string.news_turn_on)) }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            if (ui.lastRefreshMs == 0L) stringResource(R.string.news_never)
                            else stringResource(
                                R.string.news_last_updated,
                                DateUtils.getRelativeTimeSpanString(ui.lastRefreshMs, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS).toString(),
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f),
                        )
                        OutlinedButton(onClick = { vm.refresh(true) }, enabled = !ui.refreshing) {
                            Text(stringResource(if (ui.refreshing) R.string.news_refreshing else R.string.news_refresh))
                        }
                    }
                    if (ui.failedFeeds > 0) {
                        Text(
                            stringResource(R.string.news_sources_failed, ui.failedFeeds, ui.totalFeeds),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        }
        if (ui.enabled) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(HeadlineRange.entries) { r ->
                            FilterChip(
                                selected = ui.filter.range == r,
                                onClick = { vm.setRange(r) },
                                label = {
                                    Text(
                                        stringResource(
                                            when (r) {
                                                HeadlineRange.WEEK -> R.string.range_7d
                                                HeadlineRange.MONTH -> R.string.range_30d
                                                HeadlineRange.ALL -> R.string.range_all
                                            },
                                        ),
                                    )
                                },
                            )
                        }
                    }
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            FilterChip(
                                selected = ui.filter.starredOnly,
                                onClick = vm::toggleStarredOnly,
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
            if (ui.items.isEmpty()) {
                item {
                    Text(
                        stringResource(R.string.news_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                items(ui.items, key = { it.id }) { a ->
                    AffairCard(
                        a,
                        onBookmark = { vm.toggleBookmark(a.id) },
                        onRevise = { vm.revise(a.id) },
                        onOpen = { runCatching { uriHandler.openUri(a.url) } },
                        onDelete = {},
                    )
                }
            }
        }
    }
}
