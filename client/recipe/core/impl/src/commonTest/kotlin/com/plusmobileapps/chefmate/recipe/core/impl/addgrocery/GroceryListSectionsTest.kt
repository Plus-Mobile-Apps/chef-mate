@file:Suppress("FunctionName")

package com.plusmobileapps.chefmate.recipe.core.impl.addgrocery

import com.plusmobileapps.chefmate.recipe.core.addgrocery.AddRecipeToGroceryListBloc.GroceryListItem
import com.plusmobileapps.chefmate.recipe.core.addgrocery.AddRecipeToGroceryListBloc.GroceryListSection.Header
import com.plusmobileapps.chefmate.recipe.core.addgrocery.AddRecipeToGroceryListBloc.Model
import io.kotest.matchers.collections.shouldContainExactly
import kotlin.test.Test
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList

class GroceryListSectionsTest {

    private fun model(vararg lists: GroceryListItem) =
        Model(
            isLoading = false,
            isAdding = false,
            groupedIngredients = persistentListOf(),
            groceryLists = lists.toList().toPersistentList(),
        )

    @Test
    fun When_lists_are_mixed_Then_they_group_personal_then_each_family_then_shared() {
        val mine = GroceryListItem(id = 1L, name = "Mine")
        val shared = GroceryListItem(id = 2L, name = "Theirs", isShared = true)
        val smiths = GroceryListItem(id = 3L, name = "Ours", familyId = "f1", familyName = "Smiths")
        val jones = GroceryListItem(id = 4L, name = "Camp", familyId = "f2", familyName = null)
        val smithsToo =
            GroceryListItem(id = 5L, name = "Costco", familyId = "f1", familyName = "Smiths")

        val sections = model(mine, shared, smiths, jones, smithsToo).groceryListSections

        sections.map { it.header } shouldContainExactly
            listOf(
                Header.Personal,
                Header.Family("Smiths"),
                Header.Family(null),
                Header.SharedWithYou,
            )
        sections.map { section -> section.lists.map { it.id } } shouldContainExactly
            listOf(listOf(1L), listOf(3L, 5L), listOf(4L), listOf(2L))
    }

    @Test
    fun When_only_personal_lists_exist_Then_there_is_a_single_section() {
        val sections =
            model(GroceryListItem(id = 1L, name = "A"), GroceryListItem(id = 2L, name = "B"))
                .groceryListSections

        sections.map { it.header } shouldContainExactly listOf(Header.Personal)
    }
}
