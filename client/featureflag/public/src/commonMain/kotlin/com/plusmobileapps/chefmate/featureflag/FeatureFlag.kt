package com.plusmobileapps.chefmate.featureflag

sealed interface FeatureFlag<T : Any> {
    val key: String
    val defaultValue: T
    val description: String
}

abstract class BooleanFlag(
    override val key: String,
    override val defaultValue: Boolean,
    override val description: String,
) : FeatureFlag<Boolean>

abstract class StringFlag(
    override val key: String,
    override val defaultValue: String,
    override val description: String,
) : FeatureFlag<String>

object FeatureFlagRegistry {
    object CookModeV2 :
        BooleanFlag(
            key = "cook_mode_v2",
            defaultValue = false,
            description = "Opt-in flow for the next iteration of Cook Mode.",
        )

    object HomeBannerText :
        StringFlag(
            key = "home_banner_text",
            defaultValue = "",
            description = "Optional banner copy shown on the home screen. Empty hides the banner.",
        )

    object AiChat :
        BooleanFlag(
            key = "ai_chat",
            defaultValue = false,
            description =
                "Show the AI Chat entry in the More tab and enable the Gemini chat screen.",
        )

    object ScanRecipeFromPhoto :
        BooleanFlag(
            key = "scan_recipe_from_photo",
            defaultValue = false,
            description =
                "Turn the recipe-list add button into a chooser with a \"Scan from photo\" " +
                    "option. When off, the add button opens the blank editor directly.",
        )

    object ManageFamily :
        BooleanFlag(
            key = "manage_family",
            defaultValue = false,
            description = "Show the Manage Family entry in the More tab.",
        )

    val all: List<FeatureFlag<*>> =
        listOf(CookModeV2, HomeBannerText, AiChat, ScanRecipeFromPhoto, ManageFamily)
}
