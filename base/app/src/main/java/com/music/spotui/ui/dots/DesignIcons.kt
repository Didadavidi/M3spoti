package com.music.spotui.ui.dots

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/**
 * The design's icons, drawn from its own 24-unit paths: thick round strokes (2.4 to 2.8) for
 * outline icons, solid shapes for the transport controls. Use with Icon(), which tints them.
 */
object DesignIcons {
    private fun circle(cx: Float, cy: Float, r: Float) = "M${cx - r} ${cy}a$r $r 0 1 0 ${2 * r} 0a$r $r 0 1 0 ${-2 * r} 0"

    private fun stroke(name: String, width: Float, vararg d: String) = ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply {
        for (p in d) addPath(
            PathParser().parsePathString(p).toNodes(),
            fill = null,
            stroke = SolidColor(Color.Black),
            strokeLineWidth = width,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        )
    }.build()

    private fun fill(name: String, vararg d: String) = ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply {
        for (p in d) addPath(PathParser().parsePathString(p).toNodes(), fill = SolidColor(Color.Black))
    }.build()

    val Home by lazy { stroke("home", 2.4f, "M4 11l8-7 8 7v8a1 1 0 0 1-1 1h-4v-6H9v6H5a1 1 0 0 1-1-1z") }
    val Explore by lazy { stroke("explore", 2.4f, circle(12f, 12f, 9f), "M15.5 8.5l-2 5-5 2 2-5z") }
    val Library by lazy { stroke("library", 2.4f, "M4 19V5M8 19V5M12 19l3-14 5 1-3 14z") }
    val Profile by lazy { stroke("profile", 2.6f, circle(12f, 8f, 4f), "M4 21a8 8 0 0 1 16 0") }
    val Search by lazy { stroke("search", 2.6f, circle(11f, 11f, 7f), "M20 20l-4-4") }
    val Tune by lazy { stroke("tune", 2.6f, "M4 6h10M4 12h4M12 12h8M4 18h12", circle(17f, 6f, 2.2f), circle(10f, 12f, 2.2f), circle(19f, 18f, 2.2f)) }
    val Chevrons by lazy { stroke("chevrons", 3f, "M6 6l6 6-6 6M13 6l6 6-6 6") }
    val Check by lazy { stroke("check", 2.8f, "M5 12l5 5 9-10") }
    val AddToQueue by lazy { stroke("add_queue", 2.8f, "M4 6h11M4 12h11M4 18h7M18 13v8M14 17h8") }
    val Shuffle by lazy { stroke("shuffle", 2.6f, "M16 3h5v5M4 20L21 3M21 16v5h-5M15 15l6 6M4 4l5 5") }
    val Repeat by lazy { stroke("repeat", 2.6f, "M17 2l3 3-3 3M4 11V9a4 4 0 0 1 4-4h12M7 22l-3-3 3-3M20 13v2a4 4 0 0 1-4 4H4") }
    val Back by lazy { stroke("back", 2.8f, "M19 12H5M11 18l-6-6 6-6") }
    val Down by lazy { stroke("down", 2.8f, "M6 9l6 6 6-6") }
    val Close by lazy { stroke("close", 2.8f, "M6 6l12 12M18 6L6 18") }
    val Download by lazy { stroke("download", 2.6f, "M12 4v11M7 10l5 5 5-5M5 20h14") }
    val Share by lazy { stroke("share", 2.4f, circle(6f, 12f, 2.5f), circle(18f, 6f, 2.5f), circle(18f, 18f, 2.5f), "M8.2 10.8l7.6-3.6M8.2 13.2l7.6 3.6") }
    val Bookmark by lazy { stroke("bookmark", 2.6f, "M6 3h12v18l-6-4-6 4z") }
    val Filter by lazy { stroke("filter", 2.4f, "M4 5h16l-6 7.5V19l-4 2v-8.5z") }
    val Sort by lazy { stroke("sort", 2.6f, "M4 7h16M7 12h10M10 17h4") }
    val Sleep by lazy { stroke("sleep", 2.4f, "M20 15.5A8.5 8.5 0 0 1 8.5 4a8.5 8.5 0 1 0 11.5 11.5z") }
    val Lyrics by lazy { stroke("lyrics", 2.6f, "M4 6h16M4 12h10M4 18h7") }
    val Queue by lazy { stroke("queue", 2.6f, "M4 6h12M4 12h12M4 18h8M17 15l4 3-4 3z") }
    val Reorder by lazy { stroke("reorder", 2.6f, "M5 9h14M5 15h14") }
    val Plus by lazy { stroke("plus", 2.8f, "M12 5v14M5 12h14") }

    val More by lazy { fill("more", circle(12f, 5f, 2.3f), circle(12f, 12f, 2.3f), circle(12f, 19f, 2.3f)) }
    val Heart by lazy { fill("heart", "M12 20s-7-4.4-7-10a4 4 0 0 1 7-2.6A4 4 0 0 1 19 10c0 5.6-7 10-7 10z") }
    val HeartOutline by lazy { stroke("heart_outline", 2.4f, "M12 20s-7-4.4-7-10a4 4 0 0 1 7-2.6A4 4 0 0 1 19 10c0 5.6-7 10-7 10z") }
    val Play by lazy { fill("play", "M8 5.5v13a1 1 0 0 0 1.5.86l10-6.5a1 1 0 0 0 0-1.72l-10-6.5A1 1 0 0 0 8 5.5z") }
    val Pause by lazy { fill("pause", "M7.5 4.5h1a2 2 0 0 1 2 2v11a2 2 0 0 1-2 2h-1a2 2 0 0 1-2-2v-11a2 2 0 0 1 2-2zM15.5 4.5h1a2 2 0 0 1 2 2v11a2 2 0 0 1-2 2h-1a2 2 0 0 1-2-2v-11a2 2 0 0 1 2-2z") }
    val Next by lazy { fill("next", "M5 6.2v11.6a1 1 0 0 0 1.55.83L15 13v4.5a1 1 0 0 0 2 0v-11a1 1 0 0 0-2 0V11L6.55 5.37A1 1 0 0 0 5 6.2z") }
    val Previous by lazy { fill("previous", "M19 6.2v11.6a1 1 0 0 1-1.55.83L9 13v4.5a1 1 0 0 1-2 0v-11a1 1 0 0 1 2 0V11l8.45-5.63A1 1 0 0 1 19 6.2z") }
}
