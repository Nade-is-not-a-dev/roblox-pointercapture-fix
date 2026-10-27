package com.robloxfix.pointercapture.data

import com.robloxfix.pointercapture.model.KeyCombo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.BufferedReader
import java.io.File
import java.io.FileReader

/**
 * Persists custom key combos as JSON `[[keyCode,...], ...]` at [KEYS_FILE].
 * Also mirrored via `su cat` for LSPosed world-readable access.
 */
public class ComboRepository {

    public companion object {
        public const val KEYS_FILE: String = "/data/local/tmp/rbxlsp_keys.json"
    }

    public suspend fun load(): List<KeyCombo> = withContext(Dispatchers.IO) {
        // Prefer su cat for reliable read from world-readable tmp
        readViaSu()?.let { if (it.isNotEmpty()) return@withContext it }
        readDirect()?.let { if (it.isNotEmpty()) return@withContext it }
        emptyList()
    }

    public suspend fun save(combos: List<KeyCombo>): Unit = withContext(Dispatchers.IO) {
        val arr = JSONArray()
        combos.forEach { c ->
            val ja = JSONArray()
            c.keyCodes.forEach { ja.put(it) }
            arr.put(ja)
        }
        val json = arr.toString()
        val esc = json.replace("'", "'\\''")
        val cmd = "mkdir -p /data/local/tmp && printf '%s' '$esc' > $KEYS_FILE && chmod 644 $KEYS_FILE && echo ok"
        Runtime.getRuntime().exec(arrayOf("su", "-c", cmd)).waitFor()
        Unit
    }

    public suspend fun clear(): Unit = withContext(Dispatchers.IO) {
        Runtime.getRuntime().exec(arrayOf("su", "-c", "rm -f $KEYS_FILE")).waitFor()
        Unit
    }

    private fun parseJson(txt: String): List<KeyCombo> {
        val arr = JSONArray(txt)
        return (0 until arr.length()).mapNotNull { i ->
            val combo = arr.getJSONArray(i)
            val set = (0 until combo.length()).map { combo.getInt(it) }.toSet()
            if (set.isEmpty()) null else KeyCombo(set)
        }
    }

    private fun readViaSu(): List<KeyCombo>? = runCatching {
        val p = Runtime.getRuntime().exec(arrayOf("su", "-c", "cat $KEYS_FILE 2>/dev/null"))
        val txt = p.inputStream.bufferedReader().readText().trim()
        p.waitFor()
        if (txt.isEmpty()) null else parseJson(txt)
    }.getOrNull()

    private fun readDirect(): List<KeyCombo>? = runCatching {
        val f = File(KEYS_FILE)
        if (!f.exists()) return null
        val txt = BufferedReader(FileReader(f)).use(BufferedReader::readText).trim()
        if (txt.isEmpty()) null else parseJson(txt)
    }.getOrNull()
}
