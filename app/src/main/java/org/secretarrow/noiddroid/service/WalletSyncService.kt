package org.secretarrow.noiddroid.service

import android.content.Intent
import android.os.IBinder
import androidx.lifecycle.LifecycleService
import dagger.hilt.android.AndroidEntryPoint

/**
 * Wallet sync service — runs in the main process (not :miner) so the UI
 * remains responsive while it polls the wallet RPC for new blocks.
 *
 * v1 status: skeleton — the actual wallet state polling + DataStore cache
 * will be wired here in a follow-up commit. The class exists so the
 * manifest reference resolves and the service can be tested.
 */
@AndroidEntryPoint
class WalletSyncService : LifecycleService() {
    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return null
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        super.onStartCommand(intent, flags, startId)
        return START_STICKY
    }

    companion object {
        fun start(ctx: android.content.Context) {
            val i = Intent(ctx, WalletSyncService::class.java)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                ctx.startForegroundService(i)
            } else {
                ctx.startService(i)
            }
        }
    }
}
