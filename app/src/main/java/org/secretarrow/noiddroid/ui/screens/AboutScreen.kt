package org.secretarrow.noiddroid.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import org.secretarrow.noiddroid.BuildConfig
import org.secretarrow.noiddroid.ui.components.BrutalPanel
import org.secretarrow.noiddroid.ui.components.BrutalSectionTitle
import org.secretarrow.noiddroid.ui.components.BrutalStat

@Composable
fun AboutScreen(nav: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        BrutalSectionTitle("About")
        Text(
            text = "External Poseidon2b worker + wallet RPC client for the parano1d network.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        BrutalPanel(modifier = Modifier.fillMaxWidth()) {
            BrutalStat("Version", BuildConfig.VERSION_NAME)
            Spacer(Modifier.height(8.dp))
            Text("Build: ${BuildConfig.BUILD_TYPE}",
                 style = MaterialTheme.typography.labelMedium)
        }

        BrutalPanel(modifier = Modifier.fillMaxWidth()) {
            Text("Architecture", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Kotlin UI → JNI → Rust core (libnoiddroid_core.a)\n" +
                       "               → Vulkan compute shim\n" +
                       "Network: Retrofit + kotlinx.serialization JSON-RPC 2.0\n" +
                       "Background: Foreground service + WorkManager + Hilt\n" +
                       "Persist: Preferences DataStore\n",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        BrutalPanel(modifier = Modifier.fillMaxWidth()) {
            Text("Mining protocol", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(
                text = "• Worker pulls immutable 80-byte header schedule + 32-byte target\n" +
                       "• Searches 128-bit Poseidon2b nonce (CPU threads or Vulkan workgroups)\n" +
                       "• Submits via HTTP POST to node:9601/submit\n" +
                       "• Bearer token authorises getBlockTemplate + submitBlock only\n" +
                       "• Templates expire after 30s — single-use\n",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        OutlinedButton(onClick = { nav.popBackStack() }, modifier = Modifier.fillMaxWidth()) {
            Text("Back")
        }
    }
}
