package com.plusmobileapps.chefmate.grocery.data.impl.remote

import com.plusmobileapps.chefmate.di.AppScope
import com.plusmobileapps.chefmate.grocery.data.remote.GroceryRemoteDataSource
import com.plusmobileapps.chefmate.grocery.data.remote.RemoteFamilyGroceryList
import com.plusmobileapps.chefmate.grocery.data.remote.RemoteFamilyName
import com.plusmobileapps.chefmate.grocery.data.remote.RemoteGroceryItem
import com.plusmobileapps.chefmate.grocery.data.remote.RemoteGroceryList
import com.plusmobileapps.chefmate.grocery.data.remote.RemoteGroceryListCollaborator
import com.plusmobileapps.chefmate.grocery.data.remote.RemoteGroceryListInvite
import com.plusmobileapps.chefmate.grocery.data.remote.RemoteGroceryListMember
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class SupabaseGroceryRemoteDataSource(private val supabaseClient: SupabaseClient) :
    GroceryRemoteDataSource {

    override fun observeChanges(): Flow<Unit> {
        val channel = supabaseClient.channel(REALTIME_CHANNEL)
        // Register a Postgres-change binding per table before subscribing — the SDK requires
        // bindings to exist when the channel joins. Row-level security scopes each stream to the
        // lists this user can access, so we simply re-reconcile on any emission.
        val tableChanges = REALTIME_TABLES.map { table ->
            channel.postgresChangeFlow<PostgresAction>(schema = "public") { this.table = table }
        }
        return merge(*tableChanges.toTypedArray())
            .map {}
            .onStart { channel.subscribe() }
            .onCompletion {
                // Runs on cancellation too (e.g. sign-out). Force the leave through even though the
                // collecting coroutine is being cancelled, so the server-side channel is released
                // before a same-named channel is recreated on the next sign-in.
                withContext(NonCancellable) { supabaseClient.realtime.removeChannel(channel) }
            }
    }

    override suspend fun ensureDefaultList(ownerId: String): String {
        val existing =
            supabaseClient
                .from("grocery_lists")
                .select {
                    filter {
                        eq("owner_id", ownerId)
                        // A family list the user created isn't their personal default.
                        exact("family_id", null)
                    }
                    limit(1)
                }
                .decodeList<RemoteGroceryList>()

        if (existing.isNotEmpty()) {
            return existing.first().id!!
        }

        val created =
            supabaseClient
                .from("grocery_lists")
                .insert(RemoteGroceryList(ownerId = ownerId)) { select() }
                .decodeSingle<RemoteGroceryList>()

        return created.id!!
    }

    override suspend fun upsertGroceryItem(item: RemoteGroceryItem): RemoteGroceryItem =
        supabaseClient
            .from("grocery_items")
            .upsert(item) {
                select()
                if (item.id == null && item.clientId != null) {
                    onConflict = "client_id"
                }
            }
            .decodeSingle<RemoteGroceryItem>()

    override suspend fun updateGroceryItem(item: RemoteGroceryItem): RemoteGroceryItem {
        val remoteId = requireNotNull(item.id) { "updateGroceryItem requires a remote id" }
        // Send only the mutable columns. Building the payload explicitly (rather than passing the
        // whole RemoteGroceryItem) keeps immutable columns like created_at out of the UPDATE, so an
        // edit can't fail a NOT NULL constraint on the conflict-update path.
        val payload = buildJsonObject {
            put("name", item.name)
            put("is_checked", item.isChecked)
            item.updatedAt?.let { put("updated_at", it) }
            put("aisle", item.aisle)
            put("recipe_name", item.recipeName)
        }
        return supabaseClient
            .from("grocery_items")
            .update(payload) {
                select()
                filter { eq("id", remoteId) }
            }
            .decodeSingle<RemoteGroceryItem>()
    }

    override suspend fun deleteGroceryItem(remoteId: String) {
        supabaseClient.from("grocery_items").delete { filter { eq("id", remoteId) } }
    }

    override suspend fun fetchAllGroceryItems(listId: String): List<RemoteGroceryItem> =
        supabaseClient
            .from("grocery_items")
            .select { filter { eq("list_id", listId) } }
            .decodeList<RemoteGroceryItem>()

    override suspend fun createGroceryList(list: RemoteGroceryList): RemoteGroceryList =
        supabaseClient
            .from("grocery_lists")
            .insert(list) { select() }
            .decodeSingle<RemoteGroceryList>()

    override suspend fun fetchGroceryLists(ownerId: String): List<RemoteGroceryList> =
        supabaseClient
            .from("grocery_lists")
            .select { filter { eq("owner_id", ownerId) } }
            .decodeList<RemoteGroceryList>()

    override suspend fun deleteGroceryList(remoteId: String) {
        supabaseClient.from("grocery_lists").delete { filter { eq("id", remoteId) } }
    }

    override suspend fun updateGroceryList(list: RemoteGroceryList): RemoteGroceryList =
        supabaseClient
            .from("grocery_lists")
            .update(list) {
                select()
                filter { eq("id", list.id!!) }
            }
            .decodeSingle<RemoteGroceryList>()

    override suspend fun fetchAccessibleGroceryLists(): List<RemoteGroceryList> =
        supabaseClient.from("grocery_lists").select().decodeList<RemoteGroceryList>()

    override suspend fun fetchListMembers(listId: String): List<RemoteGroceryListMember> =
        supabaseClient
            .from("grocery_list_members")
            .select { filter { eq("list_id", listId) } }
            .decodeList<RemoteGroceryListMember>()

    override suspend fun fetchListCollaborators(
        listId: String
    ): List<RemoteGroceryListCollaborator> =
        supabaseClient.postgrest
            .rpc("grocery_list_collaborators", buildJsonObject { put("p_list_id", listId) })
            .decodeList<RemoteGroceryListCollaborator>()

    override suspend fun inviteToList(member: RemoteGroceryListMember): RemoteGroceryListMember =
        supabaseClient
            .from("grocery_list_members")
            .insert(member) { select() }
            .decodeSingle<RemoteGroceryListMember>()

    override suspend fun fetchPendingInvitations(email: String): List<RemoteGroceryListInvite> =
        supabaseClient.postgrest
            .rpc("grocery_list_pending_invites")
            .decodeList<RemoteGroceryListInvite>()

    override suspend fun respondToInvitation(memberId: String, userId: String, accept: Boolean) {
        val payload =
            if (accept) {
                JsonObject(
                    mapOf("user_id" to JsonPrimitive(userId), "status" to JsonPrimitive("accepted"))
                )
            } else {
                JsonObject(mapOf("status" to JsonPrimitive("rejected")))
            }
        supabaseClient.from("grocery_list_members").update(payload) {
            filter { eq("id", memberId) }
        }
    }

    override suspend fun removeFromList(memberId: String) {
        supabaseClient.from("grocery_list_members").delete { filter { eq("id", memberId) } }
    }

    override suspend fun fetchFamilyNames(familyIds: Collection<String>): List<RemoteFamilyName> {
        if (familyIds.isEmpty()) return emptyList()
        return supabaseClient
            .from("families")
            .select(Columns.list("id", "name")) { filter { isIn("id", familyIds.toList()) } }
            .decodeList<RemoteFamilyName>()
    }

    override suspend fun fetchFamilyGroceryLists(familyId: String): List<RemoteFamilyGroceryList> =
        supabaseClient.postgrest
            .rpc("family_grocery_lists", buildJsonObject { put("p_family_id", familyId) })
            .decodeList<RemoteFamilyGroceryList>()

    override suspend fun createFamilyGroceryList(familyId: String, name: String): String =
        supabaseClient.postgrest
            .rpc(
                "create_family_grocery_list",
                buildJsonObject {
                    put("p_family_id", familyId)
                    put("p_name", name)
                },
            )
            .decodeAs<JsonElement>()
            .jsonPrimitive
            .content

    override suspend fun renameFamilyGroceryList(listId: String, name: String) {
        supabaseClient.postgrest.rpc(
            "rename_family_grocery_list",
            buildJsonObject {
                put("p_list_id", listId)
                put("p_name", name)
            },
        )
    }

    override suspend fun deleteFamilyGroceryList(listId: String) {
        supabaseClient.postgrest.rpc(
            "delete_family_grocery_list",
            buildJsonObject { put("p_list_id", listId) },
        )
    }

    private companion object {
        const val REALTIME_CHANNEL = "grocery-sync"
        val REALTIME_TABLES = listOf("grocery_items", "grocery_lists", "grocery_list_members")
    }
}
