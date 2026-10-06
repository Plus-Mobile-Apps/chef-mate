package com.plusmobileapps.chefmate.family.data.testing

import com.plusmobileapps.chefmate.family.data.Family
import com.plusmobileapps.chefmate.family.data.FamilyInvite
import com.plusmobileapps.chefmate.family.data.FamilyMember
import com.plusmobileapps.chefmate.family.data.FamilyMemberStatus
import com.plusmobileapps.chefmate.family.data.FamilyPermissions
import com.plusmobileapps.chefmate.family.data.FamilyRepository
import com.plusmobileapps.chefmate.family.data.FamilyRole

/**
 * In-memory [FamilyRepository] that applies [FamilyPermissions] the way the server does, so tests
 * exercise realistic outcomes. The current user is identified by [selfEmail].
 */
class FakeFamilyRepository(val selfEmail: String = "me@example.com") : FamilyRepository {

    private data class StoredFamily(val id: String, var name: String)

    private data class StoredMember(
        val id: String,
        val familyId: String,
        val email: String,
        var role: FamilyRole,
        var status: FamilyMemberStatus,
        val name: String? = null,
    )

    private val families = mutableListOf<StoredFamily>()
    private val members = mutableListOf<StoredMember>()
    private var nextId = 1

    /** When non-null, every call throws it (e.g. to simulate being offline). */
    var failure: Throwable? = null

    /** Number of [getFamilies] calls, to assert reloads. */
    var getFamiliesCount: Int = 0
        private set

    /**
     * Adds a family where the current user has [myRole]. [others] are added as-is. Returns its id.
     */
    fun seedFamily(
        name: String,
        myRole: FamilyRole = FamilyRole.OWNER,
        others: List<SeedMember> = emptyList(),
    ): String {
        val familyId = "family-${nextId++}"
        families += StoredFamily(familyId, name)
        members +=
            StoredMember(
                id = "member-${nextId++}",
                familyId = familyId,
                email = selfEmail,
                role = myRole,
                status = FamilyMemberStatus.ACCEPTED,
                name = "Me",
            )
        others.forEach { seed ->
            members +=
                StoredMember(
                    id = seed.id ?: "member-${nextId++}",
                    familyId = familyId,
                    email = seed.email,
                    role = seed.role,
                    status = seed.status,
                    name = seed.name,
                )
        }
        return familyId
    }

    /** Adds a pending invite for the current user to a family they aren't in. Returns member id. */
    fun seedInvite(familyName: String, role: FamilyRole = FamilyRole.MEMBER): String {
        val familyId = "family-${nextId++}"
        families += StoredFamily(familyId, familyName)
        val id = "member-${nextId++}"
        members += StoredMember(id, familyId, selfEmail, role, FamilyMemberStatus.PENDING)
        return id
    }

    data class SeedMember(
        val email: String,
        val role: FamilyRole = FamilyRole.MEMBER,
        val status: FamilyMemberStatus = FamilyMemberStatus.ACCEPTED,
        val name: String? = null,
        val id: String? = null,
    )

    override suspend fun getFamilies(): List<Family> {
        check()
        getFamiliesCount++
        return families
            .mapNotNull { family ->
                val role = roleOf(family.id) ?: return@mapNotNull null
                Family(
                    id = family.id,
                    name = family.name,
                    myRole = role,
                    memberCount =
                        members.count {
                            it.familyId == family.id && it.status == FamilyMemberStatus.ACCEPTED
                        },
                )
            }
            .sortedBy { it.name.lowercase() }
    }

    override suspend fun getFamily(familyId: String): Family? =
        getFamilies().firstOrNull { it.id == familyId }

    override suspend fun getMembers(familyId: String): List<FamilyMember> {
        check()
        if (roleOf(familyId) == null) return emptyList()
        return members
            .filter { it.familyId == familyId }
            .map { it.toMember() }
            .sortedWith(
                compareBy<FamilyMember> { it.role.ordinal }
                    .thenBy { it.status != FamilyMemberStatus.ACCEPTED }
                    .thenBy { it.email }
            )
    }

    override suspend fun createFamily(name: String): String {
        check()
        require(name.isNotBlank())
        return seedFamily(name.trim())
    }

    override suspend fun renameFamily(familyId: String, name: String) {
        check()
        forbidUnless(FamilyPermissions.canRename(actor(familyId)))
        families.first { it.id == familyId }.name = name.trim()
    }

    override suspend fun deleteFamily(familyId: String) {
        check()
        forbidUnless(FamilyPermissions.canDelete(actor(familyId)))
        families.removeAll { it.id == familyId }
        members.removeAll { it.familyId == familyId }
    }

    override suspend fun invite(familyId: String, email: String, role: FamilyRole) {
        check()
        forbidUnless(role in FamilyPermissions.invitableRoles(actor(familyId)))
        val normalized = email.trim().lowercase()
        val existing = members.firstOrNull { it.familyId == familyId && it.email == normalized }
        when {
            existing == null ->
                members +=
                    StoredMember(
                        id = "member-${nextId++}",
                        familyId = familyId,
                        email = normalized,
                        role = role,
                        status = FamilyMemberStatus.PENDING,
                    )
            existing.status == FamilyMemberStatus.REJECTED -> {
                existing.status = FamilyMemberStatus.PENDING
                existing.role = role
            }
            else -> throw IllegalStateException("$normalized is already invited")
        }
    }

    override suspend fun removeMember(memberId: String) {
        check()
        val target = members.firstOrNull { it.id == memberId } ?: return
        forbidUnless(FamilyPermissions.canRemove(actor(target.familyId), target.toMember()))
        members.remove(target)
    }

    override suspend fun setRole(memberId: String, role: FamilyRole) {
        check()
        val target = members.first { it.id == memberId }
        forbidUnless(
            role != FamilyRole.OWNER &&
                FamilyPermissions.canChangeRole(actor(target.familyId), target.toMember())
        )
        target.role = role
    }

    override suspend fun leaveFamily(familyId: String) {
        check()
        forbidUnless(FamilyPermissions.canLeave(actor(familyId)))
        members.removeAll { it.familyId == familyId && it.email == selfEmail }
    }

    override suspend fun pendingInvites(): List<FamilyInvite> {
        check()
        return members
            .filter { it.email == selfEmail && it.status == FamilyMemberStatus.PENDING }
            .map { member ->
                FamilyInvite(
                    memberId = member.id,
                    familyName = families.first { it.id == member.familyId }.name,
                    role = member.role,
                )
            }
    }

    override suspend fun acceptInvite(memberId: String) {
        check()
        pendingSelfInvite(memberId).status = FamilyMemberStatus.ACCEPTED
    }

    override suspend fun declineInvite(memberId: String) {
        check()
        pendingSelfInvite(memberId).status = FamilyMemberStatus.REJECTED
    }

    private fun pendingSelfInvite(memberId: String): StoredMember = members.first {
        it.id == memberId && it.email == selfEmail && it.status == FamilyMemberStatus.PENDING
    }

    private fun roleOf(familyId: String): FamilyRole? =
        members
            .firstOrNull {
                it.familyId == familyId &&
                    it.email == selfEmail &&
                    it.status == FamilyMemberStatus.ACCEPTED
            }
            ?.role

    private fun actor(familyId: String): FamilyRole =
        roleOf(familyId) ?: throw IllegalStateException("Not a member of $familyId")

    private fun forbidUnless(allowed: Boolean) {
        if (!allowed) throw IllegalStateException("Forbidden")
    }

    private fun check() {
        failure?.let { throw it }
    }

    private fun StoredMember.toMember(): FamilyMember =
        FamilyMember(
            id = id,
            email = email,
            role = role,
            status = status,
            name = name,
            isSelf = email == selfEmail,
        )
}
