package com.plusmobileapps.chefmate.grocery.data.testing

import com.plusmobileapps.chefmate.grocery.data.GroceryItem
import com.plusmobileapps.chefmate.grocery.data.GroceryListInvite
import com.plusmobileapps.chefmate.grocery.data.GroceryListModel
import com.plusmobileapps.chefmate.grocery.data.GroceryRepository
import com.plusmobileapps.chefmate.grocery.data.IngredientParser
import com.plusmobileapps.chefmate.grocery.data.ListCollaborator
import com.plusmobileapps.chefmate.grocery.data.ListRole
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class FakeGroceryRepository : GroceryRepository {
    private val _groceries = MutableStateFlow<List<GroceryItem>>(emptyList())
    private val _lists =
        MutableStateFlow(listOf(GroceryListModel(id = 1L, name = "My Grocery List")))
    private var nextId = 1L
    private var nextListId = 2L
    private val itemListMap = mutableMapOf<Long, Long>()
    private val pendingDeletes = MutableStateFlow<Set<Long>>(emptySet())

    override fun getGroceries(): Flow<List<GroceryItem>> =
        combine(_groceries, pendingDeletes) { items, pending ->
            items.filter { it.id !in pending }
        }

    override fun getGroceries(listId: Long): Flow<List<GroceryItem>> =
        combine(_groceries, pendingDeletes) { items, pending ->
            items.filter { itemListMap[it.id] == listId && it.id !in pending }
        }

    override fun getGroceryLists(): Flow<List<GroceryListModel>> = _lists.asStateFlow()

    override suspend fun addGrocery(name: String) {
        val defaultListId = _lists.value.firstOrNull()?.id ?: 1L
        addGrocery(defaultListId, name)
    }

    override suspend fun addGrocery(listId: Long, name: String) {
        val parsed = IngredientParser.parse(name)
        val newItem =
            GroceryItem(
                id = nextId++,
                name = name,
                displayName = parsed.name,
                quantity = parsed.quantity,
                category = parsed.category,
                isChecked = false,
            )
        itemListMap[newItem.id] = listId
        _groceries.update { it + newItem }
    }

    override suspend fun addGroceries(names: List<String>) {
        val defaultListId = _lists.value.firstOrNull()?.id ?: 1L
        addGroceries(defaultListId, names)
    }

    override suspend fun addGroceries(listId: Long, names: List<String>) {
        addGroceries(listId, names, null)
    }

    override suspend fun addGroceries(listId: Long, names: List<String>, recipeName: String?) {
        val newItems = names.map { name ->
            val parsed = IngredientParser.parse(name)
            val item =
                GroceryItem(
                    id = nextId++,
                    name = name,
                    displayName = parsed.name,
                    quantity = parsed.quantity,
                    category = parsed.category,
                    isChecked = false,
                    recipeName = recipeName,
                )
            itemListMap[item.id] = listId
            item
        }
        _groceries.update { it + newItems }
    }

    override suspend fun updateChecked(item: GroceryItem, isChecked: Boolean) {
        _groceries.update { items ->
            items.map { if (it.id == item.id) it.copy(isChecked = isChecked) else it }
        }
    }

    override suspend fun deleteGrocery(item: GroceryItem) {
        itemListMap.remove(item.id)
        _groceries.update { items -> items.filter { it.id != item.id } }
    }

    override fun stageDelete(item: GroceryItem) {
        pendingDeletes.update { it + item.id }
    }

    override fun undoDelete(itemId: Long) {
        pendingDeletes.update { it - itemId }
    }

    override fun commitDelete(itemId: Long) {
        if (itemId !in pendingDeletes.value) return
        itemListMap.remove(itemId)
        _groceries.update { items -> items.filter { it.id != itemId } }
        pendingDeletes.update { it - itemId }
    }

    override suspend fun getGrocery(id: Long): GroceryItem? = _groceries.value.find { it.id == id }

    override suspend fun syncAllUnsynced() {}

    override suspend fun updateGrocery(item: GroceryItem) {
        _groceries.update { items -> items.map { if (it.id == item.id) item else it } }
    }

    override suspend fun createGroceryList(name: String): Long {
        val id = nextListId++
        _lists.update { it + GroceryListModel(id = id, name = name) }
        return id
    }

    override suspend fun deleteGroceryList(id: Long) {
        _lists.update { lists -> lists.filter { it.id != id } }
        val itemIds = itemListMap.filter { it.value == id }.keys
        itemListMap.keys.removeAll(itemIds)
        _groceries.update { items -> items.filter { it.id !in itemIds } }
    }

    override suspend fun renameGroceryList(id: Long, name: String) {
        _lists.update { lists -> lists.map { if (it.id == id) it.copy(name = name) else it } }
    }

    override suspend fun ensureDefaultList(): Long {
        val existing = _lists.value.firstOrNull()
        if (existing != null) return existing.id
        val id = nextListId++
        _lists.update { it + GroceryListModel(id = id, name = "My Grocery List") }
        return id
    }

    override suspend fun deleteAllGroceries(listId: Long) {
        val itemIds = itemListMap.filter { it.value == listId }.keys
        itemListMap.keys.removeAll(itemIds)
        _groceries.update { items -> items.filter { it.id !in itemIds } }
    }

    override suspend fun deletePurchasedGroceries(listId: Long) {
        val itemIds =
            itemListMap
                .filter { it.value == listId }
                .keys
                .filter { id -> _groceries.value.any { it.id == id && it.isChecked } }
                .toSet()
        itemListMap.keys.removeAll(itemIds)
        _groceries.update { items -> items.filter { it.id !in itemIds } }
    }

    override suspend fun clearLocalData() {
        _groceries.value = emptyList()
        _lists.value = emptyList()
        itemListMap.clear()
    }

    override fun getListCollaborators(listId: Long): Flow<List<ListCollaborator>> =
        MutableStateFlow(emptyList())

    override suspend fun refreshListMembers(listId: Long) {}

    override suspend fun inviteCollaborator(listId: Long, email: String, role: ListRole) {}

    override suspend fun removeCollaborator(listId: Long, collaboratorId: Long) {}

    val pendingInvitations = MutableStateFlow<List<GroceryListInvite>>(emptyList())
    val acceptedInvitations: MutableList<String> = mutableListOf()
    val rejectedInvitations: MutableList<String> = mutableListOf()

    override suspend fun acceptInvitation(memberId: String) {
        acceptedInvitations += memberId
        pendingInvitations.update { invites -> invites.filterNot { it.memberId == memberId } }
    }

    override suspend fun rejectInvitation(memberId: String) {
        rejectedInvitations += memberId
        pendingInvitations.update { invites -> invites.filterNot { it.memberId == memberId } }
    }

    override fun getPendingInvitations(): Flow<List<GroceryListInvite>> = pendingInvitations
}
