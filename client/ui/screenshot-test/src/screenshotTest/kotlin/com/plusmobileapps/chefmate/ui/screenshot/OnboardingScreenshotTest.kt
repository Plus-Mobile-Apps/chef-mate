package com.plusmobileapps.chefmate.ui.screenshot

import android.content.res.Configuration
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.plusmobileapps.chefmate.Platform
import com.plusmobileapps.chefmate.onboarding.OnboardingNavBar
import com.plusmobileapps.chefmate.onboarding.ShareRecipesScreen
import com.plusmobileapps.chefmate.onboarding.impl.ui.previewAddToGroceryBloc
import com.plusmobileapps.chefmate.onboarding.impl.ui.previewCookModeBloc
import com.plusmobileapps.chefmate.onboarding.impl.ui.previewFamilySharingBloc
import com.plusmobileapps.chefmate.onboarding.impl.ui.previewGroceryListsBloc
import com.plusmobileapps.chefmate.onboarding.impl.ui.previewMealPlanningBloc
import com.plusmobileapps.chefmate.onboarding.impl.ui.previewRecipeBooksBloc
import com.plusmobileapps.chefmate.onboarding.impl.ui.previewSaveRecipesBloc
import com.plusmobileapps.chefmate.onboarding.impl.ui.previewStartCookingBloc
import com.plusmobileapps.chefmate.onboarding.impl.ui.previewStartCookingSignedInBloc
import com.plusmobileapps.chefmate.onboarding.impl.ui.previewVoiceGroceryGeminiBloc
import com.plusmobileapps.chefmate.onboarding.impl.ui.previewVoiceGrocerySiriBloc
import com.plusmobileapps.chefmate.onboarding.impl.ui.previewWelcomeBloc
import com.plusmobileapps.chefmate.onboarding.impl.ui.previewWelcomeSignedInBloc
import com.plusmobileapps.chefmate.ui.Content
import com.plusmobileapps.chefmate.ui.theme.ChefMateTheme

@Composable
private fun OnboardingScreenshot(content: @Composable () -> Unit, darkTheme: Boolean = false) {
    ChefMateTheme(darkTheme = darkTheme) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            content()
        }
    }
}

// ── Nav bar ────────────────────────────────────────────────────────────────

@PreviewTest
@Preview(showBackground = true)
@Composable
fun OnboardingNavBarLightScreenshot() {
    OnboardingScreenshot(
        content = {
            OnboardingNavBar(currentStep = 1, totalSteps = 9, onBackClick = {}, onSkipClick = {})
        },
    )
}

@PreviewTest
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun OnboardingNavBarDarkScreenshot() {
    OnboardingScreenshot(
        content = {
            OnboardingNavBar(currentStep = 1, totalSteps = 9, onBackClick = {}, onSkipClick = {})
        },
        darkTheme = true,
    )
}

// ── Welcome ────────────────────────────────────────────────────────────────

@PreviewTest
@Preview(showBackground = true, heightDp = 900)
@Composable
fun OnboardingWelcomeLightScreenshot() {
    OnboardingScreenshot(content = { previewWelcomeBloc.Content() })
}

@PreviewTest
@Preview(showBackground = true, heightDp = 900, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun OnboardingWelcomeDarkScreenshot() {
    OnboardingScreenshot(content = { previewWelcomeBloc.Content() }, darkTheme = true)
}

// Re-entry variant for an already signed-in user — the sign-in button is hidden.

@PreviewTest
@Preview(showBackground = true, heightDp = 900)
@Composable
fun OnboardingWelcomeSignedInLightScreenshot() {
    OnboardingScreenshot(content = { previewWelcomeSignedInBloc.Content() })
}

@PreviewTest
@Preview(showBackground = true, heightDp = 900, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun OnboardingWelcomeSignedInDarkScreenshot() {
    OnboardingScreenshot(content = { previewWelcomeSignedInBloc.Content() }, darkTheme = true)
}

// ── Share recipes (own browser) ──────────────────────────────────────────────
// Platform-specific: iOS and Android each show a capture of their own share sheet, desktop
// describes copying the address bar. Captured directly since the variant is chosen by platform,
// not by the Bloc.

@PreviewTest
@Preview(showBackground = true, heightDp = 900)
@Composable
fun OnboardingShareRecipesAndroidLightScreenshot() {
    OnboardingScreenshot(
        content = { ShareRecipesScreen(platform = Platform.ANDROID, onNextClick = {}) },
    )
}

@PreviewTest
@Preview(showBackground = true, heightDp = 900, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun OnboardingShareRecipesAndroidDarkScreenshot() {
    OnboardingScreenshot(
        content = { ShareRecipesScreen(platform = Platform.ANDROID, onNextClick = {}) },
        darkTheme = true,
    )
}

@PreviewTest
@Preview(showBackground = true, heightDp = 900)
@Composable
fun OnboardingShareRecipesIosLightScreenshot() {
    OnboardingScreenshot(
        content = { ShareRecipesScreen(platform = Platform.IOS, onNextClick = {}) },
    )
}

@PreviewTest
@Preview(showBackground = true, heightDp = 900, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun OnboardingShareRecipesIosDarkScreenshot() {
    OnboardingScreenshot(
        content = { ShareRecipesScreen(platform = Platform.IOS, onNextClick = {}) },
        darkTheme = true,
    )
}

@PreviewTest
@Preview(showBackground = true, heightDp = 900)
@Composable
fun OnboardingShareRecipesDesktopLightScreenshot() {
    OnboardingScreenshot(
        content = { ShareRecipesScreen(platform = Platform.JVM, onNextClick = {}) },
    )
}

@PreviewTest
@Preview(showBackground = true, heightDp = 900, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun OnboardingShareRecipesDesktopDarkScreenshot() {
    OnboardingScreenshot(
        content = { ShareRecipesScreen(platform = Platform.JVM, onNextClick = {}) },
        darkTheme = true,
    )
}

// ── Save recipes (in-app browser) ─────────────────────────────────────────────

@PreviewTest
@Preview(showBackground = true, heightDp = 900)
@Composable
fun OnboardingSaveRecipesLightScreenshot() {
    OnboardingScreenshot(content = { previewSaveRecipesBloc.Content() })
}

@PreviewTest
@Preview(showBackground = true, heightDp = 900, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun OnboardingSaveRecipesDarkScreenshot() {
    OnboardingScreenshot(content = { previewSaveRecipesBloc.Content() }, darkTheme = true)
}

// ── Add to grocery list ──────────────────────────────────────────────────────

@PreviewTest
@Preview(showBackground = true, heightDp = 900)
@Composable
fun OnboardingAddToGroceryLightScreenshot() {
    OnboardingScreenshot(content = { previewAddToGroceryBloc.Content() })
}

@PreviewTest
@Preview(showBackground = true, heightDp = 900, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun OnboardingAddToGroceryDarkScreenshot() {
    OnboardingScreenshot(content = { previewAddToGroceryBloc.Content() }, darkTheme = true)
}

// ── Add groceries by voice (Siri on iOS, Gemini on Android) ─────────────────

@PreviewTest
@Preview(showBackground = true, heightDp = 900)
@Composable
fun OnboardingVoiceGrocerySiriLightScreenshot() {
    OnboardingScreenshot(content = { previewVoiceGrocerySiriBloc.Content() })
}

@PreviewTest
@Preview(showBackground = true, heightDp = 900, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun OnboardingVoiceGrocerySiriDarkScreenshot() {
    OnboardingScreenshot(content = { previewVoiceGrocerySiriBloc.Content() }, darkTheme = true)
}

@PreviewTest
@Preview(showBackground = true, heightDp = 900)
@Composable
fun OnboardingVoiceGroceryGeminiLightScreenshot() {
    OnboardingScreenshot(content = { previewVoiceGroceryGeminiBloc.Content() })
}

@PreviewTest
@Preview(showBackground = true, heightDp = 900, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun OnboardingVoiceGroceryGeminiDarkScreenshot() {
    OnboardingScreenshot(content = { previewVoiceGroceryGeminiBloc.Content() }, darkTheme = true)
}

// ── Add to meal plan ─────────────────────────────────────────────────────────

@PreviewTest
@Preview(showBackground = true, heightDp = 900)
@Composable
fun OnboardingMealPlanningLightScreenshot() {
    OnboardingScreenshot(content = { previewMealPlanningBloc.Content() })
}

@PreviewTest
@Preview(showBackground = true, heightDp = 900, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun OnboardingMealPlanningDarkScreenshot() {
    OnboardingScreenshot(content = { previewMealPlanningBloc.Content() }, darkTheme = true)
}

// Wide-window variant: on desktop/tablet the step content is far wider than a phone, so this guards
// that the framed preview stays capped at a phone-ish width (centered) instead of filling the
// window.

@PreviewTest
@Preview(showBackground = true, widthDp = 1000, heightDp = 900)
@Composable
fun OnboardingMealPlanningWideScreenshot() {
    OnboardingScreenshot(content = { previewMealPlanningBloc.Content() })
}

// ── Start cooking (Cook Mode) ─────────────────────────────────────────────────

@PreviewTest
@Preview(showBackground = true, heightDp = 900)
@Composable
fun OnboardingCookModeLightScreenshot() {
    OnboardingScreenshot(content = { previewCookModeBloc.Content() })
}

@PreviewTest
@Preview(showBackground = true, heightDp = 900, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun OnboardingCookModeDarkScreenshot() {
    OnboardingScreenshot(content = { previewCookModeBloc.Content() }, darkTheme = true)
}

// ── Recipe books ─────────────────────────────────────────────────────────────

@PreviewTest
@Preview(showBackground = true, heightDp = 900)
@Composable
fun OnboardingRecipeBooksLightScreenshot() {
    OnboardingScreenshot(content = { previewRecipeBooksBloc.Content() })
}

@PreviewTest
@Preview(showBackground = true, heightDp = 900, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun OnboardingRecipeBooksDarkScreenshot() {
    OnboardingScreenshot(content = { previewRecipeBooksBloc.Content() }, darkTheme = true)
}

// ── Grocery lists ────────────────────────────────────────────────────────────

@PreviewTest
@Preview(showBackground = true, heightDp = 900)
@Composable
fun OnboardingGroceryListsLightScreenshot() {
    OnboardingScreenshot(content = { previewGroceryListsBloc.Content() })
}

@PreviewTest
@Preview(showBackground = true, heightDp = 900, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun OnboardingGroceryListsDarkScreenshot() {
    OnboardingScreenshot(content = { previewGroceryListsBloc.Content() }, darkTheme = true)
}

// ── Family sharing ───────────────────────────────────────────────────────────

@PreviewTest
@Preview(showBackground = true, heightDp = 900)
@Composable
fun OnboardingFamilySharingLightScreenshot() {
    OnboardingScreenshot(content = { previewFamilySharingBloc.Content() })
}

@PreviewTest
@Preview(showBackground = true, heightDp = 900, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun OnboardingFamilySharingDarkScreenshot() {
    OnboardingScreenshot(content = { previewFamilySharingBloc.Content() }, darkTheme = true)
}

// ── You're all set ───────────────────────────────────────────────────────────

@PreviewTest
@Preview(showBackground = true, heightDp = 900)
@Composable
fun OnboardingStartCookingLightScreenshot() {
    OnboardingScreenshot(content = { previewStartCookingBloc.Content() })
}

@PreviewTest
@Preview(showBackground = true, heightDp = 900, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun OnboardingStartCookingDarkScreenshot() {
    OnboardingScreenshot(content = { previewStartCookingBloc.Content() }, darkTheme = true)
}

// Re-entry variant for an already signed-in user — the sign-up button is hidden.

@PreviewTest
@Preview(showBackground = true, heightDp = 900)
@Composable
fun OnboardingStartCookingSignedInLightScreenshot() {
    OnboardingScreenshot(content = { previewStartCookingSignedInBloc.Content() })
}

@PreviewTest
@Preview(showBackground = true, heightDp = 900, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun OnboardingStartCookingSignedInDarkScreenshot() {
    OnboardingScreenshot(content = { previewStartCookingSignedInBloc.Content() }, darkTheme = true)
}
