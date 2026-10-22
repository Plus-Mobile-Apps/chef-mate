package com.plusmobileapps.chefmate.recipe.core.addgrocery

import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.unit.dp
import chefmate.client.recipe.core.public.generated.resources.Res
import chefmate.client.recipe.core.public.generated.resources.recipe_add_to_grocery_list
import chefmate.client.recipe.core.public.generated.resources.recipe_add_to_grocery_list_add
import chefmate.client.recipe.core.public.generated.resources.recipe_add_to_grocery_list_no_ingredients
import chefmate.client.recipe.core.public.generated.resources.recipe_add_to_grocery_list_scale
import chefmate.client.recipe.core.public.generated.resources.recipe_add_to_grocery_list_select_list
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
                            modifier = Modifier.padding(end = ChefMateTheme.dimens.paddingSmall)
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
        OutlinedTextField(
            value = selectedList?.name ?: "",
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(Res.string.recipe_add_to_grocery_list_select_list)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier =
                Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                    .fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            groceryLists.forEach { list ->
                DropdownMenuItem(
                    text = { Text(list.name) },
                    onClick = {
                        onListSelected(list.id)
                        expanded = false
                    },
                )
            }
        }
    }
}
