package com.spcode.tnpscprep.ui.diary

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spcode.tnpscprep.data.DiaryBackup
import com.spcode.tnpscprep.data.DiaryRepository
import com.spcode.tnpscprep.data.ImportOutcome
import com.spcode.tnpscprep.data.db.DiaryEntryEntity
import com.spcode.tnpscprep.domain.Autosave
import com.spcode.tnpscprep.domain.DiaryItem
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** What the editor has already stored. Mutated only inside [DiaryViewModel.persist] (under a lock) so two saves can never create two rows. */
class EditorSession(var id: Long?, var savedText: String?, var savedMood: String?)

sealed interface Persisted {
    data object Nothing : Persisted
    data class Saved(val first: Boolean) : Persisted
    data class Deleted(val entry: DiaryEntryEntity) : Persisted
}

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class DiaryViewModel(private val repo: DiaryRepository) : ViewModel() {
    val query = MutableStateFlow("")
    val items: StateFlow<List<DiaryItem>> = query.debounce(150).flatMapLatest { repo.observe(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val lock = Mutex()

    /** Messages shown over the Journal list: an Undo bar after a delete, and the result of an import/export. Kept here so they survive leaving the editor. */
    val undo = MutableStateFlow<DiaryEntryEntity?>(null)
    val notice = MutableStateFlow<String?>(null)
    fun showUndo(e: DiaryEntryEntity) { undo.value = e }
    fun postNotice(text: String) { notice.value = text }

    suspend fun load(id: Long) = repo.get(id)

    /** Applies the autosave rules (domain/Autosave) and stores the result. A mood change alone also counts as a change. */
    suspend fun persist(s: EditorSession, date: LocalDate, text: String, mood: String?, msSinceEdit: Long, leaving: Boolean): Persisted = lock.withLock {
        var action = Autosave.decide(text, s.savedText, s.id == null, msSinceEdit, leaving)
        if (action == Autosave.Action.NONE && s.id != null && mood != s.savedMood && text.isNotBlank() && (leaving || msSinceEdit >= Autosave.DEBOUNCE_MS)) {
            action = Autosave.Action.SAVE
        }
        when (action) {
            Autosave.Action.NONE -> Persisted.Nothing
            Autosave.Action.SAVE -> {
                val first = s.id == null
                s.id = repo.save(s.id, date, text, mood)
                s.savedText = text; s.savedMood = mood
                Persisted.Saved(first)
            }
            Autosave.Action.DELETE -> {
                val removed = s.id?.let { repo.delete(it) }
                s.id = null; s.savedText = null
                if (removed != null) Persisted.Deleted(removed) else Persisted.Nothing
            }
        }
    }

    /** Fire and forget (survives the screen): used when the app stops while an editor is open. */
    fun flush(s: EditorSession, date: LocalDate, text: String, mood: String?) {
        viewModelScope.launch { persist(s, date, text, mood, Long.MAX_VALUE, leaving = true) }
    }

    suspend fun delete(id: Long) = repo.delete(id)
    fun restore(e: DiaryEntryEntity) { viewModelScope.launch { repo.restore(e) } }
    fun setPinned(id: Long, pinned: Boolean) { viewModelScope.launch { repo.setPinned(id, pinned) } }
    fun clearUndo() { undo.value = null }
    fun clearNotice() { notice.value = null }

    suspend fun exportTo(resolver: ContentResolver, uri: Uri): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val json = repo.exportJson()
            resolver.openOutputStream(uri, "wt")!!.use { it.write(json.toByteArray(Charsets.UTF_8)) }
        }.isSuccess
    }

    /** Reads at most the size cap, then hands the text to the merge-only import. */
    suspend fun importFrom(resolver: ContentResolver, uri: Uri): Result<ImportOutcome> = withContext(Dispatchers.IO) {
        runCatching {
            resolver.openInputStream(uri)!!.use { ins ->
                val bytes = ins.readNBytes(DiaryBackup.MAX_FILE_CHARS * 2 + 1)
                String(bytes, Charsets.UTF_8)
            }
        }.fold({ repo.importJson(it) }, { Result.failure(it) })
    }
}
