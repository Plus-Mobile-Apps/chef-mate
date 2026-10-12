package com.plusmobileapps.chefmate.browser.impl.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RemoteBrowserBookmark(
    val id: String? = null,
    @SerialName("owner_id") val ownerId: String,
    val url: String,
    val title: String,
    @SerialName("client_id") val clientId: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
)
