@file:Suppress("FunctionName")

package com.plusmobileapps.chefmate.root

import com.plusmobileapps.chefmate.BlocContext
import com.plusmobileapps.chefmate.Consumer
import com.plusmobileapps.chefmate.aichat.AiChatRootBloc
import com.plusmobileapps.chefmate.auth.data.OtpFlow
import com.plusmobileapps.chefmate.auth.data.testing.FakeAuthenticationRepository
import com.plusmobileapps.chefmate.auth.ui.AuthenticationBloc
import com.plusmobileapps.chefmate.auth.ui.otp.OtpBloc
import com.plusmobileapps.chefmate.browser.BrowserRootBloc
import com.plusmobileapps.chefmate.cook.CookModeBloc
import com.plusmobileapps.chefmate.di.OnboardingRepository
import com.plusmobileapps.chefmate.family.manage.ManageFamilyRootBloc
import com.plusmobileapps.chefmate.featureflag.testing.FakeFeatureFlags
import com.plusmobileapps.chefmate.notifications.NotificationsBloc
import com.plusmobileapps.chefmate.onboarding.OnboardingRootBloc
import com.plusmobileapps.chefmate.recipe.bottomnav.BottomNavBloc
import com.plusmobileapps.chefmate.recipe.core.addmeal.MealPlannerRootBloc
import com.plusmobileapps.chefmate.recipe.core.root.RecipeRootBloc
import com.plusmobileapps.chefmate.recipe.core.share.PublicRecipeBloc
import com.plusmobileapps.chefmate.recipe.data.ExtractedRecipeData
import com.plusmobileapps.chefmate.recipe.exporter.ExportRecipesBloc
import com.plusmobileapps.chefmate.settings.root.SettingsRootBloc
import com.plusmobileapps.chefmate.testing.TestBlocContext
import com.russhwolf.settings.MapSettings
import dev.mokkery.MockMode
import dev.mokkery.mock
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.instanceOf
import kotlin.test.Test

class RootBlocTest {
    val context = TestBlocContext.create()
    var bottomNavOutput: Consumer<BottomNavBloc.Output> = Consumer {}
    var recipeOutput: Consumer<RecipeRootBloc.Output> = Consumer {}
    var recipeProps: RecipeRootBloc.Props? = null
    var publicRecipeOutput: Consumer<PublicRecipeBloc.Output> = Consumer {}
    var publicRecipeRemoteId: String? = null
    var settingsRootOutput: Consumer<SettingsRootBloc.Output> = Consumer {}
    var settingsRootProps: SettingsRootBloc.Props? = null
    var manageProfileOutput:
        Consumer<com.plusmobileapps.chefmate.profile.ManageProfileBloc.Output> =
        Consumer {}
    var notificationsOutput:
        Consumer<com.plusmobileapps.chefmate.notifications.NotificationsBloc.Output> =
        Consumer {}
    var manageFamilyOutput: Consumer<ManageFamilyRootBloc.Output> = Consumer {}
    var developerSettingsOutput:
        Consumer<com.plusmobileapps.chefmate.devsettings.DeveloperSettingsBloc.Output> =
        Consumer {}
    var authOutput: Consumer<AuthenticationBloc.Output> = Consumer {}
    var authProps: AuthenticationBloc.Props? = null
    var otpOutput: Consumer<OtpBloc.Output> = Consumer {}
    var otpProps: OtpBloc.Props? = null
    var aiChatOutput: Consumer<AiChatRootBloc.Output> = Consumer {}
    var cookModeOutput: Consumer<CookModeBloc.Output> = Consumer {}
    var exportRecipesOutput: Consumer<ExportRecipesBloc.Output> = Consumer {}
    var exportRecipesProps: ExportRecipesBloc.Props? = null
    var bottomNavInitialTab: BottomNavBloc.Tab? = null
    var onboardingOutput: Consumer<OnboardingRootBloc.Output> = Consumer {}
    var onboardingProps: OnboardingRootBloc.Props? = null
    val authRepository = FakeAuthenticationRepository()
    lateinit var onboardingRepository: OnboardingRepository

    fun createRoot(
        deepLink: DeepLink = DeepLink.None,
        context: BlocContext = TestBlocContext.create(),
        onboardingCompleted: Boolean = true,
    ): RootBlocImpl =
        RootBlocImpl(
            context = context,
            deepLink = deepLink,
            onboardingRepository =
                OnboardingRepository(MapSettings())
                    .apply { if (onboardingCompleted) setOnboardingCompleted() }
                    .also { onboardingRepository = it },
            authenticationRepository = authRepository,
            onboardingRoot = { _, props, output ->
                onboardingProps = props
                onboardingOutput = output
                mock()
            },
            bottomNav = { _, output, initialTab ->
                bottomNavOutput = output
                bottomNavInitialTab = initialTab
                mock(MockMode.autoUnit)
            },
            browserRootBlocFactory =
                object : BrowserRootBloc.Factory {
                    override fun create(
                        context: BlocContext,
                        output: Consumer<BrowserRootBloc.Output>,
                        initialUrl: String?,
                        showControls: Boolean,
                        presentation: BrowserRootBloc.Presentation,
                    ): BrowserRootBloc = mock(MockMode.autoUnit)
                },
            recipeRoot = { _, props, output ->
                recipeOutput = output
                recipeProps = props
                mock()
            },
            publicRecipe = { _, remoteId, output ->
                publicRecipeRemoteId = remoteId
                publicRecipeOutput = output
                mock()
            },
            mealPlannerRoot = MealPlannerRootBloc.Factory { _, _, _ -> mock() },
            authentication = { _, props, output ->
                authProps = props
                authOutput = output
                mock()
            },
            otpBloc = { _, props, output ->
                otpProps = props
                otpOutput = output
                mock()
            },
            settingsRoot = { _, props, output ->
                settingsRootProps = props
                settingsRootOutput = output
                mock()
            },
            manageProfile = { _, output ->
                manageProfileOutput = output
                mock()
            },
            notifications = { _, output ->
                notificationsOutput = output
                mock()
            },
            manageFamilyRoot = { _, output ->
                manageFamilyOutput = output
                mock()
            },
            developerSettings = { _, output ->
                developerSettingsOutput = output
                mock()
            },
            cookMode =
                CookModeBloc.Factory { _, _, output ->
                    cookModeOutput = output
                    mock()
                },
            featureFlags = FakeFeatureFlags(),
            featureFlagsBlocFactory = { _, _ -> mock() },
            aiChat = { _, _, output ->
                aiChatOutput = output
                mock()
            },
            exportRecipes = { _, props, output ->
                exportRecipesProps = props
                exportRecipesOutput = output
                mock()
            },
            editRecipeBook = { _, _, _ -> mock() },
            editGroceryList = { _, _, _ -> mock() },
        )

    val rootBloc = createRoot(context = context)

    fun RootBloc.instance(): RootBloc.Child = state.value.active.instance

    @Test
    fun When_initialized_Then_bottom_nav_is_shown() {
        rootBloc.instance() should instanceOf<RootBloc.Child.BottomNavigation>()
        rootBloc.state.value.backStack.size shouldBe 0
    }

    @Test
    fun When_onboarding_not_completed_Then_onboarding_is_shown() {
        val root = createRoot(onboardingCompleted = false)
        root.instance() should instanceOf<RootBloc.Child.Onboarding>()
        root.state.value.backStack.size shouldBe 0
    }

    @Test
    fun When_onboarding_finishes_Then_bottom_nav_replaces_the_stack() {
        val root = createRoot(onboardingCompleted = false)
        onboardingOutput.onNext(OnboardingRootBloc.Output.Finished)
        root.instance() should instanceOf<RootBloc.Child.BottomNavigation>()
        // replaceAll wipes the onboarding flow so back can't return to it.
        root.state.value.backStack.size shouldBe 0
    }

    @Test
    fun When_bottom_nav_outputs_open_onboarding_Then_onboarding_is_pushed() {
        bottomNavOutput.onNext(BottomNavBloc.Output.OpenOnboarding)
        rootBloc.instance() should instanceOf<RootBloc.Child.Onboarding>()
        rootBloc.state.value.backStack.size shouldBe 1
    }

    @Test
    fun When_signed_in_user_reopens_onboarding_Then_it_is_dismissible_and_marked_signed_in() {
        authRepository.setAuthenticated()

        bottomNavOutput.onNext(BottomNavBloc.Output.OpenOnboarding)

        rootBloc.instance() should instanceOf<RootBloc.Child.Onboarding>()
        onboardingProps shouldBe OnboardingRootBloc.Props(isDismissible = true, isSignedIn = true)
    }

    @Test
    fun When_anonymous_user_reopens_onboarding_Then_it_is_not_marked_signed_in() {
        authRepository.setAnonymous()

        bottomNavOutput.onNext(BottomNavBloc.Output.OpenOnboarding)

        // Anonymous users can still sign in/up to upgrade, so they aren't treated as signed in.
        onboardingProps shouldBe OnboardingRootBloc.Props(isDismissible = true, isSignedIn = false)
    }

    @Test
    fun When_first_run_onboarding_Then_it_is_not_dismissible_and_not_signed_in() {
        createRoot(onboardingCompleted = false)

        onboardingProps shouldBe OnboardingRootBloc.Props(isDismissible = false, isSignedIn = false)
    }

    @Test
    fun When_reopened_onboarding_is_dismissed_Then_it_is_popped() {
        bottomNavOutput.onNext(BottomNavBloc.Output.OpenOnboarding)
        rootBloc.instance() should instanceOf<RootBloc.Child.Onboarding>()

        onboardingOutput.onNext(OnboardingRootBloc.Output.Dismissed)

        rootBloc.instance() should instanceOf<RootBloc.Child.BottomNavigation>()
        rootBloc.state.value.backStack.size shouldBe 0
    }

    @Test
    fun When_onboarding_outputs_sign_in_Then_authentication_is_pushed() {
        val root = createRoot(onboardingCompleted = false)
        onboardingOutput.onNext(OnboardingRootBloc.Output.SignIn)
        root.instance() should instanceOf<RootBloc.Child.Authentication>()
        authProps shouldBe AuthenticationBloc.Props.SignIn
        root.state.value.backStack.size shouldBe 1
    }

    @Test
    fun When_onboarding_outputs_sign_up_Then_authentication_is_pushed() {
        val root = createRoot(onboardingCompleted = false)
        onboardingOutput.onNext(OnboardingRootBloc.Output.SignUp)
        root.instance() should instanceOf<RootBloc.Child.Authentication>()
        authProps shouldBe AuthenticationBloc.Props.SignUp
        root.state.value.backStack.size shouldBe 1
    }

    @Test
    fun When_auth_succeeds_from_onboarding_Then_bottom_nav_replaces_stack_and_completes() {
        val root = createRoot(onboardingCompleted = false)
        onboardingOutput.onNext(OnboardingRootBloc.Output.SignIn)

        authOutput.onNext(AuthenticationBloc.Output.AuthenticationSuccess)

        root.instance() should instanceOf<RootBloc.Child.BottomNavigation>()
        root.state.value.backStack.size shouldBe 0
        onboardingRepository.hasCompletedOnboarding shouldBe true
    }

    @Test
    fun When_sign_up_otp_verified_from_onboarding_Then_bottom_nav_replaces_stack_and_completes() {
        val root = createRoot(onboardingCompleted = false)
        onboardingOutput.onNext(OnboardingRootBloc.Output.SignUp)
        authOutput.onNext(AuthenticationBloc.Output.EmailVerificationRequired(EMAIL))
        root.instance() should instanceOf<RootBloc.Child.OtpVerification>()

        otpOutput.onNext(OtpBloc.Output.Verified)

        root.instance() should instanceOf<RootBloc.Child.BottomNavigation>()
        root.state.value.backStack.size shouldBe 0
        onboardingRepository.hasCompletedOnboarding shouldBe true
    }

    @Test
    fun When_sign_up_otp_verified_in_app_Then_auth_and_otp_screens_are_popped() {
        bottomNavOutput.onNext(BottomNavBloc.Output.OpenSignUp)
        authOutput.onNext(AuthenticationBloc.Output.EmailVerificationRequired(EMAIL))
        rootBloc.instance() should instanceOf<RootBloc.Child.OtpVerification>()

        otpOutput.onNext(OtpBloc.Output.Verified)

        rootBloc.instance() should instanceOf<RootBloc.Child.BottomNavigation>()
        rootBloc.state.value.backStack.size shouldBe 0
    }

    @Test
    fun When_bottom_nav_outputs_create_recipe_Then_recipe_root_shown_with_create_props() {
        bottomNavOutput.onNext(BottomNavBloc.Output.AddNewRecipe)
        rootBloc.instance() should instanceOf<RootBloc.Child.RecipeRoot>()
        rootBloc.state.value.backStack.size shouldBe 1
        recipeProps shouldBe RecipeRootBloc.Props.Create

        recipeOutput.onNext(RecipeRootBloc.Output.Finished)
        rootBloc.instance() should instanceOf<RootBloc.Child.BottomNavigation>()
    }

    @Test
    fun When_bottom_nav_outputs_open_recipe_Then_recipe_root_shown_with_detail_props() {
        bottomNavOutput.onNext(BottomNavBloc.Output.OpenRecipe(123L))
        rootBloc.instance() should instanceOf<RootBloc.Child.RecipeRoot>()
        rootBloc.state.value.backStack.size shouldBe 1
        recipeProps shouldBe RecipeRootBloc.Props.Detail(123L)

        recipeOutput.onNext(RecipeRootBloc.Output.Finished)
        rootBloc.instance() should instanceOf<RootBloc.Child.BottomNavigation>()
    }

    @Test
    fun When_bottom_nav_outputs_open_url_Then_browser_is_shown() {
        bottomNavOutput.onNext(BottomNavBloc.Output.OpenUrl("https://example.com"))
        rootBloc.instance() should instanceOf<RootBloc.Child.Browser>()
        rootBloc.state.value.backStack.size shouldBe 1
    }

    @Test
    fun When_recipe_root_outputs_open_url_Then_browser_is_shown() {
        bottomNavOutput.onNext(BottomNavBloc.Output.OpenRecipe(123L))
        rootBloc.instance() should instanceOf<RootBloc.Child.RecipeRoot>()

        recipeOutput.onNext(RecipeRootBloc.Output.OpenUrl("https://example.com/recipe"))
        rootBloc.instance() should instanceOf<RootBloc.Child.Browser>()
        rootBloc.state.value.backStack.size shouldBe 2
    }

    @Test
    fun When_bottom_nav_outputs_open_app_settings_Then_settings_root_is_shown() {
        bottomNavOutput.onNext(BottomNavBloc.Output.OpenAppSettings)
        rootBloc.instance() should instanceOf<RootBloc.Child.SettingsRoot>()
        rootBloc.state.value.backStack.size shouldBe 1
        settingsRootProps shouldBe SettingsRootBloc.Props.Default
    }

    @Test
    fun When_bottom_nav_outputs_open_notifications_Then_notifications_is_shown() {
        bottomNavOutput.onNext(BottomNavBloc.Output.OpenNotifications)
        rootBloc.instance() should instanceOf<RootBloc.Child.Notifications>()
        rootBloc.state.value.backStack.size shouldBe 1
    }

    @Test
    fun Given_notifications_When_back_outputted_Then_bottom_nav_is_shown() {
        bottomNavOutput.onNext(BottomNavBloc.Output.OpenNotifications)
        rootBloc.instance() should instanceOf<RootBloc.Child.Notifications>()
        notificationsOutput.onNext(NotificationsBloc.Output.Back)
        rootBloc.instance() should instanceOf<RootBloc.Child.BottomNavigation>()
    }

    @Test
    fun Given_signed_in_When_bottom_nav_opens_manage_family_Then_it_is_shown_directly() {
        authRepository.setAuthenticated()

        bottomNavOutput.onNext(BottomNavBloc.Output.OpenManageFamily)

        rootBloc.instance() should instanceOf<RootBloc.Child.ManageFamily>()
    }

    @Test
    fun Given_signed_out_When_bottom_nav_opens_manage_family_Then_sign_in_is_shown_first() {
        bottomNavOutput.onNext(BottomNavBloc.Output.OpenManageFamily)

        rootBloc.instance() should instanceOf<RootBloc.Child.Authentication>()
        authProps shouldBe AuthenticationBloc.Props.SignIn
    }

    @Test
    fun Given_sign_in_opened_for_manage_family_When_auth_succeeds_Then_manage_family_is_shown() {
        bottomNavOutput.onNext(BottomNavBloc.Output.OpenManageFamily)

        authOutput.onNext(AuthenticationBloc.Output.AuthenticationSuccess)

        rootBloc.instance() should instanceOf<RootBloc.Child.ManageFamily>()
        // The auth screen it came from is gone, so back returns to the More tab.
        rootBloc.state.value.backStack.size shouldBe 1
    }

    @Test
    fun Given_sign_in_opened_for_manage_family_When_otp_verified_Then_manage_family_is_shown() {
        bottomNavOutput.onNext(BottomNavBloc.Output.OpenManageFamily)
        authOutput.onNext(AuthenticationBloc.Output.EmailVerificationRequired("a@b.com"))

        otpOutput.onNext(OtpBloc.Output.Verified)

        rootBloc.instance() should instanceOf<RootBloc.Child.ManageFamily>()
    }

    @Test
    fun Given_sign_in_opened_for_manage_family_When_it_is_abandoned_Then_a_later_sign_in_stays_put() {
        bottomNavOutput.onNext(BottomNavBloc.Output.OpenManageFamily)
        authOutput.onNext(AuthenticationBloc.Output.Finished)

        // An unrelated sign-in later must not strand the user on Manage Family.
        bottomNavOutput.onNext(BottomNavBloc.Output.OpenSignIn)
        authOutput.onNext(AuthenticationBloc.Output.AuthenticationSuccess)

        rootBloc.instance() should instanceOf<RootBloc.Child.BottomNavigation>()
    }

    @Test
    fun Given_anonymous_user_When_bottom_nav_opens_manage_family_Then_sign_in_is_shown_first() {
        // A guest has no account email, and family invites are addressed to one.
        authRepository.setAnonymous()

        bottomNavOutput.onNext(BottomNavBloc.Output.OpenManageFamily)

        rootBloc.instance() should instanceOf<RootBloc.Child.Authentication>()
    }

    @Test
    fun Given_manage_family_When_back_outputted_Then_bottom_nav_is_shown() {
        authRepository.setAuthenticated()
        bottomNavOutput.onNext(BottomNavBloc.Output.OpenManageFamily)

        manageFamilyOutput.onNext(ManageFamilyRootBloc.Output.Back)

        rootBloc.instance() should instanceOf<RootBloc.Child.BottomNavigation>()
    }

    @Test
    fun Given_notifications_When_open_sign_in_outputted_Then_authentication_is_shown() {
        bottomNavOutput.onNext(BottomNavBloc.Output.OpenNotifications)
        notificationsOutput.onNext(NotificationsBloc.Output.OpenSignIn)
        rootBloc.instance() should instanceOf<RootBloc.Child.Authentication>()
        authProps shouldBe AuthenticationBloc.Props.SignIn
    }

    @Test
    fun Given_notifications_When_open_sign_up_outputted_Then_authentication_is_shown() {
        bottomNavOutput.onNext(BottomNavBloc.Output.OpenNotifications)
        notificationsOutput.onNext(NotificationsBloc.Output.OpenSignUp)
        rootBloc.instance() should instanceOf<RootBloc.Child.Authentication>()
        authProps shouldBe AuthenticationBloc.Props.SignUp
    }

    @Test
    fun When_bottom_nav_opens_grocery_autocomplete_settings_Then_settings_root_deep_links() {
        bottomNavOutput.onNext(BottomNavBloc.Output.OpenGroceryAutocompleteSettings)
        rootBloc.instance() should instanceOf<RootBloc.Child.SettingsRoot>()
        rootBloc.state.value.backStack.size shouldBe 1
        settingsRootProps shouldBe SettingsRootBloc.Props.GroceryAutocomplete
    }

    @Test
    fun Given_settings_root_When_back_outputted_Then_bottom_nav_is_shown() {
        bottomNavOutput.onNext(BottomNavBloc.Output.OpenAppSettings)
        rootBloc.instance() should instanceOf<RootBloc.Child.SettingsRoot>()
        settingsRootOutput.onNext(SettingsRootBloc.Output.Back)
        rootBloc.instance() should instanceOf<RootBloc.Child.BottomNavigation>()
    }

    @Test
    fun When_dev_settings_outputs_open_feature_flags_Then_feature_flags_shown() {
        bottomNavOutput.onNext(BottomNavBloc.Output.OpenDeveloperSettings)
        rootBloc.instance() should instanceOf<RootBloc.Child.DeveloperSettings>()
        developerSettingsOutput.onNext(
            com.plusmobileapps.chefmate.devsettings.DeveloperSettingsBloc.Output.OpenFeatureFlags
        )
        rootBloc.instance() should instanceOf<RootBloc.Child.FeatureFlags>()
    }

    @Test
    fun When_auth_outputs_email_verification_Then_otp_screen_shown_with_signup_flow() {
        bottomNavOutput.onNext(BottomNavBloc.Output.OpenSignUp)
        rootBloc.instance() should instanceOf<RootBloc.Child.Authentication>()

        authOutput.onNext(AuthenticationBloc.Output.EmailVerificationRequired("user@example.com"))

        rootBloc.instance() should instanceOf<RootBloc.Child.OtpVerification>()
        otpProps shouldBe OtpBloc.Props(email = "user@example.com", flow = OtpFlow.SignUp)
    }

    @Test
    fun When_auth_outputs_passwordless_otp_Then_otp_screen_shown_with_passwordless_flow() {
        bottomNavOutput.onNext(BottomNavBloc.Output.OpenSignIn)
        rootBloc.instance() should instanceOf<RootBloc.Child.Authentication>()

        authOutput.onNext(AuthenticationBloc.Output.PasswordlessOtpSent("user@example.com"))

        rootBloc.instance() should instanceOf<RootBloc.Child.OtpVerification>()
        otpProps shouldBe
            OtpBloc.Props(email = "user@example.com", flow = OtpFlow.PasswordlessSignIn)
    }

    @Test
    fun Given_otp_screen_When_otp_verified_Then_bottom_nav_is_shown() {
        bottomNavOutput.onNext(BottomNavBloc.Output.OpenSignUp)
        authOutput.onNext(AuthenticationBloc.Output.EmailVerificationRequired("user@example.com"))
        rootBloc.instance() should instanceOf<RootBloc.Child.OtpVerification>()

        otpOutput.onNext(OtpBloc.Output.Verified)

        rootBloc.instance() should instanceOf<RootBloc.Child.BottomNavigation>()
    }

    @Test
    fun Given_otp_screen_When_otp_cancelled_Then_returns_to_previous() {
        bottomNavOutput.onNext(BottomNavBloc.Output.OpenSignUp)
        authOutput.onNext(AuthenticationBloc.Output.EmailVerificationRequired("user@example.com"))
        rootBloc.instance() should instanceOf<RootBloc.Child.OtpVerification>()

        otpOutput.onNext(OtpBloc.Output.Cancelled)

        rootBloc.instance() should instanceOf<RootBloc.Child.Authentication>()
    }

    @Test
    fun When_bottom_nav_outputs_open_extracted_recipe_Then_recipe_root_shown_with_create_from_extracted_props() {
        val extracted =
            ExtractedRecipeData(
                title = "Extracted",
                description = null,
                ingredients = listOf("flour"),
                directions = listOf("mix"),
                imageUrl = null,
                sourceUrl = "https://example.com/recipe",
                servings = 4,
                prepTime = 10,
                cookTime = 20,
                totalTime = 30,
                calories = 200,
            )

        bottomNavOutput.onNext(BottomNavBloc.Output.OpenExtractedRecipe(extracted))

        rootBloc.instance() should instanceOf<RootBloc.Child.RecipeRoot>()
        rootBloc.state.value.backStack.size shouldBe 1
        recipeProps shouldBe RecipeRootBloc.Props.CreateFromExtracted(extracted)

        recipeOutput.onNext(RecipeRootBloc.Output.Finished)
        rootBloc.instance() should instanceOf<RootBloc.Child.BottomNavigation>()
    }

    @Test
    fun When_aichat_outputs_AddAsRecipe_Then_recipe_root_shown_with_extracted_props() {
        val extracted =
            ExtractedRecipeData(
                title = "Chat-extracted",
                description = "From Gemini",
                ingredients = listOf("eggs", "flour"),
                directions = listOf("whisk", "cook"),
                imageUrl = null,
                sourceUrl = "",
                servings = 2,
                prepTime = 5,
                cookTime = 10,
                totalTime = 15,
                calories = null,
            )

        bottomNavOutput.onNext(BottomNavBloc.Output.OpenAiChat)
        rootBloc.instance() should instanceOf<RootBloc.Child.AiChat>()

        aiChatOutput.onNext(AiChatRootBloc.Output.AddAsRecipe(extracted))

        rootBloc.instance() should instanceOf<RootBloc.Child.RecipeRoot>()
        recipeProps shouldBe RecipeRootBloc.Props.CreateFromExtracted(extracted, fromAi = true)
    }

    @Test
    fun Given_recipe_detail_deeplink_When_initialized_Then_recipe_root_is_on_top() {
        val root = createRoot(DeepLink.RecipeDetail(recipeId = 42L))
        root.instance() should instanceOf<RootBloc.Child.RecipeRoot>()
        root.state.value.backStack.size shouldBe 1
        recipeProps shouldBe RecipeRootBloc.Props.Detail(recipeId = 42L)
    }

    @Test
    fun Given_public_recipe_deeplink_When_initialized_Then_public_recipe_is_on_top() {
        val uuid = "3f2504e0-4f89-41d3-9a0c-0305e82c3301"
        val root = createRoot(DeepLink.PublicRecipe(remoteId = uuid))
        root.instance() should instanceOf<RootBloc.Child.PublicRecipe>()
        root.state.value.backStack.size shouldBe 1
        publicRecipeRemoteId shouldBe uuid
    }

    @Test
    fun Given_running_app_When_public_recipe_deeplink_handled_Then_public_recipe_is_shown() {
        val uuid = "3f2504e0-4f89-41d3-9a0c-0305e82c3301"
        rootBloc.handleDeepLink("https://chefmate.plusmobileapps.com/recipe/$uuid")
        rootBloc.instance() should instanceOf<RootBloc.Child.PublicRecipe>()
        publicRecipeRemoteId shouldBe uuid
    }

    @Test
    fun Given_public_recipe_preview_When_it_resolves_to_a_recipe_Then_it_is_swapped_for_detail() {
        val uuid = "3f2504e0-4f89-41d3-9a0c-0305e82c3301"
        rootBloc.handleDeepLink("https://chefmate.plusmobileapps.com/recipe/$uuid")

        publicRecipeOutput.onNext(PublicRecipeBloc.Output.OpenRecipe(recipeId = 7L))

        rootBloc.instance() should instanceOf<RootBloc.Child.RecipeRoot>()
        recipeProps shouldBe RecipeRootBloc.Props.Detail(recipeId = 7L)
        // The read-only preview is gone — back returns to the bottom nav, not to it.
        rootBloc.state.value.backStack.size shouldBe 1
        rootBloc.state.value.backStack.first().instance should
            instanceOf<RootBloc.Child.BottomNavigation>()
    }

    @Test
    fun Given_recipe_already_open_When_its_share_link_is_opened_Then_detail_is_not_duplicated() {
        // Opening a share link for a recipe already on the stack used to leave two identical
        // RecipeRoot configurations behind, which Decompose rejects ("Configurations must be
        // unique") — a crash on every tap of a deep link to the recipe already being viewed.
        val uuid = "3f2504e0-4f89-41d3-9a0c-0305e82c3301"
        val root = createRoot(DeepLink.RecipeDetail(recipeId = 18L))
        root.handleDeepLink("https://chefmate.plusmobileapps.com/recipe/$uuid")

        publicRecipeOutput.onNext(PublicRecipeBloc.Output.OpenRecipe(recipeId = 18L))

        root.instance() should instanceOf<RootBloc.Child.RecipeRoot>()
        recipeProps shouldBe RecipeRootBloc.Props.Detail(recipeId = 18L)
        root.state.value.backStack.size shouldBe 1
        root.state.value.backStack.first().instance should
            instanceOf<RootBloc.Child.BottomNavigation>()
    }

    @Test
    fun Given_groceries_deeplink_When_initialized_Then_bottom_nav_uses_groceries_tab() {
        val root = createRoot(DeepLink.Groceries)
        root.instance() should instanceOf<RootBloc.Child.BottomNavigation>()
        root.state.value.backStack.size shouldBe 0
        bottomNavInitialTab shouldBe BottomNavBloc.Tab.GROCERIES
    }

    @Test
    fun Given_meal_planner_deeplink_When_initialized_Then_bottom_nav_uses_meals_tab() {
        val root = createRoot(DeepLink.MealPlanner)
        root.instance() should instanceOf<RootBloc.Child.BottomNavigation>()
        root.state.value.backStack.size shouldBe 0
        bottomNavInitialTab shouldBe BottomNavBloc.Tab.MEALS
    }

    @Test
    fun Given_app_settings_deeplink_When_initialized_Then_settings_root_is_on_top() {
        val root = createRoot(DeepLink.AppSettings)
        root.instance() should instanceOf<RootBloc.Child.SettingsRoot>()
        root.state.value.backStack.size shouldBe 1
    }

    @Test
    fun Given_notifications_deeplink_When_initialized_Then_notifications_is_on_top() {
        val root = createRoot(DeepLink.Notifications)
        root.instance() should instanceOf<RootBloc.Child.Notifications>()
        root.state.value.backStack.size shouldBe 1
    }

    @Test
    fun Given_running_app_When_notifications_deeplink_handled_Then_notifications_is_shown() {
        rootBloc.handleDeepLink("https://chefmate.plusmobileapps.com/notifications")
        rootBloc.instance() should instanceOf<RootBloc.Child.Notifications>()
    }

    @Test
    fun Given_sign_in_deeplink_When_initialized_Then_authentication_shown_with_sign_in_props() {
        val root = createRoot(DeepLink.SignIn)
        root.instance() should instanceOf<RootBloc.Child.Authentication>()
        root.state.value.backStack.size shouldBe 1
        authProps shouldBe AuthenticationBloc.Props.SignIn
    }

    @Test
    fun Given_sign_up_deeplink_When_initialized_Then_authentication_shown_with_sign_up_props() {
        val root = createRoot(DeepLink.SignUp)
        root.instance() should instanceOf<RootBloc.Child.Authentication>()
        root.state.value.backStack.size shouldBe 1
        authProps shouldBe AuthenticationBloc.Props.SignUp
    }

    @Test
    fun When_bottom_nav_outputs_export_all_Then_export_recipes_is_shown_with_All_props() {
        bottomNavOutput.onNext(BottomNavBloc.Output.OpenExportRecipes(recipeIds = null))
        rootBloc.instance() should instanceOf<RootBloc.Child.ExportRecipes>()
        exportRecipesProps shouldBe ExportRecipesBloc.Props.All
    }

    @Test
    fun When_bottom_nav_outputs_export_selection_Then_export_recipes_uses_Selected_props() {
        bottomNavOutput.onNext(BottomNavBloc.Output.OpenExportRecipes(recipeIds = setOf(1L, 2L)))
        rootBloc.instance() should instanceOf<RootBloc.Child.ExportRecipes>()
        exportRecipesProps shouldBe ExportRecipesBloc.Props.Selected(setOf(1L, 2L))
    }

    @Test
    fun Given_export_recipes_When_back_Then_bottom_nav_is_shown() {
        bottomNavOutput.onNext(BottomNavBloc.Output.OpenExportRecipes(recipeIds = null))
        exportRecipesOutput.onNext(ExportRecipesBloc.Output.Back)
        rootBloc.instance() should instanceOf<RootBloc.Child.BottomNavigation>()
    }

    @Test
    fun Given_export_recipes_When_finished_Then_bottom_nav_is_shown_and_notified() {
        bottomNavOutput.onNext(BottomNavBloc.Output.OpenExportRecipes(recipeIds = setOf(1L)))
        val bottomNavChild =
            rootBloc.state.value.items
                .map { it.instance }
                .filterIsInstance<RootBloc.Child.BottomNavigation>()
                .first()
                .bloc

        exportRecipesOutput.onNext(ExportRecipesBloc.Output.Finished)

        rootBloc.instance() should instanceOf<RootBloc.Child.BottomNavigation>()
        dev.mokkery.verify { bottomNavChild.onExportFinished() }
    }

    @Test
    fun When_recipe_root_opens_ai_chat_Then_ai_chat_is_pushed_full_screen() {
        bottomNavOutput.onNext(BottomNavBloc.Output.OpenRecipe(123L))
        rootBloc.instance() should instanceOf<RootBloc.Child.RecipeRoot>()

        recipeOutput.onNext(RecipeRootBloc.Output.OpenAiChat(recipeId = 123L))

        // The chat is pushed full-screen over the recipe.
        rootBloc.instance() should instanceOf<RootBloc.Child.AiChat>()
        rootBloc.state.value.backStack.size shouldBe 2
    }

    @Test
    fun When_cook_mode_opens_ai_chat_Then_ai_chat_is_pushed_full_screen() {
        bottomNavOutput.onNext(BottomNavBloc.Output.OpenCookMode(7L))
        rootBloc.instance() should instanceOf<RootBloc.Child.CookMode>()

        cookModeOutput.onNext(CookModeBloc.Output.OpenAiChat(recipeId = 7L))

        rootBloc.instance() should instanceOf<RootBloc.Child.AiChat>()
    }

    @Test
    fun Given_ai_chat_open_When_back_clicked_Then_it_pops_back_to_the_recipe() {
        bottomNavOutput.onNext(BottomNavBloc.Output.OpenRecipe(123L))
        recipeOutput.onNext(RecipeRootBloc.Output.OpenAiChat(recipeId = 123L))
        rootBloc.instance() should instanceOf<RootBloc.Child.AiChat>()

        rootBloc.onBackClicked()

        rootBloc.instance() should instanceOf<RootBloc.Child.RecipeRoot>()
        rootBloc.state.value.backStack.size shouldBe 1
    }

    @Test
    fun Given_ai_chat_open_When_output_Finished_Then_it_pops_back_to_the_recipe() {
        bottomNavOutput.onNext(BottomNavBloc.Output.OpenRecipe(123L))
        recipeOutput.onNext(RecipeRootBloc.Output.OpenAiChat(recipeId = 123L))

        aiChatOutput.onNext(AiChatRootBloc.Output.Finished)

        rootBloc.instance() should instanceOf<RootBloc.Child.RecipeRoot>()
    }

    @Test
    fun Given_recipe_ai_chat_When_AddAsRecipe_Then_recipe_root_shown() {
        val extracted =
            ExtractedRecipeData(
                title = "Chat-extracted",
                description = "From the grounded chat",
                ingredients = listOf("eggs", "flour"),
                directions = listOf("whisk", "cook"),
                imageUrl = null,
                sourceUrl = "",
                servings = 2,
                prepTime = 5,
                cookTime = 10,
                totalTime = 15,
                calories = null,
            )

        bottomNavOutput.onNext(BottomNavBloc.Output.OpenRecipe(123L))
        recipeOutput.onNext(RecipeRootBloc.Output.OpenAiChat(recipeId = 123L))

        aiChatOutput.onNext(AiChatRootBloc.Output.AddAsRecipe(extracted))

        rootBloc.instance() should instanceOf<RootBloc.Child.RecipeRoot>()
        recipeProps shouldBe RecipeRootBloc.Props.CreateFromExtracted(extracted, fromAi = true)
    }

    // A detached desktop recipe window has no stack of its own beyond the recipe, so anything that
    // leaves it is forwarded here.

    @Test
    fun Given_a_detached_recipe_window_When_it_outputs_OpenCookMode_Then_cook_mode_is_shown() {
        rootBloc.handleRecipeWindowOutput(RecipeRootBloc.Output.OpenCookMode(recipeId = 123L))

        rootBloc.instance() should instanceOf<RootBloc.Child.CookMode>()
    }

    @Test
    fun Given_a_detached_recipe_window_When_it_outputs_OpenGroceryList_Then_groceries_tab_shown() {
        val bottomNavChild =
            rootBloc.state.value.items
                .map { it.instance }
                .filterIsInstance<RootBloc.Child.BottomNavigation>()
                .first()
                .bloc

        rootBloc.handleRecipeWindowOutput(RecipeRootBloc.Output.OpenGroceryList)

        rootBloc.instance() should instanceOf<RootBloc.Child.BottomNavigation>()
        dev.mokkery.verify { bottomNavChild.onTabSelected(BottomNavBloc.Tab.GROCERIES) }
    }

    @Test
    fun Given_a_detached_recipe_window_When_it_outputs_Finished_Then_this_stack_is_untouched() {
        // The window closes itself; popping the main stack in sympathy would strand the user.
        bottomNavOutput.onNext(BottomNavBloc.Output.OpenRecipe(123L))
        val backStackBefore = rootBloc.state.value.backStack.size

        rootBloc.handleRecipeWindowOutput(RecipeRootBloc.Output.Finished)

        rootBloc.instance() should instanceOf<RootBloc.Child.RecipeRoot>()
        rootBloc.state.value.backStack.size shouldBe backStackBefore
    }
}

private const val EMAIL = "chef@example.com"
