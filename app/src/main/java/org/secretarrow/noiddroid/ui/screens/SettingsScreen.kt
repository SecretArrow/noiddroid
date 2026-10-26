package org.secretarrow.noiddroid.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.secretarrow.noiddroid.store.SettingsRepository
import org.secretarrow.noiddroid.ui.components.BrutalPanel
import org.secretarrow.noiddroid.ui.components.BrutalSectionTitle

@HiltViewModel
class SettingsViewModel
    @Inject
    constructor(
        private val settings: SettingsRepository,
    ) : ViewModel() {
        val nodeUrl: StateFlow<String> =
            settings.nodeRpcUrl
                .stateIn(viewModelScope, SharingStarted.Eagerly, SettingsRepository.DEFAULT_NODE_URL)
        val walletUrl: StateFlow<String> =
            settings.walletRpcUrl
                .stateIn(viewModelScope, SharingStarted.Eagerly, SettingsRepository.DEFAULT_WALLET_URL)
        val bearer: StateFlow<String> =
            settings.bearerToken
                .map { it ?: "" }
                .stateIn(viewModelScope, SharingStarted.Eagerly, "")
        val useGpu: StateFlow<Boolean> =
            settings.useGpu
                .stateIn(viewModelScope, SharingStarted.Eagerly, false)
        val cpuThreads: StateFlow<Int> =
            settings.cpuThreads
                .stateIn(viewModelScope, SharingStarted.Eagerly, 0)
        val payout: StateFlow<String> =
            settings.payoutAddress
                .map { it ?: "" }
                .stateIn(viewModelScope, SharingStarted.Eagerly, "")
        val darkMode: StateFlow<Boolean> =
            settings.darkMode
                .stateIn(viewModelScope, SharingStarted.Eagerly, true)
        val battThreshold: StateFlow<Int> =
            settings.batteryGuardThreshold
                .stateIn(viewModelScope, SharingStarted.Eagerly, 20)
        val thermalThreshold: StateFlow<Int> =
            settings.thermalGuardThreshold
                .stateIn(viewModelScope, SharingStarted.Eagerly, 40)
        val wakeLock: StateFlow<Boolean> =
            settings.wakeLockEnabled
                .stateIn(viewModelScope, SharingStarted.Eagerly, true)
        val schedule: StateFlow<String> =
            settings.scheduledWindows
                .stateIn(viewModelScope, SharingStarted.Eagerly, "")

        fun setNodeUrl(v: String) = viewModelScope.launch { settings.setNodeRpcUrl(v) }

        fun setWalletUrl(v: String) = viewModelScope.launch { settings.setWalletRpcUrl(v) }

        fun setBearer(v: String) = viewModelScope.launch { settings.setBearerToken(v.ifBlank { null }) }

        fun setUseGpu(v: Boolean) = viewModelScope.launch { settings.setUseGpu(v) }

        fun setCpuThreads(v: Int) = viewModelScope.launch { settings.setCpuThreads(v) }

        fun setPayout(v: String) = viewModelScope.launch { settings.setPayoutAddress(v.ifBlank { null }) }

        fun setDarkMode(v: Boolean) = viewModelScope.launch { settings.setDarkMode(v) }

        fun setBattThreshold(v: Int) = viewModelScope.launch { settings.setBatteryGuardThreshold(v) }

        fun setThermalThreshold(v: Int) = viewModelScope.launch { settings.setThermalGuardThreshold(v) }

        fun setWakeLock(v: Boolean) = viewModelScope.launch { settings.setWakeLockEnabled(v) }

        fun setSchedule(v: String) = viewModelScope.launch { settings.setScheduledWindows(v) }
    }

@Composable
fun SettingsScreen(nav: NavController) {
    val vm: SettingsViewModel = hiltViewModel()
    val nodeUrl by vm.nodeUrl.collectAsState()
    val walletUrl by vm.walletUrl.collectAsState()
    val bearer by vm.bearer.collectAsState()
    val useGpu by vm.useGpu.collectAsState()
    val cpuThreads by vm.cpuThreads.collectAsState()
    val payout by vm.payout.collectAsState()
    val darkMode by vm.darkMode.collectAsState()
    val battThreshold by vm.battThreshold.collectAsState()
    val thermalThreshold by vm.thermalThreshold.collectAsState()
    val wakeLock by vm.wakeLock.collectAsState()
    val schedule by vm.schedule.collectAsState()

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        BrutalSectionTitle("Settings")

        BrutalPanel(modifier = Modifier.fillMaxWidth()) {
            Text("Node", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = nodeUrl,
                onValueChange = vm::setNodeUrl,
                label = { Text("Mining RPC URL") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = walletUrl,
                onValueChange = vm::setWalletUrl,
                label = { Text("Wallet RPC URL") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = bearer,
                onValueChange = vm::setBearer,
                label = { Text("Bearer token") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            )
        }

        BrutalPanel(modifier = Modifier.fillMaxWidth()) {
            Text("Engine", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Use GPU (Vulkan)")
                Switch(checked = useGpu, onCheckedChange = vm::setUseGpu)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "CPU threads (0 = auto-detect): $cpuThreads",
                style = MaterialTheme.typography.bodyMedium,
            )
            Slider(
                value = cpuThreads.toFloat(),
                onValueChange = { vm.setCpuThreads(it.toInt()) },
                valueRange = 0f..16f,
                steps = 16,
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = payout,
                onValueChange = vm::setPayout,
                label = { Text("Payout address (bech32m)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
        }

        BrutalPanel(modifier = Modifier.fillMaxWidth()) {
            Text("Background guards", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            Text("Pause mining below battery: $battThreshold%")
            Slider(
                value = battThreshold.toFloat(),
                onValueChange = { vm.setBattThreshold(it.toInt()) },
                valueRange = 5f..50f,
                steps = 45,
            )
            Spacer(Modifier.height(8.dp))
            Text("Pause above thermal: $thermalThreshold°C")
            Slider(
                value = thermalThreshold.toFloat(),
                onValueChange = { vm.setThermalThreshold(it.toInt()) },
                valueRange = 30f..60f,
                steps = 30,
            )
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Wake lock")
                Switch(checked = wakeLock, onCheckedChange = vm::setWakeLock)
            }
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = schedule,
                onValueChange = vm::setSchedule,
                label = { Text("Scheduled mining (cron: e.g. 0 2 * * *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
        }

        BrutalPanel(modifier = Modifier.fillMaxWidth()) {
            Text("Appearance", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Dark mode")
                Switch(checked = darkMode, onCheckedChange = vm::setDarkMode)
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = { nav.popBackStack() }, modifier = Modifier.weight(1f)) { Text("Back") }
            OutlinedButton(onClick = { nav.navigate("about") }, modifier = Modifier.weight(1f)) { Text("About") }
        }
    }
}
