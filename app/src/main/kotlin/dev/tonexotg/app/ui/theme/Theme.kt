package dev.tonexotg.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * The app's M3 [androidx.compose.material3.ColorScheme], wired from the rig palette so every
 * screen (Presets, Settings, About, the classic editor and the connection bar) matches the rig
 * screens: charcoal chassis, panel greys and an amber accent. Upstream Tonex-OTG wired this from
 * its D1 tokens (blue accent on neutral grey). Those tokens still live in `Color.kt` and the
 * status colors below still come from them, because status colors carry meaning.
 *
 * Dark only, no light scheme or "follow system" branch (D1 §6). Every role a stock component
 * might read is set explicitly, including secondary/tertiary/containers, so no Material baseline
 * purple leaks through.
 *
 * `onSurfaceVariant` is wired to [TonexOnSurfaceSecondary]; the tertiary/muted and disabled
 * tones have no stock M3 slot and live on [TonexExtendedColors] with the status colors.
 */
private val TonexColorScheme = darkColorScheme(
    primary = RigAmber,
    onPrimary = TonexOnAccent,
    primaryContainer = RigPanelHighest,
    onPrimaryContainer = RigAmber,
    secondary = RigAmber,
    onSecondary = TonexOnAccent,
    secondaryContainer = RigPanelHighest,
    onSecondaryContainer = RigAmber,
    tertiary = RigLcdText,
    onTertiary = RigLcd,
    tertiaryContainer = RigLcd,
    onTertiaryContainer = RigLcdText,
    surface = RigChassis,
    onSurface = TonexOnSurfacePrimary,
    surfaceVariant = RigPanel,
    onSurfaceVariant = TonexOnSurfaceSecondary,
    surfaceTint = RigPanelEdge,
    surfaceContainerLowest = RigChassis,
    surfaceContainerLow = RigPanel,
    surfaceContainer = RigPanel,
    surfaceContainerHigh = RigPanelHigh,
    surfaceContainerHighest = RigPanelHighest,
    background = RigChassis,
    onBackground = TonexOnSurfacePrimary,
    outline = RigPanelEdge,
    outlineVariant = RigPanelEdge,
    error = TonexStatusError,
    onError = TonexOnStatus,
)

/**
 * D1 tokens with no stock M3 [androidx.compose.material3.ColorScheme] role: the tertiary/muted
 * and disabled on-surface tones (§2.1), and the four connection-status colors plus their
 * on-status content color (§2.3). Wired as a custom [androidx.compose.runtime.CompositionLocal]
 * extension, per D1 §7's explicit instruction, not by repurposing `tertiary`/`secondary` roles
 * that mean something else in M3.
 */
data class TonexExtendedColors(
    val onSurfaceTertiary: Color,
    val onSurfaceDisabled: Color,
    val statusConnected: Color,
    val statusConnecting: Color,
    val statusDisconnected: Color,
    val statusError: Color,
    val onStatus: Color,
)

private val TonexExtendedColorsDefault = TonexExtendedColors(
    onSurfaceTertiary = TonexOnSurfaceTertiary,
    onSurfaceDisabled = TonexOnSurfaceDisabled,
    statusConnected = TonexStatusConnected,
    statusConnecting = TonexStatusConnecting,
    statusDisconnected = TonexStatusDisconnected,
    statusError = TonexStatusError,
    onStatus = TonexOnStatus,
)

private val LocalTonexExtendedColors = staticCompositionLocalOf<TonexExtendedColors> {
    error("TonexExtendedColors requested outside of TonexTheme { }")
}

/**
 * The app's single Material 3 theme. Dark-first by construction (D1 §1, §6): there is no
 * parameter to select a light scheme or follow the system setting, because D1 defines none.
 *
 * Wrap the app's content (or any `@Preview` composable exercising a themed component) in this
 * once, at the root:
 * ```
 * TonexTheme {
 *     // content reads colors via MaterialTheme.colorScheme / TonexTheme.extendedColors,
 *     // type via MaterialTheme.typography, spacing via TonexTheme.spacing, etc.
 * }
 * ```
 */
@Composable
fun TonexTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalTonexExtendedColors provides TonexExtendedColorsDefault) {
        MaterialTheme(
            colorScheme = TonexColorScheme,
            typography = TonexTypography,
            content = content,
        )
    }
}

/**
 * Theme accessors, mirroring the `MaterialTheme.colorScheme` / `MaterialTheme.typography`
 * pattern for the tokens D1 defines that M3's own [MaterialTheme] object has no slot for:
 * the extended status/tertiary/disabled colors, the spacing scale, and touch-target sizes.
 */
object TonexTheme {
    val extendedColors: TonexExtendedColors
        @Composable get() = LocalTonexExtendedColors.current

    val spacing: TonexSpacing
        get() = TonexSpacing

    val touchTargets: TonexTouchTargets
        get() = TonexTouchTargets
}
