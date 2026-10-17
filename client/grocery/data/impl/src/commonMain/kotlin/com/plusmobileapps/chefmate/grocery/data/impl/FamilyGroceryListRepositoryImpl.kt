package com.plusmobileapps.chefmate.grocery.data.impl

import com.plusmobileapps.chefmate.di.AppScope
import com.plusmobileapps.chefmate.di.IO
import com.plusmobileapps.chefmate.grocery.data.FamilyGroceryList
import com.plusmobileapps.chefmate.grocery.data.FamilyGroceryListRepository
import com.plusmobileapps.chefmate.grocery.data.GroceryRepository
import com.plusmobileapps.chefmate.grocery.data.remote.GroceryRemoteDataSource
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
class FamilyGroceryListRepositoryImpl(
    @IO ioContext: CoroutineContext,
    private val remoteDataSource: GroceryRemoteDataSource,
    private val groceryRepository: GroceryRepository,
) : FamilyGroceryListRepository {

    // App-lifetime scope so the follow-up sync finishes even if the family screen is closed.
    private val scope = CoroutineScope(ioContext + SupervisorJob())

    override suspend fun getLists(familyId: String): List<FamilyGroceryList> =
        remoteDataSource.fetchFamilyGroceryLists(familyId).map {
            FamilyGroceryList(id = it.id, name = it.name, itemCount = it.itemCount)
        }

    override suspend fun createList(familyId: String, name: String) {
        remoteDataSource.createFamilyGroceryList(familyId, name.trim())
        refreshLocalLists()
    }

    override suspend fun renameList(listId: String, name: String) {
        remoteDataSource.renameFamilyGroceryList(listId, name.trim())
        refreshLocalLists()
    }

    override suspend fun deleteList(listId: String) {
        remoteDataSource.deleteFamilyGroceryList(listId)
        refreshLocalLists()
    }

    override fun refreshLocalLists() {
        scope.launch { groceryRepository.syncAllUnsynced() }
    }
}
