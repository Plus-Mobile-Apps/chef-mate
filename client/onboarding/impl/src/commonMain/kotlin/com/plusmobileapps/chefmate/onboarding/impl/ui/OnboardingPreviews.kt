package com.plusmobileapps.chefmate.onboarding.impl.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.plusmobileapps.chefmate.Platform
import com.plusmobileapps.chefmate.onboarding.AddToGroceryBloc
import com.plusmobileapps.chefmate.onboarding.AddToGroceryScreen
import com.plusmobileapps.chefmate.onboarding.CookModeBloc
import com.plusmobileapps.chefmate.onboarding.CookModeScreen
import com.plusmobileapps.chefmate.onboarding.FamilySharingBloc
import com.plusmobileapps.chefmate.onboarding.FamilySharingScreen
import com.plusmobileapps.chefmate.onboarding.GroceryListsBloc
import com.plusmobileapps.chefmate.onboarding.GroceryListsScreen
import com.plusmobileapps.chefmate.onboarding.MealPlanningBloc
import com.plusmobileapps.chefmate.onboarding.MealPlanningScreen
import com.plusmobileapps.chefmate.onboarding.OnboardingNavBar
import com.plusmobileapps.chefmate.onboarding.RecipeBooksBloc
import com.plusmobileapps.chefmate.onboarding.RecipeBooksScreen
import com.plusmobileapps.chefmate.onboarding.SaveRecipesBloc
import com.plusmobileapps.chefmate.onboarding.SaveRecipesScreen
import com.plusmobileapps.chefmate.onboarding.ShareRecipesScreen
import com.plusmobileapps.chefmate.onboarding.StartCookingBloc
import com.plusmobileapps.chefmate.onboarding.StartCookingScreen
import com.plusmobileapps.chefmate.onboarding.VoiceAssistant
import com.plusmobileapps.chefmate.onboarding.VoiceGroceryBloc
import com.plusmobileapps.chefmate.onboarding.VoiceGroceryScreen
import com.plusmobileapps.chefmate.onboarding.WelcomeBloc
import com.plusmobileapps.chefmate.onboarding.WelcomeScreen
import com.plusmobileapps.chefmate.ui.theme.ChefMateTheme

// Public fake Blocs so the screenshot-test module can reuse them. The onboarding step screens are
// stateless, so the fakes only need to satisfy the interface.

val previewWelcomeBloc: WelcomeBloc =
    object : WelcomeBloc {
        override fun onGetStartedClicked() = Unit

        override fun onSignInClicked() = Unit
    }

// Re-entry variant (e.g. Settings → replay) for an already signed-in user: the sign-in button is
// hidden because they can't sign in again.
val previewWelcomeSignedInBloc: WelcomeBloc =
    object : WelcomeBloc {
        override val showSignIn: Boolean = false

        override fun onGetStartedClicked() = Unit

        override fun onSignInClicked() = Unit
    }

val previewSaveRecipesBloc: SaveRecipesBloc =
    object : SaveRecipesBloc {
        override fun onNextClicked() = Unit
    }

val previewAddToGroceryBloc: AddToGroceryBloc =
    object : AddToGroceryBloc {
        override fun onNextClicked() = Unit
    }

val previewVoiceGrocerySiriBloc: VoiceGroceryBloc =
    object : VoiceGroceryBloc {
        override val assistant: VoiceAssistant = VoiceAssistant.SIRI

        override fun onNextClicked() = Unit
    }

val previewVoiceGroceryGeminiBloc: VoiceGroceryBloc =
    object : VoiceGroceryBloc {
        override val assistant: VoiceAssistant = VoiceAssistant.GEMINI

        override fun onNextClicked() = Unit
    }

val previewMealPlanningBloc: MealPlanningBloc =
    object : MealPlanningBloc {
        override fun onNextClicked() = Unit
    }

val previewCookModeBloc: CookModeBloc =
    object : CookModeBloc {
        override fun onNextClicked() = Unit
    }

val previewRecipeBooksBloc: RecipeBooksBloc =
    object : RecipeBooksBloc {
        override fun onNextClicked() = Unit
    }

val previewGroceryListsBloc: GroceryListsBloc =
    object : GroceryListsBloc {
        override fun onNextClicked() = Unit
    }

val previewFamilySharingBloc: FamilySharingBloc =
    object : FamilySharingBloc {
        override fun onNextClicked() = Unit
    }

val previewStartCookingBloc: StartCookingBloc =
    object : StartCookingBloc {
        override fun onStartCookingClicked() = Unit

        override fun onSignUpClicked() = Unit
    }

// Re-entry variant for an already signed-in user: the sign-up button is hidden because they can't
// sign up again.
val previewStartCookingSignedInBloc: StartCookingBloc =
    object : StartCookingBloc {
        override val showSignUp: Boolean = false

        override fun onStartCookingClicked() = Unit

        override fun onSignUpClicked() = Unit
    }

@Preview
@Composable
internal fun OnboardingNavBarPreview() {
    ChefMateTheme {
        OnboardingNavBar(currentStep = 1, totalSteps = 9, onBackClick = {}, onSkipClick = {})
    }
}

@Preview
@Composable
internal fun WelcomeScreenPreview() {
    ChefMateTheme { WelcomeScreen(bloc = previewWelcomeBloc) }
}

@Preview
@Composable
internal fun WelcomeScreenSignedInPreview() {
    ChefMateTheme { WelcomeScreen(bloc = previewWelcomeSignedInBloc) }
}

// Share recipes is platform-specific (iOS and Android show their own share sheet, desktop copies
// the
// address bar), so it's previewed by variant rather than through a fake Bloc.

@Preview
@Composable
internal fun ShareRecipesScreenAndroidPreview() {
    ChefMateTheme { ShareRecipesScreen(platform = Platform.ANDROID, onNextClick = {}) }
}

@Preview
@Composable
internal fun ShareRecipesScreenIosPreview() {
    ChefMateTheme { ShareRecipesScreen(platform = Platform.IOS, onNextClick = {}) }
}

@Preview
@Composable
internal fun ShareRecipesScreenDesktopPreview() {
    ChefMateTheme { ShareRecipesScreen(platform = Platform.JVM, onNextClick = {}) }
}

@Preview
@Composable
internal fun SaveRecipesScreenPreview() {
    ChefMateTheme { SaveRecipesScreen(bloc = previewSaveRecipesBloc) }
}

@Preview
@Composable
internal fun AddToGroceryScreenPreview() {
    ChefMateTheme { AddToGroceryScreen(bloc = previewAddToGroceryBloc) }
}

@Preview
@Composable
internal fun VoiceGroceryScreenSiriPreview() {
    ChefMateTheme { VoiceGroceryScreen(bloc = previewVoiceGrocerySiriBloc) }
}

@Preview
@Composable
internal fun VoiceGroceryScreenGeminiPreview() {
    ChefMateTheme { VoiceGroceryScreen(bloc = previewVoiceGroceryGeminiBloc) }
}

@Preview
@Composable
internal fun MealPlanningScreenPreview() {
    ChefMateTheme { MealPlanningScreen(bloc = previewMealPlanningBloc) }
}

@Preview
@Composable
internal fun CookModeScreenPreview() {
    ChefMateTheme { CookModeScreen(bloc = previewCookModeBloc) }
}

@Preview
@Composable
internal fun RecipeBooksScreenPreview() {
    ChefMateTheme { RecipeBooksScreen(bloc = previewRecipeBooksBloc) }
}

@Preview
@Composable
internal fun GroceryListsScreenPreview() {
    ChefMateTheme { GroceryListsScreen(bloc = previewGroceryListsBloc) }
}

@Preview
@Composable
internal fun FamilySharingScreenPreview() {
    ChefMateTheme { FamilySharingScreen(bloc = previewFamilySharingBloc) }
}

@Preview
@Composable
internal fun StartCookingScreenPreview() {
    ChefMateTheme { StartCookingScreen(bloc = previewStartCookingBloc) }
}

@Preview
@Composable
internal fun StartCookingScreenSignedInPreview() {
    ChefMateTheme { StartCookingScreen(bloc = previewStartCookingSignedInBloc) }
}
