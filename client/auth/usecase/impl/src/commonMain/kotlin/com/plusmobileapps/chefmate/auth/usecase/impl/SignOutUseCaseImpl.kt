package com.plusmobileapps.chefmate.auth.usecase.impl

import com.plusmobileapps.chefmate.aichat.AiChatLocalDataCleaner
import com.plusmobileapps.chefmate.auth.data.AuthenticationRepository
import com.plusmobileapps.chefmate.auth.usecase.SignOutUseCase
import com.plusmobileapps.chefmate.di.AppScope
import com.plusmobileapps.chefmate.grocery.data.GroceryAutocompleteRepository
import com.plusmobileapps.chefmate.grocery.data.GroceryRepository
import com.plusmobileapps.chefmate.meal.data.MealPlanRepository
import com.plusmobileapps.chefmate.recipe.data.CategoryRepository
import com.plusmobileapps.chefmate.recipe.data.RecipeRepository
import com.plusmobileapps.chefmate.recipebook.data.RecipeBookRepository
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn

@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class SignOutUseCaseImpl(
    private val authenticationRepository: AuthenticationRepository,
    private val groceryRepository: GroceryRepository,
    private val mealPlanRepository: MealPlanRepository,
    private val recipeRepository: RecipeRepository,
    private val recipeBookRepository: RecipeBookRepository,
    private val categoryRepository: CategoryRepository,
    private val groceryAutocompleteRepository: GroceryAutocompleteRepository,
    private val aiChatLocalDataCleaner: AiChatLocalDataCleaner,
) : SignOutUseCase {
    override suspend fun invoke() {
        authenticationRepository.signOut()
        mealPlanRepository.clearLocalData()
        recipeRepository.clearLocalData()
        recipeBookRepository.clearLocalData()
        categoryRepository.clearLocalData()
        groceryAutocompleteRepository.clearLocalData()
        // Grocery category rules are deliberately NOT cleared here. Every other repository above
        // is server-backed, so wiping it locally is recoverable on the next sign-in. Category
        // rules are device-local (no Supabase table yet), so clearing them would destroy the
        // user's rules permanently. They're treated as a device preference that outlives the
        // session; DeleteAccountUseCase still wipes them, since that's explicit erasure.
        groceryRepository.clearLocalData()
        groceryRepository.ensureDefaultList()
        aiChatLocalDataCleaner.clearLocalData()
    }
}
