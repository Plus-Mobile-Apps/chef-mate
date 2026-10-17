package com.plusmobileapps.chefmate.recipe.core.impl.detail

import androidx.compose.material3.SnackbarDuration
import chefmate.client.recipe.core.public.generated.resources.Res
import chefmate.client.recipe.core.public.generated.resources.recipe_add_to_grocery_list_added
import chefmate.client.recipe.core.public.generated.resources.recipe_add_to_grocery_list_view
import com.arkivanov.decompose.router.slot.ChildSlot
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.arkivanov.decompose.router.slot.childSlot
import com.arkivanov.decompose.router.slot.dismiss
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.backhandler.BackCallback
import com.plusmobileapps.chefmate.BlocContext
import com.plusmobileapps.chefmate.Consumer
import com.plusmobileapps.chefmate.di.AppScope
import com.plusmobileapps.chefmate.di.CoachMarkId
import com.plusmobileapps.chefmate.getViewModel
import com.plusmobileapps.chefmate.mapState
import com.plusmobileapps.chefmate.recipe.core.addgrocery.AddRecipeToGroceryListBloc
import com.plusmobileapps.chefmate.recipe.core.detail.RecipeDetailBloc
import com.plusmobileapps.chefmate.recipe.core.detail.RecipeDetailBloc.Output
import com.plusmobileapps.chefmate.text.FixedString
import com.plusmobileapps.chefmate.text.ResourceString
import com.plusmobileapps.chefmate.toast.ToastService
import com.plusmobileapps.chefmate.util.DateTimeUtil
import com.plusmobileapps.chefmate.util.TimeFormatterUtil
import com.plusmobileapps.metro.extensions.assistedfactory.ContributesAssistedFactory
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedInject
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@AssistedInject
@ContributesAssistedFactory(
    scope = AppScope::class,
    assistedFactory = RecipeDetailBloc.Factory::class,
)
class RecipeDetailBlocImpl(
    @Assisted context: BlocContext,
    @Assisted private val recipeId: Long,
    @Assisted private val output: Consumer<Output>,
    private val viewModelFactory: RecipeDetailViewModel.Factory,
    private val dateTimeUtil: DateTimeUtil,
    private val timeFormatterUtil: TimeFormatterUtil,
    private val addToGroceryList: AddRecipeToGroceryListBloc.Factory,
    private val toastService: ToastService,
) : RecipeDetailBloc, BlocContext by context {
    private val scope = createScope()

    private val viewModel: RecipeDetailViewModel = instanceKeeper.getViewModel {
        viewModelFactory.create(recipeId)
    }

    init {
        scope.launch {
            viewModel.output.collect { viewModelOutput ->
                when (viewModelOutput) {
                    RecipeDetailViewModel.Output.RecipeDeleted -> output.onNext(Output.Finished)
                    is RecipeDetailViewModel.Output.OpenRecipe ->
                        output.onNext(Output.OpenRecipe(viewModelOutput.recipeId))
                }
            }
        }
    }

    private val sheetNavigation = SlotNavigation<SheetConfig>()
    private val sheetRouter =
        childSlot(
            source = sheetNavigation,
            serializer = SheetConfig.serializer(),
            key = "RecipeDetailBloc_Sheet",
            childFactory = ::createSheet,
        )

    override val childSlot: Value<ChildSlot<*, RecipeDetailBloc.Sheet>> = sheetRouter

    private val fullImageNavigation = SlotNavigation<FullImageConfig>()
    private val fullImageRouter =
        childSlot(
            source = fullImageNavigation,
            serializer = FullImageConfig.serializer(),
            key = "RecipeDetailBloc_FullImage",
            childFactory = { config, _ ->
                RecipeDetailBloc.FullImage.Active(
                    imageUrl = config.imageUrl,
                    recipeId = config.recipeId,
                    title = config.title,
                )
            },
        )

    override val fullImageSlot: Value<ChildSlot<*, RecipeDetailBloc.FullImage>> = fullImageRouter

    private val fullImageBackCallback =
        BackCallback(isEnabled = fullImageRouter.value.child != null) {
            fullImageNavigation.dismiss()
        }

    init {
        backHandler.register(fullImageBackCallback)
        fullImageRouter.subscribe { slot -> fullImageBackCallback.isEnabled = slot.child != null }
    }

    override val shareLink = viewModel.shareLink

    override val state: StateFlow<RecipeDetailBloc.Model> =
        viewModel.state.mapState {
            RecipeDetailBloc.Model(
                isLoading = it.isLoading,
                isDeleting = it.isDeleting,
                showDeleteConfirmationDialog = it.showDeleteConfirmationDialog,
                showShareConfirmation = it.showShareConfirmation,
                recipe = it.recipe,
                createdAt = FixedString(dateTimeUtil.formatDateTime(instant = it.recipe.createdAt)),
                updatedAt = FixedString(dateTimeUtil.formatDateTime(instant = it.recipe.updatedAt)),
                formattedPrepTime =
                    it.recipe.prepTime?.let { time -> timeFormatterUtil.formatMinutes(time) },
                formattedCookTime =
                    it.recipe.cookTime?.let { time -> timeFormatterUtil.formatMinutes(time) },
                formattedTotalTime =
                    it.recipe.totalTime?.let { time -> timeFormatterUtil.formatMinutes(time) },
                ingredientScale = it.ingredientScale,
                activeCoachMark = it.activeCoachMark,
                showAiChat = it.showAiChat,
                isSubscribed = it.isSubscribed,
                showPremiumRequiredDialog = it.showPremiumRequiredDialog,
            )
        }

    override fun onEditClicked() {
        output.onNext(Output.EditRecipe(recipeId))
    }

    override fun onDeleteClicked() {
        viewModel.showDeleteConfirmationDialog()
    }

    override fun onDeleteConfirmed() {
        viewModel.confirmDelete()
    }

    override fun onDeleteDismissed() {
        viewModel.dismissDeleteConfirmationDialog()
    }

    override fun onFavoriteToggled() {
        viewModel.dismissCoachMark(CoachMarkId.RECIPE_DETAIL_FAVORITE)
        viewModel.toggleFavorite()
    }

    override fun onAddToGroceryListClicked() {
        viewModel.dismissCoachMark(CoachMarkId.RECIPE_DETAIL_ADD_TO_GROCERY)
        sheetNavigation.activate(SheetConfig.AddToGroceryList(recipeId))
    }

    override fun onImageClicked() {
        val recipe = viewModel.state.value.recipe
        val imageUrl = recipe.imageUrl ?: return
        fullImageNavigation.activate(
            FullImageConfig(imageUrl = imageUrl, recipeId = recipe.id, title = recipe.title)
        )
    }

    override fun onCloseFullImage() {
        fullImageNavigation.dismiss()
    }

    override fun onAddToMealPlanClicked() {
        viewModel.dismissCoachMark(CoachMarkId.RECIPE_DETAIL_ADD_TO_MEAL_PLAN)
        output.onNext(Output.OpenMealPlanner(recipeId))
    }

    override fun onCookModeClicked() {
        viewModel.dismissCoachMark(CoachMarkId.RECIPE_DETAIL_COOK_MODE)
        output.onNext(Output.OpenCookMode(recipeId))
    }

    override fun onAiChatClicked() {
        if (viewModel.requestAiChat()) {
            output.onNext(Output.OpenAiChat(recipeId))
        }
    }

    override fun onPremiumRequiredDismissed() {
        viewModel.dismissPremiumRequiredDialog()
    }

    override fun onCoachMarkDismissed(id: String) {
        viewModel.dismissCoachMark(id)
    }

    override fun onSourceUrlClicked(url: String) {
        output.onNext(Output.OpenUrl(url))
    }

    override fun onRecipeLinkClicked(clientId: String) {
        viewModel.onRecipeLinkClicked(clientId)
    }

    override fun onShareLinkClicked() {
        viewModel.onShareLinkClicked()
    }

    override fun onShareConfirmed() {
        viewModel.onShareConfirmed()
    }

    override fun onShareDismissed() {
        viewModel.onShareDismissed()
    }

    override fun onStopSharingClicked() {
        viewModel.onStopSharing()
    }

    override fun onScaleChanged(scale: Double) {
        viewModel.setScale(scale)
    }

    override fun onDismissSheet() {
        sheetNavigation.dismiss()
    }

    override fun onBackClicked() {
        if (fullImageRouter.value.child != null) {
            fullImageNavigation.dismiss()
        } else {
            output.onNext(Output.Finished)
        }
    }

    private fun createSheet(config: SheetConfig, context: BlocContext): RecipeDetailBloc.Sheet =
        when (config) {
            is SheetConfig.AddToGroceryList ->
                RecipeDetailBloc.Sheet.AddToGroceryList(
                    bloc =
                        addToGroceryList.create(
                            context = context,
                            recipeId = config.recipeId,
                            output = { groceryOutput ->
                                when (groceryOutput) {
                                    AddRecipeToGroceryListBloc.Output.Finished ->
                                        sheetNavigation.dismiss()
                                    AddRecipeToGroceryListBloc.Output.Added -> {
                                        sheetNavigation.dismiss()
                                        toastService.show(
                                            message =
                                                ResourceString(
                                                    Res.string.recipe_add_to_grocery_list_added
                                                ),
                                            actionLabel =
                                                ResourceString(
                                                    Res.string.recipe_add_to_grocery_list_view
                                                ),
                                            duration = SnackbarDuration.Long,
                                            onAction = { output.onNext(Output.OpenGroceryList) },
                                        )
                                    }
                                }
                            },
                        )
                )
        }

    @Serializable
    sealed class SheetConfig {
        data class AddToGroceryList(val recipeId: Long) : SheetConfig()
    }

    @Serializable
    data class FullImageConfig(val imageUrl: String, val recipeId: Long, val title: String)
}
