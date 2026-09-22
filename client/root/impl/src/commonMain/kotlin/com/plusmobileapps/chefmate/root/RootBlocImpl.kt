@file:OptIn(DelicateDecomposeApi::class)

package com.plusmobileapps.chefmate.root

import com.arkivanov.decompose.DelicateDecomposeApi
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.bringToFront
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.navigate
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.router.stack.popWhile
import com.arkivanov.decompose.router.stack.replaceAll
import com.arkivanov.decompose.value.Value
import com.plusmobileapps.chefmate.BlocContext
import com.plusmobileapps.chefmate.aichat.AiChatRootBloc
import com.plusmobileapps.chefmate.auth.data.AuthState
import com.plusmobileapps.chefmate.auth.data.AuthenticationRepository
import com.plusmobileapps.chefmate.auth.data.OtpFlow
import com.plusmobileapps.chefmate.auth.ui.AuthenticationBloc
import com.plusmobileapps.chefmate.auth.ui.otp.OtpBloc
import com.plusmobileapps.chefmate.browser.BrowserRootBloc
import com.plusmobileapps.chefmate.cook.CookModeBloc
import com.plusmobileapps.chefmate.devsettings.DeveloperSettingsBloc
import com.plusmobileapps.chefmate.di.AppScope
import com.plusmobileapps.chefmate.di.OnboardingRepository
import com.plusmobileapps.chefmate.family.manage.ManageFamilyRootBloc
import com.plusmobileapps.chefmate.featureflag.FeatureFlags
import com.plusmobileapps.chefmate.featureflag.FeatureFlagsBloc
import com.plusmobileapps.chefmate.grocery.core.edit.EditGroceryListBloc
import com.plusmobileapps.chefmate.notifications.NotificationsBloc
import com.plusmobileapps.chefmate.onboarding.OnboardingRootBloc
import com.plusmobileapps.chefmate.profile.ManageProfileBloc
import com.plusmobileapps.chefmate.recipe.bottomnav.BottomNavBloc
import com.plusmobileapps.chefmate.recipe.core.addmeal.MealPlannerRootBloc
import com.plusmobileapps.chefmate.recipe.core.root.RecipeRootBloc
import com.plusmobileapps.chefmate.recipe.core.root.RecipeRootBloc.Props.Detail
import com.plusmobileapps.chefmate.recipe.core.share.PublicRecipeBloc
import com.plusmobileapps.chefmate.recipe.exporter.ExportRecipesBloc
import com.plusmobileapps.chefmate.recipebook.edit.EditRecipeBookBloc
import com.plusmobileapps.chefmate.root.RootBloc.Child.BottomNavigation
import com.plusmobileapps.chefmate.root.RootBlocImpl.Configuration.RecipeRoot
import com.plusmobileapps.chefmate.settings.root.SettingsRootBloc
import com.plusmobileapps.metro.extensions.assistedfactory.ContributesAssistedFactory
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedInject
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@AssistedInject
@ContributesAssistedFactory(scope = AppScope::class, assistedFactory = RootBloc.Factory::class)
class RootBlocImpl(
    @Assisted context: BlocContext,
    @Assisted deepLink: DeepLink,
    private val onboardingRepository: OnboardingRepository,
    private val authenticationRepository: AuthenticationRepository,
    private val onboardingRoot: OnboardingRootBloc.Factory,
    private val bottomNav: BottomNavBloc.Factory,
    private val browserRootBlocFactory: BrowserRootBloc.Factory,
    private val recipeRoot: RecipeRootBloc.Factory,
    private val publicRecipe: PublicRecipeBloc.Factory,
    private val mealPlannerRoot: MealPlannerRootBloc.Factory,
    private val authentication: AuthenticationBloc.Factory,
    private val otpBloc: OtpBloc.Factory,
    private val settingsRoot: SettingsRootBloc.Factory,
    private val manageProfile: ManageProfileBloc.Factory,
    private val notifications: NotificationsBloc.Factory,
    private val manageFamilyRoot: ManageFamilyRootBloc.Factory,
    private val developerSettings: DeveloperSettingsBloc.Factory,
    private val cookMode: CookModeBloc.Factory,
    private val featureFlags: FeatureFlags,
    private val featureFlagsBlocFactory: FeatureFlagsBloc.Factory,
    private val aiChat: AiChatRootBloc.Factory,
    private val exportRecipes: ExportRecipesBloc.Factory,
    private val editRecipeBook: EditRecipeBookBloc.Factory,
    private val editGroceryList: EditGroceryListBloc.Factory,
) : RootBloc, BlocContext by context {

    init {
        createScope().launch { featureFlags.refresh() }
    }

    private val initialBottomNavTab: BottomNavBloc.Tab? =
        when (deepLink) {
            DeepLink.Groceries -> BottomNavBloc.Tab.GROCERIES
            DeepLink.MealPlanner -> BottomNavBloc.Tab.MEALS
            else -> null
        }

    private val navigation = StackNavigation<Configuration>()

    private val stack =
        childStack(
            source = navigation,
            serializer = Configuration.serializer(),
            initialStack = { initialStackFor(deepLink) },
            handleBackButton = true,
            key = "RootRouter",
            childFactory = ::createChild,
        )

    private fun initialStackFor(deepLink: DeepLink): List<Configuration> {
        // First run: show onboarding before anything else. Deep links are honored once onboarding
        // has been completed on a previous launch.
        if (!onboardingRepository.hasCompletedOnboarding) {
            return listOf(Configuration.Onboarding())
        }
        return when (deepLink) {
            DeepLink.None,
            DeepLink.Groceries,
            DeepLink.MealPlanner -> listOf(Configuration.BottomNavigation)
            is DeepLink.RecipeDetail ->
                listOf(Configuration.BottomNavigation, RecipeRoot(Detail(deepLink.recipeId)))
            is DeepLink.PublicRecipe ->
                listOf(
                    Configuration.BottomNavigation,
                    Configuration.PublicRecipe(deepLink.remoteId),
                )
            DeepLink.AppSettings ->
                listOf(Configuration.BottomNavigation, Configuration.SettingsRoot())
            DeepLink.Notifications ->
                listOf(Configuration.BottomNavigation, Configuration.Notifications)
            DeepLink.SignIn ->
                listOf(
                    Configuration.BottomNavigation,
                    Configuration.Authentication(AuthenticationBloc.Props.SignIn),
                )
            DeepLink.SignUp ->
                listOf(
                    Configuration.BottomNavigation,
                    Configuration.Authentication(AuthenticationBloc.Props.SignUp),
                )
        }
    }

    override val state: Value<ChildStack<*, RootBloc.Child>> = stack

    override fun onBackClicked() {
        navigation.pop()
    }

    override fun handleSharedUrl(url: String) {
        val bottomNavChild =
            stack.value.items.map { it.instance }.filterIsInstance<BottomNavigation>().firstOrNull()
        bottomNavChild?.bloc?.handleSharedUrl(url)
        navigation.bringToFront(Configuration.BottomNavigation)
    }

    override fun handleDeepLink(url: String) {
        // Only routed once past onboarding — a first-run user has no populated stack to return to.
        if (!onboardingRepository.hasCompletedOnboarding) return
        when (val deepLink = DeepLink.parse(url)) {
            DeepLink.Notifications -> navigation.bringToFront(Configuration.Notifications)
            DeepLink.AppSettings -> navigation.bringToFront(Configuration.SettingsRoot())
            is DeepLink.RecipeDetail ->
                navigation.bringToFront(RecipeRoot(Detail(deepLink.recipeId)))
            is DeepLink.PublicRecipe ->
                navigation.bringToFront(Configuration.PublicRecipe(deepLink.remoteId))
            DeepLink.Groceries -> {
                selectBottomNavTab(BottomNavBloc.Tab.GROCERIES)
                navigation.bringToFront(Configuration.BottomNavigation)
            }
            DeepLink.MealPlanner -> {
                selectBottomNavTab(BottomNavBloc.Tab.MEALS)
                navigation.bringToFront(Configuration.BottomNavigation)
            }
            DeepLink.SignIn ->
                navigation.bringToFront(
                    Configuration.Authentication(AuthenticationBloc.Props.SignIn)
                )
            DeepLink.SignUp ->
                navigation.bringToFront(
                    Configuration.Authentication(AuthenticationBloc.Props.SignUp)
                )
            DeepLink.None -> Unit
        }
    }

    override fun handleRecipeWindowOutput(output: RecipeRootBloc.Output) {
        // Finished means "the detached window is done" — it closes itself, and the main window's
        // stack has nothing to pop in response.
        if (output == RecipeRootBloc.Output.Finished) return
        handleRecipeRootOutput(output)
    }

    private fun selectBottomNavTab(tab: BottomNavBloc.Tab) {
        stack.value.items
            .map { it.instance }
            .filterIsInstance<BottomNavigation>()
            .firstOrNull()
            ?.bloc
            ?.onTabSelected(tab)
    }

    private fun createChild(config: Configuration, context: BlocContext): RootBloc.Child =
        when (config) {
            is Configuration.Onboarding ->
                RootBloc.Child.Onboarding(
                    bloc =
                        onboardingRoot.create(
                            context = context,
                            props =
                                OnboardingRootBloc.Props(
                                    // Re-entered flows can be backed out of; a first run can't.
                                    isDismissible = config.reentry,
                                    // A signed-in (non-anonymous) user can't sign in or up again.
                                    isSignedIn = isSignedIn(),
                                ),
                            output = ::handleOnboardingOutput,
                        )
                )

            Configuration.BottomNavigation ->
                BottomNavigation(
                    bottomNav.create(
                        context = context,
                        output = ::handleBottomNavOutput,
                        initialTab = initialBottomNavTab,
                    )
                )

            is Configuration.RecipeRoot ->
                RootBloc.Child.RecipeRoot(
                    bloc =
                        recipeRoot.create(
                            context = context,
                            props = config.props,
                            output = ::handleRecipeRootOutput,
                        )
                )

            is Configuration.PublicRecipe ->
                RootBloc.Child.PublicRecipe(
                    bloc =
                        publicRecipe.create(
                            context = context,
                            remoteId = config.remoteId,
                            output = ::handlePublicRecipeOutput,
                        )
                )

            is Configuration.Authentication ->
                RootBloc.Child.Authentication(
                    bloc =
                        authentication.create(
                            context = context,
                            props = config.props,
                            output = ::handleAuthenticationOutput,
                        )
                )

            is Configuration.OtpVerification ->
                RootBloc.Child.OtpVerification(
                    bloc =
                        otpBloc.create(
                            context = context,
                            props = OtpBloc.Props(email = config.email, flow = config.flow),
                            output = ::handleOtpOutput,
                        )
                )

            is Configuration.Browser ->
                RootBloc.Child.Browser(
                    bloc =
                        browserRootBlocFactory.create(
                            context = context,
                            output = { _ -> navigation.pop() },
                            initialUrl = config.url,
                            showControls = false,
                            presentation =
                                BrowserRootBloc.Presentation.Modal(onClose = { navigation.pop() }),
                        )
                )

            is Configuration.MealPlanner ->
                RootBloc.Child.MealPlanner(
                    bloc =
                        mealPlannerRoot.create(
                            context = context,
                            props = config.props,
                            output = ::handleMealPlannerOutput,
                        )
                )

            is Configuration.SettingsRoot ->
                RootBloc.Child.SettingsRoot(
                    bloc =
                        settingsRoot.create(
                            context = context,
                            props = config.props,
                            output = ::handleSettingsRootOutput,
                        )
                )

            Configuration.ManageProfile ->
                RootBloc.Child.ManageProfile(
                    bloc =
                        manageProfile.create(
                            context = context,
                            output = ::handleManageProfileOutput,
                        )
                )

            Configuration.Notifications ->
                RootBloc.Child.Notifications(
                    bloc =
                        notifications.create(
                            context = context,
                            output = ::handleNotificationsOutput,
                        )
                )

            Configuration.ManageFamily ->
                RootBloc.Child.ManageFamily(
                    bloc =
                        manageFamilyRoot.create(
                            context = context,
                            output = ::handleManageFamilyOutput,
                        )
                )

            Configuration.DeveloperSettings ->
                RootBloc.Child.DeveloperSettings(
                    bloc =
                        developerSettings.create(
                            context = context,
                            output = ::handleDeveloperSettingsOutput,
                        )
                )

            Configuration.FeatureFlags ->
                RootBloc.Child.FeatureFlags(
                    bloc =
                        featureFlagsBlocFactory.create(
                            context = context,
                            output = ::handleFeatureFlagsOutput,
                        )
                )

            is Configuration.CookMode ->
                RootBloc.Child.CookMode(
                    bloc =
                        cookMode.create(
                            context = context,
                            initialRecipeId = config.recipeId,
                            output = ::handleCookModeOutput,
                        )
                )

            is Configuration.AiChat ->
                RootBloc.Child.AiChat(
                    bloc =
                        aiChat.create(
                            context = context,
                            recipeContextId = config.recipeContextId,
                            output = ::handleAiChatOutput,
                        )
                )

            is Configuration.ExportRecipes ->
                RootBloc.Child.ExportRecipes(
                    bloc =
                        exportRecipes.create(
                            context = context,
                            props =
                                config.recipeIds?.let { ExportRecipesBloc.Props.Selected(it) }
                                    ?: ExportRecipesBloc.Props.All,
                            output = ::handleExportRecipesOutput,
                        )
                )

            is Configuration.EditRecipeBook ->
                RootBloc.Child.EditRecipeBook(
                    bloc =
                        editRecipeBook.create(
                            context = context,
                            props =
                                config.bookId?.let { EditRecipeBookBloc.Props.Edit(it) }
                                    ?: EditRecipeBookBloc.Props.Create,
                            output = ::handleEditRecipeBookOutput,
                        )
                )

            is Configuration.EditGroceryList ->
                RootBloc.Child.EditGroceryList(
                    bloc =
                        editGroceryList.create(
                            context = context,
                            listId = config.listId,
                            output = ::handleEditGroceryListOutput,
                        )
                )
        }

    /** A real (non-anonymous) session — an anonymous user can still sign in to upgrade. */
    private fun isSignedIn(): Boolean =
        (authenticationRepository.state.value as? AuthState.Authenticated)?.user?.isAnonymous ==
            false

    /**
     * Where to go once the user finishes signing in, for destinations that need a real account. Set
     * by [openWhenSignedIn] and consumed by [finishAuthentication]; cleared if the auth flow is
     * abandoned so a later, unrelated sign-in doesn't strand the user somewhere they didn't ask
     * for.
     */
    private var destinationAfterAuth: Configuration? = null

    /**
     * Opens [configuration] for a signed-in user, or sends them through sign-in first and lands
     * them there once it succeeds. An anonymous session counts as signed out: family membership is
     * keyed on an account email, which a guest doesn't have.
     */
    private fun openWhenSignedIn(configuration: Configuration) {
        if (isSignedIn()) {
            destinationAfterAuth = null
            navigation.bringToFront(configuration)
        } else {
            destinationAfterAuth = configuration
            navigation.bringToFront(Configuration.Authentication(AuthenticationBloc.Props.SignIn))
        }
    }

    private fun handleOnboardingOutput(output: OnboardingRootBloc.Output) {
        when (output) {
            // Onboarding marks itself completed; swap the whole stack for the main app so back
            // can't return to the flow.
            OnboardingRootBloc.Output.Finished ->
                navigation.replaceAll(Configuration.BottomNavigation)
            // Re-entered flow backed out from the first step: pop it off, back to where they were.
            OnboardingRootBloc.Output.Dismissed -> navigation.pop()
            // Open the auth flow on top of onboarding. On success, handleAuthenticationOutput tears
            // the onboarding stack down for us.
            OnboardingRootBloc.Output.SignIn ->
                navigation.bringToFront(
                    Configuration.Authentication(AuthenticationBloc.Props.SignIn)
                )
            OnboardingRootBloc.Output.SignUp ->
                navigation.bringToFront(
                    Configuration.Authentication(AuthenticationBloc.Props.SignUp)
                )
        }
    }

    private fun handleBottomNavOutput(output: BottomNavBloc.Output) {
        when (output) {
            BottomNavBloc.Output.AddNewRecipe -> {
                navigation.bringToFront(RecipeRoot(RecipeRootBloc.Props.Create))
            }

            is BottomNavBloc.Output.OpenRecipe -> {
                navigation.bringToFront(RecipeRoot(Detail(output.recipeId)))
            }

            is BottomNavBloc.Output.OpenExtractedRecipe -> {
                navigation.bringToFront(
                    RecipeRoot(
                        RecipeRootBloc.Props.CreateFromExtracted(
                            extracted = output.extracted,
                            consumePendingPhoto = output.consumePendingPhoto,
                        )
                    )
                )
            }

            BottomNavBloc.Output.OpenSignIn -> {
                navigation.bringToFront(
                    Configuration.Authentication(AuthenticationBloc.Props.SignIn)
                )
            }

            BottomNavBloc.Output.OpenSignUp -> {
                navigation.bringToFront(
                    Configuration.Authentication(AuthenticationBloc.Props.SignUp)
                )
            }

            is BottomNavBloc.Output.OpenUrl -> {
                navigation.bringToFront(Configuration.Browser(output.url))
            }

            is BottomNavBloc.Output.OpenMealPlanner -> {
                navigation.bringToFront(Configuration.MealPlanner(output.props))
            }

            BottomNavBloc.Output.OpenManageProfile -> {
                navigation.bringToFront(Configuration.ManageProfile)
            }

            BottomNavBloc.Output.OpenNotifications -> {
                navigation.bringToFront(Configuration.Notifications)
            }

            BottomNavBloc.Output.OpenManageFamily -> {
                openWhenSignedIn(Configuration.ManageFamily)
            }

            BottomNavBloc.Output.OpenAppSettings -> {
                navigation.bringToFront(Configuration.SettingsRoot())
            }

            BottomNavBloc.Output.OpenGroceryAutocompleteSettings -> {
                navigation.bringToFront(
                    Configuration.SettingsRoot(SettingsRootBloc.Props.GroceryAutocomplete)
                )
            }

            BottomNavBloc.Output.OpenAiChat -> {
                navigation.bringToFront(Configuration.AiChat())
            }

            BottomNavBloc.Output.OpenDeveloperSettings -> {
                navigation.bringToFront(Configuration.DeveloperSettings)
            }

            BottomNavBloc.Output.OpenOnboarding -> {
                navigation.bringToFront(Configuration.Onboarding(reentry = true))
            }

            is BottomNavBloc.Output.OpenCookMode -> {
                navigation.bringToFront(Configuration.CookMode(output.recipeId))
            }

            is BottomNavBloc.Output.OpenExportRecipes -> {
                navigation.bringToFront(Configuration.ExportRecipes(output.recipeIds))
            }

            is BottomNavBloc.Output.OpenEditRecipeBook -> {
                navigation.bringToFront(Configuration.EditRecipeBook(output.bookId))
            }

            is BottomNavBloc.Output.OpenEditGroceryList -> {
                navigation.bringToFront(Configuration.EditGroceryList(output.listId))
            }
        }
    }

    private fun handleExportRecipesOutput(output: ExportRecipesBloc.Output) {
        when (output) {
            ExportRecipesBloc.Output.Back -> navigation.pop()
            ExportRecipesBloc.Output.Finished -> {
                navigation.pop()
                // Tell the recipe list to drop multi-select mode now that the archive was written.
                stack.value.items
                    .map { it.instance }
                    .filterIsInstance<RootBloc.Child.BottomNavigation>()
                    .firstOrNull()
                    ?.bloc
                    ?.onExportFinished()
            }
        }
    }

    private fun handleEditRecipeBookOutput(output: EditRecipeBookBloc.Output) {
        when (output) {
            EditRecipeBookBloc.Output.Finished -> navigation.pop()
        }
    }

    private fun handleEditGroceryListOutput(output: EditGroceryListBloc.Output) {
        when (output) {
            EditGroceryListBloc.Output.Finished -> navigation.pop()
            EditGroceryListBloc.Output.OpenSignIn ->
                navigation.bringToFront(
                    Configuration.Authentication(AuthenticationBloc.Props.SignIn)
                )
            EditGroceryListBloc.Output.OpenSignUp ->
                navigation.bringToFront(
                    Configuration.Authentication(AuthenticationBloc.Props.SignUp)
                )
        }
    }

    private fun handleSettingsRootOutput(output: SettingsRootBloc.Output) {
        when (output) {
            SettingsRootBloc.Output.Back -> navigation.pop()
        }
    }

    private fun handleManageProfileOutput(output: ManageProfileBloc.Output) {
        when (output) {
            // Both pop back to the More tab; on deletion the More tab re-renders unauthenticated
            // on its own once the auth state flips.
            ManageProfileBloc.Output.Back,
            ManageProfileBloc.Output.AccountDeleted -> navigation.pop()
        }
    }

    private fun handleNotificationsOutput(output: NotificationsBloc.Output) {
        when (output) {
            NotificationsBloc.Output.Back -> navigation.pop()
            NotificationsBloc.Output.OpenSignIn ->
                navigation.bringToFront(
                    Configuration.Authentication(AuthenticationBloc.Props.SignIn)
                )
            NotificationsBloc.Output.OpenSignUp ->
                navigation.bringToFront(
                    Configuration.Authentication(AuthenticationBloc.Props.SignUp)
                )
        }
    }

    private fun handleManageFamilyOutput(output: ManageFamilyRootBloc.Output) {
        when (output) {
            ManageFamilyRootBloc.Output.Back -> navigation.pop()
        }
    }

    private fun handleDeveloperSettingsOutput(output: DeveloperSettingsBloc.Output) {
        when (output) {
            DeveloperSettingsBloc.Output.Back -> navigation.pop()
            DeveloperSettingsBloc.Output.OpenFeatureFlags -> {
                navigation.bringToFront(Configuration.FeatureFlags)
            }
        }
    }

    private fun handleFeatureFlagsOutput(output: FeatureFlagsBloc.Output) {
        when (output) {
            FeatureFlagsBloc.Output.Back -> navigation.pop()
        }
    }

    private fun handleRecipeRootOutput(output: RecipeRootBloc.Output) {
        when (output) {
            RecipeRootBloc.Output.Finished -> navigation.pop()
            is RecipeRootBloc.Output.OpenUrl -> {
                navigation.bringToFront(Configuration.Browser(output.url))
            }
            RecipeRootBloc.Output.OpenGroceryList -> {
                val bottomNavChild =
                    stack.value.items
                        .map { it.instance }
                        .filterIsInstance<BottomNavigation>()
                        .firstOrNull()
                bottomNavChild?.bloc?.onTabSelected(BottomNavBloc.Tab.GROCERIES)
                navigation.bringToFront(Configuration.BottomNavigation)
            }
            is RecipeRootBloc.Output.OpenMealPlanner -> {
                navigation.bringToFront(Configuration.MealPlanner(output.props))
            }
            is RecipeRootBloc.Output.OpenCookMode -> {
                navigation.bringToFront(Configuration.CookMode(output.recipeId))
            }
            is RecipeRootBloc.Output.OpenAiChat -> {
                navigation.bringToFront(Configuration.AiChat(recipeContextId = output.recipeId))
            }
        }
    }

    private fun handleCookModeOutput(output: CookModeBloc.Output) {
        when (output) {
            CookModeBloc.Output.Finished -> navigation.pop()
            is CookModeBloc.Output.OpenAiChat ->
                navigation.bringToFront(Configuration.AiChat(recipeContextId = output.recipeId))
        }
    }

    private fun handleAiChatOutput(output: AiChatRootBloc.Output) {
        when (output) {
            AiChatRootBloc.Output.Finished -> navigation.pop()
            is AiChatRootBloc.Output.AddAsRecipe ->
                navigation.bringToFront(
                    RecipeRoot(
                        RecipeRootBloc.Props.CreateFromExtracted(
                            extracted = output.extracted,
                            fromAi = true,
                            consumePendingPhoto = output.consumePendingPhoto,
                        )
                    )
                )
        }
    }

    private fun handleMealPlannerOutput(output: MealPlannerRootBloc.Output) {
        when (output) {
            MealPlannerRootBloc.Output.Finished -> navigation.pop()
        }
    }

    private fun handlePublicRecipeOutput(output: PublicRecipeBloc.Output) {
        when (output) {
            PublicRecipeBloc.Output.Finished -> navigation.pop()
            // Replace the preview with the real detail screen — the recipe is now in the user's
            // library (either already owned, or just saved as a copy), so back shouldn't return to
            // the read-only preview.
            is PublicRecipeBloc.Output.OpenRecipe ->
                replaceCurrentBringingToFront(RecipeRoot(Detail(output.recipeId)))
            is PublicRecipeBloc.Output.OpenUrl ->
                navigation.bringToFront(Configuration.Browser(output.url))
        }
    }

    /**
     * Drops the current top of the stack and brings [configuration] to the front, removing any
     * existing entry of the same class first.
     *
     * Plain `replaceCurrent` can't be used here: the screen being replaced may be sitting directly
     * on top of the very destination it resolves to (open recipe 18 → tap its share link → the
     * read-only preview resolves back to recipe 18), and appending the destination a second time
     * makes Decompose fail with "Configurations must be unique". Every other transition in this
     * stack goes through [bringToFront], so at most one entry per configuration class is the
     * invariant this preserves.
     */
    private fun replaceCurrentBringingToFront(configuration: Configuration) {
        navigation.navigate { stack ->
            stack.dropLast(1).filterNot { it::class == configuration::class } + configuration
        }
    }

    private fun handleAuthenticationOutput(output: AuthenticationBloc.Output) {
        when (output) {
            AuthenticationBloc.Output.Finished -> {
                // Backing out of sign-in abandons whatever it was gating.
                destinationAfterAuth = null
                navigation.pop()
            }
            AuthenticationBloc.Output.AuthenticationSuccess -> finishAuthentication()
            is AuthenticationBloc.Output.EmailVerificationRequired ->
                navigation.bringToFront(
                    Configuration.OtpVerification(email = output.email, flow = OtpFlow.SignUp)
                )
            is AuthenticationBloc.Output.EmailChangeRequired ->
                navigation.bringToFront(
                    Configuration.OtpVerification(email = output.email, flow = OtpFlow.EmailChange)
                )
            is AuthenticationBloc.Output.PasswordlessOtpSent ->
                navigation.bringToFront(
                    Configuration.OtpVerification(
                        email = output.email,
                        flow = OtpFlow.PasswordlessSignIn,
                    )
                )
            is AuthenticationBloc.Output.OpenUrl ->
                navigation.bringToFront(Configuration.Browser(output.url))
        }
    }

    private fun handleOtpOutput(output: OtpBloc.Output) {
        when (output) {
            // Verifying the code is the last step of sign up / passwordless sign in, so it lands
            // the
            // user in the same place a password sign-in does — not on top of the auth stack.
            OtpBloc.Output.Verified -> finishAuthentication()
            OtpBloc.Output.Cancelled -> {
                destinationAfterAuth = null
                navigation.pop()
            }
        }
    }

    /**
     * Leaves the authentication flow after the user is signed in, whether that finished at the
     * credentials screen (sign in) or at the OTP screen (sign up / passwordless).
     *
     * Signing in from onboarding counts as finishing it: mark complete and drop the whole
     * onboarding
     * + auth stack so back can't return to the flow, and so the next launch doesn't gate the
     *   now-signed-in user behind onboarding again. Otherwise just unwind the auth screens,
     *   preserving whatever the user was doing underneath (e.g. a deep-linked recipe).
     */
    private fun finishAuthentication() {
        val destination = destinationAfterAuth
        destinationAfterAuth = null
        val cameFromOnboarding = stack.value.items.any { it.instance is RootBloc.Child.Onboarding }
        if (cameFromOnboarding) {
            onboardingRepository.setOnboardingCompleted()
            navigation.replaceAll(Configuration.BottomNavigation)
        } else {
            navigation.popWhile {
                it is Configuration.Authentication || it is Configuration.OtpVerification
            }
        }
        // A screen the user asked for before they were signed in (Manage Family today) opens now
        // that they are, on top of whatever the auth flow was covering.
        destination?.let { navigation.bringToFront(it) }
    }

    @Serializable
    private sealed class Configuration {
        /**
         * [reentry] is true when opened from within the app (e.g. Settings → replay onboarding).
         */
        @Serializable data class Onboarding(val reentry: Boolean = false) : Configuration()

        @Serializable data object BottomNavigation : Configuration()

        @Serializable data class RecipeRoot(val props: RecipeRootBloc.Props) : Configuration()

        @Serializable data class PublicRecipe(val remoteId: String) : Configuration()

        @Serializable
        data class Authentication(val props: AuthenticationBloc.Props) : Configuration()

        @Serializable
        data class OtpVerification(val email: String, val flow: OtpFlow) : Configuration()

        @Serializable data class Browser(val url: String) : Configuration()

        @Serializable data class MealPlanner(val props: MealPlannerRootBloc.Props) : Configuration()

        @Serializable
        data class SettingsRoot(
            val props: SettingsRootBloc.Props = SettingsRootBloc.Props.Default
        ) : Configuration()

        @Serializable data object ManageProfile : Configuration()

        @Serializable data object Notifications : Configuration()

        @Serializable data object ManageFamily : Configuration()

        @Serializable data object DeveloperSettings : Configuration()

        @Serializable data object FeatureFlags : Configuration()

        @Serializable data class CookMode(val recipeId: Long) : Configuration()

        @Serializable data class AiChat(val recipeContextId: Long? = null) : Configuration()

        @Serializable data class ExportRecipes(val recipeIds: Set<Long>?) : Configuration()

        @Serializable data class EditRecipeBook(val bookId: Long?) : Configuration()

        @Serializable data class EditGroceryList(val listId: Long) : Configuration()
    }
}
