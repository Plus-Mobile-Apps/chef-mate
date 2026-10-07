package com.plusmobileapps.chefmate.ui.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import com.plusmobileapps.chefmate.text.FixedString
import com.plusmobileapps.chefmate.text.TextData

sealed class PlusHeaderData {
    abstract val title: TextData
    abstract val trailingAccessory: TrailingAccessory?

    data object None : PlusHeaderData() {
        override val title: TextData = FixedString("")
        override val trailingAccessory: TrailingAccessory? = null
    }

    data class Parent(
        override val title: TextData,
        override val trailingAccessory: TrailingAccessory? = null,
        /** Optional leading content rendered to the left of the title (e.g. a selector). */
        val leading: (@Composable () -> Unit)? = null,
        /**
         * Optional content centered in the app bar's title area — the space left between [leading]
         * and [trailingAccessory]. Rendered alongside [title], so keep it small (e.g. a count).
         */
        val center: (@Composable () -> Unit)? = null,
    ) : PlusHeaderData()

    data class Child(
        override val title: TextData,
        val onBackClick: () -> Unit,
        override val trailingAccessory: TrailingAccessory? = null,
    ) : PlusHeaderData()

    data class Modal(
        override val title: TextData,
        val onCloseClick: () -> Unit,
        override val trailingAccessory: TrailingAccessory? = null,
    ) : PlusHeaderData()

    sealed class TrailingAccessory {
        data class Icon(
            val icon: ImageVector,
            val contentDesc: TextData,
            val onClick: (() -> Unit)? = null,
        ) : TrailingAccessory()

        data class Button(val text: TextData, val onClick: () -> Unit) : TrailingAccessory()

        data class Custom(val content: @Composable RowScope.() -> Unit) : TrailingAccessory()
    }
}
