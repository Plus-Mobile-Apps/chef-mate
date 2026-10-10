package com.plusmobileapps.chefmate.notifications.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import chefmate.client.notifications.public.generated.resources.Res
import chefmate.client.notifications.public.generated.resources.notifications_accept
import chefmate.client.notifications.public.generated.resources.notifications_decline
import chefmate.client.notifications.public.generated.resources.notifications_empty_message
import chefmate.client.notifications.public.generated.resources.notifications_empty_title
import chefmate.client.notifications.public.generated.resources.notifications_family_invite_message
import chefmate.client.notifications.public.generated.resources.notifications_grocery_invite_message
import chefmate.client.notifications.public.generated.resources.notifications_recipe_book_invite_message
import chefmate.client.notifications.public.generated.resources.notifications_signed_out
import chefmate.client.notifications.public.generated.resources.notifications_signed_out_title
import chefmate.client.notifications.public.generated.resources.notifications_title
import com.plusmobileapps.chefmate.notifications.NotificationsBloc
import com.plusmobileapps.chefmate.notifications.NotificationsTestTags
import com.plusmobileapps.chefmate.notifications.data.AppNotification
import com.plusmobileapps.chefmate.text.FixedString
import com.plusmobileapps.chefmate.text.PhraseModel
import com.plusmobileapps.chefmate.text.TextData
import com.plusmobileapps.chefmate.text.asTextData
import com.plusmobileapps.chefmate.ui.components.PlusButton
import com.plusmobileapps.chefmate.ui.components.PlusButtonVariant
import com.plusmobileapps.chefmate.ui.components.PlusHeaderContainer
import com.plusmobileapps.chefmate.ui.components.PlusHeaderData
import com.plusmobileapps.chefmate.ui.components.PlusLoadingIndicator
import com.plusmobileapps.chefmate.ui.components.SignedOutPrompt
import com.plusmobileapps.chefmate.ui.theme.ChefMateTheme

@Composable
fun NotificationsScreen(bloc: NotificationsBloc, modifier: Modifier = Modifier) {
    val state by bloc.state.collectAsState()

    PlusHeaderContainer(
        modifier = modifier.testTag(NotificationsTestTags.SCREEN),
        data =
            PlusHeaderData.Child(
                title = Res.string.notifications_title.asTextData(),
                onBackClick = bloc::onBack,
            ),
        contentPadding = PaddingValues(ChefMateTheme.dimens.paddingNormal),
    ) {
        when {
            state.isLoading -> Unit
            !state.isSignedIn ->
                SignedOutPrompt(
                    title = Res.string.notifications_signed_out_title.asTextData(),
                    message = Res.string.notifications_signed_out.asTextData(),
                    onSignInClick = bloc::onSignInClicked,
                    onSignUpClick = bloc::onSignUpClicked,
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier =
                        Modifier.fillMaxWidth().padding(top = ChefMateTheme.dimens.paddingLarge),
                    signInButtonModifier = Modifier.testTag(NotificationsTestTags.SIGN_IN_BUTTON),
                    signUpButtonModifier = Modifier.testTag(NotificationsTestTags.SIGN_UP_BUTTON),
                )
            state.notifications.isEmpty() ->
                EmptyState(
                    title = Res.string.notifications_empty_title.asTextData(),
                    message = Res.string.notifications_empty_message.asTextData(),
                )
            else ->
                state.notifications.forEach { notification ->
                    NotificationCard(
                        notification = notification,
                        isProcessing = notification.key in state.processing,
                        onAccept = { bloc.onAccept(notification) },
                        onDecline = { bloc.onDecline(notification) },
                    )
                }
        }
    }
}

@Composable
private fun NotificationCard(
    notification: AppNotification,
    isProcessing: Boolean,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(bottom = ChefMateTheme.dimens.paddingNormal),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            ),
    ) {
        Column(modifier = Modifier.padding(ChefMateTheme.dimens.paddingNormal)) {
            Text(
                text = notification.message().localized(),
                style = MaterialTheme.typography.bodyMedium,
            )
            if (isProcessing) {
                Row(
                    modifier =
                        Modifier.fillMaxWidth().padding(top = ChefMateTheme.dimens.paddingNormal),
                    horizontalArrangement = Arrangement.End,
                ) {
                    PlusLoadingIndicator(modifier = Modifier.width(24.dp))
                }
            } else {
                Row(
                    modifier =
                        Modifier.fillMaxWidth().padding(top = ChefMateTheme.dimens.paddingNormal),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    PlusButton(
                        text = Res.string.notifications_decline.asTextData(),
                        variant = PlusButtonVariant.SECONDARY,
                        modifier =
                            Modifier.testTag(
                                NotificationsTestTags.DECLINE_PREFIX + notification.key
                            ),
                        onClick = onDecline,
                    )
                    Spacer(modifier = Modifier.width(ChefMateTheme.dimens.paddingNormal))
                    PlusButton(
                        text = Res.string.notifications_accept.asTextData(),
                        modifier =
                            Modifier.testTag(
                                NotificationsTestTags.ACCEPT_PREFIX + notification.key
                            ),
                        onClick = onAccept,
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyState(message: TextData, title: TextData? = null) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = ChefMateTheme.dimens.paddingLarge),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(ChefMateTheme.dimens.paddingSmall),
    ) {
        title?.let {
            Text(
                text = it.localized(),
                style = ChefMateTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
            )
        }
        Text(
            text = message.localized(),
            style = ChefMateTheme.typography.bodyMedium,
            color = ChefMateTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.testTag(NotificationsTestTags.EMPTY),
        )
    }
}

private fun AppNotification.message(): TextData =
    when (this) {
        is AppNotification.GroceryInvite ->
            PhraseModel(
                resource = Res.string.notifications_grocery_invite_message,
                "list" to FixedString(listName),
            )
        is AppNotification.RecipeBookInvite ->
            PhraseModel(
                resource = Res.string.notifications_recipe_book_invite_message,
                "book" to FixedString(bookName),
            )
        is AppNotification.FamilyInvite ->
            PhraseModel(
                resource = Res.string.notifications_family_invite_message,
                "family" to FixedString(familyName),
            )
    }
