@file:Suppress("FunctionName")
@file:OptIn(ExperimentalCoroutinesApi::class)

package com.plusmobileapps.chefmate.family.data.impl

import com.plusmobileapps.chefmate.family.data.Family
import com.plusmobileapps.chefmate.family.data.FamilyInvite
import com.plusmobileapps.chefmate.family.data.FamilyMember
import com.plusmobileapps.chefmate.family.data.FamilyMemberStatus
import com.plusmobileapps.chefmate.family.data.FamilyRole
import com.plusmobileapps.chefmate.family.data.impl.remote.FamilyRemoteDataSource
import com.plusmobileapps.chefmate.family.data.impl.remote.RemoteFamily
import com.plusmobileapps.chefmate.family.data.impl.remote.RemoteFamilyInvite
import com.plusmobileapps.chefmate.family.data.impl.remote.RemoteFamilyMember
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest

class FamilyRepositoryImplTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val remote = RecordingFamilyRemote()
    private val repository = FamilyRepositoryImpl(ioContext = testDispatcher, remote = remote)

    @Test
    fun getFamilies_maps_wire_roles() =
        runTest(testDispatcher) {
            remote.families =
                listOf(
                    RemoteFamily(id = "f1", name = "Smiths", myRole = "owner", memberCount = 3),
                    RemoteFamily(id = "f2", name = "Does", myRole = "admin", memberCount = 2),
                )

            repository.getFamilies() shouldBe
                listOf(
                    Family(id = "f1", name = "Smiths", myRole = FamilyRole.OWNER, memberCount = 3),
                    Family(id = "f2", name = "Does", myRole = FamilyRole.ADMIN, memberCount = 2),
                )
            repository.getFamily("f2")?.name shouldBe "Does"
            repository.getFamily("missing") shouldBe null
        }

    @Test
    fun getMembers_maps_roles_statuses_and_self() =
        runTest(testDispatcher) {
            remote.members =
                listOf(
                    RemoteFamilyMember(
                        memberId = "m1",
                        email = "me@example.com",
                        name = "Me",
                        role = "owner",
                        status = "accepted",
                        isSelf = true,
                    ),
                    RemoteFamilyMember(
                        memberId = "m2",
                        email = "kid@example.com",
                        role = "member",
                        status = "rejected",
                    ),
                    RemoteFamilyMember(
                        memberId = "m3",
                        email = "aunt@example.com",
                        role = "admin",
                        status = "pending",
                    ),
                )

            repository.getMembers("f1") shouldBe
                listOf(
                    FamilyMember(
                        id = "m1",
                        email = "me@example.com",
                        role = FamilyRole.OWNER,
                        status = FamilyMemberStatus.ACCEPTED,
                        name = "Me",
                        isSelf = true,
                    ),
                    FamilyMember(
                        id = "m2",
                        email = "kid@example.com",
                        role = FamilyRole.MEMBER,
                        status = FamilyMemberStatus.REJECTED,
                    ),
                    FamilyMember(
                        id = "m3",
                        email = "aunt@example.com",
                        role = FamilyRole.ADMIN,
                        status = FamilyMemberStatus.PENDING,
                    ),
                )
            remote.fetchedMembersFor shouldBe "f1"
        }

    @Test
    fun invite_normalizes_the_email_and_sends_the_wire_role() =
        runTest(testDispatcher) {
            repository.invite("f1", "  Kid@Example.COM ", FamilyRole.ADMIN)

            remote.invites shouldBe listOf(Triple("f1", "kid@example.com", "admin"))
        }

    @Test
    fun invite_and_setRole_reject_the_owner_role() =
        runTest(testDispatcher) {
            shouldThrow<IllegalArgumentException> {
                repository.invite("f1", "a@example.com", FamilyRole.OWNER)
            }
            shouldThrow<IllegalArgumentException> { repository.setRole("m1", FamilyRole.OWNER) }
            remote.invites shouldBe emptyList()
            remote.roleChanges shouldBe emptyList()
        }

    @Test
    fun create_trims_the_name_and_returns_the_new_id() =
        runTest(testDispatcher) {
            repository.createFamily("  Smiths ") shouldBe "new-family"
            remote.created shouldBe listOf("Smiths")
        }

    @Test
    fun accept_and_decline_respond_to_the_invite() =
        runTest(testDispatcher) {
            repository.acceptInvite("m1")
            repository.declineInvite("m2")

            remote.responses shouldBe listOf("m1" to true, "m2" to false)
        }

    @Test
    fun pendingInvites_maps_the_family_name_and_role() =
        runTest(testDispatcher) {
            remote.invitesForMe =
                listOf(
                    RemoteFamilyInvite(
                        memberId = "m9",
                        familyId = "f9",
                        familyName = "Garcias",
                        role = "admin",
                    )
                )

            repository.pendingInvites() shouldBe
                listOf(FamilyInvite(memberId = "m9", familyName = "Garcias", FamilyRole.ADMIN))
        }

    @Test
    fun remote_failures_propagate() =
        runTest(testDispatcher) {
            remote.failure = IllegalStateException("offline")

            shouldThrow<IllegalStateException> { repository.getFamilies() }
            shouldThrow<IllegalStateException> { repository.removeMember("m1") }
        }
}

private class RecordingFamilyRemote : FamilyRemoteDataSource {
    var failure: Throwable? = null
    var families: List<RemoteFamily> = emptyList()
    var members: List<RemoteFamilyMember> = emptyList()
    var invitesForMe: List<RemoteFamilyInvite> = emptyList()
    var fetchedMembersFor: String? = null
    val created = mutableListOf<String>()
    val invites = mutableListOf<Triple<String, String, String>>()
    val roleChanges = mutableListOf<Pair<String, String>>()
    val responses = mutableListOf<Pair<String, Boolean>>()
    val removed = mutableListOf<String>()

    private fun check() {
        failure?.let { throw it }
    }

    override suspend fun fetchFamilies(): List<RemoteFamily> = families.also { check() }

    override suspend fun fetchMembers(familyId: String): List<RemoteFamilyMember> {
        check()
        fetchedMembersFor = familyId
        return members
    }

    override suspend fun createFamily(name: String): String {
        check()
        created += name
        return "new-family"
    }

    override suspend fun renameFamily(familyId: String, name: String) = check()

    override suspend fun deleteFamily(familyId: String) = check()

    override suspend fun invite(familyId: String, email: String, role: String) {
        check()
        invites += Triple(familyId, email, role)
    }

    override suspend fun removeMember(memberId: String) {
        check()
        removed += memberId
    }

    override suspend fun setRole(memberId: String, role: String) {
        check()
        roleChanges += memberId to role
    }

    override suspend fun leaveFamily(familyId: String) = check()

    override suspend fun fetchPendingInvites(): List<RemoteFamilyInvite> = invitesForMe.also {
        check()
    }

    override suspend fun respondToInvite(memberId: String, accept: Boolean) {
        check()
        responses += memberId to accept
    }
}
