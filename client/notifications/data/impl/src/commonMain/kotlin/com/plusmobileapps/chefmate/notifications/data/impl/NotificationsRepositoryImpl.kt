package com.plusmobileapps.chefmate.notifications.data.impl

import com.plusmobileapps.chefmate.di.AppScope
import com.plusmobileapps.chefmate.family.data.FamilyRepository
import com.plusmobileapps.chefmate.grocery.data.GroceryRepository
import com.plusmobileapps.chefmate.notifications.data.AppNotification
import com.plusmobileapps.chefmate.notifications.data.NotificationsRepository
import com.plusmobileapps.chefmate.recipebook.data.RecipeBookCollaborationRepository
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.update

@OptIn(ExperimentalCoroutinesApi::class)
@SingleIn(AppScope::class)
@Inject
@ContributesBinding(AppScope::class)
class NotificationsRepositoryImpl(
    private val groceryRepository: GroceryRepository,
    private val recipeBookCollaborationRepository: RecipeBookCollaborationRepository,
    private val familyRepository: FamilyRepository,
) : NotificationsRepository {

    // Bumped by refresh()/accept()/decline() to force the grocery flow and the one-shot
    // recipe-book/family fetches to re-run. flatMapLatest re-subscribes to the grocery flow on
    // each tick — the grocery pending-invites flow is cold, so re-subscribing re-fetches from the
    // backend. This is what keeps the list (and the badge) current after a user acts on an invite,
    // since no source pushes updates on its own.
    private val refreshTrigger = MutableStateFlow(0L)

    override val notifications: Flow<List<AppNotification>> = refreshTrigger.flatMapLatest {
        combine(
            groceryRepository.getPendingInvitations(),
            // pendingInvites() is one-shot; wrap it so it re-runs whenever we re-subscribe. Each
            // source is caught on its own so one being unreachable doesn't blank the other kinds.
            flow {
                emit(
                    runCatching { recipeBookCollaborationRepository.pendingInvites() }
                        .getOrDefault(emptyList())
                )
            },
            flow {
                emit(runCatching { familyRepository.pendingInvites() }.getOrDefault(emptyList()))
            },
        ) { groceryInvites, recipeBookInvites, familyInvites ->
            groceryInvites.map {
                AppNotification.GroceryInvite(
                    memberId = it.memberId,
                    listName = it.listName,
                    role = it.role,
                )
            } +
                recipeBookInvites.map {
                    AppNotification.RecipeBookInvite(
                        memberId = it.memberId,
                        bookName = it.bookName,
                        role = it.role,
                    )
                } +
                familyInvites.map {
                    AppNotification.FamilyInvite(
                        memberId = it.memberId,
                        familyName = it.familyName,
                        role = it.role,
                    )
                }
        }
    }

    override suspend fun accept(notification: AppNotification) {
        when (notification) {
            is AppNotification.GroceryInvite ->
                groceryRepository.acceptInvitation(notification.memberId)
            is AppNotification.RecipeBookInvite ->
                recipeBookCollaborationRepository.acceptInvite(notification.memberId)
            is AppNotification.FamilyInvite -> familyRepository.acceptInvite(notification.memberId)
        }
        refresh()
    }

    override suspend fun decline(notification: AppNotification) {
        when (notification) {
            is AppNotification.GroceryInvite ->
                groceryRepository.rejectInvitation(notification.memberId)
            is AppNotification.RecipeBookInvite ->
                recipeBookCollaborationRepository.declineInvite(notification.memberId)
            is AppNotification.FamilyInvite -> familyRepository.declineInvite(notification.memberId)
        }
        refresh()
    }

    override fun refresh() {
        refreshTrigger.update { it + 1 }
    }
}
