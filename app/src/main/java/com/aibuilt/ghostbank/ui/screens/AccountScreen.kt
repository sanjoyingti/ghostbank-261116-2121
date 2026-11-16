package com.aibuilt.ghostbank.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aibuilt.ghostbank.data.AppContainer
import com.aibuilt.ghostbank.data.db.AccountEntity
import com.aibuilt.ghostbank.data.settings.GhostSettings
import com.aibuilt.ghostbank.ui.components.EmptyState
import com.aibuilt.ghostbank.ui.components.GhostCard
import com.aibuilt.ghostbank.ui.components.GhostTopBar
import com.aibuilt.ghostbank.ui.components.KeyValue
import com.aibuilt.ghostbank.ui.components.SectionTitle
import com.aibuilt.ghostbank.ui.components.StatusPill
import com.aibuilt.ghostbank.ui.ghostFactory
import com.aibuilt.ghostbank.viewmodel.AccountUi
import com.aibuilt.ghostbank.viewmodel.AccountViewModel

@Composable
fun AccountRoute(
    container: AppContainer,
    prefix: String,
    onOpenBanks: () -> Unit,
    onOpenTransfer: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val vm: AccountViewModel = viewModel(factory = ghostFactory { AccountViewModel(container) })
    val ui by vm.ui.collectAsStateWithLifecycle()
    val accounts by vm.accounts.collectAsStateWithLifecycle()
    val destination by vm.destination.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()

    LaunchedEffect(prefix) {
        if (prefix.isNotBlank()) vm.setIfsc(prefix)
    }

    AccountScreen(
        ui = ui,
        accounts = accounts,
        destination = destination,
        settings = settings,
        onAccount = vm::setAccount,
        onIfsc = vm::setIfsc,
        onLabel = vm::setLabel,
        onSave = { vm.save(makeDestination = false) },
        onSaveAsDestination = { vm.save(makeDestination = true) },
        onSetDestination = vm::setDestination,
        onDelete = vm::delete,
        onOpenBanks = onOpenBanks,
        onOpenTransfer = onOpenTransfer,
        onOpenSettings = onOpenSettings
    )
}

@Composable
fun AccountScreen(
    ui: AccountUi,
    accounts: List<AccountEntity>,
    destination: AccountEntity?,
    settings: GhostSettings,
    onAccount: (String) -> Unit,
    onIfsc: (String) -> Unit,
    onLabel: (String) -> Unit,
    onSave: () -> Unit,
    onSaveAsDestination: () -> Unit,
    onSetDestination: (Long) -> Unit,
    onDelete: (Long) -> Unit,
    onOpenBanks: () -> Unit,
    onOpenTransfer: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Scaffold(
        topBar = {
            GhostTopBar(title = "Accounts", actions = {
                IconButton(onClick = onOpenBanks) {
                    Icon(Icons.Default.AccountBalance, contentDescription = "Banks")
                }
                IconButton(onClick = onOpenTransfer) {
                    Icon(Icons.Default.SwapVert, contentDescription = "Transfer")
                }
                IconButton(onClick = onOpenSettings) {
                    Icon(Icons.Default.Settings, contentDescription = "Settings")
                }
            })
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            if (!settings.exitConfigured && settings.killSwitch) {
                item {
                    GhostCard {
                        SectionTitle("No exit configured")
                        Text(
                            "Saving an account needs a live IFSC check, but the kill-switch holds every request until a proxy or Tor exists. Set one now.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                        TextButton(onClick = onOpenSettings) { Text("Open Settings") }
                    }
                }
            }

            item {
                GhostCard {
                    SectionTitle("Destination")
                    if (destination == null) {
                        Text(
                            "Nothing marked as destination yet. Add an account and save it as destination.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        KeyValue("Account", maskAccount(destination.accountNumber))
                        KeyValue("IFSC", destination.ifsc)
                        KeyValue("Bank", destination.bankName)
                        StatusPill("credits land here", MaterialTheme.colorScheme.primary)
                    }
                }
            }

            item { SectionTitle("Add an account") }

            item {
                GhostCard {
                    OutlinedTextField(
                        value = ui.accountNumber,
                        onValueChange = onAccount,
                        label = { Text("Account number") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = ui.ifsc,
                        onValueChange = onIfsc,
                        label = { Text("IFSC (for example SBIN0001722)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = ui.label,
                        onValueChange = onLabel,
                        label = { Text("Label (optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = onSave, enabled = !ui.busy, modifier = Modifier.weight(1f)) {
                            Text("Check and save")
                        }
                        OutlinedButton(
                            onClick = onSaveAsDestination,
                            enabled = !ui.busy,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Save as dest.")
                        }
                    }
                    if (ui.busy) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.padding(4.dp))
                            Text(
                                "  Resolving the IFSC on the live resolver...",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                    ui.error?.let {
                        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                    }
                    ui.info?.let {
                        Text(it, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            item { SectionTitle("Registry (${accounts.size})") }

            if (accounts.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Default.AccountBalance,
                        title = "No accounts yet",
                        subtitle = "Add the accounts you want to harvest, or save your own account as the destination."
                    )
                }
            } else {
                items(accounts, key = { it.id }) { account ->
                    AccountRow(
                        account = account,
                        onSetDestination = onSetDestination,
                        onDelete = onDelete
                    )
                }
            }
        }
    }
}

@Composable
private fun AccountRow(
    account: AccountEntity,
    onSetDestination: (Long) -> Unit,
    onDelete: (Long) -> Unit
) {
    GhostCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(account.holderLabel, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = maskAccount(account.accountNumber),
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "${account.bankName} - ${account.ifsc}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = { onSetDestination(account.id) }) {
                Icon(
                    imageVector = if (account.isDestination) Icons.Default.Star else Icons.Default.StarBorder,
                    contentDescription = "Set as destination",
                    tint = if (account.isDestination) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
            IconButton(onClick = { onDelete(account.id) }) {
                Icon(Icons.Default.Delete, contentDescription = "Delete account")
            }
        }
        if (account.isDestination) {
            StatusPill("destination", MaterialTheme.colorScheme.primary)
        }
    }
}

/** Keeps the first two and last two digits, hides the middle. */
private fun maskAccount(number: String): String {
    if (number.length <= 4) return number
    val hidden = "•".repeat((number.length - 4).coerceIn(2, 8))
    return number.take(2) + hidden + number.takeLast(2)
}
