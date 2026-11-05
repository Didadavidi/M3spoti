package com.music.spotui.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

/** The system colour scheme in effect (wallpaper colours, light or dark): see [SystemColors]. */
private val scheme get() = SystemColors.scheme

val AppBackground: Color get() =
    if (app.myspoti.power.EnergySettings.oledBlack) Color.Black else scheme.surface
val GridBackground: Color get() = tone(0xFF2A2A2A)
/** The accent: the playing cover's primary tone, or the fallback accent while nothing plays. */
val AppPalette: Color get() = AppAccent
val AppAccent: Color get() = scheme.primary

/** Primary text and icons. */
val AppText: Color get() = scheme.onSurface
/** Secondary text and icons. */
val AppMuted: Color get() = scheme.onSurfaceVariant
/** Destructive actions (delete, leave, block) and failures shown as text. */
val AppDestructive = Color(0xFFF15E6C)
/** What a tint is mixed with to make a header. */
val AppShade: Color get() = scheme.surfaceContainerLowest

/**
 * One of the palette's raised greys (#181818 ... #3E3E3E): "a bit raised from the page". Each of
 * the Classic screens' hardcoded tones maps to the nearest step of the dynamic scheme, by how
 * light the literal grey was, so the whole page re-skins together as the cover's palette changes.
 */
fun tone(argb: Long): Color {
    val grey = (argb shr 16) and 0xFF
    return when {
        grey <= 0x1C -> scheme.surface
        grey <= 0x22 -> scheme.surfaceContainerLow
        grey <= 0x27 -> scheme.surfaceContainer
        grey <= 0x2F -> scheme.surfaceContainerHigh
        grey <= 0x3C -> scheme.surfaceContainerHighest
        else -> scheme.outlineVariant
    }
}

/**
 * The cover's colour as a page header, so the title on it stays readable in the theme's text
 * colour: darkened toward black on the dark theme, a pastel of it on the light one.
 */
fun headerTint(tint: Color, amount: Float): Color =
    if (SystemColors.dark) lerp(tint, Color.Black, amount) else lerp(tint, scheme.surface, 0.72f + amount * 0.2f)

/** Text and icons on the accent (buttons, selected chips, badges): readable in light and dark. */
val AppOnAccent: Color get() = scheme.onPrimary
