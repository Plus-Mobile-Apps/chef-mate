package com.plusmobileapps.chefmate.settings.impl

import com.plusmobileapps.chefmate.BlocContext
import com.plusmobileapps.chefmate.Consumer
import com.plusmobileapps.chefmate.buildconfig.BuildConfig
import com.plusmobileapps.chefmate.di.AppScope
import com.plusmobileapps.chefmate.getViewModel
import com.plusmobileapps.chefmate.isDebugBuild
import com.plusmobileapps.chefmate.mapState
import com.plusmobileapps.chefmate.settings.SettingsBloc
import com.plusmobileapps.chefmate.settings.SettingsBloc.Output
import com.plusmobileapps.chefmate.settings.createEmailVerificationMessage
import com.plusmobileapps.chefmate.settings.createGreeting
import com.plusmobileapps.metro.extensions.assistedfactory.ContributesAssistedFactory
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedInject
import dev.zacsweers.metro.Provider
import kotlinx.coroutines.flow.StateFlow

@AssistedInject
@ContributesAssistedFactory(scope = AppScope::class, assistedFactory = SettingsBloc.Factory::class)
class SettingsBlocImpl(
    @Assisted context: BlocContext,
    @Assisted private val output: Consumer<Output>,
    viewModelFactory: Provider<SettingsViewModel>,
) : SettingsBloc, BlocContext by context {

    private val viewModel = instanceKeeper.getViewModel { viewModelFactory() }

    override val state: StateFlow<SettingsBloc.Model> =
        viewModel.state.mapState {
            SettingsBloc.Model(
                isAuthenticated = it.isAuthenticated,
                isAnonymous = it.isAnonymous,
                greeting = it.userName?.let { name -> createGreeting(name) },
                verificationMessage =
                    it.emailAwaitingVerification?.let { email ->
                        createEmailVerificationMessage(email)
                    },
                showSignOutConfirmationDialog = it.showSignOutConfirmationDialog,
                isDebugBuild = isDebugBuild,
                isAiChatEnabled = it.isAiChatEnabled,
                isManageFamilyEnabled = it.isManageFamilyEnabled,
                isSubscribed = it.isSubscribed,
                showPremiumRequiredDialog = it.showPremiumRequiredDialog,
                versionName = BuildConfig.VERSION_NAME,
            )
        }

    override fun onSignInClicked() {
        output.onNext(Output.OpenSignIn)
    }

    override fun onSignUpClicked() {
        output.onNext(Output.OpenSignUp)
    }

    override fun onSignOutClicked() {
        viewModel.showSignOutConfirmationDialog()
    }

    override fun onSignOutConfirmed() {
        viewModel.signOut()
    }

    override fun onSignOutDismissed() {
        viewModel.dismissSignOutConfirmationDialog()
    }

    override fun onManageProfileClicked() {
        output.onNext(Output.OpenManageProfile)
    }

    override fun onNotificationsClicked() {
        output.onNext(Output.OpenNotifications)
    }

    override fun onManageFamilyClicked() {
        output.onNext(Output.OpenManageFamily)
    }

    override fun onUrlClicked(url: String) {
        output.onNext(Output.OpenUrl(url))
    }

    override fun onAppSettingsClicked() {
        output.onNext(Output.OpenAppSettings)
    }

    override fun onAiChatClicked() {
        if (viewModel.requestAiChat()) {
            output.onNext(Output.OpenAiChat)
        }
    }

    override fun onPremiumRequiredDismissed() {
        viewModel.dismissPremiumRequiredDialog()
    }

    override fun onDeveloperSettingsClicked() {
        output.onNext(Output.OpenDeveloperSettings)
    }

    override fun onReplayOnboardingClicked() {
        output.onNext(Output.OpenOnboarding)
    }
}
