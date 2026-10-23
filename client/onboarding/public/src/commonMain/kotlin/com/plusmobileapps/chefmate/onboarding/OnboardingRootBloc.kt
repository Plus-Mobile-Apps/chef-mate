package com.plusmobileapps.chefmate.onboarding

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.backhandler.BackHandlerOwner
import com.plusmobileapps.chefmate.BackClickBloc
import com.plusmobileapps.chefmate.BlocContext
import com.plusmobileapps.chefmate.Consumer
import com.plusmobileapps.chefmate.ui.ComposeScreen

/**
 * Navigation BLoC that drives the first-run onboarding flow. It owns a Decompose router that walks
 * the user from the [WelcomeBloc] through a series of feature tours ([ShareRecipesBloc],
 * [SaveRecipesBloc], [AddToGroceryBloc], [VoiceGroceryBloc], [MealPlanningBloc], [CookModeBloc],
 * [RecipeBooksBloc], [GroceryListsBloc], [FamilySharingBloc]) to the final [StartCookingBloc]. When
 * the user finishes (or skips), it marks onboarding as completed and emits [Output.Finished] so the
 * root can load the rest of the app. The [VoiceGroceryBloc] step only appears when the device has a
 * supported voice assistant (see [VoiceAssistantProvider]).
 *
 * From the welcome screen the user can also choose to sign in; that is surfaced as [Output.SignIn]
 * so the root can open the authentication flow. From the final [StartCookingBloc] step, the user
 * can also choose to sign up for an account, surfaced as [Output.SignUp].
 *
 * The flow can also be re-entered from within the app (e.g. Settings → replay onboarding). In that
 * case [Props.isDismissible] is true, so the first step shows a back arrow that exits the flow
 * ([Output.Dismissed]) instead of hiding it, and [Props.isSignedIn] can be true so an already
 * signed-in user isn't offered a sign-in or sign-up they can't use.
 */
interface OnboardingRootBloc : BackHandlerOwner, BackClickBloc, ComposeScreen {
    val routerState: Value<ChildStack<*, Child>>

    /** Total number of steps in the flow, used to render the nav bar's progress indicator. */
    val totalSteps: Int

    /**
     * True when the flow was re-entered from within the app and can be exited via the first step's
     * back arrow (see [onDismissClicked]). False on first run, where there's nowhere to go back to.
     */
    val isDismissible: Boolean

    @Composable
    override fun Content(modifier: Modifier) {
        OnboardingRootScreen(bloc = this, modifier = modifier)
    }

    /** Skip the rest of onboarding; equivalent to finishing it. Surfaced in the top nav bar. */
    fun onSkipClicked()

    /**
     * Exit the flow without completing it, from the first step's back arrow. Only meaningful when
     * [isDismissible]; the root pops onboarding off its stack ([Output.Dismissed]).
     */
    fun onDismissClicked()

    sealed class Child {

        abstract val bloc: ComposeScreen

        data class Welcome(override val bloc: WelcomeBloc) : Child()

        data class ShareRecipes(override val bloc: ShareRecipesBloc) : Child()

        data class SaveRecipes(override val bloc: SaveRecipesBloc) : Child()

        data class AddToGrocery(override val bloc: AddToGroceryBloc) : Child()

        data class VoiceGrocery(override val bloc: VoiceGroceryBloc) : Child()

        data class MealPlanning(override val bloc: MealPlanningBloc) : Child()

        data class CookMode(override val bloc: CookModeBloc) : Child()

        data class RecipeBooks(override val bloc: RecipeBooksBloc) : Child()

        data class GroceryLists(override val bloc: GroceryListsBloc) : Child()

        data class FamilySharing(override val bloc: FamilySharingBloc) : Child()

        data class StartCooking(override val bloc: StartCookingBloc) : Child()
    }

    sealed class Output {
        /**
         * The user reached the end of onboarding (or skipped); the root should load the main app.
         */
        data object Finished : Output()

        /**
         * The user backed out of a re-entered flow from the first step; the root should pop
         * onboarding off its stack and return to wherever they came from. Onboarding stays marked
         * completed, so this is distinct from [Finished].
         */
        data object Dismissed : Output()

        /** The user wants to sign in; the root should open the authentication flow. */
        data object SignIn : Output()

        /** The user wants to create an account; the root should open the sign-up flow. */
        data object SignUp : Output()
    }

    /**
     * How the flow was entered. Defaults describe a first run: not dismissible, not signed in.
     *
     * @property isDismissible drives the first step's back arrow — true when re-entered from within
     *   the app so the user can back out.
     * @property isSignedIn true when a real (non-anonymous) user re-entered the flow. They can't
     *   sign in or sign up again, so the welcome step hides its sign-in button and the final step
     *   hides its sign-up button.
     */
    data class Props(val isDismissible: Boolean = false, val isSignedIn: Boolean = false)

    fun interface Factory {
        fun create(
            context: BlocContext,
            props: Props,
            output: Consumer<Output>,
        ): OnboardingRootBloc
    }
}
