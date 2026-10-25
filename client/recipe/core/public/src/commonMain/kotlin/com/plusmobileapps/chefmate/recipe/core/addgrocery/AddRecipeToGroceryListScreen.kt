package com.plusmobileapps.chefmate.recipe.core.addgrocery

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import chefmate.client.recipe.core.public.generated.resources.Res
import chefmate.client.recipe.core.public.generated.resources.recipe_add_to_grocery_list
import chefmate.client.recipe.core.public.generated.resources.recipe_add_to_grocery_list_add
import chefmate.client.recipe.core.public.generated.resources.recipe_add_to_grocery_list_family_lists_fallback
import chefmate.client.recipe.core.public.generated.resources.recipe_add_to_grocery_list_no_ingredients
import chefmate.client.recipe.core.public.generated.resources.recipe_add_to_grocery_list_personal_lists
import chefmate.client.recipe.core.public.generated.resources.recipe_add_to_grocery_list_scale
import chefmate.client.recipe.core.public.generated.resources.recipe_add_to_grocery_list_select_list
import chefmate.client.recipe.core.public.generated.resources.recipe_add_to_grocery_list_shared_lists
import com.plusmobileapps.chefmate.grocery.core.list.GroceryDisplayGroup
import com.plusmobileapps.chefmate.grocery.core.list.GroceryDisplayItem
import com.plusmobileapps.chefmate.grocery.core.list.GroceryGroupedList
import com.plusmobileapps.chefmate.recipe.core.ingredients.IngredientScaleSelector
import com.plusmobileapps.chefmate.text.asTextData
import com.plusmobileapps.chefmate.ui.components.PlusHeaderContainer
import com.plusmobileapps.chefmate.ui.components.PlusHeaderData
import com.plusmobileapps.chefmate.ui.components.PlusLoadingIndicator
import com.plusmobileapps.chefmate.ui.components.lastItemFloatingActionButtonSpacer
import com.plusmobileapps.chefmate.ui.theme.ChefMateTheme
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddRecipeToGroceryListScreen(bloc: AddRecipeToGroceryListBloc, modifier: Modifier = Modifier) {
    val state by bloc.state.collectAsState()

    PlusHeaderContainer(
        modifier = modifier.fillMaxSize().testTag(AddRecipeToGroceryListTestTags.SCREEN),
        data =
            PlusHeaderData.Modal(
                title = stringResource(Res.string.recipe_add_to_grocery_list).asTextData(),
                onCloseClick = bloc::onBackClicked,
            ),
        // This screen only ever renders inside the recipe detail's bottom sheet, whose drag handle
        // already accounts for the status bar. Without dropping the top inset the app bar adds a
        // second status-bar gap between the drag handle and the title.
        headerWindowInsets = WindowInsets.displayCutout.only(WindowInsetsSides.Horizontal),
        scrollEnabled = false,
        floatingActionButton = {
            if (!state.isLoading && state.hasSelectedIngredients) {
                ExtendedFloatingActionButton(
                    onClick = bloc::onSaveClicked,
                    modifier = Modifier.testTag(AddRecipeToGroceryListTestTags.ADD_BUTTON),
                ) {
                    if (state.isAdding) {
                        PlusLoadingIndicator(
                            modifier = Modifier.padding(end = ChefMateTheme.dimens.paddingSmall),
                        )
                    }
                    Text(stringResource(Res.string.recipe_add_to_grocery_list_add))
                }
            }
        },
    ) {
        when {
            state.isLoading -> {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    PlusLoadingIndicator()
                }
            }
            state.isEmpty -> {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text(
                        text = stringResource(Res.string.recipe_add_to_grocery_list_no_ingredients),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            else -> {
                GroceryListSelector(
                    groceryLists = state.groceryLists,
                    sections = state.groceryListSections,
                    selectedList = state.selectedGroceryList,
                    onListSelected = bloc::onGroceryListSelected,
                )
                IngredientScaleRow(
                    scale = state.ingredientScale,
                    onScaleChange = bloc::onScaleChanged,
                )
                GroceryGroupedList(
                    groups =
                        state.groupedIngredients.map { group ->
                            GroceryDisplayGroup(
                                category = group.category,
                                items =
                                    group.items.map { item ->
                                        GroceryDisplayItem(
                                            key = item.id,
                                            displayName = item.displayName,
                                            quantity = item.quantity,
                                            isChecked = item.isSelected,
                                        )
                                    },
                            )
                        },
                    onItemClick = { key -> bloc.onIngredientToggled(key as Int) },
                    onCheckedChange = { key -> bloc.onIngredientToggled(key as Int) },
                    modifier = Modifier.weight(1f),
                    footer = { lastItemFloatingActionButtonSpacer() },
                )
            }
        }
    }
}

/**
 * The ingredient-scale row: a label and the shared [IngredientScaleSelector]. The factor is the
 * same per-recipe preference the recipe detail screen and Cook Mode use, so opening this sheet
 * after scaling a recipe adds the scaled amounts without having to pick the factor again.
 */
@Composable
private fun IngredientScaleRow(
    scale: Double,
    onScaleChange: (Double) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = ChefMateTheme.dimens.paddingNormal)
                .padding(top = ChefMateTheme.dimens.paddingExtraSmall),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(Res.string.recipe_add_to_grocery_list_scale),
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.weight(1f),
        )
        IngredientScaleSelector(
            scale = scale,
            onScaleChange = onScaleChange,
            buttonTestTag = AddRecipeToGroceryListTestTags.INGREDIENT_SCALE_BUTTON,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GroceryListSelector(
    groceryLists: List<AddRecipeToGroceryListBloc.GroceryListItem>,
    sections: List<AddRecipeToGroceryListBloc.GroceryListSection>,
    selectedList: AddRecipeToGroceryListBloc.GroceryListItem?,
    onListSelected: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (groceryLists.size <= 1) return
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
        // Disabled rather than readOnly: a read-only field still takes focus, and on iOS a tap on
        // a focused field pops the "Select All" text toolbar over the menu. The anchor handles the
        // tap itself, so the field never needs focus; the colors keep it looking enabled.
        val colors = MaterialTheme.colorScheme
        OutlinedTextField(
            value = selectedList?.name ?: "",
            onValueChange = {},
            enabled = false,
            label = { Text(stringResource(Res.string.recipe_add_to_grocery_list_select_list)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors =
                OutlinedTextFieldDefaults.colors(
                    disabledTextColor = colors.onSurface,
                    disabledBorderColor = if (expanded) colors.primary else colors.outline,
                    disabledLabelColor = if (expanded) colors.primary else colors.onSurfaceVariant,
                    disabledTrailingIconColor = colors.onSurfaceVariant,
                ),
            modifier =
                Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                    .fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            GroceryListSelectorMenuContent(
                sections = sections,
                onListSelected = { id ->
                    onListSelected(id)
                    expanded = false
                },
            )
        }
    }
}

/**
 * Rows of the grocery list picker, grouped under Personal / family / Shared with you headers. The
 * headers only show once there's more than one section, so a user with only personal lists sees a
 * plain list. Public so screenshot tests can render it directly, since the dropdown's popup doesn't
 * snapshot under the plugin.
 */
@Composable
fun ColumnScope.GroceryListSelectorMenuContent(
    sections: List<AddRecipeToGroceryListBloc.GroceryListSection>,
    onListSelected: (Long) -> Unit,
) {
    val showHeaders = sections.size > 1
    sections.forEachIndexed { index, section ->
        if (showHeaders) {
            if (index > 0) HorizontalDivider()
            Text(
                text = section.header.title(),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                // 12dp lines the header up with DropdownMenuItem's own text inset.
                modifier =
                    Modifier.padding(
                            horizontal = 12.dp,
                            vertical = ChefMateTheme.dimens.paddingSmall,
                        )
                        .semantics { heading() },
            )
        }
        section.lists.forEach { list ->
            DropdownMenuItem(text = { Text(list.name) }, onClick = { onListSelected(list.id) })
        }
    }
}

@Composable
private fun AddRecipeToGroceryListBloc.GroceryListSection.Header.title(): String =
    when (this) {
        AddRecipeToGroceryListBloc.GroceryListSection.Header.Personal ->
            stringResource(Res.string.recipe_add_to_grocery_list_personal_lists)
        is AddRecipeToGroceryListBloc.GroceryListSection.Header.Family ->
            name ?: stringResource(Res.string.recipe_add_to_grocery_list_family_lists_fallback)
        AddRecipeToGroceryListBloc.GroceryListSection.Header.SharedWithYou ->
            stringResource(Res.string.recipe_add_to_grocery_list_shared_lists)
    }
