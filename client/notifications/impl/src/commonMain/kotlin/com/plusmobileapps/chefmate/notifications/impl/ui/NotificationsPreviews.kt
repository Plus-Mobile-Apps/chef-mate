package com.plusmobileapps.chefmate.notifications.impl.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.plusmobileapps.chefmate.family.data.FamilyRole
import com.plusmobileapps.chefmate.grocery.data.ListRole
import com.plusmobileapps.chefmate.notifications.NotificationsBloc
import com.plusmobileapps.chefmate.notifications.NotificationsBloc.Model
import com.plusmobileapps.chefmate.notifications.data.AppNotification
import com.plusmobileapps.chefmate.recipebook.data.RecipeBookRole
import com.plusmobileapps.chefmate.ui.theme.ChefMateTheme
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.MutableStateFlow

private fun notificationsBloc(model: Model): NotificationsBloc =
    object : NotificationsBloc {
        override val state = MutableStateFlow(model)

        override fun onBack() = Unit

        override fun onAccept(notification: AppNotification) = Unit

        override fun onDecline(notification: AppNotification) = Unit

        override fun onSignInClicked() = Unit

        override fun onSignUpClicked() = Unit
    }

val previewNotificationsBloc: NotificationsBloc =
    notificationsBloc(
        Model(
            notifications =
                persistentListOf(
                    AppNotification.GroceryInvite(
                        memberId = "g1",
                        listName = "Weeknight Dinners",
                        role = ListRole.EDITOR,
                    ),
                    AppNotification.RecipeBookInvite(
                        memberId = "b1",
                        bookName = "Holiday Baking",
                        role = RecipeBookRole.EDITOR,
                    ),
                    AppNotification.FamilyInvite(
                        memberId = "f1",
                        familyName = "The Smiths",
                        role = FamilyRole.MEMBER,
                    ),
                ),
            isLoading = false,
        )
    )

val previewNotificationsProcessingBloc: NotificationsBloc =
    notificationsBloc(
        Model(
            notifications =
                persistentListOf(
                    AppNotification.GroceryInvite(
                        memberId = "g1",
                        listName = "Weeknight Dinners",
                        role = ListRole.EDITOR,
                    )
                ),
            isLoading = false,
            processing = kotlinx.collections.immutable.persistentSetOf("grocery:g1"),
        )
    )

val previewNotificationsEmptyBloc: NotificationsBloc = notificationsBloc(Model(isLoading = false))

val previewNotificationsSignedOutBloc: NotificationsBloc =
    notificationsBloc(Model(isLoading = false, isSignedIn = false))

@Preview
@Composable
internal fun NotificationsPreview() {
    ChefMateTheme { previewNotificationsBloc.Content(Modifier) }
}

@Preview
@Composable
internal fun NotificationsEmptyPreview() {
    ChefMateTheme { previewNotificationsEmptyBloc.Content(Modifier) }
}

@Preview
@Composable
internal fun NotificationsSignedOutPreview() {
    ChefMateTheme { previewNotificationsSignedOutBloc.Content(Modifier) }
}
