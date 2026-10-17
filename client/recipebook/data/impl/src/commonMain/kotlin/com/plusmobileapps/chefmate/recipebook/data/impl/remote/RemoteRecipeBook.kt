@file:OptIn(ExperimentalSerializationApi::class)

package com.plusmobileapps.chefmate.recipebook.data.impl.remote

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RemoteRecipeBook(
    val id: String? = null,
    @SerialName("owner_id") val ownerId: String,
    val name: String,
    @SerialName("is_default") val isDefault: Boolean = false,
    @SerialName("client_id") val clientId: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
    /**
     * The family the book belongs to, or null for a personal book. Never sent on insert/update —
     * family books are created and changed only through the family RPCs, and omitting the key keeps
     * personal-book writes working against a schema without the column.
     */
    @EncodeDefault(EncodeDefault.Mode.NEVER) @SerialName("family_id") val familyId: String? = null,
)

/** A row of the `family_recipe_books` RPC. */
@Serializable
data class RemoteFamilyRecipeBook(
    val id: String,
    val name: String,
    @SerialName("recipe_count") val recipeCount: Int = 0,
)

/** A family's id and name, used to label family books. */
@Serializable data class RemoteFamilyName(val id: String, val name: String)
