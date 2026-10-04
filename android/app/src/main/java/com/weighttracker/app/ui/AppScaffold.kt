package com.weighttracker.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.weighttracker.app.ui.theme.WtColors

enum class AppTab(val label: String, val icon: ImageVector) {
    Dashboard("Dashboard", Icons.Filled.Home),
    Log("Log Weight", Icons.Filled.Add),
    Settings("Settings", Icons.Filled.Settings),
}

/** Top bar + bottom navigation shared by all screens. */
@Composable
fun AppScaffold(
    tab: AppTab,
    onTabSelected: (AppTab) -> Unit,
    snackbar: SnackbarHostState = remember { SnackbarHostState() },
    content: @Composable () -> Unit,
) {
    Scaffold(
        topBar = {
            Column(Modifier.statusBarsPadding()) {
                Text(
                    "⚖️ WeightTracker",
                    Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    color = WtColors.Purple,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
                HorizontalDivider(color = WtColors.SoftBorder)
            }
        },
        bottomBar = {
            NavigationBar {
                AppTab.entries.forEach {
                    NavigationBarItem(
                        selected = tab == it,
                        onClick = { onTabSelected(it) },
                        icon = { Icon(it.icon, contentDescription = null) },
                        label = { Text(it.label) },
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            content()
        }
    }
}
