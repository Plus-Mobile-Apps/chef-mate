@file:Suppress("FunctionName")
@file:OptIn(ExperimentalCoroutinesApi::class, ExperimentalUuidApi::class)

package com.plusmobileapps.chefmate.grocery.data.impl

import app.cash.turbine.test
import com.plusmobileapps.chefmate.auth.data.AuthState
import com.plusmobileapps.chefmate.auth.data.ChefMateUser
import com.plusmobileapps.chefmate.auth.data.testing.FakeAuthenticationRepository
import com.plusmobileapps.chefmate.database.Database
import com.plusmobileapps.chefmate.database.testing.createTestDatabase
import com.plusmobileapps.chefmate.grocery.data.CollaborationStatus
import com.plusmobileapps.chefmate.grocery.data.GroceryCategory
import com.plusmobileapps.chefmate.grocery.data.remote.RemoteGroceryItem
import com.plusmobileapps.chefmate.grocery.data.remote.RemoteGroceryList
import com.plusmobileapps.chefmate.grocery.data.remote.RemoteGroceryListMember
import com.plusmobileapps.chefmate.grocery.data.testing.FakeGroceryCategoryOverrideRepository
import com.plusmobileapps.chefmate.grocery.data.testing.FakeGroceryRemoteDataSource
import com.plusmobileapps.chefmate.util.testing.FakeDateTimeUtil
import io.kotest.matchers.shouldBe
import kotlin.test.Test
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest

class GroceryRepositoryImplTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val db: Database = createTestDatabase()
    private val fakeAuth = FakeAuthenticationRepository()
    private val fakeRemote = FakeGroceryRemoteDataSource()
    private val dateTimeUtil = FakeDateTimeUtil()
    private val fakeOverrides = FakeGroceryCategoryOverrideRepository()

    private val repository =
        GroceryRepositoryImpl(
            queries = db.groceryQueries,
            listQueries = db.groceryListQueries,
            memberQueries = db.groceryListMemberQueries,
            ioContext = testDispatcher,
            dateTimeUtil = dateTimeUtil,
            remoteDataSource = fakeRemote,
            authRepository = fakeAuth,
            categoryOverrideRepository = fakeOverrides,
        )

    @Test
    fun syncWithRemote_links_local_default_list_to_remote_instead_of_creating_duplicate() =
        runTest(testDispatcher) {
            // Simulate: user opened grocery screen on new device, creating a local default list
            val localListId = repository.ensureDefaultList()
            localListId shouldBe 1L

            // Verify only 1 list exists locally
            repository.getGroceryLists().test { awaitItem().size shouldBe 1 }

            // Remote has the user's existing "My Grocery List" from their old device
            val remoteListId = "remote-list-123"
            fakeRemote.remoteLists["user-1"] =
                mutableListOf(
                    RemoteGroceryList(
                        id = remoteListId,
                        name = "My Grocery List",
                        ownerId = "user-1",
                    )
                )

            // User signs in, triggering sync
            fakeAuth.setState(
                AuthState.Authenticated(
                    ChefMateUser(
                        userId = "user-1",
                        userName = "Test",
                        userEmail = "test@test.com",
                        userProfileImageUrl = null,
                    )
                )
            )

            // Should still have exactly 1 list — the local list linked to the remote one
            repository.getGroceryLists().test {
                val lists = awaitItem()
                lists.size shouldBe 1
                lists.first().name shouldBe "My Grocery List"
            }

            // Verify the local list was linked to the remote one (has remoteId)
            val localList = db.groceryListQueries.getByRemoteId(remoteListId).executeAsOneOrNull()
            localList shouldBe db.groceryListQueries.getById(localListId).executeAsOne()
        }

    @Test
    fun syncWithRemote_preserves_local_items_when_linking_to_remote_list() =
        runTest(testDispatcher) {
            // User adds items to local default list before signing in
            repository.addGrocery("Apples")
            repository.addGrocery("Bananas")

            val remoteListId = "remote-list-456"
            fakeRemote.remoteLists["user-1"] =
                mutableListOf(
                    RemoteGroceryList(
                        id = remoteListId,
                        name = "My Grocery List",
                        ownerId = "user-1",
                    )
                )

            // Sign in
            fakeAuth.setState(
                AuthState.Authenticated(
                    ChefMateUser(
                        userId = "user-1",
                        userName = "Test",
                        userEmail = "test@test.com",
                        userProfileImageUrl = null,
                    )
                )
            )

            // Still 1 list, and local items are preserved
            repository.getGroceryLists().test { awaitItem().size shouldBe 1 }
            repository.getGroceries().test {
                val items = awaitItem()
                items.size shouldBe 2
            }
        }

    @Test
    fun syncWithRemote_creates_new_local_list_for_unmatched_remote_list() =
        runTest(testDispatcher) {
            // Local default list exists
            repository.ensureDefaultList()

            // Remote has a list with a different name
            fakeRemote.remoteLists["user-1"] =
                mutableListOf(
                    RemoteGroceryList(
                        id = "remote-other",
                        name = "Party Supplies",
                        ownerId = "user-1",
                    )
                )

            fakeAuth.setState(
                AuthState.Authenticated(
                    ChefMateUser(
                        userId = "user-1",
                        userName = "Test",
                        userEmail = "test@test.com",
                        userProfileImageUrl = null,
                    )
                )
            )

            // Should have 2 lists: the local default + the remote "Party Supplies"
            repository.getGroceryLists().test {
                val lists = awaitItem()
                lists.size shouldBe 2
                lists.map { it.name }.toSet() shouldBe setOf("My Grocery List", "Party Supplies")
            }
        }

    @Test
    fun syncWithRemote_pulls_remote_items_into_linked_list() =
        runTest(testDispatcher) {
            repository.ensureDefaultList()

            val remoteListId = "remote-list-789"
            fakeRemote.remoteLists["user-1"] =
                mutableListOf(
                    RemoteGroceryList(
                        id = remoteListId,
                        name = "My Grocery List",
                        ownerId = "user-1",
                    )
                )
            fakeRemote.remoteItems[remoteListId] =
                mutableListOf(
                    RemoteGroceryItem(
                        id = "item-1",
                        listId = remoteListId,
                        name = "Milk",
                        clientId = Uuid.random().toString(),
                        aisle = GroceryCategory.DAIRY.name,
                    ),
                    RemoteGroceryItem(
                        id = "item-2",
                        listId = remoteListId,
                        name = "Eggs",
                        clientId = Uuid.random().toString(),
                    ),
                )

            fakeAuth.setState(
                AuthState.Authenticated(
                    ChefMateUser(
                        userId = "user-1",
                        userName = "Test",
                        userEmail = "test@test.com",
                        userProfileImageUrl = null,
                    )
                )
            )

            // Remote items should be pulled into the linked list — and the manual aisle override
            // attached to "Milk" should round-trip even though the parser would also categorise
            // it as DAIRY; the test exercises the wire path explicitly via Eggs (no override,
            // parser fills in) and Milk (explicit override).
            repository.getGroceries().test {
                val items = awaitItem()
                items.size shouldBe 2
                items.map { it.name }.toSet() shouldBe setOf("Milk", "Eggs")
                val milk = items.first { it.name == "Milk" }
                milk.category shouldBe GroceryCategory.DAIRY
            }
        }

    @Test
    fun syncWithRemote_prunes_local_items_that_were_deleted_on_another_device() =
        runTest(testDispatcher) {
            // Local list already linked to a remote list with two synced items.
            // Simulates a device that previously synced both items.
            val localListId = repository.ensureDefaultList()
            val remoteListId = "remote-list-prune"
            val now = "2026-01-01T00:00:00"
            db.groceryListQueries.updateRemoteId(remoteId = remoteListId, id = localListId)
            db.groceryQueries.createWithRemoteId(
                name = "Milk",
                isChecked = false,
                createdAt = now,
                updatedAt = now,
                remoteId = "remote-milk",
                listRemoteId = remoteListId,
                clientId = "client-milk",
                listId = localListId,
                recipeName = null,
                aisle = null,
            )
            db.groceryQueries.createWithRemoteId(
                name = "Eggs",
                isChecked = false,
                createdAt = now,
                updatedAt = now,
                remoteId = "remote-eggs",
                listRemoteId = remoteListId,
                clientId = "client-eggs",
                listId = localListId,
                recipeName = null,
                aisle = null,
            )

            // Remote now only has Milk (Eggs was deleted on another device).
            fakeRemote.remoteLists["user-1"] =
                mutableListOf(
                    RemoteGroceryList(
                        id = remoteListId,
                        name = "My Grocery List",
                        ownerId = "user-1",
                    )
                )
            fakeRemote.remoteItems[remoteListId] =
                mutableListOf(
                    RemoteGroceryItem(
                        id = "remote-milk",
                        listId = remoteListId,
                        name = "Milk",
                        clientId = "client-milk",
                    )
                )

            fakeAuth.setState(
                AuthState.Authenticated(
                    ChefMateUser(
                        userId = "user-1",
                        userName = "Test",
                        userEmail = "test@test.com",
                        userProfileImageUrl = null,
                    )
                )
            )

            repository.getGroceries(localListId).test {
                val items = awaitItem()
                items.size shouldBe 1
                items.first().name shouldBe "Milk"
            }
        }

    @Test
    fun syncWithRemote_preserves_local_items_with_no_remote_id_during_prune() =
        runTest(testDispatcher) {
            // Local list linked to remote with one synced item and one unsynced item.
            val localListId = repository.ensureDefaultList()
            val remoteListId = "remote-list-keep-unsynced"
            val now = "2026-01-01T00:00:00"
            db.groceryListQueries.updateRemoteId(remoteId = remoteListId, id = localListId)
            db.groceryQueries.createWithRemoteId(
                name = "Milk",
                isChecked = false,
                createdAt = now,
                updatedAt = now,
                remoteId = "remote-milk",
                listRemoteId = remoteListId,
                clientId = "client-milk",
                listId = localListId,
                recipeName = null,
                aisle = null,
            )
            // Brand new local item — never pushed yet (no remoteId).
            repository.addGrocery(localListId, "Bread")

            // Remote has neither item (synced item was deleted on another device).
            fakeRemote.remoteLists["user-1"] =
                mutableListOf(
                    RemoteGroceryList(
                        id = remoteListId,
                        name = "My Grocery List",
                        ownerId = "user-1",
                    )
                )
            fakeRemote.remoteItems[remoteListId] = mutableListOf()

            fakeAuth.setState(
                AuthState.Authenticated(
                    ChefMateUser(
                        userId = "user-1",
                        userName = "Test",
                        userEmail = "test@test.com",
                        userProfileImageUrl = null,
                    )
                )
            )

            // The unsynced "Bread" item should survive — the push earlier in the
            // sync will create a remote for it. The previously-synced "Milk" is
            // pruned because it no longer exists on the remote.
            repository.getGroceries(localListId).test {
                val items = awaitItem()
                items.map { it.name }.toSet() shouldBe setOf("Bread")
            }
        }

    @Test
    fun syncWithRemote_prunes_local_list_that_was_deleted_on_another_device() =
        runTest(testDispatcher) {
            // Two local lists both linked to remote. One is removed from remote.
            val keptLocalId = repository.ensureDefaultList()
            val keptRemoteId = "remote-list-kept"
            db.groceryListQueries.updateRemoteId(remoteId = keptRemoteId, id = keptLocalId)

            val prunedLocalId = repository.createGroceryList("Party Supplies")
            val prunedRemoteId = "remote-list-pruned"
            db.groceryListQueries.updateRemoteId(remoteId = prunedRemoteId, id = prunedLocalId)
            val now = "2026-01-01T00:00:00"
            db.groceryQueries.createWithRemoteId(
                name = "Chips",
                isChecked = false,
                createdAt = now,
                updatedAt = now,
                remoteId = "remote-chips",
                listRemoteId = prunedRemoteId,
                clientId = "client-chips",
                listId = prunedLocalId,
                recipeName = null,
                aisle = null,
            )

            // Remote only has the kept list.
            fakeRemote.remoteLists["user-1"] =
                mutableListOf(
                    RemoteGroceryList(
                        id = keptRemoteId,
                        name = "My Grocery List",
                        ownerId = "user-1",
                    )
                )
            fakeRemote.remoteItems[keptRemoteId] = mutableListOf()

            fakeAuth.setState(
                AuthState.Authenticated(
                    ChefMateUser(
                        userId = "user-1",
                        userName = "Test",
                        userEmail = "test@test.com",
                        userProfileImageUrl = null,
                    )
                )
            )

            repository.getGroceryLists().test {
                val lists = awaitItem()
                lists.size shouldBe 1
                lists.first().name shouldBe "My Grocery List"
            }
            // Items belonging to the pruned list are gone too.
            db.groceryQueries.readAll().executeAsList().isEmpty() shouldBe true
        }

    @Test
    fun updateGrocery_pushes_aisle_to_remote() =
        runTest(testDispatcher) {
            // Authenticate first so push paths are exercised.
            fakeAuth.setState(
                AuthState.Authenticated(
                    ChefMateUser(
                        userId = "user-1",
                        userName = "Test",
                        userEmail = "test@test.com",
                        userProfileImageUrl = null,
                    )
                )
            )
            val listId = repository.ensureDefaultList()
            repository.addGrocery(listId, "Apples")

            // The unsynced push (pushAddToRemote) already happened; updateGrocery exercises
            // pushUpdateToRemote with the manual aisle override.
            val item = repository.getGroceries(listId).first().first()
            repository.updateGrocery(item.copy(category = GroceryCategory.MEAT))

            val pushed = fakeRemote.remoteItems.values.flatten().first { it.name == "Apples" }
            pushed.aisle shouldBe GroceryCategory.MEAT.name
        }

    @Test
    fun syncWithRemote_marks_freshly_pushed_default_list_as_owned_not_shared() =
        runTest(testDispatcher) {
            // New account: a local default list exists, and the remote has nothing yet.
            repository.ensureDefaultList()

            // Sign in. Sync pushes the local list to the remote, the fake mirrors the
            // production trigger by inserting an owner member row, and syncListMembers runs.
            fakeAuth.setState(
                AuthState.Authenticated(
                    ChefMateUser(
                        userId = "user-1",
                        userName = "Test",
                        userEmail = "test@test.com",
                        userProfileImageUrl = null,
                    )
                )
            )
            advanceUntilIdle()

            // The user's own list must not be flagged as "shared with you".
            repository.getGroceryLists().test {
                val lists = awaitItem()
                lists.size shouldBe 1
                val defaultList = lists.first()
                defaultList.name shouldBe "My Grocery List"
                defaultList.isShared shouldBe false
            }
        }

    @Test
    fun refreshListMembers_caches_pending_and_accepted_collaborators_from_remote() =
        runTest(testDispatcher) {
            val listId = repository.ensureDefaultList()
            val remoteListId = "remote-list-members"
            db.groceryListQueries.updateRemoteId(remoteId = remoteListId, id = listId)
            fakeRemote.remoteMembers[remoteListId] =
                mutableListOf(
                    RemoteGroceryListMember(
                        id = "pending-member",
                        listId = remoteListId,
                        invitedEmail = "pending@example.com",
                        role = "viewer",
                        status = "pending",
                    ),
                    RemoteGroceryListMember(
                        id = "accepted-member",
                        listId = remoteListId,
                        userId = "user-2",
                        invitedEmail = "accepted@example.com",
                        role = "editor",
                        status = "accepted",
                    ),
                )

            repository.refreshListMembers(listId)

            repository.getListCollaborators(listId).test {
                val collaborators = awaitItem()
                collaborators.map { it.email } shouldBe
                    listOf("pending@example.com", "accepted@example.com")
                collaborators.first { it.email == "pending@example.com" }.status shouldBe
                    CollaborationStatus.PENDING
                collaborators.first { it.email == "accepted@example.com" }.status shouldBe
                    CollaborationStatus.ACCEPTED
            }
        }

    @Test
    fun refreshListMembers_does_not_clear_existing_cache_when_remote_returns_empty() =
        runTest(testDispatcher) {
            val listId = repository.ensureDefaultList()
            val remoteListId = "remote-list-empty-members"
            db.groceryListQueries.updateRemoteId(remoteId = remoteListId, id = listId)
            db.groceryListMemberQueries.insert(
                listLocalId = listId,
                remoteId = "cached-member",
                userId = null,
                userEmail = "cached@example.com",
                role = "editor",
                status = "pending",
                displayName = null,
                avatarUrl = null,
            )

            repository.refreshListMembers(listId)

            repository.getListCollaborators(listId).test {
                val collaborators = awaitItem()
                collaborators.map { it.email } shouldBe listOf("cached@example.com")
            }
        }

    @Test
    fun refreshListMembers_moves_stale_remote_member_cache_to_current_list() =
        runTest(testDispatcher) {
            val staleListId = repository.ensureDefaultList()
            val currentListId = repository.createGroceryList("Shared")
            val currentRemoteListId = "remote-list-current-members"
            db.groceryListQueries.updateRemoteId(remoteId = currentRemoteListId, id = currentListId)
            db.groceryListMemberQueries.insert(
                listLocalId = staleListId,
                remoteId = "accepted-member",
                userId = null,
                userEmail = "accepted@example.com",
                role = "editor",
                status = "accepted",
                displayName = null,
                avatarUrl = null,
            )
            fakeRemote.remoteMembers[currentRemoteListId] =
                mutableListOf(
                    RemoteGroceryListMember(
                        id = "accepted-member",
                        listId = currentRemoteListId,
                        userId = "user-2",
                        invitedEmail = "accepted@example.com",
                        role = "editor",
                        status = "accepted",
                    )
                )

            repository.refreshListMembers(currentListId)

            repository.getListCollaborators(currentListId).test {
                val collaborators = awaitItem()
                collaborators.map { it.email } shouldBe listOf("accepted@example.com")
            }
            repository.getListCollaborators(staleListId).test {
                awaitItem().isEmpty() shouldBe true
            }
        }

    // ─── deleteAllGroceries ───────────────────────────────────────────────────

    @Test
    fun deleteAllGroceries_removes_all_items_from_local_db() =
        runTest(testDispatcher) {
            val listId = repository.ensureDefaultList()
            repository.addGrocery(listId, "Apples")
            repository.addGrocery(listId, "Bananas")

            repository.deleteAllGroceries(listId)

            repository.getGroceries(listId).test { awaitItem().isEmpty() shouldBe true }
        }

    @Test
    fun deleteAllGroceries_calls_remote_delete_for_each_synced_item() =
        runTest(testDispatcher) {
            val listId = repository.ensureDefaultList()
            val remoteListId = "remote-list-1"
            val now = "2026-01-01T00:00:00"
            db.groceryQueries.createWithRemoteId(
                name = "Apples",
                isChecked = false,
                createdAt = now,
                updatedAt = now,
                remoteId = "remote-apple",
                listRemoteId = remoteListId,
                clientId = "client-apple",
                listId = listId,
                recipeName = null,
                aisle = null,
            )
            db.groceryQueries.createWithRemoteId(
                name = "Bananas",
                isChecked = false,
                createdAt = now,
                updatedAt = now,
                remoteId = "remote-banana",
                listRemoteId = remoteListId,
                clientId = "client-banana",
                listId = listId,
                recipeName = null,
                aisle = null,
            )
            fakeRemote.remoteItems[remoteListId] =
                mutableListOf(
                    RemoteGroceryItem(id = "remote-apple", listId = remoteListId, name = "Apples"),
                    RemoteGroceryItem(
                        id = "remote-banana",
                        listId = remoteListId,
                        name = "Bananas",
                    ),
                )

            repository.deleteAllGroceries(listId)

            fakeRemote.remoteItems[remoteListId].orEmpty().isEmpty() shouldBe true
        }

    @Test
    fun deleteAllGroceries_skips_remote_delete_when_items_have_no_remote_id() =
        runTest(testDispatcher) {
            val listId = repository.ensureDefaultList()
            repository.addGrocery(listId, "Apples") // no remoteId
            val unrelatedListId = "unrelated-list"
            fakeRemote.remoteItems[unrelatedListId] =
                mutableListOf(
                    RemoteGroceryItem(
                        id = "remote-item-x",
                        listId = unrelatedListId,
                        name = "Other",
                    )
                )

            repository.deleteAllGroceries(listId)

            fakeRemote.remoteItems[unrelatedListId]!!.size shouldBe 1
        }

    @Test
    fun deleteAllGroceries_only_deletes_items_from_the_specified_list() =
        runTest(testDispatcher) {
            val listId1 = repository.ensureDefaultList()
            val listId2 = repository.createGroceryList("Second List")
            repository.addGrocery(listId1, "Apples")
            repository.addGrocery(listId2, "Bananas")

            repository.deleteAllGroceries(listId1)

            repository.getGroceries(listId1).test { awaitItem().isEmpty() shouldBe true }
            repository.getGroceries(listId2).test { awaitItem().size shouldBe 1 }
        }

    // ─── deletePurchasedGroceries ─────────────────────────────────────────────

    @Test
    fun deletePurchasedGroceries_removes_only_checked_items_from_db() =
        runTest(testDispatcher) {
            val listId = repository.ensureDefaultList()
            repository.addGrocery(listId, "Apples")
            repository.addGrocery(listId, "Bananas")

            val items = db.groceryQueries.readByListId(listId).executeAsList()
            val toCheck = repository.getGrocery(items.first().id)!!
            repository.updateChecked(toCheck, isChecked = true)

            repository.deletePurchasedGroceries(listId)

            repository.getGroceries(listId).test {
                val remaining = awaitItem()
                remaining.size shouldBe 1
                remaining.first().isChecked shouldBe false
            }
        }

    @Test
    fun deletePurchasedGroceries_calls_remote_delete_only_for_checked_synced_items() =
        runTest(testDispatcher) {
            val listId = repository.ensureDefaultList()
            val remoteListId = "remote-list-checked"
            val now = "2026-01-01T00:00:00"
            db.groceryQueries.createWithRemoteId(
                name = "Milk",
                isChecked = true,
                createdAt = now,
                updatedAt = now,
                remoteId = "remote-milk",
                listRemoteId = remoteListId,
                clientId = "client-milk",
                listId = listId,
                recipeName = null,
                aisle = null,
            )
            db.groceryQueries.createWithRemoteId(
                name = "Eggs",
                isChecked = false,
                createdAt = now,
                updatedAt = now,
                remoteId = "remote-eggs",
                listRemoteId = remoteListId,
                clientId = "client-eggs",
                listId = listId,
                recipeName = null,
                aisle = null,
            )
            fakeRemote.remoteItems[remoteListId] =
                mutableListOf(
                    RemoteGroceryItem(
                        id = "remote-milk",
                        listId = remoteListId,
                        name = "Milk",
                        isChecked = true,
                    ),
                    RemoteGroceryItem(id = "remote-eggs", listId = remoteListId, name = "Eggs"),
                )

            repository.deletePurchasedGroceries(listId)

            val remaining = fakeRemote.remoteItems[remoteListId].orEmpty()
            remaining.size shouldBe 1
            remaining.first().name shouldBe "Eggs"
        }

    @Test
    fun deletePurchasedGroceries_is_a_no_op_when_no_items_are_checked() =
        runTest(testDispatcher) {
            val listId = repository.ensureDefaultList()
            repository.addGrocery(listId, "Apples")
            repository.addGrocery(listId, "Bananas")

            repository.deletePurchasedGroceries(listId)

            repository.getGroceries(listId).test { awaitItem().size shouldBe 2 }
        }

    @Test
    fun realtime_change_pulls_a_remote_item_without_re_signing_in() =
        runTest(testDispatcher) {
            // Local default list linked to a remote list on sign-in.
            repository.ensureDefaultList()
            val remoteListId = "remote-list-realtime"
            fakeRemote.remoteLists["user-1"] =
                mutableListOf(
                    RemoteGroceryList(
                        id = remoteListId,
                        name = "My Grocery List",
                        ownerId = "user-1",
                    )
                )
            fakeAuth.setState(
                AuthState.Authenticated(
                    ChefMateUser(
                        userId = "user-1",
                        userName = "Test",
                        userEmail = "test@test.com",
                        userProfileImageUrl = null,
                    )
                )
            )
            advanceUntilIdle()

            // The initial sign-in sync pulled an empty list.
            repository.getGroceries().first().size shouldBe 0

            // Another device adds an item, then a realtime change notification arrives.
            fakeRemote.remoteItems[remoteListId] =
                mutableListOf(
                    RemoteGroceryItem(
                        id = "item-remote-1",
                        listId = remoteListId,
                        name = "Milk",
                        clientId = Uuid.random().toString(),
                    )
                )
            fakeRemote.changes.tryEmit(Unit)
            advanceUntilIdle()

            // The live subscription re-reconciled and pulled the new item on its own.
            repository.getGroceries().test {
                val items = awaitItem()
                items.size shouldBe 1
                items.first().name shouldBe "Milk"
            }
        }

    @Test
    fun realtime_change_pulls_an_updated_checked_state_for_an_existing_item() =
        runTest(testDispatcher) {
            // Local list already linked to a remote list with one synced, unchecked item —
            // simulates Device B that previously synced the item.
            val localListId = repository.ensureDefaultList()
            val remoteListId = "remote-list-checked-sync"
            val now = "2026-01-01T00:00:00"
            db.groceryListQueries.updateRemoteId(remoteId = remoteListId, id = localListId)
            db.groceryQueries.createWithRemoteId(
                name = "Milk",
                isChecked = false,
                createdAt = now,
                updatedAt = now,
                remoteId = "remote-milk",
                listRemoteId = remoteListId,
                clientId = "client-milk",
                listId = localListId,
                recipeName = null,
                aisle = null,
            )
            fakeRemote.remoteLists["user-1"] =
                mutableListOf(
                    RemoteGroceryList(
                        id = remoteListId,
                        name = "My Grocery List",
                        ownerId = "user-1",
                    )
                )
            fakeRemote.remoteItems[remoteListId] =
                mutableListOf(
                    RemoteGroceryItem(
                        id = "remote-milk",
                        listId = remoteListId,
                        name = "Milk",
                        isChecked = false,
                        clientId = "client-milk",
                    )
                )
            fakeAuth.setState(
                AuthState.Authenticated(
                    ChefMateUser(
                        userId = "user-1",
                        userName = "Test",
                        userEmail = "test@test.com",
                        userProfileImageUrl = null,
                    )
                )
            )
            advanceUntilIdle()
            repository.getGroceries(localListId).first().first().isChecked shouldBe false

            // Device A marks the item as purchased, then a realtime change arrives.
            fakeRemote.remoteItems[remoteListId] =
                mutableListOf(
                    RemoteGroceryItem(
                        id = "remote-milk",
                        listId = remoteListId,
                        name = "Milk",
                        isChecked = true,
                        updatedAt = "2026-01-02T00:00:00",
                        clientId = "client-milk",
                    )
                )
            fakeRemote.changes.tryEmit(Unit)
            advanceUntilIdle()

            // The heartbeat re-reconciled and the local item now reflects the purchased state.
            repository.getGroceries(localListId).test {
                awaitItem().first().isChecked shouldBe true
            }
        }

    // ─── addGroceries dedup (issue #483) ─────────────────────────────────────

    @Test
    fun addGroceries_merges_matching_quantities_into_the_existing_row() =
        runTest(testDispatcher) {
            val listId = repository.ensureDefaultList()
            repository.addGrocery(listId, "2 cups flour")

            repository.addGroceries(listId, listOf("1 cup flour"), recipeName = "Bread")

            repository.getGroceries(listId).test {
                val items = awaitItem()
                items.size shouldBe 1
                items.first().name shouldBe "3 cups flour"
            }
        }

    @Test
    fun addGrocery_bumps_the_count_when_re_adding_a_bare_recipe_item() =
        runTest(testDispatcher) {
            // Reproduces the reported no-op: a recipe adds "1 red bell pepper", then the user
            // manually adds a bare "red bell pepper" from the grocery list screen. The two should
            // combine into a count of two rather than leaving the row unchanged. The recipe
            // attribution on the existing row is preserved.
            val listId = repository.ensureDefaultList()
            repository.addGroceries(listId, listOf("1 red bell pepper"), recipeName = "Fajitas")

            repository.addGrocery(listId, "red bell pepper")

            repository.getGroceries(listId).test {
                val items = awaitItem()
                items.size shouldBe 1
                items.first().name shouldBe "2 red bell pepper"
                items.first().recipeName shouldBe "Fajitas"
            }
        }

    @Test
    fun addGrocery_bumps_a_large_recipe_item_when_re_adding_it_bare() =
        runTest(testDispatcher) {
            // Reproduces the reported no-op with a size descriptor: a recipe adds "2 large red bell
            // pepper (sliced)", then the user manually re-adds the bare name. "large" describes a
            // countable item, so it should count up to three rather than leaving the row unchanged.
            val listId = repository.ensureDefaultList()
            repository.addGroceries(
                listId,
                listOf("2 large red bell pepper (sliced)"),
                recipeName = "Pad Krapow",
            )

            repository.addGrocery(listId, "red bell pepper (sliced)")

            repository.getGroceries(listId).test {
                val items = awaitItem()
                items.size shouldBe 1
                items.first().name shouldBe "3 large red bell pepper (sliced)"
                items.first().recipeName shouldBe "Pad Krapow"
            }
        }

    @Test
    fun addGroceries_merges_two_matching_lines_within_the_same_call() =
        runTest(testDispatcher) {
            val listId = repository.ensureDefaultList()

            repository.addGroceries(listId, listOf("2 eggs", "3 eggs"), recipeName = "Cake")

            repository.getGroceries(listId).test {
                val items = awaitItem()
                items.size shouldBe 1
                items.first().name shouldBe "5 eggs"
            }
        }

    @Test
    fun addGroceries_does_not_merge_items_across_different_lists() =
        runTest(testDispatcher) {
            val listId1 = repository.ensureDefaultList()
            val listId2 = repository.createGroceryList("Second List")
            repository.addGrocery(listId1, "2 cups flour")

            repository.addGroceries(listId2, listOf("1 cup flour"), recipeName = null)

            repository.getGroceries(listId1).test {
                awaitItem().first().name shouldBe "2 cups flour"
            }
            repository.getGroceries(listId2).test {
                awaitItem().first().name shouldBe "1 cup flour"
            }
        }

    @Test
    fun addGroceries_unchecks_an_already_purchased_item_when_merging_into_it() =
        runTest(testDispatcher) {
            val listId = repository.ensureDefaultList()
            repository.addGrocery(listId, "flour")
            val existing = repository.getGroceries(listId).first().first()
            repository.updateChecked(existing, isChecked = true)

            repository.addGroceries(listId, listOf("flour"), recipeName = null)

            repository.getGroceries(listId).test { awaitItem().first().isChecked shouldBe false }
        }

    @Test
    fun addGroceries_pushes_a_merged_update_to_an_already_synced_item() =
        runTest(testDispatcher) {
            fakeAuth.setState(
                AuthState.Authenticated(
                    ChefMateUser(
                        userId = "user-1",
                        userName = "Test",
                        userEmail = "test@test.com",
                        userProfileImageUrl = null,
                    )
                )
            )
            val listId = repository.ensureDefaultList()
            repository.addGrocery(listId, "2 cups flour")
            advanceUntilIdle()

            repository.addGroceries(listId, listOf("1 cup flour"), recipeName = null)
            advanceUntilIdle()

            val pushed = fakeRemote.remoteItems.values.flatten().first { it.name.endsWith("flour") }
            pushed.name shouldBe "3 cups flour"
        }

    @Test
    fun updateChecked_pushes_the_new_checked_state_to_the_backend() =
        runTest(testDispatcher) {
            fakeAuth.setState(
                AuthState.Authenticated(
                    ChefMateUser(
                        userId = "user-1",
                        userName = "Test",
                        userEmail = "test@test.com",
                        userProfileImageUrl = null,
                    )
                )
            )
            val listId = repository.ensureDefaultList()
            repository.addGrocery(listId, "Milk")
            advanceUntilIdle()

            // The add synced, so the local item now has a remote id.
            val item = repository.getGroceries(listId).first().first { it.name == "Milk" }
            item.isChecked shouldBe false

            repository.updateChecked(item, isChecked = true)
            advanceUntilIdle()

            // The checked state reached the backend via updateGroceryItem.
            val remote = fakeRemote.remoteItems.values.flatten().first { it.name == "Milk" }
            remote.isChecked shouldBe true
        }

    @Test
    fun categoryOverride_wins_over_parser_default() =
        runTest(testDispatcher) {
            // "Paper Towels" isn't in the IngredientParser vocabulary, so it defaults to OTHER.
            repository.addGrocery("Paper Towels")
            repository.getGroceries().first().first { it.name == "Paper Towels" }.category shouldBe
                GroceryCategory.OTHER

            fakeOverrides.setOverride("Paper Towels", GroceryCategory.SNACKS)
            advanceUntilIdle()

            repository.getGroceries().test {
                awaitItem().first { it.name == "Paper Towels" }.category shouldBe
                    GroceryCategory.SNACKS
            }
        }

    @Test
    fun categoryOverride_matches_parsed_name_case_insensitively() =
        runTest(testDispatcher) {
            // The stored name carries a quantity; the rule is keyed off the parsed name ("cold
            // brew").
            repository.addGrocery("2 cans Cold Brew")
            fakeOverrides.setOverride("cold brew", GroceryCategory.BEVERAGES)
            advanceUntilIdle()

            repository.getGroceries().test {
                awaitItem().first { it.name == "2 cans Cold Brew" }.category shouldBe
                    GroceryCategory.BEVERAGES
            }
        }

    @Test
    fun per_item_stored_aisle_wins_over_category_override() =
        runTest(testDispatcher) {
            repository.addGrocery("Paper Towels")
            val item = repository.getGroceries().first().first { it.name == "Paper Towels" }
            // A per-item aisle set on this row (e.g. via the detail screen) pins it to
            // CANNED_GOODS.
            repository.updateGrocery(item.copy(category = GroceryCategory.CANNED_GOODS))
            // A conflicting name rule should NOT override the explicit per-item choice.
            fakeOverrides.setOverride("Paper Towels", GroceryCategory.SNACKS)
            advanceUntilIdle()

            repository.getGroceries().test {
                awaitItem().first { it.name == "Paper Towels" }.category shouldBe
                    GroceryCategory.CANNED_GOODS
            }
        }

    @Test
    fun removing_a_category_override_reverts_to_parser_default() =
        runTest(testDispatcher) {
            repository.addGrocery("Paper Towels")
            fakeOverrides.setOverride("Paper Towels", GroceryCategory.SNACKS)
            advanceUntilIdle()
            val id = fakeOverrides.observeOverrides().first().first().id

            fakeOverrides.removeOverride(id)
            advanceUntilIdle()

            repository.getGroceries().test {
                awaitItem().first { it.name == "Paper Towels" }.category shouldBe
                    GroceryCategory.OTHER
            }
        }
}
