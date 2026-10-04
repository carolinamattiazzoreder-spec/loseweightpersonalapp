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
import com.weighttracker.app.data.fmt1
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

class WeightViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = WeightRepository(application)
    private val writeLock = Mutex()

    private val _state = MutableStateFlow(AppState())
    val state: StateFlow<AppState> = _state.asStateFlow()

    private val _loaded = MutableStateFlow(false)
    val loaded: StateFlow<Boolean> = _loaded.asStateFlow()

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    private val loadJob = viewModelScope.launch {
        _state.value = withContext(Dispatchers.IO) { repository.load() }
        _loaded.value = true
    }

    fun addEntry(date: LocalDate, weight: Double, note: String) {
        val entry = WeightEntry(date, round1(weight), note.trim())
        update("Saved ${fmt1(entry.weight)} kg on $date") {
            it.copy(weights = it.weights + entry)
        }
    }

    fun deleteEntry(date: LocalDate) =
        update("Deleted entry for $date") { s -> s.copy(weights = s.weights.filter { it.date != date }) }

    fun saveProfile(heightCm: Int, goalWeight: Double?) =
        update("Profile saved") { it.copy(heightCm = heightCm, goalWeight = goalWeight?.let(::round1)) }

    fun clearEntries() = update("All entries deleted") { it.copy(weights = emptyList()) }

    fun importJson(uri: Uri) {
        viewModelScope.launch {
            val imported = try {
                withContext(Dispatchers.IO) {
                    val text = resolver().openInputStream(uri)?.use { it.readBytes().decodeToString() }
                        ?: error("Cannot open file")
                    StateJson.decode(text)
                }
            } catch (e: Exception) {
                _messages.emit("Import failed: ${e.message}")
                return@launch
            }
            update("Imported ${imported.weights.size} entries") { imported }
        }
    }

    fun exportJson(uri: Uri) = export(uri, "JSON backup exported") { StateJson.encode(it) }

    fun exportCsv(uri: Uri) = export(uri, "CSV exported") { StateCsv.encode(it) }

    private fun export(uri: Uri, successMessage: String, encode: (AppState) -> String) {
        viewModelScope.launch {
            val content = encode(_state.value)
            try {
                withContext(Dispatchers.IO) {
                    resolver().openOutputStream(uri)?.use { it.write(content.encodeToByteArray()) }
                        ?: error("Cannot open file")
                }
                _messages.emit(successMessage)
            } catch (e: Exception) {
                _messages.emit("Export failed: ${e.message}")
            }
        }
    }

    private fun update(successMessage: String, transform: (AppState) -> AppState) {
        viewModelScope.launch {
            loadJob.join()
            writeLock.withLock {
                val next = transform(_state.value)
                _state.value = next.copy(weights = normalizeWeights(next.weights))
                try {
                    _state.value = withContext(Dispatchers.IO) { repository.save(next) }
                    _messages.emit(successMessage)
                } catch (e: Exception) {
                    _messages.emit("Save failed: ${e.message}")
                }
            }
        }
    }

    private fun resolver() = getApplication<Application>().contentResolver
}
