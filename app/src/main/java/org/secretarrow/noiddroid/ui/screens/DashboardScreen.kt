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
fun DashboardScreen(nav: NavController) {
    val vm: DashboardViewModel = hiltViewModel()
    val minerEnabled by vm.minerEnabled.collectAsState()
    val useGpu by vm.useGpu.collectAsState()
    val nodeUrl by vm.nodeUrl.collectAsState()

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        BrutalSectionTitle("Dashboard")
        Text(
            text = "External Poseidon2b worker for the parano1d network.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        BrutalPanel(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BrutalStat("Status", if (minerEnabled) "ACTIVE" else "IDLE")
                Switch(checked = minerEnabled, onCheckedChange = vm::setMinerEnabled)
            }
        }

        BrutalPanel(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                BrutalStat("Engine", if (useGpu) "GPU/Vulkan" else "CPU/Rust")
            }
        }

        BrutalPanel(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Node",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = nodeUrl.ifBlank { "Not configured" },
                style = MaterialTheme.typography.titleMedium,
                fontFamily = MaterialTheme.typography.labelSmall.fontFamily,
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedButton(
                onClick = { nav.navigate("miner") },
                modifier = Modifier.weight(1f),
            ) { Text("Miner") }
            OutlinedButton(
                onClick = { nav.navigate("wallet") },
                modifier = Modifier.weight(1f),
            ) { Text("Wallet") }
            OutlinedButton(
                onClick = { nav.navigate("settings") },
                modifier = Modifier.weight(1f),
            ) { Text("Settings") }
        }
    }
}
