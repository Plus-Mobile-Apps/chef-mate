package com.plusmobileapps.chefmate.family.data.impl.remote

/** Thin wrapper over the Supabase family RPCs. Roles are passed as their wire values. */
interface FamilyRemoteDataSource {
    suspend fun fetchFamilies(): List<RemoteFamily>

    suspend fun fetchMembers(familyId: String): List<RemoteFamilyMember>

    suspend fun createFamily(name: String): String

    suspend fun renameFamily(familyId: String, name: String)

    suspend fun deleteFamily(familyId: String)

    suspend fun invite(familyId: String, email: String, role: String)

    suspend fun removeMember(memberId: String)

    suspend fun setRole(memberId: String, role: String)

    suspend fun leaveFamily(familyId: String)

    suspend fun fetchPendingInvites(): List<RemoteFamilyInvite>

    suspend fun respondToInvite(memberId: String, accept: Boolean)
}
