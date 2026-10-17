@file:Suppress("FunctionName")
@file:OptIn(ExperimentalTime::class)

package com.plusmobileapps.chefmate.recipe.core.impl.detail

import chefmate.client.recipe.core.public.generated.resources.Res
import chefmate.client.recipe.core.public.generated.resources.recipe_detail_linked_recipe_not_found
import chefmate.client.recipe.core.public.generated.resources.recipe_detail_share_sign_in_required
import com.plusmobileapps.chefmate.BlocContext
import com.plusmobileapps.chefmate.Consumer
import com.plusmobileapps.chefmate.di.CoachMarkController
import com.plusmobileapps.chefmate.di.CoachMarkId
import com.plusmobileapps.chefmate.featureflag.FeatureFlagRegistry
import com.plusmobileapps.chefmate.featureflag.testing.FakeFeatureFlags
import com.plusmobileapps.chefmate.recipe.core.addgrocery.AddRecipeToGroceryListBloc
import com.plusmobileapps.chefmate.recipe.core.detail.RecipeDetailBloc
import com.plusmobileapps.chefmate.recipe.data.Recipe
import com.plusmobileapps.chefmate.recipe.data.testing.FakeIngredientScalePreferences
import com.plusmobileapps.chefmate.recipe.data.testing.FakeRecipeRepository
import com.plusmobileapps.chefmate.subscription.testing.FakeSubscriptionRepository
import com.plusmobileapps.chefmate.testing.TestBlocContext
import com.plusmobileapps.chefmate.testing.TestConsumer
import com.plusmobileapps.chefmate.text.FixedString
import com.plusmobileapps.chefmate.text.ResourceString
import com.plusmobileapps.chefmate.toast.testing.FakeToastService
import com.plusmobileapps.chefmate.util.TimeFormatterUtil
import com.plusmobileapps.chefmate.util.testing.FakeDateTimeUtil
import com.russhwolf.settings.MapSettings
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.matcher.any
import dev.mokkery.mock
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlin.test.Test
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class RecipeDetailBlocImplTest {

    private val context = TestBlocContext.create()
    private val output = TestConsumer<RecipeDetailBloc.Output>()
    private val recipes = MutableStateFlow<List<Recipe>>(emptyList())
    private val repository = FakeRecipeRepository(recipes)
    private val toastService = FakeToastService()
    private val scalePreferences = FakeIngredientScalePreferences()

    private val sampleRecipe =
        Recipe(
            id = 7L,
            title = "Pancakes",
            description = null,
            ingredients = "flour\negg",
            directions = "mix\ncook",
            imageUrl = "https://example.com/pancakes.jpg",
            sourceUrl = null,
            servings = 2,
            prepTime = 5,
            cookTime = 10,
            totalTime = 15,
            calories = 300,
            starRating = null,
            isFavorite = false,
            createdAt = Instant.parse("2024-01-01T00:00:00Z"),
            updatedAt = Instant.parse("2024-01-02T00:00:00Z"),
        )

    private lateinit var coachMarkController: CoachMarkController
    private lateinit var subscriptionRepository: FakeSubscriptionRepository

    private fun createBloc(
        recipe: Recipe? = sampleRecipe,
        cookModeTooltipSeen: Boolean = false,
        aiChatEnabled: Boolean = false,
        subscribed: Boolean = false,
    ): RecipeDetailBlocImpl {
        if (recipe != null) recipes.value = listOf(recipe)
        coachMarkController = CoachMarkController(MapSettings())
        if (cookModeTooltipSeen) coachMarkController.dismiss(CoachMarkId.RECIPE_DETAIL_COOK_MODE)
        val featureFlags = FakeFeatureFlags(mapOf(FeatureFlagRegistry.AiChat to aiChatEnabled))
        subscriptionRepository = FakeSubscriptionRepository(subscribed)
        val viewModelFactory = RecipeDetailViewModel.Factory { id ->
            RecipeDetailViewModel(
                recipeId = id,
                mainContext = context.mainContext,
                repository = repository,
                coachMarkController = coachMarkController,
                toastService = toastService,
                scalePreferences = scalePreferences,
                featureFlags = featureFlags,
                subscriptionRepository = subscriptionRepository,
            )
        }
        val dateTimeUtil = FakeDateTimeUtil()
        val timeFormatterUtil =
            mock<TimeFormatterUtil>().also {
                every { it.formatMinutes(any()) } returns FixedString("")
            }
        val groceryFactory =
            object : AddRecipeToGroceryListBloc.Factory {
                override fun create(
                    context: BlocContext,
                    recipeId: Long,
                    output: Consumer<AddRecipeToGroceryListBloc.Output>,
                ): AddRecipeToGroceryListBloc = mock()
            }
        return RecipeDetailBlocImpl(
            context = context,
            recipeId = recipe?.id ?: 1L,
            output = output,
            viewModelFactory = viewModelFactory,
            dateTimeUtil = dateTimeUtil,
            timeFormatterUtil = timeFormatterUtil,
            addToGroceryList = groceryFactory,
            toastService = FakeToastService(),
        )
    }

    @Test
    fun When_onImageClicked_with_imageUrl_Then_full_image_slot_is_active() {
        val bloc = createBloc()
        bloc.onImageClicked()
        val active =
            bloc.fullImageSlot.value.child
                ?.instance
                .shouldBeInstanceOf<RecipeDetailBloc.FullImage.Active>()
        active.imageUrl shouldBe sampleRecipe.imageUrl
        active.recipeId shouldBe sampleRecipe.id
        active.title shouldBe sampleRecipe.title
    }

    @Test
    fun When_onImageClicked_with_null_imageUrl_Then_slot_stays_inactive() {
        val bloc = createBloc(recipe = sampleRecipe.copy(imageUrl = null))
        bloc.onImageClicked()
        bloc.fullImageSlot.value.child shouldBe null
    }

    @Test
    fun When_ai_chat_flag_off_Then_showAiChat_is_false() {
        val bloc = createBloc(aiChatEnabled = false)
        bloc.state.value.showAiChat shouldBe false
    }

    @Test
    fun When_ai_chat_flag_on_Then_showAiChat_is_true() {
        val bloc = createBloc(aiChatEnabled = true)
        bloc.state.value.showAiChat shouldBe true
    }

    @Test
    fun When_subscribed_and_onAiChatClicked_Then_open_ai_chat_output_emitted_with_recipe_id() {
        val bloc = createBloc(aiChatEnabled = true, subscribed = true)

        bloc.onAiChatClicked()

        output.lastValue shouldBe RecipeDetailBloc.Output.OpenAiChat(sampleRecipe.id)
    }

    @Test
    fun When_not_subscribed_and_onAiChatClicked_Then_upsell_shown_and_no_output() {
        val bloc = createBloc(aiChatEnabled = true, subscribed = false)

        bloc.onAiChatClicked()

        bloc.state.value.showPremiumRequiredDialog shouldBe true
        output.values.shouldBeEmpty()
    }

    @Test
    fun When_upsell_dismissed_Then_dialog_hidden() {
        val bloc = createBloc(aiChatEnabled = true, subscribed = false)
        bloc.onAiChatClicked()

        bloc.onPremiumRequiredDismissed()

        bloc.state.value.showPremiumRequiredDialog shouldBe false
    }

    @Test
    fun When_subscription_starts_while_open_Then_state_reflects_it() {
        val bloc = createBloc(aiChatEnabled = true, subscribed = false)
        bloc.state.value.isSubscribed shouldBe false

        subscriptionRepository.setSubscribed(true)

        bloc.state.value.isSubscribed shouldBe true
    }

    @Test
    fun When_not_subscribed_Then_ai_chat_button_still_shown() {
        // The gate is an upsell, not a hide — only the feature flag removes the button.
        val bloc = createBloc(aiChatEnabled = true, subscribed = false)

        bloc.state.value.showAiChat shouldBe true
    }

    @Test
    fun When_onCloseFullImage_Then_slot_is_dismissed() {
        val bloc = createBloc()
        bloc.onImageClicked()
        bloc.onCloseFullImage()
        bloc.fullImageSlot.value.child shouldBe null
    }

    @Test
    fun When_onBackClicked_with_active_overlay_Then_overlay_dismisses_and_no_output() {
        val bloc = createBloc()
        bloc.onImageClicked()
        bloc.onBackClicked()
        bloc.fullImageSlot.value.child shouldBe null
        output.values shouldBe emptyList()
    }

    @Test
    fun When_onBackClicked_with_inactive_overlay_Then_output_finished_emitted() {
        val bloc = createBloc()
        bloc.onBackClicked()
        output.lastValue shouldBe RecipeDetailBloc.Output.Finished
    }

    @Test
    fun When_no_marks_seen_Then_cook_mode_coach_mark_is_active_first() {
        val bloc = createBloc()
        bloc.state.value.activeCoachMark shouldBe CoachMarkId.RECIPE_DETAIL_COOK_MODE
    }

    @Test
    fun When_cook_mode_seen_Then_next_mark_in_sequence_is_active() {
        val bloc = createBloc(cookModeTooltipSeen = true)
        bloc.state.value.activeCoachMark shouldBe CoachMarkId.RECIPE_DETAIL_ADD_TO_GROCERY
    }

    @Test
    fun When_active_coach_mark_dismissed_Then_it_is_seen_and_next_advances() {
        val bloc = createBloc()
        bloc.onCoachMarkDismissed(CoachMarkId.RECIPE_DETAIL_COOK_MODE)
        coachMarkController.hasSeen(CoachMarkId.RECIPE_DETAIL_COOK_MODE) shouldBe true
        bloc.state.value.activeCoachMark shouldBe CoachMarkId.RECIPE_DETAIL_ADD_TO_GROCERY
    }

    @Test
    fun When_onCookModeClicked_Then_its_mark_is_seen_and_output_emitted() {
        val bloc = createBloc()
        bloc.onCookModeClicked()
        coachMarkController.hasSeen(CoachMarkId.RECIPE_DETAIL_COOK_MODE) shouldBe true
        bloc.state.value.activeCoachMark shouldBe CoachMarkId.RECIPE_DETAIL_ADD_TO_GROCERY
        output.lastValue shouldBe RecipeDetailBloc.Output.OpenCookMode(sampleRecipe.id)
    }

    @Test
    fun When_button_tapped_while_another_mark_active_Then_its_mark_is_not_consumed() {
        val bloc = createBloc()
        // Cook mode is active; tapping add-to-grocery must not prematurely consume its tip.
        bloc.onAddToGroceryListClicked()
        coachMarkController.hasSeen(CoachMarkId.RECIPE_DETAIL_ADD_TO_GROCERY) shouldBe false
        bloc.state.value.activeCoachMark shouldBe CoachMarkId.RECIPE_DETAIL_COOK_MODE
    }

    @Test
    fun When_share_link_clicked_on_private_recipe_Then_confirmation_is_shown() {
        val bloc = createBloc(sampleRecipe.copy(isPublic = false))
        bloc.onShareLinkClicked()
        bloc.state.value.showShareConfirmation shouldBe true
    }

    @Test
    fun When_share_confirmed_Then_recipe_is_made_public_and_link_is_emitted() {
        repository.setPublicResult = "abc-remote-id"
        val bloc = createBloc(sampleRecipe.copy(isPublic = false))
        val emitted = mutableListOf<String>()
        val job =
            CoroutineScope(context.mainContext).launch { bloc.shareLink.collect { emitted += it } }

        bloc.onShareLinkClicked()
        bloc.onShareConfirmed()

        bloc.state.value.showShareConfirmation shouldBe false
        repository.lastSetPublic shouldBe (sampleRecipe.id to true)
        emitted shouldBe listOf("https://chefmate.plusmobileapps.com/recipe/abc-remote-id")
        job.cancel()
    }

    @Test
    fun When_share_confirmed_but_not_signed_in_Then_sign_in_toast_is_shown() {
        // A null remote id models "couldn't publish" (e.g. signed out).
        repository.setPublicResult = null
        val bloc = createBloc(sampleRecipe.copy(isPublic = false))
        bloc.onShareLinkClicked()
        bloc.onShareConfirmed()
        toastService.shown shouldBe
            listOf(ResourceString(Res.string.recipe_detail_share_sign_in_required))
    }

    @Test
    fun When_share_link_clicked_on_already_public_recipe_Then_link_emitted_without_confirmation() {
        val bloc = createBloc(sampleRecipe.copy(isPublic = true, remoteId = "already-public"))
        val emitted = mutableListOf<String>()
        val job =
            CoroutineScope(context.mainContext).launch { bloc.shareLink.collect { emitted += it } }

        bloc.onShareLinkClicked()

        bloc.state.value.showShareConfirmation shouldBe false
        emitted shouldBe listOf("https://chefmate.plusmobileapps.com/recipe/already-public")
        job.cancel()
    }

    @Test
    fun When_stop_sharing_clicked_Then_recipe_is_made_private() {
        val bloc = createBloc(sampleRecipe.copy(isPublic = true, remoteId = "already-public"))
        bloc.onStopSharingClicked()
        repository.lastSetPublic shouldBe (sampleRecipe.id to false)
    }

    @Test
    fun When_recipe_has_saved_scale_Then_state_reflects_it() {
        scalePreferences.setScale(sampleRecipe.id, 2.0)
        val bloc = createBloc()
        bloc.state.value.ingredientScale shouldBe 2.0
    }

    @Test
    fun When_onScaleChanged_Then_persisted_and_state_updates() {
        val bloc = createBloc()

        bloc.onScaleChanged(0.5)

        scalePreferences.scaleFor(sampleRecipe.id).value shouldBe 0.5
        bloc.state.value.ingredientScale shouldBe 0.5
    }

    @Test
    fun When_never_scaled_Then_scale_defaults_to_one() {
        val bloc = createBloc()
        bloc.state.value.ingredientScale shouldBe 1.0
    }

    @Test
    fun When_recipe_link_clicked_and_target_exists_Then_open_recipe_output_emitted() {
        val bloc = createBloc()
        val target = sampleRecipe.copy(id = 42L, clientId = "target-client-id")
        recipes.value = recipes.value + target

        bloc.onRecipeLinkClicked("target-client-id")

        output.lastValue shouldBe RecipeDetailBloc.Output.OpenRecipe(42L)
    }

    @Test
    fun When_recipe_link_clicked_and_target_missing_Then_not_found_toast_shown() {
        val bloc = createBloc()

        bloc.onRecipeLinkClicked("unknown-client-id")

        output.values shouldBe emptyList()
        toastService.shown shouldBe
            listOf(ResourceString(Res.string.recipe_detail_linked_recipe_not_found))
    }
}
