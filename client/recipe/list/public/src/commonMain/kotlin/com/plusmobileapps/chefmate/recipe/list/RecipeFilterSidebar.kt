package com.plusmobileapps.chefmate.recipe.list

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import chefmate.client.recipe.list.public.generated.resources.Res
import chefmate.client.recipe.list.public.generated.resources.recipe_list_clear_filters
import chefmate.client.recipe.list.public.generated.resources.recipe_list_filter_by
import chefmate.client.recipe.list.public.generated.resources.recipe_list_filter_by_category
import com.plusmobileapps.chefmate.recipe.data.BuiltinCategory
import com.plusmobileapps.chefmate.recipe.data.Category
import com.plusmobileapps.chefmate.ui.theme.ChefMateTheme
import org.jetbrains.compose.resources.stringResource

/**
 * Width of the filter rail. Wide enough for the longest quick-filter label ("Quick (under 30 min)")
 * on one line, narrow enough to leave the list the majority of an expanded window.
 */
private val SidebarWidth = 260.dp

/**
 * Always-visible filter rail shown to the left of the recipe list on
 * [com.plusmobileapps.chefmate.ui.components.WindowSizeClass.EXPANDED] widths. Unlike the sort &
 * filter sheet it has no "Apply" step — every toggle takes effect immediately, which is the point
 * of surfacing it permanently. The same options are hidden from the sheet at this width so there's
 * only ever one place to change them.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RecipeFilterSidebar(
    activeFilters: Set<RecipeFilterOption>,
    activeCategories: Set<BuiltinCategory>,
    activeUserCategoryIds: Set<Long>,
    availableUserCategories: List<Category>,
    onFilterToggled: (RecipeFilterOption) -> Unit,
    onCategoryToggled: (BuiltinCategory) -> Unit,
    onUserCategoryToggled: (Long) -> Unit,
    onClearFilters: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dimens = ChefMateTheme.dimens
    val anyFilterActive =
        activeFilters.isNotEmpty() ||
            activeCategories.isNotEmpty() ||
            activeUserCategoryIds.isNotEmpty()

    Row(modifier = modifier.width(SidebarWidth).fillMaxHeight()) {
        Column(
            modifier =
                Modifier.weight(1f)
                    .fillMaxHeight()
                    .testTag(RecipeListTestTags.FILTER_SIDEBAR)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = dimens.paddingNormal, vertical = dimens.paddingSmall)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(Res.string.recipe_list_filter_by),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (anyFilterActive) {
                    TextButton(onClick = onClearFilters) {
                        Text(stringResource(Res.string.recipe_list_clear_filters))
                    }
                }
            }
            Spacer(Modifier.height(dimens.paddingSmall))
            RecipeFilterOption.entries.forEach { filter ->
                SidebarFilterRow(
                    label = stringResource(filter.labelRes()),
                    icon = filter.icon(),
                    selected = filter in activeFilters,
                    onClick = { onFilterToggled(filter) },
                )
            }

            Spacer(Modifier.height(dimens.paddingNormal))
            Text(
                text = stringResource(Res.string.recipe_list_filter_by_category),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(dimens.paddingSmall))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(dimens.paddingSmall),
                verticalArrangement = Arrangement.spacedBy(dimens.paddingExtraSmall),
            ) {
                BuiltinCategory.entries.forEach { category ->
                    FilterChip(
                        selected = category in activeCategories,
                        onClick = { onCategoryToggled(category) },
                        label = { Text(stringResource(category.labelRes())) },
                    )
                }
                // Skip user categories that shadow a preset (by builtinId) so the same label isn't
                // offered twice — matches the sheet's chip list.
                availableUserCategories
                    .filter { it.builtinId == null }
                    .forEach { category ->
                        FilterChip(
                            selected = category.id in activeUserCategoryIds,
                            onClick = { onUserCategoryToggled(category.id) },
                            label = { Text(category.name) },
                        )
                    }
            }
            Spacer(Modifier.height(dimens.paddingNormal))
        }
        VerticalDivider()
    }
}

@Composable
private fun SidebarFilterRow(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val dimens = ChefMateTheme.dimens
    Row(
        modifier =
            Modifier.fillMaxWidth()
                .clip(ChefMateTheme.shapes.large)
                .toggleable(value = selected, role = Role.Checkbox, onValueChange = { onClick() })
                .background(
                    if (selected) MaterialTheme.colorScheme.secondaryContainer
                    else Color.Transparent
                )
                .padding(horizontal = dimens.paddingSmall, vertical = dimens.paddingSmall),
        horizontalArrangement = Arrangement.spacedBy(dimens.paddingSmall),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val contentColor =
            if (selected) MaterialTheme.colorScheme.onSecondaryContainer
            else MaterialTheme.colorScheme.onSurfaceVariant
        Icon(imageVector = icon, contentDescription = null, tint = contentColor)
        Text(text = label, style = MaterialTheme.typography.bodyLarge, color = contentColor)
    }
}

private fun RecipeFilterOption.icon(): ImageVector =
    when (this) {
        RecipeFilterOption.FAVORITES -> Icons.Default.Favorite
        RecipeFilterOption.RATED -> Icons.Default.Star
        RecipeFilterOption.QUICK_RECIPES -> Icons.Outlined.AccessTime
    }
