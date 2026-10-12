package com.plusmobileapps.chefmate.cook.impl.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.plusmobileapps.chefmate.cook.CookModeBloc
import com.plusmobileapps.chefmate.cook.WhatsCookingBloc
import com.plusmobileapps.chefmate.cook.ui.CookModeScreen
import com.plusmobileapps.chefmate.recipe.data.Recipe
import com.plusmobileapps.chefmate.ui.theme.ChefMateTheme
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.MutableStateFlow

val previewWhatsCookingBloc: WhatsCookingBloc =
    object : WhatsCookingBloc {
        override val state =
            MutableStateFlow(
                WhatsCookingBloc.Model(
                    recipes =
                        persistentListOf(
                            WhatsCookingBloc.Model.Item(1L, "Pasta Carbonara", null),
                            WhatsCookingBloc.Model.Item(2L, "Caesar Salad", null),
                        )
                )
            )

        override fun onRecipeClicked(recipeId: Long) = Unit

        override fun onSelectModeToggled() = Unit

        override fun onSelectionToggled(recipeId: Long) = Unit

        override fun onDeleteSelectedClicked() = Unit

        override fun onCloseClicked() = Unit
    }

private fun cookBloc(model: CookModeBloc.Model): CookModeBloc =
    object : CookModeBloc {
        override val state = MutableStateFlow(model)
        override val whatsCookingBloc = previewWhatsCookingBloc

        override fun onCloseClicked() = Unit

        override fun onFinishClicked() = Unit

        override fun onRecipeChipClicked(recipeId: Long) = Unit

        override fun onScaleChanged(scale: Double) = Unit

        override fun onAiChatClicked() = Unit

        override fun onViewRecipeClicked() = Unit

        override fun onPremiumRequiredDismissed() = Unit

        override fun onLayoutToggled() = Unit

        override fun onKeepScreenOnToggled() = Unit

        override fun onCoachMarkDismissed(id: String) = Unit

        override fun onBackClicked() = Unit
    }

private val activeSessionsSample =
    persistentListOf(
        CookModeBloc.Model.Chip(1L, "Pasta Carbonara", isActive = true),
        CookModeBloc.Model.Chip(2L, "Caesar Salad", isActive = false),
    )

// Inline **bold** / _italic_ markdown in the ingredients and directions exercises the rich-text
// rendering on the cook-mode rows (issue: markdown wasn't formatted in cook mode).
private val sampleWithMarkdown =
    Recipe.Sample.copy(
        ingredients =
            "200g **spaghetti**\n100g _guanciale_\n2 large eggs\n50g pecorino romano\n" +
                "Freshly cracked **black pepper**\nSalt",
        directions =
            "Bring a large pot of **salted water** to a boil and cook pasta until _al dente_.\n" +
                "Meanwhile, render the guanciale in a wide pan over medium heat until crisp.\n" +
                "Whisk eggs and pecorino in a bowl with plenty of black pepper.\n" +
                "Drain pasta (reserving a cup of water) and add to the pan _off the heat_.\n" +
                "Pour in the egg mixture and toss vigorously until **silky**.",
    )

val previewCookBlocStacked: CookModeBloc =
    cookBloc(
        CookModeBloc.Model(
            isLoading = false,
            activeRecipe = sampleWithMarkdown,
            activeSessions = activeSessionsSample,
            layoutMode = CookModeBloc.LayoutMode.Stacked,
            keepScreenOn = true,
        )
    )

val previewCookBlocSplit: CookModeBloc =
    cookBloc(previewCookBlocStacked.state.value.copy(layoutMode = CookModeBloc.LayoutMode.Split))

// AI Chat feature flag on for a subscriber: the header surfaces the "Ask AI" button, unlocked.
val previewCookBlocWithAiChat: CookModeBloc =
    cookBloc(previewCookBlocStacked.state.value.copy(showAiChat = true, isSubscribed = true))

// Same, but for a non-subscriber: the button is dimmed and opens the premium upsell.
val previewCookBlocWithLockedAiChat: CookModeBloc =
    cookBloc(previewCookBlocStacked.state.value.copy(showAiChat = true, isSubscribed = false))

val previewCookBlocLoading: CookModeBloc = cookBloc(CookModeBloc.Model(isLoading = true))

val previewCookBlocEmpty: CookModeBloc = cookBloc(CookModeBloc.Model(isLoading = false))

// Recipe with an unusually long title — guards against the header growing past two lines and
// breaking the sticky section header (issue #149).
val previewCookBlocLongTitle: CookModeBloc =
    cookBloc(
        CookModeBloc.Model(
            isLoading = false,
            activeRecipe =
                Recipe.Sample.copy(
                    title =
                        "Grandma's Authentic Sunday Slow-Simmered Pasta Carbonara with " +
                            "Hand-Rolled Spaghetti and Crispy Guanciale"
                ),
            activeSessions = activeSessionsSample,
            layoutMode = CookModeBloc.LayoutMode.Stacked,
            keepScreenOn = true,
        )
    )

// Recipe whose ingredients are grouped into sub-sections (issue #177): the "For the …:" headers
// render bold and are not crossable.
val previewCookBlocSections: CookModeBloc =
    cookBloc(
        CookModeBloc.Model(
            isLoading = false,
            activeRecipe = Recipe.SampleWithSections,
            activeSessions = activeSessionsSample,
            layoutMode = CookModeBloc.LayoutMode.Stacked,
            keepScreenOn = true,
        )
    )

@Preview(showBackground = true, heightDp = 1100)
@Composable
internal fun CookModeSectionsPreview() {
    ChefMateTheme { CookModeScreen(bloc = previewCookBlocSections) }
}

@Preview(showBackground = true, heightDp = 1100)
@Composable
internal fun CookModeStackedPreview() {
    ChefMateTheme { CookModeScreen(bloc = previewCookBlocStacked) }
}

@Preview(showBackground = true, heightDp = 1100, widthDp = 800)
@Composable
internal fun CookModeSplitPreview() {
    ChefMateTheme { CookModeScreen(bloc = previewCookBlocSplit) }
}

@Preview(showBackground = true, heightDp = 1100)
@Composable
internal fun CookModeLoadingPreview() {
    ChefMateTheme { CookModeScreen(bloc = previewCookBlocLoading) }
}

@Preview(showBackground = true, heightDp = 1100)
@Composable
internal fun CookModeEmptyPreview() {
    ChefMateTheme { CookModeScreen(bloc = previewCookBlocEmpty) }
}

@Preview(showBackground = true, heightDp = 1100, widthDp = 800)
@Composable
internal fun CookModeSplitDarkPreview() {
    ChefMateTheme(darkTheme = true) { CookModeScreen(bloc = previewCookBlocSplit) }
}

// Mobile landscape: widthDp stays under 600dp so the responsive container picks the
// COMPACT (mobile) shell with the bottom-sheet peek row.
@Preview(showBackground = true, widthDp = 580, heightDp = 360)
@Composable
internal fun CookModeStackedLandscapePreview() {
    ChefMateTheme { CookModeScreen(bloc = previewCookBlocStacked) }
}

@Preview(showBackground = true, widthDp = 580, heightDp = 360)
@Composable
internal fun CookModeStackedLandscapeDarkPreview() {
    ChefMateTheme(darkTheme = true) { CookModeScreen(bloc = previewCookBlocStacked) }
}

// Tablet/large-phone landscape with the split body — mirrors the configuration in issue #155
// where the body layouts must stay clear of horizontal insets without double-padding the
// cutout side. (Screenshot rendering doesn't inject real display-cutout insets, so this
// preview locks in the no-cutout baseline for the split landscape layout.)
@Preview(showBackground = true, widthDp = 1000, heightDp = 460)
@Composable
internal fun CookModeSplitLandscapePreview() {
    ChefMateTheme { CookModeScreen(bloc = previewCookBlocSplit) }
}

@Preview(showBackground = true, widthDp = 1000, heightDp = 460)
@Composable
internal fun CookModeSplitLandscapeDarkPreview() {
    ChefMateTheme(darkTheme = true) { CookModeScreen(bloc = previewCookBlocSplit) }
}

@Preview(showBackground = true, widthDp = 1000, heightDp = 460)
@Composable
internal fun CookModeStackedLandscapeWidePreview() {
    ChefMateTheme { CookModeScreen(bloc = previewCookBlocStacked) }
}

@Preview(showBackground = true, heightDp = 1100)
@Composable
internal fun CookModeLongTitlePreview() {
    ChefMateTheme { CookModeScreen(bloc = previewCookBlocLongTitle) }
}
