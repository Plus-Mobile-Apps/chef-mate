package com.plusmobileapps.chefmate.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** Top-bar info action that opens [FlagHelpDialog]. */
@Composable
fun FlagHelpAction() {
    var open by remember { mutableStateOf(false) }
    AdminIconButton(Icons.Outlined.Info, "How flags work", onClick = { open = true })
    if (open) FlagHelpDialog(onDismiss = { open = false })
}

/**
 * Explains what each flag field does and how the client evaluates a flag. The evaluation order
 * mirrors `FeatureFlagEvaluator` in `:client:featureflag:impl`; keep the two in sync.
 */
@Composable
fun FlagHelpDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Outlined.Info, contentDescription = null) },
        title = { Text("How feature flags work") },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 520.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(AdminDimens.paddingNormal),
            ) {
                HelpHeading("How the app decides")
                Text(
                    "The app checks these in order. The first one that fails gives the user " +
                        "the in-code default instead of this flag's value.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Column(verticalArrangement = Arrangement.spacedBy(AdminDimens.paddingSmall)) {
                    Step(1, "Enabled is on.")
                    Step(2, "The device's platform is selected, or no platform is.")
                    Step(3, "The app version is within the min / max bounds, if set.")
                    Step(4, "The user is on the allowlist, or falls inside the rollout %.")
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                HelpHeading("Fields")
                Field(
                    "Key",
                    "The name the app looks the flag up by, e.g. cook_mode_v2. Lower-case " +
                        "snake_case. It can't be changed after the flag is created.",
                )
                Field(
                    "Value",
                    "What users get when the flag applies. Boolean flags are true / false; " +
                        "string flags hold any text (e.g. a variant name).",
                )
                Field(
                    "Enabled",
                    "Global kill switch. Off means everyone gets the in-code default, " +
                        "regardless of anything else.",
                )
                Field(
                    "Rollout %",
                    "Share of users who get the value. Each user lands in a stable bucket per " +
                        "flag, so raising the % only adds users, it never reshuffles them.",
                )
                Field(
                    "Platforms",
                    "Restricts the flag to the selected platforms; none selected means all. " +
                        "The app currently reports android, ios, or jvm (desktop), so " +
                        "selecting desktop or web matches no client.",
                )
                Field(
                    "Min / max version",
                    "Optional app version bounds (e.g. 2.4.0). Users outside the range get " +
                        "the default.",
                )
                Field(
                    "User allowlist",
                    "Supabase user ids that get the flag on top of the rollout %. Only works " +
                        "for signed-in users, and still respects Enabled, Platforms and " +
                        "versions.",
                )
                Field("Description", "Notes for admins. The app never reads it.")

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                HelpHeading("Tips")
                Field(
                    "One user only",
                    "Enabled on, Rollout 0%, and that user's id in the allowlist.",
                )
                Field(
                    "Archive vs delete",
                    "Archive hides the flag from the app but keeps the row, so it can be " +
                        "restored. Delete removes it permanently.",
                )
            }
        },
        confirmButton = { AdminButton(text = "Got it", onClick = onDismiss) },
    )
}

@Composable
private fun HelpHeading(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
    )
}

@Composable
private fun Step(number: Int, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(24.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "$number",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimary,
            )
        }
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(start = AdminDimens.paddingSmall + 4.dp),
        )
    }
}

@Composable
private fun Field(name: String, description: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            name,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
