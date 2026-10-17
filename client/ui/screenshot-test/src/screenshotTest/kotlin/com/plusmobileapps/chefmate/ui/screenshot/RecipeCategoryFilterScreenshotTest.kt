package com.plusmobileapps.chefmate.ui.screenshot

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.plusmobileapps.chefmate.recipe.data.BuiltinCategory
import com.plusmobileapps.chefmate.recipe.data.Category
import com.plusmobileapps.chefmate.recipe.list.RecipeSortOption
import com.plusmobileapps.chefmate.recipe.list.SortFilterSheetContent
import com.plusmobileapps.chefmate.recipe.list.impl.ui.previewRecipeListBlocCategoryFiltered
import com.plusmobileapps.chefmate.ui.Content
import com.plusmobileapps.chefmate.ui.components.PlusResponsiveModalDialogSurface
import com.plusmobileapps.chefmate.ui.theme.ChefMateTheme

// Snapshot coverage for the category filter feature:
// 1. Recipe list with an active category filter — verifies the filter-icon badge shows the
//    active count and tints the icon, signalling that filters are applied.
// 2. The sort/filter sheet body with one category pre-selected — locks in the layout of the
//    new "Category" section alongside Sort + Filter.
//
// We render `SortFilterSheetContent` directly (it's been extracted out of the ModalBottomSheet
// wrapper) because Dialog-based composables don't render reliably under the screenshot plugin.

// region Active filter indicator on the list

@PreviewTest
@Preview(showBackground = true, heightDp = 1100)
@Composable
fun RecipeListCategoryFilteredLightScreenshot() {
    ChefMateTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            previewRecipeListBlocCategoryFiltered.Content()
        }
    }
}

@PreviewTest
@Preview(showBackground = true, heightDp = 1100, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun RecipeListCategoryFilteredDarkScreenshot() {
    ChefMateTheme(darkTheme = true) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            previewRecipeListBlocCategoryFiltered.Content()
        }
    }
}

// endregion

// region Filter sheet body

@PreviewTest
@Preview(showBackground = true, heightDp = 700)
@Composable
fun SortFilterSheetCategorySelectedLightScreenshot() {
    ChefMateTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            SortFilterSheetContent(
                initialSort = RecipeSortOption.RECENTLY_ADDED,
                initialFilters = emptySet(),
                initialCategories = setOf(BuiltinCategory.BREAKFAST),
                onApply = { _, _, _, _ -> },
            )
        }
    }
}

@PreviewTest
@Preview(showBackground = true, heightDp = 700, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun SortFilterSheetCategorySelectedDarkScreenshot() {
    ChefMateTheme(darkTheme = true) {
        Surface(color = MaterialTheme.colorScheme.surface) {
            SortFilterSheetContent(
                initialSort = RecipeSortOption.RECENTLY_ADDED,
                initialFilters = emptySet(),
                initialCategories = setOf(BuiltinCategory.BREAKFAST),
                onApply = { _, _, _, _ -> },
            )
        }
    }
}

// User-created category chips appear after the preset row in the same FlowRow. This snapshot
// locks in the wrap behavior and makes sure the user-category chip selection visually matches
// preset chip selection (no accidental styling drift).
@PreviewTest
@Preview(showBackground = true, heightDp = 800)
@Composable
fun SortFilterSheetWithUserCategoriesLightScreenshot() {
    ChefMateTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            SortFilterSheetContent(
                initialSort = RecipeSortOption.RECENTLY_ADDED,
                initialFilters = emptySet(),
                initialCategories = setOf(BuiltinCategory.BREAKFAST),
                initialUserCategoryIds = setOf(101L),
                availableUserCategories =
                    listOf(
                        Category(id = 100L, name = "Family Favorite"),
                        Category(id = 101L, name = "Weeknight"),
                        Category(id = 102L, name = "Slow Cooker"),
                    ),
                onApply = { _, _, _, _ -> },
            )
        }
    }
}

@PreviewTest
@Preview(showBackground = true, heightDp = 800, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun SortFilterSheetWithUserCategoriesDarkScreenshot() {
    ChefMateTheme(darkTheme = true) {
        Surface(color = MaterialTheme.colorScheme.surface) {
            SortFilterSheetContent(
                initialSort = RecipeSortOption.RECENTLY_ADDED,
                initialFilters = emptySet(),
                initialCategories = setOf(BuiltinCategory.BREAKFAST),
                initialUserCategoryIds = setOf(101L),
                availableUserCategories =
                    listOf(
                        Category(id = 100L, name = "Family Favorite"),
                        Category(id = 101L, name = "Weeknight"),
                        Category(id = 102L, name = "Slow Cooker"),
                    ),
                onApply = { _, _, _, _ -> },
            )
        }
    }
}

// endregion

// region Expanded-width dialog presentation

// On expanded windows PlusResponsiveModal presents the sheet as a centered dialog, and the
// quick filters + categories have moved to the left rail — so only sort is left in it. We render
// PlusResponsiveModalDialogSurface directly because Dialog doesn't snapshot under the plugin.

@PreviewTest
@Preview(showBackground = true, widthDp = 700, heightDp = 320)
@Composable
fun SortFilterDialogSortOnlyLightScreenshot() {
    ChefMateTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            // A Box relaxes the preview's fixed-width constraints the way a real Dialog window
            // does, so the surface's 560.dp cap actually applies.
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                PlusResponsiveModalDialogSurface(title = null, onCloseClick = null) {
                    SortFilterSheetContent(
                        initialSort = RecipeSortOption.TOP_RATED,
                        initialFilters = emptySet(),
                        initialCategories = emptySet(),
                        onApply = { _, _, _, _ -> },
                        showFilters = false,
                    )
                }
            }
        }
    }
}

@PreviewTest
@Preview(
    showBackground = true,
    widthDp = 700,
    heightDp = 320,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
fun SortFilterDialogSortOnlyDarkScreenshot() {
    ChefMateTheme(darkTheme = true) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            // A Box relaxes the preview's fixed-width constraints the way a real Dialog window
            // does, so the surface's 560.dp cap actually applies.
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                PlusResponsiveModalDialogSurface(title = null, onCloseClick = null) {
                    SortFilterSheetContent(
                        initialSort = RecipeSortOption.TOP_RATED,
                        initialFilters = emptySet(),
                        initialCategories = emptySet(),
                        onApply = { _, _, _, _ -> },
                        showFilters = false,
                    )
                }
            }
        }
    }
}

// With no rail on screen (an empty library) the dialog still carries the full sheet.
@PreviewTest
@Preview(showBackground = true, widthDp = 700, heightDp = 700)
@Composable
fun SortFilterDialogFullLightScreenshot() {
    ChefMateTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            // A Box relaxes the preview's fixed-width constraints the way a real Dialog window
            // does, so the surface's 560.dp cap actually applies.
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                PlusResponsiveModalDialogSurface(title = null, onCloseClick = null) {
                    SortFilterSheetContent(
                        initialSort = RecipeSortOption.RECENTLY_ADDED,
                        initialFilters = emptySet(),
                        initialCategories = setOf(BuiltinCategory.BREAKFAST),
                        onApply = { _, _, _, _ -> },
                    )
                }
            }
        }
    }
}

// endregion
