package com.plusmobileapps.chefmate.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.plusmobileapps.chefmate.BlocContext
import com.plusmobileapps.chefmate.Consumer
import com.plusmobileapps.chefmate.settings.ui.SettingsScreen
import com.plusmobileapps.chefmate.text.TextData
import com.plusmobileapps.chefmate.ui.ComposeScreen
import kotlinx.coroutines.flow.StateFlow

interface SettingsBloc : ComposeScreen {
    val state: StateFlow<Model>

    @Composable
    override fun Content(modifier: Modifier) {
        SettingsScreen(bloc = this, modifier = modifier)
    }

    fun onSignInClicked()

    fun onSignUpClicked()

    fun onSignOutClicked()

    fun onSignOutConfirmed()

    fun onSignOutDismissed()

    fun onManageProfileClicked()

    fun onNotificationsClicked()

    fun onManageFamilyClicked()

    fun onUrlClicked(url: String)

    fun onAppSettingsClicked()

    /**
     * Open the AI chat. For a non-subscriber this opens the premium upsell instead — see
     * [Model.isSubscribed].
     */
    fun onAiChatClicked()

    /** Dismiss the "ChefMate Premium required" dialog. */
    fun onPremiumRequiredDismissed()

    fun onDeveloperSettingsClicked()

    fun onReplayOnboardingClicked()

    data class Model(
        val isAuthenticated: Boolean = false,
        /**
         * True when the user is signed in via Supabase anonymous auth. Surfaced separately from
         * [isAuthenticated] because the UI shows neither a greeting nor a Sign-Out row in this
         * state — anonymous sessions have no credentials to come back to, so "Sign Out" would
         * silently orphan their data instead of doing what users expect.
         */
        val isAnonymous: Boolean = false,
        val greeting: TextData? = null,
        val verificationMessage: TextData? = null,
        val showSignOutConfirmationDialog: Boolean = false,
        val isDebugBuild: Boolean = false,
        val isAiChatEnabled: Boolean = false,
        /**
         * Manage Family is behind a flag and shown in every auth state: a signed-out user still
         * sees the row, and tapping it routes them through sign-in first.
         */
        val isManageFamilyEnabled: Boolean = false,
        /**
         * ChefMate Premium entitlement. Unlike [isAiChatEnabled] this doesn't hide the row: a
         * non-subscriber still sees it, badged as premium, and tapping it opens the upsell.
         */
        val isSubscribed: Boolean = false,
        /** True while the premium upsell dialog is shown. */
        val showPremiumRequiredDialog: Boolean = false,
        val versionName: String = "",
    )

    sealed class Output {
        data object OpenSignUp : Output()

        data object OpenSignIn : Output()

        data object OpenManageProfile : Output()

        data object OpenNotifications : Output()

        data object OpenManageFamily : Output()

        data object OpenAppSettings : Output()

        data object OpenAiChat : Output()

        data object OpenDeveloperSettings : Output()

        /** Replay the first-run onboarding flow from the More tab. */
        data object OpenOnboarding : Output()

        data class OpenUrl(val url: String) : Output()
    }

    fun interface Factory {
        fun create(context: BlocContext, output: Consumer<Output>): SettingsBloc
    }
}
