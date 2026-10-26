package org.secretarrow.noiddroid.rpc

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import org.secretarrow.noiddroid.store.SettingsRepository
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

@Qualifier @Retention(AnnotationRetention.BINARY)
annotation class MiningClient

@Qualifier @Retention(AnnotationRetention.BINARY)
annotation class WalletClient

@Module @InstallIn(SingletonComponent::class)
object RpcModule {

    @Provides @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = true
        prettyPrint = false
    }

    @Provides @Singleton
    fun provideOkHttp(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        })
        .build()

    /**
     * Mining Retrofit. Base URL is read once from settings at construct time;
     * URL changes mid-session require app restart (deliberate — re-pointing
     * a worker at a different node mid-block invalidates templates anyway).
     */
    @Provides @Singleton @MiningClient
    fun provideMiningRetrofit(
        client: OkHttpClient,
        json: Json,
        settings: SettingsRepository,
    ): Retrofit {
        val url = runBlocking { settings.nodeRpcUrl.first() }.let { ensureSlash(it) }
        val authClient = client.newBuilder()
            .addInterceptor(BearerInterceptor(settings))
            .build()
        return Retrofit.Builder()
            .baseUrl(url)
            .client(authClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }

    @Provides @Singleton
    fun provideMiningApi(@MiningClient r: Retrofit): MiningApi = r.create(MiningApi::class.java)

    @Provides @Singleton @WalletClient
    fun provideWalletRetrofit(client: OkHttpClient, json: Json): Retrofit =
        Retrofit.Builder()
            .baseUrl("http://127.0.0.1:9510/")
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

    @Provides @Singleton
    fun provideWalletApi(@WalletClient r: Retrofit): WalletApi = r.create(WalletApi::class.java)

    private fun ensureSlash(url: String): String =
        if (url.endsWith("/")) url else "$url/"
}

/**
 * Adds the `Authorization: Bearer <token>` header to every mining request.
 * Reads the current token synchronously — DataStore is backed by an in-memory
 * cache after first load, so this is a few microseconds.
 */
class BearerInterceptor(private val settings: SettingsRepository) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = runBlocking { settings.bearerToken.first() }
        val req = if (token.isNullOrBlank()) chain.request()
                  else chain.request().newBuilder()
                       .header("Authorization", "Bearer $token")
                       .build()
        return chain.proceed(req)
    }
}
