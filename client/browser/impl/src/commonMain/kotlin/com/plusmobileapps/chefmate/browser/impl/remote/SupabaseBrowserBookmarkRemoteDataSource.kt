package com.plusmobileapps.chefmate.browser.impl.remote

import com.plusmobileapps.chefmate.di.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from

@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class SupabaseBrowserBookmarkRemoteDataSource(private val supabaseClient: SupabaseClient) :
    BrowserBookmarkRemoteDataSource {

    override suspend fun upsertBookmark(bookmark: RemoteBrowserBookmark): RemoteBrowserBookmark =
        supabaseClient
            .from(TABLE)
            .upsert(bookmark) {
                select()
                if (bookmark.id == null && bookmark.clientId != null) {
                    onConflict = "client_id"
                }
            }
            .decodeSingle<RemoteBrowserBookmark>()

    override suspend fun deleteBookmark(remoteId: String) {
        supabaseClient.from(TABLE).delete { filter { eq("id", remoteId) } }
    }

    override suspend fun fetchAllBookmarks(ownerId: String): List<RemoteBrowserBookmark> =
        supabaseClient
            .from(TABLE)
            .select { filter { eq("owner_id", ownerId) } }
            .decodeList<RemoteBrowserBookmark>()

    private companion object {
        const val TABLE = "browser_bookmarks"
    }
}
