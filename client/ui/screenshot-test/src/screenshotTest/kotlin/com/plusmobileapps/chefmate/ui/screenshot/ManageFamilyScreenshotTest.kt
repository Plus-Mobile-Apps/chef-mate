package com.plusmobileapps.chefmate.ui.screenshot

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.plusmobileapps.chefmate.family.manage.FamilyDetailScreen
import com.plusmobileapps.chefmate.family.manage.FamilyListScreen
import com.plusmobileapps.chefmate.family.manage.previewFamilyDetailAdminBloc
import com.plusmobileapps.chefmate.family.manage.previewFamilyDetailDeleteDialogBloc
import com.plusmobileapps.chefmate.family.manage.previewFamilyDetailInviteErrorBloc
import com.plusmobileapps.chefmate.family.manage.previewFamilyDetailMemberBloc
import com.plusmobileapps.chefmate.family.manage.previewFamilyDetailOwnerBloc
import com.plusmobileapps.chefmate.family.manage.previewFamilyListBloc
import com.plusmobileapps.chefmate.family.manage.previewFamilyListCreateDialogBloc
import com.plusmobileapps.chefmate.family.manage.previewFamilyListEmptyBloc
import com.plusmobileapps.chefmate.family.manage.previewFamilyListErrorBloc
import com.plusmobileapps.chefmate.family.manage.previewFamilyListLoadingBloc
import com.plusmobileapps.chefmate.ui.theme.ChefMateTheme

// Manage Family: the list of families and one family's detail, captured per role because the
// available actions (invite, role picker, rename, leave, delete) differ for owner, admin and
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
fun FamilyDetailInviteErrorScreenshot() {
    ChefMateTheme { FamilyDetailScreen(bloc = previewFamilyDetailInviteErrorBloc) }
}

@PreviewTest
@Preview(showBackground = true, heightDp = 1100)
@Composable
fun FamilyDetailDeleteDialogScreenshot() {
    ChefMateTheme { FamilyDetailScreen(bloc = previewFamilyDetailDeleteDialogBloc) }
}
