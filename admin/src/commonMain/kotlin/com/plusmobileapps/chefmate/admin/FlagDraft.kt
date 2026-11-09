package com.plusmobileapps.chefmate.admin

enum class FlagType {
    BOOL,
    STRING,
}

/**
 * Editable, UI-facing form of a feature flag. Keeps fields as raw editor inputs (strings/sets) and
 * converts to/from [AdminFeatureFlag]. Validation and conversion are pure so they can be unit
 * tested without Compose.
 */
data class FlagDraft(
    val key: String = "",
    val type: FlagType = FlagType.BOOL,
    val boolValue: Boolean = false,
    val stringValue: String = "",
    val enabled: Boolean = false,
    val rolloutPercent: Int = 100,
    val platforms: Set<String> = emptySet(),
    val minVersion: String = "",
    val maxVersion: String = "",
    // Raw editor text for the user-id allowlist: one id per line (commas also accepted). Parsed
    // into the `user_ids` array by [toFlag].
    val userIdsText: String = "",
    val description: String = "",
    val archived: Boolean = false,
    val isNew: Boolean = true,
) {
    /** Selected platform values that no app reports (left over from older rows), sorted. */
    val unknownPlatforms: List<String>
        get() = platforms.filterNot(::isKnownPlatform).sorted()

    companion object {
        // Targetable platforms: the client's `Platform` enum names (ANDROID / IOS / JVM), which
        // FeatureFlagEvaluator.platformMatches compares case-insensitively. Stored lower-case.
        // There is no web client, and the desktop app reports JVM, so older rows that list
        // `desktop` or `web` match no app; the editor surfaces those via [isKnownPlatform].
        val ALL_PLATFORMS = listOf("android", "ios", "jvm")

        fun isKnownPlatform(id: String): Boolean = id.lowercase() in ALL_PLATFORMS

        fun platformLabel(id: String): String =
            when (id.lowercase()) {
                "android" -> "Android"
                "ios" -> "iOS"
                "jvm" -> "Desktop"
                else -> id
            }

        // User ids are separated by commas, newlines, or surrounding whitespace.
        private val USER_ID_SPLIT = Regex("[\\s,]+")

        fun parseUserIds(text: String): List<String> =
            text.split(USER_ID_SPLIT).map { it.trim() }.filter { it.isNotEmpty() }.distinct()

        fun from(flag: AdminFeatureFlag): FlagDraft {
            val isBool = flag.valueType.equals(AdminFeatureFlag.TYPE_BOOL, ignoreCase = true)
            return FlagDraft(
                key = flag.key,
                type = if (isBool) FlagType.BOOL else FlagType.STRING,
                boolValue = flag.value.equals("true", ignoreCase = true),
                stringValue = if (isBool) "" else flag.value,
                enabled = flag.enabled,
                rolloutPercent = flag.rolloutPercent,
                // Lower-cased so chips match regardless of how the row was written; evaluation is
                // case-insensitive, so this doesn't change who gets the flag. Unknown values are
                // kept (dropping one could widen a flag to every platform).
                platforms = flag.platforms?.map { it.lowercase() }?.toSet() ?: emptySet(),
                minVersion = flag.minVersion.orEmpty(),
                maxVersion = flag.maxVersion.orEmpty(),
                userIdsText = flag.userIds?.joinToString("\n").orEmpty(),
                description = flag.description.orEmpty(),
                archived = flag.archived,
                isNew = false,
            )
        }
    }
}

data class FlagValidation(
    val keyError: String? = null,
    val rolloutError: String? = null,
    val versionError: String? = null,
) {
    val isValid: Boolean
        get() = keyError == null && rolloutError == null && versionError == null
}

object FlagDraftValidator {
    // Flag keys are lower snake_case identifiers (matches FeatureFlagRegistry keys like
    // `cook_mode_v2`).
    private val KEY_REGEX = Regex("^[a-z0-9_]+$")
    // Dotted numeric versions, e.g. 1, 1.2, 1.2.3.
    private val VERSION_REGEX = Regex("^\\d+(\\.\\d+)*$")

    fun validate(draft: FlagDraft): FlagValidation =
        FlagValidation(
            keyError =
                when {
                    draft.key.isBlank() -> "Key is required"
                    !KEY_REGEX.matches(draft.key) ->
                        "Use lower-case letters, numbers, and underscores"
                    else -> null
                },
            rolloutError =
                if (draft.rolloutPercent in 0..100) null else "Rollout must be between 0 and 100",
            versionError =
                listOf(draft.minVersion, draft.maxVersion)
                    .firstOrNull { it.isNotBlank() && !VERSION_REGEX.matches(it.trim()) }
                    ?.let { "Versions must look like 1.2.3" },
        )
}

fun FlagDraft.toFlag(): AdminFeatureFlag =
    AdminFeatureFlag(
        key = key.trim(),
        valueType =
            if (type == FlagType.BOOL) AdminFeatureFlag.TYPE_BOOL else AdminFeatureFlag.TYPE_STRING,
        value = if (type == FlagType.BOOL) boolValue.toString() else stringValue,
        enabled = enabled,
        rolloutPercent = rolloutPercent,
        platforms = platforms.takeIf { it.isNotEmpty() }?.sorted(),
        minVersion = minVersion.trim().ifBlank { null },
        maxVersion = maxVersion.trim().ifBlank { null },
        userIds = FlagDraft.parseUserIds(userIdsText).takeIf { it.isNotEmpty() },
        archived = archived,
        description = description.trim().ifBlank { null },
    )

/**
 * Explains why [unknown] platform values matter. When they're the only ones selected the flag
 * reaches nobody, and removing them all would leave the list empty, which means *every* platform,
 * so that case gets a stronger warning.
 */
fun unknownPlatformsWarning(unknown: List<String>, selected: Set<String>): String {
    val names = unknown.joinToString(", ")
    return if (selected.all { !FlagDraft.isKnownPlatform(it) }) {
        "No app reports $names, so this flag currently reaches nobody. Removing " +
            (if (unknown.size == 1) "it" else "them") +
            " with nothing else selected targets every platform, so select the platforms you " +
            "mean first. Desktop is the desktop app."
    } else {
        "No app reports $names, so " +
            (if (unknown.size == 1) "it has" else "they have") +
            " no effect and can be removed. Desktop is the desktop app."
    }
}
