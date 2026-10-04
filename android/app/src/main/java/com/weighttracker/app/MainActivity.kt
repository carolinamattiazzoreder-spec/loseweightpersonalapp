package com.weighttracker.app

import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.weighttracker.app.ui.AppScaffold
import com.weighttracker.app.ui.AppTab
import com.weighttracker.app.ui.DashboardScreen
import com.weighttracker.app.ui.LogScreen
import com.weighttracker.app.ui.SettingsActions
import com.weighttracker.app.ui.SettingsScreen
import com.weighttracker.app.ui.WeightViewModel
import com.weighttracker.app.ui.theme.WeightTrackerTheme
import com.weighttracker.app.ui.theme.WtColors

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // The app is light-only, so keep dark system-bar icons even in system dark mode.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT),
        )
        setContent {
            WeightTrackerTheme {
                WeightTrackerApp()
            }
        }
    }
}

@Composable
private fun WeightTrackerApp(vm: WeightViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val loaded by vm.loaded.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableStateOf(AppTab.Dashboard) }
    val snackbar = remember { SnackbarHostState() }
    val settingsActions = remember(vm) {
        SettingsActions(
            saveProfile = vm::saveProfile,
            exportJson = vm::exportJson,
            exportCsv = vm::exportCsv,
            importJson = vm::importJson,
            clearEntries = vm::clearEntries,
        )
    }

    LaunchedEffect(vm) {
        vm.messages.collect { snackbar.showSnackbar(it) }
    }

    AppScaffold(tab = tab, onTabSelected = { tab = it }, snackbar = snackbar) {
        if (!loaded) {
            Box(Modifier.fillMaxSize()) {
                CircularProgressIndicator(Modifier.align(Alignment.Center), color = WtColors.Purple)
            }
        } else {
            when (tab) {
                AppTab.Dashboard -> DashboardScreen(state)
                AppTab.Log -> LogScreen(state, onSave = vm::addEntry, onDelete = vm::deleteEntry)
                AppTab.Settings -> SettingsScreen(state, settingsActions)
            }
        }
    }
}
