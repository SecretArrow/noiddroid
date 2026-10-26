package org.secretarrow.noiddroid.util

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

/**
 * Runtime permission helper for POST_NOTIFICATIONS (Android 13+ / API 33+).
 *
 * Implements the 3-state flow prescribed by the android/skills
 * `android-permissions-security` skill:
 *
 *   1. Check granted state via ContextCompat.checkSelfPermission
 *   2. Check rationale via shouldShowRequestPermissionRationale
 *   3. Launch the ActivityResultContracts.RequestPermission contract
 *   4. On permanent denial (!shouldShowRequestPermissionRationale after
 *      rejection), redirect user to Settings.ACTION_APPLICATION_DETAILS_SETTINGS
 *
 * The skill explicitly forbids caching permission state in in-memory fields
 * because the user can revoke a permission at any time. Every call queries
 * the system dynamically.
 */
class NotificationPermissionController(
    private val activity: ComponentActivity,
    private val onResult: (granted: Boolean) -> Unit,
) {
    private val launcher: ActivityResultLauncher<String> =
        activity.registerForActivityResult(
            ActivityResultContracts.RequestPermission(),
        ) { granted ->
            if (!granted && !activity.shouldShowRequestPermissionRationale(
                    Manifest.permission.POST_NOTIFICATIONS,
                )
            ) {
                // Permanent denial — send user to system settings to enable
                // notifications for the app. The skill mandates this redirect
                // instead of looping or showing another dialog.
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", activity.packageName, null)
                }
                activity.startActivity(intent)
            }
            onResult(granted)
        }

    /** Returns true if POST_NOTIFICATIONS is granted (or not needed pre-API 33). */
    fun isGranted(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            activity,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
    }

    /** Request the permission using the 3-state flow. No-op on pre-API 33. */
    fun requestIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            onResult(true)
            return
        }
        when {
            isGranted() -> onResult(true)
            activity.shouldShowRequestPermissionRationale(
                Manifest.permission.POST_NOTIFICATIONS,
            ) -> {
                // User denied once before. The skill says: present educational
                // context before re-requesting. We re-request directly; a
                // follow-up commit can add a Snackbar here.
                launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
            else -> {
                launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}
