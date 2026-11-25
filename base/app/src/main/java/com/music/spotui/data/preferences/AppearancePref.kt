package com.music.spotui.data.preferences

/*
 * Appearance choices on top of the look: the wallpaper's colours (Material You) for the Classic
 * look, and the font of both looks. Compose state, so every screen redraws when one changes.
 */

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

private const val PREF = "settings_prefs"

private fun prefs(c: Context) = c.getSharedPreferences(PREF, Context.MODE_PRIVATE)

/**
 * "Dynamic colors (Material You)": the Classic look's accent taken from the wallpaper (Android 12
 * and later), in place of the accent chosen in the settings. Off by default. The Immersive look
 * keeps its own colours, taken from the playing cover.
 */
object DynamicColorPref {
    private const val KEY = "dynamic_color"

    /** The phone has wallpaper colours to give (Android 12 and later). */
    val available: Boolean get() = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S

    var on by mutableStateOf(prefs(com.music.spotui.di.Graph.app).getBoolean(KEY, false))
        private set

    /** On, and the phone can give the colours. */
    val active: Boolean get() = on && available

    fun set(c: Context, value: Boolean) {
        on = value
        cached = null
        prefs(c).edit().putBoolean(KEY, value).apply()
    }

    // The wallpaper's accent, read once per configuration (see [refresh]): it is asked at every draw.
    @Volatile private var cached: Color? = null

    /** The configuration changed (a new wallpaper recreates the activity): the accent is read again. */
    fun refresh() { cached = null }

    /**
     * The wallpaper's accent for the Classic look, or null (off, Immersive, before Android 12). The
     * same tone Material's dynamic dark scheme gives its primary colour (system_accent1_200).
     */
    fun accent(): Color? {
        if (!on || android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.S || LookPref.look != Look.CLASSIC) return null
        cached?.let { return it }
        return runCatching { Color(com.music.spotui.di.Graph.app.getColor(android.R.color.system_accent1_200)) }
            .getOrNull()?.also { cached = it }
    }
}

/**
 * "App font": Myspoti's own fonts (Manrope in the Classic look, Inter in the Immersive one, the
 * default) or the phone's system font. No font is bundled for it: the system one is the phone's.
 */
object AppFontPref {
    private const val KEY = "system_font"

    /** True: the phone's system font everywhere in the app. */
    var system by mutableStateOf(prefs(com.music.spotui.di.Graph.app).getBoolean(KEY, false))
        private set

    fun set(c: Context, value: Boolean) {
        system = value
        prefs(c).edit().putBoolean(KEY, value).apply()
    }
}

/**
 * How covers are drawn: [AUTO] in dots or in waves, whichever suits the picture (the design);
 * always [DOTS]; always [WAVES]; or the [ORIGINAL] picture, no halftone (lighter for the phone).
 */
object CoverStylePref {
    const val AUTO = 0
    const val DOTS = 1
    const val WAVES = 2
    const val ORIGINAL = 3
    private const val KEY = "cover_style"

    var mode by mutableStateOf(prefs(com.music.spotui.di.Graph.app).getInt(KEY, AUTO))
        private set

    fun set(c: Context, value: Int) {
        mode = value
        prefs(c).edit().putInt(KEY, value).apply()
    }
}

/**
 * The decorative backgrounds behind the Home hero, page headers and the player: the design's dots
 * (moving with the music), or the same pattern as a soft, still gradient (lighter for the phone).
 */
object FieldStylePref {
    private const val KEY = "field_gradient"

    var gradient by mutableStateOf(prefs(com.music.spotui.di.Graph.app).getBoolean(KEY, false))
        private set

    fun set(c: Context, value: Boolean) {
        gradient = value
        prefs(c).edit().putBoolean(KEY, value).apply()
    }
}

/**
 * "Download lyrics with songs": a downloaded song's lyrics are looked up and kept with it, so the
 * lyrics show without a connection. On by default.
 */
object LyricsOfflinePref {
    private const val KEY = "download_lyrics"

    var on by mutableStateOf(prefs(com.music.spotui.di.Graph.app).getBoolean(KEY, true))
        private set

    fun set(c: Context, value: Boolean) {
        on = value
        prefs(c).edit().putBoolean(KEY, value).apply()
    }
}
