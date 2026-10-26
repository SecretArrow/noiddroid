package org.secretarrow.noiddroid.rpc

import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

/**
 * Mining worker RPC — calls the parano1d node's extminer endpoint.
 * Per spec: bearer token authorises ONLY getBlockTemplate + submitBlock.
 */
interface MiningApi {

    @POST("v1/template")
    suspend fun getBlockTemplate(
        @Header("Authorization") bearer: String,
        @Header("X-Worker-Id") workerId: String,
        @Header("X-Payout-Address") payout: String? = null,
    ): BlockTemplate

    @POST("v1/submit")
    suspend fun submitBlock(
        @Header("Authorization") bearer: String,
        @Body submission: BlockSubmission,
    ): SubmitResult
}
