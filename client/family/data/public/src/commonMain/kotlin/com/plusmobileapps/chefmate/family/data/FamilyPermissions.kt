package com.plusmobileapps.chefmate.family.data

/**
 * The family permission rules, mirrored from the server RPCs so the UI only offers actions that
 * will succeed. The server remains the authority.
 *
 * - Owner: invites (as admin or member), changes roles, removes anyone but themselves, renames and
 *   deletes the family. Can't leave and can't be removed.
 * - Admin: invites members, removes members, renames the family, leaves.
 * - Member: views and leaves.
 */
object FamilyPermissions {

    fun canInvite(actor: FamilyRole): Boolean = actor != FamilyRole.MEMBER

    /** Roles [actor] may assign to a new invite; empty when they can't invite at all. */
    fun invitableRoles(actor: FamilyRole): List<FamilyRole> =
        when (actor) {
            FamilyRole.OWNER -> listOf(FamilyRole.MEMBER, FamilyRole.ADMIN)
            FamilyRole.ADMIN -> listOf(FamilyRole.MEMBER)
            FamilyRole.MEMBER -> emptyList()
        }

    /**
     * Whether [actor] may remove [target] (or cancel their invite). Leaving yourself is separate.
     */
    fun canRemove(actor: FamilyRole, target: FamilyMember): Boolean =
        when {
            target.isSelf -> false
            target.role == FamilyRole.OWNER -> false
            target.role == FamilyRole.ADMIN -> actor == FamilyRole.OWNER
            else -> actor != FamilyRole.MEMBER
        }

    /** Whether [actor] may promote or demote [target]. Only the owner changes roles. */
    fun canChangeRole(actor: FamilyRole, target: FamilyMember): Boolean =
        actor == FamilyRole.OWNER && target.role != FamilyRole.OWNER && !target.isSelf

    fun canPromote(actor: FamilyRole, target: FamilyMember): Boolean =
        canChangeRole(actor, target) && target.role == FamilyRole.MEMBER

    fun canDemote(actor: FamilyRole, target: FamilyMember): Boolean =
        canChangeRole(actor, target) && target.role == FamilyRole.ADMIN

    fun canRename(actor: FamilyRole): Boolean = actor != FamilyRole.MEMBER

    fun canDelete(actor: FamilyRole): Boolean = actor == FamilyRole.OWNER

    fun canLeave(actor: FamilyRole): Boolean = actor != FamilyRole.OWNER
}
