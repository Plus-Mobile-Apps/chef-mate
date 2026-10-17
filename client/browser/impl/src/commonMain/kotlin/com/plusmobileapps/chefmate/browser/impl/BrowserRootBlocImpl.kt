@file:OptIn(DelicateDecomposeApi::class)

package com.plusmobileapps.chefmate.browser.impl

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.Dp
import chefmate.client.browser.public.generated.resources.Res
import chefmate.client.browser.public.generated.resources.browser_open_in_default_browser
import com.arkivanov.decompose.Cancellation
import com.arkivanov.decompose.DelicateDecomposeApi
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.navigate
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.router.stack.replaceAll
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.backhandler.BackCallback
import com.arkivanov.essenty.lifecycle.doOnPause
import com.arkivanov.essenty.lifecycle.doOnResume
import com.plusmobileapps.chefmate.BlocContext
import com.plusmobileapps.chefmate.Consumer
import com.plusmobileapps.chefmate.browser.BrowserBloc
import com.plusmobileapps.chefmate.browser.BrowserEditQueryBloc
import com.plusmobileapps.chefmate.browser.BrowserLandingBloc
import com.plusmobileapps.chefmate.browser.BrowserPreferences
import com.plusmobileapps.chefmate.browser.BrowserRootBloc
import com.plusmobileapps.chefmate.browser.BrowserRootScreen
import com.plusmobileapps.chefmate.browser.BrowserSelectEngineBloc
import com.plusmobileapps.chefmate.di.AppScope
import com.plusmobileapps.chefmate.text.FixedString
import com.plusmobileapps.chefmate.text.asTextData
import com.plusmobileapps.chefmate.ui.components.PlusHeaderContainer
import com.plusmobileapps.chefmate.ui.components.PlusHeaderData
import com.plusmobileapps.metro.extensions.assistedfactory.ContributesAssistedFactory
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedInject
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@AssistedInject
@ContributesAssistedFactory(
    scope = AppScope::class,
    assistedFactory = BrowserRootBloc.Factory::class,
)
class BrowserRootBlocImpl(
    @Assisted context: BlocContext,
    @Assisted private val output: Consumer<BrowserRootBloc.Output>,
    @Assisted private val initialUrl: String?,
    @Assisted private val showControls: Boolean,
    @Assisted private val presentation: BrowserRootBloc.Presentation,
    private val browserBlocFactory: BrowserBloc.Factory,
    private val landingBlocFactory: BrowserLandingBloc.Factory,
    private val editQueryBlocFactory: BrowserEditQueryBloc.Factory,
    private val selectEngineBlocFactory: BrowserSelectEngineBloc.Factory,
    private val browserPreferences: BrowserPreferences,
) : BrowserRootBloc, BlocContext by context {

    private val navigation = StackNavigation<Configuration>()
    private val scope = createScope()

    private val webViewBackCallback =
        BackCallback(isEnabled = false) {
            val activeChild = stack.value.active.instance
            if (activeChild is BrowserRootBloc.Child.Browser) {
                activeChild.bloc.onGoBack()
            }
        }

    private val stack =
        childStack(
            source = navigation,
            serializer = Configuration.serializer(),
            initialStack = {
                if (initialUrl != null) listOf(Configuration.Browser(initialUrl))
                else listOf(homeConfiguration())
            },
            handleBackButton = true,
            key = "BrowserRootRouter",
            childFactory = ::createChild,
        )

    init {
        backHandler.register(webViewBackCallback)
        observeCanGoBack()
    }

    override val routerState: Value<ChildStack<*, BrowserRootBloc.Child>> = stack

    override fun navigateToUrl(url: String) {
        navigation.replaceAll(Configuration.Browser(url))
    }

    @Composable
    override fun Content(modifier: Modifier) {
        when (val p = presentation) {
            BrowserRootBloc.Presentation.Embedded ->
                BrowserRootScreen(bloc = this, modifier = modifier)
            is BrowserRootBloc.Presentation.Modal -> {
                val childStack by routerState.subscribeAsState()
                val activeChild = childStack.active.instance
                val currentUrl =
                    if (activeChild is BrowserRootBloc.Child.Browser) {
                        activeChild.bloc.state.collectAsState().value.currentUrl
                    } else {
                        ""
                    }
                val uriHandler = LocalUriHandler.current
                PlusHeaderContainer(
                    modifier = modifier,
                    data =
                        PlusHeaderData.Modal(
                            title = FixedString(""),
                            onCloseClick = p.onClose,
                            trailingAccessory =
                                if (currentUrl.isBlank()) null
                                else
                                    PlusHeaderData.TrailingAccessory.Icon(
                                        icon = Icons.AutoMirrored.Filled.OpenInNew,
                                        contentDesc =
                                            Res.string.browser_open_in_default_browser.asTextData(),
                                        // Guarded because a scheme with no handler installed makes
                                        // the platform handler throw.
                                        onClick = {
                                            runCatching { uriHandler.openUri(currentUrl) }
                                        },
                                    ),
                        ),
                    scrollEnabled = false,
                    maxContentWidth = Dp.Unspecified,
                    content = { BrowserRootScreen(bloc = this@BrowserRootBlocImpl) },
                )
            }
        }
    }

    private fun observeCanGoBack() {
        var stackCancellation: Cancellation? = null
        var canGoBackJob: Job? = null

        lifecycle.doOnResume {
            stackCancellation = stack.subscribe { childStack ->
                canGoBackJob?.cancel()
                val activeChild = childStack.active.instance
                if (activeChild is BrowserRootBloc.Child.Browser) {
                    canGoBackJob = scope.launch {
                        activeChild.bloc.state.collect { state ->
                            webViewBackCallback.isEnabled = state.canGoBack
                        }
                    }
                } else {
                    webViewBackCallback.isEnabled = false
                }
            }
        }

        lifecycle.doOnPause {
            canGoBackJob?.cancel()
            canGoBackJob = null
            stackCancellation?.cancel()
            stackCancellation = null
            webViewBackCallback.isEnabled = false
        }
    }

    private fun createChild(config: Configuration, context: BlocContext): BrowserRootBloc.Child =
        when (config) {
            Configuration.SelectEngine ->
                BrowserRootBloc.Child.SelectEngine(
                    selectEngineBlocFactory.create(context) { selectOutput ->
                        when (selectOutput) {
                            BrowserSelectEngineBloc.Output.EngineSelected ->
                                navigation.replaceAll(Configuration.Landing)
                        }
                    }
                )
            Configuration.Landing ->
                BrowserRootBloc.Child.Landing(
                    landingBlocFactory.create(context) { landingOutput ->
                        when (landingOutput) {
                            BrowserLandingBloc.Output.OpenEditQuery ->
                                navigation.replaceTopEditQueryWith(Configuration.EditQuery(""))
                            is BrowserLandingBloc.Output.OpenUrl ->
                                navigation.pushBrowser(Configuration.Browser(landingOutput.url))
                        }
                    }
                )
            is Configuration.EditQuery ->
                BrowserRootBloc.Child.EditQuery(
                    editQueryBlocFactory.create(
                        context = context,
                        output = { editOutput ->
                            when (editOutput) {
                                is BrowserEditQueryBloc.Output.Navigate ->
                                    navigation.replaceEditQueryWithBrowser(
                                        Configuration.Browser(editOutput.url)
                                    )
                                BrowserEditQueryBloc.Output.Cancel -> navigation.pop()
                            }
                        },
                        initialText = config.initialText,
                    )
                )
            is Configuration.Browser ->
                BrowserRootBloc.Child.Browser(
                    browserBlocFactory
                        .create(
                            context = context,
                            output = { blocOutput ->
                                when (blocOutput) {
                                    is BrowserBloc.Output.RecipeExtracted ->
                                        output.onNext(
                                            BrowserRootBloc.Output.RecipeExtracted(
                                                blocOutput.extracted
                                            )
                                        )
                                    is BrowserBloc.Output.NavigateToLanding ->
                                        navigation.replaceTopEditQueryWith(
                                            Configuration.EditQuery(blocOutput.initialText)
                                        )
                                    BrowserBloc.Output.NavigateHome ->
                                        navigation.replaceAll(homeConfiguration())
                                }
                            },
                            showControls = showControls,
                        )
                        .also { bloc ->
                            bloc.onUrlChanged(config.url)
                            bloc.onNavigate()
                        }
                )
        }

    /** The landing screen, or the first-run engine picker until a default engine is chosen. */
    private fun homeConfiguration(): Configuration =
        if (browserPreferences.defaultSearchEngine.value == null) Configuration.SelectEngine
        else Configuration.Landing

    /**
     * Drops any existing EditQuery from the stack before adding the new one. Decompose enforces
     * unique configurations within a stack, so re-entering the EditQuery flow with the same
     * initialText (e.g. tapping the X clear button which triggers focus on an empty field) would
     * otherwise crash.
     */
    private fun StackNavigation<Configuration>.replaceTopEditQueryWith(
        newConfig: Configuration.EditQuery
    ) {
        navigate { stack -> stack.filter { it !is Configuration.EditQuery } + newConfig }
    }

    /**
     * Adds [newConfig] on top, dropping any existing Browser for the same URL first — Decompose
     * enforces unique configurations, so opening the same page twice would otherwise crash.
     */
    private fun StackNavigation<Configuration>.pushBrowser(newConfig: Configuration.Browser) {
        navigate { stack -> stack.filter { it != newConfig } + newConfig }
    }

    /**
     * Pops the EditQuery on top, drops any existing Browser with the same URL (to avoid duplicate
     * configuration crashes when submitting the same URL twice), and adds the new Browser.
     */
    private fun StackNavigation<Configuration>.replaceEditQueryWithBrowser(
        newConfig: Configuration.Browser
    ) {
        navigate { stack -> stack.dropLast(1).filter { it != newConfig } + newConfig }
    }

    @Serializable
    private sealed class Configuration {
        @Serializable data object SelectEngine : Configuration()

        @Serializable data object Landing : Configuration()

        @Serializable data class EditQuery(val initialText: String) : Configuration()

        @Serializable data class Browser(val url: String) : Configuration()
    }
}
