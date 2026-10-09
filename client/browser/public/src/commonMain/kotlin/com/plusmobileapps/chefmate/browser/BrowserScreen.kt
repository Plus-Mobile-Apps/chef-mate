@file:OptIn(ExperimentalSharedTransitionApi::class)

package com.plusmobileapps.chefmate.browser

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import chefmate.client.browser.public.generated.resources.Res
import chefmate.client.browser.public.generated.resources.browser_address_hint
import chefmate.client.browser.public.generated.resources.browser_back
import chefmate.client.browser.public.generated.resources.browser_clear
import chefmate.client.browser.public.generated.resources.browser_download
import chefmate.client.browser.public.generated.resources.browser_extraction_failed_body
import chefmate.client.browser.public.generated.resources.browser_forward
import chefmate.client.browser.public.generated.resources.browser_navigate
import chefmate.client.browser.public.generated.resources.browser_open_in_default_browser
import com.plusmobileapps.chefmate.text.FixedString
import com.plusmobileapps.chefmate.text.asTextData
import com.plusmobileapps.chefmate.ui.components.PlusButton
import com.plusmobileapps.chefmate.ui.components.PlusDialog
import com.plusmobileapps.chefmate.ui.components.PlusTextFieldDefaults
import org.jetbrains.compose.resources.stringResource

@Composable
fun BrowserScreen(
    bloc: BrowserBloc,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
    modifier: Modifier = Modifier,
) {
    val viewState by bloc.state.collectAsState()
    val uriHandler = LocalUriHandler.current

    val failureMessage = viewState.extractionFailureMessage
    if (failureMessage != null) {
        PlusDialog(
            title = failureMessage,
            message = Res.string.browser_extraction_failed_body.asTextData(),
            confirmButtonText = FixedString("OK"),
            onConfirmClick = bloc::onDismissMessage,
            onDismissRequest = bloc::onDismissMessage,
        )
    }

    Column(modifier = modifier.fillMaxSize()) {
        if (viewState.showControls) {
            BrowserAddressBar(
                url = viewState.addressBarText,
                canGoBack = viewState.canGoBack,
                canGoForward = viewState.canGoForward,
                canOpenExternally = viewState.currentUrl.isNotBlank(),
                onUrlChanged = bloc::onUrlChanged,
                onNavigate = bloc::onNavigate,
                onGoBack = bloc::onGoBack,
                onGoForward = bloc::onGoForward,
                onAddressBarFocused = bloc::onAddressBarFocused,
                // Hand the page that's actually loaded (not the possibly half-typed address text)
                // to the OS, which routes it to the user's default browser. Guarded because a
                // scheme with no handler installed makes the platform handler throw.
                onOpenInDefaultBrowser = {
                    runCatching { uriHandler.openUri(viewState.currentUrl) }
                },
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = animatedVisibilityScope,
            )
        }
        PlatformWebView(
            url = viewState.navigateUrl,
            onUrlLoaded = bloc::onUrlLoadedInWebView,
            onLoadingChanged = bloc::onWebViewLoadingChanged,
            onCanNavigateChanged = bloc::onCanNavigateChanged,
            goBackTrigger = viewState.goBackTrigger,
            goForwardTrigger = viewState.goForwardTrigger,
            captureHtmlTrigger = viewState.captureHtmlTrigger,
            onHtmlCaptured = bloc::onHtmlCaptured,
            instanceKeeper = bloc.instanceKeeper,
            modifier = Modifier.fillMaxWidth().weight(1f),
        )
        if (viewState.showControls) {
            BrowserBottomBar(
                showExtract = viewState.currentUrl.isNotBlank(),
                isExtracting = viewState.isExtracting,
                onExtractRecipe = bloc::onExtractRecipe,
            )
        }
    }
}

/** Public so `client/ui/screenshot-test` can snapshot it without a live [PlatformWebView]. */
@Composable
fun BrowserAddressBar(
    url: String,
    canGoBack: Boolean,
    canGoForward: Boolean,
    canOpenExternally: Boolean,
    onUrlChanged: (String) -> Unit,
    onNavigate: () -> Unit,
    onGoBack: () -> Unit,
    onGoForward: () -> Unit,
    onAddressBarFocused: () -> Unit,
    onOpenInDefaultBrowser: () -> Unit,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
    modifier: Modifier = Modifier,
) {
    var textFieldValue by remember { mutableStateOf(TextFieldValue(url)) }
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(url) {
        if (url != textFieldValue.text) {
            textFieldValue = TextFieldValue(url)
        }
    }

    val sharedFieldModifier =
        if (sharedTransitionScope != null && animatedVisibilityScope != null) {
            with(sharedTransitionScope) {
                Modifier.sharedElement(
                    sharedContentState = rememberSharedContentState(key = "browser-address-bar"),
                    animatedVisibilityScope = animatedVisibilityScope,
                )
            }
        } else Modifier

    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onGoBack, enabled = canGoBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(Res.string.browser_back),
            )
        }
        IconButton(onClick = onGoForward, enabled = canGoForward) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = stringResource(Res.string.browser_forward),
            )
        }
        OutlinedTextField(
            value = textFieldValue,
            onValueChange = { newValue ->
                textFieldValue = newValue
                onUrlChanged(newValue.text)
            },
            modifier =
                Modifier.weight(1f)
                    .then(sharedFieldModifier)
                    .focusRequester(focusRequester)
                    .onFocusChanged { focusState ->
                        if (focusState.isFocused) {
                            onAddressBarFocused()
                        }
                    },
            placeholder = { Text(stringResource(Res.string.browser_address_hint)) },
            singleLine = true,
            shape = PlusTextFieldDefaults.RoundedShape,
            keyboardOptions =
                KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Go),
            keyboardActions =
                KeyboardActions(
                    onGo = {
                        onNavigate()
                        keyboardController?.hide()
                    }
                ),
            trailingIcon = {
                if (textFieldValue.text.isNotEmpty()) {
                    IconButton(
                        onClick = {
                            textFieldValue = TextFieldValue("")
                            onUrlChanged("")
                            focusRequester.requestFocus()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(Res.string.browser_clear),
                        )
                    }
                }
            },
        )
        IconButton(
            onClick = {
                onNavigate()
                keyboardController?.hide()
            }
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = stringResource(Res.string.browser_navigate),
            )
        }
        IconButton(
            onClick = onOpenInDefaultBrowser,
            enabled = canOpenExternally,
            modifier = Modifier.testTag(BrowserTestTags.OPEN_IN_DEFAULT_BROWSER),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                contentDescription = stringResource(Res.string.browser_open_in_default_browser),
            )
        }
    }
}

/**
 * The extract button deliberately reflects only [isExtracting], not whether the WebView is still
 * loading. A load that never reports a terminal state — one stalled subresource on a heavy recipe
 * page is enough — would otherwise leave the button disabled with no way to retry. Extracting
 * against a half-rendered page is the safer failure: it surfaces a dismissible error the user can
 * retry, and the extractor already falls back to fetching the URL when the captured markup carries
 * no recipe.
 */
@Composable
private fun BrowserBottomBar(
    showExtract: Boolean,
    isExtracting: Boolean,
    onExtractRecipe: () -> Unit,
    modifier: Modifier = Modifier,
) {
    HorizontalDivider()
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showExtract) {
            PlusButton(
                text = Res.string.browser_download.asTextData(),
                isLoading = isExtracting,
                onClick = onExtractRecipe,
            )
        }
    }
}
