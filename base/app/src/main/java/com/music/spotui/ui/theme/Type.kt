package com.music.spotui.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import app.myspoti.engine.R

/**
 * UI typeface: Manrope (SIL Open Font License 1.1, licenses/Manrope-OFL.txt), a free
 * geometric sans close to the official app's look. One variable font file, each weight
 * picked through its "wght" axis (200 to 800: the heaviest cut asks for 900 and gets 800).
 * It has Latin, Cyrillic and Greek glyphs. The Figtree it replaced had no Cyrillic, so Android
 * drew those letters with the system font and a Russian or Ukrainian interface jumped between
 * two typefaces (issue #62).
 */
@OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)
private fun sans(weight: Int) = Font(
    R.font.app_sans,
    weight = FontWeight(weight),
    variationSettings = androidx.compose.ui.text.font.FontVariation.Settings(
        androidx.compose.ui.text.font.FontVariation.weight(weight),
    ),
)

val AppSans = FontFamily(sans(400), sans(500), sans(600), sans(700), sans(800), sans(900))

// Kept as an alias so existing references (Theme.kt etc.) don't break.
val Montserrat = AppSans

/**
 * Display/headline/title typeface: Google Sans Flex (OFL-1.1, licenses/GoogleSansFlex-OFL.txt).
 * Has no Cyrillic or Greek glyphs (issue #62), so it is used for titles only — body text stays
 * on Manrope ([AppSans]), which covers Myspoti's Russian and Greek strings.
 */
@OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)
private fun flex(weight: Int) = Font(
    R.font.flex,
    weight = FontWeight(weight),
    variationSettings = androidx.compose.ui.text.font.FontVariation.Settings(
        androidx.compose.ui.text.font.FontVariation.weight(weight),
    ),
)

val AppSansTitle = FontFamily(flex(400), flex(500), flex(600), flex(700), flex(800))

private val base = TextStyle(fontFamily = AppSans)
private val title = TextStyle(fontFamily = AppSansTitle, fontWeight = FontWeight.Bold)

val Typography = Typography(
    displayLarge = title, displayMedium = title, displaySmall = title,
    headlineLarge = title, headlineMedium = title, headlineSmall = title,
    titleLarge = title, titleMedium = title, titleSmall = title,
    bodyLarge = base.copy(fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = base, bodySmall = base,
    labelLarge = base, labelMedium = base, labelSmall = base,
)
