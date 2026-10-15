@file:Suppress("FunctionName")
@file:OptIn(ExperimentalCoroutinesApi::class)

package com.plusmobileapps.chefmate.grocery.data.impl

import com.plusmobileapps.chefmate.auth.data.AuthState
import com.plusmobileapps.chefmate.auth.data.ChefMateUser
import com.plusmobileapps.chefmate.auth.data.testing.FakeAuthenticationRepository
import com.plusmobileapps.chefmate.database.Database
import com.plusmobileapps.chefmate.database.testing.createTestDatabase
import com.plusmobileapps.chefmate.grocery.data.FamilyGroceryList
import com.plusmobileapps.chefmate.grocery.data.GroceryListModel
import com.plusmobileapps.chefmate.grocery.data.ListRole
import com.plusmobileapps.chefmate.grocery.data.remote.RemoteGroceryList
import com.plusmobileapps.chefmate.grocery.data.testing.FakeGroceryCategoryOverrideRepository
import com.plusmobileapps.chefmate.grocery.data.testing.FakeGroceryRemoteDataSource
import com.plusmobileapps.chefmate.util.testing.FakeDateTimeUtil
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest

/** Family grocery lists: the RPC-backed management repository and how sync caches family lists. */
class FamilyGroceryListRepositoryImplTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val db: Database = createTestDatabase()
    private val fakeAuth = FakeAuthenticationRepository()
    private val fakeRemote =
        FakeGroceryRemoteDataSource().apply { familyNames[FAMILY_ID] = "The Smiths" }

    private val groceryRepository =
        GroceryRepositoryImpl(
            queries = db.groceryQueries,
            listQueries = db.groceryListQueries,
            memberQueries = db.groceryListMemberQueries,
            ioContext = testDispatcher,
            dateTimeUtil = FakeDateTimeUtil(),
            remoteDataSource = fakeRemote,
            authRepository = fakeAuth,
            categoryOverrideRepository = FakeGroceryCategoryOverrideRepository(),
        )

    private val repository =
        FamilyGroceryListRepositoryImpl(
            ioContext = testDispatcher,
            remoteDataSource = fakeRemote,
            groceryRepository = groceryRepository,
        )

    @Test
    fun createList_creates_the_list_remotely_and_syncs_it_into_the_local_cache() =
        runTest(testDispatcher) {
            signIn()

            repository.createList(FAMILY_ID, "  Costco run ")

            repository.getLists(FAMILY_ID).map { it.name } shouldBe listOf("Costco run")
            val local = localLists().single { it.isFamily }
            local.name shouldBe "Costco run"
            local.familyId shouldBe FAMILY_ID
            local.familyName shouldBe "The Smiths"
        }

    @Test
    fun a_family_can_have_several_lists() =
        runTest(testDispatcher) {
            signIn()

            repository.createList(FAMILY_ID, "Weekly")
            repository.createList(FAMILY_ID, "Party")

            repository.getLists(FAMILY_ID).map { it.name } shouldBe listOf("Weekly", "Party")
            localLists().filter { it.isFamily }.map { it.name }.toSet() shouldBe
                setOf("Weekly", "Party")
        }

    @Test
    fun renameList_updates_the_local_name_from_the_remote() =
        runTest(testDispatcher) {
            signIn()
            repository.createList(FAMILY_ID, "Weekly")
            val listId = repository.getLists(FAMILY_ID).single().id

            repository.renameList(listId, "Weekly shop")

            localLists().single { it.isFamily }.name shouldBe "Weekly shop"
        }

    @Test
    fun deleteList_removes_the_list_and_prunes_it_locally() =
        runTest(testDispatcher) {
            signIn()
            repository.createList(FAMILY_ID, "Weekly")
            val listId = repository.getLists(FAMILY_ID).single().id

            repository.deleteList(listId)

            repository.getLists(FAMILY_ID) shouldBe emptyList<FamilyGroceryList>()
            localLists().none { it.isFamily } shouldBe true
        }

    @Test
    fun rpc_failures_propagate_to_the_caller() =
        runTest(testDispatcher) {
            fakeRemote.familyListError = IllegalStateException("family: forbidden")

            shouldThrow<IllegalStateException> { repository.createList(FAMILY_ID, "Weekly") }
            shouldThrow<IllegalStateException> { repository.getLists(FAMILY_ID) }
        }

    @Test
    fun sync_caches_a_family_list_as_editable_but_not_owned_or_shared() =
        runTest(testDispatcher) {
            // Created by another member of the family.
            addRemoteFamilyList(id = "family-list", name = "Weekly", creator = "user-2")

            signIn()

            val list = localLists().single { it.isFamily }
            list.role shouldBe ListRole.EDITOR
            list.isShared shouldBe false
            list.familyName shouldBe "The Smiths"
        }

    @Test
    fun a_family_list_the_user_created_is_still_not_owned() =
        runTest(testDispatcher) {
            addRemoteFamilyList(id = "family-list", name = "Weekly", creator = USER_ID)

            signIn()

            localLists().single { it.isFamily }.role shouldBe ListRole.EDITOR
        }

    @Test
    fun sync_never_links_the_local_default_list_to_a_family_list_with_the_same_name() =
        runTest(testDispatcher) {
            groceryRepository.ensureDefaultList()
            addRemoteFamilyList(id = "family-list", name = "My Grocery List", creator = USER_ID)

            signIn()

            val lists = localLists()
            lists.size shouldBe 2
            lists.single { !it.isFamily }.role shouldBe ListRole.OWNER
        }

    @Test
    fun ensureDefaultList_prefers_a_personal_list_over_a_family_list() =
        runTest(testDispatcher) {
            addRemoteFamilyList(id = "family-list", name = "Weekly", creator = "user-2")
            fakeRemote.remoteLists
                .getOrPut(USER_ID) { mutableListOf() }
                .add(RemoteGroceryList(id = "personal", name = "Mine", ownerId = USER_ID))
            signIn()

            val defaultId = groceryRepository.ensureDefaultList()

            localLists().single { it.id == defaultId }.name shouldBe "Mine"
        }

    @Test
    fun leaving_the_family_prunes_its_lists_on_the_next_sync() =
        runTest(testDispatcher) {
            addRemoteFamilyList(id = "family-list", name = "Weekly", creator = "user-2")
            signIn()
            localLists().any { it.isFamily } shouldBe true

            // RLS stops returning the family's lists once the user has left.
            fakeRemote.remoteLists.remove(FAMILY_ID)
            repository.refreshLocalLists()

            localLists().none { it.isFamily } shouldBe true
        }

    @Test
    fun a_deleted_family_hands_the_list_back_to_its_creator_as_a_personal_list() =
        runTest(testDispatcher) {
            addRemoteFamilyList(id = "family-list", name = "Weekly", creator = USER_ID)
            signIn()

            // families.id ON DELETE SET NULL
            fakeRemote.remoteLists[FAMILY_ID] =
                mutableListOf(
                    RemoteGroceryList(id = "family-list", name = "Weekly", ownerId = USER_ID)
                )
            repository.refreshLocalLists()

            val list = localLists().single { it.name == "Weekly" }
            list.isFamily shouldBe false
            list.familyName shouldBe null
            list.role shouldBe ListRole.OWNER
        }

    private fun addRemoteFamilyList(id: String, name: String, creator: String) {
        fakeRemote.remoteLists
            .getOrPut(FAMILY_ID) { mutableListOf() }
            .add(RemoteGroceryList(id = id, name = name, ownerId = creator, familyId = FAMILY_ID))
    }

    private suspend fun localLists(): List<GroceryListModel> =
        groceryRepository.getGroceryLists().first()

    private fun signIn() {
        fakeAuth.setState(
            AuthState.Authenticated(
                ChefMateUser(
                    userId = USER_ID,
                    userName = "Test",
                    userEmail = "test@test.com",
                    userProfileImageUrl = null,
                )
            )
        )
    }

    private companion object {
        const val USER_ID = "user-1"
        const val FAMILY_ID = "family-1"
    }
}
