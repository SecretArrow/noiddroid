package org.secretarrow.noiddroid.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import org.secretarrow.noiddroid.ui.components.BrutalPanel
import org.secretarrow.noiddroid.ui.components.BrutalSectionTitle
import org.secretarrow.noiddroid.ui.components.BrutalStat

@Composable
fun WalletScreen(nav: NavController) {
    val vm: WalletViewModel = hiltViewModel()
    val state by vm.state.collectAsState()

    // Refresh on first composition + when lifecycle resumes.
    LaunchedEffect(Unit) { vm.refresh() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        BrutalSectionTitle("Wallet")
        Text(
            text = "Connects to your parano1d wallet RPC. All operations require " +
                    "the wallet's bearer token (set in Settings).",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        when (val s = state) {
            is WalletUiState.Loading -> BrutalPanel(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "Fetching holdings…",
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            is WalletUiState.NotConfigured -> BrutalPanel(
                modifier = Modifier.fillMaxWidth(),
                accent = true,
            ) {
                BrutalStat("Balance", "— NOID")
                Spacer(Modifier.height(8.dp))
                Text(
                    "No wallet RPC configured. Open Settings to set the wallet URL " +
                            "and bearer token.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            is WalletUiState.Error -> BrutalPanel(modifier = Modifier.fillMaxWidth()) {
                BrutalStat("Error", s.message.take(40))
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = { vm.refresh() }, modifier = Modifier.fillMaxWidth()) {
                    Text("Retry")
                }
            }

            is WalletUiState.Loaded -> {
                if (s.holdings.isEmpty()) {
                    BrutalPanel(modifier = Modifier.fillMaxWidth(), accent = true) {
                        BrutalStat("Balance", "0.00 NOID")
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Wallet connected but no holdings yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    s.holdings.forEach { h ->
                        BrutalPanel(modifier = Modifier.fillMaxWidth(), accent = true) {
                            BrutalStat(h.symbol, h.displayAmount())
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "asset_id: ${h.assetId.take(16)}…",
                                style = MaterialTheme.typography.labelMedium,
                            )
                        }
                    }
                }
                BrutalPanel(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "Pending: ${s.pending} transaction${if (s.pending == 1) "" else "s"}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedButton(
                onClick = { vm.refresh() },
                modifier = Modifier.weight(1f),
            ) { Text("Refresh") }
            OutlinedButton(
                onClick = { nav.navigate("settings") },
                modifier = Modifier.weight(1f),
            ) { Text("Settings") }
        }
    }
}
