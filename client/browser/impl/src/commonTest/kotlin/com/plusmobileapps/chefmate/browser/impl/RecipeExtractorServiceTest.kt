@file:Suppress("FunctionName")

package com.plusmobileapps.chefmate.browser.impl

import com.plusmobileapps.chefmate.ChefMateUrls
import com.plusmobileapps.chefmate.recipe.data.Recipe
import com.plusmobileapps.chefmate.recipe.data.testing.FakeRecipeRepository
import io.kotest.matchers.shouldBe
import kotlin.test.Test
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalTime::class)
class RecipeExtractorServiceTest {

    private val recipeRepository = FakeRecipeRepository()
    private val service = RecipeExtractorServiceImpl(Dispatchers.Default, recipeRepository)

    // region parseDuration

    @Test
    fun parseDuration_null_returns_null() {
        RecipeExtractorServiceImpl.parseDuration(null) shouldBe null
    }

    @Test
    fun parseDuration_hours_only() {
        RecipeExtractorServiceImpl.parseDuration("PT2H") shouldBe 120
    }

    @Test
    fun parseDuration_minutes_only() {
        RecipeExtractorServiceImpl.parseDuration("PT30M") shouldBe 30
    }

    @Test
    fun parseDuration_hours_and_minutes() {
        RecipeExtractorServiceImpl.parseDuration("PT1H15M") shouldBe 75
    }

    @Test
    fun parseDuration_zero_minutes_returns_null() {
        RecipeExtractorServiceImpl.parseDuration("PT0M") shouldBe null
    }

    @Test
    fun parseDuration_invalid_format_returns_null() {
        RecipeExtractorServiceImpl.parseDuration("30 minutes") shouldBe null
    }

    // endregion

    // region parseLooseDuration

    @Test
    fun parseLooseDuration_null_returns_null() {
        RecipeExtractorServiceImpl.parseLooseDuration(null) shouldBe null
    }

    @Test
    fun parseLooseDuration_hours_only() {
        RecipeExtractorServiceImpl.parseLooseDuration("1 hour") shouldBe 60
    }

    @Test
    fun parseLooseDuration_hours_and_minutes() {
        RecipeExtractorServiceImpl.parseLooseDuration("1 hr 20 min") shouldBe 80
    }

    @Test
    fun parseLooseDuration_fractional_hours() {
        RecipeExtractorServiceImpl.parseLooseDuration("1.5 hours") shouldBe 90
    }

    @Test
    fun parseLooseDuration_ignores_trailing_plus_clause() {
        RecipeExtractorServiceImpl.parseLooseDuration("1 hour, plus 8 hours chilling") shouldBe 60
    }

    @Test
    fun parseLooseDuration_without_a_duration_returns_null() {
        RecipeExtractorServiceImpl.parseLooseDuration("overnight") shouldBe null
    }

    // endregion

    // region HTML entity decoding

    @Test
    fun When_title_has_html_entity_apostrophe_Then_decoded() {
        val json = recipeJson(name = "The World&#39;s Easiest Cranberry Sauce")
        val recipe = service.parseRecipeJsonText(json, SOURCE_URL)
        recipe?.title shouldBe "The World's Easiest Cranberry Sauce"
    }

    @Test
    fun When_title_has_amp_entity_Then_decoded() {
        val json = recipeJson(name = "Mac &amp; Cheese")
        val recipe = service.parseRecipeJsonText(json, SOURCE_URL)
        recipe?.title shouldBe "Mac & Cheese"
    }

    @Test
    fun When_description_has_html_entity_Then_decoded() {
        val json = recipeJson(description = "It&#39;s the best recipe")
        val recipe = service.parseRecipeJsonText(json, SOURCE_URL)
        recipe?.description shouldBe "It's the best recipe"
    }

    @Test
    fun When_ingredient_has_html_entity_Then_decoded() {
        val json = recipeJson(ingredients = listOf("1 cup chef&#39;s choice broth"))
        val recipe = service.parseRecipeJsonText(json, SOURCE_URL)
        recipe?.ingredients shouldBe listOf("1 cup chef's choice broth")
    }

    @Test
    fun When_HowToStep_direction_has_html_entity_Then_decoded() {
        val json =
            recipeJson(
                instructions = """[{"@type":"HowToStep","text":"Don&#39;t overcook the sauce"}]"""
            )
        val recipe = service.parseRecipeJsonText(json, SOURCE_URL)
        recipe?.directions shouldBe listOf("Don't overcook the sauce")
    }

    @Test
    fun When_HowToSection_direction_has_html_entity_Then_decoded() {
        val json =
            recipeJson(
                instructions =
                    """[{"@type":"HowToSection","itemListElement":[{"@type":"HowToStep","text":"It&#39;s step one"}]}]"""
            )
        val recipe = service.parseRecipeJsonText(json, SOURCE_URL)
        recipe?.directions shouldBe listOf("It's step one")
    }

    @Test
    fun When_plain_string_direction_has_html_entity_Then_decoded() {
        val json = recipeJson(instructions = """["Stir &amp; serve"]""")
        val recipe = service.parseRecipeJsonText(json, SOURCE_URL)
        recipe?.directions shouldBe listOf("Stir & serve")
    }

    @Test
    fun When_recipe_json_has_no_recipe_type_Then_returns_null() {
        val json = """{"@type":"Article","name":"Not a recipe"}"""
        service.parseRecipeJsonText(json, SOURCE_URL) shouldBe null
    }

    @Test
    fun When_recipe_is_nested_in_graph_Then_parsed() {
        val json =
            """{"@graph":[{"@type":"Recipe","name":"Soup &amp; Salad","recipeIngredient":[],"recipeInstructions":[]}]}"""
        val recipe = service.parseRecipeJsonText(json, SOURCE_URL)
        recipe?.title shouldBe "Soup & Salad"
    }

    @Test
    fun When_recipe_yield_is_a_range_Then_the_lower_bound_is_used() {
        val json = """{"@type":"Recipe","name":"Chili","recipeYield":"6 to 8 servings"}"""
        service.parseRecipeJsonText(json, SOURCE_URL)?.servings shouldBe 6
    }

    @Test
    fun When_calories_are_comma_separated_Then_parsed_as_one_number() {
        val json = """{"@type":"Recipe","name":"Feast","nutrition":{"calories":"1,240 kcal"}}"""
        service.parseRecipeJsonText(json, SOURCE_URL)?.calories shouldBe 1240
    }

    // endregion

    // region non-string field shapes

    @Test
    fun When_duration_is_a_range_object_Then_the_lower_bound_is_used() {
        val json =
            """{"@type":["Recipe"],"name":"Sous Vide Burgers","recipeIngredient":["Beef"],
               "cookTime":{"@type":"Duration","minValue":"PT45M","maxValue":"PT245M"},
               "totalTime":{"@type":"Duration","minValue":"PT65M","maxValue":"PT265M"},
               "prepTime":"PT10M"}"""
        val recipe = service.parseRecipeJsonText(json, SOURCE_URL)
        recipe?.cookTime shouldBe 45
        recipe?.totalTime shouldBe 65
        recipe?.prepTime shouldBe 10
    }

    @Test
    fun When_duration_is_an_object_Then_the_rest_of_the_recipe_still_parses() {
        val json =
            """{"@type":"Recipe","name":"Braise","recipeIngredient":["Beef"],
               "recipeInstructions":["Sear"],"totalTime":{"@type":"Duration","maxValue":"PT4H"}}"""
        val recipe = service.parseRecipeJsonText(json, SOURCE_URL)
        recipe?.title shouldBe "Braise"
        recipe?.ingredients shouldBe listOf("Beef")
        recipe?.directions shouldBe listOf("Sear")
        recipe?.totalTime shouldBe 240
    }

    @Test
    fun When_title_is_only_a_headline_Then_used_as_the_title() {
        val json = """{"@type":["Recipe"],"headline":"Halal Cart Chicken","recipeIngredient":[]}"""
        service.parseRecipeJsonText(json, SOURCE_URL)?.title shouldBe "Halal Cart Chicken"
    }

    @Test
    fun When_a_step_object_is_untyped_Then_its_text_is_still_read() {
        val json = recipeJson(instructions = """[{"text":"Sear the beef"},{"name":"Rest it"}]""")
        service.parseRecipeJsonText(json, SOURCE_URL)?.directions shouldBe
            listOf("Sear the beef", "Rest it")
    }

    @Test
    fun When_ingredients_are_a_single_string_Then_parsed_as_one_entry() {
        val json = """{"@type":"Recipe","name":"Toast","recipeIngredient":"1 slice bread"}"""
        service.parseRecipeJsonText(json, SOURCE_URL)?.ingredients shouldBe listOf("1 slice bread")
    }

    @Test
    fun When_nutrition_is_not_an_object_Then_calories_are_null_and_parsing_continues() {
        val json =
            """{"@type":"Recipe","name":"Snack","recipeIngredient":["Nuts"],
                       "nutrition":["not an object"]}"""
        val recipe = service.parseRecipeJsonText(json, SOURCE_URL)
        recipe?.calories shouldBe null
        recipe?.ingredients shouldBe listOf("Nuts")
    }

    // endregion

    // region ingredient sections

    @Test
    fun When_ingredients_have_named_groups_Then_headers_are_extracted() {
        val html =
            wprmIngredients(
                group(
                    name = "For the chicken",
                    ingredients = listOf(amountUnitName("1", "lb", "chicken thighs")),
                ),
                group(
                    name = "For the green sauce",
                    ingredients = listOf(amountUnitName("½", "cup", "sour cream")),
                ),
            )
        service.parseIngredientSections(html) shouldBe
            listOf(
                "For the chicken:",
                "1 lb chicken thighs",
                "For the green sauce:",
                "½ cup sour cream",
            )
    }

    @Test
    fun When_first_group_is_unnamed_Then_only_named_section_gets_a_header() {
        val html =
            wprmIngredients(
                group(
                    name = "",
                    ingredients = listOf(amountUnitName("2", "Tablespoons", "avocado oil")),
                ),
                group(
                    name = "Sauce Ingredients",
                    ingredients = listOf(amountUnitName("3", "Tablespoons", "soy sauce")),
                ),
            )
        service.parseIngredientSections(html) shouldBe
            listOf("2 Tablespoons avocado oil", "Sauce Ingredients:", "3 Tablespoons soy sauce")
    }

    @Test
    fun When_no_group_is_named_Then_returns_null_to_fall_back_to_json_ld() {
        val html =
            wprmIngredients(
                group(name = "", ingredients = listOf(amountUnitName("1", "cup", "flour")))
            )
        service.parseIngredientSections(html) shouldBe null
    }

    @Test
    fun When_markup_is_absent_Then_returns_null() {
        service.parseIngredientSections("<html><body><p>no recipe</p></body></html>") shouldBe null
    }

    @Test
    fun When_ingredient_has_notes_Then_appended_after_comma() {
        val html =
            wprmIngredients(
                group(
                    name = "Sauce",
                    ingredients =
                        listOf(
                            """<span class="wprm-recipe-ingredient-amount">1</span>""" +
                                """<span class="wprm-recipe-ingredient-name">tofu</span>""" +
                                """<span class="wprm-recipe-ingredient-notes">16 oz</span>"""
                        ),
                )
            )
        service.parseIngredientSections(html) shouldBe listOf("Sauce:", "1 tofu, 16 oz")
    }

    private fun amountUnitName(amount: String, unit: String, name: String): String =
        """<span class="wprm-recipe-ingredient-amount">$amount</span>""" +
            """<span class="wprm-recipe-ingredient-unit">$unit</span>""" +
            """<span class="wprm-recipe-ingredient-name">$name</span>"""

    private fun group(name: String, ingredients: List<String>): String {
        val heading =
            if (name.isEmpty()) {
                ""
            } else {
                """<h4 class="wprm-recipe-group-name wprm-recipe-ingredient-group-name">$name</h4>"""
            }
        val items =
            ingredients.joinToString("") { """<li class="wprm-recipe-ingredient">$it</li>""" }
        return """<div class="wprm-recipe-ingredient-group">$heading<ul class="wprm-recipe-ingredients">$items</ul></div>"""
    }

    private fun wprmIngredients(vararg groups: String): String =
        """<html><body>${groups.joinToString("")}</body></html>"""

    // endregion

    // region microdata / hrecipe fallback

    @Test
    fun When_page_has_no_json_ld_Then_hrecipe_markup_is_extracted() {
        val recipe = service.parseMicrodataRecipe(JETPACK_RECIPE_PAGE, SOURCE_URL)

        recipe?.title shouldBe "Baked Farro with Summer Vegetables"
        recipe?.ingredients shouldBe
            listOf("Olive oil", "Kosher salt", "1 cup (210 grams) uncooked farro")
        recipe?.directions shouldBe
            listOf(
                "If you have an ovenproof 11-inch pan with a lid, use it here.",
                "Heat your oven to 375°F.",
                "Bake for 30 to 40 minutes, until the farro is cooked.",
            )
        recipe?.sourceUrl shouldBe SOURCE_URL
    }

    @Test
    fun When_hrecipe_yield_is_a_range_Then_the_lower_bound_is_used() {
        // "Servings: 6 to 8" — stripping non-digits would give a nonsense 68.
        service.parseMicrodataRecipe(JETPACK_RECIPE_PAGE, SOURCE_URL)?.servings shouldBe 6
    }

    @Test
    fun When_hrecipe_time_is_prose_Then_it_is_read_as_english() {
        // The Jetpack shortcode puts prose in datetime, so ISO-8601 parsing can't apply.
        service.parseMicrodataRecipe(JETPACK_RECIPE_PAGE, SOURCE_URL)?.totalTime shouldBe 60
    }

    @Test
    fun When_hrecipe_has_no_image_or_description_Then_open_graph_tags_are_used() {
        val recipe = service.parseMicrodataRecipe(JETPACK_RECIPE_PAGE, SOURCE_URL)

        recipe?.imageUrl shouldBe "https://example.com/farro.jpg"
        recipe?.description shouldBe "A pinnacle-of-summer baked grain dish."
    }

    @Test
    fun When_page_has_no_recipe_markup_at_all_Then_returns_null() {
        val html = "<html><body><p>Just a blog post.</p></body></html>"
        service.parseMicrodataRecipe(html, SOURCE_URL) shouldBe null
    }

    @Test
    fun When_recipe_root_has_no_ingredients_or_directions_Then_returns_null() {
        val html =
            """<html><body><div class="hrecipe"><h3 class="fn">Just a title</h3></div></body></html>"""
        service.parseMicrodataRecipe(html, SOURCE_URL) shouldBe null
    }

    @Test
    fun When_directions_use_list_items_Then_each_becomes_a_step() {
        val html =
            """
            |<html><body><div class="hrecipe">
            |<li class="ingredient">1 cup flour</li>
            |<ol class="instructions"><li>Mix it.</li><li>Bake it.</li></ol>
            |</div></body></html>
            """
                .trimMargin()

        service.parseMicrodataRecipe(html, SOURCE_URL)?.directions shouldBe
            listOf("Mix it.", "Bake it.")
    }

    @Test
    fun When_directions_are_separated_by_line_breaks_Then_each_becomes_a_step() {
        val html =
            """
            |<html><body><div class="hrecipe">
            |<li class="ingredient">1 cup flour</li>
            |<div class="instructions">Mix it.<br>Bake it.</div>
            |</div></body></html>
            """
                .trimMargin()

        service.parseMicrodataRecipe(html, SOURCE_URL)?.directions shouldBe
            listOf("Mix it.", "Bake it.")
    }

    @Test
    fun When_a_json_ld_script_is_malformed_Then_it_does_not_break_extraction() {
        service.parseRecipeJsonText("{ not json", SOURCE_URL) shouldBe null
    }

    /**
     * Mirrors the markup Smitten Kitchen (and other Jetpack Recipe shortcode blogs) publishes:
     * schema.org microdata plus hrecipe classes, no JSON-LD, and WordPress's auto-paragraph
     * mangling that leaves the first direction unwrapped and `<p>` tags interleaved with `</div>`.
     */
    private val JETPACK_RECIPE_PAGE =
        """
        |<html><head>
        |<meta property="og:description" content="A pinnacle-of-summer baked grain dish." />
        |<meta property="og:image" content="https://example.com/farro.jpg" />
        |</head><body>
        |<div class="hrecipe h-recipe jetpack-recipe" itemscope itemtype="https://schema.org/Recipe"><h3 class="p-name jetpack-recipe-title fn" itemprop="name">Baked Farro with Summer Vegetables</h3><ul class="jetpack-recipe-meta"><li class="jetpack-recipe-servings p-yield yield" itemprop="recipeYield"><strong>Servings: </strong>6 to 8</li><li class="jetpack-recipe-time"><time itemprop="totalTime" datetime="1 hour, plus chopping"><strong>Time:</strong> <span class="time">1 hour, plus chopping</span></time></li></ul><div class="jetpack-recipe-content"></p>
        |<p><div class="jetpack-recipe-ingredients"><ul><li class="jetpack-recipe-ingredient p-ingredient ingredient" itemprop="recipeIngredient">Olive oil</li><li class="jetpack-recipe-ingredient p-ingredient ingredient" itemprop="recipeIngredient">Kosher salt</li><li class="jetpack-recipe-ingredient p-ingredient ingredient" itemprop="recipeIngredient">1 cup (210 grams) uncooked farro</li></ul></div></p>
        |<p><div class="jetpack-recipe-directions e-instructions">If you have an ovenproof 11-inch pan with a lid, use it here.</p>
        |<p>Heat your oven to 375°F.</p>
        |<p>Bake for 30 to 40 minutes, until the farro is cooked.</p>
        |<p></div></p>
        |<p></div></div>
        |</body></html>
        """
            .trimMargin()

    // endregion

    // region own share links

    @Test
    fun When_url_is_our_own_share_link_Then_fetches_the_public_recipe_directly() = runTest {
        val remoteId = "08c5daa3-5de0-4045-a660-ea396ce25d3e"
        val url = ChefMateUrls.recipeShareUrl(remoteId)
        recipeRepository.publicRecipes[remoteId] =
            testRecipe(
                title = "Weeknight Tacos",
                ingredients = "For the tacos:\n1 lb ground beef\n8 tortillas",
                directions = "Brown the beef.\nWarm the tortillas.",
            )

        val extracted = service.extractRecipe(url, renderedHtml = null)

        extracted.title shouldBe "Weeknight Tacos"
        extracted.ingredients shouldBe listOf("For the tacos:", "1 lb ground beef", "8 tortillas")
        extracted.directions shouldBe listOf("Brown the beef.", "Warm the tortillas.")
        extracted.sourceUrl shouldBe url
    }

    @Test
    fun When_our_own_share_link_has_no_matching_public_recipe_Then_throws() = runTest {
        val url = ChefMateUrls.recipeShareUrl("missing-id")

        val error = runCatching {
            service.extractRecipe(url, renderedHtml = null)
        }
            .exceptionOrNull()

        error?.message shouldBe "No recipe data found on this page"
    }

    // endregion

    // region rendered WebView HTML

    @Test
    fun When_the_rendered_page_has_a_recipe_Then_it_is_used_without_fetching_the_url() = runTest {
        // Any real fetch of this host would fail the test by throwing, which is the point: sites
        // behind a bot check only ever yield their recipe through the WebView.
        val extracted =
            service.extractRecipe(
                "https://blocked.invalid/recipe",
                renderedHtml =
                    """<html><head><script type="application/ld+json">
                       {"@type":"Recipe","name":"Rendered Roast",
                        "recipeIngredient":["1 roast"],"recipeInstructions":["Roast it"]}
                       </script></head><body></body></html>""",
            )

        extracted.title shouldBe "Rendered Roast"
        extracted.ingredients shouldBe listOf("1 roast")
        extracted.directions shouldBe listOf("Roast it")
        extracted.sourceUrl shouldBe "https://blocked.invalid/recipe"
    }

    @Test
    fun When_the_rendered_page_has_only_microdata_Then_it_is_still_used() = runTest {
        val extracted =
            service.extractRecipe(
                "https://blocked.invalid/recipe",
                renderedHtml =
                    """<html><body><div class="hrecipe">
                       <h1 class="fn">Rendered Stew</h1>
                       <span class="ingredient">2 carrots</span>
                       <div class="instructions"><p>Simmer.</p></div>
                       </div></body></html>""",
            )

        extracted.title shouldBe "Rendered Stew"
        extracted.ingredients shouldBe listOf("2 carrots")
        extracted.directions shouldBe listOf("Simmer.")
    }

    @Test
    fun When_the_rendered_page_has_no_recipe_and_the_url_cannot_be_fetched_Then_reports_no_recipe() =
        runTest {
            val error = runCatching {
                service.extractRecipe(
                    "https://blocked.invalid/not-a-recipe",
                    renderedHtml = "<html><body><p>Just an article.</p></body></html>",
                )
            }
                .exceptionOrNull()

            // Not the HTTP failure: we already had the page the user is looking at, and it simply
            // had no recipe in it.
            error?.message shouldBe "No recipe data found on this page"
        }

    // endregion

    private fun testRecipe(title: String, ingredients: String, directions: String) =
        Recipe(
            id = 1L,
            title = title,
            description = null,
            ingredients = ingredients,
            directions = directions,
            imageUrl = null,
            sourceUrl = null,
            servings = null,
            prepTime = null,
            cookTime = null,
            totalTime = null,
            calories = null,
            starRating = null,
            remoteId = "08c5daa3-5de0-4045-a660-ea396ce25d3e",
            isPublic = true,
            createdAt = Instant.DISTANT_PAST,
            updatedAt = Instant.DISTANT_PAST,
        )

    private fun recipeJson(
        name: String = "Test Recipe",
        description: String? = null,
        ingredients: List<String> = emptyList(),
        instructions: String = "[]",
    ): String {
        val descriptionField = if (description != null) """"description":"$description",""" else ""
        val ingredientsJson = ingredients.joinToString(",") { "\"$it\"" }
        return """{"@type":"Recipe","name":"$name",$descriptionField"recipeIngredient":[$ingredientsJson],"recipeInstructions":$instructions}"""
    }

    companion object {
        private const val SOURCE_URL = "https://example.com/recipe"
    }
}
