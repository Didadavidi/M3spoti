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

    /** Home in light and dark (system colours from the design's own seed; covers are synthetic). */
    @Test fun homeLight() = home(dark = false)
    @Test fun homeDark() = home(dark = true)

    private fun home(dark: Boolean) {
        assumeTrue(dirs != null)
        val app = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.app.Application>()
        com.music.spotui.di.Graph::class.java.getDeclaredField("app").apply { isAccessible = true }.set(com.music.spotui.di.Graph, app)
        val st = com.music.spotui.di.Graph.currentSongState
        val songs = (1..12).map { com.music.spotui.data.entity.SongsModel(it, "Song $it", "Low Tide Radio", "Marlow Vey", "", "spotify:track:$it", spotifyTrackId = "t$it") }
        st.updateQueue(songs)
        st.updateSongState("", "Harbor Lights", "Marlow Vey", true, 1, 3, "Low Tide Radio")
        val mix = com.music.spotui.data.entity.HomeItem.Playlist("Daily Mix 1", "", "Marlow Vey, Juno Park, Ilse Moor and more", "mix1")
        val others = listOf(
            com.music.spotui.data.entity.HomeItem.Playlist("Daily Mix 2", "", "Okoro, The Ferns", "mix2"),
            com.music.spotui.data.entity.HomeItem.Playlist("Daily Mix 3", "", "Ava Lindqvist", "mix3"),
            com.music.spotui.data.entity.HomeItem.Album("a1", "Night Ferry", "", "Album • Ilse Moor"),
            com.music.spotui.data.entity.HomeItem.Artist("Juno Park", "", "ar1"),
        )
        run {
            val scheme = com.music.spotui.ui.theme.SystemColors.build(0xFF6B3FD4.toInt(), dark, false)
            setScheme(scheme, dark)
            shoot(if (dark) "home-dark" else "home-light") {
                androidx.compose.material3.MaterialTheme(colorScheme = scheme, typography = com.music.spotui.ui.theme.Typography) {
                    val nav = androidx.navigation.compose.rememberNavController()
                    Column(Modifier.background(scheme.surface).size(420.dp, 1500.dp)) {
                        com.music.spotui.ui.screens.HomeTopBar(nav) {}
                        com.music.spotui.ui.screens.HomeHero(nav, mix, others)
                        com.music.spotui.ui.screens.HomeKeepListening(nav, others)
                        com.music.spotui.ui.screens.HomeShelf(nav, "Made for you", others + mix)
                    }
                }
            }
        }
    }

    /** Points SystemColors at [scheme] (its setter is private: the test sets the backing state). */
    private fun setScheme(scheme: androidx.compose.material3.ColorScheme, dark: Boolean) {
        val o = com.music.spotui.ui.theme.SystemColors
        for (name in listOf("scheme\$delegate", "dark\$delegate")) {
            val f = o.javaClass.getDeclaredField(name)
            f.isAccessible = true
            @Suppress("UNCHECKED_CAST")
            val st = f.get(o) as androidx.compose.runtime.MutableState<Any>
            st.value = if (name.startsWith("scheme")) scheme else dark
        }
    }
}
