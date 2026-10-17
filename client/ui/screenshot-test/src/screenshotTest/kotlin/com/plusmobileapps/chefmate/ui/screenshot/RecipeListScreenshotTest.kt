package com.plusmobileapps.chefmate.ui.screenshot

import android.content.res.Configuration
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.plusmobileapps.chefmate.recipe.list.RecipeListBloc
import com.plusmobileapps.chefmate.recipe.list.impl.ui.previewRecipeListBloc
import com.plusmobileapps.chefmate.recipe.list.impl.ui.previewRecipeListBlocAllRecipes
import com.plusmobileapps.chefmate.recipe.list.impl.ui.previewRecipeListBlocCooking
import com.plusmobileapps.chefmate.recipe.list.impl.ui.previewRecipeListBlocFilterSidebar
import com.plusmobileapps.chefmate.recipe.list.impl.ui.previewRecipeListBlocPendingInvite
import com.plusmobileapps.chefmate.recipe.list.impl.ui.previewRecipeListBlocScanError
import com.plusmobileapps.chefmate.recipe.list.impl.ui.previewRecipeListBlocScanning
import com.plusmobileapps.chefmate.recipe.list.impl.ui.previewRecipeListBlocSearchEmpty
import com.plusmobileapps.chefmate.recipe.list.impl.ui.previewRecipeListBlocSelectionMode
import com.plusmobileapps.chefmate.recipe.list.impl.ui.previewRecipeListBlocSelectionModeEmpty
import com.plusmobileapps.chefmate.ui.Content
import com.plusmobileapps.chefmate.ui.theme.ChefMateTheme

// RecipeListScreen has no top-level Surface — in production its background comes from the app
// shell (Scaffold / PlusNavRailHeaderContainer). Wrap each test in one so dark snapshots actually
// render dark instead of inheriting the default white screenshot canvas.
@Composable
private fun RecipeListScreenshot(bloc: RecipeListBloc, darkTheme: Boolean = false) {
    ChefMateTheme(darkTheme = darkTheme) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            bloc.Content()
        }
    }
}

// ── Phone portrait (360 × 1100 dp) ─────────────────────────────────────────

@PreviewTest
@Preview(showBackground = true, heightDp = 1100)
@Composable
fun RecipeListPhonePortraitLightScreenshot() {
    RecipeListScreenshot(bloc = previewRecipeListBloc)
}

@PreviewTest
@Preview(showBackground = true, heightDp = 1100, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun RecipeListPhonePortraitDarkScreenshot() {
    RecipeListScreenshot(bloc = previewRecipeListBloc, darkTheme = true)
}

// ── "All recipes" cross-book selector + search-empty broaden action ────────

@PreviewTest
@Preview(showBackground = true, heightDp = 1100)
@Composable
fun RecipeListAllRecipesLightScreenshot() {
    RecipeListScreenshot(bloc = previewRecipeListBlocAllRecipes)
}

@PreviewTest
@Preview(showBackground = true, heightDp = 1100)
@Composable
fun RecipeListSearchEmptyLightScreenshot() {
    RecipeListScreenshot(bloc = previewRecipeListBlocSearchEmpty)
}

@PreviewTest
@Preview(showBackground = true, heightDp = 1100, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun RecipeListSearchEmptyDarkScreenshot() {
    RecipeListScreenshot(bloc = previewRecipeListBlocSearchEmpty, darkTheme = true)
}

// ── Cooking-session FAB stack — regression coverage for #150 ───────────────
// The Continue/Done Cooking FAB stack used to double-apply navigationBars +
// displayCutout insets and render off the bottom edge of its container.

@PreviewTest
@Preview(showBackground = true, heightDp = 1100)
@Composable
fun RecipeListCookingPhonePortraitLightScreenshot() {
    RecipeListScreenshot(bloc = previewRecipeListBlocCooking)
}

@PreviewTest
@Preview(showBackground = true, heightDp = 1100, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun RecipeListCookingPhonePortraitDarkScreenshot() {
    RecipeListScreenshot(bloc = previewRecipeListBlocCooking, darkTheme = true)
}

// Wider-canvas (landscape, tablet) cooking-FAB variants intentionally omitted: the FAB stack
// uses Modifier.align(BottomEnd) + 16.dp padding regardless of canvas size — there's no
// responsive-layout branching to cover, and the FAB shadow rendered slightly differently
// across Mac arm64 / Linux x64 (1–2 px subpixel AA), causing CI flakiness.

// ── Expanded width — permanent filter rail ────────────────────────────────
// At >= 840 dp the quick filters and categories move out of the sort & filter sheet and into a
// rail down the left edge, applied on tap with no Apply step. The app-bar action narrows to
// sort-only, so the filter badge is gone here by design.

@PreviewTest
@Preview(showBackground = true, widthDp = 1000, heightDp = 900)
@Composable
fun RecipeListFilterSidebarLightScreenshot() {
    RecipeListScreenshot(bloc = previewRecipeListBlocFilterSidebar)
}

@PreviewTest
@Preview(
    showBackground = true,
    widthDp = 1000,
    heightDp = 900,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
fun RecipeListFilterSidebarDarkScreenshot() {
    RecipeListScreenshot(bloc = previewRecipeListBlocFilterSidebar, darkTheme = true)
}

// ── Multi-select mode ──────────────────────────────────────────────────────

@PreviewTest
@Preview(showBackground = true, heightDp = 1100)
@Composable
fun RecipeListSelectionLightScreenshot() {
    RecipeListScreenshot(bloc = previewRecipeListBlocSelectionMode)
}

@PreviewTest
@Preview(showBackground = true, heightDp = 1100, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun RecipeListSelectionDarkScreenshot() {
    RecipeListScreenshot(bloc = previewRecipeListBlocSelectionMode, darkTheme = true)
}

@PreviewTest
@Preview(showBackground = true, heightDp = 1100)
@Composable
fun RecipeListSelectionEmptyScreenshot() {
    RecipeListScreenshot(bloc = previewRecipeListBlocSelectionModeEmpty)
}

// ── Scan-from-photo flow ───────────────────────────────────────────────────

@PreviewTest
@Preview(showBackground = true, heightDp = 1100)
@Composable
fun RecipeListScanningLightScreenshot() {
    RecipeListScreenshot(bloc = previewRecipeListBlocScanning)
}

@PreviewTest
@Preview(showBackground = true, heightDp = 1100, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun RecipeListScanningDarkScreenshot() {
    RecipeListScreenshot(bloc = previewRecipeListBlocScanning, darkTheme = true)
}

@PreviewTest
@Preview(showBackground = true, heightDp = 1100)
@Composable
fun RecipeListScanErrorLightScreenshot() {
    RecipeListScreenshot(bloc = previewRecipeListBlocScanError)
}

// ── Pending recipe-book invite banner (accept / decline) ───────────────────

@PreviewTest
@Preview(showBackground = true, heightDp = 1100)
@Composable
fun RecipeListPendingInviteLightScreenshot() {
    RecipeListScreenshot(bloc = previewRecipeListBlocPendingInvite)
}

@PreviewTest
@Preview(showBackground = true, heightDp = 1100, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun RecipeListPendingInviteDarkScreenshot() {
    RecipeListScreenshot(bloc = previewRecipeListBlocPendingInvite, darkTheme = true)
}
