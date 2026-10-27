package com.robloxfix.pointercapture.model

/**
 * Single source of truth for the main screen — collected via [collectAsStateWithLifecycle].
 */
public data class AppUiState(
    val combos: List<KeyCombo> = emptyList(),
    val isComboDialogVisible: Boolean = false,
    val captureKeys: Set<Int> = emptySet(),
)
