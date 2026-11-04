@file:Suppress("FunctionName")

package com.plusmobileapps.chefmate.recipe.core.impl.root

import com.plusmobileapps.chefmate.BlocContext
import com.plusmobileapps.chefmate.Consumer
import com.plusmobileapps.chefmate.recipe.core.detail.RecipeDetailBloc
import com.plusmobileapps.chefmate.recipe.core.edit.EditRecipeBloc
import com.plusmobileapps.chefmate.recipe.core.root.RecipeRootBloc
import com.plusmobileapps.chefmate.recipe.data.ExtractedRecipeData
import com.plusmobileapps.chefmate.testing.TestBlocContext
import com.plusmobileapps.chefmate.testing.TestConsumer
import dev.mokkery.mock
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlin.test.Test

class RecipeRootBlocImplTest {

    private val context = TestBlocContext.create()
    private val output = TestConsumer<RecipeRootBloc.Output>()
    private var detailOutput: Consumer<RecipeDetailBloc.Output> = Consumer {}
    private var editOutput: Consumer<EditRecipeBloc.Output> = Consumer {}
    private var lastEditRecipeId: Long? = null
    private var lastDetailInitialTitle: String? = null
    private var lastEditExtractedRecipe: ExtractedRecipeData? = null
    private var lastEditFromAi: Boolean? = null
    private var lastEditConsumePendingPhoto: Boolean? = null

    private fun createBloc(
        props: RecipeRootBloc.Props = RecipeRootBloc.Props.Detail(1L),
    ): RecipeRootBlocImpl =
        RecipeRootBlocImpl(
            context = context,
            props = props,
            output = output,
            detailBloc =
                object : RecipeDetailBloc.Factory {
                    override fun create(
                        context: BlocContext,
                        recipeId: Long,
                        initialTitle: String?,
                        output: Consumer<RecipeDetailBloc.Output>,
                    ): RecipeDetailBloc {
                        detailOutput = output
                        lastDetailInitialTitle = initialTitle
                        return mock()
                    }
                },
            editBloc =
                EditRecipeBloc.Factory {
                    _,
                    recipeId,
                    extractedRecipe,
                    fromAi,
                    consumePendingPhoto,
                    output ->
                    lastEditRecipeId = recipeId
                    lastEditExtractedRecipe = extractedRecipe
                    lastEditFromAi = fromAi
                    lastEditConsumePendingPhoto = consumePendingPhoto
                    editOutput = output
                    mock()
                },
        )

    @Test
    fun When_created_with_detail_props_Then_detail_child_is_shown() {
        val bloc = createBloc(RecipeRootBloc.Props.Detail(42L))
        bloc.routerState.value.active.instance.shouldBeInstanceOf<RecipeRootBloc.Child.Detail>()
    }

    @Test
    fun When_created_with_detail_props_initial_title_Then_detail_receives_it() {
        createBloc(RecipeRootBloc.Props.Detail(42L, initialTitle = "Pancakes"))
        lastDetailInitialTitle shouldBe "Pancakes"
    }

    @Test
    fun Detail_props_equality_ignores_the_initial_title() {
        // bringToFront dedupes on equality: the same recipe opened with or without a title is the
        // same destination.
        RecipeRootBloc.Props.Detail(42L, initialTitle = "Pancakes") shouldBe
            RecipeRootBloc.Props.Detail(42L)
    }

    @Test
    fun When_created_with_create_props_Then_edit_child_is_shown() {
        val bloc = createBloc(RecipeRootBloc.Props.Create)
        bloc.routerState.value.active.instance.shouldBeInstanceOf<RecipeRootBloc.Child.Edit>()
    }

    @Test
    fun When_detail_outputs_finished_Then_root_outputs_finished() {
        createBloc(RecipeRootBloc.Props.Detail(1L))
        detailOutput.onNext(RecipeDetailBloc.Output.Finished)
        output.lastValue shouldBe RecipeRootBloc.Output.Finished
    }

    @Test
    fun When_detail_outputs_edit_recipe_Then_edit_child_is_shown() {
        val bloc = createBloc(RecipeRootBloc.Props.Detail(1L))
        detailOutput.onNext(RecipeDetailBloc.Output.EditRecipe(1L))
        bloc.routerState.value.active.instance.shouldBeInstanceOf<RecipeRootBloc.Child.Edit>()
    }

    @Test
    fun When_detail_outputs_open_url_Then_root_outputs_open_url() {
        createBloc(RecipeRootBloc.Props.Detail(1L))
        detailOutput.onNext(RecipeDetailBloc.Output.OpenUrl("https://example.com/recipe"))
        output.lastValue shouldBe RecipeRootBloc.Output.OpenUrl("https://example.com/recipe")
    }

    @Test
    fun When_detail_outputs_open_recipe_Then_linked_recipe_detail_is_shown() {
        val bloc = createBloc(RecipeRootBloc.Props.Detail(1L))
        detailOutput.onNext(RecipeDetailBloc.Output.OpenRecipe(99L))
        bloc.routerState.value.active.instance.shouldBeInstanceOf<RecipeRootBloc.Child.Detail>()
    }

    @Test
    fun When_edit_outputs_cancelled_from_create_Then_root_outputs_finished() {
        createBloc(RecipeRootBloc.Props.Create)
        editOutput.onNext(EditRecipeBloc.Output.Cancelled)
        output.lastValue shouldBe RecipeRootBloc.Output.Finished
    }

    @Test
    fun When_edit_outputs_finished_Then_detail_child_is_shown() {
        val bloc = createBloc(RecipeRootBloc.Props.Detail(1L))
        detailOutput.onNext(RecipeDetailBloc.Output.EditRecipe(1L))
        editOutput.onNext(EditRecipeBloc.Output.Finished(1L))
        bloc.routerState.value.active.instance.shouldBeInstanceOf<RecipeRootBloc.Child.Detail>()
    }

    @Test
    fun When_created_with_create_from_extracted_props_Then_edit_child_is_shown_with_seed() {
        val extracted = sampleExtractedRecipe()
        val bloc = createBloc(RecipeRootBloc.Props.CreateFromExtracted(extracted))
        bloc.routerState.value.active.instance.shouldBeInstanceOf<RecipeRootBloc.Child.Edit>()
        lastEditRecipeId shouldBe null
        lastEditExtractedRecipe shouldBe extracted
        lastEditFromAi shouldBe false
    }

    @Test
    fun When_created_with_create_from_extracted_from_ai_Then_edit_child_seeded_with_from_ai() {
        val extracted = sampleExtractedRecipe()
        createBloc(RecipeRootBloc.Props.CreateFromExtracted(extracted, fromAi = true))
        lastEditFromAi shouldBe true
    }

    @Test
    fun When_create_from_extracted_with_consume_pending_photo_Then_edit_child_seeded_with_flag() {
        val extracted = sampleExtractedRecipe()
        createBloc(RecipeRootBloc.Props.CreateFromExtracted(extracted, consumePendingPhoto = true))
        lastEditConsumePendingPhoto shouldBe true
    }

    @Test
    fun When_edit_outputs_cancelled_from_create_from_extracted_Then_root_outputs_finished() {
        createBloc(RecipeRootBloc.Props.CreateFromExtracted(sampleExtractedRecipe()))
        editOutput.onNext(EditRecipeBloc.Output.Cancelled)
        output.lastValue shouldBe RecipeRootBloc.Output.Finished
    }

    private fun sampleExtractedRecipe() =
        ExtractedRecipeData(
            title = "Test",
            description = null,
            ingredients = listOf("flour"),
            directions = listOf("mix"),
            imageUrl = null,
            sourceUrl = "https://example.com/recipe",
            servings = 4,
            prepTime = 10,
            cookTime = 20,
            totalTime = 30,
            calories = 200,
        )
}
