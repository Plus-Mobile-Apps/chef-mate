package com.plusmobileapps.chefmate.recipebook.data.impl

import com.plusmobileapps.chefmate.recipebook.data.impl.remote.FamilyRecipeBookRemoteDataSource
import com.plusmobileapps.chefmate.recipebook.data.impl.remote.RecipeBookRemoteDataSource
import com.plusmobileapps.chefmate.recipebook.data.impl.remote.RemoteFamilyName
import com.plusmobileapps.chefmate.recipebook.data.impl.remote.RemoteFamilyRecipeBook
import com.plusmobileapps.chefmate.recipebook.data.impl.remote.RemoteRecipeBook

/**
 * Stands in for the server's recipe_books table as RLS shows it to the signed-in user: [books] is
 * what [fetchAccessibleRecipeBooks] returns, and the family RPCs read and write the same list.
 */
class FakeRecipeBookServer : RecipeBookRemoteDataSource {
    val books: MutableList<RemoteRecipeBook> = mutableListOf()
    private var nextId = 1

    override suspend fun upsertRecipeBook(book: RemoteRecipeBook): RemoteRecipeBook {
        val saved = book.copy(id = book.id ?: "remote-book-${nextId++}")
        books.removeAll { it.id == saved.id }
        books += saved
        return saved
    }

    override suspend fun deleteRecipeBook(remoteId: String) {
        books.removeAll { it.id == remoteId }
    }

    override suspend fun fetchAccessibleRecipeBooks(): List<RemoteRecipeBook> = books.toList()

    fun addFamilyBook(id: String, name: String, familyId: String, creator: String) {
        books += RemoteRecipeBook(id = id, ownerId = creator, name = name, familyId = familyId)
    }
}

/** In-memory [FamilyRecipeBookRemoteDataSource] over a [FakeRecipeBookServer]. */
class FakeFamilyRecipeBookRemoteDataSource(
    private val server: FakeRecipeBookServer = FakeRecipeBookServer()
) : FamilyRecipeBookRemoteDataSource {
    /** Family id → name for the families the signed-in user belongs to. */
    val familyNames = mutableMapOf<String, String>()

    /** When set, every family-book RPC throws it, e.g. to simulate a permission error. */
    var error: Throwable? = null

    private var nextId = 1

    override suspend fun fetchFamilyNames(familyIds: Collection<String>): List<RemoteFamilyName> =
        familyIds.mapNotNull { id ->
            familyNames[id]?.let { RemoteFamilyName(id, it) }
        }

    override suspend fun fetchFamilyRecipeBooks(familyId: String): List<RemoteFamilyRecipeBook> {
        error?.let { throw it }
        return server.books
            .filter { it.familyId == familyId }
            .map { RemoteFamilyRecipeBook(id = it.id!!, name = it.name) }
    }

    /** Family books are created by the family, standing in for the creating user. */
    override suspend fun createFamilyRecipeBook(familyId: String, name: String): String {
        error?.let { throw it }
        val id = "remote-family-book-${nextId++}"
        server.addFamilyBook(id = id, name = name, familyId = familyId, creator = familyId)
        return id
    }

    override suspend fun renameFamilyRecipeBook(bookId: String, name: String) {
        error?.let { throw it }
        val index = server.books.indexOfFirst { it.id == bookId }
        if (index >= 0) server.books[index] = server.books[index].copy(name = name)
    }

    override suspend fun deleteFamilyRecipeBook(bookId: String) {
        error?.let { throw it }
        server.books.removeAll { it.id == bookId }
    }
}
