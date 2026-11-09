package com.plusmobileapps.chefmate.recipe.core.impl.root

import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.bringToFront
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.navigate
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.value.Value
import com.plusmobileapps.chefmate.BlocContext
import com.plusmobileapps.chefmate.Consumer
import com.plusmobileapps.chefmate.di.AppScope
import com.plusmobileapps.chefmate.recipe.core.addmeal.MealPlannerRootBloc
import com.plusmobileapps.chefmate.recipe.core.detail.RecipeDetailBloc
import com.plusmobileapps.chefmate.recipe.core.edit.EditRecipeBloc
import com.plusmobileapps.chefmate.recipe.core.root.RecipeRootBloc
import com.plusmobileapps.chefmate.recipe.data.ExtractedRecipeData
import com.plusmobileapps.metro.extensions.assistedfactory.ContributesAssistedFactory
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedInject
import kotlinx.serialization.Serializable

@AssistedInject
@ContributesAssistedFactory(
    scope = AppScope::class,
    assistedFactory = RecipeRootBloc.Factory::class,
)
class RecipeRootBlocImpl(
    @Assisted context: BlocContext,
    @Assisted private val props: RecipeRootBloc.Props,
    @Assisted private val output: Consumer<RecipeRootBloc.Output>,
    private val detailBloc: RecipeDetailBloc.Factory,
    private val editBloc: EditRecipeBloc.Factory,
) : RecipeRootBloc, BlocContext by context {
    private val navigation = StackNavigation<Configuration>()
    private val stack =
        childStack(
            source = navigation,
            serializer = Configuration.serializer(),
            initialStack = {
                when (props) {
                    is RecipeRootBloc.Props.Detail ->
                        listOf(
                            Configuration.Detail(
                                recipeId = props.recipeId,
                                initialTitle = props.initialTitle,
                            ),
                        )
                    is RecipeRootBloc.Props.Create ->
                        listOf(Configuration.Edit(recipeId = null, extracted = null))
                    is RecipeRootBloc.Props.CreateFromExtracted ->
                        listOf(
                            Configuration.Edit(
                                recipeId = null,
                                extracted = props.extracted,
                                fromAi = props.fromAi,
                                consumePendingPhoto = props.consumePendingPhoto,
                            ),
                        )
                }
            },
            handleBackButton = true,
            key = "RecipeRootRouter",
            childFactory = ::createChild,
        )

    override val routerState: Value<ChildStack<*, RecipeRootBloc.Child>> = stack

    override fun onBackClicked() {
        navigation.pop()
    }

    private fun createChild(config: Configuration, context: BlocContext): RecipeRootBloc.Child =
        when (config) {
            is Configuration.Detail ->
                RecipeRootBloc.Child.Detail(
                    bloc =
                        detailBloc.create(
                            context = context,
                            recipeId = config.recipeId,
                            initialTitle = config.initialTitle,
                            output = ::handleDetailOutput,
                        ),
                )
            is Configuration.Edit ->
                RecipeRootBloc.Child.Edit(
                    bloc =
                        editBloc.create(
                            context = context,
                            recipeId = config.recipeId,
                            extractedRecipe = config.extracted,
                            fromAi = config.fromAi,
                            consumePendingPhoto = config.consumePendingPhoto,
                            output = ::handleEditOutput,
                        ),
                )
        }

    private fun handleEditOutput(output: EditRecipeBloc.Output) {
        when (output) {
            EditRecipeBloc.Output.Cancelled -> {
                if (
                    props is RecipeRootBloc.Props.Create ||
                        props is RecipeRootBloc.Props.CreateFromExtracted
                ) {
                    this.output.onNext(RecipeRootBloc.Output.Finished)
                } else {
                    navigation.pop()
                }
            }
            is EditRecipeBloc.Output.Finished -> {
                navigation.navigate { listOf(Configuration.Detail(output.recipeId)) }
            }
        }
    }

    private fun handleDetailOutput(output: RecipeDetailBloc.Output) {
        when (output) {
            RecipeDetailBloc.Output.Finished -> {
                this.output.onNext(RecipeRootBloc.Output.Finished)
            }
            is RecipeDetailBloc.Output.EditRecipe -> {
                navigation.bringToFront(Configuration.Edit(output.recipeId, extracted = null))
            }
            is RecipeDetailBloc.Output.OpenRecipe -> {
                // Bring the linked recipe's detail to the front — reusing an existing entry when
                // the
                // recipe is already in the stack (e.g. A → B → A) so links can't grow it unbounded.
                navigation.bringToFront(Configuration.Detail(output.recipeId))
            }
            is RecipeDetailBloc.Output.OpenUrl -> {
                this.output.onNext(RecipeRootBloc.Output.OpenUrl(output.url))
            }
            RecipeDetailBloc.Output.OpenGroceryList -> {
                this.output.onNext(RecipeRootBloc.Output.OpenGroceryList)
            }
            is RecipeDetailBloc.Output.OpenMealPlanner -> {
                this.output.onNext(
                    RecipeRootBloc.Output.OpenMealPlanner(
                        MealPlannerRootBloc.Props.FromRecipeDetail(output.recipeId),
                    ),
                )
            }
            is RecipeDetailBloc.Output.OpenCookMode -> {
                this.output.onNext(RecipeRootBloc.Output.OpenCookMode(output.recipeId))
            }
            is RecipeDetailBloc.Output.OpenAiChat -> {
                this.output.onNext(RecipeRootBloc.Output.OpenAiChat(output.recipeId))
            }
        }
    }

    @Serializable
    sealed class Configuration {
        /** Equal by [recipeId] alone, so `bringToFront` dedupes regardless of [initialTitle]. */
        @Serializable
        data class Detail(val recipeId: Long, val initialTitle: String? = null) : Configuration() {
            override fun equals(other: Any?): Boolean =
                other is Detail && other.recipeId == recipeId

            override fun hashCode(): Int = recipeId.hashCode()
        }

        @Serializable
        data class Edit(
            val recipeId: Long?,
            val extracted: ExtractedRecipeData?,
            val fromAi: Boolean = false,
            val consumePendingPhoto: Boolean = false,
        ) : Configuration()
    }
}
