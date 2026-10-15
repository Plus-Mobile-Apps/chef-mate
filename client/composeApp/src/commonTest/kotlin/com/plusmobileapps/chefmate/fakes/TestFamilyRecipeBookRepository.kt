package com.plusmobileapps.chefmate.fakes

import com.plusmobileapps.chefmate.di.AppScope
import com.plusmobileapps.chefmate.recipebook.data.FamilyRecipeBook
import com.plusmobileapps.chefmate.recipebook.data.FamilyRecipeBookRepository
import com.plusmobileapps.chefmate.recipebook.data.impl.FamilyRecipeBookRepositoryImpl
import com.plusmobileapps.chefmate.recipebook.data.testing.FakeFamilyRecipeBookRepository
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn

/**
 * Replaces the production [FamilyRecipeBookRepositoryImpl] in tests. Family books are managed
 * through online-only RPCs, so without this the Manage Family recipe books section would only ever
 * show its load error.
 */
@SingleIn(AppScope::class)
@Inject
@ContributesBinding(scope = AppScope::class, replaces = [FamilyRecipeBookRepositoryImpl::class])
class TestFamilyRecipeBookRepository(
    private val fake: FakeFamilyRecipeBookRepository = FakeFamilyRecipeBookRepository()
) : FamilyRecipeBookRepository by fake {

    /** Seeds a book into the family with [familyId], as if an owner or admin had created it. */
    fun seedBook(familyId: String, name: String) {
        fake.books.getOrPut(familyId) { mutableListOf() }.add(FamilyRecipeBook("seed-$name", name))
    }
}
