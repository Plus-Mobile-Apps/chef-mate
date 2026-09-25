package com.plusmobileapps.chefmate.root

import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.backhandler.BackHandlerOwner
import com.plusmobileapps.chefmate.BackClickBloc
import com.plusmobileapps.chefmate.BlocContext
import com.plusmobileapps.chefmate.aichat.AiChatRootBloc
import com.plusmobileapps.chefmate.auth.ui.AuthenticationBloc
import com.plusmobileapps.chefmate.auth.ui.otp.OtpBloc
import com.plusmobileapps.chefmate.browser.BrowserRootBloc
import com.plusmobileapps.chefmate.cook.CookModeBloc
import com.plusmobileapps.chefmate.devsettings.DeveloperSettingsBloc
import com.plusmobileapps.chefmate.family.manage.ManageFamilyRootBloc
import com.plusmobileapps.chefmate.featureflag.FeatureFlagsBloc
import com.plusmobileapps.chefmate.grocery.core.edit.EditGroceryListBloc
import com.plusmobileapps.chefmate.notifications.NotificationsBloc
import com.plusmobileapps.chefmate.onboarding.OnboardingRootBloc
import com.plusmobileapps.chefmate.profile.ManageProfileBloc
import com.plusmobileapps.chefmate.recipe.bottomnav.BottomNavBloc
import com.plusmobileapps.chefmate.recipe.core.addmeal.MealPlannerRootBloc
import com.plusmobileapps.chefmate.recipe.core.root.RecipeRootBloc
import com.plusmobileapps.chefmate.recipe.core.share.PublicRecipeBloc
import com.plusmobileapps.chefmate.recipe.exporter.ExportRecipesBloc
import com.plusmobileapps.chefmate.recipebook.edit.EditRecipeBookBloc
import com.plusmobileapps.chefmate.settings.root.SettingsRootBloc
import com.plusmobileapps.chefmate.ui.ComposeScreen

interface RootBloc : BackHandlerOwner, BackClickBloc {
    val state: Value<ChildStack<*, Child>>

    fun handleSharedUrl(url: String)

    /**
     * Route an incoming deep link while the app is already running (Android `onNewIntent`, iOS
     * `continueUserActivity`/`onOpenURL`). Cold-launch links are handled by the initial stack; this
     * covers the warm-start case where that stack was already built.
     */
    fun handleDeepLink(url: String)

    /**
     * Route an output from a detached recipe window (desktop) into this — the main — window's
     * stack. Such a window renders only the recipe stack, so anything that leaves it (the grocery
     * list, cook mode, AI chat, the meal planner, a web link) has to land here instead.
     *
     * [RecipeRootBloc.Output.Finished] is ignored: the detached window closes itself on that, and
     * popping the main window's stack in sympathy would be wrong.
     */
    fun handleRecipeWindowOutput(output: RecipeRootBloc.Output)

    sealed class Child {

        abstract val bloc: ComposeScreen

        data class Onboarding(override val bloc: OnboardingRootBloc) : Child()

        data class BottomNavigation(override val bloc: BottomNavBloc) : Child()

        data class RecipeRoot(override val bloc: RecipeRootBloc) : Child()

        data class PublicRecipe(override val bloc: PublicRecipeBloc) : Child()

        data class Authentication(override val bloc: AuthenticationBloc) : Child()

        data class OtpVerification(override val bloc: OtpBloc) : Child()

        data class Browser(override val bloc: BrowserRootBloc) : Child()

        data class MealPlanner(override val bloc: MealPlannerRootBloc) : Child()

        data class SettingsRoot(override val bloc: SettingsRootBloc) : Child()

        data class ManageProfile(override val bloc: ManageProfileBloc) : Child()

        data class Notifications(override val bloc: NotificationsBloc) : Child()

        data class ManageFamily(override val bloc: ManageFamilyRootBloc) : Child()

        data class DeveloperSettings(override val bloc: DeveloperSettingsBloc) : Child()

        data class FeatureFlags(override val bloc: FeatureFlagsBloc) : Child()

        data class CookMode(override val bloc: CookModeBloc) : Child()

        /**
         * The AI chat, presented full-screen. Opened grounded in a recipe (from a recipe or Cook
         * Mode) or standalone from the More tab. Slides up as a modal (see `isModal` in
         * RootScreen).
         */
        data class AiChat(override val bloc: AiChatRootBloc) : Child()

        data class ExportRecipes(override val bloc: ExportRecipesBloc) : Child()

        data class EditRecipeBook(override val bloc: EditRecipeBookBloc) : Child()

        data class EditGroceryList(override val bloc: EditGroceryListBloc) : Child()
    }

    fun interface Factory {
        fun create(context: BlocContext, deepLink: DeepLink): RootBloc
    }
}
