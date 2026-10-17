package com.plusmobileapps.chefmate.recipe.list

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import chefmate.client.recipe.list.public.generated.resources.Res
import chefmate.client.recipe.list.public.generated.resources.recipe_list_book_all_recipes
import chefmate.client.recipe.list.public.generated.resources.recipe_list_book_create
import chefmate.client.recipe.list.public.generated.resources.recipe_list_book_edit
import chefmate.client.recipe.list.public.generated.resources.recipe_list_book_family_fallback
import chefmate.client.recipe.list.public.generated.resources.recipe_list_book_my_books
import chefmate.client.recipe.list.public.generated.resources.recipe_list_book_shared_badge
import chefmate.client.recipe.list.public.generated.resources.recipe_list_book_shared_with_you
import com.plusmobileapps.chefmate.recipebook.data.RecipeBook
import com.plusmobileapps.chefmate.ui.components.PlusDropdownSelector
import com.plusmobileapps.chefmate.ui.theme.ChefMateTheme
import org.jetbrains.compose.resources.stringResource

/**
 * Body of the recipe-book picker, shown by [PlusDropdownSelector] as a dropdown on tablet/desktop
 * and a bottom sheet on phones. Mirrors the grocery list selector: the cross-book "All recipes"
 * entry, then the user's own books, then the books other people have shared with them, then one
 * section per family the user shares books with.
 */
@Composable
fun RecipeBookPickerContent(
    books: List<RecipeBook>,
    activeBookId: Long?,
    isAllRecipesSelected: Boolean,
    onBookSelected: (Long) -> Unit,
    onAllRecipesSelected: () -> Unit,
    onEditBook: (Long) -> Unit,
    onCreateBook: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.testTag(RecipeListTestTags.BOOK_PICKER)) {
        ListItem(
            headlineContent = {
                Text(
                    text = stringResource(Res.string.recipe_list_book_all_recipes),
                    color = selectionColor(isAllRecipesSelected),
                )
            },
            modifier =
                Modifier.clickable(onClick = onAllRecipesSelected)
                    .testTag(RecipeListTestTags.BOOK_PICKER_ALL_RECIPES),
            leadingContent = {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.MenuBook,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = selectionColor(isAllRecipesSelected),
                )
            },
        )
        HorizontalDivider()
        RecipeBookSections(
            books = books,
            activeBookId = activeBookId,
            onBookSelected = onBookSelected,
            onEditBook = onEditBook,
            onCreateBook = onCreateBook,
        )
    }
}

/**
 * Body of the "Add to book" sheet shown when filing a multi-selection under a book. Groups books
 * the same way as [RecipeBookPickerContent] — "My books", "Shared with you", then one section per
 * family — but without the "All recipes" entry, edit actions, or the create row.
 */
@Composable
fun RecipeBookTargetPickerContent(
    books: List<RecipeBook>,
    onBookSelected: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        RecipeBookSections(
            books = books,
            activeBookId = null,
            onBookSelected = onBookSelected,
            onEditBook = null,
            onCreateBook = null,
        )
    }
}

/**
 * The grouped book sections shared by both pickers. Null [onEditBook] hides the edit actions and
 * null [onCreateBook] hides the create row.
 */
@Composable
private fun RecipeBookSections(
    books: List<RecipeBook>,
    activeBookId: Long?,
    onBookSelected: (Long) -> Unit,
    onEditBook: ((Long) -> Unit)?,
    onCreateBook: (() -> Unit)?,
) {
    val myBooks = books.filter { it.isOwnedByCurrentUser }
    val sharedBooks = books.filter { !it.isOwnedByCurrentUser && !it.isFamily }
    val familyBooks = books.filter { it.isFamily }.groupBy { it.familyId }

    SectionHeader(text = stringResource(Res.string.recipe_list_book_my_books))
    myBooks.forEach { book ->
        RecipeBookPickerItem(
            book = book,
            isSelected = book.id == activeBookId,
            onBookSelected = onBookSelected,
            onEditBook = onEditBook,
        )
    }
    if (onCreateBook != null) {
        HorizontalDivider()
        ListItem(
            headlineContent = { Text(stringResource(Res.string.recipe_list_book_create)) },
            modifier =
                Modifier.clickable(onClick = onCreateBook)
                    .testTag(RecipeListTestTags.BOOK_PICKER_CREATE),
            leadingContent = {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
            },
        )
    }
    if (sharedBooks.isNotEmpty()) {
        HorizontalDivider()
        SectionHeader(
            text = stringResource(Res.string.recipe_list_book_shared_with_you),
            modifier = Modifier.testTag(RecipeListTestTags.BOOK_PICKER_SHARED_HEADER),
        )
        sharedBooks.forEach { book ->
            RecipeBookPickerItem(
                book = book,
                isSelected = book.id == activeBookId,
                onBookSelected = onBookSelected,
                onEditBook = onEditBook,
            )
        }
    }
    // One section per family, titled with the family's name. Family books are managed from
    // Manage Family, so they offer no edit action here.
    familyBooks.values.forEach { familyBookList ->
        HorizontalDivider()
        SectionHeader(
            text =
                familyBookList.first().familyName
                    ?: stringResource(Res.string.recipe_list_book_family_fallback),
            modifier = Modifier.testTag(RecipeListTestTags.BOOK_PICKER_FAMILY_HEADER),
        )
        familyBookList.forEach { book ->
            RecipeBookPickerItem(
                book = book,
                isSelected = book.id == activeBookId,
                onBookSelected = onBookSelected,
                onEditBook = null,
            )
        }
    }
}

@Composable
private fun SectionHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        modifier =
            modifier.padding(
                horizontal = ChefMateTheme.dimens.paddingNormal,
                vertical = ChefMateTheme.dimens.paddingSmall,
            ),
    )
}

@Composable
private fun RecipeBookPickerItem(
    book: RecipeBook,
    isSelected: Boolean,
    onBookSelected: (Long) -> Unit,
    /** Null hides the edit action. */
    onEditBook: ((Long) -> Unit)?,
) {
    ListItem(
        headlineContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = book.name, color = selectionColor(isSelected))
                // A family book sits under its family's header, which already says it's shared.
                if (!book.isOwnedByCurrentUser && !book.isFamily) {
                    Text(
                        text = stringResource(Res.string.recipe_list_book_shared_badge),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = ChefMateTheme.dimens.paddingSmall),
                    )
                }
            }
        },
        modifier = Modifier.fillMaxWidth().clickable { onBookSelected(book.id) },
        trailingContent =
            onEditBook?.let { onEdit ->
                {
                    IconButton(onClick = { onEdit(book.id) }) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = stringResource(Res.string.recipe_list_book_edit),
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            },
    )
}

@Composable
private fun selectionColor(isSelected: Boolean) =
    if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
