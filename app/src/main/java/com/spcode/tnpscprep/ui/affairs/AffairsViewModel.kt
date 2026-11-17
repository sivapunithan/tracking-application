package com.spcode.tnpscprep.ui.affairs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spcode.tnpscprep.data.AffairRepository
import com.spcode.tnpscprep.domain.AffairCategory
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AffairsUiState(
    val list: AffairListUi = AffairListUi(emptyList(), emptyList()),
    val filter: AffairFilter = AffairFilter(),
    val dataUpTo: LocalDate? = null,
)

class AffairsViewModel(private val repo: AffairRepository) : ViewModel() {
    private val _filter = MutableStateFlow(AffairFilter())
    val filter: StateFlow<AffairFilter> = _filter.asStateFlow()

    val ui: StateFlow<AffairsUiState> = combine(
        repo.observePack(), repo.observeUser(), repo.observeStates(), _filter, repo.observeDataUpTo(),
    ) { pack, user, states, filter, upTo ->
        AffairsUiState(buildAffairList(pack, user, states, filter), filter, upTo)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AffairsUiState())

    fun setMonth(m: YearMonth?) { _filter.value = _filter.value.copy(month = m) }
    fun setCategory(c: AffairCategory?) { _filter.value = _filter.value.copy(category = c) }
    fun toggleBookmarkedOnly() { _filter.value = _filter.value.copy(bookmarkedOnly = !_filter.value.bookmarkedOnly) }

    fun toggleBookmark(id: String) { viewModelScope.launch { repo.toggleBookmark(id) } }
    fun revise(id: String) { viewModelScope.launch { repo.revise(id) } }
    fun resetRevision(id: String) { viewModelScope.launch { repo.resetRevision(id) } }
    fun addOwn(date: LocalDate, category: AffairCategory, title: String, summary: String) {
        viewModelScope.launch { repo.addUserItem(date, category.name, title, summary) }
    }
    fun deleteOwn(userId: Long) { viewModelScope.launch { repo.deleteUserItem(userId) } }
}
