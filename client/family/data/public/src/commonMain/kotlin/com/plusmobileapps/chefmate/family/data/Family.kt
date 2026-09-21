package com.plusmobileapps.chefmate.family.data

/** A family the current user belongs to. [id] is the remote (Supabase) id. */
data class Family(
    val id: String,
    val name: String,
    /** The current user's role in this family. */
    val myRole: FamilyRole,
    /** Accepted members, including the owner. */
    val memberCount: Int,
)

/** Invite lifecycle for a [FamilyMember]. */
enum class FamilyMemberStatus {
    PENDING,
    ACCEPTED,
    REJECTED;

    companion object {
        fun fromWire(value: String?): FamilyMemberStatus =
            when (value?.lowercase()) {
                "accepted" -> ACCEPTED
                "rejected" -> REJECTED
                else -> PENDING
            }
    }
}

/**
 * A row in a family's member list: the owner, admins, members, and outstanding or declined invites.
 * [id] is the remote member-row id.
 */
data class FamilyMember(
    val id: String,
    val email: String,
    val role: FamilyRole,
    val status: FamilyMemberStatus,
    /** Display name from the user's profile; null for invites without an account yet. */
    val name: String? = null,
    /** Profile photo URL when known; null falls back to a lettered avatar. */
    val avatarUrl: String? = null,
    /** True for the row belonging to the current user. */
    val isSelf: Boolean = false,
) {
    /** The name to show for this member, falling back to their email. */
    val displayName: String
        get() = name?.takeIf { it.isNotBlank() } ?: email
}

/** A pending invite to join a family, addressed to the current user. */
data class FamilyInvite(val memberId: String, val familyName: String, val role: FamilyRole)
