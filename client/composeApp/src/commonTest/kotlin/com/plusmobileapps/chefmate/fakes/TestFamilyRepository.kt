package com.plusmobileapps.chefmate.fakes

import com.plusmobileapps.chefmate.di.AppScope
import com.plusmobileapps.chefmate.family.data.FamilyRepository
import com.plusmobileapps.chefmate.family.data.impl.FamilyRepositoryImpl
import com.plusmobileapps.chefmate.family.data.testing.FakeFamilyRepository
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn

/**
 * Replaces the production [FamilyRepositoryImpl] in tests. Families are online-only, so without
 * this every Manage Family screen would render its offline error state. [FakeFamilyRepository]
 * enforces the same permission rules the server does, so flows behave realistically.
 */
@SingleIn(AppScope::class)
@Inject
@ContributesBinding(scope = AppScope::class, replaces = [FamilyRepositoryImpl::class])
class TestFamilyRepository(private val fake: FakeFamilyRepository = FakeFamilyRepository()) :
    FamilyRepository by fake {

    /** Seeds a family the current user owns (or holds [seed]'s role in). Returns its id. */
    fun seedFamily(
        name: String,
        myRole: com.plusmobileapps.chefmate.family.data.FamilyRole =
            com.plusmobileapps.chefmate.family.data.FamilyRole.OWNER,
        others: List<FakeFamilyRepository.SeedMember> = emptyList(),
    ): String = fake.seedFamily(name = name, myRole = myRole, others = others)

    /** Seeds a pending invite addressed to the current user. Returns the member-row id. */
    fun seedInvite(familyName: String): String = fake.seedInvite(familyName)

    /** When non-null, every call throws it, so tests can exercise the offline paths. */
    var failure: Throwable?
        get() = fake.failure
        set(value) {
            fake.failure = value
        }
}
