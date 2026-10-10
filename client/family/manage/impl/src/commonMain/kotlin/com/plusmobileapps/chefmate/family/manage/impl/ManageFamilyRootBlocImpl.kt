@file:OptIn(DelicateDecomposeApi::class)

package com.plusmobileapps.chefmate.family.manage.impl

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
import com.plusmobileapps.chefmate.family.manage.FamilyDetailBloc
import com.plusmobileapps.chefmate.family.manage.FamilyListBloc
import com.plusmobileapps.chefmate.family.manage.ManageFamilyRootBloc
import com.plusmobileapps.chefmate.family.manage.ManageFamilyRootBloc.Output
import com.plusmobileapps.metro.extensions.assistedfactory.ContributesAssistedFactory
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedInject
import kotlinx.serialization.Serializable

@AssistedInject
@ContributesAssistedFactory(
    scope = AppScope::class,
    assistedFactory = ManageFamilyRootBloc.Factory::class,
)
class ManageFamilyRootBlocImpl(
    @Assisted context: BlocContext,
    @Assisted private val output: Consumer<Output>,
    private val familyList: FamilyListBloc.Factory,
    private val familyDetail: FamilyDetailBloc.Factory,
) : ManageFamilyRootBloc, BlocContext by context {

    private val navigation = StackNavigation<Configuration>()

    private val stack =
        childStack(
            source = navigation,
            serializer = Configuration.serializer(),
            initialStack = { listOf(Configuration.FamilyList) },
            handleBackButton = true,
            key = "ManageFamilyRouter",
            childFactory = ::createChild,
        )

    override val routerState: Value<ChildStack<*, ManageFamilyRootBloc.Child>> = stack

    override fun onBackClicked() {
        navigation.pop()
    }

    private fun createChild(
        config: Configuration,
        context: BlocContext,
    ): ManageFamilyRootBloc.Child =
        when (config) {
            Configuration.FamilyList ->
                ManageFamilyRootBloc.Child.FamilyList(
                    bloc = familyList.create(context = context, output = ::handleListOutput)
                )

            is Configuration.FamilyDetail ->
                ManageFamilyRootBloc.Child.FamilyDetail(
                    bloc =
                        familyDetail.create(
                            context = context,
                            familyId = config.familyId,
                            output = ::handleDetailOutput,
                        )
                )
        }

    private fun handleListOutput(output: FamilyListBloc.Output) {
        when (output) {
            FamilyListBloc.Output.Back -> this.output.onNext(Output.Back)
            is FamilyListBloc.Output.OpenFamily ->
                navigation.bringToFront(Configuration.FamilyDetail(output.familyId))
        }
    }

    private fun handleDetailOutput(output: FamilyDetailBloc.Output) {
        when (output) {
            // Both return to the list. Closed means the family was left or deleted, so the list
            // underneath is stale — it refreshes itself on resume, which popping triggers.
            FamilyDetailBloc.Output.Back,
            FamilyDetailBloc.Output.Closed -> navigation.pop()
        }
    }

    @Serializable
    private sealed class Configuration {
        @Serializable data object FamilyList : Configuration()

        @Serializable data class FamilyDetail(val familyId: String) : Configuration()
    }
}
