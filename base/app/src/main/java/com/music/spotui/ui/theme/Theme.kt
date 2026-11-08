package com.music.spotui.ui.theme

import android.app.Activity
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

@Composable
fun SpotuiTheme(
    content: @Composable () -> Unit
) {
    val energy by app.myspoti.power.EnergySettings.state.collectAsStateWithLifecycle()
    val policy = energy.policy
    androidx.compose.runtime.LaunchedEffect(policy.videoMaxHeight, policy.allowBackgroundLookups) {
        com.music.spotui.video.MusicVideoController.onEnergyPolicyChanged()
    }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    // The album colours (player, queue, lyrics) follow the playing cover, process-wide.
    LaunchedEffect(Unit) { ExpressiveColor.start(context, scope) }
    // The system colours (every other screen): light or dark as the phone or Settings say.
    val dark = when (ColorPrefs.mode) {
        ThemeMode.SYSTEM -> androidx.compose.foundation.isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    // Read so a change rebuilds the scheme: AMOLED black and the wallpaper switch.
    @Suppress("UNUSED_VARIABLE") val oled = app.myspoti.power.EnergySettings.oledBlack
    @Suppress("UNUSED_VARIABLE") val wallpaper = ColorPrefs.systemColours
    SystemColors.update(context, dark)
    val visualScheme: ColorScheme = SystemColors.scheme
    val view = LocalView.current
    // Dark icons on the light palette, white ones on the dark and on the always-dark pages.
    val lightIcons = !dark && SystemBars.darkPages == 0
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.attributes = window.attributes.apply { preferredRefreshRate = policy.displayRefreshCap }
            window.statusBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = lightIcons
            // B19: the navigation bar was left to the system — a white band under the always-dark
            // UI (reported in the modal sheets, where the bar was opaque) with dark icons on it.
            window.navigationBarColor = Color.Transparent.toArgb()
            // Without this Android lays its own translucent scrim on a transparent bar (API 29+),
            // which showed as a differently coloured strip under the full-screen Canvas.
            window.isNavigationBarContrastEnforced = false
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = lightIcons
            // Behind the first frame and under overscroll: the palette's page colour.
            window.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(AppBackground.toArgb()))
        }
    }

    MaterialTheme(
        colorScheme = visualScheme,
        motionScheme = MotionScheme.expressive(),
        typography = Typography,
    ) {
        // Bare Text() uses LocalTextStyle, which MaterialTheme does NOT derive
        // from the typography — so provide Montserrat as the global default so
        // every screen picks it up without touching each Text call.
        // An extension's look may make the text a little smaller or larger (ui.styles.text.scale).
        val density = androidx.compose.ui.platform.LocalDensity.current
        val scale = com.music.spotui.extensions.ExtensionLook.textScale
        androidx.compose.runtime.CompositionLocalProvider(
            androidx.compose.ui.platform.LocalDensity provides
                if (scale == 1f) density else androidx.compose.ui.unit.Density(density.density, density.fontScale * scale),
        ) {
            androidx.compose.material3.ProvideTextStyle(
                value = androidx.compose.material3.LocalTextStyle.current.copy(
                    fontFamily = Montserrat,
                    letterSpacing = (-0.2).sp,
                ),
                content = content,
            )
        }
    }
}
