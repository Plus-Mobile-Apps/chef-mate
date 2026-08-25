package com.plusmobileapps.chefmate.settings.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import chefmate.client.settings.public.generated.resources.Res
import chefmate.client.settings.public.generated.resources.app_settings_backup_section
import chefmate.client.settings.public.generated.resources.app_settings_bottom_nav_order
import chefmate.client.settings.public.generated.resources.app_settings_browser_history_enabled
import chefmate.client.settings.public.generated.resources.app_settings_browser_section
import chefmate.client.settings.public.generated.resources.app_settings_clear_history
import chefmate.client.settings.public.generated.resources.app_settings_clear_history_dialog_cancel
import chefmate.client.settings.public.generated.resources.app_settings_clear_history_dialog_confirm
import chefmate.client.settings.public.generated.resources.app_settings_clear_history_dialog_message
import chefmate.client.settings.public.generated.resources.app_settings_clear_history_dialog_title
import chefmate.client.settings.public.generated.resources.app_settings_default_search_engine
import chefmate.client.settings.public.generated.resources.app_settings_default_search_engine_dialog_title
import chefmate.client.settings.public.generated.resources.app_settings_default_search_engine_none
import chefmate.client.settings.public.generated.resources.app_settings_grocery_autocomplete
import chefmate.client.settings.public.generated.resources.app_settings_grocery_category_rules
import chefmate.client.settings.public.generated.resources.app_settings_grocery_section
import chefmate.client.settings.public.generated.resources.app_settings_navigation_section
import chefmate.client.settings.public.generated.resources.app_settings_recipe_categories
import chefmate.client.settings.public.generated.resources.app_settings_recipes_section
import chefmate.client.settings.public.generated.resources.app_settings_title
import chefmate.client.settings.public.generated.resources.export_recipes
import chefmate.client.settings.public.generated.resources.import_recipes
import com.plusmobileapps.chefmate.browser.SearchEngine
import com.plusmobileapps.chefmate.browser.logo
import com.plusmobileapps.chefmate.settings.AppSettingsBloc
import com.plusmobileapps.chefmate.text.TextData
import com.plusmobileapps.chefmate.text.asTextData
import com.plusmobileapps.chefmate.ui.components.PlusButton
import com.plusmobileapps.chefmate.ui.components.PlusButtonVariant
import com.plusmobileapps.chefmate.ui.components.PlusDialog
import com.plusmobileapps.chefmate.ui.components.PlusDialogScaffold
import com.plusmobileapps.chefmate.ui.components.PlusHeaderContainer
import com.plusmobileapps.chefmate.ui.components.PlusHeaderData
import com.plusmobileapps.chefmate.ui.theme.ChefMateTheme
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun AppSettingsScreen(bloc: AppSettingsBloc, modifier: Modifier = Modifier) {
    val state by bloc.state.collectAsState()

    if (state.showClearHistoryDialog) {
        ClearHistoryDialog(
            onConfirm = bloc::onClearHistoryConfirmed,
            onDismiss = bloc::onClearHistoryDismissed,
        )
    }

    if (state.showSearchEnginePicker) {
        SearchEnginePickerDialog(
            selectedEngine = state.selectedSearchEngine,
            onEngineSelected = bloc::onSearchEngineSelected,
            onDismiss = bloc::onSearchEnginePickerDismissed,
        )
    }

    PlusHeaderContainer(
        modifier = modifier,
        data =
            PlusHeaderData.Child(
                title = Res.string.app_settings_title.asTextData(),
                onBackClick = bloc::onBack,
            ),
        content = {
            SectionHeader(name = Res.string.app_settings_navigation_section.asTextData())
            SettingsRow(
                name = Res.string.app_settings_bottom_nav_order.asTextData(),
                onClick = bloc::onBottomNavOrderClicked,
            )
            HorizontalDivider()
            SectionHeader(name = Res.string.app_settings_browser_section.asTextData())
            DefaultSearchEngineRow(
                selectedEngine = state.selectedSearchEngine,
                onClick = bloc::onDefaultSearchEngineClicked,
            )
            HorizontalDivider()
            HistoryToggleRow(
                name = Res.string.app_settings_browser_history_enabled.asTextData(),
                checked = state.isHistoryEnabled,
                onCheckedChange = bloc::onHistoryEnabledChanged,
            )
            HorizontalDivider()
            SettingsRow(
                name = Res.string.app_settings_clear_history.asTextData(),
                onClick = bloc::onClearHistoryClicked,
            )
            HorizontalDivider()
            SectionHeader(name = Res.string.app_settings_recipes_section.asTextData())
            SettingsRow(
                name = Res.string.app_settings_recipe_categories.asTextData(),
                onClick = bloc::onRecipeCategoriesClicked,
            )
            HorizontalDivider()
            SectionHeader(name = Res.string.app_settings_grocery_section.asTextData())
            SettingsRow(
                name = Res.string.app_settings_grocery_autocomplete.asTextData(),
                onClick = bloc::onGroceryAutocompleteClicked,
            )
            HorizontalDivider()
            SettingsRow(
                name = Res.string.app_settings_grocery_category_rules.asTextData(),
                onClick = bloc::onGroceryCategoryRulesClicked,
            )
            HorizontalDivider()
            SectionHeader(name = Res.string.app_settings_backup_section.asTextData())
            SettingsRow(
                name = Res.string.import_recipes.asTextData(),
                onClick = bloc::onImportRecipesClicked,
            )
            HorizontalDivider()
            SettingsRow(
                name = Res.string.export_recipes.asTextData(),
                onClick = bloc::onExportRecipesClicked,
            )
        },
    )
}

@Composable
private fun ClearHistoryDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    PlusDialog(
        title = Res.string.app_settings_clear_history_dialog_title.asTextData(),
        message = Res.string.app_settings_clear_history_dialog_message.asTextData(),
        confirmButtonText = Res.string.app_settings_clear_history_dialog_confirm.asTextData(),
        dismissButtonText = Res.string.app_settings_clear_history_dialog_cancel.asTextData(),
        onConfirmClick = onConfirm,
        onDismissRequest = onDismiss,
    )
}

@Composable
private fun DefaultSearchEngineRow(
    selectedEngine: SearchEngine?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .height(ChefMateTheme.dimens.rowHeight)
                .clickable { onClick() }
                .padding(horizontal = ChefMateTheme.dimens.paddingNormal),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            stringResource(Res.string.app_settings_default_search_engine),
            style = ChefMateTheme.typography.titleMedium,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (selectedEngine != null) {
                Icon(
                    painter = painterResource(selectedEngine.logo),
                    contentDescription = null,
                    tint = Color.Unspecified,
                    modifier = Modifier.size(ChefMateTheme.dimens.paddingLarge),
                )
                Spacer(Modifier.size(ChefMateTheme.dimens.paddingSmall))
            }
            Text(
                text =
                    selectedEngine?.displayName
                        ?: stringResource(Res.string.app_settings_default_search_engine_none),
                style = ChefMateTheme.typography.bodyMedium,
                color = ChefMateTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SearchEnginePickerDialog(
    selectedEngine: SearchEngine?,
    onEngineSelected: (SearchEngine) -> Unit,
    onDismiss: () -> Unit,
) {
    PlusDialogScaffold(
        onDismissRequest = onDismiss,
        header = {
            Text(stringResource(Res.string.app_settings_default_search_engine_dialog_title))
        },
        content = {
            Column {
                SearchEngine.entries.forEach { engine ->
                    Row(
                        modifier =
                            Modifier.fillMaxWidth()
                                .height(ChefMateTheme.dimens.rowHeight)
                                .selectable(
                                    selected = engine == selectedEngine,
                                    onClick = { onEngineSelected(engine) },
                                ),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = engine == selectedEngine,
                            onClick = { onEngineSelected(engine) },
                        )
                        Icon(
                            painter = painterResource(engine.logo),
                            contentDescription = null,
                            tint = Color.Unspecified,
                            modifier = Modifier.size(ChefMateTheme.dimens.paddingLarge),
                        )
                        Spacer(Modifier.size(ChefMateTheme.dimens.paddingNormal))
                        Text(engine.displayName, style = ChefMateTheme.typography.titleMedium)
                    }
                }
            }
        },
        footer = {
            PlusButton(
                text = Res.string.app_settings_clear_history_dialog_cancel.asTextData(),
                variant = PlusButtonVariant.SECONDARY,
                onClick = onDismiss,
            )
        },
    )
}

@Composable
private fun SectionHeader(name: TextData, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = ChefMateTheme.dimens.paddingNormal),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            name.localized(),
            style = ChefMateTheme.typography.titleSmall,
            color = ChefMateTheme.colorScheme.primary,
            modifier = Modifier.padding(vertical = ChefMateTheme.dimens.paddingSmall),
        )
    }
}

@Composable
private fun HistoryToggleRow(
    name: TextData,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .height(ChefMateTheme.dimens.rowHeight)
                .clickable { onCheckedChange(!checked) }
                .padding(horizontal = ChefMateTheme.dimens.paddingNormal),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(name.localized(), style = ChefMateTheme.typography.titleMedium)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
