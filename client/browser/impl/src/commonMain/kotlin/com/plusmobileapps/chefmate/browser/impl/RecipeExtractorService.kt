package com.plusmobileapps.chefmate.browser.impl

import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.nodes.Document
import com.fleeksoft.ksoup.nodes.Element
import com.fleeksoft.ksoup.nodes.Node
import com.fleeksoft.ksoup.nodes.TextNode
import com.fleeksoft.ksoup.parser.Parser
import com.plusmobileapps.chefmate.ChefMateUrls
import com.plusmobileapps.chefmate.di.AppScope
import com.plusmobileapps.chefmate.di.IO
import com.plusmobileapps.chefmate.recipe.data.ExtractedRecipeData
import com.plusmobileapps.chefmate.recipe.data.IngredientSection
import com.plusmobileapps.chefmate.recipe.data.Recipe
import com.plusmobileapps.chefmate.recipe.data.RecipeRepository
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

interface RecipeExtractorService {
    /**
     * @param renderedHtml The page's markup as the in-app WebView rendered it, or `null` when it
     *   couldn't be read. Preferred over fetching [url], which for a growing number of sites
     *   returns a bot-check page rather than the recipe.
     */
    suspend fun extractRecipe(url: String, renderedHtml: String?): ExtractedRecipeData
}

@SingleIn(AppScope::class)
@Inject
@ContributesBinding(AppScope::class)
class RecipeExtractorServiceImpl(
    @IO private val ioContext: CoroutineContext,
    private val recipeRepository: RecipeRepository,
) : RecipeExtractorService {

    private val httpClient = HttpClient()

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun extractRecipe(url: String, renderedHtml: String?): ExtractedRecipeData =
        withContext(ioContext) {
            // Our own share links render their recipe content client-side after load (fetched from
            // Supabase), so there's no server-rendered markup for the scraper below to parse —
            // fetch the recipe directly instead of scraping the page.
            ChefMateUrls.recipeShareUrlRemoteId(url)?.let { remoteId ->
                return@withContext recipeRepository
                    .fetchPublicRecipe(remoteId)
                    .getOrElse { throw IllegalStateException(NO_RECIPE_FOUND) }
                    .toExtractedRecipeData(url)
            }

            // The rendered DOM is strictly better source material than a fresh fetch — it's the
            // page after its scripts ran, and after whatever bot check stood in front of it.
            renderedHtml
                ?.let { parseRecipe(Ksoup.parse(it), url) }
                ?.let {
                    return@withContext it
                }

            val fetched = runCatching {
                val response = httpClient.get(url) { header("User-Agent", USER_AGENT) }
                if (!response.status.isSuccess()) {
                    throw IllegalStateException(
                        "Could not load the page (HTTP ${response.status.value})"
                    )
                }
                response.bodyAsText()
            }

            // Having already read the rendered page, a failed fetch tells the user nothing useful —
            // the page they're looking at simply had no recipe markup in it.
            val html = if (renderedHtml != null) fetched.getOrNull() else fetched.getOrThrow()

            html?.let { parseRecipe(Ksoup.parse(it), url) }
                ?: throw IllegalStateException(NO_RECIPE_FOUND)
        }

    /** Returns `null` when [document] carries no recipe markup this parser understands. */
    private fun parseRecipe(document: Document, url: String): ExtractedRecipeData? {
        for (script in document.select("script[type=application/ld+json]")) {
            val data = parseRecipeJsonText(script.data(), url) ?: continue
            // schema.org flattens grouped ingredients, so recover any sub-section headers
            // (e.g. "For the sauce:") from the page markup when present.
            val grouped = parseIngredientSections(document)
            return if (grouped != null) data.copy(ingredients = grouped) else data
        }

        // No JSON-LD recipe — fall back to microdata/microformat markup.
        return parseMicrodataRecipe(document, url)
    }

    private fun Recipe.toExtractedRecipeData(sourceUrl: String): ExtractedRecipeData =
        ExtractedRecipeData(
            title = title,
            description = description,
            ingredients = ingredients.splitLines(),
            directions = directions.splitLines(),
            imageUrl = imageUrl,
            sourceUrl = sourceUrl,
            servings = servings,
            prepTime = prepTime,
            cookTime = cookTime,
            totalTime = totalTime,
            calories = calories,
        )

    private fun String.splitLines(): List<String> = split("\n").filter { it.isNotBlank() }

    /**
     * Recovers grouped ingredients (with their sub-section headers) from WP Recipe Maker markup,
     * the most common WordPress recipe plugin. Returns `null` when the markup is absent or has no
     * named groups, so the caller can fall back to the flat JSON-LD ingredient list.
     */
    internal fun parseIngredientSections(html: String): List<String>? =
        parseIngredientSections(Ksoup.parse(html))

    private fun parseIngredientSections(document: Element): List<String>? {
        val groups = document.select("div.wprm-recipe-ingredient-group")
        if (groups.isEmpty()) return null

        val lines = mutableListOf<String>()
        var sawHeader = false
        for (group in groups) {
            val name =
                group.selectFirst(".wprm-recipe-ingredient-group-name")?.text()?.trim().orEmpty()
            if (name.isNotEmpty()) {
                lines += IngredientSection.header(name)
                sawHeader = true
            }
            for (item in group.select("li.wprm-recipe-ingredient")) {
                val text = ingredientText(item)
                if (text.isNotEmpty()) lines += text
            }
        }

        // Only worth overriding the JSON-LD list when we actually found section headers.
        return if (sawHeader && lines.isNotEmpty()) lines else null
    }

    /** Reconstructs a single WPRM ingredient line from its amount/unit/name/notes spans. */
    private fun ingredientText(item: Element): String {
        val name = item.selectFirst(".wprm-recipe-ingredient-name")?.text()?.trim().orEmpty()
        if (name.isEmpty()) {
            // Non-structured markup: fall back to the checkbox label, else the raw list text.
            return item.selectFirst("input.wprm-checkbox")?.attr("aria-label")?.trim()
                ?: item.text().trim()
        }
        val amount = item.selectFirst(".wprm-recipe-ingredient-amount")?.text()?.trim().orEmpty()
        val unit = item.selectFirst(".wprm-recipe-ingredient-unit")?.text()?.trim().orEmpty()
        val notes = item.selectFirst(".wprm-recipe-ingredient-notes")?.text()?.trim().orEmpty()
        val head = listOf(amount, unit, name).filter { it.isNotEmpty() }.joinToString(" ")
        return if (notes.isNotEmpty()) "$head, $notes" else head
    }

    /**
     * Fallback for pages that describe their recipe with schema.org **microdata** or an
     * **hrecipe/h-recipe microformat** instead of JSON-LD — most notably the Jetpack Recipe
     * shortcode used by Smitten Kitchen and other WordPress.com-hosted blogs. Returns `null` when
     * the page has no such markup, so the caller can report that no recipe was found.
     */
    internal fun parseMicrodataRecipe(html: String, sourceUrl: String): ExtractedRecipeData? =
        parseMicrodataRecipe(Ksoup.parse(html), sourceUrl)

    private fun parseMicrodataRecipe(document: Document, sourceUrl: String): ExtractedRecipeData? {
        val root = document.selectFirst(RECIPE_ROOT_SELECTOR) ?: return null

        val ingredients =
            root.select(INGREDIENT_SELECTOR).mapNotNull { it.text().trim().nullIfBlank() }
        val directions = root.selectFirst(INSTRUCTIONS_SELECTOR)?.let(::textBlocks).orEmpty()
        // Both empty means we matched some unrelated markup, not a real recipe.
        if (ingredients.isEmpty() && directions.isEmpty()) return null

        val title =
            root.selectFirst(NAME_SELECTOR)?.text()?.trim().nullIfBlank()
                ?: document.metaContent("og:title")
                ?: ""

        return ExtractedRecipeData(
            title = title,
            description =
                root.selectFirst(DESCRIPTION_SELECTOR)?.text()?.trim().nullIfBlank()
                    ?: document.metaContent("og:description"),
            ingredients = ingredients,
            directions = directions,
            imageUrl =
                root.selectFirst(IMAGE_SELECTOR)?.imageUrl() ?: document.metaContent("og:image"),
            sourceUrl = sourceUrl,
            servings = root.selectFirst(YIELD_SELECTOR)?.text().firstNumber(),
            prepTime = root.selectFirst(PREP_TIME_SELECTOR).durationMinutes(),
            cookTime = root.selectFirst(COOK_TIME_SELECTOR).durationMinutes(),
            totalTime = root.selectFirst(TOTAL_TIME_SELECTOR).durationMinutes(),
            calories = root.selectFirst(CALORIES_SELECTOR)?.text().firstNumber(),
        )
    }

    private fun Document.metaContent(property: String): String? =
        selectFirst("meta[property='$property'], meta[name='$property']")
            ?.attr("content")
            ?.trim()
            .nullIfBlank()

    /** Microdata/microformat markup carries the image url on any of these, depending on the tag. */
    private fun Element.imageUrl(): String? =
        listOf("src", "content", "href").firstNotNullOfOrNull { attr(it).trim().nullIfBlank() }

    /**
     * Durations are published as an ISO-8601 `datetime`/`title` attribute in well-formed markup,
     * but plugins routinely put prose there instead (`datetime="1 hour, plus chopping"`), so fall
     * back to reading the value as English.
     */
    private fun Element?.durationMinutes(): Int? {
        if (this == null) return null
        val candidates =
            listOf(attr("datetime").trim(), attr("title").trim(), text().trim()).filter {
                it.isNotEmpty()
            }
        return candidates.firstNotNullOfOrNull { parseDuration(it) }
            ?: candidates.firstNotNullOfOrNull { parseLooseDuration(it) }
    }

    /**
     * Splits an element's rendered text into one entry per block-level child, so a directions
     * container written as a run of `<p>`s (or `<li>`s, or `<br>`-separated text) becomes one
     * direction per line. Text sitting directly on the container counts as its own block, which
     * matters because WordPress's auto-paragraph mangling routinely leaves the first step
     * unwrapped.
     */
    private fun textBlocks(element: Element): List<String> {
        val blocks = mutableListOf<String>()
        val current = StringBuilder()

        fun flush() {
            val text = current.toString().collapseWhitespace()
            if (text.isNotEmpty()) blocks += text
            current.clear()
        }

        fun visit(node: Node) {
            when (node) {
                is TextNode -> current.append(node.text())
                is Element -> {
                    val isBlock = node.tagName() in BLOCK_TAGS
                    if (isBlock) flush()
                    node.childNodes().forEach(::visit)
                    if (isBlock) flush()
                }
            }
        }

        element.childNodes().forEach(::visit)
        flush()
        return blocks
    }

    private fun findRecipeJson(jsonText: String): JsonObject? =
        findRecipeInElement(json.parseToJsonElement(jsonText))

    private fun findRecipeInElement(element: JsonElement): JsonObject? {
        return when (element) {
            is JsonObject -> {
                val type = element["@type"]
                val isRecipe =
                    when (type) {
                        null -> false
                        is JsonArray -> type.any { it.stringOrNull() == "Recipe" }
                        else -> type.stringOrNull() == "Recipe"
                    }
                if (isRecipe) {
                    element
                } else {
                    // Check @graph array
                    val graph = element["@graph"]
                    if (graph is JsonArray) {
                        graph.firstNotNullOfOrNull { findRecipeInElement(it) }
                    } else {
                        null
                    }
                }
            }
            is JsonArray -> element.firstNotNullOfOrNull { findRecipeInElement(it) }
            else -> null
        }
    }

    /**
     * Returns `null` for anything that isn't a well-formed schema.org `Recipe`, including malformed
     * JSON and unexpected value shapes, so one bad `ld+json` block on a page can't stop us from
     * trying the remaining blocks — or the microdata fallback.
     */
    internal fun parseRecipeJsonText(jsonText: String, sourceUrl: String): ExtractedRecipeData? =
        runCatching {
            findRecipeJson(jsonText)?.let { parseRecipeFromJson(it, sourceUrl) }
        }
        .getOrNull()

    private fun String.decodeHtmlEntities(): String = Parser.unescapeEntities(this, false)

    private fun parseRecipeFromJson(obj: JsonObject, sourceUrl: String): ExtractedRecipeData {
        // `headline` is the fallback for publishers that only title the article, not the recipe.
        val title =
            (obj["name"].stringOrNull() ?: obj["headline"].stringOrNull())?.decodeHtmlEntities()
                ?: ""
        val description = obj["description"].stringOrNull()?.decodeHtmlEntities()

        val ingredients =
            (obj["recipeIngredient"] ?: obj["ingredients"]).stringList().map {
                it.decodeHtmlEntities()
            }

        val directions = parseDirections(obj)

        val imageUrl =
            when (val img = obj["image"]) {
                is JsonArray -> img.firstOrNull()?.let { extractImageUrl(it) }
                is JsonObject -> extractImageUrl(img)
                else -> img.stringOrNull()
            }

        val servings = parseServings(obj)
        val prepTime = obj["prepTime"].durationMinutes()
        val cookTime = obj["cookTime"].durationMinutes()
        val totalTime = obj["totalTime"].durationMinutes()
        val calories = parseCalories(obj)

        return ExtractedRecipeData(
            title = title,
            description = description,
            ingredients = ingredients,
            directions = directions,
            imageUrl = imageUrl,
            sourceUrl = sourceUrl,
            servings = servings,
            prepTime = prepTime,
            cookTime = cookTime,
            totalTime = totalTime,
            calories = calories,
        )
    }

    private fun parseDirections(obj: JsonObject): List<String> {
        val instructions = obj["recipeInstructions"] ?: return emptyList()
        return when (instructions) {
            is JsonArray ->
                instructions.flatMap { element ->
                    when {
                        element !is JsonObject ->
                            listOfNotNull(element.stringOrNull()?.decodeHtmlEntities())
                        element["@type"].stringOrNull() == "HowToSection" ->
                            (element["itemListElement"] as? JsonArray)?.mapNotNull { item ->
                                (item as? JsonObject)?.get("text").stringOrNull()
                            } ?: emptyList()
                        // Anything else object-shaped is a step: HowToStep, CreativeWork, or an
                        // untyped `{"text": …}`. Read `text`, falling back to `name` for the
                        // publishers that put the step body there instead.
                        else ->
                            listOfNotNull(
                                element["text"].stringOrNull() ?: element["name"].stringOrNull()
                            )
                    }.map { it.decodeHtmlEntities() }
                }
            else -> listOfNotNull(instructions.stringOrNull()?.decodeHtmlEntities())
        }
    }

    private fun extractImageUrl(element: JsonElement): String? =
        when (element) {
            is JsonObject -> element["url"].stringOrNull()
            else -> element.stringOrNull()
        }

    private fun parseServings(obj: JsonObject): Int? =
        when (val yield = obj["recipeYield"]) {
            null -> null
            is JsonArray -> yield.firstOrNull().stringOrNull().firstNumber()
            else -> yield.stringOrNull().firstNumber()
        }

    private fun parseCalories(obj: JsonObject): Int? =
        (obj["nutrition"] as? JsonObject)?.get("calories").stringOrNull().firstNumber()

    companion object {
        // A real mobile-browser User-Agent. Some sites' WAFs return 403 to bot-style or
        // spoofed desktop-Chrome User-Agents, but accept genuine mobile-browser strings.
        private const val NO_RECIPE_FOUND = "No recipe data found on this page"

        private const val USER_AGENT =
            "Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) AppleWebKit/605.1.15 " +
                "(KHTML, like Gecko) Version/17.0 Mobile/15E148 Safari/604.1"

        // schema.org microdata first, then the older hrecipe/h-recipe microformat class names.
        private const val RECIPE_ROOT_SELECTOR =
            "[itemtype*=schema.org/Recipe], .hrecipe, .h-recipe"
        private const val NAME_SELECTOR = "[itemprop=name], .fn, .p-name"
        private const val DESCRIPTION_SELECTOR =
            "[itemprop=description], .summary, .p-summary, .e-summary"
        private const val INGREDIENT_SELECTOR =
            "[itemprop=recipeIngredient], [itemprop=ingredients], .ingredient, .p-ingredient"
        private const val INSTRUCTIONS_SELECTOR =
            "[itemprop=recipeInstructions], .instructions, .e-instructions"
        private const val IMAGE_SELECTOR = "[itemprop=image], .photo, .u-photo"
        private const val YIELD_SELECTOR = "[itemprop=recipeYield], .yield, .p-yield"
        private const val PREP_TIME_SELECTOR = "[itemprop=prepTime], .preptime, .dt-preptime"
        private const val COOK_TIME_SELECTOR = "[itemprop=cookTime], .cooktime, .dt-cooktime"
        private const val TOTAL_TIME_SELECTOR =
            "[itemprop=totalTime], .totaltime, .dt-totaltime, .duration, .dt-duration"
        private const val CALORIES_SELECTOR = "[itemprop=calories]"

        private val BLOCK_TAGS =
            setOf("p", "li", "div", "br", "tr", "h1", "h2", "h3", "h4", "h5", "h6")

        private val WHITESPACE = Regex("\\s+")
        private val NUMBER = Regex("\\d[\\d,]*")
        // "1 hour", "1 hr 20 min", "1.5 hours", "45 minutes"
        private val LOOSE_DURATION =
            Regex(
                "(\\d+(?:\\.\\d+)?)\\s*(hours?|hrs?|h|minutes?|mins?|m)\\b",
                RegexOption.IGNORE_CASE,
            )

        private fun String?.nullIfBlank(): String? = this?.takeIf { it.isNotBlank() }

        /**
         * schema.org values are only *conventionally* strings — publishers routinely nest an object
         * or an array where the spec suggests text. Reading them through this (rather than
         * `jsonPrimitive`, which throws) keeps one oddly-shaped field from failing the whole
         * recipe.
         */
        private fun JsonElement?.stringOrNull(): String? = (this as? JsonPrimitive)?.contentOrNull

        /** Reads a field that may be published as a single string or a list of them. */
        private fun JsonElement?.stringList(): List<String> =
            when (this) {
                null -> emptyList()
                is JsonArray -> mapNotNull { it.stringOrNull() }
                else -> listOfNotNull(stringOrNull())
            }

        /**
         * A schema.org duration is usually a bare ISO-8601 string, but a recipe that publishes a
         * *range* — Serious Eats' sous vide burgers cook for anywhere from 45 minutes to 4 hours —
         * uses a `Duration` object with `minValue`/`maxValue` instead. Take the lower bound, the
         * same end of a range [firstNumber] takes for a "6 to 8" yield.
         */
        private fun JsonElement?.durationMinutes(): Int? =
            when (this) {
                null -> null
                is JsonArray -> firstOrNull().durationMinutes()
                is JsonObject ->
                    parseDuration(this["minValue"].stringOrNull())
                        ?: parseDuration(this["value"].stringOrNull())
                        ?: parseDuration(this["maxValue"].stringOrNull())
                else -> parseDuration(stringOrNull())
            }

        private fun String.collapseWhitespace(): String = replace(WHITESPACE, " ").trim()

        /**
         * Reads the leading count out of a free-form value. Yields are routinely written as a range
         * ("Servings: 6 to 8"), where stripping every non-digit would produce a nonsense 68.
         */
        private fun String?.firstNumber(): Int? =
            this?.let { NUMBER.find(it)?.value?.replace(",", "")?.toIntOrNull() }

        /** Parses an English duration ("1 hour, plus chopping") into minutes. */
        fun parseLooseDuration(text: String?): Int? {
            if (text == null) return null
            // Trailing "plus …" clauses describe unattended time we shouldn't add to the total.
            val head = text.split(Regex("\\bplus\\b", RegexOption.IGNORE_CASE)).first()
            var minutes = 0.0
            for (match in LOOSE_DURATION.findAll(head)) {
                val value = match.groupValues[1].toDoubleOrNull() ?: continue
                val isHours = match.groupValues[2].lowercase().startsWith("h")
                minutes += if (isHours) value * 60 else value
            }
            return minutes.toInt().takeIf { it > 0 }
        }

        fun parseDuration(iso8601: String?): Int? {
            if (iso8601 == null) return null
            var minutes = 0
            val upper = iso8601.uppercase()
            if (!upper.startsWith("PT")) return null
            val remaining = upper.removePrefix("PT")

            val hourMatch = Regex("(\\d+)H").find(remaining)
            if (hourMatch != null) minutes += hourMatch.groupValues[1].toInt() * 60

            val minMatch = Regex("(\\d+)M").find(remaining)
            if (minMatch != null) minutes += minMatch.groupValues[1].toInt()

            return if (minutes > 0) minutes else null
        }
    }
}
