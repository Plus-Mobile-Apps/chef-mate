package com.plusmobileapps.chefmate

import co.touchlab.kermit.Logger
import com.plusmobileapps.chefmate.di.AppScope
import com.plusmobileapps.chefmate.di.IO
import com.plusmobileapps.chefmate.grocery.data.GroceryAutocompleteRepository
import com.plusmobileapps.chefmate.grocery.data.GroceryItem
import com.plusmobileapps.chefmate.grocery.data.GroceryListModel
import com.plusmobileapps.chefmate.grocery.data.GroceryRepository
import com.plusmobileapps.chefmate.grocery.data.IngredientParser
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Phone side of the watch companion. The phone is the source of truth: the watch holds no
 * Kotlin/Supabase, so this bridges the shared [GroceryRepository] to the watch over
 * WatchConnectivity. It emits a JSON snapshot of all lists + items (which the Swift
 * `WatchDataSender` pushes to the watch), and applies the watch's toggle/add actions (which sync to
 * Supabase).
 *
 * Everything is exposed as plain primitives / JSON so the Swift glue never touches Kotlin models.
 */
@Inject
@SingleIn(AppScope::class)
class WatchDataBridge(
    private val repository: GroceryRepository,
    private val autocompleteRepository: GroceryAutocompleteRepository,
    @IO private val ioContext: CoroutineContext,
) {
    private val scope = CoroutineScope(SupervisorJob() + ioContext)
    private val json = Json { encodeDefaults = true }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val snapshotFlow: Flow<String> =
        repository.getGroceryLists().flatMapLatest { lists ->
            if (lists.isEmpty()) {
                flowOf(encode(emptyList()))
            } else {
                combine(lists.map { list -> repository.getGroceries(list.id).map { list to it } }) {
                    encode(it.toList())
                }
            }
        }

    /** Streams a JSON snapshot whenever the grocery data changes. Returns a cancel function. */
    fun observeSnapshot(onSnapshot: (String) -> Unit): () -> Unit {
        val job = scope.launch { snapshotFlow.collect { onSnapshot(it) } }
        return { job.cancel() }
    }

    /** Delivers the current JSON snapshot once (for answering a watch pull request). */
    fun currentSnapshot(onResult: (String) -> Unit) {
        scope.launch { onResult(snapshotFlow.first()) }
    }

    /**
     * Adds [name] to [listId] and pushes it to Supabase, invoking [onComplete] once the write has
     * landed.
     *
     * [onComplete] is what lets the Swift caller hold the app alive while this runs. iOS wakes the
     * phone app in the background purely to hand `WatchDataSender` a queued watch mutation and
     * suspends it again the moment the delegate callback returns — so a fire-and-forget [launch]
     * here is killed before it reaches the database, and the phone's next snapshot push then
     * overwrites the watch's optimistic edit. See `WatchDataSender.keepingAlive`.
     */
    fun addItem(listId: Long, name: String, onComplete: () -> Unit) {
        scope.launch {
            try {
                repository.addGrocery(listId, name)
                repository.syncAllUnsynced()
            } finally {
                onComplete()
            }
        }
    }

    /**
     * Toggles an item. [onComplete] behaves as in [addItem].
     *
     * Deliberately does *not* call `syncAllUnsynced()`: [GroceryRepository.updateChecked] already
     * marks the row dirty and pushes it, and kicking off a full reconcile alongside that push races
     * it — the reconcile can pull a pre-toggle copy of the row and write it back over the check
     * once the push clears the dirty flag. The dirty row self-heals on the next reconcile either
     * way.
     */
    fun setChecked(itemId: Long, isChecked: Boolean, onComplete: () -> Unit) {
        scope.launch {
            try {
                val item = repository.getGrocery(itemId)
                if (item == null) {
                    // The watch is holding ids from a snapshot that no longer matches the
                    // database (a remote reconcile can delete and recreate rows). Dropping this
                    // silently is what makes a watch toggle look like it never happened.
                    Logger.w(tag = TAG) { "setChecked: no local grocery with id=$itemId" }
                } else {
                    repository.updateChecked(item, isChecked)
                }
            } finally {
                onComplete()
            }
        }
    }

    /** Local id of the default list, creating one if needed (used by the Siri "add" intent). */
    fun ensureDefaultList(onResult: (Long) -> Unit) {
        scope.launch { onResult(repository.ensureDefaultList()) }
    }

    /**
     * Item names Siri should recognize inside an "Add <item> to Chef Mate" phrase, most personal
     * first: the user's saved autocomplete entries, then items already on their lists, then the
     * built-in vocabulary. Deduplicated case-insensitively and capped at [limit], since every name
     * multiplies into each parameterized App Shortcut phrase and Siri caps the total.
     */
    fun siriSuggestionNames(limit: Int, onResult: (List<String>) -> Unit) {
        scope.launch {
            val saved = autocompleteRepository.observeItems().first().map { it.name }
            val onLists = repository.getGroceries().first().map { it.displayName }
            val seen = mutableSetOf<String>()
            val names =
                (saved.asSequence() + onLists.asSequence() + IngredientParser.allSuggestionNames())
                    .map { it.trim() }
                    .filter { it.isNotEmpty() && seen.add(it.lowercase()) }
                    .take(limit)
                    .toList()
            onResult(names)
        }
    }

    private fun encode(listToItems: List<Pair<GroceryListModel, List<GroceryItem>>>): String {
        val lists = listToItems.map { (list, _) ->
            WatchListDto(list.id, list.name, list.isShared || list.isFamily)
        }
        val items = listToItems.flatMap { (list, items) ->
            items.map {
                WatchItemDto(
                    id = it.id,
                    listId = list.id,
                    name = it.displayName,
                    quantity = it.quantity,
                    category = it.category.name,
                    isChecked = it.isChecked,
                )
            }
        }
        return json.encodeToString(WatchSnapshot(lists, items))
    }

    private companion object {
        const val TAG = "WatchDataBridge"
    }
}

@Serializable
private data class WatchSnapshot(val lists: List<WatchListDto>, val items: List<WatchItemDto>)

@Serializable private data class WatchListDto(val id: Long, val name: String, val isShared: Boolean)

@Serializable
private data class WatchItemDto(
    val id: Long,
    val listId: Long,
    val name: String,
    val quantity: String?,
    val category: String,
    val isChecked: Boolean,
)
