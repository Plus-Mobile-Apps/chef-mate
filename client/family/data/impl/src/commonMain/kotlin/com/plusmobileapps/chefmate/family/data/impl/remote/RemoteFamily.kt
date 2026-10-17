package com.plusmobileapps.chefmate.family.data.impl.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** A row from the `my_families` RPC. */
@Serializable
data class RemoteFamily(
    val id: String,
    val name: String,
    @SerialName("my_role") val myRole: String,
    @SerialName("member_count") val memberCount: Int = 0,
)

/** A row from the `family_member_list` RPC. */
@Serializable
data class RemoteFamilyMember(
    @SerialName("member_id") val memberId: String,
    val email: String,
    val name: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    val role: String,
    val status: String,
    @SerialName("is_self") val isSelf: Boolean = false,
)

/** A row from the `my_pending_family_invites` RPC. */
@Serializable
data class RemoteFamilyInvite(
    @SerialName("member_id") val memberId: String,
    @SerialName("family_id") val familyId: String,
    @SerialName("family_name") val familyName: String,
    val role: String,
)
