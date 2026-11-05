package com.music.spotui.ui.theme

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.google.android.material.color.utilities.Hct
import com.google.android.material.color.utilities.SchemeVibrant

/** Light or dark: the phone's choice (the default), or always one of the two. */
enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** Settings, Colours: the theme mode and where the two colour sources apply. */
object ColorPrefs {
    private const val PREF = "settings_prefs"
    private const val KEY_MODE = "theme_mode"
    private const val KEY_ALBUM = "album_colours"
    private const val KEY_SYSTEM = "system_colours"

    private fun prefs(c: Context) = c.getSharedPreferences(PREF, Context.MODE_PRIVATE)
    private val app get() = com.music.spotui.di.Graph.app

    var mode by mutableStateOf(
        ThemeMode.entries.firstOrNull { it.name == prefs(app).getString(KEY_MODE, null) } ?: ThemeMode.SYSTEM
    )
        private set

    /** Now Playing, Queue and Lyrics follow the artwork (else they use the dark system colours). */
    var albumColours by mutableStateOf(prefs(app).getBoolean(KEY_ALBUM, true))
        private set

    /** Home, Library, Album pages and Settings use the wallpaper's colours (else the design's purple). */
    var systemColours by mutableStateOf(prefs(app).getBoolean(KEY_SYSTEM, true))
        private set

    fun setMode(c: Context, value: ThemeMode) { mode = value; prefs(c).edit().putString(KEY_MODE, value.name).apply() }
    fun setAlbumColours(c: Context, value: Boolean) { albumColours = value; prefs(c).edit().putBoolean(KEY_ALBUM, value).apply() }
    fun setSystemColours(c: Context, value: Boolean) { systemColours = value; prefs(c).edit().putBoolean(KEY_SYSTEM, value).apply() }
}

/**
 * The "system colours" of every screen but the player: Material You's wallpaper colour (Android 12
 * and later) as a seed, built into a vibrant tonal scheme, light or dark as [ColorPrefs.mode] says.
 * Color.kt's globals (AppText, AppBackground, tone()...) read [scheme], so screens drawn with them
 * follow light/dark and the wallpaper without each knowing.
 */
@SuppressLint("RestrictedApi")
object SystemColors {
    /** The design's own purple, when there is no wallpaper colour or the user turned it off. */
    val DesignSeed = Color(0xFF6B3FD4)

    var dark by mutableStateOf(true)
        private set

    var scheme: ColorScheme by mutableStateOf(build(DesignSeed.toArgb(), dark = true, oled = false))
        private set

    private var key: Triple<Int, Boolean, Boolean>? = null

    /** Rebuilds the scheme when the seed, light/dark or AMOLED black changed. Cheap otherwise. */
    fun update(context: Context, dark: Boolean) {
        val seed = seed(context)
        val oled = dark && app.myspoti.power.EnergySettings.oledBlack
        val k = Triple(seed, dark, oled)
        if (k == key) return
        key = k
        this.dark = dark
        scheme = build(seed, dark, oled)
    }

    private fun seed(context: Context): Int {
        if (ColorPrefs.systemColours && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // system_accent1_600: the wallpaper's own seed tone, as Android derives it.
            return runCatching { dynamicLightColorScheme(context).primary.toArgb() }.getOrDefault(DesignSeed.toArgb())
        }
        return DesignSeed.toArgb()
    }

    fun build(seedArgb: Int, dark: Boolean, oled: Boolean): ColorScheme {
        val s = SchemeVibrant(Hct.fromInt(seedArgb), dark, 0.0).toComposeColorScheme()
        if (!oled) return s
        return s.copy(
            background = Color.Black, surface = Color.Black, surfaceDim = Color.Black,
            surfaceContainerLowest = Color.Black, surfaceContainerLow = Color(0xFF0A0A0C),
        )
    }
}

/**
 * The design's roles, read off a scheme, so every screen names the same thing the same way:
 * the page, the raised cards on it, the soft fills (progress, the big play buttons) and the two
 * warm badges (coral and pink) the mock-ups use for secondary actions.
 */
val ColorScheme.page: Color get() = surface
val ColorScheme.card: Color get() = surfaceContainerHigh
val ColorScheme.cardHigh: Color get() = surfaceContainerHighest
val ColorScheme.soft: Color get() = primaryContainer
val ColorScheme.onSoft: Color get() = onPrimaryContainer
val ColorScheme.coral: Color get() = tertiaryContainer
val ColorScheme.onCoral: Color get() = onTertiaryContainer
val ColorScheme.pink: Color get() = secondaryContainer
val ColorScheme.onPink: Color get() = onSecondaryContainer

/**
 * The design's deep container (the mini player, the Home hero): the scheme's primary hue at
 * almost-black on a light page; on a dark page the highest surface, so it still stands out.
 */
@SuppressLint("RestrictedApi")
fun ColorScheme.deep(): Color =
    if (surface.luminanceIsLight()) Color(Hct.from(Hct.fromInt(primary.toArgb()).hue, 40.0, 10.0).toInt())
    else surfaceContainerHighest

/** Text and icons on [deep]. */
fun ColorScheme.onDeep(): Color = if (surface.luminanceIsLight()) inverseOnSurface else onSurface

private fun Color.luminanceIsLight() = (0.2126f * red + 0.7152f * green + 0.0722f * blue) > 0.5f
