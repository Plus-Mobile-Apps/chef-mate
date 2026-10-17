@file:OptIn(ExperimentalMaterial3Api::class)

package com.plusmobileapps.chefmate.grocery.core.detail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
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
import chefmate.client.grocery.core.public.generated.resources.Res
import chefmate.client.grocery.core.public.generated.resources.grocery_detail_aisle_label
import chefmate.client.grocery.core.public.generated.resources.grocery_detail_always_file_here
import chefmate.client.grocery.core.public.generated.resources.grocery_detail_name_label
import chefmate.client.grocery.core.public.generated.resources.grocery_detail_quantity_decrease
import chefmate.client.grocery.core.public.generated.resources.grocery_detail_quantity_increase
import chefmate.client.grocery.core.public.generated.resources.grocery_detail_quantity_label
import chefmate.client.grocery.core.public.generated.resources.grocery_recipe_source
import chefmate.client.grocery.core.public.generated.resources.purchased
import chefmate.client.ui.public.generated.resources.Res as CommonRes
import chefmate.client.ui.public.generated.resources.save
import com.plusmobileapps.chefmate.grocery.core.displayName
import com.plusmobileapps.chefmate.grocery.data.GroceryCategory
import com.plusmobileapps.chefmate.grocery.data.GroceryItem
import com.plusmobileapps.chefmate.grocery.data.GroceryQuantityStepper
import com.plusmobileapps.chefmate.text.FixedString
import com.plusmobileapps.chefmate.text.PhraseModel
import com.plusmobileapps.chefmate.ui.components.PlusLoadingIndicator
import com.plusmobileapps.chefmate.ui.components.PlusStepperTextField
import com.plusmobileapps.chefmate.ui.components.PlusTextField
import com.plusmobileapps.chefmate.ui.theme.ChefMateTheme
import org.jetbrains.compose.resources.stringResource

/**
 * Sheet-shaped detail body for the bottom-sheet overlay rendered from `GroceryListScreen`. Snapshot
 * tests target this directly because the Compose screenshot plugin can't render `ModalBottomSheet`
 * reliably.
 */
@Composable
fun GroceryDetailSheetContent(bloc: GroceryDetailBloc, modifier: Modifier = Modifier) {
    val state by bloc.models.collectAsState()
    val dimens = ChefMateTheme.dimens

    Column(
        modifier =
            modifier
                .testTag(GroceryDetailTestTags.SHEET)
                .fillMaxWidth()
                // Lift the whole sheet above whichever is larger — the keyboard when a field is
                // focused, or the navigation bar when it isn't (the ime inset already includes the
                // nav bar, so the two never stack). Without this the save button sits behind the
                // keyboard with no way to reach it.
                .windowInsetsPadding(WindowInsets.ime.union(WindowInsets.navigationBars))
    ) {
        when (val model = state) {
            is GroceryDetailBloc.Model.Loading ->
                Box(
                    modifier =
                        Modifier.fillMaxWidth()
                            .padding(horizontal = dimens.paddingNormal)
                            .padding(dimens.paddingLarge),
                    contentAlignment = Alignment.Center,
                ) {
                    PlusLoadingIndicator()
                }
            is GroceryDetailBloc.Model.Loaded -> {
                // The fields scroll within whatever height is left once the keyboard has claimed
                // its share; `fill = false` keeps the sheet wrapping its content when there is
                // room to spare. The save button lives outside the scroll so it stays anchored.
                Column(
                    modifier =
                        Modifier.weight(1f, fill = false)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = dimens.paddingNormal),
                    verticalArrangement = Arrangement.spacedBy(dimens.paddingNormal),
                ) {
                    GroceryDetailFields(
                        item = model.item,
                        alwaysFileHere = model.alwaysFileHere,
                        bloc = bloc,
                    )
                }
                Button(
                    onClick = bloc::onSaveClicked,
                    modifier =
                        Modifier.fillMaxWidth()
                            .padding(horizontal = dimens.paddingNormal)
                            .padding(top = dimens.paddingNormal),
                ) {
                    Text(stringResource(CommonRes.string.save))
                }
            }
        }
    }
}

@Composable
private fun GroceryDetailFields(
    item: GroceryItem,
    alwaysFileHere: Boolean,
    bloc: GroceryDetailBloc,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(ChefMateTheme.dimens.paddingSmall),
    ) {
        PlusTextField(
            modifier = Modifier.fillMaxWidth(),
            value = item.displayName,
            onValueChange = bloc::onGroceryNameChanged,
            label = { Text(stringResource(Res.string.grocery_detail_name_label)) },
            singleLine = true,
        )
        // Free-form text, so the field stays editable ("a pinch", "2 bunches"); the buttons only
        // step the leading amount and grey out when there is nothing sensible to step.
        PlusStepperTextField(
            modifier = Modifier.fillMaxWidth().testTag(GroceryDetailTestTags.QUANTITY_STEPPER),
            value = item.quantity.orEmpty(),
            onValueChange = bloc::onGroceryQuantityChanged,
            onDecrement = bloc::onQuantityDecrementClicked,
            onIncrement = bloc::onQuantityIncrementClicked,
            decrementContentDescription =
                stringResource(Res.string.grocery_detail_quantity_decrease),
            incrementContentDescription =
                stringResource(Res.string.grocery_detail_quantity_increase),
            decrementEnabled = GroceryQuantityStepper.canDecrement(item.quantity),
            incrementEnabled = GroceryQuantityStepper.canIncrement(item.quantity),
            label = { Text(stringResource(Res.string.grocery_detail_quantity_label)) },
        )
    }
    AisleDropdown(selected = item.category, onSelected = bloc::onAisleChanged)
    if (item.displayName.isNotBlank()) {
        Row(
            modifier =
                Modifier.fillMaxWidth().testTag(GroceryDetailTestTags.ALWAYS_FILE_HERE).clickable {
                    bloc.onAlwaysFileHereToggled(!alwaysFileHere)
                },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(checked = alwaysFileHere, onCheckedChange = bloc::onAlwaysFileHereToggled)
            Text(
                text =
                    PhraseModel(
                            Res.string.grocery_detail_always_file_here,
                            "name" to FixedString(item.displayName),
                            "aisle" to item.category.displayName(),
                        )
                        .localized(),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
    val recipeName = item.recipeName
    if (recipeName != null) {
        Text(
            text =
                PhraseModel(Res.string.grocery_recipe_source, "recipe" to FixedString(recipeName))
                    .localized(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    Row(
        modifier =
            Modifier.fillMaxWidth().clickable { bloc.onGroceryCheckedChanged(!item.isChecked) },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = item.isChecked, onCheckedChange = bloc::onGroceryCheckedChanged)
        Text(stringResource(Res.string.purchased))
    }
}

@Composable
private fun AisleDropdown(
    selected: GroceryCategory,
    onSelected: (GroceryCategory) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier.fillMaxWidth().testTag(GroceryDetailTestTags.AISLE_DROPDOWN),
    ) {
        OutlinedTextField(
            value = selected.displayName().localized(),
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(Res.string.grocery_detail_aisle_label)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier =
                Modifier.menuAnchor(
                        ExposedDropdownMenuAnchorType.PrimaryNotEditable,
                        enabled = true,
                    )
                    .fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            GroceryCategory.entries.forEach { category ->
                DropdownMenuItem(
                    text = { Text(category.displayName().localized()) },
                    onClick = {
                        onSelected(category)
                        expanded = false
                    },
                )
            }
        }
    }
}
