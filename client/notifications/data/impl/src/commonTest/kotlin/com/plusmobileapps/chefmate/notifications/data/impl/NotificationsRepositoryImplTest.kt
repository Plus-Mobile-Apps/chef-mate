package com.plusmobileapps.chefmate.notifications.data.impl

import app.cash.turbine.test
import com.plusmobileapps.chefmate.family.data.FamilyRole
import com.plusmobileapps.chefmate.family.data.testing.FakeFamilyRepository
import com.plusmobileapps.chefmate.grocery.data.GroceryListInvite
import com.plusmobileapps.chefmate.grocery.data.ListRole
import com.plusmobileapps.chefmate.grocery.data.testing.FakeGroceryRepository
import com.plusmobileapps.chefmate.notifications.data.AppNotification
import com.plusmobileapps.chefmate.recipebook.data.RecipeBookInvite
import com.plusmobileapps.chefmate.recipebook.data.RecipeBookRole
import com.plusmobileapps.chefmate.recipebook.data.testing.FakeRecipeBookCollaborationRepository
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import kotlin.test.Test
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest

class NotificationsRepositoryImplTest {

    private val grocery = FakeGroceryRepository()
    private val recipeBook = FakeRecipeBookCollaborationRepository()
    private val family = FakeFamilyRepository()

    private val repository =
        NotificationsRepositoryImpl(
            groceryRepository = grocery,
            recipeBookCollaborationRepository = recipeBook,
            familyRepository = family,
        )

    @Test
    fun merges_grocery_recipe_book_and_family_pending_invites() = runTest {
        grocery.pendingInvitations.value =
            listOf(
                GroceryListInvite(memberId = "g1", listName = "Weeknight", role = ListRole.EDITOR)
            )
        recipeBook.invites =
            mutableListOf(
                RecipeBookInvite(
                    memberId = "b1",
                    bookName = "Desserts",
                    role = RecipeBookRole.EDITOR,
                )
            )

        val familyMemberId = family.seedInvite("Smiths", role = FamilyRole.ADMIN)

        repository.notifications.first() shouldContainExactlyInAnyOrder
            listOf(
                AppNotification.GroceryInvite("g1", "Weeknight", ListRole.EDITOR),
                AppNotification.RecipeBookInvite("b1", "Desserts", RecipeBookRole.EDITOR),
                AppNotification.FamilyInvite(familyMemberId, "Smiths", FamilyRole.ADMIN),
            )
    }

    @Test
    fun a_family_source_that_fails_does_not_blank_the_other_kinds() = runTest {
        grocery.pendingInvitations.value =
            listOf(GroceryListInvite("g1", "Weeknight", ListRole.EDITOR))
        family.failure = IllegalStateException("offline")

        repository.notifications.first() shouldBe
            listOf(AppNotification.GroceryInvite("g1", "Weeknight", ListRole.EDITOR))
    }

    @Test
    fun accept_family_invite_routes_to_family_repository() = runTest {
        val memberId = family.seedInvite("Smiths")

        repository.accept(AppNotification.FamilyInvite(memberId, "Smiths", FamilyRole.MEMBER))

        family.pendingInvites() shouldBe emptyList()
        family.getFamilies().single().name shouldBe "Smiths"
    }

    @Test
    fun decline_family_invite_routes_to_family_repository() = runTest {
        val memberId = family.seedInvite("Smiths")

        repository.decline(AppNotification.FamilyInvite(memberId, "Smiths", FamilyRole.MEMBER))

        family.pendingInvites() shouldBe emptyList()
        // Declining keeps the row as rejected rather than joining the family.
        family.getFamilies() shouldBe emptyList()
    }

    @Test
    fun emits_empty_when_nothing_pending() = runTest {
        repository.notifications.first() shouldBe emptyList()
    }

    @Test
    fun accept_grocery_invite_routes_to_grocery_repository() = runTest {
        val invite = AppNotification.GroceryInvite("g1", "Weeknight", ListRole.EDITOR)
        grocery.pendingInvitations.value =
            listOf(GroceryListInvite("g1", "Weeknight", ListRole.EDITOR))

        repository.accept(invite)

        grocery.acceptedInvitations shouldBe listOf("g1")
    }

    @Test
    fun decline_recipe_book_invite_routes_to_recipe_book_repository() = runTest {
        val invite = AppNotification.RecipeBookInvite("b1", "Desserts", RecipeBookRole.EDITOR)
        recipeBook.invites =
            mutableListOf(RecipeBookInvite("b1", "Desserts", RecipeBookRole.EDITOR))

        repository.decline(invite)

        recipeBook.declined shouldBe listOf("b1")
    }

    @Test
    fun refresh_re_fetches_recipe_book_invites() = runTest {
        repository.notifications.test {
            awaitItem() shouldBe emptyList()

            // Recipe-book invites are one-shot; a new invite only appears after refresh() re-runs
            // the fetch.
            recipeBook.invites =
                mutableListOf(RecipeBookInvite("b1", "Desserts", RecipeBookRole.EDITOR))
            repository.refresh()

            awaitItem() shouldContainExactlyInAnyOrder
                listOf(AppNotification.RecipeBookInvite("b1", "Desserts", RecipeBookRole.EDITOR))
        }
    }
}
