package com.plusmobileapps.chefmate.ui.components

import androidx.compose.runtime.Composable

/**
 * Returns a key that changes whenever a text field using the platform-native text input (see
 * [withNativeTextInput]) needs its cached "can paste" state refreshed.
 *
 * On iOS, Compose decides once — when the field enters composition — whether the native edit menu
 * offers Paste, and only re-checks after one of its own menu actions. Text copied afterwards (in
 * another app, or in another field of this screen) therefore never surfaces Paste on long-press;
 * only the system items like AutoFill appear. The iOS implementation bumps this key when the
 * pasteboard has changed since the field last looked: immediately for an unfocused field (or when
 * it loses focus), and on returning to the app even while focused, since restarting the focused
 * field's input session is only worth it then. Always `0` on platforms without native input.
 */
@Composable internal expect fun rememberNativePasteRefreshKey(isFocused: Boolean): Int
