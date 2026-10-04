package com.weighttracker.app.ui.preview

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.weighttracker.app.data.AppState
import com.weighttracker.app.ui.AppScaffold
import com.weighttracker.app.ui.AppTab
import com.weighttracker.app.ui.DashboardScreen
import com.weighttracker.app.ui.LogScreen
import com.weighttracker.app.ui.SettingsActions
import com.weighttracker.app.ui.SettingsScreen
import com.weighttracker.app.ui.theme.WeightTrackerTheme

/** Renders one tab of the app with the given data, without a ViewModel. */
@Composable
fun AppPreview(tab: AppTab, state: AppState = SampleData.state) {
    WeightTrackerTheme {
        AppScaffold(tab = tab, onTabSelected = {}) {
            when (tab) {
                AppTab.Dashboard -> DashboardScreen(state, onOpenLog = {}, onOpenSettings = {}, today = SampleData.today)
                AppTab.Log -> LogScreen(state, onSave = { _, _, _ -> }, onDelete = {}, today = SampleData.today)
                AppTab.Settings -> SettingsScreen(state, SettingsActions())
            }
        }
    }
}

@Preview(name = "Painel", showSystemUi = true, device = "id:pixel_7", heightDp = 1800)
@Composable
private fun DashboardPreview() = AppPreview(AppTab.Dashboard)

@Preview(name = "Painel (vazio)", showSystemUi = true, device = "id:pixel_7")
@Composable
private fun EmptyDashboardPreview() = AppPreview(AppTab.Dashboard, AppState())

@Preview(name = "Registrar", showSystemUi = true, device = "id:pixel_7")
@Composable
private fun LogPreview() = AppPreview(AppTab.Log)

@Preview(name = "Ajustes", showSystemUi = true, device = "id:pixel_7")
@Composable
private fun SettingsPreview() = AppPreview(AppTab.Settings)
