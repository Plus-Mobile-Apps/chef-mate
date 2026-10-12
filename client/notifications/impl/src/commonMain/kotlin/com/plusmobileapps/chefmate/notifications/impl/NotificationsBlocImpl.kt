package com.plusmobileapps.chefmate.notifications.impl

import com.plusmobileapps.chefmate.BlocContext
import com.plusmobileapps.chefmate.Consumer
import com.plusmobileapps.chefmate.di.AppScope
import com.plusmobileapps.chefmate.getViewModel
import com.plusmobileapps.chefmate.notifications.NotificationsBloc
import com.plusmobileapps.chefmate.notifications.NotificationsBloc.Model
import com.plusmobileapps.chefmate.notifications.NotificationsBloc.Output
import com.plusmobileapps.chefmate.notifications.data.AppNotification
import com.plusmobileapps.metro.extensions.assistedfactory.ContributesAssistedFactory
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedInject
import kotlinx.coroutines.flow.StateFlow

@AssistedInject
@ContributesAssistedFactory(
    scope = AppScope::class,
    assistedFactory = NotificationsBloc.Factory::class,
)
class NotificationsBlocImpl(
    @Assisted context: BlocContext,
    @Assisted private val output: Consumer<Output>,
    viewModelFactory: () -> NotificationsViewModel,
) : NotificationsBloc, BlocContext by context {

    private val viewModel = instanceKeeper.getViewModel { viewModelFactory() }

    override val state: StateFlow<Model> = viewModel.state

    override fun onBack() {
        output.onNext(Output.Back)
    }

    override fun onAccept(notification: AppNotification) {
        viewModel.accept(notification)
    }

    override fun onDecline(notification: AppNotification) {
        viewModel.decline(notification)
    }

    override fun onSignInClicked() {
        output.onNext(Output.OpenSignIn)
    }

    override fun onSignUpClicked() {
        output.onNext(Output.OpenSignUp)
    }
}
