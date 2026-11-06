package com.music.spotui.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Who decides the colour of the status and navigation bar icons. [SpotuiTheme] sets them on every
 * recomposition from the light or dark palette; a page that is dark whatever the theme (the
 * album-colour player, queue and lyrics) counts itself in [darkPages] while it shows, and the
 * theme then keeps the icons light. Setting the icons from the page itself was undone by the
 * theme's next recomposition: dark icons on the dark queue and player in the light theme.
 */
object SystemBars {
    var darkPages by mutableIntStateOf(0)
        internal set
}

/** This page is dark whatever the theme: light status and navigation bar icons while it shows. */
@Composable
fun DarkSystemBars() {
    DisposableEffect(Unit) {
        SystemBars.darkPages++
        onDispose { SystemBars.darkPages = (SystemBars.darkPages - 1).coerceAtLeast(0) }
    }
}

/**
 * A band in [color] behind the status bar, so its icons stay readable over what the page draws
 * there: [solid] for a list that scrolls under it, else a fade (a dot field or a cover under the
 * icons, which are white on dark pages and black on light ones whatever the dots below).
 */
@Composable
fun StatusBarScrim(color: Color, modifier: Modifier = Modifier, solid: Boolean = false, fade: Dp = 28.dp) {
    val bar = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val brush = if (solid) Brush.verticalGradient(0f to color, 1f to color)
    else Brush.verticalGradient(0f to color.copy(alpha = 0.92f), 0.55f to color.copy(alpha = 0.7f), 1f to color.copy(alpha = 0f))
    Box(modifier.fillMaxWidth().height(if (solid) bar else bar + fade).background(brush))
}
