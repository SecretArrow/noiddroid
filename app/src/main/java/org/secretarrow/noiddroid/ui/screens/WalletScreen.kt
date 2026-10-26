package org.secretarrow.noiddroid.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        BrutalSectionTitle("Wallet")
        Text(
            text = "Connects to your parano1d wallet RPC (port 9510 by default). " +
                    "All operations require the wallet's bearer token.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        BrutalPanel(modifier = Modifier.fillMaxWidth(), accent = true) {
            BrutalStat("Balance", "0.00 NOID")
            Spacer(Modifier.height(8.dp))
            Text("Pending: 0 transactions",
                 style = MaterialTheme.typography.labelMedium)
        }

        BrutalPanel(modifier = Modifier.fillMaxWidth()) {
            Text("Recent receipts",
                 style = MaterialTheme.typography.titleMedium,
                 fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text("No transactions yet — connect a wallet in Settings.",
                 style = MaterialTheme.typography.bodyMedium,
                 color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = { /* TODO: send flow */ }, modifier = Modifier.weight(1f)) { Text("Send") }
            OutlinedButton(onClick = { /* TODO: receive flow */ }, modifier = Modifier.weight(1f)) { Text("Receive") }
        }
    }
}
