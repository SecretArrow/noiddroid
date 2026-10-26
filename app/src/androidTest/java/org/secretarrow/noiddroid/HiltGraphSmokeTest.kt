package org.secretarrow.noiddroid

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.secretarrow.noiddroid.rpc.WalletRepository

/**
 * Verifies that the Hilt graph resolves at runtime on a real device /
 * emulator. Per testing-setup skill Step 2: "@HiltAndroidTest +
 * HiltAndroidRule" is the canonical pattern for instrumented Hilt tests.
 *
 * This test injects a [WalletRepository] and starts the [MainActivity]
 * via ComposeTestRule. If the Hilt graph is broken (missing @Provides,
 * unresolvable binding, etc.) the test fails at setup time — surfacing
 * the bug before a user sees a runtime crash.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class HiltGraphSmokeTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Inject
    lateinit var walletRepository: WalletRepository

    @Test
    fun hilt_graph_resolves_wallet_repository() {
        hiltRule.inject()
        // If injection succeeded, the binding exists in the singleton
        // component. The Repository itself is a @Singleton — checking
        // that it's non-null is the smoke test.
        check(walletRepository is WalletRepository)
    }

    @Test
    fun main_activity_launches_and_shows_dashboard_title() {
        hiltRule.inject()
        composeRule.onNodeWithText("DASHBOARD").assertIsDisplayed()
    }
}
