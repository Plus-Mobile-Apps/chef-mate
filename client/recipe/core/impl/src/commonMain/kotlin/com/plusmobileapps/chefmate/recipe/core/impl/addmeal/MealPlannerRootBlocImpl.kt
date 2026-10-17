package com.plusmobileapps.chefmate.recipe.core.impl.addmeal

import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.router.stack.pushNew
import com.arkivanov.decompose.value.Value
import com.plusmobileapps.chefmate.BlocContext
import com.plusmobileapps.chefmate.Consumer
import com.plusmobileapps.chefmate.di.AppScope
import com.plusmobileapps.chefmate.recipe.core.addmeal.ChooseDateBloc
import com.plusmobileapps.chefmate.recipe.core.addmeal.ChooseMealTypeBloc
import com.plusmobileapps.chefmate.recipe.core.addmeal.MealPlannerRootBloc
import com.plusmobileapps.chefmate.recipe.core.addmeal.MealPlannerRootBloc.Output
import com.plusmobileapps.chefmate.recipe.core.addmeal.RecipePickerBloc
import com.plusmobileapps.metro.extensions.assistedfactory.ContributesAssistedFactory
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedInject
import kotlinx.serialization.Serializable

@AssistedInject
@ContributesAssistedFactory(
    scope = AppScope::class,
    assistedFactory = MealPlannerRootBloc.Factory::class,
)
class MealPlannerRootBlocImpl(
    @Assisted context: BlocContext,
    @Assisted private val props: MealPlannerRootBloc.Props,
    @Assisted private val output: Consumer<Output>,
    private val recipePickerFactory: RecipePickerBloc.Factory,
    private val chooseDateFactory: ChooseDateBloc.Factory,
    private val chooseMealTypeFactory: ChooseMealTypeBloc.Factory,
) : MealPlannerRootBloc, BlocContext by context {

    private val navigation = StackNavigation<Configuration>()
    private val stack =
        childStack(
            source = navigation,
            serializer = Configuration.serializer(),
            initialStack = {
                when (props) {
                    is MealPlannerRootBloc.Props.FromRecipeDetail ->
                        listOf(Configuration.ChooseDate(recipeId = props.recipeId))
                    is MealPlannerRootBloc.Props.FromMealPlanner ->
                        listOf(Configuration.RecipePicker)
                }
            },
            handleBackButton = true,
            key = "MealPlannerRootRouter",
            childFactory = ::createChild,
        )

    override val routerState: Value<ChildStack<*, MealPlannerRootBloc.Child>> = stack

    override fun onBackClicked() {
        if (stack.value.backStack.isNotEmpty()) {
            navigation.pop()
        } else {
            output.onNext(Output.Finished)
        }
    }

    private fun createChild(
        config: Configuration,
        context: BlocContext,
    ): MealPlannerRootBloc.Child =
        when (config) {
            Configuration.RecipePicker ->
                MealPlannerRootBloc.Child.RecipePicker(
                    bloc =
                        recipePickerFactory.create(
                            context = context,
                            output = { pickerOutput ->
                                when (pickerOutput) {
                                    is RecipePickerBloc.Output.RecipeSelected ->
                                        navigation.pushNew(
                                            Configuration.ChooseDate(pickerOutput.recipeId)
                                        )
                                }
                            },
                        )
                )
            is Configuration.ChooseDate ->
                MealPlannerRootBloc.Child.ChooseDate(
                    bloc =
                        chooseDateFactory.create(
                            context = context,
                            recipeId = config.recipeId,
                            output = { dateOutput ->
                                when (dateOutput) {
                                    is ChooseDateBloc.Output.DateSelected ->
                                        navigation.pushNew(
                                            Configuration.ChooseMealType(
                                                recipeId = config.recipeId,
                                                date = dateOutput.date.toString(),
                                            )
                                        )
                                    ChooseDateBloc.Output.Finished -> output.onNext(Output.Finished)
                                    ChooseDateBloc.Output.Back -> onBackClicked()
                                }
                            },
                        )
                )
            is Configuration.ChooseMealType ->
                MealPlannerRootBloc.Child.ChooseMealType(
                    bloc =
                        chooseMealTypeFactory.create(
                            context = context,
                            recipeId = config.recipeId,
                            date = config.date,
                            output = { mealTypeOutput ->
                                when (mealTypeOutput) {
                                    ChooseMealTypeBloc.Output.Finished ->
                                        output.onNext(Output.Finished)
                                    ChooseMealTypeBloc.Output.Back -> navigation.pop()
                                }
                            },
                        )
                )
        }

    @Serializable
    sealed class Configuration {
        @Serializable data object RecipePicker : Configuration()

        @Serializable data class ChooseDate(val recipeId: Long) : Configuration()

        @Serializable
        data class ChooseMealType(val recipeId: Long, val date: String) : Configuration()
    }
}
