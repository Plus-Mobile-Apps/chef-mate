package com.plusmobileapps.chefmate.ui.screenshot

import android.content.res.Configuration
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.plusmobileapps.chefmate.recipe.list.RecipeBookPickerContent
import com.plusmobileapps.chefmate.recipe.list.RecipeBookTargetPickerContent
import com.plusmobileapps.chefmate.recipe.list.impl.ui.previewRecipeBooksWithFamily
import com.plusmobileapps.chefmate.recipe.list.impl.ui.previewRecipeBooksWithShared
import com.plusmobileapps.chefmate.recipebook.data.RecipeBook
import com.plusmobileapps.chefmate.ui.theme.ChefMateTheme

// Snapshot coverage for the recipe-book picker body. It renders inside a ModalBottomSheet on
// phones and a DropdownMenu on tablet/desktop, neither of which renders under the screenshot
// test plugin, so we render `RecipeBookPickerContent` directly.

@Composable
private fun RecipeBookPickerScreenshot(
    books: List<RecipeBook>,
    activeBookId: Long?,
    isAllRecipesSelected: Boolean = false,
    darkTheme: Boolean = false,
) {
    ChefMateTheme(darkTheme = darkTheme) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
            RecipeBookPickerContent(
                books = books,
                activeBookId = activeBookId,
                isAllRecipesSelected = isAllRecipesSelected,
                onBookSelected = {},
                onAllRecipesSelected = {},
                onEditBook = {},
                onCreateBook = {},
            )
        }
    }
}

// ── Owned books plus a "Shared with you" section ───────────────────────────

@PreviewTest
@Preview(showBackground = true, heightDp = 700)
@Composable
fun RecipeBookPickerSharedLightScreenshot() {
    RecipeBookPickerScreenshot(
        books = previewRecipeBooksWithShared,
        activeBookId = RecipeBook.Sample.id,
    )
}

@PreviewTest
@Preview(showBackground = true, heightDp = 700, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun RecipeBookPickerSharedDarkScreenshot() {
    RecipeBookPickerScreenshot(
        books = previewRecipeBooksWithShared,
        activeBookId = RecipeBook.Sample.id,
        darkTheme = true,
    )
}

// ── No shared books — the shared section is omitted entirely ───────────────

@PreviewTest
@Preview(showBackground = true, heightDp = 700)
@Composable
fun RecipeBookPickerOwnedOnlyLightScreenshot() {
    RecipeBookPickerScreenshot(books = RecipeBook.Samples, activeBookId = RecipeBook.Sample.id)
}

// ── "All recipes" active — cross-book view highlighted, no book selected ───

@PreviewTest
@Preview(showBackground = true, heightDp = 700)
@Composable
fun RecipeBookPickerAllRecipesLightScreenshot() {
    RecipeBookPickerScreenshot(
        books = previewRecipeBooksWithShared,
        activeBookId = null,
        isAllRecipesSelected = true,
    )
}

// ── Family books — one section per family, no edit action ──────────────────

@PreviewTest
@Preview(showBackground = true, heightDp = 900)
@Composable
fun RecipeBookPickerFamilyLightScreenshot() {
    RecipeBookPickerScreenshot(books = previewRecipeBooksWithFamily, activeBookId = 6L)
}

@PreviewTest
@Preview(showBackground = true, heightDp = 900, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun RecipeBookPickerFamilyDarkScreenshot() {
    RecipeBookPickerScreenshot(
        books = previewRecipeBooksWithFamily,
        activeBookId = 6L,
        darkTheme = true,
    )
}

// ── Bulk "Add to book" sheet body — same grouping, no All recipes/edit/create ─

@Composable
private fun RecipeBookTargetPickerScreenshot(darkTheme: Boolean = false) {
    ChefMateTheme(darkTheme = darkTheme) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
            RecipeBookTargetPickerContent(books = previewRecipeBooksWithFamily, onBookSelected = {})
        }
    }
}

@PreviewTest
@Preview(showBackground = true, heightDp = 800)
@Composable
fun RecipeBookTargetPickerLightScreenshot() {
    RecipeBookTargetPickerScreenshot()
}

@PreviewTest
@Preview(showBackground = true, heightDp = 800, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun RecipeBookTargetPickerDarkScreenshot() {
    RecipeBookTargetPickerScreenshot(darkTheme = true)
}
