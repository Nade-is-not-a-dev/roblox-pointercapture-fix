package com.robloxfix.pointercapture

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.robloxfix.pointercapture.data.ComboRepository
import com.robloxfix.pointercapture.model.AppUiState
import com.robloxfix.pointercapture.model.KeyCombo
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Single source of truth for the main screen.
 *
 * Exposes [uiState] as a [StateFlow] collected via `collectAsStateWithLifecycle()`.
 * All repository calls run on [Dispatchers.IO] inside the repository; this VM
 * stays on [viewModelScope] and respects structured cancellation.
 */
public class MainViewModel(
    application: Application,
) : AndroidViewModel(application) {

    private val comboRepo = ComboRepository()

    private val _uiState = MutableStateFlow(AppUiState())
    public val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()

    /** Tracks ongoing refresh to avoid overlapping loads. */
    private var refreshJob: Job? = null

    init {
        refreshAll()
    }

    /**
     * Refreshes combos. Previous job is cancelled
     * to honour structured concurrency on teardown.
     */
    public fun refreshAll() {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            val combos = comboRepo.load()
            _uiState.update { it.copy(combos = combos) }
        }
    }

    public fun showComboDialog(show: Boolean) {
        _uiState.update { it.copy(isComboDialogVisible = show, captureKeys = emptySet()) }
    }

    public fun setCaptureKeys(keys: Set<Int>) {
        _uiState.update { it.copy(captureKeys = keys) }
    }

    public fun addCombo(combo: KeyCombo) {
        viewModelScope.launch {
            val current = comboRepo.load().toMutableList().also { it.add(combo) }
            comboRepo.save(current)
            _uiState.update { it.copy(combos = current, isComboDialogVisible = false, captureKeys = emptySet()) }
        }
    }

    public fun removeCombo(index: Int) {
        viewModelScope.launch {
            val current = comboRepo.load().toMutableList()
            if (index in current.indices) {
                current.removeAt(index)
                comboRepo.save(current)
                _uiState.update { it.copy(combos = current) }
            }
        }
    }

    public fun resetToDefault() {
        viewModelScope.launch {
            comboRepo.clear()
            _uiState.update { it.copy(combos = emptyList()) }
        }
    }

    override fun onCleared() {
        super.onCleared()
        // viewModelScope is cancelled automatically; explicit refreshJob cancel for clarity
        refreshJob?.cancel()
    }
}
