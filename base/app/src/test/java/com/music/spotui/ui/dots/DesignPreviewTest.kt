package com.music.spotui.ui.dots

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * Renders the design's building blocks to PNG files, to look at them without a phone. Skipped
 * unless the build passes -PdesignPreview=<covers dir>:<out dir> (covers: any PNG/JPEG pictures).
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], application = android.app.Application::class, qualifiers = "w420dp-h900dp-xhdpi")
class DesignPreviewTest {
    @get:Rule
    val compose = createComposeRule()

    private val dirs = System.getProperty("designPreview")?.split(":")

    private fun shoot(name: String, content: @Composable () -> Unit) {
        compose.setContent(content)
        compose.waitForIdle()
        val out = File(dirs!![1], "$name.png")
        out.outputStream().use { compose.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test
    fun halftoneCovers() {
        assumeTrue(dirs != null)
        val covers = File(dirs!![0]).listFiles { f -> f.extension.lowercase() in setOf("png", "jpg", "jpeg") }!!.sortedBy { it.name }
        val decoded = covers.map { f ->
            val bmp = BitmapFactory.decodeFile(f.path)
            val px = IntArray(bmp.width * bmp.height)
            bmp.getPixels(px, 0, bmp.width, 0, 0, bmp.width, bmp.height)
            Triple(f.nameWithoutExtension, bmp, Halftone.separate(px, bmp.width, bmp.height))
        }
        shoot("covers") {
            Column(Modifier.background(Color(0xFFFBF4FF)).padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                for ((name, bmp, art) in decoded) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Image(bmp.asImageBitmap(), null, Modifier.size(110.dp))
                        Canvas(Modifier.size(110.dp).clip(DotShapes.Squircle)) { drawHalftone(art, 30f, 0f) }
                        Canvas(Modifier.size(110.dp).clip(DotShapes.coverShapeFor(name))) { drawHalftone(art, 22f, 1.7f) }
                        Column {
                            Text(name, fontSize = 11.sp)
                            Text(art.style.name, fontSize = 11.sp)
                            Row {
                                for (c in listOf(art.paper, art.inkA, art.inkB)) Box(Modifier.size(16.dp).background(Color(c)))
                            }
                        }
                    }
                }
            }
        }
    }

    @Test
    fun fieldsAndControls() {
        assumeTrue(dirs != null)
        shoot("fields") {
            Column(Modifier.background(Color(0xFFFBF4FF)).padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.size(190.dp, 220.dp).clip(androidx.compose.foundation.shape.RoundedCornerShape(28.dp)).background(Color(0xFF1D0B33))) {
                        Canvas(Modifier.size(190.dp, 220.dp)) { drawDotField(DotField.HOME_HERO, Color(0xFF9D7BF0), Color(0xFFFF8A7A), 4.dp.toPx() * 2, 0.6f) }
                    }
                    Box(Modifier.size(190.dp, 220.dp).background(Color(0xFF2A0C06))) {
                        Canvas(Modifier.size(190.dp, 220.dp)) { drawDotField(DotField.PLAYER, Color(0xFFFF7A45), Color(0xFFFFE9D6), 4.5.dp.toPx() * 2, 0.6f) }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.size(190.dp, 150.dp).background(Color(0xFFFBF4FF))) {
                        Canvas(Modifier.size(190.dp, 150.dp)) { drawDotField(DotField.PAGE_HEADER, Color(0xFFFF4A1C), Color(0xFF1B1B3A), 4.dp.toPx() * 2, 0.6f) }
                    }
                    Box(Modifier.size(190.dp, 150.dp).background(Color(0xFF2A0C06))) {
                        Canvas(Modifier.size(190.dp, 150.dp)) { drawMoire(Color(0xFFFF7A45), 0.6f, lines = 30, alpha = 0.5f) }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    MorphPlayButton(true, {}, Color(0xFFCDB4FF), Color(0xFF22005D), width = 72.dp, label = "pause")
                    MorphPlayButton(false, {}, Color(0xFFCDB4FF), Color(0xFF22005D), width = 72.dp, label = "play")
                    Box(Modifier.size(56.dp).clip(DotShapes.cookie()).background(Color(0xFFFF8A7A)))
                    Box(Modifier.size(56.dp).clip(DotShapes.Arch).background(Color(0xFF6B3FD4)))
                    Box(Modifier.size(56.dp).clip(DotShapes.Blob).background(Color(0xFFFFB4C8)))
                    Box(Modifier.size(56.dp).clip(DotShapes.Leaf).background(Color(0xFFD7C2FF)))
                }
                Box(Modifier.background(Color(0xFF2A0C06)).padding(12.dp)) {
                    WavyProgress({ 0.42f }, Color(0xFFFF7A45), Color(0xFF5C1D0E), Modifier.size(360.dp, 28.dp), amplitude = 6.dp, wavelength = 14.dp, stroke = 6.dp, thumb = Color.White, animate = false)
                }
            }
        }
    }
}
