@file:OptIn(ExperimentalFoundationApi::class)

package com.plusmobileapps.chefmate.recipe.core.addmeal

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import chefmate.client.recipe.core.public.generated.resources.Res
import chefmate.client.recipe.core.public.generated.resources.add_meal_plan_breakfast
import chefmate.client.recipe.core.public.generated.resources.add_meal_plan_choose_date
import chefmate.client.recipe.core.public.generated.resources.add_meal_plan_choose_date_and_meal_type
import chefmate.client.recipe.core.public.generated.resources.add_meal_plan_dinner
import chefmate.client.recipe.core.public.generated.resources.add_meal_plan_lunch
import chefmate.client.recipe.core.public.generated.resources.add_meal_plan_meal_type
import chefmate.client.recipe.core.public.generated.resources.add_meal_plan_next
import chefmate.client.recipe.core.public.generated.resources.add_meal_plan_no_meals
import chefmate.client.recipe.core.public.generated.resources.add_meal_plan_save
import chefmate.client.recipe.core.public.generated.resources.add_meal_plan_snacks
import chefmate.client.recipe.core.public.generated.resources.add_meal_plan_today
import chefmate.client.recipe.core.public.generated.resources.meal_plan_picker_add_to
import com.plusmobileapps.chefmate.meal.data.MealPlanItem
import com.plusmobileapps.chefmate.meal.data.MealType
import com.plusmobileapps.chefmate.text.asTextData
import com.plusmobileapps.chefmate.ui.components.PlusHeaderContainer
import com.plusmobileapps.chefmate.ui.components.PlusHeaderContainerDefaults
import com.plusmobileapps.chefmate.ui.components.PlusHeaderData
import com.plusmobileapps.chefmate.ui.components.PlusLoadingIndicator
import com.plusmobileapps.chefmate.ui.components.PlusResponsiveContainer
import com.plusmobileapps.chefmate.ui.components.RecipeImage
import com.plusmobileapps.chefmate.ui.components.WindowSizeClass
import com.plusmobileapps.chefmate.ui.theme.ChefMateTheme
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus
import org.jetbrains.compose.resources.stringResource

@Composable
fun ChooseDateScreen(
    bloc: ChooseDateBloc,
    showAsChild: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val state by bloc.state.collectAsState()

    PlusResponsiveContainer(modifier = modifier.fillMaxSize()) { windowSizeClass ->
        // Past compact widths there's room to pick the meal type beside the calendar, so the
        // separate meal-type step is folded into this screen and Save replaces Next.
        val combined = windowSizeClass != WindowSizeClass.COMPACT
        val title =
            stringResource(
                    if (combined) Res.string.add_meal_plan_choose_date_and_meal_type
                    else Res.string.add_meal_plan_choose_date
                )
                .asTextData()
        val headerData: PlusHeaderData =
            if (showAsChild) {
                PlusHeaderData.Child(title = title, onBackClick = bloc::onBackClicked)
            } else {
                PlusHeaderData.Modal(title = title, onCloseClick = bloc::onBackClicked)
            }

        PlusHeaderContainer(
            modifier = Modifier.fillMaxSize(),
            data = headerData,
            scrollEnabled = false,
            maxContentWidth =
                if (combined) CombinedMaxContentWidth
                else PlusHeaderContainerDefaults.MaxContentWidth,
            floatingActionButton = {
                if (state.selectedDate != null) {
                    if (combined) {
                        ExtendedFloatingActionButton(onClick = bloc::onSaveClicked) {
                            if (state.isSaving) {
                                PlusLoadingIndicator()
                            } else {
                                Text(stringResource(Res.string.add_meal_plan_save))
                            }
                        }
                    } else {
                        ExtendedFloatingActionButton(onClick = bloc::onNextClicked) {
                            Text(stringResource(Res.string.add_meal_plan_next))
                        }
                    }
                }
            },
        ) {
            if (state.familyPlans.isNotEmpty()) {
                Row(
                    modifier =
                        Modifier.fillMaxWidth()
                            .padding(
                                horizontal = ChefMateTheme.dimens.paddingNormal,
                                vertical = ChefMateTheme.dimens.paddingSmall,
                            ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(ChefMateTheme.dimens.paddingSmall),
                ) {
                    Text(
                        text = stringResource(Res.string.meal_plan_picker_add_to),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    MealPlanSelector(
                        families = state.familyPlans,
                        selectedFamilyId = state.selectedFamilyId,
                        expanded = state.isPlanPickerOpen,
                        windowSizeClass = windowSizeClass,
                        onClick = bloc::onPlanSelectorClicked,
                        onDismiss = bloc::onPlanPickerDismissed,
                        onPlanSelected = bloc::onPlanSelected,
                    )
                }
            }
            if (combined) {
                Row(modifier = Modifier.fillMaxWidth().weight(1f)) {
                    ChooseDateContent(
                        state = state,
                        onPreviousMonth = bloc::onPreviousMonth,
                        onNextMonth = bloc::onNextMonth,
                        onTodayClicked = bloc::onTodayClicked,
                        onDaySelected = bloc::onDaySelected,
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                    )
                    MealTypePane(
                        recipeTitle = state.recipeTitle,
                        selectedMealType = state.selectedMealType,
                        onMealTypeSelected = bloc::onMealTypeSelected,
                        modifier =
                            Modifier.width(MealTypePaneWidth)
                                .padding(vertical = ChefMateTheme.dimens.paddingNormal),
                    )
                }
            } else {
                ChooseDateContent(
                    state = state,
                    onPreviousMonth = bloc::onPreviousMonth,
                    onNextMonth = bloc::onNextMonth,
                    onTodayClicked = bloc::onTodayClicked,
                    onDaySelected = bloc::onDaySelected,
                    modifier = Modifier.fillMaxWidth().weight(1f),
                )
            }
        }
    }
}

/** Content cap for the combined layout: the calendar pane plus the meal-type pane beside it. */
private val CombinedMaxContentWidth = 840.dp

private val MealTypePaneWidth = 280.dp

@Composable
private fun MealTypePane(
    recipeTitle: String,
    selectedMealType: MealType,
    onMealTypeSelected: (MealType) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        if (recipeTitle.isNotEmpty()) {
            Text(
                text = recipeTitle,
                style = MaterialTheme.typography.titleMedium,
                modifier =
                    Modifier.padding(
                        horizontal = ChefMateTheme.dimens.paddingNormal,
                        vertical = ChefMateTheme.dimens.paddingSmall,
                    ),
            )
        }
        MealSectionHeader(
            title = stringResource(Res.string.add_meal_plan_meal_type),
            modifier = Modifier.padding(horizontal = ChefMateTheme.dimens.paddingNormal),
        )
        MealTypeOptions(
            selectedMealType = selectedMealType,
            onMealTypeSelected = onMealTypeSelected,
        )
    }
}

@Composable
private fun ChooseDateContent(
    state: ChooseDateBloc.Model,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onTodayClicked: () -> Unit,
    onDaySelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Every row is capped to the calendar's width and centered, so the month navigation, the
    // selected date and the day's meals line up with the grid instead of stretching past it. The
    // list itself stays full-width so it scrolls from the gutters too.
    val rowModifier = Modifier.widthIn(max = ChefMateTheme.dimens.maxCalendarWidth).fillMaxWidth()
    val mealRowModifier = rowModifier.padding(horizontal = ChefMateTheme.dimens.paddingNormal)
    LazyColumn(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = PaddingValues(bottom = ChefMateTheme.dimens.fabClearance),
    ) {
        item(key = "month_nav") {
            Row(
                modifier = rowModifier.padding(horizontal = ChefMateTheme.dimens.paddingSmall),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onPreviousMonth) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = null)
                }
                Text(text = state.monthLabel, style = MaterialTheme.typography.titleMedium)
                IconButton(onClick = onNextMonth) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
                }
            }
        }

        if (state.selectedDate != null) {
            item(key = "selected_date") {
                Row(
                    modifier = rowModifier.padding(horizontal = ChefMateTheme.dimens.paddingNormal),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = state.formattedSelectedDate,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    TextButton(onClick = onTodayClicked) {
                        Text(stringResource(Res.string.add_meal_plan_today))
                    }
                }
            }
        }

        item(key = "calendar") {
            MonthCalendar(
                firstDayOfMonth = state.firstDayOfMonth,
                selectedDate = state.selectedDate,
                daysWithMeals = state.daysWithMeals,
                onDaySelected = onDaySelected,
                modifier = rowModifier.padding(ChefMateTheme.dimens.paddingNormal),
            )
        }

        if (state.selectedDate != null) {
            val meals = state.selectedDayMeals
            if (meals.isEmpty()) {
                item(key = "no_meals") {
                    Box(
                        modifier = rowModifier.height(120.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = stringResource(Res.string.add_meal_plan_no_meals),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else {
                val breakfast = meals.filter { it.mealType == MealType.BREAKFAST }
                val lunch = meals.filter { it.mealType == MealType.LUNCH }
                val dinner = meals.filter { it.mealType == MealType.DINNER }
                val snacks = meals.filter { it.mealType == MealType.SNACKS }

                if (breakfast.isNotEmpty()) {
                    stickyHeader(key = "breakfast") {
                        MealSectionHeader(
                            stringResource(Res.string.add_meal_plan_breakfast),
                            mealRowModifier,
                        )
                    }
                    items(breakfast, key = { it.id }) { meal -> MealRow(meal, mealRowModifier) }
                }
                if (lunch.isNotEmpty()) {
                    stickyHeader(key = "lunch") {
                        MealSectionHeader(
                            stringResource(Res.string.add_meal_plan_lunch),
                            mealRowModifier,
                        )
                    }
                    items(lunch, key = { it.id }) { meal -> MealRow(meal, mealRowModifier) }
                }
                if (dinner.isNotEmpty()) {
                    stickyHeader(key = "dinner") {
                        MealSectionHeader(
                            stringResource(Res.string.add_meal_plan_dinner),
                            mealRowModifier,
                        )
                    }
                    items(dinner, key = { it.id }) { meal -> MealRow(meal, mealRowModifier) }
                }
                if (snacks.isNotEmpty()) {
                    stickyHeader(key = "snacks") {
                        MealSectionHeader(
                            stringResource(Res.string.add_meal_plan_snacks),
                            mealRowModifier,
                        )
                    }
                    items(snacks, key = { it.id }) { meal -> MealRow(meal, mealRowModifier) }
                }
            }
        }
    }
}

@Composable
private fun MealSectionHeader(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier =
            modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .padding(vertical = ChefMateTheme.dimens.paddingSmall),
    )
}

@Composable
private fun MealRow(meal: MealPlanItem, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = ChefMateTheme.dimens.paddingSmall),
        horizontalArrangement = spacedBy(ChefMateTheme.dimens.paddingNormal),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RecipeImage(
            imageUrl = meal.recipeImageUrl,
            contentDescription = meal.recipeTitle,
            modifier = Modifier.size(40.dp),
        )
        Text(text = meal.recipeTitle, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun MonthCalendar(
    firstDayOfMonth: LocalDate,
    selectedDate: LocalDate?,
    daysWithMeals: Set<String>,
    onDaySelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val daysInMonth = firstDayOfMonth.plus(1, DateTimeUnit.MONTH).plus(-1, DateTimeUnit.DAY).day
    val firstDayOffset =
        when (firstDayOfMonth.dayOfWeek) {
            DayOfWeek.SUNDAY -> 0
            DayOfWeek.MONDAY -> 1
            DayOfWeek.TUESDAY -> 2
            DayOfWeek.WEDNESDAY -> 3
            DayOfWeek.THURSDAY -> 4
            DayOfWeek.FRIDAY -> 5
            DayOfWeek.SATURDAY -> 6
        }
    val totalCells = firstDayOffset + daysInMonth
    val rows = (totalCells + 6) / 7

    Column(modifier = modifier, verticalArrangement = spacedBy(4.dp)) {
        Row(modifier = Modifier.fillMaxWidth()) {
            for (dayLabel in listOf("S", "M", "T", "W", "T", "F", "S")) {
                Text(
                    text = dayLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        for (row in 0 until rows) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (col in 0..6) {
                    val cellIndex = row * 7 + col
                    val dayNumber = cellIndex - firstDayOffset + 1
                    if (dayNumber in 1..daysInMonth) {
                        val date = LocalDate(firstDayOfMonth.year, firstDayOfMonth.month, dayNumber)
                        DayCell(
                            day = dayNumber,
                            isSelected = date == selectedDate,
                            hasMeals = date.toString() in daysWithMeals,
                            onClick = { onDaySelected(date) },
                            modifier = Modifier.weight(1f),
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    day: Int,
    isSelected: Boolean,
    hasMeals: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val onPrimaryColor = MaterialTheme.colorScheme.onPrimary
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.clickable(onClick = onClick).padding(vertical = 2.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier =
                Modifier.size(32.dp)
                    .background(
                        color = if (isSelected) primaryColor else Color.Transparent,
                        shape = CircleShape,
                    ),
        ) {
            Text(
                text = day.toString(),
                style = MaterialTheme.typography.bodySmall,
                color = if (isSelected) onPrimaryColor else onSurfaceColor,
                textAlign = TextAlign.Center,
            )
        }
        Box(modifier = Modifier.height(6.dp), contentAlignment = Alignment.Center) {
            if (hasMeals) {
                Box(
                    modifier =
                        Modifier.size(4.dp)
                            .background(
                                color = if (isSelected) onPrimaryColor else primaryColor,
                                shape = CircleShape,
                            )
                )
            }
        }
    }
}
