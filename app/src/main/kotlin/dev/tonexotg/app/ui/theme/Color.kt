package dev.tonexotg.app.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Every color token from `docs/design/ui-design-tokens.md` (D1 §2), transcribed verbatim and
 * exactly once. Nothing outside this file may declare a `Color(0x...)` literal for a token
 * defined here — reference these vals (directly, or indirectly via [TonexColorScheme] /
 * [TonexExtendedColors]) instead. This is a translation of D1, not a new design decision: if a
 * value here doesn't match D1 §2's table, that's a bug in this file.
 */

// ---- 2.1 Surfaces & content (dark theme — the only theme in v1) ----

/** `color.surface.base` — app background, the resting state of every screen. */
val TonexSurfaceBase = Color(0xFF121212)

/** `color.surface.raised-1` — preset list rows, top app bar, bottom bar. */
val TonexSurfaceRaised1 = Color(0xFF1D1D1D)

/** `color.surface.raised-2` — dialogs, bottom sheets, parameter-edit panels. */
val TonexSurfaceRaised2 = Color(0xFF262626)

/** `color.surface.raised-3` — pressed/active fill on a raised-1 element; highest overlay. */
val TonexSurfaceRaised3 = Color(0xFF303030)

/** `color.on-surface.primary` — primary text/icons: active preset name, key numerals. */
val TonexOnSurfacePrimary = Color(0xFFFFFFFF)

/** `color.on-surface.secondary` — body text, control labels. */
val TonexOnSurfaceSecondary = Color(0xFFE0E0E0)

/** `color.on-surface.tertiary` — captions, secondary metadata. */
val TonexOnSurfaceTertiary = Color(0xFF9E9E9E)

/** `color.on-surface.disabled` — disabled control text/icons only. */
val TonexOnSurfaceDisabled = Color(0xFF6B6B6B)

// ---- 2.2 Accent (interactive / selection) ----

/**
 * `color.accent.primary` — selection ring on the active preset tile, slider fill/thumb, primary
 * button fill, focus indicator.
 */
val TonexAccentPrimary = Color(0xFF52B8FF)

/** `color.on-accent` — text/icon glyphs drawn on top of an accent-primary fill. */
val TonexOnAccent = Color(0xFF121212)

// ---- 2.3 Connection status — color + shape + icon + text, never color alone ----
// See ui/components/ConnectionStatusIndicator.kt for the shape/text half of this requirement;
// color alone never carries the meaning at any call site.

/** `color.status.connected` — filled circle (●) + "Connected". */
val TonexStatusConnected = Color(0xFF34D177)

/**
 * `color.status.connecting` — hollow ring (same silhouette as disconnected) + "Connecting…".
 * D1 defines this token's value as literally equal to accent-primary, not a coincidentally
 * similar color — reusing [TonexAccentPrimary] here keeps that a single definition.
 */
val TonexStatusConnecting = TonexAccentPrimary

/** `color.status.disconnected` — hollow/outline ring (○) + "Not Connected". */
val TonexStatusDisconnected = Color(0xFF9AA0A6)

/** `color.status.error` — filled triangle with glyph (▲!) + "Error". */
val TonexStatusError = Color(0xFFFF5252)

/**
 * `color.on-status` — text/icon drawn on any status fill. D1 fixes this to the same dark value
 * as [TonexOnAccent] deliberately (white fails contrast against the light-toned status/accent
 * fills — D1 §2.4) — reusing that val keeps the "why" visible instead of repeating the literal.
 */
val TonexOnStatus = TonexOnAccent

// ---- Rig skin (the "hardware" look used by ui/screens/rig) ----
// An original dark-hardware palette: charcoal chassis, brushed-steel knobs, amber LEDs and a
// green-on-black LCD. Kept here with every other color literal, per this file's own rule.

/** Chassis background behind the rig screens — a touch warmer than [TonexSurfaceBase]. */
val RigChassis = Color(0xFF151517)

/** Raised panel (block editor, preset tiles). */
val RigPanel = Color(0xFF1F2023)

/** Panel edge / divider hairline. */
val RigPanelEdge = Color(0xFF34363B)

/** Knob body, light and dark ends of its radial gradient. */
val RigKnobLight = Color(0xFF5A5D63)
val RigKnobDark = Color(0xFF1C1D20)

/** Unlit knob track / unlit LED. */
val RigTrack = Color(0xFF2C2E33)

/** Amber accent: value arcs, lit LEDs, active selection. */
val RigAmber = Color(0xFFFFB02E)

/** Dim amber for glows and secondary highlights. */
val RigAmberDim = Color(0x55FFB02E)

/** LCD background and its phosphor-green text. */
val RigLcd = Color(0xFF0B120D)
val RigLcdText = Color(0xFF8CFFB0)
val RigLcdTextDim = Color(0xFF3E7A52)

/** Footswitch slot badge colors (A / B / C). */
val RigSlotA = Color(0xFFFF6B5A)
val RigSlotB = Color(0xFF5AB4FF)
val RigSlotC = Color(0xFFB98CFF)
