package com.plusmobileapps.chefmate.onboarding.impl

import com.plusmobileapps.chefmate.BlocContext
import com.plusmobileapps.chefmate.Consumer
import com.plusmobileapps.chefmate.di.AppScope
import com.plusmobileapps.chefmate.onboarding.VoiceAssistant
import com.plusmobileapps.chefmate.onboarding.VoiceGroceryBloc
import com.plusmobileapps.metro.extensions.assistedfactory.ContributesAssistedFactory
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedInject

@AssistedInject
@ContributesAssistedFactory(
    scope = AppScope::class,
    assistedFactory = VoiceGroceryBloc.Factory::class,
)
class VoiceGroceryBlocImpl(
    @Assisted context: BlocContext,
    @Assisted override val assistant: VoiceAssistant,
    @Assisted private val output: Consumer<VoiceGroceryBloc.Output>,
) : VoiceGroceryBloc, BlocContext by context {

    override fun onNextClicked() {
        output.onNext(VoiceGroceryBloc.Output.Next)
    }
}
