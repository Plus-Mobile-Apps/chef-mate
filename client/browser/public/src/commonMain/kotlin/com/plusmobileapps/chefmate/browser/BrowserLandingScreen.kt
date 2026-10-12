@file:OptIn(ExperimentalSharedTransitionApi::class)

package com.plusmobileapps.chefmate.browser

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import chefmate.client.browser.public.generated.resources.Res
import chefmate.client.browser.public.generated.resources.app_icon
import chefmate.client.browser.public.generated.resources.browser_bookmark_delete
import chefmate.client.browser.public.generated.resources.browser_bookmark_options
import chefmate.client.browser.public.generated.resources.browser_bookmarks_title
import chefmate.client.browser.public.generated.resources.browser_landing_engine_dropdown
import chefmate.client.browser.public.generated.resources.browser_landing_hint
import chefmate.client.browser.public.generated.resources.browser_landing_open_engine
import chefmate.client.browser.public.generated.resources.browser_landing_tagline
import chefmate.client.browser.public.generated.resources.browser_search_onboarding
import com.plusmobileapps.chefmate.di.CoachMarkId
import com.plusmobileapps.chefmate.text.FixedString
import com.plusmobileapps.chefmate.text.PhraseModel
import com.plusmobileapps.chefmate.text.asTextData
import com.plusmobileapps.chefmate.ui.components.PlusButton
import com.plusmobileapps.chefmate.ui.components.PlusButtonVariant
import com.plusmobileapps.chefmate.ui.components.PlusOnboardingTooltip
import com.plusmobileapps.chefmate.ui.components.PlusTextFieldDefaults
import com.plusmobileapps.chefmate.ui.components.PlusTooltipPlacement
import com.plusmobileapps.chefmate.ui.theme.ChefMateTheme
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

    // Scrolls once bookmarks outgrow the screen, but stays vertically centred while the content is
    // shorter than the viewport — the min height gives the centred arrangement room to work in.
    BoxWithConstraints(modifier = modifier.testTag(BrowserTestTags.LANDING_SCREEN).fillMaxSize()) {
        val viewportHeight = maxHeight
        Column(
            modifier =
                Modifier.fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .heightIn(min = viewportHeight)
                    .padding(horizontal = 32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            LandingContent(
                bloc = bloc,
                state = state,
                sharedFieldModifier = sharedFieldModifier,
                onSearchFieldFocused = {
                    keyboardController?.hide()
                    bloc.onSearchFieldFocused()
                },
            )
        }
    }
}

@Composable
private fun LandingContent(
    bloc: BrowserLandingBloc,
    state: BrowserLandingBloc.Model,
    sharedFieldModifier: Modifier,
    onSearchFieldFocused: () -> Unit,
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
                    if (focusState.isFocused) onSearchFieldFocused()
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

    Spacer(Modifier.height(16.dp))
    PlusButton(
        text =
            PhraseModel(
                Res.string.browser_landing_open_engine,
                "engine" to FixedString(state.selectedEngine.displayName),
            ),
        variant = PlusButtonVariant.SECONDARY,
        onClick = bloc::onOpenEngineHomeClicked,
        modifier = Modifier.testTag(BrowserTestTags.LANDING_OPEN_ENGINE_HOME),
    )

    Text(
        text = stringResource(Res.string.browser_landing_tagline),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(top = 16.dp).fillMaxWidth(),
    )

    if (state.bookmarks.isNotEmpty()) {
        BookmarksSection(
            bookmarks = state.bookmarks,
            onBookmarkClick = bloc::onBookmarkClicked,
            onDeleteBookmark = bloc::onDeleteBookmarkClicked,
            modifier =
                Modifier.padding(
                    top = ChefMateTheme.dimens.paddingExtraLarge,
                    bottom = ChefMateTheme.dimens.paddingLarge,
                ),
        )
    }
}

/** Narrowest a bookmark tile may get before the grid drops a column. */
private val BookmarkTileMinWidth = 136.dp

private val BookmarkMonogramSize = 40.dp

@Composable
private fun BookmarksSection(
    bookmarks: List<BrowserBookmark>,
    onBookmarkClick: (BrowserBookmark) -> Unit,
    onDeleteBookmark: (BrowserBookmark) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = ChefMateTheme.dimens.paddingSmall
    Column(
        modifier = modifier.fillMaxWidth().testTag(BrowserTestTags.LANDING_BOOKMARKS),
        verticalArrangement = Arrangement.spacedBy(spacing),
    ) {
        Text(
            text = stringResource(Res.string.browser_bookmarks_title),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = ChefMateTheme.dimens.paddingExtraSmall),
        )
        // A hand-rolled grid rather than LazyVerticalGrid: it sits inside the landing screen's
        // scrolling column, and a lazy grid can't be nested in a same-direction scroll.
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val columns =
                ((maxWidth + spacing) / (BookmarkTileMinWidth + spacing)).toInt().coerceAtLeast(1)
            Column(verticalArrangement = Arrangement.spacedBy(spacing)) {
                bookmarks.chunked(columns).forEach { rowBookmarks ->
                    Row(horizontalArrangement = Arrangement.spacedBy(spacing)) {
                        rowBookmarks.forEach { bookmark ->
                            BookmarkTile(
                                bookmark = bookmark,
                                onClick = { onBookmarkClick(bookmark) },
                                onDelete = { onDeleteBookmark(bookmark) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                        // Pad a short last row so its tiles keep the same width as the rest.
                        repeat(columns - rowBookmarks.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun BookmarkTile(
    bookmark: BrowserBookmark,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val host = remember(bookmark.url) { bookmark.url.displayHost() }
    OutlinedCard(
        onClick = onClick,
        modifier = modifier.testTag(BrowserTestTags.bookmarkTile(bookmark.id)),
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier =
                    Modifier.fillMaxWidth()
                        .padding(
                            horizontal = ChefMateTheme.dimens.paddingSmall,
                            vertical = ChefMateTheme.dimens.paddingNormal,
                        ),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                BookmarkMonogram(host = host)
                Spacer(Modifier.height(ChefMateTheme.dimens.paddingSmall))
                Text(
                    text = bookmark.title,
                    style = MaterialTheme.typography.titleSmall,
                    textAlign = TextAlign.Center,
                    minLines = 2,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = host,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            BookmarkOptions(
                bookmark = bookmark,
                onDelete = onDelete,
                modifier = Modifier.align(Alignment.TopEnd),
            )
        }
    }
}

@Composable
private fun BookmarkOptions(
    bookmark: BrowserBookmark,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        IconButton(
            onClick = { expanded = true },
            modifier = Modifier.testTag(BrowserTestTags.bookmarkOptions(bookmark.id)),
        ) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription =
                    PhraseModel(
                            Res.string.browser_bookmark_options,
                            "title" to FixedString(bookmark.title),
                        )
                        .localized(),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(Res.string.browser_bookmark_delete)) },
                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
                onClick = {
                    expanded = false
                    onDelete()
                },
                modifier = Modifier.testTag(BrowserTestTags.BOOKMARK_DELETE),
            )
        }
    }
}

/** A letter avatar standing in for the site's favicon, which would mean a fetch per tile. */
@Composable
internal fun BookmarkMonogram(host: String) {
    Box(
        modifier =
            Modifier.size(BookmarkMonogramSize)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = host.firstOrNull()?.uppercase().orEmpty(),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}

/** `https://www.seriouseats.com/banana-bread` → `seriouseats.com`. */
internal fun String.displayHost(): String =
    substringAfter("://").substringBefore('/').substringBefore('?').removePrefix("www.").ifEmpty {
        this
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
