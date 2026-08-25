@file:OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)

package com.plusmobileapps.chefmate.grocery.categoryrules

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import chefmate.client.grocery.category_rules.public.generated.resources.Res
import chefmate.client.grocery.category_rules.public.generated.resources.grocery_category_rules_category_label
import chefmate.client.grocery.category_rules.public.generated.resources.grocery_category_rules_create_a11y
import chefmate.client.grocery.category_rules.public.generated.resources.grocery_category_rules_create_cancel_a11y
import chefmate.client.grocery.category_rules.public.generated.resources.grocery_category_rules_create_confirm_a11y
import chefmate.client.grocery.category_rules.public.generated.resources.grocery_category_rules_delete
import chefmate.client.grocery.category_rules.public.generated.resources.grocery_category_rules_delete_cancel
import chefmate.client.grocery.category_rules.public.generated.resources.grocery_category_rules_delete_confirm
import chefmate.client.grocery.category_rules.public.generated.resources.grocery_category_rules_delete_message
import chefmate.client.grocery.category_rules.public.generated.resources.grocery_category_rules_delete_title
import chefmate.client.grocery.category_rules.public.generated.resources.grocery_category_rules_empty
import chefmate.client.grocery.category_rules.public.generated.resources.grocery_category_rules_more_a11y
import chefmate.client.grocery.category_rules.public.generated.resources.grocery_category_rules_name_placeholder
import chefmate.client.grocery.category_rules.public.generated.resources.grocery_category_rules_row_summary
import chefmate.client.grocery.category_rules.public.generated.resources.grocery_category_rules_section
import chefmate.client.grocery.category_rules.public.generated.resources.grocery_category_rules_title
import com.plusmobileapps.chefmate.grocery.categoryrules.GroceryCategoryRulesBloc.CreateState
import com.plusmobileapps.chefmate.grocery.categoryrules.GroceryCategoryRulesBloc.DialogState
import com.plusmobileapps.chefmate.grocery.categoryrules.GroceryCategoryRulesBloc.Rule
import com.plusmobileapps.chefmate.grocery.core.displayName
import com.plusmobileapps.chefmate.grocery.data.GroceryCategory
import com.plusmobileapps.chefmate.text.FixedString
import com.plusmobileapps.chefmate.text.PhraseModel
import com.plusmobileapps.chefmate.text.ResourceString
import com.plusmobileapps.chefmate.text.asTextData
import com.plusmobileapps.chefmate.ui.components.PlusDialog
import com.plusmobileapps.chefmate.ui.components.PlusHeaderContainer
import com.plusmobileapps.chefmate.ui.components.PlusHeaderContainerDefaults
import com.plusmobileapps.chefmate.ui.components.PlusHeaderData
import com.plusmobileapps.chefmate.ui.theme.ChefMateTheme
import org.jetbrains.compose.resources.stringResource

@Composable
fun GroceryCategoryRulesScreen(bloc: GroceryCategoryRulesBloc, modifier: Modifier = Modifier) {
    val model by bloc.state.collectAsState()
    GroceryCategoryRulesContent(model = model, handlers = blocHandlers(bloc), modifier = modifier)
}

internal data class GroceryCategoryRulesHandlers(
    val onBackClicked: () -> Unit,
    val onCreateClicked: () -> Unit,
    val onCreateCancelled: () -> Unit,
    val onCreateNameChanged: (String) -> Unit,
    val onCreateCategorySelected: (GroceryCategory) -> Unit,
    val onCreateSubmitted: () -> Unit,
    val onDeleteRequested: (Rule) -> Unit,
    val onDeleteConfirmed: () -> Unit,
    val onDeleteDismissed: () -> Unit,
)

private fun blocHandlers(bloc: GroceryCategoryRulesBloc): GroceryCategoryRulesHandlers =
    GroceryCategoryRulesHandlers(
        onBackClicked = bloc::onBackClicked,
        onCreateClicked = bloc::onCreateClicked,
        onCreateCancelled = bloc::onCreateCancelled,
        onCreateNameChanged = bloc::onCreateNameChanged,
        onCreateCategorySelected = bloc::onCreateCategorySelected,
        onCreateSubmitted = bloc::onCreateSubmitted,
        onDeleteRequested = bloc::onDeleteRequested,
        onDeleteConfirmed = bloc::onDeleteConfirmed,
        onDeleteDismissed = bloc::onDeleteDismissed,
    )

@Composable
internal fun GroceryCategoryRulesContent(
    model: GroceryCategoryRulesBloc.Model,
    handlers: GroceryCategoryRulesHandlers,
    modifier: Modifier = Modifier,
) {
    PlusHeaderContainer(
        modifier = modifier.testTag(GroceryCategoryRulesTestTags.SCREEN),
        data =
            PlusHeaderData.Child(
                title = Res.string.grocery_category_rules_title.asTextData(),
                onBackClick = handlers.onBackClicked,
            ),
        // Disable the container's outer scroll so the inner LazyColumn owns scrolling (Compose
        // throws on a scrollable nested under infinite-height constraints).
        scrollEnabled = false,
        maxContentWidth = Dp.Unspecified,
        horizontalAlignment = Alignment.CenterHorizontally,
        content = {
            CreateRuleRow(
                createState = model.createState,
                onNameChanged = handlers.onCreateNameChanged,
                onCategorySelected = handlers.onCreateCategorySelected,
                onSubmit = handlers.onCreateSubmitted,
                onCancel = handlers.onCreateCancelled,
            )
            RuleList(
                rules = model.rules,
                createOpen = model.createState is CreateState.Editing,
                onCreateClicked = handlers.onCreateClicked,
                onDeleteRequested = handlers.onDeleteRequested,
            )
        },
    )

    when (val dialog = model.dialog) {
        DialogState.None -> Unit
        is DialogState.Delete ->
            PlusDialog(
                title = ResourceString(Res.string.grocery_category_rules_delete_title),
                message =
                    PhraseModel(
                        Res.string.grocery_category_rules_delete_message,
                        "name" to FixedString(dialog.target.name),
                    ),
                confirmButtonText =
                    ResourceString(Res.string.grocery_category_rules_delete_confirm),
                dismissButtonText = ResourceString(Res.string.grocery_category_rules_delete_cancel),
                onConfirmClick = handlers.onDeleteConfirmed,
                onDismissRequest = handlers.onDeleteDismissed,
            )
    }
}

@Composable
private fun CreateRuleRow(
    createState: CreateState,
    onNameChanged: (String) -> Unit,
    onCategorySelected: (GroceryCategory) -> Unit,
    onSubmit: () -> Unit,
    onCancel: () -> Unit,
) {
    AnimatedContent(
        targetState = createState,
        modifier =
            Modifier.widthIn(max = PlusHeaderContainerDefaults.MaxContentWidth).fillMaxWidth(),
        contentKey = { it is CreateState.Editing },
        transitionSpec = { fadeIn() togetherWith fadeOut() },
    ) { state ->
        if (state is CreateState.Editing) {
            val focusRequester = remember { FocusRequester() }
            LaunchedEffect(Unit) { focusRequester.requestFocus() }
            Column(
                modifier =
                    Modifier.fillMaxWidth()
                        .padding(
                            start = ChefMateTheme.dimens.paddingNormal,
                            end = ChefMateTheme.dimens.paddingSmall,
                            top = ChefMateTheme.dimens.paddingSmall,
                        ),
                verticalArrangement = Arrangement.spacedBy(ChefMateTheme.dimens.paddingSmall),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = state.name,
                        onValueChange = onNameChanged,
                        placeholder = {
                            Text(stringResource(Res.string.grocery_category_rules_name_placeholder))
                        },
                        singleLine = true,
                        keyboardOptions =
                            KeyboardOptions(imeAction = ImeAction.Done, autoCorrectEnabled = false),
                        keyboardActions = KeyboardActions(onDone = { onSubmit() }),
                        modifier =
                            Modifier.weight(1f)
                                .focusRequester(focusRequester)
                                .testTag(GroceryCategoryRulesTestTags.CREATE_FIELD),
                    )
                    IconButton(onClick = { if (state.name.isNotBlank()) onSubmit() }) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription =
                                stringResource(
                                    Res.string.grocery_category_rules_create_confirm_a11y
                                ),
                        )
                    }
                    IconButton(onClick = onCancel) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription =
                                stringResource(
                                    Res.string.grocery_category_rules_create_cancel_a11y
                                ),
                        )
                    }
                }
                CategoryDropdown(selected = state.category, onSelected = onCategorySelected)
            }
        } else {
            Box(modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun CategoryDropdown(
    selected: GroceryCategory,
    onSelected: (GroceryCategory) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier.fillMaxWidth().testTag(GroceryCategoryRulesTestTags.CREATE_CATEGORY),
    ) {
        OutlinedTextField(
            value = selected.displayName().localized(),
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(Res.string.grocery_category_rules_category_label)) },
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

@Composable
private fun RuleList(
    rules: List<Rule>,
    createOpen: Boolean,
    onCreateClicked: () -> Unit,
    onDeleteRequested: (Rule) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth().fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        stickyHeader(key = "header") {
            SectionHeader(
                title = stringResource(Res.string.grocery_category_rules_section),
                trailing = {
                    if (!createOpen) {
                        IconButton(
                            onClick = onCreateClicked,
                            modifier = Modifier.testTag(GroceryCategoryRulesTestTags.ADD_BUTTON),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription =
                                    stringResource(Res.string.grocery_category_rules_create_a11y),
                            )
                        }
                    }
                },
            )
        }
        if (rules.isEmpty()) {
            item(key = "empty") { EmptyHint() }
        } else {
            items(rules, key = { "rule-${it.id}" }) { rule ->
                RuleRow(rule = rule, onDeleteClicked = { onDeleteRequested(rule) })
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, trailing: @Composable () -> Unit) {
    Surface(
        modifier =
            Modifier.widthIn(max = PlusHeaderContainerDefaults.MaxContentWidth).fillMaxWidth(),
        color = ChefMateTheme.colorScheme.background,
    ) {
        Row(
            modifier =
                Modifier.fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .padding(start = ChefMateTheme.dimens.paddingNormal),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                style = ChefMateTheme.typography.titleSmall,
                color = ChefMateTheme.colorScheme.primary,
                modifier = Modifier.weight(1f),
            )
            trailing()
        }
    }
}

@Composable
private fun EmptyHint() {
    Box(
        modifier =
            Modifier.widthIn(max = PlusHeaderContainerDefaults.MaxContentWidth)
                .fillMaxWidth()
                .padding(
                    horizontal = ChefMateTheme.dimens.paddingNormal,
                    vertical = ChefMateTheme.dimens.paddingSmall,
                )
    ) {
        Text(
            text = stringResource(Res.string.grocery_category_rules_empty),
            style = MaterialTheme.typography.bodyMedium,
            color = ChefMateTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun RuleRow(rule: Rule, onDeleteClicked: () -> Unit) {
    Row(
        modifier =
            Modifier.widthIn(max = PlusHeaderContainerDefaults.MaxContentWidth)
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .padding(start = ChefMateTheme.dimens.paddingNormal)
                .testTag(GroceryCategoryRulesTestTags.RULE_ROW),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text =
                PhraseModel(
                        Res.string.grocery_category_rules_row_summary,
                        "name" to FixedString(rule.name),
                        "aisle" to rule.category.displayName(),
                    )
                    .localized(),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        RuleOverflowMenu(ruleName = rule.name, onDeleteClicked = onDeleteClicked)
    }
}

@Composable
private fun RuleOverflowMenu(ruleName: String, onDeleteClicked: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription =
                    PhraseModel(
                            Res.string.grocery_category_rules_more_a11y,
                            "name" to FixedString(ruleName),
                        )
                        .localized(),
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(Res.string.grocery_category_rules_delete)) },
                onClick = {
                    expanded = false
                    onDeleteClicked()
                },
            )
        }
    }
}
