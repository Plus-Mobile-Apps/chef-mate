package com.plusmobileapps.chefmate.ui.text

import com.mohamedrejeb.richeditor.model.RichTextState
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class LineBreakTagsTest {

    @Test
    fun textWithoutTagsIsUnchanged() {
        "1 cup flour\n2 eggs\n\n- pinch of salt".withoutLineBreakTags() shouldBe
            "1 cup flour\n2 eggs\n\n- pinch of salt"
    }

    @Test
    fun standaloneTagBecomesBlankLine() {
        "Step one\n\n<br>".withoutLineBreakTags() shouldBe "Step one\n\n"
    }

    @Test
    fun consecutiveTagsEachBecomeBlankLines() {
        "Step one\n\n<br>\n<br>".withoutLineBreakTags() shouldBe "Step one\n\n\n"
    }

    @Test
    fun tagIsStrippedFromALineThatKeepsItsText() {
        "Step one<br>".withoutLineBreakTags() shouldBe "Step one"
    }

    /**
     * The bug this normalization exists for: pressing enter twice at the end of a field left a
     * literal `<br>` in the stored text that reappeared as fast as it was deleted.
     */
    @Test
    fun trailingBlankParagraphsDoNotSerializeToATag() {
        val state = RichTextState()
        state.setMarkdown("Step one\n\n")

        state.toMarkdown() shouldBe "Step one\n\n<br>"
        state.toMarkdown().withoutLineBreakTags() shouldBe "Step one\n\n"
    }

    /**
     * The editor feeds the normalized markdown straight back into the rich editor when the value
     * changes externally, so normalizing must be a fixed point — otherwise the two halves of the
     * sync fight each other on every keystroke.
     */
    @Test
    fun normalizedMarkdownRoundTripsThroughTheRichEditor() {
        listOf(
                "",
                "Step one",
                "1 cup flour\n2 eggs",
                "Step one\n\n",
                "- a\n- b\n- c",
                "1. a\n2. b",
                "**bold** step\nplain step",
            )
            .forEach { markdown ->
                val state = RichTextState()
                state.setMarkdown(markdown)

                state.toMarkdown().withoutLineBreakTags() shouldBe markdown
            }
    }
}
