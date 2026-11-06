package com.plusmobileapps.chefmate.notifications

import chefmate.client.notifications.public.generated.resources.Res
import chefmate.client.notifications.public.generated.resources.notifications_family_invite_message
import chefmate.client.notifications.public.generated.resources.notifications_grocery_invite_message
import chefmate.client.notifications.public.generated.resources.notifications_recipe_book_invite_message
import com.plusmobileapps.chefmate.notifications.data.AppNotification
import com.plusmobileapps.chefmate.text.FixedString
import com.plusmobileapps.chefmate.text.PhraseModel
import com.plusmobileapps.chefmate.text.TextData

/**
 * The body text for [notification]. Shared by the Compose screen and the native iOS screen, which
 * can't see the [AppNotification] subclasses (`NotificationMessageKt.notificationMessage`).
 */
fun notificationMessage(notification: AppNotification): TextData =
    when (notification) {
        is AppNotification.GroceryInvite ->
            PhraseModel(
                resource = Res.string.notifications_grocery_invite_message,
                "list" to FixedString(notification.listName),
            )
        is AppNotification.RecipeBookInvite ->
            PhraseModel(
                resource = Res.string.notifications_recipe_book_invite_message,
                "book" to FixedString(notification.bookName),
            )
        is AppNotification.FamilyInvite ->
            PhraseModel(
                resource = Res.string.notifications_family_invite_message,
                "family" to FixedString(notification.familyName),
            )
    }
