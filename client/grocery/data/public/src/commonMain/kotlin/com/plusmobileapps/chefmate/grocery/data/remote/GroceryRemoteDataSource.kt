package com.plusmobileapps.chefmate.grocery.data.remote

import kotlinx.coroutines.flow.Flow

interface GroceryRemoteDataSource {
    /**
     * Emits once per remote change (INSERT/UPDATE/DELETE) on the grocery tables the signed-in user
     * can access. Collectors respond by re-running a full reconcile so a change made on another
     * device lands in the local cache. The underlying realtime channel subscribes on first
     * collection and tears down when collection stops.
     */
    fun observeChanges(): Flow<Unit>

    suspend fun ensureDefaultList(ownerId: String): String

    suspend fun upsertGroceryItem(item: RemoteGroceryItem): RemoteGroceryItem

    /**
     * Updates an existing item's mutable columns (name, checked state, aisle, recipe) by its remote
     * id. Unlike [upsertGroceryItem] this never sends immutable columns such as `created_at`, so an
     * edit can't trip a NOT NULL / conflict-update constraint. [item].id must be non-null.
     */
    suspend fun updateGroceryItem(item: RemoteGroceryItem): RemoteGroceryItem

    suspend fun deleteGroceryItem(remoteId: String)

    suspend fun fetchAllGroceryItems(listId: String): List<RemoteGroceryItem>

    suspend fun createGroceryList(list: RemoteGroceryList): RemoteGroceryList

    suspend fun fetchGroceryLists(ownerId: String): List<RemoteGroceryList>

    suspend fun fetchAccessibleGroceryLists(): List<RemoteGroceryList>

    suspend fun deleteGroceryList(remoteId: String)

    suspend fun updateGroceryList(list: RemoteGroceryList): RemoteGroceryList

    suspend fun fetchListMembers(listId: String): List<RemoteGroceryListMember>

    suspend fun fetchListCollaborators(listId: String): List<RemoteGroceryListCollaborator>

    suspend fun inviteToList(member: RemoteGroceryListMember): RemoteGroceryListMember

    suspend fun fetchPendingInvitations(email: String): List<RemoteGroceryListInvite>

    suspend fun respondToInvitation(memberId: String, userId: String, accept: Boolean)

    suspend fun removeFromList(memberId: String)

    /** Names of the families with [familyIds] that the user belongs to. */
    suspend fun fetchFamilyNames(familyIds: Collection<String>): List<RemoteFamilyName>

    /** The family's grocery lists. Empty when the user isn't a member. */
    suspend fun fetchFamilyGroceryLists(familyId: String): List<RemoteFamilyGroceryList>

    /** Creates a list in the family and returns its id. Family owners and admins only. */
    suspend fun createFamilyGroceryList(familyId: String, name: String): String

    suspend fun renameFamilyGroceryList(listId: String, name: String)

    suspend fun deleteFamilyGroceryList(listId: String)
}
