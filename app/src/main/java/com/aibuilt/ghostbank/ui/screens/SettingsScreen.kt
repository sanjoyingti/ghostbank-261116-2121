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
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aibuilt.ghostbank.data.AppContainer
import com.aibuilt.ghostbank.ui.components.GhostCard
import com.aibuilt.ghostbank.ui.components.GhostTopBar
import com.aibuilt.ghostbank.ui.components.KeyValue
import com.aibuilt.ghostbank.ui.components.SectionTitle
import com.aibuilt.ghostbank.ui.components.StatusPill
import com.aibuilt.ghostbank.ui.ghostFactory
import com.aibuilt.ghostbank.viewmodel.SettingsViewModel
import kotlin.math.roundToInt

@Composable
fun SettingsRoute(container: AppContainer, onOpenTrace: () -> Unit) {
    val vm: SettingsViewModel = viewModel(factory = ghostFactory { SettingsViewModel(container) })
    val settings by vm.settings.collectAsStateWithLifecycle()
    val vas by vm.virtualAccounts.collectAsStateWithLifecycle()

    var vaNumber by remember { mutableStateOf("") }
    var vaIfsc by remember { mutableStateOf("") }
    var vaLabel by remember { mutableStateOf("") }

    Scaffold(topBar = { GhostTopBar(title = "Settings") }) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            item {
                GhostCard {
                    SectionTitle("Destination stored")
                    KeyValue("Account", if (settings.destinationAccount.isBlank()) "not set" else maskAccount(settings.destinationAccount))
                    KeyValue("IFSC", settings.destinationIfsc.ifBlank { "not set" })
                    Text(
                        "The destination does not change by itself; set it on the Accounts tab.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            item {
                GhostCard {
                    SectionTitle("Velocity caps")
                    Text(
                        "Requests per minute: ${settings.maxRequestsPerMinute}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Slider(
                        value = settings.maxRequestsPerMinute.toFloat(),
                        onValueChange = { vm.setCaps(it.roundToInt().coerceAtLeast(1), settings.maxAccountsPerHour) },
                        valueRange = 1f..200f
                    )
                    Text(
                        "Accounts per hour: ${settings.maxAccountsPerHour}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Slider(
                        value = settings.maxAccountsPerHour.toFloat(),
                        onValueChange = { vm.setCaps(settings.maxRequestsPerMinute, it.roundToInt().coerceAtLeast(1)) },
                        valueRange = 1f..200f
                    )
                }
            }

            item {
                GhostCard {
                    SectionTitle("Working window")
                    Text(
                        "From ${settings.windowStartHour}:00 to ${settings.windowEndHour}:00",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Slider(
                        value = settings.windowStartHour.toFloat(),
                        onValueChange = { vm.setWindow(it.roundToInt().coerceIn(0, 23), settings.windowEndHour) },
                        valueRange = 0f..23f
                    )
                    Slider(
                        value = settings.windowEndHour.toFloat(),
                        onValueChange = { vm.setWindow(settings.windowStartHour, it.roundToInt().coerceIn(0, 23)) },
                        valueRange = 0f..23f
                    )
                }
            }

            item {
                GhostCard {
                    SectionTitle("Rotating virtual destination accounts")
                    Text(
                        "Credit lands in a virtual account that forwards to you, so the digits on the rail rotate.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = vaNumber,
                        onValueChange = { vaNumber = it.filter { c -> c.isDigit() }.take(18) },
                        label = { Text("Virtual account number") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = vaIfsc,
                        onValueChange = { vaIfsc = it.uppercase().filter { c -> c.isLetterOrDigit() }.take(11) },
                        label = { Text("Virtual account IFSC") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = vaLabel,
                        onValueChange = { vaLabel = it.take(40) },
                        label = { Text("Label (optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = {
                            vm.addVirtualAccount(vaNumber, vaIfsc, vaLabel)
                            vaNumber = ""
                            vaIfsc = ""
                            vaLabel = ""
                        },
                        enabled = vaNumber.length >= 9 && vaIfsc.length == 11,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Add virtual account") }
                }
            }

            items(vas) { va ->
                GhostCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SectionTitle(maskAccount(va.accountNumber))
                        StatusPill(if (va.active) "active" else "paused")
                    }
                    KeyValue("IFSC", va.ifsc)
                    KeyValue("Label", va.label)
                    KeyValue("Uses", va.uses.toString())
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { vm.toggleVirtualAccount(va.id, !va.active) }) {
                            Text(if (va.active) "Pause" else "Resume")
                        }
                        OutlinedButton(onClick = { vm.removeVirtualAccount(va.id) }) { Text("Delete") }
                    }
                }
            }

            item {
                GhostCard {
                    SectionTitle("Maintenance")
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = vm::clearLogs, modifier = Modifier.fillMaxWidth()) {
                            Text("Clear trace log")
                        }
                        OutlinedButton(onClick = onOpenTrace, modifier = Modifier.fillMaxWidth()) {
                            Text("Open trace audit")
                        }
                        Text(
                            "One HTTP stack (OkHttp), BouncyCastle and Conscrypt excluded, no third-party SDK.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
