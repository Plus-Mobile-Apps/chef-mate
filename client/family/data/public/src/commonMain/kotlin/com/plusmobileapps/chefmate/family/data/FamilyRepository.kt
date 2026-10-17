package com.plusmobileapps.chefmate.family.data

/**
 * Families the current user belongs to and their members. These are online operations backed by
 * Supabase — nothing is cached locally — and every method throws on network or permission failure
 * so callers can surface an error. Permission rules live in [FamilyPermissions] and are enforced
 * server-side.
 */
interface FamilyRepository {
    /** Families the current user has joined, sorted by name. */
    suspend fun getFamilies(): List<Family>

    /** The family with [familyId], or null when the user isn't a member (anymore). */
    suspend fun getFamily(familyId: String): Family?

    /** Every member and invite of the family, owner first, then admins, then members. */
    suspend fun getMembers(familyId: String): List<FamilyMember>

    /** Creates a family owned by the current user and returns its id. */
    suspend fun createFamily(name: String): String

    suspend fun renameFamily(familyId: String, name: String)

    /** Deletes the family for everyone. Owner only. */
    suspend fun deleteFamily(familyId: String)

    /** Invites [email] to the family as [role] ([FamilyRole.ADMIN] is owner only). */
    suspend fun invite(familyId: String, email: String, role: FamilyRole)

    /** Removes the member / cancels the invite with [memberId]. */
    suspend fun removeMember(memberId: String)

    /** Promotes or demotes the member with [memberId]. Owner only. */
    suspend fun setRole(memberId: String, role: FamilyRole)

    /** Removes the current user from the family. Not allowed for the owner. */
    suspend fun leaveFamily(familyId: String)

    /** Pending family invites addressed to the current user. */
    suspend fun pendingInvites(): List<FamilyInvite>

    suspend fun acceptInvite(memberId: String)

    /** Declines the invite; the row is kept as rejected so admins see the outcome. */
    suspend fun declineInvite(memberId: String)
}
