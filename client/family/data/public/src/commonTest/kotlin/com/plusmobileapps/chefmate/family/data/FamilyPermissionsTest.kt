@file:Suppress("FunctionName")

package com.plusmobileapps.chefmate.family.data

import com.plusmobileapps.chefmate.family.data.FamilyRole.ADMIN
import com.plusmobileapps.chefmate.family.data.FamilyRole.MEMBER
import com.plusmobileapps.chefmate.family.data.FamilyRole.OWNER
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class FamilyPermissionsTest {

    private fun member(role: FamilyRole, isSelf: Boolean = false) =
        FamilyMember(
            id = "m-$role",
            email = "${role.wireValue}@example.com",
            role = role,
            status = FamilyMemberStatus.ACCEPTED,
            isSelf = isSelf,
        )

    @Test
    fun only_owners_and_admins_can_invite() {
        FamilyPermissions.canInvite(OWNER) shouldBe true
        FamilyPermissions.canInvite(ADMIN) shouldBe true
        FamilyPermissions.canInvite(MEMBER) shouldBe false
    }

    @Test
    fun only_the_owner_can_invite_admins() {
        FamilyPermissions.invitableRoles(OWNER) shouldBe listOf(MEMBER, ADMIN)
        FamilyPermissions.invitableRoles(ADMIN) shouldBe listOf(MEMBER)
        FamilyPermissions.invitableRoles(MEMBER) shouldBe emptyList()
    }

    @Test
    fun the_owner_can_never_be_removed() {
        FamilyRole.entries.forEach { actor ->
            FamilyPermissions.canRemove(actor, member(OWNER)) shouldBe false
        }
    }

    @Test
    fun only_the_owner_can_remove_an_admin() {
        FamilyPermissions.canRemove(OWNER, member(ADMIN)) shouldBe true
        FamilyPermissions.canRemove(ADMIN, member(ADMIN)) shouldBe false
        FamilyPermissions.canRemove(MEMBER, member(ADMIN)) shouldBe false
    }

    @Test
    fun owners_and_admins_can_remove_members() {
        FamilyPermissions.canRemove(OWNER, member(MEMBER)) shouldBe true
        FamilyPermissions.canRemove(ADMIN, member(MEMBER)) shouldBe true
        FamilyPermissions.canRemove(MEMBER, member(MEMBER)) shouldBe false
    }

    @Test
    fun nobody_removes_themselves_they_leave_instead() {
        FamilyPermissions.canRemove(ADMIN, member(ADMIN, isSelf = true)) shouldBe false
        FamilyPermissions.canLeave(OWNER) shouldBe false
        FamilyPermissions.canLeave(ADMIN) shouldBe true
        FamilyPermissions.canLeave(MEMBER) shouldBe true
    }

    @Test
    fun only_the_owner_promotes_and_demotes() {
        FamilyPermissions.canPromote(OWNER, member(MEMBER)) shouldBe true
        FamilyPermissions.canDemote(OWNER, member(ADMIN)) shouldBe true
        FamilyPermissions.canPromote(OWNER, member(ADMIN)) shouldBe false
        FamilyPermissions.canDemote(OWNER, member(MEMBER)) shouldBe false
        FamilyPermissions.canChangeRole(OWNER, member(OWNER)) shouldBe false
        FamilyPermissions.canPromote(ADMIN, member(MEMBER)) shouldBe false
        FamilyPermissions.canDemote(ADMIN, member(ADMIN)) shouldBe false
    }

    @Test
    fun owners_and_admins_rename_only_the_owner_deletes() {
        FamilyPermissions.canRename(OWNER) shouldBe true
        FamilyPermissions.canRename(ADMIN) shouldBe true
        FamilyPermissions.canRename(MEMBER) shouldBe false
        FamilyPermissions.canDelete(OWNER) shouldBe true
        FamilyPermissions.canDelete(ADMIN) shouldBe false
        FamilyPermissions.canDelete(MEMBER) shouldBe false
    }
}
