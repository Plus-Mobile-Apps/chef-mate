package com.plusmobileapps.chefmate.family.data.impl

import com.plusmobileapps.chefmate.di.AppScope
import com.plusmobileapps.chefmate.di.IO
import com.plusmobileapps.chefmate.family.data.Family
import com.plusmobileapps.chefmate.family.data.FamilyInvite
import com.plusmobileapps.chefmate.family.data.FamilyMember
import com.plusmobileapps.chefmate.family.data.FamilyMemberStatus
import com.plusmobileapps.chefmate.family.data.FamilyRepository
import com.plusmobileapps.chefmate.family.data.FamilyRole
import com.plusmobileapps.chefmate.family.data.impl.remote.FamilyRemoteDataSource
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.withContext

@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class FamilyRepositoryImpl(
    @IO private val ioContext: CoroutineContext,
    private val remote: FamilyRemoteDataSource,
) : FamilyRepository {

    override suspend fun getFamilies(): List<Family> =
        withContext(ioContext) {
            remote.fetchFamilies().map {
                Family(
                    id = it.id,
                    name = it.name,
                    myRole = FamilyRole.fromWire(it.myRole),
                    memberCount = it.memberCount,
                )
            }
        }

    override suspend fun getFamily(familyId: String): Family? =
        getFamilies().firstOrNull { it.id == familyId }

    override suspend fun getMembers(familyId: String): List<FamilyMember> =
        withContext(ioContext) {
            remote.fetchMembers(familyId).map {
                FamilyMember(
                    id = it.memberId,
                    email = it.email,
                    role = FamilyRole.fromWire(it.role),
                    status = FamilyMemberStatus.fromWire(it.status),
                    name = it.name,
                    avatarUrl = it.avatarUrl,
                    isSelf = it.isSelf,
                )
            }
        }

    override suspend fun createFamily(name: String): String =
        withContext(ioContext) { remote.createFamily(name.trim()) }

    override suspend fun renameFamily(familyId: String, name: String) =
        withContext(ioContext) { remote.renameFamily(familyId, name.trim()) }

    override suspend fun deleteFamily(familyId: String) =
        withContext(ioContext) { remote.deleteFamily(familyId) }

    override suspend fun invite(familyId: String, email: String, role: FamilyRole) {
        require(role != FamilyRole.OWNER) { "A family has exactly one owner" }
        withContext(ioContext) { remote.invite(familyId, email.trim().lowercase(), role.wireValue) }
    }

    override suspend fun removeMember(memberId: String) =
        withContext(ioContext) { remote.removeMember(memberId) }

    override suspend fun setRole(memberId: String, role: FamilyRole) {
        require(role != FamilyRole.OWNER) { "Ownership can't be assigned" }
        withContext(ioContext) { remote.setRole(memberId, role.wireValue) }
    }

    override suspend fun leaveFamily(familyId: String) =
        withContext(ioContext) { remote.leaveFamily(familyId) }

    override suspend fun pendingInvites(): List<FamilyInvite> =
        withContext(ioContext) {
            remote.fetchPendingInvites().map {
                FamilyInvite(
                    memberId = it.memberId,
                    familyName = it.familyName,
                    role = FamilyRole.fromWire(it.role),
                )
            }
        }

    override suspend fun acceptInvite(memberId: String) =
        withContext(ioContext) { remote.respondToInvite(memberId, accept = true) }

    override suspend fun declineInvite(memberId: String) =
        withContext(ioContext) { remote.respondToInvite(memberId, accept = false) }
}
