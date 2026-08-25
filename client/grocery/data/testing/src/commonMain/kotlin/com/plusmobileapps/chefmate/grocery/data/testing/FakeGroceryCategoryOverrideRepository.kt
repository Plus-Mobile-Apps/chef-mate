package com.plusmobileapps.chefmate.grocery.data.testing

import com.plusmobileapps.chefmate.grocery.data.GroceryCategory
import com.plusmobileapps.chefmate.grocery.data.GroceryCategoryOverride
import com.plusmobileapps.chefmate.grocery.data.GroceryCategoryOverrideRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeGroceryCategoryOverrideRepository(
    private val overrides: MutableStateFlow<List<GroceryCategoryOverride>> =
        MutableStateFlow(emptyList())
) : GroceryCategoryOverrideRepository {

    // Tests are single-threaded; non-atomic counter is fine.
    private var nextId: Long = 1L

    override fun observeOverrides(): Flow<List<GroceryCategoryOverride>> = overrides

    override fun observeOverrideMap(): Flow<Map<String, GroceryCategory>> = overrides.map { list ->
        list.associate { it.name.lowercase() to it.category }
    }

    override suspend fun setOverride(name: String, category: GroceryCategory) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return
        val existing = overrides.value.firstOrNull { it.name.equals(trimmed, ignoreCase = true) }
        overrides.value =
            if (existing != null) {
                    overrides.value.map {
                        if (it.id == existing.id) it.copy(category = category) else it
                    }
                } else {
                    overrides.value +
                        GroceryCategoryOverride(id = nextId++, name = trimmed, category = category)
                }
                .sortedBy { it.name.lowercase() }
    }

    override suspend fun removeOverride(id: Long) {
        overrides.value = overrides.value.filterNot { it.id == id }
    }

    override suspend fun removeOverrideByName(name: String) {
        val trimmed = name.trim()
        overrides.value = overrides.value.filterNot { it.name.equals(trimmed, ignoreCase = true) }
    }

    override suspend fun clearLocalData() {
        overrides.value = emptyList()
    }
}
