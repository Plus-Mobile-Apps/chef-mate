package com.plusmobileapps.chefmate.recipebook.data.testing

import com.plusmobileapps.chefmate.recipebook.data.FamilyRecipeBook
import com.plusmobileapps.chefmate.recipebook.data.FamilyRecipeBookRepository

/** In-memory [FamilyRecipeBookRepository]. Set [error] to make every call throw. */
class FakeFamilyRecipeBookRepository(
    /** Family id → that family's books. */
    val books: MutableMap<String, MutableList<FamilyRecipeBook>> = mutableMapOf()
) : FamilyRecipeBookRepository {
    var error: Throwable? = null
    var refreshCount = 0
        private set

    private var nextId = 1

    override suspend fun getBooks(familyId: String): List<FamilyRecipeBook> {
        error?.let { throw it }
        return books[familyId].orEmpty().toList()
    }

    override suspend fun createBook(familyId: String, name: String) {
        error?.let { throw it }
        books.getOrPut(familyId) { mutableListOf() }.add(FamilyRecipeBook("book-${nextId++}", name))
    }

    override suspend fun renameBook(bookId: String, name: String) {
        error?.let { throw it }
        books.values.forEach { familyBooks ->
            val index = familyBooks.indexOfFirst { it.id == bookId }
            if (index >= 0) familyBooks[index] = familyBooks[index].copy(name = name)
        }
    }

    override suspend fun deleteBook(bookId: String) {
        error?.let { throw it }
        books.values.forEach { familyBooks -> familyBooks.removeAll { it.id == bookId } }
    }

    override fun refreshLocalBooks() {
        refreshCount++
    }
}
