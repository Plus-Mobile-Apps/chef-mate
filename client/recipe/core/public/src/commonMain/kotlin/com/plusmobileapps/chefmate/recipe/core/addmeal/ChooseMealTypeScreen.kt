package com.plusmobileapps.chefmate.recipe.core.addmeal

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import chefmate.client.recipe.core.public.generated.resources.Res
import chefmate.client.recipe.core.public.generated.resources.add_meal_plan_breakfast
import chefmate.client.recipe.core.public.generated.resources.add_meal_plan_choose_meal_type
import chefmate.client.recipe.core.public.generated.resources.add_meal_plan_dinner
import chefmate.client.recipe.core.public.generated.resources.add_meal_plan_lunch
import chefmate.client.recipe.core.public.generated.resources.add_meal_plan_save
import chefmate.client.recipe.core.public.generated.resources.add_meal_plan_snacks
import com.plusmobileapps.chefmate.meal.data.MealType
import com.plusmobileapps.chefmate.text.asTextData
import com.plusmobileapps.chefmate.ui.components.PlusHeaderContainer
import com.plusmobileapps.chefmate.ui.components.PlusHeaderData
import com.plusmobileapps.chefmate.ui.components.PlusLoadingIndicator
import com.plusmobileapps.chefmate.ui.theme.ChefMateTheme
import org.jetbrains.compose.resources.stringResource

@Composable
fun ChooseMealTypeScreen(bloc: ChooseMealTypeBloc, modifier: Modifier = Modifier) {
    val state by bloc.state.collectAsState()

    PlusHeaderContainer(
        modifier = modifier.fillMaxSize(),
        data =
            PlusHeaderData.Child(
                title = stringResource(Res.string.add_meal_plan_choose_meal_type).asTextData(),
                onBackClick = bloc::onBackClicked,
            ),
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = bloc::onSaveClicked) {
                if (state.isSaving) {
                    PlusLoadingIndicator()
                } else {
                    Text(stringResource(Res.string.add_meal_plan_save))
                }
            }
        },
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(ChefMateTheme.dimens.paddingNormal)) {
            if (state.recipeTitle.isNotEmpty()) {
                Text(
                    text = state.recipeTitle,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = ChefMateTheme.dimens.paddingNormal),
                )
            }

            MealTypeOptions(
                selectedMealType = state.selectedMealType,
                onMealTypeSelected = bloc::onMealTypeSelected,
            )
        }
    }
}

/**
 * The meal-type radio list. Shared by [ChooseMealTypeScreen] and the side pane [ChooseDateScreen]
 * shows on wide layouts, where both choices are made on one screen.
 */
@Composable
internal fun MealTypeOptions(
    selectedMealType: MealType,
    onMealTypeSelected: (MealType) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        MealTypeOption.entries.forEach { option ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().clickable { onMealTypeSelected(option.type) },
            ) {
                RadioButton(
                    selected = selectedMealType == option.type,
                    onClick = { onMealTypeSelected(option.type) },
                )
                Text(
                    text = stringResource(option.labelRes),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
    }
}

private enum class MealTypeOption(
    val type: MealType,
    val labelRes: org.jetbrains.compose.resources.StringResource,
) {
    BREAKFAST(MealType.BREAKFAST, Res.string.add_meal_plan_breakfast),
    LUNCH(MealType.LUNCH, Res.string.add_meal_plan_lunch),
    DINNER(MealType.DINNER, Res.string.add_meal_plan_dinner),
    SNACKS(MealType.SNACKS, Res.string.add_meal_plan_snacks),
}
