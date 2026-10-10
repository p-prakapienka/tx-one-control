package dev.tonexotg.app.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.tonexotg.app.ui.components.RigLed
import dev.tonexotg.app.ui.theme.RigAmber
import dev.tonexotg.app.ui.theme.RigChassis
import dev.tonexotg.app.ui.theme.RigPanelEdge
import dev.tonexotg.app.ui.theme.TonexOnSurfaceTertiary
import dev.tonexotg.app.ui.theme.TonexTheme

/**
 * The app's top-level destinations, shown as tabs along the bottom of the screen. Anything not
 * listed here (the two editors) is a pushed screen with its own back button, and the tab bar is
 * hidden while it is showing.
 */
internal enum class TopLevelTab(val route: TonexRoute, val label: String) {
    RIG(TonexRoute.STAGE, "Rig"),
    PRESETS(TonexRoute.PRESET_LIST, "Presets"),
    SETTINGS(TonexRoute.SETTINGS, "Settings"),
    ABOUT(TonexRoute.ABOUT, "About"),
    ;

    companion object {
        fun forRoute(route: String?): TopLevelTab? = entries.firstOrNull { it.route.route == route }
    }
}

/** Bottom tab bar in the rig style: the selected tab gets a lit LED and amber label. */
@Composable
internal fun RigTabBar(
    selected: TopLevelTab,
    onSelect: (TopLevelTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(RigChassis)
            .windowInsetsPadding(WindowInsets.navigationBars),
    ) {
        HorizontalDivider(color = RigPanelEdge)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .selectableGroup(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            TopLevelTab.entries.forEach { tab ->
                val isSelected = tab == selected
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 56.dp)
                        .selectable(selected = isSelected, role = Role.Tab, onClick = { onSelect(tab) })
                        .testTag("tab.${tab.name.lowercase()}"),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        RigLed(lit = isSelected, size = 6.dp)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = tab.label.uppercase(),
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                            ),
                            color = if (isSelected) RigAmber else TonexOnSurfaceTertiary,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}

@Preview(name = "Rig tab bar", backgroundColor = 0xFF151517, showBackground = true)
@Composable
private fun RigTabBarPreview() {
    TonexTheme {
        RigTabBar(selected = TopLevelTab.RIG, onSelect = {})
    }
}
