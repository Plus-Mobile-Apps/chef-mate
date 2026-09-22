@file:Suppress("FunctionName")
@file:OptIn(ExperimentalCoroutinesApi::class)

package com.plusmobileapps.chefmate.family.manage.impl

import com.plusmobileapps.chefmate.family.data.FamilyMemberStatus
import com.plusmobileapps.chefmate.family.data.FamilyRole
import com.plusmobileapps.chefmate.family.data.testing.FakeFamilyRepository
import com.plusmobileapps.chefmate.family.manage.FamilyDetailBloc.Dialog
import com.plusmobileapps.chefmate.toast.testing.FakeToastService
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlin.test.Test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest

class FamilyDetailViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val toastService = FakeToastService()

    private fun viewModel(
        repository: FakeFamilyRepository,
        familyId: String,
        onClosed: () -> Unit = {},
    ) =
        FamilyDetailViewModel(
            familyId = familyId,
            onClosed = onClosed,
            mainContext = testDispatcher,
            repository = repository,
            toastService = toastService,
        )

    private fun ownedFamily(): Pair<FakeFamilyRepository, String> {
        val repository = FakeFamilyRepository()
        val familyId =
            repository.seedFamily(
                name = "Smiths",
                myRole = FamilyRole.OWNER,
                others =
                    listOf(
                        FakeFamilyRepository.SeedMember(
                            email = "admin@example.com",
                            role = FamilyRole.ADMIN,
                            id = "admin-row",
                        ),
                        FakeFamilyRepository.SeedMember(
                            email = "member@example.com",
                            role = FamilyRole.MEMBER,
                            id = "member-row",
                        ),
                        FakeFamilyRepository.SeedMember(
                            email = "pending@example.com",
                            status = FamilyMemberStatus.PENDING,
                            id = "pending-row",
                        ),
                    ),
            )
        return repository to familyId
    }

    @Test
    fun an_owner_sees_every_action_and_the_invites_listed_separately() =
        runTest(testDispatcher) {
            val (repository, familyId) = ownedFamily()

            val vm = viewModel(repository, familyId)

            val state = vm.state.value
            state.isLoading shouldBe false
            state.name shouldBe "Smiths"
            state.myRole shouldBe FamilyRole.OWNER
            state.members.map { it.member.email } shouldBe
                listOf("me@example.com", "admin@example.com", "member@example.com")
            state.invites.map { it.member.email } shouldBe listOf("pending@example.com")
            state.invitableRoles shouldBe listOf(FamilyRole.MEMBER, FamilyRole.ADMIN)
            state.canRename shouldBe true
            state.canDelete shouldBe true
            // The owner can't leave — deleting is the only way out for them.
            state.canLeave shouldBe false
        }

    @Test
    fun an_owner_may_promote_a_member_and_demote_an_admin_but_not_touch_themselves() =
        runTest(testDispatcher) {
            val (repository, familyId) = ownedFamily()

            val vm = viewModel(repository, familyId)

            val rows = vm.state.value.members.associateBy { it.member.email }
            rows.getValue("member@example.com").canPromote shouldBe true
            rows.getValue("member@example.com").canDemote shouldBe false
            rows.getValue("admin@example.com").canDemote shouldBe true
            rows.getValue("admin@example.com").canRemove shouldBe true
            rows.getValue("me@example.com").hasActions shouldBe false
        }

    @Test
    fun an_admin_may_only_invite_members_and_cannot_delete_the_family() =
        runTest(testDispatcher) {
            val repository = FakeFamilyRepository()
            val familyId =
                repository.seedFamily(
                    name = "Smiths",
                    myRole = FamilyRole.ADMIN,
                    others =
                        listOf(
                            FakeFamilyRepository.SeedMember(
                                email = "owner@example.com",
                                role = FamilyRole.OWNER,
                            ),
                            FakeFamilyRepository.SeedMember(email = "member@example.com"),
                        ),
                )

            val vm = viewModel(repository, familyId)

            val state = vm.state.value
            state.invitableRoles shouldBe listOf(FamilyRole.MEMBER)
            state.inviteRole shouldBe FamilyRole.MEMBER
            state.canRename shouldBe true
            state.canDelete shouldBe false
            state.canLeave shouldBe true
            state.members.single { it.member.email == "owner@example.com" }.canRemove shouldBe false
        }

    @Test
    fun a_member_gets_no_actions_beyond_leaving() =
        runTest(testDispatcher) {
            val repository = FakeFamilyRepository()
            val familyId =
                repository.seedFamily(
                    name = "Smiths",
                    myRole = FamilyRole.MEMBER,
                    others =
                        listOf(
                            FakeFamilyRepository.SeedMember(
                                email = "owner@example.com",
                                role = FamilyRole.OWNER,
                            )
                        ),
                )

            val vm = viewModel(repository, familyId)

            val state = vm.state.value
            state.invitableRoles shouldBe emptyList()
            state.canRename shouldBe false
            state.canDelete shouldBe false
            state.canLeave shouldBe true
            state.members.none { it.hasActions } shouldBe true
        }

    @Test
    fun inviting_rejects_a_malformed_email_without_calling_the_backend() =
        runTest(testDispatcher) {
            val (repository, familyId) = ownedFamily()
            val vm = viewModel(repository, familyId)

            vm.onInviteEmailChanged("not-an-email")
            vm.onInviteClicked()

            vm.state.value.inviteError shouldNotBe null
            vm.state.value.inviteEmail shouldBe "not-an-email"
            repository.getMembers(familyId).none { it.email == "not-an-email" } shouldBe true
        }

    @Test
    fun a_successful_invite_clears_the_field_toasts_and_reloads() =
        runTest(testDispatcher) {
            val (repository, familyId) = ownedFamily()
            val vm = viewModel(repository, familyId)

            vm.onInviteEmailChanged("new@example.com")
            vm.onInviteRoleChanged(FamilyRole.ADMIN)
            vm.onInviteClicked()

            vm.state.value.inviteEmail shouldBe ""
            vm.state.value.inviteError shouldBe null
            vm.state.value.isInviting shouldBe false
            toastService.shown.size shouldBe 1
            vm.state.value.invites.map { it.member.email } shouldBe
                listOf("new@example.com", "pending@example.com")
        }

    @Test
    fun a_rejected_invite_surfaces_an_inline_error() =
        runTest(testDispatcher) {
            val (repository, familyId) = ownedFamily()
            val vm = viewModel(repository, familyId)

            // Already invited — the backend rejects a duplicate.
            vm.onInviteEmailChanged("pending@example.com")
            vm.onInviteClicked()

            vm.state.value.inviteError shouldNotBe null
            vm.state.value.isInviting shouldBe false
            toastService.shown.size shouldBe 0
        }

    @Test
    fun promoting_a_member_persists_and_reloads_the_row() =
        runTest(testDispatcher) {
            val (repository, familyId) = ownedFamily()
            val vm = viewModel(repository, familyId)

            vm.onPromoteClicked("member-row")

            vm.state.value.isWorking shouldBe false
            vm.state.value.members
                .single { it.member.email == "member@example.com" }
                .member
                .role shouldBe FamilyRole.ADMIN
        }

    @Test
    fun demoting_an_admin_persists() =
        runTest(testDispatcher) {
            val (repository, familyId) = ownedFamily()
            val vm = viewModel(repository, familyId)

            vm.onDemoteClicked("admin-row")

            vm.state.value.members
                .single { it.member.email == "admin@example.com" }
                .member
                .role shouldBe FamilyRole.MEMBER
        }

    @Test
    fun removing_a_member_asks_first_and_then_drops_the_row() =
        runTest(testDispatcher) {
            val (repository, familyId) = ownedFamily()
            val vm = viewModel(repository, familyId)

            vm.onRemoveMemberClicked("member-row")
            (vm.state.value.dialog as Dialog.RemoveMember).member.email shouldBe
                "member@example.com"

            vm.onDialogConfirmed()

            vm.state.value.dialog shouldBe null
            vm.state.value.members.none { it.member.email == "member@example.com" } shouldBe true
        }

    @Test
    fun dismissing_a_dialog_leaves_everything_untouched() =
        runTest(testDispatcher) {
            val (repository, familyId) = ownedFamily()
            val vm = viewModel(repository, familyId)

            vm.onRemoveMemberClicked("member-row")
            vm.onDialogDismissed()

            vm.state.value.dialog shouldBe null
            vm.state.value.members.map { it.member.email } shouldBe
                listOf("me@example.com", "admin@example.com", "member@example.com")
        }

    @Test
    fun renaming_updates_the_title() =
        runTest(testDispatcher) {
            val (repository, familyId) = ownedFamily()
            val vm = viewModel(repository, familyId)

            vm.onRenameClicked()
            (vm.state.value.dialog as Dialog.Rename).name shouldBe "Smiths"
            vm.onRenameNameChanged("The Smiths")
            vm.onDialogConfirmed()

            vm.state.value.name shouldBe "The Smiths"
            vm.state.value.dialog shouldBe null
        }

    @Test
    fun renaming_to_a_blank_name_is_refused() =
        runTest(testDispatcher) {
            val (repository, familyId) = ownedFamily()
            val vm = viewModel(repository, familyId)

            vm.onRenameClicked()
            vm.onRenameNameChanged("   ")
            vm.onDialogConfirmed()

            vm.state.value.dialog shouldNotBe null
            vm.state.value.name shouldBe "Smiths"
        }

    @Test
    fun deleting_the_family_closes_the_screen() =
        runTest(testDispatcher) {
            val (repository, familyId) = ownedFamily()
            var closed = false
            val vm = viewModel(repository, familyId, onClosed = { closed = true })

            vm.onDeleteClicked()
            vm.state.value.dialog shouldBe Dialog.Delete
            vm.onDialogConfirmed()

            closed shouldBe true
            repository.getFamilies() shouldBe emptyList()
        }

    @Test
    fun leaving_the_family_closes_the_screen() =
        runTest(testDispatcher) {
            val repository = FakeFamilyRepository()
            val familyId =
                repository.seedFamily(
                    name = "Smiths",
                    myRole = FamilyRole.MEMBER,
                    others =
                        listOf(
                            FakeFamilyRepository.SeedMember(
                                email = "owner@example.com",
                                role = FamilyRole.OWNER,
                            )
                        ),
                )
            var closed = false
            val vm = viewModel(repository, familyId, onClosed = { closed = true })

            vm.onLeaveClicked()
            vm.onDialogConfirmed()

            closed shouldBe true
            repository.getFamilies() shouldBe emptyList()
        }

    @Test
    fun a_failed_action_toasts_and_leaves_the_screen_open() =
        runTest(testDispatcher) {
            val (repository, familyId) = ownedFamily()
            var closed = false
            val vm = viewModel(repository, familyId, onClosed = { closed = true })

            repository.failure = IllegalStateException("offline")
            vm.onDeleteClicked()
            vm.onDialogConfirmed()

            closed shouldBe false
            vm.state.value.isWorking shouldBe false
            toastService.shown.size shouldBe 1
        }

    @Test
    fun a_family_that_is_gone_shows_the_load_error() =
        runTest(testDispatcher) {
            val repository = FakeFamilyRepository()

            val vm = viewModel(repository, familyId = "never-existed")

            vm.state.value.isLoading shouldBe false
            vm.state.value.loadError shouldNotBe null
        }

    @Test
    fun retrying_after_a_failed_load_recovers() =
        runTest(testDispatcher) {
            val (repository, familyId) = ownedFamily()
            repository.failure = IllegalStateException("offline")
            val vm = viewModel(repository, familyId)
            vm.state.value.loadError shouldNotBe null

            repository.failure = null
            vm.load()

            vm.state.value.loadError shouldBe null
            vm.state.value.name shouldBe "Smiths"
        }
}
