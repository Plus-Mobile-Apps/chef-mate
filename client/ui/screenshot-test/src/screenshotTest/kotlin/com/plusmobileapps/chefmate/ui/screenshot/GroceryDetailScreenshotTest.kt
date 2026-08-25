package com.plusmobileapps.chefmate.ui.screenshot

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.plusmobileapps.chefmate.grocery.core.detail.GroceryDetailBloc
import com.plusmobileapps.chefmate.grocery.core.detail.GroceryDetailSheetContent
import com.plusmobileapps.chefmate.grocery.core.impl.detail.ui.previewGroceryDetailBlocAlwaysFileHere
import com.plusmobileapps.chefmate.grocery.core.impl.detail.ui.previewGroceryDetailBlocFromRecipe
import com.plusmobileapps.chefmate.grocery.core.impl.detail.ui.previewGroceryDetailBlocLoaded
import com.plusmobileapps.chefmate.grocery.core.impl.detail.ui.previewGroceryDetailBlocLoading
import com.plusmobileapps.chefmate.grocery.core.impl.detail.ui.previewGroceryDetailBlocPurchased
import com.plusmobileapps.chefmate.text.FixedString
import com.plusmobileapps.chefmate.ui.components.PlusResponsiveModalDialogSurface
import com.plusmobileapps.chefmate.ui.theme.ChefMateTheme

// ModalBottomSheet doesn't render reliably under the Compose screenshot test plugin, so these
// tests target [GroceryDetailSheetContent] directly. The list-screen overlay is exercised at
// runtime via robot UI tests in client/composeApp/src/commonTest.
@Composable
private fun GroceryDetailSheet(bloc: GroceryDetailBloc, darkTheme: Boolean = false) {
    ChefMateTheme(darkTheme = darkTheme) {
        Surface(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surface) {
            GroceryDetailSheetContent(bloc = bloc)
        }
    }
}

@PreviewTest
@Preview(showBackground = true, heightDp = 500)
@Composable
fun GroceryDetailSheetLoadedLightScreenshot() {
    GroceryDetailSheet(bloc = previewGroceryDetailBlocLoaded)
}

@PreviewTest
@Preview(showBackground = true, heightDp = 500, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun GroceryDetailSheetLoadedDarkScreenshot() {
    GroceryDetailSheet(bloc = previewGroceryDetailBlocLoaded, darkTheme = true)
}

@PreviewTest
@Preview(showBackground = true, heightDp = 500)
@Composable
fun GroceryDetailSheetPurchasedLightScreenshot() {
    GroceryDetailSheet(bloc = previewGroceryDetailBlocPurchased)
}

@PreviewTest
@Preview(showBackground = true, heightDp = 500)
@Composable
fun GroceryDetailSheetFromRecipeLightScreenshot() {
    GroceryDetailSheet(bloc = previewGroceryDetailBlocFromRecipe)
}

@PreviewTest
@Preview(showBackground = true, heightDp = 300)
@Composable
fun GroceryDetailSheetLoadingLightScreenshot() {
    GroceryDetailSheet(bloc = previewGroceryDetailBlocLoading)
}

@PreviewTest
@Preview(showBackground = true, heightDp = 500)
@Composable
fun GroceryDetailSheetAlwaysFileHereLightScreenshot() {
    GroceryDetailSheet(bloc = previewGroceryDetailBlocAlwaysFileHere)
}

// ── EXPANDED-width dialog variant ──────────────────────────────────────────
//
// On windows ≥ 840dp wide, PlusResponsiveModal switches to a centered dialog. Dialog itself
// doesn't snapshot reliably under the screenshot plugin, so we render
// PlusResponsiveModalDialogSurface
// directly inside a Box that fills the wide canvas.

@Composable
private fun GroceryDetailDialog(bloc: GroceryDetailBloc, darkTheme: Boolean = false) {
    ChefMateTheme(darkTheme = darkTheme) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                PlusResponsiveModalDialogSurface(
                    title = FixedString("Grocery Detail"),
                    onCloseClick = {},
                ) {
                    GroceryDetailSheetContent(bloc = bloc)
                }
            }
        }
    }
}

@PreviewTest
@Preview(showBackground = true, widthDp = 1000, heightDp = 700)
@Composable
fun GroceryDetailDialogExpandedLightScreenshot() {
    GroceryDetailDialog(bloc = previewGroceryDetailBlocLoaded)
}

@PreviewTest
@Preview(
    showBackground = true,
    widthDp = 1000,
    heightDp = 700,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
fun GroceryDetailDialogExpandedDarkScreenshot() {
    GroceryDetailDialog(bloc = previewGroceryDetailBlocLoaded, darkTheme = true)
}
