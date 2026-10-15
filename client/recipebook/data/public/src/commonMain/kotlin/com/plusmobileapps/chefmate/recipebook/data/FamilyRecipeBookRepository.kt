package com.plusmobileapps.chefmate.recipebook.data

/** A recipe book that belongs to a family. [id] is the remote (Supabase) id. */
data class FamilyRecipeBook(val id: String, val name: String, val recipeCount: Int = 0)

/**
 * Manages the recipe books a family shares, from Manage Family. Every accepted member can view a
 * family book's recipes and file their own recipes into it; only family owners and admins create,
 * rename and delete the books (see `FamilyPermissions.canManageRecipeBooks`, enforced server-side).
 *
 * These are online operations that throw on network or permission failure. Each write re-syncs the
 * local recipe books (and recipes) in the background so the change shows up in the book picker.
 */
interface FamilyRecipeBookRepository {
    /** The family's books, oldest first. */
    suspend fun getBooks(familyId: String): List<FamilyRecipeBook>

    suspend fun createBook(familyId: String, name: String)

    suspend fun renameBook(bookId: String, name: String)

    /** Deletes the book for the whole family. The recipes in it stay with their authors. */
    suspend fun deleteBook(bookId: String)

    /**
     * Re-syncs the local recipe books and recipes in the background, e.g. after leaving a family so
     * its books (and the other members' recipes in them) disappear.
     */
    fun refreshLocalBooks()
}
