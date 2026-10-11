package dev.tonexotg.app.ui.screens.settings

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import dev.tonexotg.app.probe.ProbeActivity
import dev.tonexotg.app.ui.components.ParameterControl
import dev.tonexotg.app.ui.components.ParameterSwitch
import dev.tonexotg.app.ui.screens.about.loadAppVersionInfo
import dev.tonexotg.app.ui.screens.presets.GlobalParametersUiState
import dev.tonexotg.app.ui.theme.TonexTheme
import dev.tonexotg.protocol.ParameterId

/**
 * Issue #96: the app's Settings screen. Its one real job today is to be the in-app entry point
 * for [ProbeActivity] (the S20 hardware probe / diagnostics, issue #25), which used to have its
 * own separate launcher icon -- see that Activity's own KDoc and the manifest for why it no
 * longer does. Launching it via an explicit [Intent] here (rather than folding its Compose UI
 * into this NavHost) keeps that Activity's own lifecycle/CoroutineScope contract (see its KDoc,
 * "Deliberately NOT scope.cancel()...") unchanged.
 *
 * Issue #126: also the discoverable "Report a bug" entry point for regular users, as its own
 * "Support" section above "Diagnostics" -- the debug menu's own GitHub-issue shortcut
 * (`buildDebugDumpIssueUri`, issue #96) stays diagnostic-only per issue #126's "out of scope".
 */
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    globalParameters: GlobalParametersUiState? = null,
    onGlobalRangeChange: (ParameterId, Float) -> Unit = { _, _ -> },
    onGlobalRangeChangeFinished: (ParameterId) -> Unit = {},
    onGlobalSwitchToggle: (ParameterId, Boolean) -> Unit = { _, _ -> },
    onAboutClick: (() -> Unit)? = null,
) {
    val context = LocalContext.current

    Surface(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(TonexTheme.spacing.space3),
            verticalArrangement = Arrangement.spacedBy(TonexTheme.spacing.space3),
        ) {
            // The pedal's device-wide settings, moved here from the old Presets screen's tray.
            // Same rows, same view-model write path (PresetListViewModel's global throttler).
            Text(text = "Pedal", style = MaterialTheme.typography.titleLarge)
            if (globalParameters == null) {
                Text(
                    text = "Connect the pedal to change its global settings.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                listOf(globalParameters.bpm, globalParameters.inputTrim, globalParameters.tuningReference).forEach { row ->
                    ParameterControl(
                        label = row.label,
                        value = row.value,
                        valueRange = row.range,
                        valueText = row.valueText,
                        onValueChange = { onGlobalRangeChange(row.id, it) },
                        onValueChangeFinished = { onGlobalRangeChangeFinished(row.id) },
                        abbreviation = row.abbreviation,
                        modifier = Modifier.testTag("settings.global.range.${row.id.index}"),
                    )
                }
                listOf(globalParameters.cabSimBypass, globalParameters.tempoSource, globalParameters.bypass).forEach { row ->
                    ParameterSwitch(
                        label = row.label,
                        checked = row.checked,
                        onCheckedChange = { onGlobalSwitchToggle(row.id, it) },
                        abbreviation = row.abbreviation,
                        modifier = Modifier.testTag("settings.global.switch.${row.id.index}"),
                    )
                }
            }

            HorizontalDivider()

            Text(text = "Support", style = MaterialTheme.typography.titleLarge)
            Text(
                text = "Found a problem? Report it and we'll take a look.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedButton(
                onClick = {
                    val appVersionInfo = loadAppVersionInfo(context)
                    val uri = buildBugReportIssueUri(appVersionInfo)
                    context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Report a bug")
            }

            HorizontalDivider()

            Text(text = "Diagnostics", style = MaterialTheme.typography.titleLarge)
            Text(
                text = "Diagnostic tools for troubleshooting a pedal connection. Not needed for " +
                    "normal use.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedButton(
                onClick = { context.startActivity(Intent(context, ProbeActivity::class.java)) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Open hardware probe / debug menu")
            }

            if (onAboutClick != null) {
                HorizontalDivider()
                OutlinedButton(
                    onClick = onAboutClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings.aboutButton"),
                ) {
                    Text("About, credits & licences")
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsScreenPreview() {
    TonexTheme {
        SettingsScreen()
    }
}
