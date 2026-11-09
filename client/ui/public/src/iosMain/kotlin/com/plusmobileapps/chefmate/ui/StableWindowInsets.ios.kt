@file:Suppress("ktlint:standard:filename")

package com.plusmobileapps.chefmate.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.runtime.Composable

@Composable actual fun WindowInsets.stableWindowInsets(): WindowInsets = snapshotWindowInsets()
