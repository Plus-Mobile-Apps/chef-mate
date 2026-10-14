package com.plusmobileapps.chefmate.recipe.core.addmeal

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import chefmate.client.recipe.core.public.generated.resources.Res
import chefmate.client.recipe.core.public.generated.resources.meal_plan_picker_family_plans
import chefmate.client.recipe.core.public.generated.resources.meal_plan_picker_personal_plan
import chefmate.client.recipe.core.public.generated.resources.meal_plan_picker_selector
import com.plusmobileapps.chefmate.meal.data.FamilyPlan
import com.plusmobileapps.chefmate.ui.components.PlusDropdownSelector
import com.plusmobileapps.chefmate.ui.components.WindowSizeClass
import com.plusmobileapps.chefmate.ui.theme.ChefMateTheme
import org.jetbrains.compose.resources.stringResource

/**
 * Pill naming the meal plan in use — "My meals" or a family's shared plan — that opens
 * [MealPlanPickerContent]. Shared by the meal planner and the add-meal flow.
 *
 * @param selectedFamilyId the selected family plan, or null for the personal plan.
 */
@Composable
fun MealPlanSelector(
    families: List<FamilyPlan>,
    selectedFamilyId: String?,
    expanded: Boolean,
    windowSizeClass: WindowSizeClass,
    onClick: () -> Unit,
    onDismiss: () -> Unit,
    onPlanSelected: (familyId: String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    PlusDropdownSelector(
        text =
            families.firstOrNull { it.familyId == selectedFamilyId }?.name
                ?: stringResource(Res.string.meal_plan_picker_personal_plan),
        expanded = expanded,
        windowSizeClass = windowSizeClass,
        onClick = onClick,
        onDismiss = onDismiss,
        iconContentDescription = stringResource(Res.string.meal_plan_picker_selector),
        modifier = modifier.testTag(MealPlanPickerTestTags.SELECTOR),
    ) {
        MealPlanPickerContent(
            families = families,
            selectedFamilyId = selectedFamilyId,
            onPlanSelected = onPlanSelected,
        )
    }
}

/**
 * Body of the meal plan picker: the personal plan, then each family's shared plan. Shown by
 * [PlusDropdownSelector] as a dropdown on wide layouts and a bottom sheet on phones.
 */
@Composable
fun MealPlanPickerContent(
    families: List<FamilyPlan>,
    selectedFamilyId: String?,
    onPlanSelected: (familyId: String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.testTag(MealPlanPickerTestTags.PICKER)) {
        PlanItem(
            name = stringResource(Res.string.meal_plan_picker_personal_plan),
            icon = Icons.Outlined.Person,
            isSelected = selectedFamilyId == null,
            onClick = { onPlanSelected(null) },
            modifier = Modifier.testTag(MealPlanPickerTestTags.PERSONAL),
        )
        HorizontalDivider()
        Text(
            text = stringResource(Res.string.meal_plan_picker_family_plans),
            style = MaterialTheme.typography.titleMedium,
            modifier =
                Modifier.padding(
                    horizontal = ChefMateTheme.dimens.paddingNormal,
                    vertical = ChefMateTheme.dimens.paddingSmall,
                ),
        )
        families.forEach { family ->
            PlanItem(
                name = family.name,
                icon = Icons.Outlined.Group,
                isSelected = family.familyId == selectedFamilyId,
                onClick = { onPlanSelected(family.familyId) },
                modifier = Modifier.testTag(MealPlanPickerTestTags.family(family.familyId)),
            )
        }
    }
}

@Composable
private fun PlanItem(
    name: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val color =
        if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
    ListItem(
        headlineContent = { Text(text = name, color = color) },
        modifier = modifier.clickable(onClick = onClick),
        leadingContent = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = color,
            )
        },
        trailingContent =
            if (isSelected) {
                {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = color,
                    )
                }
            } else {
                null
            },
    )
}
