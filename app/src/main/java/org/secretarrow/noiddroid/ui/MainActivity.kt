package org.secretarrow.noiddroid.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import org.secretarrow.noiddroid.ui.screens.AboutScreen
import org.secretarrow.noiddroid.ui.screens.DashboardScreen
import org.secretarrow.noiddroid.ui.screens.MinerScreen
import org.secretarrow.noiddroid.ui.screens.SettingsScreen
import org.secretarrow.noiddroid.ui.screens.WalletScreen
import org.secretarrow.noiddroid.ui.theme.NoiddroidTheme

/**
 * Single-activity host. Splash -> Compose NavHost. Edge-to-edge enabled.
 * All routing handled by [NavRoute].
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            // Obtain the SplashViewModel via Compose's hiltViewModel() so
            // Hilt doesn't try to @Inject a @HiltViewModel directly into
            // the Activity (which it forbids).
            val splashVm: SplashViewModel = hiltViewModel()
            val themeVm: ThemeViewModel = hiltViewModel()
            val darkMode = themeVm.darkMode.collectAsStateWithLifecycle(initialValue = null)
            val ready = splashVm.ready.collectAsState()

            splash.setKeepOnScreenCondition { !ready.value }

            NoiddroidTheme(darkTheme = darkMode.value ?: true) {
                val nav = rememberNavController()
                NavHost(
                    navController = nav,
                    startDestination = NavRoute.Dashboard.path,
                ) {
                    composable(NavRoute.Dashboard.path) {
                        DashboardScreen(nav = nav)
                    }
                    composable(NavRoute.Miner.path) {
                        MinerScreen(nav = nav)
                    }
                    composable(NavRoute.Wallet.path) {
                        WalletScreen(nav = nav)
                    }
                    composable(NavRoute.Settings.path) {
                        SettingsScreen(nav = nav)
                    }
                    composable(NavRoute.About.path) {
                        AboutScreen(nav = nav)
                    }
                }
            }
        }
    }
}

/** Top-level navigation routes — single source of truth. */
sealed class NavRoute(val path: String) {
    data object Dashboard : NavRoute("dashboard")
    data object Miner : NavRoute("miner")
    data object Wallet : NavRoute("wallet")
    data object Settings : NavRoute("settings")
    data object About : NavRoute("about")
}
