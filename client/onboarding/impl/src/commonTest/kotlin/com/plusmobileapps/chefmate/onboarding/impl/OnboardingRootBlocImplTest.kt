@file:Suppress("FunctionName")

package com.plusmobileapps.chefmate.onboarding.impl

import com.plusmobileapps.chefmate.BlocContext
import com.plusmobileapps.chefmate.Consumer
import com.plusmobileapps.chefmate.di.OnboardingRepository
import com.plusmobileapps.chefmate.onboarding.AddToGroceryBloc
import com.plusmobileapps.chefmate.onboarding.CookModeBloc
import com.plusmobileapps.chefmate.onboarding.FamilySharingBloc
import com.plusmobileapps.chefmate.onboarding.GroceryListsBloc
import com.plusmobileapps.chefmate.onboarding.MealPlanningBloc
import com.plusmobileapps.chefmate.onboarding.OnboardingRootBloc
import com.plusmobileapps.chefmate.onboarding.RecipeBooksBloc
import com.plusmobileapps.chefmate.onboarding.SaveRecipesBloc
import com.plusmobileapps.chefmate.onboarding.ShareRecipesBloc
import com.plusmobileapps.chefmate.onboarding.StartCookingBloc
import com.plusmobileapps.chefmate.onboarding.WelcomeBloc
import com.plusmobileapps.chefmate.testing.TestBlocContext
import com.russhwolf.settings.MapSettings
import dev.mokkery.mock
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.instanceOf
import kotlin.test.Test

class OnboardingRootBlocImplTest {
    val onboardingRepository = OnboardingRepository(MapSettings())

    var welcomeOutput: Consumer<WelcomeBloc.Output> = Consumer {}
    var welcomeShowSignIn: Boolean? = null
    var shareRecipesOutput: Consumer<ShareRecipesBloc.Output> = Consumer {}
    var saveRecipesOutput: Consumer<SaveRecipesBloc.Output> = Consumer {}
    var addToGroceryOutput: Consumer<AddToGroceryBloc.Output> = Consumer {}
    var mealPlanningOutput: Consumer<MealPlanningBloc.Output> = Consumer {}
    var cookModeOutput: Consumer<CookModeBloc.Output> = Consumer {}
    var recipeBooksOutput: Consumer<RecipeBooksBloc.Output> = Consumer {}
    var groceryListsOutput: Consumer<GroceryListsBloc.Output> = Consumer {}
    var familySharingOutput: Consumer<FamilySharingBloc.Output> = Consumer {}
    var startCookingOutput: Consumer<StartCookingBloc.Output> = Consumer {}
    var startCookingShowSignUp: Boolean? = null

    var rootOutput: OnboardingRootBloc.Output? = null

    // Each bloc needs its own context; sharing one would register the router key twice.
    fun createBloc(
        props: OnboardingRootBloc.Props = OnboardingRootBloc.Props(),
        context: BlocContext = TestBlocContext.create(),
    ) =
        OnboardingRootBlocImpl(
            context = context,
            props = props,
            output = { rootOutput = it },
            onboardingRepository = onboardingRepository,
            welcome = { _, showSignIn, output ->
                welcomeShowSignIn = showSignIn
                welcomeOutput = output
                mock()
            },
            shareRecipes = { _, output ->
                shareRecipesOutput = output
                mock()
            },
            saveRecipes = { _, output ->
                saveRecipesOutput = output
                mock()
            },
            addToGrocery = { _, output ->
                addToGroceryOutput = output
                mock()
            },
            mealPlanning = { _, output ->
                mealPlanningOutput = output
                mock()
            },
            cookMode = { _, output ->
                cookModeOutput = output
                mock()
            },
            recipeBooks = { _, output ->
                recipeBooksOutput = output
                mock()
            },
            groceryLists = { _, output ->
                groceryListsOutput = output
                mock()
            },
            familySharing = { _, output ->
                familySharingOutput = output
                mock()
            },
            startCooking = { _, showSignUp, output ->
                startCookingShowSignUp = showSignUp
                startCookingOutput = output
                mock()
            },
        )

    val bloc = createBloc()

    fun OnboardingRootBloc.instance(): OnboardingRootBloc.Child = routerState.value.active.instance

    /** Steps the most-recently-created bloc through the tour to the final StartCooking step. */
    fun advanceToStartCooking() {
        welcomeOutput.onNext(WelcomeBloc.Output.GetStarted)
        shareRecipesOutput.onNext(ShareRecipesBloc.Output.Next)
        saveRecipesOutput.onNext(SaveRecipesBloc.Output.Next)
        addToGroceryOutput.onNext(AddToGroceryBloc.Output.Next)
        mealPlanningOutput.onNext(MealPlanningBloc.Output.Next)
        cookModeOutput.onNext(CookModeBloc.Output.Next)
        recipeBooksOutput.onNext(RecipeBooksBloc.Output.Next)
        groceryListsOutput.onNext(GroceryListsBloc.Output.Next)
        familySharingOutput.onNext(FamilySharingBloc.Output.Next)
    }

    @Test
    fun When_initialized_Then_welcome_is_shown() {
        bloc.instance() should instanceOf<OnboardingRootBloc.Child.Welcome>()
        bloc.routerState.value.backStack.size shouldBe 0
    }

    @Test
    fun When_the_user_steps_through_the_tour_Then_each_screen_is_shown_in_order() {
        welcomeOutput.onNext(WelcomeBloc.Output.GetStarted)
        bloc.instance() should instanceOf<OnboardingRootBloc.Child.ShareRecipes>()

        shareRecipesOutput.onNext(ShareRecipesBloc.Output.Next)
        bloc.instance() should instanceOf<OnboardingRootBloc.Child.SaveRecipes>()

        saveRecipesOutput.onNext(SaveRecipesBloc.Output.Next)
        bloc.instance() should instanceOf<OnboardingRootBloc.Child.AddToGrocery>()

        addToGroceryOutput.onNext(AddToGroceryBloc.Output.Next)
        bloc.instance() should instanceOf<OnboardingRootBloc.Child.MealPlanning>()

        mealPlanningOutput.onNext(MealPlanningBloc.Output.Next)
        bloc.instance() should instanceOf<OnboardingRootBloc.Child.CookMode>()

        cookModeOutput.onNext(CookModeBloc.Output.Next)
        bloc.instance() should instanceOf<OnboardingRootBloc.Child.RecipeBooks>()

        recipeBooksOutput.onNext(RecipeBooksBloc.Output.Next)
        bloc.instance() should instanceOf<OnboardingRootBloc.Child.GroceryLists>()

        groceryListsOutput.onNext(GroceryListsBloc.Output.Next)
        bloc.instance() should instanceOf<OnboardingRootBloc.Child.FamilySharing>()

        familySharingOutput.onNext(FamilySharingBloc.Output.Next)
        bloc.instance() should instanceOf<OnboardingRootBloc.Child.StartCooking>()
    }

    @Test
    fun When_start_cooking_clicked_Then_onboarding_is_completed_and_finished_emitted() {
        advanceToStartCooking()

        startCookingOutput.onNext(StartCookingBloc.Output.StartCooking)

        onboardingRepository.hasCompletedOnboarding shouldBe true
        rootOutput shouldBe OnboardingRootBloc.Output.Finished
    }

    @Test
    fun When_skip_clicked_Then_onboarding_is_completed_and_finished_emitted() {
        // Skip lives in the shared nav bar and can be tapped from any step.
        welcomeOutput.onNext(WelcomeBloc.Output.GetStarted)

        bloc.onSkipClicked()

        onboardingRepository.hasCompletedOnboarding shouldBe true
        rootOutput shouldBe OnboardingRootBloc.Output.Finished
    }

    @Test
    fun When_back_clicked_Then_returns_to_the_previous_step() {
        welcomeOutput.onNext(WelcomeBloc.Output.GetStarted)
        bloc.instance() should instanceOf<OnboardingRootBloc.Child.ShareRecipes>()

        bloc.onBackClicked()

        bloc.instance() should instanceOf<OnboardingRootBloc.Child.Welcome>()
    }

    @Test
    fun When_welcome_outputs_sign_in_Then_sign_in_emitted_without_completing() {
        welcomeOutput.onNext(WelcomeBloc.Output.SignIn)

        rootOutput shouldBe OnboardingRootBloc.Output.SignIn
        // Completion is deferred to the root once auth actually succeeds.
        onboardingRepository.hasCompletedOnboarding shouldBe false
    }

    @Test
    fun When_dismissible_and_dismiss_clicked_Then_dismissed_emitted_without_completing() {
        val bloc = createBloc(OnboardingRootBloc.Props(isDismissible = true))

        bloc.isDismissible shouldBe true
        bloc.onDismissClicked()

        rootOutput shouldBe OnboardingRootBloc.Output.Dismissed
        // Backing out of a re-entered flow doesn't change the already-completed state.
        onboardingRepository.hasCompletedOnboarding shouldBe false
    }

    @Test
    fun When_signed_in_Then_welcome_and_start_cooking_hide_their_auth_buttons() {
        val bloc = createBloc(OnboardingRootBloc.Props(isSignedIn = true))

        welcomeShowSignIn shouldBe false

        advanceToStartCooking()
        bloc.instance() should instanceOf<OnboardingRootBloc.Child.StartCooking>()
        startCookingShowSignUp shouldBe false
    }

    @Test
    fun When_default_props_Then_not_dismissible_and_auth_buttons_shown() {
        bloc.isDismissible shouldBe false
        welcomeShowSignIn shouldBe true

        advanceToStartCooking()
        startCookingShowSignUp shouldBe true
    }

    @Test
    fun When_start_cooking_outputs_sign_up_Then_sign_up_emitted_without_completing() {
        advanceToStartCooking()

        startCookingOutput.onNext(StartCookingBloc.Output.SignUp)

        rootOutput shouldBe OnboardingRootBloc.Output.SignUp
        // Completion is deferred to the root once auth actually succeeds.
        onboardingRepository.hasCompletedOnboarding shouldBe false
    }
}
