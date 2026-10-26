package org.secretarrow.noiddroid.rpc

import javax.inject.Inject
import javax.inject.Singleton
import org.secretarrow.noiddroid.store.SettingsRepository
import kotlinx.coroutines.flow.first

/**
 * Wraps [MiningApi] with bearer token lookup from settings.
 * Single-use template lifecycle: a template fetched here must be
 * submitted within 30 seconds (per spec).
 */
@Singleton
class MiningRepository @Inject constructor(
    private val api: MiningApi,
    private val settings: SettingsRepository,
) {
    suspend fun getBlockTemplate(): BlockTemplate {
        val bearer = settings.bearerToken.first() ?: ""
        val payout = settings.payoutAddress.first()
        return api.getBlockTemplate(
            bearer = "Bearer $bearer",
            workerId = WORKER_ID,
            payout = payout,
        )
    }

    suspend fun submitBlock(templateId: String, nonceHex: String): SubmitResult {
        val bearer = settings.bearerToken.first() ?: ""
        return api.submitBlock(
            bearer = "Bearer $bearer",
            submission = BlockSubmission(templateId, nonceHex),
        )
    }

    companion object {
        // Stable per-install worker id. Generated once and persisted.
        const val WORKER_ID = "noiddroid-android-${BuildConfigFlavour.BUILD_HASH}"
    }
}

/** Build flavour injected at compile time. See app/build.gradle.kts `buildConfig`. */
object BuildConfigFlavour {
    val BUILD_HASH: String = "alpha01"
}
