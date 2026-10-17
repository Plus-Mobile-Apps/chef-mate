package com.plusmobileapps.chefmate.notifications.data.impl

import com.plusmobileapps.chefmate.auth.data.AuthState
import com.plusmobileapps.chefmate.auth.data.AuthenticationRepository
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
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

@OptIn(ExperimentalCoroutinesApi::class)
@SingleIn(AppScope::class)
@Inject
@ContributesBinding(AppScope::class)
class NotificationsRepositoryImpl(
    private val groceryRepository: GroceryRepository,
    private val recipeBookCollaborationRepository: RecipeBookCollaborationRepository,
    private val familyRepository: FamilyRepository,
    authenticationRepository: AuthenticationRepository,
) : NotificationsRepository {

    // Bumped by refresh()/accept()/decline() to force the grocery flow and the one-shot
    // recipe-book/family fetches to re-run. flatMapLatest re-subscribes to the grocery flow on
    // each tick — the grocery pending-invites flow is cold, so re-subscribing re-fetches from the
    // backend. This is what keeps the list (and the badge) current after a user acts on an invite,
    // since no source pushes updates on its own.
    private val refreshTrigger = MutableStateFlow(0L)

    // The account whose invites the one-shot fetches should load, or null when there is none
    // (signed out, or an anonymous guest — invites are addressed to an email). Keying the
    // fetches on this matters on a cold start from an invite email: the app opens straight onto
    // Notifications before the Supabase session has been restored, so a fetch made then runs
    // unauthenticated, is refused, and would otherwise stay empty after the session arrives.
    private val signedInEmail: Flow<String?> =
        authenticationRepository.state
            .map { state ->
                (state as? AuthState.Authenticated)
                    ?.user
                    ?.takeUnless { it.isAnonymous }
                    ?.userEmail
                    ?.trim()
                    ?.lowercase()
                    ?.takeUnless { it.isEmpty() }
            }
            .distinctUntilChanged()

    override val notifications: Flow<List<AppNotification>> =
        combine(refreshTrigger, signedInEmail) { _, email -> email }
            .flatMapLatest { email ->
                combine(
                    groceryRepository.getPendingInvitations(),
                    // pendingInvites() is one-shot; wrap it so it re-runs whenever we
                    // re-subscribe. Each source is caught on its own so one being unreachable
                    // doesn't blank the other kinds.
                    oneShot(email) { recipeBookCollaborationRepository.pendingInvites() },
                    oneShot(email) { familyRepository.pendingInvites() },
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

    private fun <T> oneShot(email: String?, fetch: suspend () -> List<T>): Flow<List<T>> = flow {
        emit(if (email == null) emptyList() else runCatching { fetch() }.getOrDefault(emptyList()))
    }
}
