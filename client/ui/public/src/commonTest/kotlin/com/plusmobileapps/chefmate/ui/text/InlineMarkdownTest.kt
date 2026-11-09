package com.plusmobileapps.chefmate.ui.text

import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class InlineMarkdownTest {

    @Test
    fun plainTextHasNoSpans() {
        val result = "2 cups flour".toInlineMarkdownAnnotatedString()
        result.text shouldBe "2 cups flour"
        result.spanStyles.size shouldBe 0
    }

    @Test
    fun boldMarkersAreStrippedAndStyled() {
        val result = "**preheat** the oven".toInlineMarkdownAnnotatedString()
        result.text shouldBe "preheat the oven"
        result.spanStyles.size shouldBe 1
        val span = result.spanStyles.single()
        span.start shouldBe 0
        span.end shouldBe 7
        span.item.fontWeight shouldBe FontWeight.Bold
    }

    @Test
    fun italicMarkersAreStrippedAndStyled() {
        val result = "stir _gently_".toInlineMarkdownAnnotatedString()
        result.text shouldBe "stir gently"
        val span = result.spanStyles.single()
        span.start shouldBe 5
        span.end shouldBe 11
        span.item.fontStyle shouldBe FontStyle.Italic
    }

    @Test
    fun boldCanNestItalic() {
        val result = "**bold _and italic_**".toInlineMarkdownAnnotatedString()
        result.text shouldBe "bold and italic"
        val bold = result.spanStyles.first { it.item.fontWeight == FontWeight.Bold }
        bold.start shouldBe 0
        bold.end shouldBe 15
        val italic = result.spanStyles.first { it.item.fontStyle == FontStyle.Italic }
        italic.start shouldBe 5
        italic.end shouldBe 15
    }

    @Test
    fun unmatchedMarkerIsLiteral() {
        val result = "**not closed".toInlineMarkdownAnnotatedString()
        result.text shouldBe "**not closed"
        result.spanStyles.size shouldBe 0
    }

    @Test
    fun strayItalicMarkerIsLiteral() {
        val result = "salt _ pepper".toInlineMarkdownAnnotatedString()
        result.text shouldBe "salt _ pepper"
        result.spanStyles.size shouldBe 0
    }

    @Test
    fun linkWithoutHandlerRendersPlainLabel() {
        val result = "add [Green Sauce](chefmate://recipe/abc)".toInlineMarkdownAnnotatedString()
        result.text shouldBe "add Green Sauce"
        result.getLinkAnnotations(0, result.length).size shouldBe 0
    }

    @Test
    fun linkWithHandlerStripsMarkdownAndAnnotates() {
        var clicked: String? = null
        val result =
            "add [Green Sauce](chefmate://recipe/abc)"
                .toInlineMarkdownAnnotatedString(onLinkClick = { clicked = it })

        result.text shouldBe "add Green Sauce"
        val link = result.getLinkAnnotations(0, result.length).single()
        link.start shouldBe 4
        link.end shouldBe 15
        val clickable = link.item as LinkAnnotation.Clickable
        clickable.tag shouldBe "chefmate://recipe/abc"

        clickable.linkInteractionListener!!.onClick(clickable)
        clicked shouldBe "chefmate://recipe/abc"
    }

    @Test
    fun linkLabelCanContainInlineStyles() {
        val result =
            "[**Bold** sauce](chefmate://recipe/abc)"
                .toInlineMarkdownAnnotatedString(onLinkClick = {})
        result.text shouldBe "Bold sauce"
        result.getLinkAnnotations(0, result.length).size shouldBe 1
        result.spanStyles.single().item.fontWeight shouldBe FontWeight.Bold
    }

    @Test
    fun unmatchedLinkSyntaxIsLiteral() {
        val result = "see [Sauce] later".toInlineMarkdownAnnotatedString(onLinkClick = {})
        result.text shouldBe "see [Sauce] later"
        result.getLinkAnnotations(0, result.length).size shouldBe 0
    }

    @Test
    fun bareUrlWithHandlerIsLinked() {
        var clicked: String? = null
        val result =
            "Adapted from https://example.com/recipes/green_sauce today"
                .toInlineMarkdownAnnotatedString(onLinkClick = { clicked = it })

        result.text shouldBe "Adapted from https://example.com/recipes/green_sauce today"
        result.spanStyles.size shouldBe 0
        val link = result.getLinkAnnotations(0, result.length).single()
        link.start shouldBe 13
        link.end shouldBe 52
        val clickable = link.item as LinkAnnotation.Clickable
        clickable.linkInteractionListener!!.onClick(clickable)
        clicked shouldBe "https://example.com/recipes/green_sauce"
    }

    @Test
    fun bareUrlDropsTrailingPunctuation() {
        val result = "See http://example.com/a.".toInlineMarkdownAnnotatedString(onLinkClick = {})
        val link = result.getLinkAnnotations(0, result.length).single()
        (link.item as LinkAnnotation.Clickable).tag shouldBe "http://example.com/a"
    }

    @Test
    fun bareUrlWithoutHandlerKeepsUnderscoresLiteral() {
        val result = "via https://example.com/a_b_c".toInlineMarkdownAnnotatedString()
        result.text shouldBe "via https://example.com/a_b_c"
        result.spanStyles.size shouldBe 0
        result.getLinkAnnotations(0, result.length).size shouldBe 0
    }

    @Test
    fun schemeInsideWordIsNotLinked() {
        val result = "xhttps://example.com".toInlineMarkdownAnnotatedString(onLinkClick = {})
        result.getLinkAnnotations(0, result.length).size shouldBe 0
    }
}
