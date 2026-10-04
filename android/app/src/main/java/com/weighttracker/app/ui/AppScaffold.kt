package com.weighttracker.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
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
    Dashboard("Painel", WtIcons.Home),
    Log("Registrar", WtIcons.PlusCircle),
    Settings("Ajustes", WtIcons.Sliders),
}

/** Bottom navigation, the "registrar" FAB on the Painel, and the snackbar. */
@Composable
fun AppScaffold(
    tab: AppTab,
    onTabSelected: (AppTab) -> Unit,
    snackbar: SnackbarHostState = remember { SnackbarHostState() },
    content: @Composable () -> Unit,
) {
    Scaffold(
        containerColor = WtColors.Bg,
        bottomBar = {
            NavigationBar(containerColor = WtColors.Surface, tonalElevation = 0.dp) {
                AppTab.entries.forEach {
                    val selected = tab == it
                    NavigationBarItem(
                        selected = selected,
                        onClick = { onTabSelected(it) },
                        icon = { Icon(it.icon, contentDescription = null, modifier = Modifier.size(21.dp)) },
                        label = { Text(it.label, fontSize = 11.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = WtColors.Ink,
                            selectedTextColor = WtColors.Ink,
                            indicatorColor = WtColors.Mint,
                            unselectedIconColor = WtColors.Muted,
                            unselectedTextColor = WtColors.Muted,
                        ),
                    )
                }
            }
        },
        floatingActionButton = {
            if (tab == AppTab.Dashboard) {
                FloatingActionButton(
                    onClick = { onTabSelected(AppTab.Log) },
                    shape = CircleShape,
                    containerColor = WtColors.Primary,
                    contentColor = WtColors.OnPrimary,
                    elevation = FloatingActionButtonDefaults.elevation(6.dp),
                ) {
                    Icon(WtIcons.Plus, contentDescription = "Registrar peso", modifier = Modifier.size(24.dp))
                }
            }
        },
        snackbarHost = {
            SnackbarHost(snackbar) { data ->
                Snackbar(
                    data,
                    shape = RoundedCornerShape(16.dp),
                    containerColor = WtColors.Ink,
                    contentColor = WtColors.OnPrimary,
                    actionColor = WtColors.Mint,
                )
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            content()
        }
    }
}
