package com.aibuilt.ghostbank.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.aibuilt.ghostbank.data.AppContainer
import com.aibuilt.ghostbank.ui.screens.AccountRoute
import com.aibuilt.ghostbank.ui.screens.BankPickerRoute
import com.aibuilt.ghostbank.ui.screens.HarvestRoute
import com.aibuilt.ghostbank.ui.screens.LeakCheckRoute
import com.aibuilt.ghostbank.ui.screens.LedgerRoute
import com.aibuilt.ghostbank.ui.screens.SettingsRoute
import com.aibuilt.ghostbank.ui.screens.StealthRoute
import com.aibuilt.ghostbank.ui.screens.TraceAuditRoute
import com.aibuilt.ghostbank.ui.screens.TransferRoute

object Routes {
    const val ACCOUNT = "account"
    const val BANKS = "banks"
    const val TRANSFER = "transfer"
    const val HARVEST = "harvest"
    const val LEDGER = "ledger"
    const val STEALTH = "stealth"
    const val TRACE = "trace"
    const val LEAK = "leak"
    const val SETTINGS = "settings"
}

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab(Routes.ACCOUNT, "Account", Icons.Default.AccountBalance),
    Tab(Routes.HARVEST, "Harvest", Icons.Default.Download),
    Tab(Routes.LEDGER, "Ledger", Icons.Default.ReceiptLong),
    Tab(Routes.STEALTH, "Stealth", Icons.Default.Shield),
    Tab(Routes.SETTINGS, "Settings", Icons.Default.Settings)
)

@Composable
fun GhostBankRoot(container: AppContainer) {
    val nav = rememberNavController()
    val backStackEntry by nav.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route?.substringBefore("?")

    Scaffold(
        bottomBar = {
            if (currentRoute in tabs.map { it.route }) {
                NavigationBar {
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = currentRoute == tab.route,
                            onClick = {
                                if (currentRoute != tab.route) {
                                    nav.navigate(tab.route) {
                                        popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = "${Routes.ACCOUNT}?prefix={prefix}",
            modifier = Modifier.padding(padding)
        ) {
            composable(
                route = "${Routes.ACCOUNT}?prefix={prefix}",
                arguments = listOf(navArgument("prefix") {
                    type = NavType.StringType
                    defaultValue = ""
                })
            ) { entry ->
                AccountRoute(
                    container = container,
                    prefix = entry.arguments?.getString("prefix").orEmpty(),
                    onOpenBanks = { nav.navigate(Routes.BANKS) },
                    onOpenTransfer = { nav.navigate(Routes.TRANSFER) },
                    onOpenSettings = { nav.navigate(Routes.SETTINGS) }
                )
            }
            composable(Routes.BANKS) {
                BankPickerRoute(
                    container = container,
                    onBack = { nav.popBackStack() },
                    onPick = { prefix ->
                        nav.navigate("${Routes.ACCOUNT}?prefix=$prefix") {
                            popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                        }
                    }
                )
            }
            composable(Routes.TRANSFER) {
                TransferRoute(container = container, onBack = { nav.popBackStack() })
            }
            composable(Routes.HARVEST) {
                HarvestRoute(container = container, onOpenTransfer = { nav.navigate(Routes.TRANSFER) })
            }
            composable(Routes.LEDGER) {
                LedgerRoute(container = container)
            }
            composable(Routes.STEALTH) {
                StealthRoute(
                    container = container,
                    onOpenLeak = { nav.navigate(Routes.LEAK) },
                    onOpenTrace = { nav.navigate(Routes.TRACE) }
                )
            }
            composable(Routes.TRACE) {
                TraceAuditRoute(container = container, onBack = { nav.popBackStack() })
            }
            composable(Routes.LEAK) {
                LeakCheckRoute(container = container, onBack = { nav.popBackStack() })
            }
            composable(Routes.SETTINGS) {
                SettingsRoute(container = container, onOpenTrace = { nav.navigate(Routes.TRACE) })
            }
        }
    }
}
