package com.robloxfix.pointercapture

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.KeyEvent
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.robloxfix.pointercapture.model.KeyCombo
import com.robloxfix.pointercapture.ui.theme.AppTheme
import com.robloxfix.pointercapture.ui.theme.Bg
import com.robloxfix.pointercapture.ui.theme.Card
import com.robloxfix.pointercapture.ui.theme.Field
import com.robloxfix.pointercapture.ui.theme.TextSecondary

/**
 * Entry activity — hosts Compose UI backed by [MainViewModel].
 * No XML layouts; all UI is declarative via Compose.
 */
public class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppTheme {
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                MainScreen(
                    uiState = uiState,
                    onCopy = ::copyToClipboard,
                    onAddCombo = { viewModel.showComboDialog(true) },
                    onRemoveCombo = { viewModel.removeCombo(it) },
                    onReset = { viewModel.resetToDefault() },
                )
                if (uiState.isComboDialogVisible) {
                    CaptureDialog(
                        captureKeys = uiState.captureKeys,
                        onKeysChange = { viewModel.setCaptureKeys(it) },
                        onSave = { viewModel.addCombo(KeyCombo(it)) },
                        onDismiss = { viewModel.showComboDialog(false) },
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshAll()
    }

    private fun copyToClipboard(value: String) {
        val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("rbx", value))
        Toast.makeText(this, "Copied", Toast.LENGTH_SHORT).show()
    }
}

@Composable
private fun MainScreen(
    uiState: com.robloxfix.pointercapture.model.AppUiState,
    onCopy: (String) -> Unit,
    onAddCombo: () -> Unit,
    onRemoveCombo: (Int) -> Unit,
    onReset: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        // Header — title + version only
        Text(text = "Roblox Pointer Fix", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Text(text = "v3.0", fontSize = 11.sp, color = Color(0xFF757575), modifier = Modifier.padding(top = 2.dp))
        Spacer(modifier = Modifier.height(14.dp))

        SectionLabel("HOW IT WORKS")
        AppCard {
            StepRow(number = "1", title = "Install & Enable", description = "Install via LSPosed, enable with scope com.roblox.*, reboot.", icon = Icons.Filled.Download)
            StepRow(number = "2", title = "Connect", description = "Connect OTG mouse and keyboard before joining a game.", icon = Icons.Filled.Keyboard)
            ToggleLockSection(combos = uiState.combos, onAddCombo = onAddCombo, onRemoveCombo = onRemoveCombo, onReset = onReset)
            StepRow(number = "4", title = "Unlock", description = "Esc / Back or tap screen to unlock.", icon = Icons.Filled.LockOpen)
        }

        SectionLabel("REQUIRED SCOPE")
        AppCard {
            CopyField(value = "com.roblox.*", onCopy = onCopy)
            Text(
                text = "Enable all com.roblox.* packages in LSPosed; System Framework is not required.",
                fontSize = 11.5.sp,
                color = Color(0xFF9E9E9E),
                modifier = Modifier.padding(top = 6.dp),
            )
        }

        Text(
            text = "Not affiliated with Roblox.",
            fontSize = 11.sp,
            color = Color(0xFF757575),
            modifier = Modifier.padding(top = 12.dp, bottom = 8.dp).fillMaxWidth(),
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        color = Color(0xFF8A8A8A),
        modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
    )
}

@Composable
private fun AppCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Card),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) { content() }
    }
}

@Composable
private fun StepRow(
    number: String,
    title: String,
    description: String,
    icon: ImageVector,
) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier.size(36.dp).clip(CircleShape).background(Color(0xFF2A2A2A)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
        }
        Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
            Text(text = "$number. $title", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text(text = description, fontSize = 12.5.sp, color = TextSecondary, modifier = Modifier.padding(top = 2.dp))
        }
    }
}

@Composable
private fun ToggleLockSection(
    combos: List<KeyCombo>,
    onAddCombo: () -> Unit,
    onRemoveCombo: (Int) -> Unit,
    onReset: () -> Unit,
) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(Color(0xFF2A2A2A)), contentAlignment = Alignment.Center) {
            Icon(imageVector = Icons.Filled.Keyboard, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
        }
        Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
            Text(text = "3. Toggle Lock", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text(text = "Add a key or combo to toggle pointer lock.", fontSize = 12.5.sp, color = TextSecondary, modifier = Modifier.padding(top = 2.dp))
        }
    }
    Column(modifier = Modifier.padding(start = 48.dp, top = 6.dp)) {
        if (combos.isEmpty()) {
            Text(text = "Default: Ctrl + Alt", fontSize = 12.sp, color = Color(0xFF9E9E9E), modifier = Modifier.padding(bottom = 6.dp))
        } else {
            combos.forEachIndexed { idx, combo ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Field)
                        .border(1.dp, Color(0xFF3A3A3A), RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(text = combo.displayName(), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.weight(1f))
                    IconButton(onClick = { onRemoveCombo(idx) }, modifier = Modifier.size(24.dp)) {
                        Icon(imageVector = Icons.Filled.Close, contentDescription = "Remove", tint = Color(0xFFFF6B6B), modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            androidx.compose.material3.Button(onClick = onAddCombo) { Text("+  Add shortcut") }
            androidx.compose.material3.OutlinedButton(onClick = onReset) { Text("Reset") }
        }
    }
}

@Composable
private fun CopyField(
    value: String,
    onCopy: (String) -> Unit,
) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).background(Field).padding(horizontal = 12.dp, vertical = 10.dp),
        ) {
            Text(text = value, fontSize = 12.5.sp, color = Color(0xFFE0E0E0))
        }
        Spacer(modifier = Modifier.width(8.dp))
        androidx.compose.material3.FilledTonalIconButton(onClick = { onCopy(value) }, modifier = Modifier.size(44.dp)) {
            Icon(imageVector = Icons.Filled.ContentCopy, contentDescription = "Copy", tint = Color.White)
        }
    }
}

@Composable
private fun CaptureDialog(
    captureKeys: Set<Int>,
    onKeysChange: (Set<Int>) -> Unit,
    onSave: (Set<Int>) -> Unit,
    onDismiss: () -> Unit,
) {
    val focusRequester = androidx.compose.ui.focus.FocusRequester()
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add shortcut", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .focusRequester(focusRequester)
                    .onPreviewKeyEvent { event ->
                        if (event.nativeKeyEvent.action == KeyEvent.ACTION_DOWN) {
                            val androidCode = event.nativeKeyEvent.keyCode
                            if (!captureKeys.contains(androidCode)) {
                                onKeysChange(captureKeys + androidCode)
                            }
                            return@onPreviewKeyEvent true
                        }
                        false
                    },
            ) {
                Text("Press keys on your keyboard. Use + to add more keys to the same combo.", fontSize = 12.sp, color = TextSecondary)
                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Field).padding(12.dp),
                ) {
                    Text(
                        text = if (captureKeys.isEmpty()) "No keys yet"
                        else KeyCombo(captureKeys).displayName(),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text("Tap + then press another key to make a combo", fontSize = 11.sp, color = Color(0xFF757575))
            }
        },
        confirmButton = {
            TextButton(onClick = { if (captureKeys.isNotEmpty()) onSave(captureKeys) }) { Text("Save") }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { onKeysChange(emptySet()) }) { Text("Clear") }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        },
    )
}


