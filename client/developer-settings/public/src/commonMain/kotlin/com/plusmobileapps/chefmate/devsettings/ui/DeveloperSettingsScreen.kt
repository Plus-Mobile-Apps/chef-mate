package com.plusmobileapps.chefmate.devsettings.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import chefmate.client.developer_settings.public.generated.resources.Res
import chefmate.client.developer_settings.public.generated.resources.dev_coach_marks_reset_message
import chefmate.client.developer_settings.public.generated.resources.dev_coach_marks_reset_title
import chefmate.client.developer_settings.public.generated.resources.dev_env_fake
import chefmate.client.developer_settings.public.generated.resources.dev_env_prod
import chefmate.client.developer_settings.public.generated.resources.dev_env_testing
import chefmate.client.developer_settings.public.generated.resources.dev_environment
import chefmate.client.developer_settings.public.generated.resources.dev_feature_flags
import chefmate.client.developer_settings.public.generated.resources.dev_login_as_user
import chefmate.client.developer_settings.public.generated.resources.dev_no_test_users
import chefmate.client.developer_settings.public.generated.resources.dev_no_test_users_message
import chefmate.client.developer_settings.public.generated.resources.dev_picker_dismiss
import chefmate.client.developer_settings.public.generated.resources.dev_premium_subscription
import chefmate.client.developer_settings.public.generated.resources.dev_premium_subscription_subtitle
import chefmate.client.developer_settings.public.generated.resources.dev_reset_coach_marks
import chefmate.client.developer_settings.public.generated.resources.dev_restart_required_message
import chefmate.client.developer_settings.public.generated.resources.dev_restart_required_title
import chefmate.client.developer_settings.public.generated.resources.dev_sign_in_error_title
import chefmate.client.developer_settings.public.generated.resources.dev_sign_in_error_unknown
import chefmate.client.developer_settings.public.generated.resources.dev_sign_out_test_user
import chefmate.client.developer_settings.public.generated.resources.dev_signed_out
import chefmate.client.developer_settings.public.generated.resources.dev_user_label_format
import chefmate.client.developer_settings.public.generated.resources.developer_settings
import com.plusmobileapps.chefmate.Environment
import com.plusmobileapps.chefmate.devsettings.DeveloperSettingsBloc
import com.plusmobileapps.chefmate.devsettings.TestUser
import com.plusmobileapps.chefmate.text.FixedString
import com.plusmobileapps.chefmate.text.PhraseModel
import com.plusmobileapps.chefmate.text.TextData
import com.plusmobileapps.chefmate.text.asTextData
import com.plusmobileapps.chefmate.ui.components.PlusDialog
import com.plusmobileapps.chefmate.ui.components.PlusHeaderContainer
import com.plusmobileapps.chefmate.ui.components.PlusHeaderData
import com.plusmobileapps.chefmate.ui.theme.ChefMateTheme
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.MutableStateFlow

@Composable
fun DeveloperSettingsScreen(bloc: DeveloperSettingsBloc, modifier: Modifier = Modifier) {
    val state by bloc.state.collectAsState()

    if (state.showEnvironmentPicker) {
        EnvironmentPickerDialog(
            current = state.currentEnvironment,
            onSelected = bloc::onEnvironmentSelected,
            onDismiss = bloc::onEnvironmentPickerDismissed,
        )
    }

    if (state.showUserPicker) {
        UserPickerDialog(
            users = state.availableUsers,
            selectedIndex = state.selectedUserIndex,
            onSelected = bloc::onUserSelected,
            onDismiss = bloc::onUserPickerDismissed,
        )
    }

    if (state.showRestartPrompt) {
        PlusDialog(
            title = Res.string.dev_restart_required_title.asTextData(),
            message = Res.string.dev_restart_required_message.asTextData(),
            onConfirmClick = bloc::onRestartPromptDismissed,
            onDismissRequest = bloc::onRestartPromptDismissed,
        )
    }

    if (state.showCoachMarksResetConfirmation) {
        PlusDialog(
            title = Res.string.dev_coach_marks_reset_title.asTextData(),
            message = Res.string.dev_coach_marks_reset_message.asTextData(),
            onConfirmClick = bloc::onCoachMarksResetConfirmationDismissed,
            onDismissRequest = bloc::onCoachMarksResetConfirmationDismissed,
        )
    }

    state.signInError?.let { rawMessage ->
        val message: TextData =
            if (rawMessage.isBlank()) Res.string.dev_sign_in_error_unknown.asTextData()
            else FixedString(rawMessage)
        PlusDialog(
            title = Res.string.dev_sign_in_error_title.asTextData(),
            message = message,
            onConfirmClick = bloc::onSignInErrorDismissed,
            onDismissRequest = bloc::onSignInErrorDismissed,
        )
    }

    Surface(modifier = modifier.fillMaxSize(), color = ChefMateTheme.colorScheme.background) {
        PlusHeaderContainer(
            data =
                PlusHeaderData.Child(
                    title = Res.string.developer_settings.asTextData(),
                    onBackClick = bloc::onBack,
                ),
            content = {
                DeveloperRow(
                    title = Res.string.dev_environment.asTextData(),
                    trailing = state.currentEnvironment.label(),
                    onClick = bloc::onEnvironmentClicked,
                )
                HorizontalDivider()
                DeveloperRow(
                    title = Res.string.dev_login_as_user.asTextData(),
                    trailing =
                        state.currentUserEmail?.let { FixedString(it) }
                            ?: Res.string.dev_signed_out.asTextData(),
                    onClick = bloc::onLoginAsUserClicked,
                )
                if (state.isAuthenticated) {
                    HorizontalDivider()
                    DeveloperRow(
                        title = Res.string.dev_sign_out_test_user.asTextData(),
                        onClick = bloc::onSignOutTestUserClicked,
                    )
                }
                HorizontalDivider()
                DeveloperRow(
                    title = Res.string.dev_feature_flags.asTextData(),
                    onClick = bloc::onFeatureFlagsClicked,
                )
                HorizontalDivider()
                DeveloperSwitchRow(
                    title = Res.string.dev_premium_subscription.asTextData(),
                    subtitle = Res.string.dev_premium_subscription_subtitle.asTextData(),
                    checked = state.isSubscribed,
                    onCheckedChange = bloc::onSubscribedToggled,
                )
                HorizontalDivider()
                DeveloperRow(
                    title = Res.string.dev_reset_coach_marks.asTextData(),
                    onClick = bloc::onClearCoachMarksClicked,
                )
            },
        )
    }
}

@Composable
private fun DeveloperRow(
    title: TextData,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: TextData? = null,
) {
    val description = title.localized()
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .height(ChefMateTheme.dimens.rowHeight)
                .clickable { onClick() }
                .padding(horizontal = ChefMateTheme.dimens.paddingNormal)
                .semantics { contentDescription = description },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title.localized(), style = ChefMateTheme.typography.titleMedium)
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (trailing != null) {
                Text(
                    trailing.localized(),
                    style = ChefMateTheme.typography.bodyMedium,
                    color = ChefMateTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null)
        }
    }
}

/**
 * Row whose control is a switch rather than a drill-in. The whole row toggles, so the target isn't
 * limited to the switch itself.
 */
@Composable
private fun DeveloperSwitchRow(
    title: TextData,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: TextData? = null,
) {
    val description = title.localized()
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
                .padding(
                    horizontal = ChefMateTheme.dimens.paddingNormal,
                    vertical = ChefMateTheme.dimens.paddingSmall,
                )
                .semantics { contentDescription = description },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title.localized(), style = ChefMateTheme.typography.titleMedium)
            if (subtitle != null) {
                Text(
                    subtitle.localized(),
                    style = ChefMateTheme.typography.bodySmall,
                    color = ChefMateTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Switch(checked = checked, onCheckedChange = null)
    }
}

@Composable
private fun EnvironmentPickerDialog(
    current: Environment,
    onSelected: (Environment) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(Res.string.dev_environment.asTextData().localized()) },
        text = {
            Column {
                Environment.entries.forEach { env ->
                    Row(
                        modifier =
                            Modifier.fillMaxWidth()
                                .selectable(
                                    selected = env == current,
                                    onClick = { onSelected(env) },
                                )
                                .padding(vertical = ChefMateTheme.dimens.paddingSmall),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = env == current, onClick = { onSelected(env) })
                        Text(
                            env.label().localized(),
                            style = ChefMateTheme.typography.bodyLarge,
                            modifier = Modifier.padding(start = ChefMateTheme.dimens.paddingSmall),
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(Res.string.dev_picker_dismiss.asTextData().localized())
            }
        },
    )
}

@Composable
private fun UserPickerDialog(
    users: List<TestUser>,
    selectedIndex: Int?,
    onSelected: (TestUser) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(Res.string.dev_login_as_user.asTextData().localized()) },
        text = {
            if (users.isEmpty()) {
                Column {
                    Text(
                        Res.string.dev_no_test_users.asTextData().localized(),
                        style = ChefMateTheme.typography.titleMedium,
                    )
                    Text(
                        Res.string.dev_no_test_users_message.asTextData().localized(),
                        style = ChefMateTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = ChefMateTheme.dimens.paddingSmall),
                    )
                }
            } else {
                Column {
                    users.forEach { user ->
                        Row(
                            modifier =
                                Modifier.fillMaxWidth()
                                    .selectable(
                                        selected = user.index == selectedIndex,
                                        onClick = { onSelected(user) },
                                    )
                                    .padding(vertical = ChefMateTheme.dimens.paddingSmall),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = user.index == selectedIndex,
                                onClick = { onSelected(user) },
                            )
                            Text(
                                user.label().localized(),
                                style = ChefMateTheme.typography.bodyLarge,
                                modifier =
                                    Modifier.padding(start = ChefMateTheme.dimens.paddingSmall),
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(Res.string.dev_picker_dismiss.asTextData().localized())
            }
        },
    )
}

private fun Environment.label(): TextData =
    when (this) {
        Environment.PROD -> Res.string.dev_env_prod.asTextData()
        Environment.TESTING -> Res.string.dev_env_testing.asTextData()
        Environment.FAKE -> Res.string.dev_env_fake.asTextData()
    }

private fun TestUser.label(): TextData =
    PhraseModel(
        resource = Res.string.dev_user_label_format,
        "index" to FixedString(index.toString()),
        "email" to FixedString(email),
    )

val previewDeveloperSettingsBloc =
    object : DeveloperSettingsBloc {
        override val state =
            MutableStateFlow(
                DeveloperSettingsBloc.Model(
                    currentEnvironment = Environment.PROD,
                    availableUsers =
                        persistentListOf(
                            TestUser(1, "alice@chefmate.test", "password1"),
                            TestUser(2, "bob@chefmate.test", "password2"),
                        ),
                    selectedUserIndex = 1,
                    currentUserEmail = "alice@chefmate.test",
                    isAuthenticated = true,
                )
            )

        override fun onBack() = Unit

        override fun onEnvironmentClicked() = Unit

        override fun onEnvironmentSelected(environment: Environment) = Unit

        override fun onEnvironmentPickerDismissed() = Unit

        override fun onLoginAsUserClicked() = Unit

        override fun onUserSelected(user: TestUser) = Unit

        override fun onUserPickerDismissed() = Unit

        override fun onSignOutTestUserClicked() = Unit

        override fun onRestartPromptDismissed() = Unit

        override fun onSignInErrorDismissed() = Unit

        override fun onFeatureFlagsClicked() = Unit

        override fun onSubscribedToggled(subscribed: Boolean) = Unit

        override fun onClearCoachMarksClicked() = Unit

        override fun onCoachMarksResetConfirmationDismissed() = Unit
    }
