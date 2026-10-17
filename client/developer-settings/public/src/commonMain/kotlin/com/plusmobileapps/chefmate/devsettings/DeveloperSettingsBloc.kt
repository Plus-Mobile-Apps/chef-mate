package com.plusmobileapps.chefmate.devsettings

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.plusmobileapps.chefmate.BlocContext
import com.plusmobileapps.chefmate.Consumer
import com.plusmobileapps.chefmate.Environment
import com.plusmobileapps.chefmate.devsettings.ui.DeveloperSettingsScreen
import com.plusmobileapps.chefmate.ui.ComposeScreen
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.StateFlow

interface DeveloperSettingsBloc : ComposeScreen {
    val state: StateFlow<Model>

    @Composable
    override fun Content(modifier: Modifier) {
        DeveloperSettingsScreen(bloc = this, modifier = modifier)
    }

    fun onBack()

    fun onEnvironmentClicked()

    fun onEnvironmentSelected(environment: Environment)

    fun onEnvironmentPickerDismissed()

    fun onLoginAsUserClicked()

    fun onUserSelected(user: TestUser)

    fun onUserPickerDismissed()

    fun onSignOutTestUserClicked()

    fun onRestartPromptDismissed()

    fun onSignInErrorDismissed()

    fun onFeatureFlagsClicked()

    /** Flip the fake premium entitlement that gates AI chat throughout the app. */
    fun onSubscribedToggled(subscribed: Boolean)

    fun onClearCoachMarksClicked()

    fun onCoachMarksResetConfirmationDismissed()

    data class Model(
        val currentEnvironment: Environment = Environment.PROD,
        val availableUsers: ImmutableList<TestUser> = persistentListOf(),
        val selectedUserIndex: Int? = null,
        val currentUserEmail: String? = null,
        val isAuthenticated: Boolean = false,
        val showEnvironmentPicker: Boolean = false,
        val showUserPicker: Boolean = false,
        val showRestartPrompt: Boolean = false,
        val signInError: String? = null,
        val showCoachMarksResetConfirmation: Boolean = false,
        /** Fake premium entitlement; see [DeveloperPreferences.isSubscribed]. */
        val isSubscribed: Boolean = false,
    )

    sealed class Output {
        data object Back : Output()

        data object OpenFeatureFlags : Output()
    }

    fun interface Factory {
        fun create(context: BlocContext, output: Consumer<Output>): DeveloperSettingsBloc
    }
}
