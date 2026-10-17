package com.plusmobileapps.chefmate.recipebook.data.impl.remote

import com.plusmobileapps.chefmate.di.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class SupabaseFamilyRecipeBookRemoteDataSource(private val supabaseClient: SupabaseClient) :
    FamilyRecipeBookRemoteDataSource {

    override suspend fun fetchFamilyNames(familyIds: Collection<String>): List<RemoteFamilyName> {
        if (familyIds.isEmpty()) return emptyList()
        return supabaseClient
            .from("families")
            .select(Columns.list("id", "name")) { filter { isIn("id", familyIds.toList()) } }
            .decodeList<RemoteFamilyName>()
    }

    override suspend fun fetchFamilyRecipeBooks(familyId: String): List<RemoteFamilyRecipeBook> =
        supabaseClient.postgrest
            .rpc("family_recipe_books", buildJsonObject { put("p_family_id", familyId) })
            .decodeList<RemoteFamilyRecipeBook>()

    override suspend fun createFamilyRecipeBook(familyId: String, name: String): String =
        supabaseClient.postgrest
            .rpc(
                "create_family_recipe_book",
                buildJsonObject {
                    put("p_family_id", familyId)
                    put("p_name", name)
                },
            )
            .decodeAs<JsonElement>()
            .jsonPrimitive
            .content

    override suspend fun renameFamilyRecipeBook(bookId: String, name: String) {
        supabaseClient.postgrest.rpc(
            "rename_family_recipe_book",
            buildJsonObject {
                put("p_book_id", bookId)
                put("p_name", name)
            },
        )
    }

    override suspend fun deleteFamilyRecipeBook(bookId: String) {
        supabaseClient.postgrest.rpc(
            "delete_family_recipe_book",
            buildJsonObject { put("p_book_id", bookId) },
        )
    }
}
