package org.secretarrow.noiddroid

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * End-to-end smoke test — boots the app, verifies the Dashboard renders,
 * taps through to each screen, asserts each tab is reachable.
 *
 * Runs on a real device or emulator (API 26+) via Gradle Managed Devices.
 */
@RunWith(AndroidJUnit4::class)
class AppSmokeTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun dashboard_shows_title_and_status() {
        compose.onNodeWithText("DASHBOARD").assertIsDisplayed()
        compose.onNodeWithText("IDLE").assertIsDisplayed()
    }

    @Test
    fun user_can_navigate_to_miner_screen() {
        compose.onNodeWithText("Miner").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("MINER").assertIsDisplayed()
    }

    @Test
    fun user_can_navigate_to_wallet_screen() {
        compose.onNodeWithText("Wallet").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("WALLET").assertIsDisplayed()
    }

    @Test
    fun user_can_navigate_to_settings_screen() {
        compose.onNodeWithText("Settings").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("SETTINGS").assertIsDisplayed()
    }

    @Test
    fun user_can_navigate_to_about_screen() {
        compose.onNodeWithText("Settings").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("About").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("ABOUT").assertIsDisplayed()
    }
}
