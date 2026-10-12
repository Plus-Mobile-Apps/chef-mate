package com.plusmobileapps.chefmate.auth.data.impl

import com.plusmobileapps.chefmate.Environment
import com.plusmobileapps.chefmate.EnvironmentProvider
import com.plusmobileapps.chefmate.buildconfig.BuildConfig
import com.plusmobileapps.chefmate.di.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.functions.Functions
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.storage.Storage

@BindingContainer
@ContributesTo(AppScope::class)
object SupabaseModule {
    @SingleIn(AppScope::class)
    @Provides
    fun provideSupabaseClient(environmentProvider: EnvironmentProvider): SupabaseClient {
        // The client is bound to the env at first injection — switching env at runtime requires
        // an app restart, which the dev-settings UI prompts for. FAKE falls back to PROD URL
        // because we still need a valid client to construct; remote calls are gated by sign-in
        // (the env switch signs the user out), so seeded FAKE data stays local until someone
        // signs back in.
        val (url, key) =
            when (environmentProvider.environment.value) {
                Environment.TESTING ->
                    BuildConfig.SUPABASE_TESTING_URL to BuildConfig.SUPABASE_TESTING_KEY
                Environment.PROD,
                Environment.FAKE -> BuildConfig.SUPABASE_PROD_URL to BuildConfig.SUPABASE_PROD_KEY
            }
        return createSupabaseClient(supabaseUrl = url, supabaseKey = key) {
            install(Auth)
            install(Postgrest)
            install(Realtime)
            install(Storage)
            install(Functions)
        }
    }
}
