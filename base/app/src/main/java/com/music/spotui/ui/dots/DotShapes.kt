package com.music.spotui.ui.dots

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * The cover shapes of the design. Each is drawn in any size: cookie (a scalloped disc), arch,
 * blob, squircle, circle and leaf. [coverShapeFor] picks one per item, stable across visits.
 */
object DotShapes {
    /** A disc with [lobes] soft scallops, [depth] of the radius deep (the design's "cookie"). */
    fun cookie(lobes: Int = 9, depth: Float = 0.12f): Shape = GenericShape { size, _ ->
        val n = 144
        for (i in 0..n) {
            val a = i / n.toFloat() * 2f * PI.toFloat()
            val r = 0.5f * (1f - depth * (1f - cos(lobes * a)) / 2f)
            val x = size.width * (0.5f + r * cos(a))
            val y = size.height * (0.5f + r * sin(a))
            if (i == 0) moveTo(x, y) else lineTo(x, y)
        }
        close()
    }

    /** Fully round top, gently rounded bottom. */
    val Arch: Shape = GenericShape { size, _ ->
        val top = size.width / 2f
        val bottom = size.minDimension * 0.13f
        addRoundRect(RoundRect(0f, 0f, size.width, size.height,
            topLeftCornerRadius = CornerRadius(top, top), topRightCornerRadius = CornerRadius(top, top),
            bottomRightCornerRadius = CornerRadius(bottom, bottom), bottomLeftCornerRadius = CornerRadius(bottom, bottom)))
    }

    /** A soft pebble: elliptical corners of unequal size (58% 42% 50% 50% / 46% 54% 46% 54%). */
    val Blob: Shape = GenericShape { size, _ ->
        val w = size.width
        val h = size.height
        addRoundRect(RoundRect(0f, 0f, w, h,
            topLeftCornerRadius = CornerRadius(w * 0.58f, h * 0.46f),
            topRightCornerRadius = CornerRadius(w * 0.42f, h * 0.54f),
            bottomRightCornerRadius = CornerRadius(w * 0.50f, h * 0.46f),
            bottomLeftCornerRadius = CornerRadius(w * 0.50f, h * 0.54f)))
    }

    val Squircle: Shape = RoundedCornerShape(32)
    val Circle: Shape = CircleShape

    /** Two opposite round corners, two tight ones. */
    val Leaf: Shape = RoundedCornerShape(topStartPercent = 50, topEndPercent = 8, bottomEndPercent = 50, bottomStartPercent = 8)

    /** The player's tilted cover: a capsule (round ends). */
    val Pill: Shape = RoundedCornerShape(50)

    /** Rounded-square tiles (list rows, the queue): the design's 24 dp on a 76 dp tile. */
    val Tile: Shape = RoundedCornerShape(30)

    private val varied by lazy { listOf(cookie(), Arch, Blob, Squircle, Leaf) }

    /**
     * The shape a cover gets: artists are always circles; albums, playlists, shows... get one of
     * the varied shapes, picked from their id so an item keeps its shape everywhere.
     */
    fun coverShapeFor(id: String, artist: Boolean = false): Shape =
        if (artist) Circle else varied[(id.hashCode() and 0x7FFFFFFF) % varied.size]

    /** Badge shapes cycling down a list (queue numbers, settings icons): circle, square, arch, leaf. */
    val badges: List<Shape> = listOf(
        CircleShape,
        RoundedCornerShape(14.dp),
        RoundedCornerShape(topStartPercent = 50, topEndPercent = 50, bottomEndPercent = 28, bottomStartPercent = 28),
        RoundedCornerShape(topStartPercent = 28, topEndPercent = 50, bottomEndPercent = 28, bottomStartPercent = 50),
    )
}
