@file:OptIn(ExperimentalTime::class)

package com.plusmobileapps.chefmate.recipebook.data

import com.plusmobileapps.chefmate.recipe.data.SyncStatus
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

data class RecipeBook(
    val id: Long,
    val name: String,
    /** The single "My Recipes" book every user starts with. New recipes default here. */
    val isDefault: Boolean = false,
    /**
     * Whether the current user owns this book (vs. being a collaborator on a book shared with
     * them). True for locally-created books that haven't synced an owner yet. Gates collaborator
     * management in the UI. Always false for a family book, even for its creator.
     */
    val isOwnedByCurrentUser: Boolean = true,
    val syncStatus: SyncStatus = SyncStatus.NOT_SYNCED,
    val createdAt: Instant,
    val updatedAt: Instant,
    /** The remote id of the family this book belongs to, or null for a personal book. */
    val familyId: String? = null,
    /** The family's name, for labeling; null when unknown or for a personal book. */
    val familyName: String? = null,
) {
    /**
     * Family books are managed (renamed, deleted) from Manage Family rather than the book picker.
     */
    val isFamily: Boolean
        get() = familyId != null

    companion object {
        val Empty =
            RecipeBook(
                id = -1,
                name = "",
                isDefault = false,
                createdAt = Instant.DISTANT_PAST,
                updatedAt = Instant.DISTANT_PAST,
            )

        /** Populated sample for @Preview composables and screenshot tests. */
        val Sample =
            RecipeBook(
                id = 1L,
                name = "My Recipes",
                isDefault = true,
                syncStatus = SyncStatus.SYNCED,
                createdAt = Instant.DISTANT_PAST,
                updatedAt = Instant.DISTANT_PAST,
            )

        val Samples: List<RecipeBook> =
            listOf(
                Sample,
                Sample.copy(id = 2L, name = "Weeknight Dinners", isDefault = false),
                Sample.copy(id = 3L, name = "Holiday Baking", isDefault = false),
            )
    }
}
