@file:OptIn(ExperimentalSerializationApi::class)

package com.plusmobileapps.chefmate.grocery.data.remote

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RemoteGroceryItem(
    val id: String? = null,
    @SerialName("list_id") val listId: String,
    val name: String,
    @SerialName("is_checked") val isChecked: Boolean = false,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("client_id") val clientId: String? = null,
    @SerialName("recipe_name") val recipeName: String? = null,
    val aisle: String? = null,
)

@Serializable
data class RemoteGroceryList(
    val id: String? = null,
    val name: String = "My Grocery List",
    @SerialName("owner_id") val ownerId: String,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
    /**
     * The family the list belongs to, or null for a personal list. Never sent on insert/update —
     * family lists are created and changed only through the family RPCs, and omitting the key keeps
     * personal-list writes working against a schema without the column.
     */
    @EncodeDefault(EncodeDefault.Mode.NEVER) @SerialName("family_id") val familyId: String? = null,
)

/** A row of the `family_grocery_lists` RPC. */
@Serializable
data class RemoteFamilyGroceryList(
    val id: String,
    val name: String,
    @SerialName("item_count") val itemCount: Int = 0,
)

/** A family's id and name, used to label family lists. */
@Serializable data class RemoteFamilyName(val id: String, val name: String)

@Serializable data class RemoteGroceryListName(val id: String, val name: String)
