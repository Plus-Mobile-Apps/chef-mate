@file:OptIn(ExperimentalSharedTransitionApi::class)

package com.plusmobileapps.chefmate.browser

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import chefmate.client.browser.public.generated.resources.Res
import chefmate.client.browser.public.generated.resources.browser_back
import chefmate.client.browser.public.generated.resources.browser_clear
import chefmate.client.browser.public.generated.resources.browser_history_delete
import chefmate.client.browser.public.generated.resources.browser_landing_hint
import com.plusmobileapps.chefmate.text.FixedString
import com.plusmobileapps.chefmate.text.PhraseModel
import com.plusmobileapps.chefmate.ui.components.PlusTextFieldDefaults
import org.jetbrains.compose.resources.stringResource

@Composable
fun BrowserEditQueryScreen(
    bloc: BrowserEditQueryBloc,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
    modifier: Modifier = Modifier,
) {
    val state by bloc.state.collectAsState()
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }
    var textFieldValue by remember {
        mutableStateOf(
            TextFieldValue(state.searchText, selection = TextRange(0, state.searchText.length))
        )
    }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

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
        modifier = modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.size(16.dp))
        OutlinedTextField(
            value = textFieldValue,
            onValueChange = { newValue ->
                textFieldValue = newValue
                bloc.onSearchTextChanged(newValue.text)
            },
            modifier =
                Modifier.fillMaxWidth().then(sharedFieldModifier).focusRequester(focusRequester),
            placeholder = {
                Text(
                    PhraseModel(
                            Res.string.browser_landing_hint,
                            "engine" to FixedString(state.searchEngine.displayName),
                        )
                        .localized()
                )
            },
            leadingIcon = {
                IconButton(
                    onClick = {
                        keyboardController?.hide()
                        bloc.onCancel()
                    }
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(Res.string.browser_back),
                    )
                }
            },
            trailingIcon = {
                if (textFieldValue.text.isNotEmpty()) {
                    IconButton(
                        onClick = {
                            textFieldValue = TextFieldValue("")
                            bloc.onSearchTextChanged("")
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
            singleLine = true,
            shape = PlusTextFieldDefaults.RoundedShape,
            keyboardOptions =
                KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Search),
            keyboardActions =
                KeyboardActions(
                    onSearch = {
                        bloc.onNavigate()
                        keyboardController?.hide()
                    }
                ),
        )

        Spacer(modifier = Modifier.size(8.dp))

        LazyColumn(modifier = Modifier.fillMaxWidth()) {
            items(items = state.history, key = { it.id }) { entry ->
                HistoryRow(
                    entry = entry,
                    onClick = {
                        keyboardController?.hide()
                        bloc.onHistoryItemClicked(entry)
                    },
                    onDelete = { bloc.onHistoryItemDeleteClicked(entry) },
                )
            }
        }
    }
}

@Composable
private fun HistoryRow(
    entry: BrowserHistoryEntry,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Default.History,
            contentDescription = null,
            modifier = Modifier.padding(horizontal = 12.dp),
        )
        Text(
            text = entry.url,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onDelete) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = stringResource(Res.string.browser_history_delete),
            )
        }
    }
}
