package com.plusmobileapps.chefmate.grocery.data

/** A grocery list that belongs to a family. [id] is the remote (Supabase) id. */
data class FamilyGroceryList(val id: String, val name: String, val itemCount: Int = 0)

/**
 * Manages the grocery lists a family shares, from Manage Family. Every accepted member can view and
 * edit a family list's items; only family owners and admins create, rename and delete the lists
 * (see `FamilyPermissions.canManageGroceryLists`, enforced server-side).
 *
 * These are online operations that throw on network or permission failure. Each write re-syncs the
 * local grocery cache in the background so the change shows up on the grocery list.
 */
interface FamilyGroceryListRepository {
    /** The family's lists, oldest first. */
    suspend fun getLists(familyId: String): List<FamilyGroceryList>

    suspend fun createList(familyId: String, name: String)

    suspend fun renameList(listId: String, name: String)

    /** Deletes the list and its items for the whole family. */
    suspend fun deleteList(listId: String)

    /**
     * Re-syncs the local grocery cache in the background, e.g. after leaving a family so its lists
     * disappear.
     */
    fun refreshLocalLists()
}
