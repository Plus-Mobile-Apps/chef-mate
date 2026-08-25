package com.plusmobileapps.chefmate.client.database.di

import com.plusmobileapps.chefmate.client.database.DriverFactory
import com.plusmobileapps.chefmate.database.AiChatConversationQueries
import com.plusmobileapps.chefmate.database.AiChatMessageQueries
import com.plusmobileapps.chefmate.database.BrowserHistoryQueries
import com.plusmobileapps.chefmate.database.CategoryQueries
import com.plusmobileapps.chefmate.database.CookingSessionQueries
import com.plusmobileapps.chefmate.database.Database
import com.plusmobileapps.chefmate.database.GroceryAutocompleteItemQueries
import com.plusmobileapps.chefmate.database.GroceryCategoryOverrideQueries
import com.plusmobileapps.chefmate.database.GroceryListMemberQueries
import com.plusmobileapps.chefmate.database.GroceryListQueries
import com.plusmobileapps.chefmate.database.GroceryQueries
import com.plusmobileapps.chefmate.database.MealPlanQueries
import com.plusmobileapps.chefmate.database.RecipeBookQueries
import com.plusmobileapps.chefmate.database.RecipeBookRecipeQueries
import com.plusmobileapps.chefmate.database.RecipeCategoryQueries
import com.plusmobileapps.chefmate.database.RecipeQueries
import com.plusmobileapps.chefmate.di.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn

@SingleIn(AppScope::class)
@ContributesTo(AppScope::class)
interface DatabaseComponent {
    @SingleIn(AppScope::class)
    @Provides
    fun database(driverFactory: DriverFactory): Database =
        Database.Companion.invoke(driverFactory.createDriver())

    @SingleIn(AppScope::class)
    @Provides
    fun providesGroceryQueries(database: Database): GroceryQueries = database.groceryQueries

    @SingleIn(AppScope::class)
    @Provides
    fun providesGroceryListQueries(database: Database): GroceryListQueries =
        database.groceryListQueries

    @SingleIn(AppScope::class)
    @Provides
    fun providesMealPlanQueries(database: Database): MealPlanQueries = database.mealPlanQueries

    @SingleIn(AppScope::class)
    @Provides
    fun providesRecipeQueries(database: Database): RecipeQueries = database.recipeQueries

    @SingleIn(AppScope::class)
    @Provides
    fun providesRecipeBookQueries(database: Database): RecipeBookQueries =
        database.recipeBookQueries

    @SingleIn(AppScope::class)
    @Provides
    fun providesRecipeBookRecipeQueries(database: Database): RecipeBookRecipeQueries =
        database.recipeBookRecipeQueries

    @SingleIn(AppScope::class)
    @Provides
    fun providesBrowserHistoryQueries(database: Database): BrowserHistoryQueries =
        database.browserHistoryQueries

    @SingleIn(AppScope::class)
    @Provides
    fun providesCookingSessionQueries(database: Database): CookingSessionQueries =
        database.cookingSessionQueries

    @SingleIn(AppScope::class)
    @Provides
    fun providesCategoryQueries(database: Database): CategoryQueries = database.categoryQueries

    @SingleIn(AppScope::class)
    @Provides
    fun providesRecipeCategoryQueries(database: Database): RecipeCategoryQueries =
        database.recipeCategoryQueries

    @SingleIn(AppScope::class)
    @Provides
    fun providesAiChatMessageQueries(database: Database): AiChatMessageQueries =
        database.aiChatMessageQueries

    @SingleIn(AppScope::class)
    @Provides
    fun providesAiChatConversationQueries(database: Database): AiChatConversationQueries =
        database.aiChatConversationQueries

    @SingleIn(AppScope::class)
    @Provides
    fun providesGroceryListMemberQueries(database: Database): GroceryListMemberQueries =
        database.groceryListMemberQueries

    @SingleIn(AppScope::class)
    @Provides
    fun providesGroceryAutocompleteItemQueries(database: Database): GroceryAutocompleteItemQueries =
        database.groceryAutocompleteItemQueries

    @SingleIn(AppScope::class)
    @Provides
    fun providesGroceryCategoryOverrideQueries(database: Database): GroceryCategoryOverrideQueries =
        database.groceryCategoryOverrideQueries
}
