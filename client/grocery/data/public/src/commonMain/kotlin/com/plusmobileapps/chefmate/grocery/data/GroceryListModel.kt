package com.plusmobileapps.chefmate.grocery.data

data class GroceryListModel(
    val id: Long,
    val name: String,
    val syncStatus: SyncStatus = SyncStatus.NOT_SYNCED,
    val role: ListRole = ListRole.OWNER,
    val isShared: Boolean = false,
    /** The remote id of the family this list belongs to, or null for a personal list. */
    val familyId: String? = null,
    /** The family's name, for labeling; null when unknown or for a personal list. */
    val familyName: String? = null,
) {
    /**
     * Family lists are managed (renamed, deleted) from Manage Family rather than the grocery list.
     */
    val isFamily: Boolean
        get() = familyId != null
}
