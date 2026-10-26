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
import javax.inject.Inject
import org.secretarrow.noiddroid.ui.screens.AboutScreen
import org.secretarrow.noiddroid.ui.screens.DashboardScreen
import org.secretarrow.noiddroid.ui.screens.MinerScreen
import org.secretarrow.noiddroid.ui.screens.SettingsScreen
import org.secretarrow.noiddroid.ui.screens.WalletScreen
import org.secretarrow.noiddroid.ui.theme.NoiddroidTheme

/**
 * Single-activity host. Splash -> Compose NavHost. Edge-to-edge enabled.
 * All routing handled by [org.secretarrow.noiddroid.ui.NavRoute].
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var splashViewModel: SplashViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        splash.setKeepOnScreenCondition { !splashViewModel.ready.value }

        setContent {
            val theme = hiltViewModel<ThemeViewModel>()
            val darkMode = theme.darkMode.collectAsStateWithLifecycle(initialValue = null)
            val ready = splashViewModel.ready.collectAsState()

            NoiddroidTheme(
                darkTheme = darkMode.value ?: true,
            ) {
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
sealed class NavRoute(
    val path: String,
) {
    data object Dashboard : NavRoute("dashboard")

    data object Miner : NavRoute("miner")

    data object Wallet : NavRoute("wallet")

    data object Settings : NavRoute("settings")

    data object About : NavRoute("about")
}
