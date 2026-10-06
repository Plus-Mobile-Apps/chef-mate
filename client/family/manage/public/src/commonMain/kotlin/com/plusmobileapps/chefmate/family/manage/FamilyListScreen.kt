package com.plusmobileapps.chefmate.family.manage

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import chefmate.client.family.manage.public.generated.resources.Res
import chefmate.client.family.manage.public.generated.resources.family_create_confirm
import chefmate.client.family.manage.public.generated.resources.family_create_title
import chefmate.client.family.manage.public.generated.resources.family_list_create_a11y
import chefmate.client.family.manage.public.generated.resources.family_list_create_first
import chefmate.client.family.manage.public.generated.resources.family_list_empty_message
import chefmate.client.family.manage.public.generated.resources.family_list_empty_title
import chefmate.client.family.manage.public.generated.resources.family_list_member_count
import chefmate.client.family.manage.public.generated.resources.family_list_section
import chefmate.client.family.manage.public.generated.resources.family_list_title
import chefmate.client.family.manage.public.generated.resources.family_retry
import com.plusmobileapps.chefmate.family.data.Family
import com.plusmobileapps.chefmate.text.FixedString
import com.plusmobileapps.chefmate.text.PhraseModel
import com.plusmobileapps.chefmate.text.TextData
import com.plusmobileapps.chefmate.text.asTextData
import com.plusmobileapps.chefmate.ui.components.PlusButton
import com.plusmobileapps.chefmate.ui.components.PlusHeaderContainer
import com.plusmobileapps.chefmate.ui.components.PlusHeaderData
import com.plusmobileapps.chefmate.ui.components.PlusLoadingIndicator
import com.plusmobileapps.chefmate.ui.theme.ChefMateTheme

@Composable
fun FamilyListScreen(bloc: FamilyListBloc, modifier: Modifier = Modifier) {
    val model by bloc.state.collectAsState()

    PlusHeaderContainer(
        modifier = modifier.testTag(FamilyListTestTags.SCREEN),
        data =
            PlusHeaderData.Child(
                title = Res.string.family_list_title.asTextData(),
                onBackClick = bloc::onBackClicked,
                trailingAccessory =
                    if (model.families.isNotEmpty()) {
                        PlusHeaderData.TrailingAccessory.Icon(
                            icon = Icons.Default.Add,
                            contentDesc = Res.string.family_list_create_a11y.asTextData(),
                            onClick = bloc::onCreateFamilyClicked,
                        )
                    } else {
                        null
                    },
            ),
    ) {
        val loadError = model.loadError
        when {
            model.isLoading -> CenteredLoading()
            loadError != null -> LoadError(message = loadError, onRetry = bloc::onRetryClicked)
            model.isEmpty -> EmptyFamilies(onCreate = bloc::onCreateFamilyClicked)
            else -> {
                SectionTitle(Res.string.family_list_section.asTextData())
                model.families.forEach { family ->
                    FamilyRow(family = family, onClick = { bloc.onFamilyClicked(family.id) })
                    HorizontalDivider()
                }
            }
        }
    }

    model.createDialog?.let { dialog ->
        FamilyNameDialog(
            title = Res.string.family_create_title.asTextData(),
            confirmText = Res.string.family_create_confirm.asTextData(),
            name = dialog.name,
            canConfirm = dialog.canConfirm,
            isSaving = dialog.isSaving,
            error = dialog.error,
            onNameChange = bloc::onNewFamilyNameChanged,
            onConfirm = bloc::onCreateConfirmed,
            onDismiss = bloc::onCreateDismissed,
        )
    }
}

@Composable
internal fun SectionTitle(title: TextData, modifier: Modifier = Modifier) {
    Text(
        text = title.localized(),
        style = ChefMateTheme.typography.titleSmall,
        color = ChefMateTheme.colorScheme.primary,
        modifier =
            modifier.padding(
                horizontal = ChefMateTheme.dimens.paddingNormal,
                vertical = ChefMateTheme.dimens.paddingSmall,
            ),
    )
}

@Composable
private fun FamilyRow(family: Family, onClick: () -> Unit) {
    val subtitle =
        PhraseModel(
                Res.string.family_list_member_count,
                "count" to FixedString(family.memberCount.toString()),
                "role" to family.myRole.label(),
            )
            .localized()
    Row(
        modifier =
            Modifier.fillMaxWidth()
                .heightIn(min = ChefMateTheme.dimens.rowHeight)
                .clickable(onClick = onClick)
                .padding(
                    horizontal = ChefMateTheme.dimens.paddingNormal,
                    vertical = ChefMateTheme.dimens.paddingSmall,
                )
                .semantics(mergeDescendants = true) {
                    contentDescription = "${family.name}, $subtitle"
                }
                .testTag(FamilyListTestTags.FAMILY_ROW),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = family.name, style = ChefMateTheme.typography.titleMedium)
            Text(
                text = subtitle,
                style = ChefMateTheme.typography.bodySmall,
                color = ChefMateTheme.colorScheme.onSurfaceVariant,
            )
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null)
    }
}

@Composable
private fun EmptyFamilies(onCreate: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(ChefMateTheme.dimens.paddingLarge),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(ChefMateTheme.dimens.paddingNormal),
    ) {
        Text(
            text = Res.string.family_list_empty_title.asTextData().localized(),
            style = ChefMateTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
        )
        Text(
            text = Res.string.family_list_empty_message.asTextData().localized(),
            style = ChefMateTheme.typography.bodyMedium,
            color = ChefMateTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        PlusButton(
            text = Res.string.family_list_create_first.asTextData(),
            onClick = onCreate,
            modifier = Modifier.testTag(FamilyListTestTags.CREATE_FIRST_BUTTON),
        )
    }
}

@Composable
internal fun CenteredLoading() {
    Box(
        modifier = Modifier.fillMaxWidth().padding(ChefMateTheme.dimens.paddingExtraLarge),
        contentAlignment = Alignment.Center,
    ) {
        PlusLoadingIndicator()
    }
}

@Composable
internal fun LoadError(message: TextData, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(ChefMateTheme.dimens.paddingLarge),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(ChefMateTheme.dimens.paddingNormal),
    ) {
        Text(
            text = message.localized(),
            style = ChefMateTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
        PlusButton(text = Res.string.family_retry.asTextData(), onClick = onRetry)
    }
}
