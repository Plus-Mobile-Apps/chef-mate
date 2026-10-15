package com.plusmobileapps.chefmate.recipebook.data.impl

import com.plusmobileapps.chefmate.di.AppScope
import com.plusmobileapps.chefmate.di.IO
import com.plusmobileapps.chefmate.recipe.data.RecipeRepository
import com.plusmobileapps.chefmate.recipebook.data.FamilyRecipeBook
import com.plusmobileapps.chefmate.recipebook.data.FamilyRecipeBookRepository
import com.plusmobileapps.chefmate.recipebook.data.RecipeBookRepository
import com.plusmobileapps.chefmate.recipebook.data.impl.remote.FamilyRecipeBookRemoteDataSource
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class FamilyRecipeBookRepositoryImpl(
    @IO ioContext: CoroutineContext,
    private val remoteDataSource: FamilyRecipeBookRemoteDataSource,
    private val recipeBookRepository: RecipeBookRepository,
    private val recipeRepository: RecipeRepository,
) : FamilyRecipeBookRepository {

    // App-lifetime scope so the follow-up sync finishes even if the family screen is closed.
    private val scope = CoroutineScope(ioContext + SupervisorJob())

    override suspend fun getBooks(familyId: String): List<FamilyRecipeBook> =
        remoteDataSource.fetchFamilyRecipeBooks(familyId).map {
            FamilyRecipeBook(id = it.id, name = it.name, recipeCount = it.recipeCount)
        }

    override suspend fun createBook(familyId: String, name: String) {
        remoteDataSource.createFamilyRecipeBook(familyId, name.trim())
        refreshLocalBooks()
    }

    override suspend fun renameBook(bookId: String, name: String) {
        remoteDataSource.renameFamilyRecipeBook(bookId, name.trim())
        refreshLocalBooks()
    }

    override suspend fun deleteBook(bookId: String) {
        remoteDataSource.deleteFamilyRecipeBook(bookId)
        refreshLocalBooks()
    }

    override fun refreshLocalBooks() {
        scope.launch {
            // Books first: recipe sync files recipes under the books it already knows about.
            recipeBookRepository.syncAllUnsynced()
            recipeRepository.syncAllUnsynced()
        }
    }
}
