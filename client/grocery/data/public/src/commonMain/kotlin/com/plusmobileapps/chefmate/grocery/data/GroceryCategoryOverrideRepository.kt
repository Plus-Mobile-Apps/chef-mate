package com.plusmobileapps.chefmate.grocery.data

import kotlinx.coroutines.flow.Flow

/** A single user-defined "always file [name] under [category]" rule. */
data class GroceryCategoryOverride(val id: Long, val name: String, val category: GroceryCategory)

/**
 * Manages the user's persistent name→aisle rules. When a grocery item's parsed name matches a rule,
 * the rule's [GroceryCategory] overrides the built-in [IngredientParser] guess (a per-item stored
 * aisle on the grocery row still wins over the rule). Rules are created inline from the grocery
 * item detail screen and managed from the grocery category rules settings screen.
 */
interface GroceryCategoryOverrideRepository {
    /** Emits the saved rules, sorted case-insensitively by name. */
    fun observeOverrides(): Flow<List<GroceryCategoryOverride>>

    /**
     * Emits the rules as a lookup map keyed by the lowercased name, for fast categorization while
     * mapping stored grocery rows.
     */
    fun observeOverrideMap(): Flow<Map<String, GroceryCategory>>

    /**
     * Adds or updates the rule for [name] → [category]. Trimmed; blanks are ignored. Matching an
     * existing name (case-insensitively) updates its aisle.
     */
    suspend fun setOverride(name: String, category: GroceryCategory)

    suspend fun removeOverride(id: Long)

    /** Removes any rule matching [name] (case-insensitive). No-op if none exists. */
    suspend fun removeOverrideByName(name: String)

    /**
     * Wipes every rule. Unlike the other grocery repositories, this is **not** called on sign-in or
     * sign-out: rules are device-local (there is no backend table yet), so clearing them on a
     * routine session change would destroy them permanently instead of restoring them on the next
     * sync. Only explicit erasure — `DeleteAccountUseCase` — calls this.
     */
    suspend fun clearLocalData()
}
