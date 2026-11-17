@file:Suppress("FunctionName")
@file:OptIn(ExperimentalCoroutinesApi::class)

package com.plusmobileapps.chefmate.featureflag.impl

import com.plusmobileapps.chefmate.auth.data.testing.FakeAuthenticationRepository
import com.plusmobileapps.chefmate.featureflag.FeatureFlagRegistry
import com.plusmobileapps.chefmate.featureflag.Override
import com.russhwolf.settings.MapSettings
import io.kotest.matchers.shouldBe
import kotlin.test.Test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest

class FeatureFlagRepositoryImplTest {

    private val cookFlag = FeatureFlagRegistry.CookModeV2
    private val bannerFlag = FeatureFlagRegistry.HomeBannerText

    @Test
    fun When_no_remote_or_override_Then_default_returned() = runTest {
        val repo = buildRepo(remoteRows = emptyList())

        repo.valueOf(cookFlag).value shouldBe cookFlag.defaultValue
        repo.valueOf(bannerFlag).value shouldBe bannerFlag.defaultValue
    }

    @Test
    fun When_remote_enables_flag_at_full_rollout_Then_value_returned_after_refresh() = runTest {
        val rows =
            listOf(
                FeatureFlagRow(
                    key = cookFlag.key,
                    valueType = "bool",
                    value = "true",
                    enabled = true,
                    rolloutPercent = 100,
                ),
            )
        val repo = buildRepo(remoteRows = rows)

        repo.refresh()

        repo.valueOf(cookFlag).value shouldBe true
    }

    @Test
    fun When_override_force_on_Then_overrides_remote_off() = runTest {
        val (repo, overrides) =
            buildRepoWithOverrides(
                remoteRows =
                    listOf(
                        FeatureFlagRow(
                            key = cookFlag.key,
                            valueType = "bool",
                            value = "true",
                            enabled = false, // remote says disabled
                            rolloutPercent = 100,
                        ),
                    ),
            )
        repo.refresh()
        repo.valueOf(cookFlag).value shouldBe false

        overrides.setOverride(cookFlag, Override.ForceValue(true))

        repo.valueOf(cookFlag).value shouldBe true
    }

    @Test
    fun When_override_cleared_Then_falls_back_to_remote() = runTest {
        val (repo, overrides) =
            buildRepoWithOverrides(
                remoteRows =
                    listOf(
                        FeatureFlagRow(
                            key = bannerFlag.key,
                            valueType = "string",
                            value = "remote-text",
                            enabled = true,
                            rolloutPercent = 100,
                        ),
                    ),
            )
        repo.refresh()

        overrides.setOverride(bannerFlag, Override.ForceValue("local-text"))
        repo.valueOf(bannerFlag).value shouldBe "local-text"

        overrides.setOverride(bannerFlag, Override.Default)

        repo.valueOf(bannerFlag).value shouldBe "remote-text"
    }

    @Test
    fun When_remote_fetch_fails_Then_cached_values_are_kept() = runTest {
        val cache = MapSettings()
        // Pre-populate cache so the repo loads "remote-text" without hitting the network.
        FeatureFlagCache(cache)
            .save(
                listOf(
                    FeatureFlagRow(
                        key = bannerFlag.key,
                        valueType = "string",
                        value = "remote-text",
                        enabled = true,
                        rolloutPercent = 100,
                    ),
                ),
            )
        val repo =
            buildRepo(
                remoteRows = null, // signals throwing remote
                cacheSettings = cache,
            )

        repo.valueOf(bannerFlag).value shouldBe "remote-text"
        repo.refresh() // throws inside repo; should be swallowed
        repo.valueOf(bannerFlag).value shouldBe "remote-text"
    }

    private fun buildRepo(
        remoteRows: List<FeatureFlagRow>?,
        cacheSettings: MapSettings = MapSettings(),
    ): FeatureFlagRepositoryImpl = buildRepoWithOverrides(remoteRows, cacheSettings).first

    private fun buildRepoWithOverrides(
        remoteRows: List<FeatureFlagRow>?,
        cacheSettings: MapSettings = MapSettings(),
    ): Pair<FeatureFlagRepositoryImpl, FeatureFlagOverridesImpl> {
        val overridesSettings = MapSettings()
        val overrides = FeatureFlagOverridesImpl(overridesSettings)
        val authRepo = FakeAuthenticationRepository()
        val identity = BucketingIdentityProvider(authRepo, MapSettings(), Dispatchers.Unconfined)
        val remote = FakeRemote(remoteRows)
        val repo =
            FeatureFlagRepositoryImpl(
                evaluator = FeatureFlagEvaluator(),
                cache = FeatureFlagCache(cacheSettings),
                remote = remote,
                overrides = overrides,
                identityProvider = identity,
            )
        return repo to overrides
    }

    private class FakeRemote(private val rows: List<FeatureFlagRow>?) :
        FeatureFlagRemoteDataSource {
        override suspend fun fetch(): List<FeatureFlagRow> =
            rows ?: throw RuntimeException("network down")
    }
}
