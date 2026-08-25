package com.plusmobileapps.chefmate.auth.usecase.impl

import com.plusmobileapps.chefmate.aichat.AiChatLocalDataCleaner
import com.plusmobileapps.chefmate.auth.data.AuthState
import com.plusmobileapps.chefmate.auth.data.AuthenticationRepository
import com.plusmobileapps.chefmate.auth.usecase.SignInUseCase
import com.plusmobileapps.chefmate.di.AppScope
import com.plusmobileapps.chefmate.grocery.data.GroceryAutocompleteRepository
import com.plusmobileapps.chefmate.grocery.data.GroceryRepository
import com.plusmobileapps.chefmate.meal.data.MealPlanRepository
import com.plusmobileapps.chefmate.recipe.data.CategoryRepository
import com.plusmobileapps.chefmate.recipe.data.RecipeRepository
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.first

@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class SignInUseCaseImpl(
    private val authenticationRepository: AuthenticationRepository,
    private val groceryRepository: GroceryRepository,
    private val mealPlanRepository: MealPlanRepository,
    private val recipeRepository: RecipeRepository,
    private val categoryRepository: CategoryRepository,
    private val groceryAutocompleteRepository: GroceryAutocompleteRepository,
    private val aiChatLocalDataCleaner: AiChatLocalDataCleaner,
) : SignInUseCase {

    override suspend fun guestRecipesToDiscard(): Int {
        // A real signed-in user shouldn't have "guest" recipes — if they do, those came from
        // someone else's anon session on this device and are still local-only data the user
        // about to sign in didn't create. Skip the count in that case so we don't surprise
        // them with a discard dialog.
        val authState = authenticationRepository.state.value
        if (authState is AuthState.Authenticated && !authState.user.isAnonymous) return 0
        // Includes the "anonymous session exists" case AND the "no session yet, but the
        // user has been creating local recipes" case. With deferred anon bootstrap (since
        // photo upload became the only trigger), the latter is now reachable.
        return recipeRepository.getRecipes().first().size
    }

    override suspend fun invoke(email: String, password: String): Result<Unit> {
        // Wipe before swapping sessions so the post-sign-in sync starts from a clean local DB
        // rather than a mix of anon-owned rows (with stale remoteIds the new uid can't touch)
        // and the real account's pull. Mirrors the order in SignOutUseCaseImpl.
        mealPlanRepository.clearLocalData()
        recipeRepository.clearLocalData()
        categoryRepository.clearLocalData()
        groceryAutocompleteRepository.clearLocalData()
        // Grocery category rules are deliberately NOT cleared. The wipe above exists to avoid
        // reconciling anon-owned rows against the new account's pull — but rules never sync, so
        // there is no pull to reconcile and clearing would just destroy a guest's rules the
        // moment they sign up. See SignOutUseCaseImpl for the full rationale.
        groceryRepository.clearLocalData()
        groceryRepository.ensureDefaultList()
        aiChatLocalDataCleaner.clearLocalData()
        return authenticationRepository.signInWithEmailAndPassword(email, password)
    }
}
