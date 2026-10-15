package com.plusmobileapps.chefmate.ui.screenshot

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.plusmobileapps.chefmate.family.manage.FamilyDetailScreen
import com.plusmobileapps.chefmate.family.manage.FamilyListScreen
import com.plusmobileapps.chefmate.family.manage.previewFamilyDetailAdminBloc
import com.plusmobileapps.chefmate.family.manage.previewFamilyDetailCreateGroceryListDialogBloc
import com.plusmobileapps.chefmate.family.manage.previewFamilyDetailCreateRecipeBookDialogBloc
import com.plusmobileapps.chefmate.family.manage.previewFamilyDetailDeleteDialogBloc
import com.plusmobileapps.chefmate.family.manage.previewFamilyDetailDeleteGroceryListDialogBloc
import com.plusmobileapps.chefmate.family.manage.previewFamilyDetailDeleteRecipeBookDialogBloc
import com.plusmobileapps.chefmate.family.manage.previewFamilyDetailGroceryListsEmptyBloc
import com.plusmobileapps.chefmate.family.manage.previewFamilyDetailGroceryListsErrorBloc
import com.plusmobileapps.chefmate.family.manage.previewFamilyDetailGroceryListsMemberBloc
import com.plusmobileapps.chefmate.family.manage.previewFamilyDetailGroceryListsOwnerBloc
import com.plusmobileapps.chefmate.family.manage.previewFamilyDetailInviteDialogBloc
import com.plusmobileapps.chefmate.family.manage.previewFamilyDetailInviteErrorBloc
import com.plusmobileapps.chefmate.family.manage.previewFamilyDetailMemberBloc
import com.plusmobileapps.chefmate.family.manage.previewFamilyDetailOwnerBloc
import com.plusmobileapps.chefmate.family.manage.previewFamilyDetailRecipeBooksEmptyBloc
import com.plusmobileapps.chefmate.family.manage.previewFamilyDetailRecipeBooksErrorBloc
import com.plusmobileapps.chefmate.family.manage.previewFamilyDetailRecipeBooksMemberBloc
import com.plusmobileapps.chefmate.family.manage.previewFamilyDetailRecipeBooksOwnerBloc
import com.plusmobileapps.chefmate.family.manage.previewFamilyDetailSharedSectionsBloc
import com.plusmobileapps.chefmate.family.manage.previewFamilyListBloc
import com.plusmobileapps.chefmate.family.manage.previewFamilyListCreateDialogBloc
import com.plusmobileapps.chefmate.family.manage.previewFamilyListEmptyBloc
import com.plusmobileapps.chefmate.family.manage.previewFamilyListErrorBloc
import com.plusmobileapps.chefmate.family.manage.previewFamilyListLoadingBloc
import com.plusmobileapps.chefmate.ui.theme.ChefMateTheme

// Manage Family: the list of families and one family's detail, captured per role because the
// available actions (invite dialog, role picker, rename, leave, delete) differ for owner, admin and
// member.

@PreviewTest
@Preview(showBackground = true, heightDp = 800)
@Composable
fun FamilyListScreenshot() {
    ChefMateTheme { FamilyListScreen(bloc = previewFamilyListBloc) }
}

@PreviewTest
@Preview(showBackground = true, heightDp = 800, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun FamilyListDarkScreenshot() {
    ChefMateTheme(darkTheme = true) { FamilyListScreen(bloc = previewFamilyListBloc) }
}

@PreviewTest
@Preview(showBackground = true, heightDp = 800)
@Composable
fun FamilyListEmptyScreenshot() {
    ChefMateTheme { FamilyListScreen(bloc = previewFamilyListEmptyBloc) }
}

@PreviewTest
@Preview(showBackground = true, heightDp = 800)
@Composable
fun FamilyListLoadingScreenshot() {
    ChefMateTheme { FamilyListScreen(bloc = previewFamilyListLoadingBloc) }
}

@PreviewTest
@Preview(showBackground = true, heightDp = 800)
@Composable
fun FamilyListErrorScreenshot() {
    ChefMateTheme { FamilyListScreen(bloc = previewFamilyListErrorBloc) }
}

@PreviewTest
@Preview(showBackground = true, heightDp = 800)
@Composable
fun FamilyListCreateDialogScreenshot() {
    ChefMateTheme { FamilyListScreen(bloc = previewFamilyListCreateDialogBloc) }
}

@PreviewTest
@Preview(showBackground = true, heightDp = 1100)
@Composable
fun FamilyDetailOwnerScreenshot() {
    ChefMateTheme { FamilyDetailScreen(bloc = previewFamilyDetailOwnerBloc) }
}

@PreviewTest
@Preview(showBackground = true, heightDp = 1100, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun FamilyDetailOwnerDarkScreenshot() {
    ChefMateTheme(darkTheme = true) { FamilyDetailScreen(bloc = previewFamilyDetailOwnerBloc) }
}

@PreviewTest
@Preview(showBackground = true, heightDp = 1100)
@Composable
fun FamilyDetailAdminScreenshot() {
    ChefMateTheme { FamilyDetailScreen(bloc = previewFamilyDetailAdminBloc) }
}

@PreviewTest
@Preview(showBackground = true, heightDp = 1100)
@Composable
fun FamilyDetailMemberScreenshot() {
    ChefMateTheme { FamilyDetailScreen(bloc = previewFamilyDetailMemberBloc) }
}

@PreviewTest
@Preview(showBackground = true, heightDp = 1100)
@Composable
fun FamilyDetailInviteDialogScreenshot() {
    ChefMateTheme { FamilyDetailScreen(bloc = previewFamilyDetailInviteDialogBloc) }
}

@PreviewTest
@Preview(showBackground = true, heightDp = 1100)
@Composable
fun FamilyDetailInviteErrorScreenshot() {
    ChefMateTheme { FamilyDetailScreen(bloc = previewFamilyDetailInviteErrorBloc) }
}

@PreviewTest
@Preview(showBackground = true, heightDp = 1100)
@Composable
fun FamilyDetailDeleteDialogScreenshot() {
    ChefMateTheme { FamilyDetailScreen(bloc = previewFamilyDetailDeleteDialogBloc) }
}

// Family grocery lists: owners/admins get rename/delete menus and "New grocery list"; members
// only see the lists. A failed lists load errors the section, not the screen.

@PreviewTest
@Preview(showBackground = true, heightDp = 1200)
@Composable
fun FamilyDetailGroceryListsOwnerScreenshot() {
    ChefMateTheme { FamilyDetailScreen(bloc = previewFamilyDetailGroceryListsOwnerBloc) }
}

@PreviewTest
@Preview(showBackground = true, heightDp = 1200, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun FamilyDetailGroceryListsOwnerDarkScreenshot() {
    ChefMateTheme(darkTheme = true) {
        FamilyDetailScreen(bloc = previewFamilyDetailGroceryListsOwnerBloc)
    }
}

@PreviewTest
@Preview(showBackground = true, heightDp = 1200)
@Composable
fun FamilyDetailGroceryListsMemberScreenshot() {
    ChefMateTheme { FamilyDetailScreen(bloc = previewFamilyDetailGroceryListsMemberBloc) }
}

@PreviewTest
@Preview(showBackground = true, heightDp = 1200)
@Composable
fun FamilyDetailGroceryListsEmptyScreenshot() {
    ChefMateTheme { FamilyDetailScreen(bloc = previewFamilyDetailGroceryListsEmptyBloc) }
}

@PreviewTest
@Preview(showBackground = true, heightDp = 1200)
@Composable
fun FamilyDetailGroceryListsErrorScreenshot() {
    ChefMateTheme { FamilyDetailScreen(bloc = previewFamilyDetailGroceryListsErrorBloc) }
}

@PreviewTest
@Preview(showBackground = true, heightDp = 1200)
@Composable
fun FamilyDetailCreateGroceryListDialogScreenshot() {
    ChefMateTheme { FamilyDetailScreen(bloc = previewFamilyDetailCreateGroceryListDialogBloc) }
}

@PreviewTest
@Preview(showBackground = true, heightDp = 1200)
@Composable
fun FamilyDetailDeleteGroceryListDialogScreenshot() {
    ChefMateTheme { FamilyDetailScreen(bloc = previewFamilyDetailDeleteGroceryListDialogBloc) }
}

@PreviewTest
@Preview(showBackground = true, heightDp = 1200)
@Composable
fun FamilyDetailRecipeBooksOwnerScreenshot() {
    ChefMateTheme { FamilyDetailScreen(bloc = previewFamilyDetailRecipeBooksOwnerBloc) }
}

@PreviewTest
@Preview(showBackground = true, heightDp = 1200, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun FamilyDetailRecipeBooksOwnerDarkScreenshot() {
    ChefMateTheme(darkTheme = true) {
        FamilyDetailScreen(bloc = previewFamilyDetailRecipeBooksOwnerBloc)
    }
}

@PreviewTest
@Preview(showBackground = true, heightDp = 1200)
@Composable
fun FamilyDetailRecipeBooksMemberScreenshot() {
    ChefMateTheme { FamilyDetailScreen(bloc = previewFamilyDetailRecipeBooksMemberBloc) }
}

@PreviewTest
@Preview(showBackground = true, heightDp = 1200)
@Composable
fun FamilyDetailRecipeBooksEmptyScreenshot() {
    ChefMateTheme { FamilyDetailScreen(bloc = previewFamilyDetailRecipeBooksEmptyBloc) }
}

@PreviewTest
@Preview(showBackground = true, heightDp = 1200)
@Composable
fun FamilyDetailRecipeBooksErrorScreenshot() {
    ChefMateTheme { FamilyDetailScreen(bloc = previewFamilyDetailRecipeBooksErrorBloc) }
}

@PreviewTest
@Preview(showBackground = true, heightDp = 1200)
@Composable
fun FamilyDetailCreateRecipeBookDialogScreenshot() {
    ChefMateTheme { FamilyDetailScreen(bloc = previewFamilyDetailCreateRecipeBookDialogBloc) }
}

@PreviewTest
@Preview(showBackground = true, heightDp = 1200)
@Composable
fun FamilyDetailDeleteRecipeBookDialogScreenshot() {
    ChefMateTheme { FamilyDetailScreen(bloc = previewFamilyDetailDeleteRecipeBookDialogBloc) }
}

@PreviewTest
@Preview(showBackground = true, heightDp = 1600)
@Composable
fun FamilyDetailSharedSectionsScreenshot() {
    ChefMateTheme { FamilyDetailScreen(bloc = previewFamilyDetailSharedSectionsBloc) }
}
