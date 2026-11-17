package com.spcode.tnpscprep.ui.syllabus

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spcode.tnpscprep.data.SettingsStore
import com.spcode.tnpscprep.data.SyllabusRepository
import com.spcode.tnpscprep.data.db.StudiedTopic
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Holds UI state as StateFlow (Compose collects it) and survives rotation. Room's Flows re-emit
 * on every table change, so the first-launch seed or a tap appears on screen with no manual refresh.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SyllabusViewModel(private val repo: SyllabusRepository, private val settings: SettingsStore) : ViewModel() {
    // ONE global "My exams" choice (several allowed) for the whole app: Today, Study and Revise all read it.
    val examMask: StateFlow<Int> = settings.examMask

    val subjects: StateFlow<List<SubjectUi>> = combine(
        repo.observeSubjects(),
        repo.observeUnits(),
        examMask.flatMapLatest { repo.observeTopics(it) },
        ::buildSyllabusUi,
    ).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** The topic she touched most recently (inside My exams): the "Continue" card opens its subject. */
    val lastTouched: StateFlow<StudiedTopic?> = examMask.flatMapLatest { repo.observeStudied(it) }
        .map { list -> list.maxByOrNull { it.lastUpdated } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun setExamMask(mask: Int) = settings.setExamMask(mask)
    fun onTopicTap(topicId: String) { viewModelScope.launch { repo.advance(topicId) } }
    fun onTopicLongPress(topicId: String) { viewModelScope.launch { repo.reset(topicId) } }
    fun onToggleStar(topicId: String) { viewModelScope.launch { repo.toggleStar(topicId) } }
    fun onSaveNote(topicId: String, text: String) { viewModelScope.launch { repo.saveNote(topicId, text) } }
    suspend fun loadNote(topicId: String): String = repo.getNote(topicId)
}
