@file:OptIn(ExperimentalSharedTransitionApi::class)

package com.plusmobileapps.chefmate.browser

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import chefmate.client.browser.public.generated.resources.Res
import chefmate.client.browser.public.generated.resources.app_icon
import chefmate.client.browser.public.generated.resources.browser_landing_engine_dropdown
import chefmate.client.browser.public.generated.resources.browser_landing_hint
import chefmate.client.browser.public.generated.resources.browser_landing_tagline
import chefmate.client.browser.public.generated.resources.browser_search_onboarding
import com.plusmobileapps.chefmate.di.CoachMarkId
import com.plusmobileapps.chefmate.text.FixedString
import com.plusmobileapps.chefmate.text.PhraseModel
import com.plusmobileapps.chefmate.text.asTextData
import com.plusmobileapps.chefmate.ui.components.PlusOnboardingTooltip
import com.plusmobileapps.chefmate.ui.components.PlusTextFieldDefaults
import com.plusmobileapps.chefmate.ui.components.PlusTooltipPlacement
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun BrowserLandingScreen(
    bloc: BrowserLandingBloc,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
    modifier: Modifier = Modifier,
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val state by bloc.state.collectAsState()

    val sharedFieldModifier =
        if (sharedTransitionScope != null && animatedVisibilityScope != null) {
            with(sharedTransitionScope) {
                Modifier.sharedElement(
                    sharedContentState = rememberSharedContentState(key = "browser-address-bar"),
                    animatedVisibilityScope = animatedVisibilityScope,
                )
            }
        } else Modifier

    Column(
        modifier =
            modifier
                .testTag(BrowserTestTags.LANDING_SCREEN)
                .fillMaxSize()
                .padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(16.dp))
        Image(
            painter = painterResource(Res.drawable.app_icon),
            contentDescription = null,
            modifier = Modifier.size(144.dp).clip(RoundedCornerShape(32.dp)),
        )
        Spacer(Modifier.height(8.dp))
        Text(text = "Chef Mate", style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(24.dp))

        SearchEngineDropdown(
            selectedEngine = state.selectedEngine,
            onEngineSelected = bloc::onSearchEngineSelected,
        )
        Spacer(Modifier.height(16.dp))

        PlusOnboardingTooltip(
            text = Res.string.browser_search_onboarding.asTextData(),
            visible = state.activeCoachMark == CoachMarkId.BROWSER_SEARCH,
            onDismiss = { bloc.onCoachMarkDismissed(CoachMarkId.BROWSER_SEARCH) },
            placement = PlusTooltipPlacement.BELOW,
            modifier = Modifier.fillMaxWidth(),
        ) {
            OutlinedTextField(
                value = "",
                onValueChange = {},
                readOnly = true,
                modifier =
                    Modifier.fillMaxWidth().then(sharedFieldModifier).onFocusChanged { focusState ->
                        if (focusState.isFocused) {
                            keyboardController?.hide()
                            bloc.onSearchFieldFocused()
                        }
                    },
                placeholder = {
                    Text(
                        PhraseModel(
                                Res.string.browser_landing_hint,
                                "engine" to FixedString(state.selectedEngine.displayName),
                            )
                            .localized()
                    )
                },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = PlusTextFieldDefaults.RoundedShape,
            )
        }

        Text(
            text = stringResource(Res.string.browser_landing_tagline),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 16.dp).fillMaxWidth(),
        )
    }
}

@Composable
private fun SearchEngineDropdown(
    selectedEngine: SearchEngine,
    onEngineSelected: (SearchEngine) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        AssistChip(
            onClick = { expanded = true },
            modifier = Modifier.testTag(BrowserTestTags.LANDING_ENGINE_DROPDOWN),
            label = { Text(selectedEngine.displayName) },
            leadingIcon = {
                Icon(
                    painter = painterResource(selectedEngine.logo),
                    contentDescription = null,
                    tint = Color.Unspecified,
                    modifier = Modifier.size(AssistChipDefaults.IconSize),
                )
            },
            trailingIcon = {
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = stringResource(Res.string.browser_landing_engine_dropdown),
                )
            },
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            SearchEngine.entries.forEach { engine ->
                DropdownMenuItem(
                    text = { Text(engine.displayName) },
                    onClick = {
                        expanded = false
                        onEngineSelected(engine)
                    },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(engine.logo),
                            contentDescription = null,
                            tint = Color.Unspecified,
                            modifier = Modifier.size(24.dp),
                        )
                    },
                    modifier = Modifier.testTag(BrowserTestTags.engineOption(engine)),
                )
            }
        }
    }
}
