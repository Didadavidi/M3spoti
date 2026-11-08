package com.music.spotui.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import app.myspoti.engine.R

/**
 * The one typeface: Roboto Flex (SIL Open Font License 1.1, licenses/RobotoFlex-OFL.txt), one
 * variable file with Latin, Cyrillic and Greek (issue #62: every script in one face). The design
 * leans on its width axis: headings are set wide and black, body text at normal width.
 *
 * Compose matches a FontFamily on weight and style only, so each width is its own family, every
 * weight of it a fixed point on the "wght"/"wdth"/"opsz" axes of the same file.
 */
@OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)
private fun flex(weight: Int, width: Float, opticalSize: Float) = Font(
    R.font.roboto_flex,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(
        FontVariation.weight(weight),
        FontVariation.width(width),
        FontVariation.Setting("opsz", opticalSize),
    ),
)

private val weights = listOf(400, 500, 600, 700, 800, 900)
private fun family(width: Float, opticalSize: Float) = FontFamily(weights.map { flex(it, width, opticalSize) })

private val ownSans = family(width = 100f, opticalSize = 14f)

/** Body text, labels, buttons: normal width; the phone's own font when chosen in the settings (AppFontPref). */
val AppSans: FontFamily get() = if (com.music.spotui.data.preferences.AppFontPref.system) FontFamily.Default else ownSans
/** Card and row titles ("Night Ferry", track names): a little wide. */
val FlexTitle = family(width = 125f, opticalSize = 24f)
/** Screen titles and section headers ("Albums", "Keep listening"): wide. */
val FlexDisplay = family(width = 140f, opticalSize = 48f)
/** The biggest words ("Your Mix", the player's title): the widest cut. */
val FlexWide = family(width = 151f, opticalSize = 96f)
/** Big numbers (the player's elapsed time): tall and narrow. */
val FlexCondensed = family(width = 25f, opticalSize = 96f)

// Kept as aliases so existing references don't break.
val AppSansTitle = FlexDisplay
val Montserrat: FontFamily get() = AppSans

/** The design's type scale in [sans] for body text and in the wide cuts for titles (all [sans] with the system font). */
private fun designTypography(sans: FontFamily, title: FontFamily, display: FontFamily, wide: FontFamily): Typography {
    val base = TextStyle(fontFamily = sans)
    return Typography(
        displayLarge = TextStyle(fontFamily = wide, fontWeight = FontWeight.Black, fontSize = 68.sp, lineHeight = 58.sp, letterSpacing = (-1.5).sp),
        displayMedium = TextStyle(fontFamily = display, fontWeight = FontWeight.Black, fontSize = 48.sp, lineHeight = 50.sp, letterSpacing = (-1).sp),
        displaySmall = TextStyle(fontFamily = wide, fontWeight = FontWeight.Black, fontSize = 36.sp, lineHeight = 38.sp, letterSpacing = (-0.5).sp),
        headlineLarge = TextStyle(fontFamily = display, fontWeight = FontWeight.Black, fontSize = 32.sp, lineHeight = 36.sp),
        headlineMedium = TextStyle(fontFamily = display, fontWeight = FontWeight.Black, fontSize = 28.sp, lineHeight = 32.sp),
        headlineSmall = TextStyle(fontFamily = display, fontWeight = FontWeight.Black, fontSize = 22.sp, lineHeight = 28.sp),
        titleLarge = TextStyle(fontFamily = title, fontWeight = FontWeight.Black, fontSize = 20.sp, lineHeight = 24.sp),
        titleMedium = TextStyle(fontFamily = title, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp, lineHeight = 22.sp),
        titleSmall = TextStyle(fontFamily = sans, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, lineHeight = 20.sp),
        bodyLarge = base.copy(fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.Medium),
        bodyMedium = base.copy(fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium),
        bodySmall = base.copy(fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.SemiBold),
        labelLarge = base.copy(fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.Bold),
        labelMedium = base.copy(fontSize = 13.sp, lineHeight = 16.sp, fontWeight = FontWeight.SemiBold),
        labelSmall = base.copy(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.SemiBold),
    )
}

private val ownTypography by lazy { designTypography(ownSans, FlexTitle, FlexDisplay, FlexWide) }
private val systemTypography by lazy { FontFamily.Default.let { designTypography(it, it, it, it) } }

val Typography: Typography get() = if (com.music.spotui.data.preferences.AppFontPref.system) systemTypography else ownTypography
