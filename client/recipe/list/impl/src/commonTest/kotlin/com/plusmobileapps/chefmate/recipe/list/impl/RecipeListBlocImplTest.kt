@file:Suppress("FunctionName")
@file:OptIn(ExperimentalCoroutinesApi::class, ExperimentalTime::class)

package com.plusmobileapps.chefmate.recipe.list.impl

import app.cash.turbine.test
import com.plusmobileapps.chefmate.cook.data.CookingSessionRepository
import com.plusmobileapps.chefmate.di.CoachMarkController
import com.plusmobileapps.chefmate.featureflag.testing.FakeFeatureFlags
import com.plusmobileapps.chefmate.recipe.data.BuiltinCategory
import com.plusmobileapps.chefmate.recipe.data.Recipe
import com.plusmobileapps.chefmate.recipe.data.SyncStatus
import com.plusmobileapps.chefmate.recipe.data.testing.FakeCategoryRepository
import com.plusmobileapps.chefmate.recipe.data.testing.FakePendingRecipePhotoStore
import com.plusmobileapps.chefmate.recipe.data.testing.FakeRecipeImageExtractor
import com.plusmobileapps.chefmate.recipe.data.testing.FakeRecipeRepository
import com.plusmobileapps.chefmate.recipe.list.RecipeFilterOption
import com.plusmobileapps.chefmate.recipe.list.RecipeListBloc
import com.plusmobileapps.chefmate.recipe.list.RecipeListItem
import com.plusmobileapps.chefmate.recipebook.data.testing.FakeRecipeBookCollaborationRepository
import com.plusmobileapps.chefmate.recipebook.data.testing.FakeRecipeBookRepository
import com.plusmobileapps.chefmate.testing.TestBlocContext
import com.plusmobileapps.chefmate.testing.TestConsumer
import com.plusmobileapps.chefmate.text.FixedString
import com.plusmobileapps.chefmate.text.TextData
import com.plusmobileapps.chefmate.toast.testing.FakeToastService
import com.plusmobileapps.chefmate.util.TimeFormatterUtil
import com.russhwolf.settings.MapSettings
import com.russhwolf.settings.Settings
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.matcher.any
import dev.mokkery.mock
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlin.test.Test
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest

class RecipeListBlocImplTest {

    private val context = TestBlocContext.create()
    private val output = TestConsumer<RecipeListBloc.Output>()
    private val recipes = MutableStateFlow<List<Recipe>>(emptyList())
    private val repository = FakeRecipeRepository(recipes)
    private val cookingSessionRepository: CookingSessionRepository = mock {
        every { observeRecipeIds() } returns MutableStateFlow(emptyList())
    }
    private val timeFormatterUtil =
        object : TimeFormatterUtil {
            override fun formatMinutes(totalMinutes: Int): TextData =
                FixedString("${totalMinutes}m")
        }
    private val settings: Settings = mock {
        every { getBoolean("recipe_list_is_grid_view", false) } returns false
        every { getString("recipe_list_sort_option", "RECENTLY_ADDED") } returns "RECENTLY_ADDED"
        every { getString("recipe_list_active_filters", "") } returns ""
        every { getString("recipe_list_active_categories", "") } returns ""
        every { getString("recipe_list_active_user_categories", "") } returns ""
        every { putBoolean(any(), any()) } returns Unit
        every { putString(any(), any()) } returns Unit
    }
    private val categoryRepository = FakeCategoryRepository()
    private val imageExtractor = FakeRecipeImageExtractor()
    private val pendingPhotoStore = FakePendingRecipePhotoStore()
    private val featureFlags = FakeFeatureFlags()
    private val toastService = FakeToastService()

    private val bloc =
        RecipeListBlocImpl(
            context = context,
            output = output,
            viewModelFactory = {
                RecipeListViewModel(
                    mainContext = context.mainContext,
                    repository = repository,
                    recipeBookRepository = FakeRecipeBookRepository(MutableStateFlow(emptyList())),
                    collaborationRepository = FakeRecipeBookCollaborationRepository(),
                    categoryRepository = categoryRepository,
                    cookingSessionRepository = cookingSessionRepository,
                    imageExtractor = imageExtractor,
                    pendingPhotoStore = pendingPhotoStore,
                    featureFlags = featureFlags,
                    settings = settings,
                    coachMarkController = CoachMarkController(MapSettings()),
                )
            },
            timeFormatterUtil = timeFormatterUtil,
            toastService = toastService,
        )

    private fun recipe(
        id: Long,
        title: String = "Recipe $id",
        isFavorite: Boolean = false,
        starRating: Int? = null,
        totalTime: Int? = null,
        syncStatus: SyncStatus = SyncStatus.NOT_SYNCED,
    ) =
        Recipe(
            id = id,
            title = title,
            description = "Description $id",
            ingredients = "ingredients",
            directions = "directions",
            imageUrl = "http://img/$id",
            sourceUrl = null,
            servings = 4,
            prepTime = null,
            cookTime = null,
            totalTime = totalTime,
            calories = 200,
            starRating = starRating,
            isFavorite = isFavorite,
            syncStatus = syncStatus,
            createdAt = Instant.fromEpochSeconds(id * 1000),
            updatedAt = Instant.fromEpochSeconds(id * 1000),
        )

    private fun listItem(id: Long = 1, title: String = "Test") =
        RecipeListItem(
            id = id,
            title = title,
            description = null,
            imageUrl = null,
            starRating = null,
            totalTime = null,
            formattedTotalTime = null,
            servings = null,
            calories = null,
            isFavorite = false,
        )

    @Test
    fun When_recipe_clicked_Then_OpenRecipe_output_emitted_with_title() {
        bloc.onRecipeClicked(listItem(id = 42, title = "Pancakes"))
        output.lastValue shouldBe RecipeListBloc.Output.OpenRecipe(42, "Pancakes")
    }

    @Test
    fun When_add_recipe_clicked_Then_AddNewRecipe_output_emitted() {
        bloc.onAddRecipeClicked()
        output.lastValue shouldBe RecipeListBloc.Output.AddNewRecipe
    }

    @Test
    fun When_scan_photo_succeeds_Then_OpenScannedRecipe_output_emitted() = runTest {
        val extracted =
            com.plusmobileapps.chefmate.recipe.data.ExtractedRecipeData(
                title = "Scanned",
                description = null,
                ingredients = listOf("flour"),
                directions = listOf("mix"),
                imageUrl = null,
                sourceUrl = "",
                servings = null,
                prepTime = null,
                cookTime = null,
                totalTime = null,
                calories = null,
            )
        imageExtractor.response = extracted

        bloc.onScanRecipePhotoPicked(byteArrayOf(1, 2, 3), "jpg")

        output.lastValue shouldBe RecipeListBloc.Output.OpenScannedRecipe(extracted)
    }

    @Test
    fun When_recipes_loaded_Then_mapped_to_RecipeListItem() = runTest {
        bloc.state.test {
            awaitItem() // initial

            recipes.value =
                listOf(recipe(1, totalTime = 45, starRating = 3, syncStatus = SyncStatus.SYNCED))
            val state = awaitItem()
            state.recipes.size shouldBe 1

            val item = state.recipes.first()
            item.id shouldBe 1L
            item.title shouldBe "Recipe 1"
            item.description shouldBe "Description 1"
            item.imageUrl shouldBe "http://img/1"
            item.starRating shouldBe 3
            item.totalTime shouldBe 45
            item.formattedTotalTime shouldBe FixedString("45m")
            item.servings shouldBe 4
            item.calories shouldBe 200
            item.isFavorite shouldBe false
            item.syncStatus shouldBe SyncStatus.SYNCED
        }
    }

    @Test
    fun When_recipe_has_no_totalTime_Then_formattedTotalTime_is_null() = runTest {
        bloc.state.test {
            awaitItem()
            recipes.value = listOf(recipe(1, totalTime = null))
            val item = awaitItem().recipes.first()
            item.formattedTotalTime shouldBe null
        }
    }

    @Test
    fun When_recipe_has_totalTime_Then_formattedTotalTime_is_not_null() = runTest {
        bloc.state.test {
            awaitItem()
            recipes.value = listOf(recipe(1, totalTime = 45))
            val item = awaitItem().recipes.first()
            item.formattedTotalTime.shouldBeInstanceOf<TextData>()
        }
    }

    @Test
    fun When_delete_recipe_Then_recipe_removed_from_state() = runTest {
        bloc.state.test {
            awaitItem()
            recipes.value = listOf(recipe(1), recipe(2))
            awaitItem().recipes.size shouldBe 2

            bloc.onDeleteRecipe(listItem(id = 1))
            val nextState = awaitItem()
            nextState.recipes.size shouldBe 1
            nextState.recipes.first().id shouldBe 2L
        }
    }

    @Test
    fun When_syncStatus_varies_Then_mapped_correctly() = runTest {
        bloc.state.test {
            awaitItem()
            recipes.value =
                listOf(
                    recipe(1, syncStatus = SyncStatus.NOT_SYNCED),
                    recipe(2, syncStatus = SyncStatus.SYNCING),
                    recipe(3, syncStatus = SyncStatus.SYNCED),
                )
            val items = awaitItem().recipes
            val byId = items.associateBy { it.id }
            byId[1L]!!.syncStatus shouldBe SyncStatus.NOT_SYNCED
            byId[2L]!!.syncStatus shouldBe SyncStatus.SYNCING
            byId[3L]!!.syncStatus shouldBe SyncStatus.SYNCED
        }
    }

    @Test
    fun When_clear_filters_Then_all_filters_removed_from_state() = runTest {
        bloc.state.test {
            awaitItem()
            recipes.value = listOf(recipe(1, isFavorite = true), recipe(2, isFavorite = false))
            awaitItem()

            bloc.onFilterToggled(RecipeFilterOption.FAVORITES)
            awaitItem().activeFilters shouldBe setOf(RecipeFilterOption.FAVORITES)

            bloc.onClearFilters()
            val cleared = awaitItem()
            cleared.activeFilters shouldBe emptySet()
            cleared.recipes.size shouldBe 2
        }
    }

    @Test
    fun When_browse_recipes_clicked_Then_OpenBrowser_output_emitted() {
        bloc.onBrowseRecipesClicked()
        output.lastValue shouldBe RecipeListBloc.Output.OpenBrowser
    }

    @Test
    fun When_export_clicked_outside_selection_mode_Then_OpenExportRecipes_with_null_ids() {
        bloc.onExportClicked()
        output.lastValue shouldBe RecipeListBloc.Output.OpenExportRecipes(recipeIds = null)
    }

    @Test
    fun When_enter_selection_mode_Then_state_flips_with_empty_selection() = runTest {
        bloc.state.test {
            awaitItem().isSelectionMode shouldBe false
            bloc.onEnterSelectionMode()
            val next = awaitItem()
            next.isSelectionMode shouldBe true
            next.selectedRecipeIds shouldBe emptySet()
        }
    }

    @Test
    fun When_recipe_clicked_in_selection_mode_Then_toggles_selection_no_navigation() = runTest {
        bloc.state.test {
            awaitItem() // initial
            recipes.value = listOf(recipe(1))
            awaitItem() // recipes loaded
            bloc.onEnterSelectionMode()
            awaitItem() // entered selection mode

            bloc.onRecipeClicked(listItem(id = 1))
            awaitItem().selectedRecipeIds shouldBe setOf(1L)

            bloc.onRecipeClicked(listItem(id = 1))
            awaitItem().selectedRecipeIds shouldBe emptySet()
        }
        output.values shouldBe emptyList()
    }

    @Test
    fun When_export_clicked_with_selection_Then_OpenExportRecipes_carries_ids_and_selection_persists() =
        runTest {
            bloc.state.test {
                awaitItem()
                recipes.value = listOf(recipe(1), recipe(2))
                awaitItem()
                bloc.onEnterSelectionMode()
                awaitItem()
                bloc.onToggleRecipeSelected(listItem(id = 1))
                awaitItem()
                bloc.onToggleRecipeSelected(listItem(id = 2))
                awaitItem()

                bloc.onExportClicked()
                // No new emission expected — selection mode and the picked ids stay put so the
                // user can come back to the same selection if they bail out of the exporter.
                expectNoEvents()
            }
            output.lastValue shouldBe RecipeListBloc.Output.OpenExportRecipes(setOf(1L, 2L))
            bloc.state.value.isSelectionMode shouldBe true
            bloc.state.value.selectedRecipeIds shouldBe setOf(1L, 2L)
        }

    @Test
    fun When_export_finished_called_after_selection_Then_selection_mode_exits() = runTest {
        bloc.state.test {
            awaitItem()
            recipes.value = listOf(recipe(1), recipe(2))
            awaitItem()
            bloc.onEnterSelectionMode()
            awaitItem()
            bloc.onToggleRecipeSelected(listItem(id = 1))
            awaitItem()

            bloc.onExportFinished()
            val after = awaitItem()
            after.isSelectionMode shouldBe false
            after.selectedRecipeIds shouldBe emptySet()
        }
    }

    @Test
    fun When_toggle_select_all_visible_Then_selects_all_then_clears() = runTest {
        bloc.state.test {
            awaitItem()
            recipes.value = listOf(recipe(1), recipe(2), recipe(3))
            awaitItem()
            bloc.onEnterSelectionMode()
            awaitItem()

            bloc.onToggleSelectAllVisible()
            awaitItem().selectedRecipeIds shouldBe setOf(1L, 2L, 3L)

            bloc.onToggleSelectAllVisible()
            awaitItem().selectedRecipeIds shouldBe emptySet()
        }
    }

    @Test
    fun When_long_press_outside_selection_Then_enters_selection_with_that_recipe() = runTest {
        bloc.state.test {
            awaitItem() // initial
            recipes.value = listOf(recipe(1), recipe(2))
            awaitItem() // recipes loaded

            bloc.onRecipeLongClicked(listItem(id = 2))
            val next = awaitItem()
            next.isSelectionMode shouldBe true
            next.selectedRecipeIds shouldBe setOf(2L)
        }
        output.values shouldBe emptyList()
    }

    @Test
    fun When_long_press_in_selection_mode_Then_toggles_like_a_tap() = runTest {
        bloc.state.test {
            awaitItem()
            recipes.value = listOf(recipe(1), recipe(2))
            awaitItem()
            bloc.onEnterSelectionMode()
            awaitItem()

            bloc.onRecipeLongClicked(listItem(id = 1))
            awaitItem().selectedRecipeIds shouldBe setOf(1L)
            bloc.onRecipeLongClicked(listItem(id = 1))
            awaitItem().selectedRecipeIds shouldBe emptySet()
        }
    }

    @Test
    fun When_bulk_category_add_completes_Then_confirmation_toast_shown() = runTest {
        recipes.value = listOf(recipe(1))
        bloc.onEnterSelectionMode()
        bloc.onToggleRecipeSelected(listItem(id = 1))

        bloc.onAddSelectedToBuiltinCategory(BuiltinCategory.DINNER)

        toastService.shown.size shouldBe 1
        recipes.value.first().categories.map { it.builtinId } shouldBe
            listOf(BuiltinCategory.DINNER.id)
    }

    @Test
    fun When_recipes_loaded_Then_totalRecipeCount_reflects_unfiltered_count() = runTest {
        bloc.state.test {
            awaitItem()
            recipes.value = listOf(recipe(1, isFavorite = true), recipe(2, isFavorite = false))
            awaitItem().totalRecipeCount shouldBe 2

            bloc.onFilterToggled(RecipeFilterOption.FAVORITES)
            val filtered = awaitItem()
            filtered.recipes.size shouldBe 1
            filtered.totalRecipeCount shouldBe 2
        }
    }
}
