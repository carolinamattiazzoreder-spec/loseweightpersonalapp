package com.weighttracker.app.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.weighttracker.app.data.AppState
import com.weighttracker.app.data.StateCsv
import com.weighttracker.app.data.StateJson
import com.weighttracker.app.data.WeightEntry
import com.weighttracker.app.data.WeightRepository
import com.weighttracker.app.data.normalizeWeights
import com.weighttracker.app.data.round1
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.time.LocalDate

/** A snackbar message; [undoable] ones offer "DESFAZER". */
data class UiMessage(val text: String, val undoable: Boolean = false)

class WeightViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = WeightRepository(application)
    private val writeLock = Mutex()

    private val _state = MutableStateFlow(AppState())
    val state: StateFlow<AppState> = _state.asStateFlow()

    private val _loaded = MutableStateFlow(false)
    val loaded: StateFlow<Boolean> = _loaded.asStateFlow()

    private val _messages = MutableSharedFlow<UiMessage>(extraBufferCapacity = 8)
    val messages: SharedFlow<UiMessage> = _messages.asSharedFlow()

    /** State before the last undoable change. */
    private var undoState: AppState? = null

    private val loadJob = viewModelScope.launch {
        _state.value = withContext(Dispatchers.IO) { repository.load() }
        _loaded.value = true
    }

    fun addEntry(date: LocalDate, weight: Double, note: String) {
        val entry = WeightEntry(date, round1(weight), note.trim())
        update("Registro salvo: ${kg(entry.weight)} kg", undoable = true) {
            it.copy(weights = it.weights + entry)
        }
    }

    fun deleteEntry(date: LocalDate) =
        update("Registro excluído", undoable = true) { s -> s.copy(weights = s.weights.filter { it.date != date }) }

    fun saveProfile(heightCm: Int, goalWeight: Double?) =
        update("Ajustes salvos") { it.copy(heightCm = heightCm, goalWeight = goalWeight?.let(::round1)) }

    fun clearEntries() = update("Todos os registros foram apagados", undoable = true) { it.copy(weights = emptyList()) }

    /** Restores the state from before the last save/delete. */
    fun undo() {
        val previous = undoState ?: return
        undoState = null
        update(null) { previous }
    }

    fun importJson(uri: Uri) {
        viewModelScope.launch {
            val imported = try {
                withContext(Dispatchers.IO) {
                    val text = resolver().openInputStream(uri)?.use { it.readBytes().decodeToString() }
                        ?: error("Cannot open file")
                    StateJson.decode(text)
                }
            } catch (e: Exception) {
                _messages.emit(UiMessage("Falha ao importar: ${e.message}"))
                return@launch
            }
            update("${imported.weights.size} registros importados", undoable = true) { imported }
        }
    }

    fun exportJson(uri: Uri) = export(uri, "Backup exportado") { StateJson.encode(it) }

    fun exportCsv(uri: Uri) = export(uri, "Planilha exportada") { StateCsv.encode(it) }

    private fun export(uri: Uri, successMessage: String, encode: (AppState) -> String) {
        viewModelScope.launch {
            val content = encode(_state.value)
            try {
                withContext(Dispatchers.IO) {
                    resolver().openOutputStream(uri)?.use { it.write(content.encodeToByteArray()) }
                        ?: error("Cannot open file")
                }
                _messages.emit(UiMessage(successMessage))
            } catch (e: Exception) {
                _messages.emit(UiMessage("Falha ao exportar: ${e.message}"))
            }
        }
    }

    private fun update(successMessage: String?, undoable: Boolean = false, transform: (AppState) -> AppState) {
        viewModelScope.launch {
            loadJob.join()
            writeLock.withLock {
                val before = _state.value
                val next = transform(before)
                _state.value = next.copy(weights = normalizeWeights(next.weights))
                try {
                    _state.value = withContext(Dispatchers.IO) { repository.save(next) }
                    undoState = if (undoable) before else null
                    successMessage?.let { _messages.emit(UiMessage(it, undoable)) }
                } catch (e: Exception) {
                    _state.value = before
                    _messages.emit(UiMessage("Falha ao salvar: ${e.message}"))
                }
            }
        }
    }

    private fun resolver() = getApplication<Application>().contentResolver
}
