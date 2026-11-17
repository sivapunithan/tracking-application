package com.spcode.tnpscprep.ui.syllabus

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spcode.tnpscprep.data.SyllabusRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Holds UI state as StateFlow (Compose collects it) and survives rotation. Room's Flows re-emit
 * on every table change, so the first-launch seed or a tap appears on screen with no manual refresh.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SyllabusViewModel(private val repo: SyllabusRepository) : ViewModel() {
    private val _filter = MutableStateFlow(ExamFilter.ALL)
    val filter: StateFlow<ExamFilter> = _filter.asStateFlow()

    val subjects: StateFlow<List<SubjectUi>> = combine(
        repo.observeSubjects(),
        repo.observeUnits(),
        _filter.flatMapLatest { repo.observeTopics(it.mask) },
        ::buildSyllabusUi,
    ).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setFilter(f: ExamFilter) { _filter.value = f }
    fun onTopicTap(topicId: String) { viewModelScope.launch { repo.advance(topicId) } }
    fun onTopicLongPress(topicId: String) { viewModelScope.launch { repo.reset(topicId) } }
}
