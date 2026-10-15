package com.plusmobileapps.chefmate.ui.screenshot

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.plusmobileapps.chefmate.browser.previewBrowserLandingBloc
import com.plusmobileapps.chefmate.cook.impl.ui.previewCookBlocSplit
import com.plusmobileapps.chefmate.family.manage.FamilyDetailScreen
import com.plusmobileapps.chefmate.family.manage.previewFamilyDetailOnboardingBloc
import com.plusmobileapps.chefmate.grocery.core.impl.list.ui.previewGroceryListBlocFamily
import com.plusmobileapps.chefmate.grocery.core.list.GroceryListSelectorContent
import com.plusmobileapps.chefmate.meal.core.impl.ui.previewMealPlanBlocFamilyPlan
import com.plusmobileapps.chefmate.recipe.bottomnav.BottomNavBarPreview
import com.plusmobileapps.chefmate.recipe.bottomnav.BottomNavBloc
import com.plusmobileapps.chefmate.recipe.core.detail.RecipeDetailScreen
import com.plusmobileapps.chefmate.recipe.core.detail.previewRecipeDetailBloc
import com.plusmobileapps.chefmate.recipe.list.RecipeBookPickerContent
import com.plusmobileapps.chefmate.recipe.list.impl.ui.previewRecipeListBloc
import com.plusmobileapps.chefmate.recipebook.data.RecipeBook
import com.plusmobileapps.chefmate.toast.LocalToastService
import com.plusmobileapps.chefmate.toast.testing.FakeToastService
import com.plusmobileapps.chefmate.ui.Content
import com.plusmobileapps.chefmate.ui.theme.ChefMateTheme

// Source captures for the onboarding feature previews
// (client/onboarding/public/.../drawable/onboarding_preview_*.png). Each one renders the real
// feature screen with sample data, framed like a phone and dressed the way the app shows it (the
// floating bottom nav on top-level tabs, an open picker sheet). Re-record these after a design
// change, then run scripts/update-onboarding-previews.sh to copy them into the onboarding module.
//
// ModalBottomSheet doesn't render under the screenshot plugin, so the open-picker captures draw
// the sheet chrome (scrim, container, drag handle) by hand around the picker's real content.

@Composable
private fun Capture(darkTheme: Boolean, content: @Composable BoxScope.() -> Unit) {
    CompositionLocalProvider(LocalToastService provides FakeToastService()) {
        ChefMateTheme(darkTheme = darkTheme) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background,
            ) {
                Box(modifier = Modifier.fillMaxSize(), content = content)
            }
        }
    }
}

/** The floating glass bottom nav, pinned over the bottom of a top-level tab's capture. */
@Composable
private fun BoxScope.BottomNav(selectedTab: BottomNavBloc.Tab) {
    Box(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()) {
        BottomNavBarPreview(notificationCount = 0, selectedTab = selectedTab)
    }
}

/** A phone bottom sheet open over the capture, matching Material's ModalBottomSheet defaults. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BoxScope.OpenSheet(content: @Composable ColumnScope.() -> Unit) {
    Box(modifier = Modifier.fillMaxSize().background(BottomSheetDefaults.ScrimColor))
    Surface(
        modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
        shape = BottomSheetDefaults.ExpandedShape,
        color = BottomSheetDefaults.ContainerColor,
        tonalElevation = BottomSheetDefaults.Elevation,
    ) {
        Column {
            Box(modifier = Modifier.align(Alignment.CenterHorizontally)) {
                BottomSheetDefaults.DragHandle()
            }
            content()
        }
    }
}

// ── Add to grocery: recipe detail with its floating toolbar ─────────────────

@Composable
private fun AddToGroceryCapture(darkTheme: Boolean) {
    Capture(darkTheme) { RecipeDetailScreen(bloc = previewRecipeDetailBloc) }
}

@PreviewTest
@Preview(showBackground = true, widthDp = 360, heightDp = 560)
@Composable
fun OnboardingCaptureAddToGroceryLight() {
    AddToGroceryCapture(darkTheme = false)
}

@PreviewTest
@Preview(
    showBackground = true,
    widthDp = 360,
    heightDp = 560,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
fun OnboardingCaptureAddToGroceryDark() {
    AddToGroceryCapture(darkTheme = true)
}

// ── Meal planning: a family meal plan on the Meals tab ──────────────────────

@Composable
private fun MealPlanningCapture(darkTheme: Boolean) {
    Capture(darkTheme) {
        previewMealPlanBlocFamilyPlan.Content()
        BottomNav(BottomNavBloc.Tab.MEALS)
    }
}

@PreviewTest
@Preview(showBackground = true, widthDp = 360, heightDp = 560)
@Composable
fun OnboardingCaptureMealPlanningLight() {
    MealPlanningCapture(darkTheme = false)
}

@PreviewTest
@Preview(
    showBackground = true,
    widthDp = 360,
    heightDp = 560,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
fun OnboardingCaptureMealPlanningDark() {
    MealPlanningCapture(darkTheme = true)
}

// ── Cook mode ────────────────────────────────────────────────────────────────

@Composable
private fun CookModeCapture(darkTheme: Boolean) {
    Capture(darkTheme) { previewCookBlocSplit.Content() }
}

@PreviewTest
@Preview(showBackground = true, widthDp = 360, heightDp = 560)
@Composable
fun OnboardingCaptureCookModeLight() {
    CookModeCapture(darkTheme = false)
}

@PreviewTest
@Preview(
    showBackground = true,
    widthDp = 360,
    heightDp = 560,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
fun OnboardingCaptureCookModeDark() {
    CookModeCapture(darkTheme = true)
}

// ── Recipe books: the book picker open over the recipe list ─────────────────

// Two personal books and two family books — few enough that the family section fits on screen.
private val onboardingRecipeBooks =
    listOf(
        RecipeBook.Sample,
        RecipeBook.Sample.copy(id = 2L, name = "Weeknight Dinners", isDefault = false),
        RecipeBook.Sample.copy(
            id = 6L,
            name = "Sunday Dinners",
            isDefault = false,
            isOwnedByCurrentUser = false,
            familyId = "family-1",
            familyName = "The Smiths",
        ),
        RecipeBook.Sample.copy(
            id = 7L,
            name = "Grandma’s Baking",
            isDefault = false,
            isOwnedByCurrentUser = false,
            familyId = "family-1",
            familyName = "The Smiths",
        ),
    )

@Composable
private fun RecipeBooksCapture(darkTheme: Boolean) {
    Capture(darkTheme) {
        previewRecipeListBloc.Content()
        OpenSheet {
            RecipeBookPickerContent(
                books = onboardingRecipeBooks,
                activeBookId = RecipeBook.Sample.id,
                isAllRecipesSelected = false,
                onBookSelected = {},
                onAllRecipesSelected = {},
                onEditBook = {},
                onCreateBook = {},
            )
        }
    }
}

@PreviewTest
@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Composable
fun OnboardingCaptureRecipeBooksLight() {
    RecipeBooksCapture(darkTheme = false)
}

@PreviewTest
@Preview(
    showBackground = true,
    widthDp = 360,
    heightDp = 640,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
fun OnboardingCaptureRecipeBooksDark() {
    RecipeBooksCapture(darkTheme = true)
}

// ── Grocery lists: the list picker open over a family list ──────────────────

@Composable
private fun GroceryListsCapture(darkTheme: Boolean) {
    val bloc = previewGroceryListBlocFamily
    Capture(darkTheme) {
        bloc.Content()
        OpenSheet {
            GroceryListSelectorContent(
                state = bloc.state.value,
                onDismiss = {},
                onListSelected = {},
                onCreateListClicked = {},
                onEditListClicked = {},
            )
        }
    }
}

@PreviewTest
@Preview(showBackground = true, widthDp = 360, heightDp = 560)
@Composable
fun OnboardingCaptureGroceryListsLight() {
    GroceryListsCapture(darkTheme = false)
}

@PreviewTest
@Preview(
    showBackground = true,
    widthDp = 360,
    heightDp = 560,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
fun OnboardingCaptureGroceryListsDark() {
    GroceryListsCapture(darkTheme = true)
}

// ── Save recipes: the built-in browser on the Browser tab ───────────────────

@Composable
private fun SaveRecipesCapture(darkTheme: Boolean) {
    Capture(darkTheme) {
        previewBrowserLandingBloc.Content()
        BottomNav(BottomNavBloc.Tab.BROWSER)
    }
}

@PreviewTest
@Preview(showBackground = true, widthDp = 360, heightDp = 620)
@Composable
fun OnboardingCaptureSaveRecipesLight() {
    SaveRecipesCapture(darkTheme = false)
}

@PreviewTest
@Preview(
    showBackground = true,
    widthDp = 360,
    heightDp = 620,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
fun OnboardingCaptureSaveRecipesDark() {
    SaveRecipesCapture(darkTheme = true)
}

// ── Family sharing: a family's shared grocery lists and recipe books ────────

@Composable
private fun FamilySharingCapture(darkTheme: Boolean) {
    Capture(darkTheme) { FamilyDetailScreen(bloc = previewFamilyDetailOnboardingBloc) }
}

@PreviewTest
@Preview(showBackground = true, widthDp = 360, heightDp = 720)
@Composable
fun OnboardingCaptureFamilySharingLight() {
    FamilySharingCapture(darkTheme = false)
}

@PreviewTest
@Preview(
    showBackground = true,
    widthDp = 360,
    heightDp = 720,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
fun OnboardingCaptureFamilySharingDark() {
    FamilySharingCapture(darkTheme = true)
}
