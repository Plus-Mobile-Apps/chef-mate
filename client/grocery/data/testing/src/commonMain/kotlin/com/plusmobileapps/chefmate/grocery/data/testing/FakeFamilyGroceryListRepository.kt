package com.plusmobileapps.chefmate.grocery.data.testing

import com.plusmobileapps.chefmate.grocery.data.FamilyGroceryList
import com.plusmobileapps.chefmate.grocery.data.FamilyGroceryListRepository

/** In-memory [FamilyGroceryListRepository]. Set [error] to make every call throw. */
class FakeFamilyGroceryListRepository(
    /** Family id → that family's lists. */
    val lists: MutableMap<String, MutableList<FamilyGroceryList>> = mutableMapOf()
) : FamilyGroceryListRepository {
    var error: Throwable? = null
    var refreshCount = 0
        private set

    private var nextId = 1

    override suspend fun getLists(familyId: String): List<FamilyGroceryList> {
        error?.let { throw it }
        return lists[familyId].orEmpty().toList()
    }

    override suspend fun createList(familyId: String, name: String) {
        error?.let { throw it }
        lists
            .getOrPut(familyId) { mutableListOf() }
            .add(FamilyGroceryList("list-${nextId++}", name))
    }

    override suspend fun renameList(listId: String, name: String) {
        error?.let { throw it }
        lists.values.forEach { familyLists ->
            val index = familyLists.indexOfFirst { it.id == listId }
            if (index >= 0) familyLists[index] = familyLists[index].copy(name = name)
        }
    }

    override suspend fun deleteList(listId: String) {
        error?.let { throw it }
        lists.values.forEach { familyLists -> familyLists.removeAll { it.id == listId } }
    }

    override fun refreshLocalLists() {
        refreshCount++
    }
}
