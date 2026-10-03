package com.weighttracker.app.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.weighttracker.app.data.AppState
import com.weighttracker.app.data.fmt1
import com.weighttracker.app.ui.theme.WtColors
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private enum class Confirm { Import, ClearAll }

class SettingsActions(
    val saveProfile: (heightCm: Int, goalWeight: Double?) -> Unit = { _, _ -> },
    val exportJson: (Uri) -> Unit = {},
    val exportCsv: (Uri) -> Unit = {},
    val importJson: (Uri) -> Unit = {},
    val clearEntries: () -> Unit = {},
)

@Composable
fun SettingsScreen(
    state: AppState,
    actions: SettingsActions,
    modifier: Modifier = Modifier,
) {
    val focus = LocalFocusManager.current
    var heightText by rememberSaveable(state.heightCm) { mutableStateOf(state.heightCm.toString()) }
    var goalText by rememberSaveable(state.goalWeight) { mutableStateOf(state.goalWeight?.let(::fmt1) ?: "") }
    var confirm by remember { mutableStateOf<Confirm?>(null) }
    var importUri by remember { mutableStateOf<Uri?>(null) }

    val height = heightText.trim().toIntOrNull()
    val heightValid = height != null && height in 100..250
    val goal = parseDecimal(goalText)
    val goalValid = goalText.isBlank() || (goal != null && goal in 20.0..300.0)

    val stamp = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)
    val exportJson = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let(actions.exportJson)
    }
    val exportCsv = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        uri?.let(actions.exportCsv)
    }
    val importJson = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            importUri = uri
            confirm = Confirm.Import
        }
    }

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        SectionLabel("Profile")
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = heightText,
                onValueChange = { heightText = it },
                label = { Text("Height (cm)") },
                singleLine = true,
                isError = !heightValid,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
                value = goalText,
                onValueChange = { goalText = it },
                label = { Text("Goal weight (kg)") },
                singleLine = true,
                isError = !goalValid,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f),
            )
        }
        PrimaryButton(
            text = "💾 Save profile",
            enabled = heightValid && goalValid,
            onClick = {
                actions.saveProfile(height!!, if (goalText.isBlank()) null else goal)
                focus.clearFocus()
            },
        )

        SectionLabel("Export data")
        OutlinedButton(onClick = { exportJson.launch("weighttracker_backup_$stamp.json") }, Modifier.fillMaxWidth()) {
            Text("⬇️ Export backup (JSON)")
        }
        OutlinedButton(onClick = { exportCsv.launch("weighttracker_$stamp.csv") }, Modifier.fillMaxWidth()) {
            Text("⬇️ Export as spreadsheet (CSV)")
        }

        SectionLabel("Import data")
        Text(
            "Restore a JSON backup — including one downloaded from the Streamlit web app (Settings → Download backup).",
            color = WtColors.Muted,
            fontSize = 12.sp,
        )
        OutlinedButton(
            onClick = { importJson.launch(arrayOf("application/json", "application/octet-stream", "text/plain")) },
            Modifier.fillMaxWidth(),
        ) {
            Text("📥 Import JSON backup")
        }

        SectionLabel("Danger zone", color = WtColors.Bad)
        OutlinedButton(onClick = { confirm = Confirm.ClearAll }, Modifier.fillMaxWidth()) {
            Text("🗑️ Delete all entries", color = WtColors.Bad)
        }

        SectionLabel("Storage info")
        Card(Modifier.fillMaxWidth()) {
            Text("📦 On this device · weight_data.json", color = WtColors.Body, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text("Entries: ${state.weights.size}", color = WtColors.Subtle, fontSize = 12.sp)
            Text("Last saved: ${state.lastSavedAt ?: "Never"}", color = WtColors.Subtle, fontSize = 12.sp)
            Text(
                "Backed up automatically by Android Backup when enabled on your phone.",
                Modifier.padding(top = 4.dp),
                color = WtColors.Muted,
                fontSize = 11.sp,
            )
        }
    }

    when (confirm) {
        Confirm.Import -> ConfirmDialog(
            title = "Import backup?",
            text = "This overwrites all current data with the contents of the file.",
            action = "Import",
            onConfirm = { importUri?.let(actions.importJson) },
            onDismiss = { confirm = null },
        )
        Confirm.ClearAll -> ConfirmDialog(
            title = "Delete all entries?",
            text = "This permanently deletes all logged entries. Height and goal are kept.",
            action = "Delete all",
            onConfirm = actions.clearEntries,
            onDismiss = { confirm = null },
        )
        null -> Unit
    }
}

@Composable
private fun ConfirmDialog(title: String, text: String, action: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = {
            TextButton(onClick = { onConfirm(); onDismiss() }) { Text(action, color = WtColors.Bad) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
