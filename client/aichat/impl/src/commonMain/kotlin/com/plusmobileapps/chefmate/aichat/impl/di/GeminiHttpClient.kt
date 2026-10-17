package com.plusmobileapps.chefmate.aichat.impl.di

import com.plusmobileapps.chefmate.di.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.Qualifier
import dev.zacsweers.metro.SingleIn
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.sse.SSE
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/**
 * Qualifier for the single [HttpClient] shared by every Gemini integration in the chat module.
 *
 * Both [com.plusmobileapps.chefmate.aichat.impl.GeminiClient] (streaming `:streamGenerateContent`)
 * and [com.plusmobileapps.chefmate.aichat.impl.GeminiRecipeExtractor] (one-shot `:generateContent`)
 * resolve their client via this qualifier so they share the same connection pool, JSON config, and
 * installed plugins.
 */
@Qualifier
@Target(
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.VALUE_PARAMETER,
    AnnotationTarget.TYPE,
)
annotation class GeminiHttpClient

/**
 * Connection details for the `ai-chat` Supabase Edge Function that proxies to Gemini.
 *
 * The Gemini API key used to live in the client build
 * ([com.plusmobileapps.chefmate.buildconfig.BuildConfig]), which leaked it to anyone who cracked
 * the binary. It now lives only in the edge function; the client just needs the function URL and
 * Supabase auth headers, which this abstraction supplies. Injected (rather than reading
 * [SupabaseClient] directly in the Gemini clients) so those clients stay unit-testable against a
 * plain fake.
 */
interface AiChatFunctionConfig {
    /**
     * Absolute URL of the `ai-chat` edge function, e.g.
     * `https://xyz.supabase.co/functions/v1/ai-chat`.
     */
    val functionUrl: String

    /** Supabase anon/public key, sent as the `apikey` header. */
    val anonKey: String

    /**
     * Bearer token for the `Authorization` header: the current user's access token (the app is
     * always signed in, anonymously at minimum), falling back to the anon key.
     */
    fun accessToken(): String
}

@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class SupabaseAiChatFunctionConfig(private val supabaseClient: SupabaseClient) :
    AiChatFunctionConfig {

    override val functionUrl: String =
        "${supabaseClient.supabaseHttpUrl}/functions/v1/$FUNCTION_NAME"

    override val anonKey: String = supabaseClient.supabaseKey

    override fun accessToken(): String =
        supabaseClient.auth.currentAccessTokenOrNull() ?: supabaseClient.supabaseKey

    companion object {
        private const val FUNCTION_NAME = "ai-chat"
    }
}

@BindingContainer
@ContributesTo(AppScope::class)
object GeminiHttpClientComponent {

    @Provides
    @SingleIn(AppScope::class)
    @GeminiHttpClient
    fun providesGeminiHttpClient(): HttpClient = HttpClient {
        install(SSE)
        install(ContentNegotiation) {
            json(
                Json {
                    ignoreUnknownKeys = true
                    encodeDefaults = false
                }
            )
        }
    }
}
