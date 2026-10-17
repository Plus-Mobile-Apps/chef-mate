@file:OptIn(DelicateDecomposeApi::class)

package com.plusmobileapps.chefmate.onboarding.impl

import com.arkivanov.decompose.DelicateDecomposeApi
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.bringToFront
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.value.Value
import com.plusmobileapps.chefmate.BlocContext
import com.plusmobileapps.chefmate.Consumer
import com.plusmobileapps.chefmate.di.AppScope
import com.plusmobileapps.chefmate.di.OnboardingRepository
import com.plusmobileapps.chefmate.onboarding.AddToGroceryBloc
import com.plusmobileapps.chefmate.onboarding.CookModeBloc
import com.plusmobileapps.chefmate.onboarding.FamilySharingBloc
import com.plusmobileapps.chefmate.onboarding.GroceryListsBloc
import com.plusmobileapps.chefmate.onboarding.MealPlanningBloc
import com.plusmobileapps.chefmate.onboarding.OnboardingRootBloc
import com.plusmobileapps.chefmate.onboarding.OnboardingRootBloc.Output
import com.plusmobileapps.chefmate.onboarding.RecipeBooksBloc
import com.plusmobileapps.chefmate.onboarding.SaveRecipesBloc
import com.plusmobileapps.chefmate.onboarding.ShareRecipesBloc
import com.plusmobileapps.chefmate.onboarding.StartCookingBloc
import com.plusmobileapps.chefmate.onboarding.WelcomeBloc
import com.plusmobileapps.metro.extensions.assistedfactory.ContributesAssistedFactory
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedInject
import kotlinx.serialization.Serializable

@AssistedInject
@ContributesAssistedFactory(
    scope = AppScope::class,
    assistedFactory = OnboardingRootBloc.Factory::class,
)
class OnboardingRootBlocImpl(
    @Assisted context: BlocContext,
    @Assisted private val props: OnboardingRootBloc.Props,
    @Assisted private val output: Consumer<Output>,
    private val onboardingRepository: OnboardingRepository,
    private val welcome: WelcomeBloc.Factory,
    private val shareRecipes: ShareRecipesBloc.Factory,
    private val saveRecipes: SaveRecipesBloc.Factory,
    private val addToGrocery: AddToGroceryBloc.Factory,
    private val mealPlanning: MealPlanningBloc.Factory,
    private val cookMode: CookModeBloc.Factory,
    private val recipeBooks: RecipeBooksBloc.Factory,
    private val groceryLists: GroceryListsBloc.Factory,
    private val familySharing: FamilySharingBloc.Factory,
    private val startCooking: StartCookingBloc.Factory,
) : OnboardingRootBloc, BlocContext by context {

    private val navigation = StackNavigation<Configuration>()

    private val stack =
        childStack(
            source = navigation,
            serializer = Configuration.serializer(),
            initialStack = { listOf(Configuration.Welcome) },
            handleBackButton = true,
            key = "OnboardingRootRouter",
            childFactory = ::createChild,
        )

    override val routerState: Value<ChildStack<*, OnboardingRootBloc.Child>> = stack

    override val totalSteps: Int = STEP_COUNT

    override val isDismissible: Boolean = props.isDismissible

    override fun onBackClicked() {
        navigation.pop()
    }

    override fun onSkipClicked() {
        finishOnboarding()
    }

    override fun onDismissClicked() {
        // The flow stays marked completed; just hand control back so the root can pop it.
        output.onNext(Output.Dismissed)
    }

    private fun createChild(config: Configuration, context: BlocContext): OnboardingRootBloc.Child =
        when (config) {
            Configuration.Welcome ->
                OnboardingRootBloc.Child.Welcome(
                    bloc =
                        welcome.create(
                            context = context,
                            showSignIn = !props.isSignedIn,
                            output = ::handleWelcomeOutput,
                        )
                )

            Configuration.ShareRecipes ->
                OnboardingRootBloc.Child.ShareRecipes(
                    bloc =
                        shareRecipes.create(context = context, output = ::handleShareRecipesOutput)
                )

            Configuration.SaveRecipes ->
                OnboardingRootBloc.Child.SaveRecipes(
                    bloc = saveRecipes.create(context = context, output = ::handleSaveRecipesOutput)
                )

            Configuration.AddToGrocery ->
                OnboardingRootBloc.Child.AddToGrocery(
                    bloc =
                        addToGrocery.create(context = context, output = ::handleAddToGroceryOutput)
                )

            Configuration.MealPlanning ->
                OnboardingRootBloc.Child.MealPlanning(
                    bloc =
                        mealPlanning.create(context = context, output = ::handleMealPlanningOutput)
                )

            Configuration.CookMode ->
                OnboardingRootBloc.Child.CookMode(
                    bloc = cookMode.create(context = context, output = ::handleCookModeOutput)
                )

            Configuration.RecipeBooks ->
                OnboardingRootBloc.Child.RecipeBooks(
                    bloc = recipeBooks.create(context = context, output = ::handleRecipeBooksOutput)
                )

            Configuration.GroceryLists ->
                OnboardingRootBloc.Child.GroceryLists(
                    bloc =
                        groceryLists.create(context = context, output = ::handleGroceryListsOutput)
                )

            Configuration.FamilySharing ->
                OnboardingRootBloc.Child.FamilySharing(
                    bloc =
                        familySharing.create(
                            context = context,
                            output = ::handleFamilySharingOutput,
                        )
                )

            Configuration.StartCooking ->
                OnboardingRootBloc.Child.StartCooking(
                    bloc =
                        startCooking.create(
                            context = context,
                            showSignUp = !props.isSignedIn,
                            output = ::handleStartCookingOutput,
                        )
                )
        }

    /** Brings the next step to the front of the onboarding stack. */
    private fun advanceTo(configuration: Configuration) {
        navigation.bringToFront(configuration)
    }

    private fun handleWelcomeOutput(output: WelcomeBloc.Output) {
        when (output) {
            WelcomeBloc.Output.GetStarted -> advanceTo(Configuration.ShareRecipes)
            // The root opens the auth flow; on success it tears down onboarding for us.
            WelcomeBloc.Output.SignIn -> this.output.onNext(Output.SignIn)
        }
    }

    private fun handleShareRecipesOutput(output: ShareRecipesBloc.Output) {
        when (output) {
            ShareRecipesBloc.Output.Next -> advanceTo(Configuration.SaveRecipes)
        }
    }

    private fun handleSaveRecipesOutput(output: SaveRecipesBloc.Output) {
        when (output) {
            SaveRecipesBloc.Output.Next -> advanceTo(Configuration.AddToGrocery)
        }
    }

    private fun handleAddToGroceryOutput(output: AddToGroceryBloc.Output) {
        when (output) {
            AddToGroceryBloc.Output.Next -> advanceTo(Configuration.MealPlanning)
        }
    }

    private fun handleMealPlanningOutput(output: MealPlanningBloc.Output) {
        when (output) {
            MealPlanningBloc.Output.Next -> advanceTo(Configuration.CookMode)
        }
    }

    private fun handleCookModeOutput(output: CookModeBloc.Output) {
        when (output) {
            CookModeBloc.Output.Next -> advanceTo(Configuration.RecipeBooks)
        }
    }

    private fun handleRecipeBooksOutput(output: RecipeBooksBloc.Output) {
        when (output) {
            RecipeBooksBloc.Output.Next -> advanceTo(Configuration.GroceryLists)
        }
    }

    private fun handleGroceryListsOutput(output: GroceryListsBloc.Output) {
        when (output) {
            GroceryListsBloc.Output.Next -> advanceTo(Configuration.FamilySharing)
        }
    }

    private fun handleFamilySharingOutput(output: FamilySharingBloc.Output) {
        when (output) {
            FamilySharingBloc.Output.Next -> advanceTo(Configuration.StartCooking)
        }
    }

    private fun handleStartCookingOutput(output: StartCookingBloc.Output) {
        when (output) {
            StartCookingBloc.Output.StartCooking -> finishOnboarding()
            // The root opens the auth flow; on success it tears down onboarding for us.
            StartCookingBloc.Output.SignUp -> this.output.onNext(Output.SignUp)
        }
    }

    /** Persist completion so the flow is never shown again, then hand control back to the root. */
    private fun finishOnboarding() {
        onboardingRepository.setOnboardingCompleted()
        output.onNext(Output.Finished)
    }

    private companion object {
        /**
         * Welcome, Share recipes, Save recipes, Add to grocery, Meal planning, Cook mode, Recipe
         * books, Grocery lists, Family sharing, Start cooking.
         */
        const val STEP_COUNT = 10
    }

    @Serializable
    private sealed class Configuration {
        @Serializable data object Welcome : Configuration()

        @Serializable data object ShareRecipes : Configuration()

        @Serializable data object SaveRecipes : Configuration()

        @Serializable data object AddToGrocery : Configuration()

        @Serializable data object MealPlanning : Configuration()

        @Serializable data object CookMode : Configuration()

        @Serializable data object RecipeBooks : Configuration()

        @Serializable data object GroceryLists : Configuration()

        @Serializable data object FamilySharing : Configuration()

        @Serializable data object StartCooking : Configuration()
    }
}
