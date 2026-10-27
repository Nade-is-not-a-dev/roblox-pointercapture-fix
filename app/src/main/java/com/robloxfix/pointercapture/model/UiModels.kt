package com.robloxfix.pointercapture.model

import androidx.annotation.DrawableRes

/**
 * Single step in the "How it works" section.
 */
public data class StepItem(
    val number: Int,
    val title: String,
    val description: String,
    @DrawableRes val iconRes: Int,
)

/**
 * Custom key-combo for toggling pointer lock.
 * Stored as JSON `[[keyCode, ...], ...]` in `/data/local/tmp/rbxlsp_keys.json`.
 */
public data class KeyCombo(
    val keyCodes: Set<Int>,
) {
    public fun displayName(): String {
        if (keyCodes.isEmpty()) return "—"
        val names = keyCodes.map { code ->
            keyCodeToLabel(code)
        }.sorted()
        return names.joinToString(" + ")
    }

    private fun keyCodeToLabel(code: Int): String = try {
        android.view.KeyEvent.keyCodeToString(code)
            .removePrefix("KEYCODE_")
            .replace("CTRL_LEFT", "Ctrl")
            .replace("CTRL_RIGHT", "Ctrl")
            .replace("ALT_LEFT", "Alt")
            .replace("ALT_RIGHT", "Alt")
            .replace("SHIFT_LEFT", "Shift")
            .replace("SHIFT_RIGHT", "Shift")
    } catch (_: Throwable) {
        code.toString()
    }
}

/**
 * Scope displayed in REQUIRED SCOPE card.
 * Matches all variants: com.roblox.client, com.roblox.vng, etc.
 */
public data class ScopeInfo(
    val packagePrefix: String = "com.roblox.",
    val packageName: String = "com.roblox.*",
    val note: String = "Enable all com.roblox.* packages in LSPosed; System Framework is not required.",
)
