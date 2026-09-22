package com.plusmobileapps.chefmate.recipe.bottomnav.impl

import com.arkivanov.decompose.Cancellation
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.bringToFront
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.lifecycle.doOnPause
import com.arkivanov.essenty.lifecycle.doOnResume
import com.plusmobileapps.chefmate.BlocContext
import com.plusmobileapps.chefmate.Consumer
import com.plusmobileapps.chefmate.browser.BrowserRootBloc
import com.plusmobileapps.chefmate.di.AppScope
import com.plusmobileapps.chefmate.getViewModel
import com.plusmobileapps.chefmate.grocery.core.list.GroceryListBloc
import com.plusmobileapps.chefmate.mapState
import com.plusmobileapps.chefmate.meal.core.MealPlanBloc
import com.plusmobileapps.chefmate.recipe.bottomnav.BottomNavBloc
import com.plusmobileapps.chefmate.recipe.bottomnav.BottomNavBloc.Child.Browser
import com.plusmobileapps.chefmate.recipe.bottomnav.BottomNavBloc.Child.GroceryList
import com.plusmobileapps.chefmate.recipe.bottomnav.BottomNavBloc.Child.Meals
import com.plusmobileapps.chefmate.recipe.bottomnav.BottomNavBloc.Child.RecipeList
import com.plusmobileapps.chefmate.recipe.bottomnav.BottomNavBloc.Child.Settings
import com.plusmobileapps.chefmate.recipe.bottomnav.BottomNavBloc.Output.OpenAppSettings
import com.plusmobileapps.chefmate.recipe.bottomnav.BottomNavBloc.Output.OpenManageProfile
import com.plusmobileapps.chefmate.recipe.bottomnav.BottomNavBloc.Output.OpenSignIn
import com.plusmobileapps.chefmate.recipe.bottomnav.BottomNavBloc.Output.OpenSignUp
import com.plusmobileapps.chefmate.recipe.list.RecipeListBloc
import com.plusmobileapps.chefmate.settings.SettingsBloc
import com.plusmobileapps.metro.extensions.assistedfactory.ContributesAssistedFactory
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedInject
import dev.zacsweers.metro.Provider
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable

@AssistedInject
@ContributesAssistedFactory(scope = AppScope::class, assistedFactory = BottomNavBloc.Factory::class)
class BottomNavBlocImpl(
    @Assisted context: BlocContext,
    @Assisted private val output: Consumer<BottomNavBloc.Output>,
    @Assisted initialTab: BottomNavBloc.Tab?,
    viewModelFactory: Provider<BottomNavViewModel>,
    private val browser: BrowserRootBloc.Factory,
    private val groceryList: GroceryListBloc.Factory,
    private val mealPlan: MealPlanBloc.Factory,
    private val recipeList: RecipeListBloc.Factory,
    private val settings: SettingsBloc.Factory,
) : BottomNavBloc, BlocContext by context {

    private val scope = createScope()

    private val viewModel: BottomNavViewModel = instanceKeeper.getViewModel { viewModelFactory() }

    private val navigation = StackNavigation<Configuration>()

    private var browserRootBloc: BrowserRootBloc? = null
    private var pendingUrl: String? = null

    private val stack =
        childStack(
            source = navigation,
            serializer = Configuration.serializer(),
            initialStack = { listOf(initialTab.toInitialConfiguration()) },
            handleBackButton = true,
            key = "BottomNavRouter",
            childFactory = ::createChild,
        )

    private fun BottomNavBloc.Tab?.toInitialConfiguration(): Configuration =
        when (this) {
            null,
            BottomNavBloc.Tab.RECIPES -> Configuration.Recipe
            BottomNavBloc.Tab.GROCERIES -> Configuration.Grocery
            BottomNavBloc.Tab.MEALS -> Configuration.Meals
            BottomNavBloc.Tab.BROWSER -> Configuration.Browser
            BottomNavBloc.Tab.SETTINGS -> Configuration.Settings
        }

    init {
        initialTab?.let(viewModel::selectTab)
        observeRouter()
    }

    override val state: StateFlow<BottomNavBloc.Model> =
        viewModel.state.mapState {
            BottomNavBloc.Model(
                selectedTab = it.selectedTab,
                tabs = it.tabs.toImmutableList(),
                notificationCount = it.notificationCount,
            )
        }

    override val content: Value<ChildStack<*, BottomNavBloc.Child>> = stack

    override fun onBackClicked() {
        navigation.pop()
    }

    override fun handleSharedUrl(url: String) {
        val existingBloc = browserRootBloc
        if (existingBloc != null) {
            existingBloc.navigateToUrl(url)
        } else {
            pendingUrl = url
        }
        navigation.bringToFront(Configuration.Browser)
        viewModel.selectTab(BottomNavBloc.Tab.BROWSER)
    }

    override fun onExportFinished() {
        // The recipe-list child stays cached in the bottom-nav stack while the user is on the
        // exporter, so its bloc is still alive and ready to hear "you can drop selection mode now".
        stack.value.items
            .map { it.instance }
            .filterIsInstance<RecipeList>()
            .firstOrNull()
            ?.bloc
            ?.onExportFinished()
    }

    override fun onTabSelected(tab: BottomNavBloc.Tab) {
        val configuration =
            when (tab) {
                BottomNavBloc.Tab.RECIPES -> Configuration.Recipe
                BottomNavBloc.Tab.GROCERIES -> Configuration.Grocery
                BottomNavBloc.Tab.MEALS -> Configuration.Meals
                BottomNavBloc.Tab.BROWSER -> Configuration.Browser
                BottomNavBloc.Tab.SETTINGS -> Configuration.Settings
            }
        navigation.bringToFront(configuration)
        viewModel.selectTab(tab)
    }

    private fun createChild(
        configuration: Configuration,
        context: BlocContext,
    ): BottomNavBloc.Child =
        when (configuration) {
            Configuration.Recipe -> {
                RecipeList(recipeList.create(context = context, output = ::handleRecipeListOutput))
            }

            Configuration.Browser -> {
                val bloc = browser.create(context = context, output = ::handleBrowserOutput)
                browserRootBloc = bloc
                pendingUrl?.let { url ->
                    bloc.navigateToUrl(url)
                    pendingUrl = null
                }
                Browser(bloc)
            }

            Configuration.Grocery -> {
                val bloc = groceryList.create(context = context, output = ::handleGroceryListOutput)
                GroceryList(bloc)
            }

            Configuration.Meals -> {
                val bloc = mealPlan.create(context = context, output = ::handleMealPlanOutput)
                Meals(bloc)
            }

            Configuration.Settings -> {
                val bloc = settings.create(context = context, output = ::handleSettingsOutput)
                Settings(bloc)
            }
        }

    private fun handleRecipeListOutput(output: RecipeListBloc.Output) {
        when (output) {
            RecipeListBloc.Output.AddNewRecipe -> {
                this.output.onNext(BottomNavBloc.Output.AddNewRecipe)
            }
            is RecipeListBloc.Output.OpenRecipe -> {
                this.output.onNext(BottomNavBloc.Output.OpenRecipe(output.recipeId))
            }
            RecipeListBloc.Output.OpenBrowser -> {
                onTabSelected(BottomNavBloc.Tab.BROWSER)
            }
            is RecipeListBloc.Output.OpenCookMode -> {
                this.output.onNext(BottomNavBloc.Output.OpenCookMode(output.recipeId))
            }
            is RecipeListBloc.Output.OpenExportRecipes -> {
                this.output.onNext(BottomNavBloc.Output.OpenExportRecipes(output.recipeIds))
            }
            is RecipeListBloc.Output.OpenScannedRecipe -> {
                this.output.onNext(
                    BottomNavBloc.Output.OpenExtractedRecipe(
                        extracted = output.extracted,
                        consumePendingPhoto = true,
                    )
                )
            }
            is RecipeListBloc.Output.OpenEditRecipeBook -> {
                this.output.onNext(BottomNavBloc.Output.OpenEditRecipeBook(output.bookId))
            }
        }
    }

    private fun handleBrowserOutput(output: BrowserRootBloc.Output) {
        when (output) {
            is BrowserRootBloc.Output.RecipeExtracted -> {
                this.output.onNext(BottomNavBloc.Output.OpenExtractedRecipe(output.extracted))
            }
        }
    }

    private fun handleGroceryListOutput(output: GroceryListBloc.Output) {
        when (output) {
            GroceryListBloc.Output.OpenRecipes -> {
                onTabSelected(BottomNavBloc.Tab.RECIPES)
            }
            is GroceryListBloc.Output.OpenEditList -> {
                this.output.onNext(BottomNavBloc.Output.OpenEditGroceryList(output.listId))
            }
            GroceryListBloc.Output.OpenAutocompleteSettings -> {
                this.output.onNext(BottomNavBloc.Output.OpenGroceryAutocompleteSettings)
            }
        }
    }

    private fun handleMealPlanOutput(output: MealPlanBloc.Output) {
        when (output) {
            is MealPlanBloc.Output.OpenRecipe ->
                this.output.onNext(BottomNavBloc.Output.OpenRecipe(output.recipeId))
            is MealPlanBloc.Output.OpenMealPlanner ->
                this.output.onNext(BottomNavBloc.Output.OpenMealPlanner(output.props))
            is MealPlanBloc.Output.OpenCookMode ->
                this.output.onNext(BottomNavBloc.Output.OpenCookMode(output.recipeId))
        }
    }

    private fun handleSettingsOutput(output: SettingsBloc.Output) {
        when (output) {
            SettingsBloc.Output.OpenSignIn -> OpenSignIn
            SettingsBloc.Output.OpenSignUp -> OpenSignUp
            SettingsBloc.Output.OpenManageProfile -> OpenManageProfile
            SettingsBloc.Output.OpenNotifications -> BottomNavBloc.Output.OpenNotifications
            SettingsBloc.Output.OpenManageFamily -> BottomNavBloc.Output.OpenManageFamily
            SettingsBloc.Output.OpenAppSettings -> OpenAppSettings
            SettingsBloc.Output.OpenAiChat -> BottomNavBloc.Output.OpenAiChat
            SettingsBloc.Output.OpenDeveloperSettings -> BottomNavBloc.Output.OpenDeveloperSettings
            SettingsBloc.Output.OpenOnboarding -> BottomNavBloc.Output.OpenOnboarding
            is SettingsBloc.Output.OpenUrl -> BottomNavBloc.Output.OpenUrl(output.url)
        }.let { this.output.onNext(it) }
    }

    private fun observeRouter() {
        var cancellation: Cancellation? = null
        lifecycle.doOnResume {
            cancellation = stack.subscribe { value ->
                when (value.active.instance) {
                    is Browser -> BottomNavBloc.Tab.BROWSER
                    is BottomNavBloc.Child.GroceryList -> BottomNavBloc.Tab.GROCERIES
                    is Meals -> BottomNavBloc.Tab.MEALS
                    is BottomNavBloc.Child.RecipeList -> BottomNavBloc.Tab.RECIPES
                    is Settings -> BottomNavBloc.Tab.SETTINGS
                }.let(viewModel::selectTab)
            }
        }
        lifecycle.doOnPause {
            cancellation?.cancel()
            cancellation = null
        }
    }

    @Serializable
    private sealed class Configuration {
        @Serializable data object Recipe : Configuration()

        @Serializable data object Grocery : Configuration()

        @Serializable data object Meals : Configuration()

        @Serializable data object Browser : Configuration()

        @Serializable data object Settings : Configuration()
    }
}
