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
import org.secretarrow.noiddroid.service.MiningState
import org.secretarrow.noiddroid.ui.components.BrutalPanel
import org.secretarrow.noiddroid.ui.components.BrutalSectionTitle
import org.secretarrow.noiddroid.ui.components.BrutalStat

@Composable
fun MinerScreen(nav: NavController) {
    val vm: MinerStatusViewModel = hiltViewModel()
    val state by vm.state.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        BrutalSectionTitle("Miner")
        Text(
            text = "Live nonce-search telemetry. The worker owns no State — " +
                    "templates are fetched from your node and results submitted back.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        BrutalPanel(modifier = Modifier.fillMaxWidth(), accent = state is MiningState.Hashing) {
            when (val s = state) {
                is MiningState.Hashing -> {
                    BrutalStat("Hash rate", formatHash(s.rate))
                    Spacer(Modifier.height(8.dp))
                    Text("Template: ${s.template.templateId.take(8)}…",
                         style = MaterialTheme.typography.labelMedium)
                }
                is MiningState.Paused  -> BrutalStat("Paused", s.reason)
                is MiningState.BlockFound -> BrutalStat("Block found", s.templateId.take(8) + "…")
                is MiningState.Error   -> BrutalStat("Error", s.message.take(24) + "…")
                is MiningState.Idle    -> BrutalStat("Status", "IDLE")
            }
        }

        BrutalPanel(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "How it works",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "1. Worker polls node for new template\n" +
                       "2. CPU threads or Vulkan workgroups search 128-bit nonce\n" +
                       "3. Found nonce submitted via submitBlock\n" +
                       "4. Templates expire after 30 seconds — fetched fresh each round",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedButton(
                onClick = { nav.navigate("settings") },
                modifier = Modifier.weight(1f),
            ) { Text("Configure") }
            OutlinedButton(
                onClick = { nav.navigate("about") },
                modifier = Modifier.weight(1f),
            ) { Text("About") }
        }
    }
}

private fun formatHash(rate: Long): String = when {
    rate >= 1_000_000 -> "%.2f MH/s".format(rate / 1_000_000.0)
    rate >= 1_000    -> "%.2f kH/s".format(rate / 1_000.0)
    else             -> "$rate H/s"
}
