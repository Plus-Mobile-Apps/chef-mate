package com.plusmobileapps.chefmate.recipebook.data.impl.remote

/** Family recipe books: the family-book RPCs plus family names for labeling synced books. */
interface FamilyRecipeBookRemoteDataSource {
    /** Names of the families with [familyIds] that the user belongs to. */
    suspend fun fetchFamilyNames(familyIds: Collection<String>): List<RemoteFamilyName>

    /** The family's recipe books. Empty when the user isn't a member. */
    suspend fun fetchFamilyRecipeBooks(familyId: String): List<RemoteFamilyRecipeBook>

    /** Creates a book in the family and returns its id. Family owners and admins only. */
    suspend fun createFamilyRecipeBook(familyId: String, name: String): String

    suspend fun renameFamilyRecipeBook(bookId: String, name: String)

    suspend fun deleteFamilyRecipeBook(bookId: String)
}
