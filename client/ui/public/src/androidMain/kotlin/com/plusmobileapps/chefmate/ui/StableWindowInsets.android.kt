@file:Suppress("ktlint:standard:filename")

package com.plusmobileapps.chefmate.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.runtime.Composable

// Android's insets are long-lived objects that animate without recomposition; snapshotting them
// would only add recompositions.
@Composable actual fun WindowInsets.stableWindowInsets(): WindowInsets = this
