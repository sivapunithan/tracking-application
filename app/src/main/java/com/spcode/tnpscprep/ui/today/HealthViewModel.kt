package com.spcode.tnpscprep.ui.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spcode.tnpscprep.data.HealthProfile
import com.spcode.tnpscprep.data.HealthRepository
import com.spcode.tnpscprep.data.SettingsStore
import com.spcode.tnpscprep.data.db.HealthEntryEntity
import java.time.LocalDate
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HealthViewModel(private val repo: HealthRepository, private val settings: SettingsStore) : ViewModel() {
    val entries: StateFlow<List<HealthEntryEntity>> =
        repo.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val profile: StateFlow<HealthProfile> = settings.healthProfile

    fun saveProfile(p: HealthProfile) = settings.setHealthProfile(p)
    fun add(kind: String, value: Double) { viewModelScope.launch { repo.add(kind, value, LocalDate.now()) } }
    fun delete(id: Long) { viewModelScope.launch { repo.delete(id) } }
    fun deleteAll() { viewModelScope.launch { repo.deleteAll() } }
}
