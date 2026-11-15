package com.weighttracker.app.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.platform.LocalContext
import com.weighttracker.app.data.UpdateCheck
import com.weighttracker.app.data.UpdateChecker
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.weighttracker.app.data.AppState
import com.weighttracker.app.ui.theme.Tone
import com.weighttracker.app.ui.theme.WtColors
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private enum class Confirm { Import, ClearAll }

class SettingsActions(
    val saveProfile: (name: String, heightCm: Int, goalWeight: Double?) -> Unit = { _, _, _ -> },
    val exportJson: (Uri) -> Unit = {},
    val exportCsv: (Uri) -> Unit = {},
    val importJson: (Uri) -> Unit = {},
    val clearEntries: () -> Unit = {},
    val checkUpdate: () -> Unit = {},
)

@Composable
fun SettingsScreen(
    state: AppState,
    actions: SettingsActions,
    modifier: Modifier = Modifier,
    installedVersion: String = "—",
    updateStatus: UpdateStatus = UpdateStatus.Idle,
) {
    val focus = LocalFocusManager.current
    val context = LocalContext.current
    fun open(url: String) {
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (e: ActivityNotFoundException) {
            // No browser: nothing sensible to do.
        }
    }
    val currentName = userName(state)
    var nameText by rememberSaveable(currentName) { mutableStateOf(currentName) }
    var heightText by rememberSaveable(state.heightCm) { mutableStateOf(state.heightCm.toString()) }
    var goalText by rememberSaveable(state.goalWeight) { mutableStateOf(state.goalWeight?.let(::kg) ?: "") }
    var confirm by remember { mutableStateOf<Confirm?>(null) }
    var importUri by remember { mutableStateOf<Uri?>(null) }

    val height = heightText.trim().toIntOrNull()
    val heightValid = height != null && height in 100..250
    val goal = parseDecimal(goalText)
    val goalValid = goalText.isBlank() || (goal != null && goal in 20.0..300.0)
    val nameValid = nameText.trim().length <= 30

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
    val fieldStyle = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.Bold)

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 18.dp, end = 18.dp, top = 22.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ScreenTitle("Ajustes")

        WtCard(spacing = 13.dp) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                IconCircle(WtIcons.Flag, Tone(WtColors.Rose, WtColors.RoseInk))
                CardTitle("Perfil e meta")
            }
            LabeledField(
                label = "Nome",
                value = nameText,
                onValueChange = { nameText = it },
                placeholder = "Seu nome",
                isError = !nameValid,
                errorText = "Use até 30 caracteres",
                textStyle = fieldStyle,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                LabeledField(
                    label = "Altura (cm)",
                    value = heightText,
                    onValueChange = { heightText = it },
                    keyboardType = KeyboardType.Number,
                    isError = !heightValid,
                    errorText = "Entre 100 e 250 cm",
                    textStyle = fieldStyle,
                    modifier = Modifier.weight(1f),
                )
                LabeledField(
                    label = "Peso meta (kg)",
                    value = goalText,
                    onValueChange = { goalText = it },
                    keyboardType = KeyboardType.Decimal,
                    isError = !goalValid,
                    errorText = "Entre 20 e 300 kg",
                    placeholder = "72,0",
                    textStyle = fieldStyle,
                    modifier = Modifier.weight(1f),
                )
            }
            PrimaryPillButton(
                text = "Salvar ajustes",
                enabled = heightValid && goalValid && nameValid,
                onClick = {
                    actions.saveProfile(nameText, height!!, if (goalText.isBlank()) null else goal)
                    focus.clearFocus()
                },
            )
        }

        WtCard(spacing = 10.dp) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                IconCircle(WtIcons.Database, Tone(WtColors.Teal, WtColors.TealInk))
                Column(Modifier.weight(1f)) {
                    CardTitle("Backup")
                    Text("Exporte ou restaure seus registros", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = WtColors.Muted)
                }
            }
            TonalButton("Exportar backup (JSON)", WtIcons.Download, Tone(WtColors.Sky, WtColors.SkyInk)) {
                exportJson.launch("weighttracker_backup_$stamp.json")
            }
            TonalButton("Exportar planilha (CSV)", WtIcons.Table, Tone(WtColors.Mint, WtColors.MintInk)) {
                exportCsv.launch("weighttracker_$stamp.csv")
            }
            TonalButton("Importar backup (JSON)", WtIcons.Upload, Tone(WtColors.Amber, WtColors.AmberInk)) {
                importJson.launch(arrayOf("application/json", "application/octet-stream", "text/plain"))
            }
            Text(
                "Importar substitui todos os registros atuais. Aceita o backup do app web (Streamlit).",
                Modifier.padding(horizontal = 4.dp), fontSize = 11.sp, fontWeight = FontWeight.Medium, color = WtColors.Muted,
            )
        }

        WtCard(padding = androidx.compose.foundation.layout.PaddingValues(horizontal = 18.dp, vertical = 16.dp), spacing = 8.dp) {
            Text("Neste aparelho · weight_data.json", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = WtColors.Ink)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Pill("${state.weights.size} registros", WtColors.Sky, WtColors.SkyInk)
                Pill(PtDate.savedAt(state.lastSavedAt)?.let { "Salvo em $it" } ?: "Ainda não salvo", WtColors.Line, WtColors.Ink2)
            }
            Text(
                "Com o backup do Android ativado, os dados também vão para sua conta Google.",
                fontSize = 11.sp, fontWeight = FontWeight.Medium, color = WtColors.Muted,
            )
        }

        WtCard(spacing = 10.dp) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                IconCircle(WtIcons.Download, Tone(WtColors.Sky, WtColors.SkyInk))
                Column(Modifier.weight(1f)) {
                    CardTitle("Atualizações")
                    Text("Versão instalada: $installedVersion", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = WtColors.Muted)
                }
            }
            AnimatedContent(
                targetState = updateStatus,
                transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(120)) },
                label = "update",
            ) { status ->
                when (status) {
                    UpdateStatus.Idle -> TonalButton("Verificar atualização", WtIcons.Download, Tone(WtColors.Sky, WtColors.SkyInk), actions.checkUpdate)
                    UpdateStatus.Checking -> Row(
                        Modifier.fillMaxWidth().padding(vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                    ) {
                        CircularProgressIndicator(Modifier.size(18.dp), color = WtColors.Primary, strokeWidth = 2.dp)
                        Text("Verificando no GitHub…", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = WtColors.Ink2)
                    }
                    is UpdateStatus.Done -> when (val result = status.result) {
                        is UpdateCheck.UpToDate -> Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("✓ Você já está na versão mais recente.", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = WtColors.Good)
                            TonalButton("Verificar de novo", WtIcons.Download, Tone(WtColors.Line, WtColors.Ink2), actions.checkUpdate)
                        }
                        is UpdateCheck.Available -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Nova versão ${result.version} disponível", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = WtColors.Ink)
                            if (result.notes.isNotBlank()) {
                                Text(result.notes.lineSequence().first(), fontSize = 12.sp, color = WtColors.Muted, maxLines = 3)
                            }
                            PrimaryPillButton("Baixar atualização") { open(result.downloadUrl) }
                            Text(
                                "Depois do download, abra o arquivo para instalar por cima. Seus registros continuam.",
                                fontSize = 11.sp, fontWeight = FontWeight.Medium, color = WtColors.Muted,
                            )
                        }
                        is UpdateCheck.Failed -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(result.reason, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = WtColors.RoseInk)
                            TonalButton("Abrir página de downloads", WtIcons.Download, Tone(WtColors.Amber, WtColors.AmberInk)) { open(UpdateChecker.RELEASES_PAGE) }
                            TonalButton("Tentar de novo", WtIcons.Download, Tone(WtColors.Line, WtColors.Ink2), actions.checkUpdate)
                        }
                    }
                }
            }
        }

        DangerOutlinedButton("Apagar todos os registros") { confirm = Confirm.ClearAll }
    }

    when (confirm) {
        Confirm.Import -> ConfirmDialog(
            title = "Importar backup?",
            text = "Isso substitui todos os registros atuais pelo conteúdo do arquivo.",
            action = "Importar",
            onConfirm = { importUri?.let(actions.importJson) },
            onDismiss = { confirm = null },
        )
        Confirm.ClearAll -> ConfirmDialog(
            title = "Apagar todos os registros?",
            text = "Os registros de peso serão apagados. Altura e meta continuam salvas.",
            action = "Apagar tudo",
            onConfirm = actions.clearEntries,
            onDismiss = { confirm = null },
        )
        null -> Unit
    }
}
