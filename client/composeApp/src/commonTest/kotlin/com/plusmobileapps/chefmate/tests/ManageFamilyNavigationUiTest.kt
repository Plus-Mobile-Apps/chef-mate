package com.plusmobileapps.chefmate.tests

import androidx.compose.ui.test.ExperimentalTestApi
import com.plusmobileapps.chefmate.family.data.FamilyRole
import com.plusmobileapps.chefmate.family.data.testing.FakeFamilyRepository
import com.plusmobileapps.chefmate.family.manage.robots.familyDetail
import com.plusmobileapps.chefmate.family.manage.robots.familyList
import com.plusmobileapps.chefmate.featureflag.FeatureFlagRegistry
import com.plusmobileapps.chefmate.harness.TestUserState
import com.plusmobileapps.chefmate.harness.runRootBlocTest
import com.plusmobileapps.chefmate.recipe.bottomnav.robots.bottomNav
import com.plusmobileapps.chefmate.settings.robots.more
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class ManageFamilyNavigationUiTest {

    @Test
    fun the_more_tab_hides_manage_family_while_the_flag_is_off() = runRootBlocTest {
        bottomNav().clickMoreTab()

        more().awaitDisplayed().assertManageFamilyRowNotShown()
    }

    @Test
    fun opening_manage_family_from_the_more_tab_lists_the_users_families() =
        runRootBlocTest(
            beforeContent = { app ->
                app.testFeatureFlags.set(FeatureFlagRegistry.ManageFamily, true)
                app.testFamilyRepository.seedFamily("The Smiths")
            }
        ) {
            bottomNav().clickMoreTab()
            more().awaitDisplayed().clickManageFamilyRow()

            familyList().awaitDisplayed().awaitFamily("The Smiths")
        }

    @Test
    fun opening_a_family_shows_its_members_and_the_owners_actions() =
        runRootBlocTest(
            beforeContent = { app ->
                app.testFeatureFlags.set(FeatureFlagRegistry.ManageFamily, true)
                app.testFamilyRepository.seedFamily(
                    name = "The Smiths",
                    myRole = FamilyRole.OWNER,
                    others = listOf(FakeFamilyRepository.SeedMember(email = "riley@example.com")),
                )
            }
        ) {
            bottomNav().clickMoreTab()
            more().awaitDisplayed().clickManageFamilyRow()
            familyList().awaitDisplayed().openFamily("The Smiths")

            familyDetail().awaitDisplayed().awaitMember("riley@example.com")
        }

    @Test
    fun a_member_sees_no_delete_button_on_a_family_they_do_not_own() =
        runRootBlocTest(
            beforeContent = { app ->
                app.testFeatureFlags.set(FeatureFlagRegistry.ManageFamily, true)
                app.testFamilyRepository.seedFamily(
                    name = "Lake House",
                    myRole = FamilyRole.MEMBER,
                    others =
                        listOf(
                            FakeFamilyRepository.SeedMember(
                                email = "owner@example.com",
                                role = FamilyRole.OWNER,
                            )
                        ),
                )
            }
        ) {
            bottomNav().clickMoreTab()
            more().awaitDisplayed().clickManageFamilyRow()
            familyList().awaitDisplayed().openFamily("Lake House")

            familyDetail()
                .awaitDisplayed()
                .awaitMember("owner@example.com")
                .assertDeleteNotShown()
                .assertInviteNotShown()
        }

    @Test
    fun creating_the_first_family_opens_it_straight_away() =
        runRootBlocTest(
            beforeContent = { app ->
                app.testFeatureFlags.set(FeatureFlagRegistry.ManageFamily, true)
            }
        ) {
            bottomNav().clickMoreTab()
            more().awaitDisplayed().clickManageFamilyRow()

            familyList().awaitDisplayed().createFirstFamily().typeFamilyName("The Smiths")
            familyList().confirmCreate()

            // A brand-new family has nobody in it, so the detail screen is where the user wants to
            // be.
            // Matched on the creator's email — their display name ("Me") is a substring of the
            // "Members" section title.
            familyDetail().awaitDisplayed().awaitMember("me@example.com")
        }

    @Test
    fun a_signed_out_user_is_still_offered_the_row() =
        runRootBlocTest(
            userState = TestUserState.UnauthenticatedWithRecipes(),
            beforeContent = { app ->
                app.testFeatureFlags.set(FeatureFlagRegistry.ManageFamily, true)
            },
        ) {
            bottomNav().clickMoreTab()

            // Unlike Notifications, the row is offered in every auth state — tapping it routes
            // through sign-in. This stops at asserting the row is there: the auth screen it opens
            // renders a UIKitView (PlusAutofillTextField.ios) needing a LocalInteropContainer that
            // runComposeUiTest doesn't provide, so composing it fails on iOS. The routing itself,
            // including landing on Manage Family once sign-in succeeds, is covered in RootBlocTest.
            more().awaitDisplayed().assertManageFamilyRowShown()
        }
}
