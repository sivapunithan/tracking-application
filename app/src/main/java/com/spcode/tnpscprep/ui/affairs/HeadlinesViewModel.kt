package com.spcode.tnpscprep.ui.affairs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spcode.tnpscprep.data.AffairRepository
import com.spcode.tnpscprep.data.NewsRepository
import com.spcode.tnpscprep.data.SettingsStore
import com.spcode.tnpscprep.domain.AffairCategory
import java.time.ZoneId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HeadlinesUiState(
    val items: List<AffairUi> = emptyList(),
    val filter: HeadlineFilter = HeadlineFilter(),
    val enabled: Boolean = true,
    val lastRefreshMs: Long = 0L,
    val refreshing: Boolean = false,
    val failedFeeds: Int = 0,
    val totalFeeds: Int = 0,
)

@OptIn(ExperimentalCoroutinesApi::class)
class HeadlinesViewModel(
    private val news: NewsRepository,
    private val affairs: AffairRepository,
    private val settings: SettingsStore,
    private val zone: ZoneId = ZoneId.systemDefault(),
) : ViewModel() {
    private val _filter = MutableStateFlow(HeadlineFilter())

    /** Small flows for the Settings switch (so Settings doesn't recompose with the whole list). */
    val enabled: StateFlow<Boolean> = settings.newsEnabled
    val lastRefreshMs: StateFlow<Long> = settings.lastNewsRefreshMs

    private val list = combine(
        _filter.flatMapLatest { news.observeSince(it.range.days) }, affairs.observeStates(), _filter,
    ) { items, states, f -> buildHeadlineList(items, states, f, zone) }

    private val network = combine(settings.newsEnabled, settings.lastNewsRefreshMs, news.refreshing, news.lastResults) { on, last, busy, res ->
        arrayOf<Any>(on, last, busy, res.count { it.error != null }, res.size)
    }

    val ui: StateFlow<HeadlinesUiState> = combine(list, _filter, network) { items, f, n ->
        HeadlinesUiState(items, f, n[0] as Boolean, n[1] as Long, n[2] as Boolean, n[3] as Int, n[4] as Int)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HeadlinesUiState())

    init { refresh(force = false) } // quietly updates when she opens the tab (skipped if fetched < 6 h ago or switched off)

    fun refresh(force: Boolean = true) { viewModelScope.launch { news.refresh(force) } }

    fun setEnabled(on: Boolean) {
        settings.setNewsEnabled(on)
        if (on) refresh(force = true)
    }

    fun setRange(r: HeadlineRange) { _filter.value = _filter.value.copy(range = r) }
    fun setCategory(c: AffairCategory?) { _filter.value = _filter.value.copy(category = c) }
    fun toggleStarredOnly() { _filter.value = _filter.value.copy(starredOnly = !_filter.value.starredOnly) }

    fun toggleBookmark(id: String) { viewModelScope.launch { affairs.toggleBookmark(id) } }
    fun revise(id: String) { viewModelScope.launch { affairs.revise(id) } }
}
