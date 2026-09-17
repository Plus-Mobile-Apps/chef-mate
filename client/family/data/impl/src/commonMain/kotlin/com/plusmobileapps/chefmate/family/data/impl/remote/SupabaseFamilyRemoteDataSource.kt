package com.plusmobileapps.chefmate.family.data.impl.remote

import com.plusmobileapps.chefmate.di.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/**
 * Every family read and write goes through a SECURITY DEFINER RPC (see
 * `supabase/migrations/20260916_add_families.sql`), which enforces the permission rules
 * server-side.
 */
@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class SupabaseFamilyRemoteDataSource(private val supabaseClient: SupabaseClient) :
    FamilyRemoteDataSource {

    override suspend fun fetchFamilies(): List<RemoteFamily> =
        supabaseClient.postgrest.rpc("my_families").decodeList<RemoteFamily>()

    override suspend fun fetchMembers(familyId: String): List<RemoteFamilyMember> =
        supabaseClient.postgrest
            .rpc("family_member_list", buildJsonObject { put("p_family_id", familyId) })
            .decodeList<RemoteFamilyMember>()

    override suspend fun createFamily(name: String): String =
        supabaseClient.postgrest
            .rpc("create_family", buildJsonObject { put("p_name", name) })
            .decodeAs<JsonElement>()
            .jsonPrimitive
            .content

    override suspend fun renameFamily(familyId: String, name: String) {
        call(
            "rename_family",
            buildJsonObject {
                put("p_family_id", familyId)
                put("p_name", name)
            },
        )
    }

    override suspend fun deleteFamily(familyId: String) {
        call("delete_family", buildJsonObject { put("p_family_id", familyId) })
    }

    override suspend fun invite(familyId: String, email: String, role: String) {
        call(
            "invite_family_member",
            buildJsonObject {
                put("p_family_id", familyId)
                put("p_email", email)
                put("p_role", role)
            },
        )
    }

    override suspend fun removeMember(memberId: String) {
        call("remove_family_member", buildJsonObject { put("p_member_id", memberId) })
    }

    override suspend fun setRole(memberId: String, role: String) {
        call(
            "set_family_member_role",
            buildJsonObject {
                put("p_member_id", memberId)
                put("p_role", role)
            },
        )
    }

    override suspend fun leaveFamily(familyId: String) {
        call("leave_family", buildJsonObject { put("p_family_id", familyId) })
    }

    override suspend fun fetchPendingInvites(): List<RemoteFamilyInvite> =
        supabaseClient.postgrest.rpc("my_pending_family_invites").decodeList<RemoteFamilyInvite>()

    override suspend fun respondToInvite(memberId: String, accept: Boolean) {
        call(
            "respond_family_invite",
            buildJsonObject {
                put("p_member_id", memberId)
                put("p_accept", accept)
            },
        )
    }

    private suspend fun call(function: String, params: JsonObject) {
        supabaseClient.postgrest.rpc(function, params)
    }
}
