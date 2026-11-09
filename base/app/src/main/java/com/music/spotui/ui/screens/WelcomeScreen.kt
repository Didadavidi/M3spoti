package com.music.spotui.ui.screens

import com.music.spotui.ui.theme.tone
import com.music.spotui.ui.theme.AppText

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.myspoti.engine.R
import com.music.spotui.ui.components.pressable
import com.music.spotui.ui.theme.AppAccent
import com.music.spotui.ui.theme.AppBackground
import com.music.spotui.ui.theme.AppSans
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import com.music.spotui.ui.theme.deep
import com.music.spotui.ui.theme.coral
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.foundation.border
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * First screen of a signed-out app, in the design: a deep hero card ("Hi. Let's play.") with the
 * dot field and halftone shapes, then what signing in is and the button that opens Spotify's own
 * login page ([onLogin]). The email and the code are typed on that page, never in the app.
 * The parts rise into place one after the other.
 */
@Composable
fun WelcomeScreen(onLogin: () -> Unit) {
    val s = com.music.spotui.ui.theme.SystemColors.scheme
    // One entrance per part, a beat apart: hero, text, button.
    val parts = remember { List(3) { Animatable(0f) } }
    LaunchedEffect(Unit) {
        parts.forEachIndexed { i, a -> launch { delay(120L + i * 180L); a.animateTo(1f, tween(650, easing = FastOutSlowInEasing)) } }
    }
    fun Modifier.rise(i: Int) = graphicsLayer {
        val v = parts[i].value
        alpha = v
        translationY = (1f - v) * 24.dp.toPx()
    }
    var help by remember { mutableStateOf(false) }
    if (help) com.music.spotui.ui.components.LoginHelpDialog(onClose = { help = false })
    Column(
        Modifier.fillMaxSize().background(AppBackground).systemBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        WelcomeHero(Modifier.weight(1f).fillMaxWidth().rise(0))
        Column(Modifier.fillMaxWidth().padding(start = 8.dp, end = 8.dp, top = 24.dp).rise(1)) {
            Text(
                stringResource(R.string.welcome_signin_title), color = s.onSurface,
                fontFamily = com.music.spotui.ui.theme.FlexDisplay, fontWeight = FontWeight.Black, fontSize = 26.sp,
            )
            Text(
                stringResource(R.string.welcome_privacy), color = s.onSurfaceVariant, fontFamily = AppSans,
                fontSize = 15.sp, lineHeight = 20.sp, modifier = Modifier.padding(top = 6.dp),
            )
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth().padding(top = 22.dp).rise(2),
        ) {
            // A TV remote: the button takes the focus at once and shows it (a ring); a focus with
            // no look left nothing on screen to say what OK would do.
            val ctx = androidx.compose.ui.platform.LocalContext.current
            val tv = remember { com.music.spotui.ui.tv.Tv.isTv(ctx) }
            val focus = remember { androidx.compose.ui.focus.FocusRequester() }
            var focused by remember { mutableStateOf(false) }
            if (tv) LaunchedEffect(Unit) { delay(300); runCatching { focus.requestFocus() } }
            androidx.compose.foundation.layout.Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .then(if (tv) Modifier.focusRequester(focus).onFocusChanged { focused = it.isFocused } else Modifier)
                    .pressable(0.96f, onClick = onLogin)
                    .clip(RoundedCornerShape(30.dp))
                    .then(if (focused) Modifier.border(3.dp, s.onSurface, RoundedCornerShape(30.dp)) else Modifier)
                    .background(s.primary),
            ) {
                Text(stringResource(R.string.welcome_login), color = s.onPrimary, fontFamily = AppSans,
                    fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                androidx.compose.material3.Icon(
                    androidx.compose.material.icons.Icons.AutoMirrored.Filled.ArrowForward, null,
                    tint = s.onPrimary, modifier = Modifier.size(22.dp),
                )
            }
            // What goes wrong at sign-in, and the way to a report: the app's login help.
            androidx.compose.foundation.layout.Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 14.dp).clip(RoundedCornerShape(50))
                    .pressable(0.96f) { help = true }.padding(horizontal = 14.dp, vertical = 10.dp),
            ) {
                androidx.compose.material3.Icon(
                    androidx.compose.material.icons.Icons.AutoMirrored.Outlined.HelpOutline, null,
                    tint = s.primary, modifier = Modifier.size(18.dp),
                )
                Text(stringResource(R.string.welcome_trouble), color = s.primary, fontFamily = AppSans,
                    fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.padding(start = 6.dp))
            }
        }
    }
}

/**
 * The design's welcome card: the deep colour under the Home hero's dot field, a tilted capsule and
 * two discs in halftone (the design's banded art: nothing is playing yet), and "Hi. Let's play."
 * set huge and wide at its foot.
 */
@Composable
private fun WelcomeHero(modifier: Modifier) {
    val s = com.music.spotui.ui.theme.SystemColors.scheme
    val deep = s.deep()
    androidx.compose.foundation.layout.BoxWithConstraints(modifier.clip(RoundedCornerShape(40.dp)).background(deep)) {
        com.music.spotui.ui.dots.DotFieldBackground(
            field = com.music.spotui.ui.dots.DotField.HOME_HERO,
            inkA = androidx.compose.ui.graphics.lerp(s.primary, s.primaryContainer, 0.45f),
            inkB = s.coral,
            step = 8f,
        )
        val w = maxWidth
        val ring = Modifier.border(6.dp, deep, androidx.compose.foundation.shape.CircleShape)
        // The capsule, upper right, tilted like the Home hero's.
        Box(
            Modifier.align(Alignment.TopEnd).padding(top = w * 0.08f, end = w * 0.04f)
                .size(w * 0.42f, w * 0.66f).graphicsLayer { rotationZ = 38f }
                .border(6.dp, deep, com.music.spotui.ui.dots.DotShapes.Pill).padding(3.dp),
        ) {
            com.music.spotui.ui.dots.HalftoneCover(null, Modifier.fillMaxSize(), com.music.spotui.ui.dots.DotShapes.Pill,
                cells = 24f, animate = true, fallbackSeed = "M3spoti")
        }
        Box(Modifier.align(Alignment.TopStart).padding(start = w * 0.12f, top = w * 0.2f).size(w * 0.3f).then(ring).padding(3.dp)) {
            com.music.spotui.ui.dots.HalftoneCover(null, Modifier.fillMaxSize(), androidx.compose.foundation.shape.CircleShape,
                cells = 16f, animate = true, fallbackSeed = "Hi")
        }
        Text(
            stringResource(R.string.welcome_hi),
            color = Color.White, fontFamily = com.music.spotui.ui.theme.FlexWide, fontWeight = FontWeight.Black,
            fontSize = 54.sp, lineHeight = 50.sp, letterSpacing = (-1.5).sp,
            modifier = Modifier.align(Alignment.BottomStart).padding(start = 22.dp, bottom = 22.dp, end = 16.dp),
        )
    }
}


/** A soft green light behind the logo, drifting slowly. */
@Composable
internal fun Glow(modifier: Modifier = Modifier, alpha: Float = 0.30f) {
    // "Remove animations": the light stays where it starts.
    val reduce = com.music.spotui.ui.look.rememberReduceMotion()
    val phase = if (reduce) 0f else {
        val t = rememberInfiniteTransition(label = "glow")
        val p by t.animateFloat(0f, (2 * PI).toFloat(), infiniteRepeatable(tween(16_000, easing = LinearEasing)), label = "phase")
        p
    }
    Canvas(modifier) {
        val c = Offset(size.width * (0.5f + 0.12f * sin(phase)), size.height * (0.30f + 0.05f * cos(phase)))
        val r = size.width * 1.05f
        drawCircle(Brush.radialGradient(listOf(AppAccent.copy(alpha = alpha), Color.Transparent), c, r), r, c)
    }
}

/** "Your music," then what Myspoti adds to it, one feature after the other. */
@Composable
private fun Features() {
    val features = listOf(
        stringResource(R.string.welcome_rotate_ads),
        stringResource(R.string.welcome_rotate_lyrics),
        stringResource(R.string.welcome_rotate_clips),
        stringResource(R.string.welcome_rotate_offline),
    )
    var index by remember { mutableIntStateOf(0) }
    val reduce = com.music.spotui.ui.look.rememberReduceMotion()
    LaunchedEffect(Unit) {
        delay(1_200)
        while (true) { delay(2_400); index = (index + 1) % features.size }
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(stringResource(R.string.welcome_lead), color = AppText.copy(alpha = 0.7f), fontFamily = AppSans,
            fontSize = 17.sp, textAlign = TextAlign.Center)
        AnimatedContent(
            targetState = index,
            transitionSpec = {
                if (reduce) EnterTransition.None togetherWith ExitTransition.None
                else (slideInVertically(tween(400, easing = FastOutSlowInEasing)) { it / 2 } + fadeIn(tween(400))) togetherWith
                    (slideOutVertically(tween(400, easing = FastOutSlowInEasing)) { -it / 2 } + fadeOut(tween(250)))
            },
            label = "feature",
        ) { i ->
            Text(features[i], color = AppText, fontFamily = AppSans, fontWeight = FontWeight.Bold, fontSize = 19.sp,
                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 2.dp))
        }
    }
}
