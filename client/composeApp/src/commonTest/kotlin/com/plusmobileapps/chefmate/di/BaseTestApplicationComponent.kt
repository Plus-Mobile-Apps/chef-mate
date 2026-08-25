package com.plusmobileapps.chefmate.di

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
import com.plusmobileapps.chefmate.fakes.FakeDatabase
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn

abstract class BaseTestApplicationComponent : TestApplicationComponent {

    @Provides @SingleIn(AppScope::class) fun providesFakeDatabase(): FakeDatabase = FakeDatabase()

    @Provides fun providesDatabase(fakeDatabase: FakeDatabase): Database = fakeDatabase

    @Provides fun providesRecipeQueries(database: Database): RecipeQueries = database.recipeQueries

    @Provides
    fun providesRecipeBookQueries(database: Database): RecipeBookQueries =
        database.recipeBookQueries

    @Provides
    fun providesRecipeBookRecipeQueries(database: Database): RecipeBookRecipeQueries =
        database.recipeBookRecipeQueries

    @Provides
    fun providesGroceryQueries(database: Database): GroceryQueries = database.groceryQueries

    @Provides
    fun providesGroceryListQueries(database: Database): GroceryListQueries =
        database.groceryListQueries

    @Provides
    fun providesGroceryListMemberQueries(database: Database): GroceryListMemberQueries =
        database.groceryListMemberQueries

    @Provides
    fun providesGroceryAutocompleteItemQueries(database: Database): GroceryAutocompleteItemQueries =
        database.groceryAutocompleteItemQueries

    @Provides
    fun providesGroceryCategoryOverrideQueries(database: Database): GroceryCategoryOverrideQueries =
        database.groceryCategoryOverrideQueries

    @Provides
    fun providesMealPlanQueries(database: Database): MealPlanQueries = database.mealPlanQueries

    @Provides
    fun providesBrowserHistoryQueries(database: Database): BrowserHistoryQueries =
        database.browserHistoryQueries

    @Provides
    fun providesCookingSessionQueries(database: Database): CookingSessionQueries =
        database.cookingSessionQueries

    @Provides
    fun providesCategoryQueries(database: Database): CategoryQueries = database.categoryQueries

    @Provides
    fun providesRecipeCategoryQueries(database: Database): RecipeCategoryQueries =
        database.recipeCategoryQueries

    @Provides
    fun providesAiChatMessageQueries(database: Database): AiChatMessageQueries =
        database.aiChatMessageQueries

    @Provides
    fun providesAiChatConversationQueries(database: Database): AiChatConversationQueries =
        database.aiChatConversationQueries
}
