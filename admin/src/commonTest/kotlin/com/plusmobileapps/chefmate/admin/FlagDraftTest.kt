package com.plusmobileapps.chefmate.admin

import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import kotlin.test.Test

class FlagDraftTest {

    @Test
    fun validKeyPassesValidation() {
        val v = FlagDraftValidator.validate(FlagDraft(key = "cook_mode_v2"))
        v.keyError shouldBe null
        v.isValid shouldBe true
    }

    @Test
    fun blankKeyFails() {
        FlagDraftValidator.validate(FlagDraft(key = "")).keyError.shouldNotBeNull()
    }

    @Test
    fun keyWithUppercaseOrDashFails() {
        FlagDraftValidator.validate(FlagDraft(key = "Cook-Mode")).keyError.shouldNotBeNull()
    }

    @Test
    fun rolloutOutOfRangeFails() {
        FlagDraftValidator.validate(FlagDraft(key = "x", rolloutPercent = 150))
            .rolloutError
            .shouldNotBeNull()
        FlagDraftValidator.validate(FlagDraft(key = "x", rolloutPercent = -1))
            .rolloutError
            .shouldNotBeNull()
    }

    @Test
    fun malformedVersionFails() {
        FlagDraftValidator.validate(FlagDraft(key = "x", minVersion = "1.x"))
            .versionError
            .shouldNotBeNull()
    }

    @Test
    fun wellFormedVersionsPass() {
        val v =
            FlagDraftValidator.validate(
                FlagDraft(key = "x", minVersion = "1", maxVersion = "2.3.4"),
            )
        v.versionError shouldBe null
    }

    @Test
    fun boolDraftMapsToTrueFalseStrings() {
        FlagDraft(key = "x", type = FlagType.BOOL, boolValue = true).toFlag().let {
            it.valueType shouldBe AdminFeatureFlag.TYPE_BOOL
            it.value shouldBe "true"
        }
        FlagDraft(key = "x", type = FlagType.BOOL, boolValue = false).toFlag().value shouldBe
            "false"
    }

    @Test
    fun stringDraftKeepsRawValue() {
        FlagDraft(key = "x", type = FlagType.STRING, stringValue = "Welcome!").toFlag().let {
            it.valueType shouldBe AdminFeatureFlag.TYPE_STRING
            it.value shouldBe "Welcome!"
        }
    }

    @Test
    fun emptyPlatformsAndBlankVersionsBecomeNull() {
        val flag =
            FlagDraft(key = "x", platforms = emptySet(), minVersion = " ", maxVersion = "").toFlag()
        flag.platforms shouldBe null
        flag.minVersion shouldBe null
        flag.maxVersion shouldBe null
    }

    @Test
    fun platformsAreSortedForStableStorage() {
        FlagDraft(key = "x", platforms = setOf("ios", "android")).toFlag().platforms shouldBe
            listOf("android", "ios")
    }

    @Test
    fun editingPreservesArchivedAndDoesNotForceEnabled() {
        // Loading an archived, disabled flag into the editor and saving must keep it archived and
        // disabled — archive state is independent of the enabled toggle.
        val original =
            AdminFeatureFlag(
                key = "old_flag",
                valueType = AdminFeatureFlag.TYPE_BOOL,
                value = "false",
                enabled = false,
                archived = true,
            )
        val roundTripped = FlagDraft.from(original).toFlag()
        roundTripped.archived shouldBe true
        roundTripped.enabled shouldBe false
        roundTripped.key shouldBe "old_flag"
    }

    @Test
    fun userIdsTextParsesAcrossCommasNewlinesAndWhitespace() {
        val flag = FlagDraft(key = "x", userIdsText = "  uid-1,uid-2\nuid-3  uid-1\n").toFlag()
        // Trimmed, split on commas/newlines/spaces, blanks dropped, duplicates removed (order
        // kept).
        flag.userIds shouldBe listOf("uid-1", "uid-2", "uid-3")
    }

    @Test
    fun blankUserIdsBecomeNull() {
        FlagDraft(key = "x", userIdsText = "   \n  ").toFlag().userIds shouldBe null
    }

    @Test
    fun userIdsRoundTripThroughEditor() {
        val original =
            AdminFeatureFlag(
                key = "x",
                valueType = AdminFeatureFlag.TYPE_BOOL,
                value = "true",
                enabled = true,
                userIds = listOf("uid-a", "uid-b"),
            )
        FlagDraft.from(original).toFlag().userIds shouldBe listOf("uid-a", "uid-b")
    }

    @Test
    fun fromMarksExistingFlagAsNotNew() {
        FlagDraft.from(
                AdminFeatureFlag(key = "k", valueType = "string", value = "v", enabled = true),
            )
            .isNew shouldBe false
    }

    @Test
    fun platformOptionsMatchTheClientPlatformEnum() {
        // FeatureFlagEvaluator compares against Platform.name (ANDROID / IOS / JVM).
        FlagDraft.ALL_PLATFORMS shouldBe listOf("android", "ios", "jvm")
    }

    @Test
    fun fromLowercasesPlatformsSoChipsMatch() {
        val flag =
            AdminFeatureFlag(
                key = "k",
                valueType = "bool",
                value = "true",
                enabled = true,
                platforms = listOf("ANDROID", "Jvm"),
            )
        FlagDraft.from(flag).platforms shouldBe setOf("android", "jvm")
    }

    @Test
    fun legacyPlatformsAreKeptAndReportedAsUnknown() {
        val flag =
            AdminFeatureFlag(
                key = "k",
                valueType = "bool",
                value = "true",
                enabled = true,
                platforms = listOf("web", "ios", "desktop"),
            )
        val draft = FlagDraft.from(flag)
        draft.unknownPlatforms shouldBe listOf("desktop", "web")
        // Never dropped on save: removing them could widen the flag to every platform.
        draft.toFlag().platforms shouldBe listOf("desktop", "ios", "web")
    }

    @Test
    fun unknownOnlyPlatformsGetTheEveryPlatformWarning() {
        unknownPlatformsWarning(listOf("web"), setOf("web")) shouldContain "every platform"
        unknownPlatformsWarning(listOf("web"), setOf("web", "ios")) shouldNotContain
            "every platform"
    }
}
