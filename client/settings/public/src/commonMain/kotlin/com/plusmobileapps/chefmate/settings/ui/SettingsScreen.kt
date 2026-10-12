@file:OptIn(ExperimentalMaterial3Api::class)

package com.plusmobileapps.chefmate.settings.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.MailOutline
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material.icons.outlined.Replay
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import chefmate.client.settings.public.generated.resources.Res
import chefmate.client.settings.public.generated.resources.about
import chefmate.client.settings.public.generated.resources.contact_us
import chefmate.client.settings.public.generated.resources.developer_settings
import chefmate.client.settings.public.generated.resources.greeting_authenticated
import chefmate.client.settings.public.generated.resources.manage_profile
import chefmate.client.settings.public.generated.resources.privacy_policy
import chefmate.client.settings.public.generated.resources.settings
import chefmate.client.settings.public.generated.resources.settings_ai_chat
import chefmate.client.settings.public.generated.resources.settings_guest_banner
import chefmate.client.settings.public.generated.resources.settings_notifications
import chefmate.client.settings.public.generated.resources.settings_replay_onboarding
import chefmate.client.settings.public.generated.resources.sign_in
import chefmate.client.settings.public.generated.resources.sign_out
import chefmate.client.settings.public.generated.resources.sign_out_confirmation_cancel
import chefmate.client.settings.public.generated.resources.sign_out_confirmation_confirm
import chefmate.client.settings.public.generated.resources.sign_out_confirmation_message
import chefmate.client.settings.public.generated.resources.sign_out_confirmation_title
import chefmate.client.settings.public.generated.resources.sign_up
import chefmate.client.settings.public.generated.resources.terms_of_use
import chefmate.client.settings.public.generated.resources.version_label
import com.plusmobileapps.chefmate.settings.SettingsBloc
import com.plusmobileapps.chefmate.settings.SettingsTestTags
import com.plusmobileapps.chefmate.subscription.ui.PremiumLockBadge
import com.plusmobileapps.chefmate.subscription.ui.PremiumRequiredDialog
import com.plusmobileapps.chefmate.text.FixedString
import com.plusmobileapps.chefmate.text.PhraseModel
import com.plusmobileapps.chefmate.text.TextData
import com.plusmobileapps.chefmate.text.asTextData
import com.plusmobileapps.chefmate.ui.components.LocalBottomNavBarInset
import com.plusmobileapps.chefmate.ui.components.PlusAvatar
import com.plusmobileapps.chefmate.ui.components.PlusButton
import com.plusmobileapps.chefmate.ui.components.PlusButtonVariant
import com.plusmobileapps.chefmate.ui.components.PlusDialog
import com.plusmobileapps.chefmate.ui.theme.ChefMateTheme
import com.plusmobileapps.chefmate.util.rememberEmailLauncher
import kotlinx.coroutines.flow.MutableStateFlow

/** Support inbox surfaced by the "Contact Us" row in the More tab. */
private const val SUPPORT_EMAIL = "support@plusmobileapps.com"

@Composable
fun SettingsScreen(bloc: SettingsBloc, modifier: Modifier = Modifier) {
    val viewState by bloc.state.collectAsState()
    val launchEmail = rememberEmailLauncher()

    if (viewState.showSignOutConfirmationDialog) {
        PlusDialog(
            title = Res.string.sign_out_confirmation_title.asTextData(),
            message = Res.string.sign_out_confirmation_message.asTextData(),
            confirmButtonText = Res.string.sign_out_confirmation_confirm.asTextData(),
            dismissButtonText = Res.string.sign_out_confirmation_cancel.asTextData(),
            onConfirmClick = bloc::onSignOutConfirmed,
            onDismissRequest = bloc::onSignOutDismissed,
        )
    }

    // Upsell shown when a non-subscriber taps the AI chat row.
    if (viewState.showPremiumRequiredDialog) {
        PremiumRequiredDialog(onDismiss = bloc::onPremiumRequiredDismissed)
    }

    // Guests and signed-out users have no profile, so there is no header to collapse — and the
    // collapse behavior must not be attached then, or it would swallow the list's scroll.
    val hasProfile = viewState.isAuthenticated && !viewState.isAnonymous
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .testTag(SettingsTestTags.SCREEN)
                .then(
                    if (hasProfile) Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
                    else Modifier
                )
    ) {
        if (hasProfile) {
            val collapseRangePx =
                with(LocalDensity.current) {
                    (profileHeaderExpandedHeight(hasGreeting = viewState.greeting != null) -
                            ProfileHeaderCollapsedHeight)
                        .toPx()
                }
            SideEffect { scrollBehavior.state.heightOffsetLimit = -collapseRangePx }
            SettingsProfileHeader(
                imageUrl = viewState.profileImageUrl,
                name = viewState.profileName.orEmpty(),
                greeting = viewState.greeting,
                collapsedFraction = { scrollBehavior.state.collapsedFraction },
                onAvatarClick = bloc::onManageProfileClicked,
            )
        }
        Column(
            modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())
        ) {
            when {
                // Anonymous Supabase session — they're authenticated under the hood (so cloud
                // sync works) but have no credentials to come back to. Treat them as
                // "signed out, with a hint they're using a guest account": show Sign In / Sign
                // Up buttons and hide Sign Out (which would silently orphan their data).
                viewState.isAuthenticated && viewState.isAnonymous -> {
                    GuestBanner()
                    HorizontalDivider()
                    AuthButtons(
                        onSignInClick = bloc::onSignInClicked,
                        onSignUpClick = bloc::onSignUpClicked,
                    )
                }
                viewState.isAuthenticated -> {
                    SettingsRow(
                        name = Res.string.manage_profile.asTextData(),
                        icon = Icons.Outlined.AccountCircle,
                        onClick = bloc::onManageProfileClicked,
                    )
                }
                else -> {
                    viewState.verificationMessage?.let { message ->
                        EmailVerificationMessage(message = message)
                        HorizontalDivider()
                    }
                    AuthButtons(
                        onSignInClick = bloc::onSignInClicked,
                        onSignUpClick = bloc::onSignUpClicked,
                    )
                }
            }
            // Notifications aggregate collaboration invites, which are addressed to a real account
            // — so the row is only meaningful for a signed-in, non-anonymous user.
            if (viewState.isAuthenticated && !viewState.isAnonymous) {
                HorizontalDivider()
                SettingsRow(
                    name = Res.string.settings_notifications.asTextData(),
                    icon = Icons.Outlined.Notifications,
                    onClick = bloc::onNotificationsClicked,
                )
            }
            HorizontalDivider()
            SettingsRow(
                name = Res.string.settings.asTextData(),
                icon = Icons.Outlined.Settings,
                onClick = bloc::onAppSettingsClicked,
            )
            HorizontalDivider()
            SettingsRow(
                name = Res.string.settings_replay_onboarding.asTextData(),
                icon = Icons.Outlined.Replay,
                onClick = bloc::onReplayOnboardingClicked,
            )
            if (viewState.isAiChatEnabled) {
                HorizontalDivider()
                // Still tappable for a non-subscriber — the click opens the premium upsell.
                SettingsRow(
                    name = Res.string.settings_ai_chat.asTextData(),
                    icon = Icons.Outlined.AutoAwesome,
                    onClick = bloc::onAiChatClicked,
                    trailing = if (viewState.isSubscribed) null else ({ PremiumLockBadge() }),
                )
            }
            HorizontalDivider()
            SettingsRow(
                name = Res.string.privacy_policy.asTextData(),
                icon = Icons.Outlined.PrivacyTip,
                onClick = {
                    bloc.onUrlClicked("https://chefmate.plusmobileapps.com/privacy-policy/")
                },
            )
            HorizontalDivider()
            SettingsRow(
                name = Res.string.terms_of_use.asTextData(),
                icon = Icons.Outlined.Description,
                onClick = {
                    bloc.onUrlClicked("https://chefmate.plusmobileapps.com/terms-of-use/")
                },
            )
            HorizontalDivider()
            SettingsRow(
                name = Res.string.about.asTextData(),
                icon = Icons.Outlined.Info,
                onClick = { bloc.onUrlClicked("https://chefmate.plusmobileapps.com/about/") },
            )
            HorizontalDivider()
            SettingsRow(
                name = Res.string.contact_us.asTextData(),
                icon = Icons.Outlined.MailOutline,
                onClick = { launchEmail(SUPPORT_EMAIL) },
            )
            if (viewState.isDebugBuild) {
                HorizontalDivider()
                SettingsRow(
                    name = Res.string.developer_settings.asTextData(),
                    icon = Icons.Outlined.Code,
                    onClick = bloc::onDeveloperSettingsClicked,
                )
            }
            HorizontalDivider()
            // Sign Out sits apart from the navigation rows as a destructive action button.
            if (viewState.isAuthenticated && !viewState.isAnonymous) {
                PlusButton(
                    text = Res.string.sign_out.asTextData(),
                    variant = PlusButtonVariant.DESTRUCTIVE,
                    modifier =
                        Modifier.fillMaxWidth()
                            .padding(
                                horizontal = ChefMateTheme.dimens.paddingNormal,
                                vertical = ChefMateTheme.dimens.paddingLarge,
                            ),
                    onClick = bloc::onSignOutClicked,
                )
            }
            VersionLabel(versionName = viewState.versionName)
            // The floating nav pill overlaps the bottom of the screen; scroll the version label
            // clear of it.
            Spacer(modifier = Modifier.height(LocalBottomNavBarInset.current))
        }
    }
}

private val ProfileAvatarExpandedSize = 96.dp
private val ProfileAvatarCollapsedSize = 40.dp
private val ProfileHeaderCollapsedHeight = 64.dp
private const val GreetingCollapsedScale = 0.8f

/** Full height of [SettingsProfileHeader] before any scroll has collapsed it. */
@Composable
private fun profileHeaderExpandedHeight(hasGreeting: Boolean): Dp {
    val dimens = ChefMateTheme.dimens
    val greetingHeight =
        if (hasGreeting) {
            dimens.paddingSmall +
                with(LocalDensity.current) {
                    ChefMateTheme.typography.headlineSmall.lineHeight.toDp()
                }
        } else {
            0.dp
        }
    return dimens.paddingNormal + ProfileAvatarExpandedSize + greetingHeight + dimens.paddingNormal
}

/**
 * The More tab's profile header. Expanded, a large avatar sits centered above the greeting; as
 * [collapsedFraction] goes from 0 to 1 the avatar shrinks and slides to the start, and the greeting
 * shrinks and slides in beside it, ending as a regular-height app bar. Tapping the avatar opens
 * Manage Profile.
 *
 * Both children are drawn at their expanded size and scaled with a graphics layer, so the animation
 * never re-measures text or reloads the image. Public so `:client:ui:screenshot-test` can capture
 * the intermediate states.
 */
@Composable
fun SettingsProfileHeader(
    imageUrl: String?,
    name: String,
    greeting: TextData?,
    collapsedFraction: () -> Float,
    onAvatarClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dimens = ChefMateTheme.dimens
    val expandedHeight = profileHeaderExpandedHeight(hasGreeting = greeting != null)
    val avatarCollapsedScale = ProfileAvatarCollapsedSize / ProfileAvatarExpandedSize
    val fraction = { collapsedFraction().coerceIn(0f, 1f) }
    Layout(
        modifier =
            modifier.fillMaxWidth().clipToBounds().background(ChefMateTheme.colorScheme.surface),
        content = {
            PlusAvatar(
                imageUrl = imageUrl,
                contentDescription = Res.string.manage_profile.asTextData().localized(),
                fallbackText = name,
                size = ProfileAvatarExpandedSize,
                modifier =
                    Modifier.graphicsLayer {
                            val scale = lerp(1f, avatarCollapsedScale, fraction())
                            scaleX = scale
                            scaleY = scale
                            transformOrigin = TransformOrigin(0f, 0f)
                        }
                        .clip(CircleShape)
                        .clickable(onClick = onAvatarClick),
            )
            if (greeting != null) {
                Text(
                    text = greeting.localized(),
                    style = ChefMateTheme.typography.headlineSmall,
                    color = ChefMateTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier =
                        Modifier.graphicsLayer {
                            val scale = lerp(1f, GreetingCollapsedScale, fraction())
                            scaleX = scale
                            scaleY = scale
                            transformOrigin = TransformOrigin(0f, 0f)
                        },
                )
            }
        },
    ) { measurables, constraints ->
        val f = fraction()
        val width = constraints.maxWidth
        val padding = dimens.paddingNormal.roundToPx()
        val gap = dimens.paddingSmall.roundToPx()
        val avatarExpandedPx = ProfileAvatarExpandedSize.roundToPx()
        val avatarCollapsedPx = ProfileAvatarCollapsedSize.roundToPx()
        val collapsedHeightPx = ProfileHeaderCollapsedHeight.roundToPx()
        val avatar = measurables[0].measure(Constraints.fixed(avatarExpandedPx, avatarExpandedPx))
        val text = measurables.getOrNull(1)?.measure(Constraints(maxWidth = width - 2 * padding))
        layout(width, lerp(expandedHeight.roundToPx(), collapsedHeightPx, f)) {
            avatar.placeRelative(
                x = lerp((width - avatarExpandedPx) / 2, padding, f),
                y = lerp(padding, (collapsedHeightPx - avatarCollapsedPx) / 2, f),
            )
            text?.placeRelative(
                x = lerp((width - text.width) / 2, padding + avatarCollapsedPx + padding, f),
                y =
                    lerp(
                        padding + avatarExpandedPx + gap,
                        ((collapsedHeightPx - text.height * GreetingCollapsedScale) / 2).toInt(),
                        f,
                    ),
            )
        }
    }
}

@Composable
private fun EmailVerificationMessage(message: TextData, modifier: Modifier = Modifier) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.secondaryContainer)
                .padding(ChefMateTheme.dimens.paddingNormal)
    ) {
        Text(
            message.localized(),
            style = ChefMateTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
        )
    }
}

@Composable
private fun AuthButtons(
    onSignInClick: () -> Unit,
    onSignUpClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(ChefMateTheme.dimens.paddingNormal),
        horizontalArrangement = Arrangement.spacedBy(ChefMateTheme.dimens.paddingSmall),
    ) {
        PlusButton(
            text = Res.string.sign_in.asTextData(),
            variant = PlusButtonVariant.SECONDARY,
            modifier = Modifier.weight(1f),
            onClick = onSignInClick,
        )
        PlusButton(
            text = Res.string.sign_up.asTextData(),
            modifier = Modifier.weight(1f),
            onClick = onSignUpClick,
        )
    }
}

@Composable
private fun GuestBanner(modifier: Modifier = Modifier) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.tertiaryContainer)
                .padding(ChefMateTheme.dimens.paddingNormal)
    ) {
        Text(
            Res.string.settings_guest_banner.asTextData().localized(),
            style = ChefMateTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onTertiaryContainer,
        )
    }
}

@Composable
private fun VersionLabel(versionName: String, modifier: Modifier = Modifier) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(
                    horizontal = ChefMateTheme.dimens.paddingNormal,
                    vertical = ChefMateTheme.dimens.paddingSmall,
                ),
        horizontalArrangement = Arrangement.Center,
    ) {
        Text(
            PhraseModel(resource = Res.string.version_label, "version" to FixedString(versionName))
                .localized(),
            style = ChefMateTheme.typography.bodySmall,
            color = ChefMateTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
internal fun SettingsRow(
    name: TextData,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    val contentDescription = name.localized()
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .height(ChefMateTheme.dimens.rowHeight)
                .clickable { onClick() }
                .padding(horizontal = ChefMateTheme.dimens.paddingNormal)
                .semantics { this.contentDescription = contentDescription },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = ChefMateTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.width(ChefMateTheme.dimens.paddingNormal))
        }
        Text(
            name.localized(),
            style = ChefMateTheme.typography.titleMedium,
            modifier = Modifier.weight(1f),
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(ChefMateTheme.dimens.paddingSmall),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            trailing?.invoke()
            Icon(Icons.Default.ChevronRight, contentDescription = null)
        }
    }
}

/** Signed-out More tab — Sign In / Sign Up buttons. Public for `:client:ui:screenshot-test`. */
val previewSettingsBlocSignedOut: SettingsBloc =
    object : SettingsBloc {
        override val state =
            kotlinx.coroutines.flow.MutableStateFlow(
                SettingsBloc.Model(isAuthenticated = false, greeting = null, versionName = "1.4.3")
            )

        override fun onSignInClicked() = Unit

        override fun onSignUpClicked() = Unit

        override fun onSignOutClicked() = Unit

        override fun onSignOutConfirmed() = Unit

        override fun onSignOutDismissed() = Unit

        override fun onManageProfileClicked() = Unit

        override fun onNotificationsClicked() = Unit

        override fun onUrlClicked(url: String) = Unit

        override fun onAppSettingsClicked() = Unit

        override fun onAiChatClicked() = Unit

        override fun onPremiumRequiredDismissed() = Unit

        override fun onDeveloperSettingsClicked() = Unit

        override fun onReplayOnboardingClicked() = Unit
    }

private val previewBlocAuthenticated =
    object : SettingsBloc {
        override val state =
            kotlinx.coroutines.flow.MutableStateFlow(
                SettingsBloc.Model(
                    isAuthenticated = true,
                    greeting =
                        com.plusmobileapps.chefmate.text.PhraseModel(
                            resource = Res.string.greeting_authenticated,
                            "name" to com.plusmobileapps.chefmate.text.FixedString("John Doe"),
                        ),
                    profileName = "John Doe",
                    versionName = "1.4.3",
                )
            )

        override fun onSignInClicked() = Unit

        override fun onSignUpClicked() = Unit

        override fun onSignOutClicked() = Unit

        override fun onSignOutConfirmed() = Unit

        override fun onSignOutDismissed() = Unit

        override fun onManageProfileClicked() = Unit

        override fun onNotificationsClicked() = Unit

        override fun onUrlClicked(url: String) = Unit

        override fun onAppSettingsClicked() = Unit

        override fun onAiChatClicked() = Unit

        override fun onPremiumRequiredDismissed() = Unit

        override fun onDeveloperSettingsClicked() = Unit

        override fun onReplayOnboardingClicked() = Unit
    }

/** Anonymous (guest) More tab — banner plus Sign In / Sign Up buttons, no Sign Out. */
val previewSettingsBlocGuest: SettingsBloc =
    object : SettingsBloc {
        override val state =
            kotlinx.coroutines.flow.MutableStateFlow(
                SettingsBloc.Model(
                    isAuthenticated = true,
                    isAnonymous = true,
                    versionName = "1.4.3",
                )
            )

        override fun onSignInClicked() = Unit

        override fun onSignUpClicked() = Unit

        override fun onSignOutClicked() = Unit

        override fun onSignOutConfirmed() = Unit

        override fun onSignOutDismissed() = Unit

        override fun onManageProfileClicked() = Unit

        override fun onNotificationsClicked() = Unit

        override fun onUrlClicked(url: String) = Unit

        override fun onAppSettingsClicked() = Unit

        override fun onAiChatClicked() = Unit

        override fun onPremiumRequiredDismissed() = Unit

        override fun onDeveloperSettingsClicked() = Unit

        override fun onReplayOnboardingClicked() = Unit
    }

/**
 * AI Chat row surfaced for a subscriber — no badge, tapping opens the chat. Public so
 * `:client:ui:screenshot-test` can reuse it.
 */
val previewSettingsBlocAiChatUnlocked: SettingsBloc =
    object : SettingsBloc by previewBlocAuthenticated {
        override val state =
            MutableStateFlow(
                previewBlocAuthenticated.state.value.copy(
                    isAiChatEnabled = true,
                    isSubscribed = true,
                )
            )
    }

/** AI Chat row surfaced for a non-subscriber — badged "Premium", tapping opens the upsell. */
val previewSettingsBlocAiChatLocked: SettingsBloc =
    object : SettingsBloc by previewBlocAuthenticated {
        override val state =
            MutableStateFlow(
                previewBlocAuthenticated.state.value.copy(
                    isAiChatEnabled = true,
                    isSubscribed = false,
                )
            )
    }

@Preview(showBackground = true)
@Composable
internal fun SettingsScreenAiChatUnlockedPreview() {
    ChefMateTheme { SettingsScreen(bloc = previewSettingsBlocAiChatUnlocked) }
}

@Preview(showBackground = true)
@Composable
internal fun SettingsScreenAiChatLockedPreview() {
    ChefMateTheme { SettingsScreen(bloc = previewSettingsBlocAiChatLocked) }
}

@Preview(showBackground = true)
@Composable
internal fun SettingsScreenUnauthenticatedPreview() {
    ChefMateTheme { SettingsScreen(bloc = previewSettingsBlocSignedOut) }
}

@Preview(showBackground = true)
@Composable
internal fun SettingsScreenAuthenticatedPreview() {
    ChefMateTheme { SettingsScreen(bloc = previewBlocAuthenticated) }
}

@Preview(showBackground = true)
@Composable
internal fun SettingsScreenAnonymousPreview() {
    ChefMateTheme { SettingsScreen(bloc = previewSettingsBlocGuest) }
}

@Preview(showBackground = true)
@Composable
internal fun SettingsScreenDarkPreview() {
    ChefMateTheme(darkTheme = true) { SettingsScreen(bloc = previewSettingsBlocSignedOut) }
}
