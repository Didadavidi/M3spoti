/* SPDX-License-Identifier: GPL-3.0-only */
package com.music.spotui.ui.tv

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.myspoti.engine.R
import app.myspoti.host.android.MyspotiRuntime
import app.myspoti.host.android.SavedAccount
import com.music.spotui.di.Graph
import com.music.spotui.di.SessionActions
import com.music.spotui.ui.components.sendLoginReport
import com.music.spotui.ui.dots.DesignIcons
import com.music.spotui.ui.dots.DotField
import com.music.spotui.ui.dots.DotShapes
import com.music.spotui.ui.theme.FlexWide
import com.music.spotui.ui.theme.coral
import com.music.spotui.ui.theme.onCoral
import com.music.spotui.ui.theme.onPink
import com.music.spotui.ui.theme.pink
import kotlinx.coroutines.delay

/*
 * Signing in on a TV, in the M3spoti TV design. The e-mail and the code are typed on Spotify's
 * own sign-in page (never in the app), so the design's e-mail and code steps are that page; around
 * it: the welcome, the help, and once signed in "who's listening?" with the accounts kept here.
 */

/** The deep panel colour of the sign-in screens: the page, a third of the way to the primary container. */
@Composable
private fun deepPanel(): Color = lerp(tvScheme.surface, tvScheme.primaryContainer, 0.35f)

/** The right-hand panel: deep, round on its left side, with the design's dot field. */
@Composable
private fun SignInPanel(width: androidx.compose.ui.unit.Dp, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val ui = tvScheme
    val shape = RoundedCornerShape(topStart = 64.dp, bottomStart = 64.dp)
    Box(modifier.width(width).fillMaxHeight().clip(shape).background(deepPanel())) {
        TvDots(ui.primary, ui.coral, field = DotField.HOME_HERO)
        content()
    }
}

/** Welcome: "Hi. Let's play.", a line, Get started (Spotify's sign-in page) and Help; halftone shapes on the right. */
@Composable
fun TvWelcome(onLogin: () -> Unit) {
    val ui = tvScheme
    var help by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    if (help) { TvLoginHelp(onClose = { help = false }); return }
    val first = remember { FocusRequester() }
    Box(Modifier.fillMaxSize().background(ui.surface)) {
        SignInPanel(440.dp, Modifier.align(Alignment.CenterEnd)) {
            TvArt("", "welcome-a", Modifier.offset(127.dp, 68.dp).size(255.dp, 305.dp).graphicsLayer { rotationZ = 28f }, DotShapes.Pill, cells = 26f, animate = true)
            TvArt("", "welcome-b", Modifier.offset(60.dp, 300.dp).size(130.dp), CircleShape, cells = 18f, animate = true)
        }
        Column(Modifier.align(Alignment.CenterStart).padding(start = 48.dp).width(430.dp)) {
            TvText(stringResource(R.string.welcome_hi), ui.onSurface, 60.sp, FontWeight.Black, family = FlexWide, lines = 2)
            Spacer(Modifier.height(16.dp))
            TvText(stringResource(R.string.tv_welcome_body), ui.onSurfaceVariant, 20.sp, FontWeight.Medium, lines = 3)
            Spacer(Modifier.height(28.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                TvButton(stringResource(R.string.tv_get_started), Icons.AutoMirrored.Filled.ArrowForward, ui.primary, ui.onPrimary,
                    Modifier.focusRequester(first), iconAfter = true, onClick = onLogin)
                TvButton(stringResource(R.string.tv_help), null, ui.surfaceContainerHigh, ui.onSurface) { help = true }
            }
        }
    }
    LaunchedEffect(Unit) { runCatching { delay(300); first.requestFocus() } }
}

/**
 * Trouble signing in: three tips as cards (each with its badge in the design's shapes), and on the
 * right Start over (a new sign-in page, from nothing), Send the report, and Back.
 */
@Composable
fun TvLoginHelp(onClose: () -> Unit) {
    val ui = tvScheme
    val context = LocalContext.current
    val first = remember { FocusRequester() }
    androidx.activity.compose.BackHandler(onBack = onClose)
    Box(Modifier.fillMaxSize().background(ui.surface)) {
        Column(Modifier.padding(start = 48.dp, top = 40.dp).width(500.dp)) {
            TvTitle(stringResource(R.string.welcome_trouble), size = 44.sp)
            TvText(stringResource(R.string.login_help_code_title), ui.onSurfaceVariant, 20.sp, FontWeight.Medium, Modifier.padding(top = 10.dp))
            Spacer(Modifier.height(22.dp))
            val tips = listOf(
                Triple(Icons.Outlined.Email, R.string.login_tip_spam, R.string.tv_tip_spam_detail),
                Triple(Icons.Outlined.Schedule, R.string.login_tip_expire, R.string.tv_tip_expire_detail),
                Triple(Icons.Outlined.Edit, R.string.login_tip_spelling, R.string.tv_tip_spelling_detail),
            )
            val badges: List<Triple<Shape, Color, Color>> = listOf(
                Triple(CircleShape, ui.primary, ui.onPrimary),
                Triple(RoundedCornerShape(16.dp), ui.coral, ui.onCoral),
                Triple(RoundedCornerShape(topStartPercent = 50, topEndPercent = 50, bottomStartPercent = 30, bottomEndPercent = 30), ui.pink, ui.onPink),
            )
            tips.forEachIndexed { i, (icon, title, detail) ->
                val shape = when (i) {
                    0 -> RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp, bottomStart = 8.dp, bottomEnd = 8.dp)
                    tips.lastIndex -> RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 28.dp, bottomEnd = 28.dp)
                    else -> RoundedCornerShape(8.dp)
                }
                Row(Modifier.padding(bottom = 6.dp).fillMaxWidth().height(80.dp).clip(shape).background(ui.surfaceContainerLow).padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    val (bShape, bg, fg) = badges[i]
                    Box(Modifier.size(52.dp).clip(bShape).background(bg), contentAlignment = Alignment.Center) {
                        Icon(icon, null, tint = fg, modifier = Modifier.size(24.dp))
                    }
                    Column(Modifier.padding(start = 16.dp)) {
                        TvText(stringResource(title), ui.onSurface, 20.sp, FontWeight.Black)
                        TvText(stringResource(detail), ui.onSurfaceVariant, 16.sp, FontWeight.Medium)
                    }
                }
            }
        }
        SignInPanel(360.dp, Modifier.align(Alignment.CenterEnd)) {
            Column(Modifier.align(Alignment.Center).width(288.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // A half-finished flow is what Spotify answers "Oops! Something went wrong" to: from nothing.
                TvButton(stringResource(R.string.login_help_restart), DesignIcons.Repeat, ui.primary, ui.onPrimary,
                    Modifier.fillMaxWidth().focusRequester(first), gap = deepPanel()) { onClose(); MyspotiRuntime.host.retryLogin() }
                TvButton(stringResource(R.string.login_help_send), null, ui.surfaceContainerHigh, ui.onSurface, Modifier.fillMaxWidth(), gap = deepPanel()) {
                    onClose(); sendLoginReport(context)
                }
                TvButton(stringResource(R.string.tv_back), DesignIcons.Back, ui.surfaceContainerHigh, ui.onSurface, Modifier.fillMaxWidth(), gap = deepPanel(), onClick = onClose)
            }
        }
    }
    LaunchedEffect(Unit) { runCatching { delay(200); first.requestFocus() } }
}

/** "You're in" is shown once per TV, after the first sign-in. */
internal object TvYoureIn {
    private const val KEY = "TvYoureInSeen"
    private fun prefs(c: Context) = c.getSharedPreferences("myspoti_prefs", Context.MODE_PRIVATE)
    fun needed(c: Context) = !prefs(c).getBoolean(KEY, false)
    fun done(c: Context) = prefs(c).edit().putBoolean(KEY, true).apply()
}

/**
 * You're in! Who's listening?: the accounts kept on this TV as big cards (the one signed in first,
 * focused; another one switches to it), Add account, and "Signed in as …" with Sign out.
 */
@Composable
internal fun TvYoureInScreen(onDone: () -> Unit) {
    val ui = tvScheme
    val context = LocalContext.current
    val host = MyspotiRuntime.host
    val list by host.accounts.list.collectAsStateWithLifecycle()
    val livePlan by app.myspoti.spotify.AccountPlan.premium.collectAsStateWithLifecycle()
    val shown = remember(list) {
        val active = list.current ?: SavedAccount(profile = list.active)
        listOf(active) + list.accounts.filter { it.profile != list.active && it.known }.sortedByDescending { it.usedAt }
    }
    val first = remember { FocusRequester() }
    val activeName = com.music.spotui.data.api.ProfileCache.name ?: list.current?.name
    Box(Modifier.fillMaxSize().background(ui.surface)) {
        TvDots(ui.primary, ui.coral)
        Column(Modifier.fillMaxSize().padding(top = 40.dp)) {
            TvTitle(stringResource(R.string.tv_youre_in), Modifier.padding(horizontal = 48.dp), size = 44.sp)
            TvText(stringResource(R.string.tv_youre_in_body), ui.onSurfaceVariant, 20.sp, FontWeight.Medium, Modifier.padding(start = 48.dp, top = 10.dp))
            val looks = listOf(
                Triple<Shape, Color, Color>(CircleShape, ui.primary, ui.onPrimary),
                Triple(RoundedCornerShape(32.dp), ui.coral, ui.onCoral),
                Triple(RoundedCornerShape(topStart = 60.dp, topEnd = 60.dp, bottomStart = 24.dp, bottomEnd = 24.dp), ui.pink, ui.onPink),
            )
            LazyRow(contentPadding = PaddingValues(horizontal = 48.dp, vertical = 20.dp), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                itemsIndexed(shown, key = { _, a -> a.profile }) { i, a ->
                    val active = i == 0
                    val name = (if (active) activeName else null) ?: a.name ?: stringResource(R.string.spotify_account)
                    val premium = if (active) livePlan ?: a.premium else a.premium
                    val plan = when (premium) { true -> stringResource(R.string.accounts_plan_premium); false -> stringResource(R.string.accounts_plan_free); null -> null }
                    val (shape, bg, fg) = looks[i % looks.size]
                    AccountCard(name, plan, active, if (active) Modifier.focusRequester(first) else Modifier, avatar = {
                        Box(Modifier.size(if (active) 120.dp else 112.dp).clip(shape).background(bg), contentAlignment = Alignment.Center) {
                            TvText(name.take(1).uppercase(), fg, 52.sp, FontWeight.Black, family = com.music.spotui.ui.theme.FlexDisplay)
                        }
                    }) {
                        if (active) { TvYoureIn.done(context); onDone() }
                        else { TvYoureIn.done(context); SessionActions.switchAccount(Graph.app, a.profile) }
                    }
                }
                item(key = "add") {
                    AccountCard(stringResource(R.string.tv_add_account), stringResource(R.string.tv_add_account_detail), false, outlined = true, avatar = {
                        Box(Modifier.size(112.dp).clip(CircleShape).background(ui.surfaceContainerHigh), contentAlignment = Alignment.Center) {
                            Icon(DesignIcons.Plus, null, tint = ui.onSurface, modifier = Modifier.size(48.dp))
                        }
                    }) { SessionActions.addAccount(Graph.app) }
                }
            }
            Row(Modifier.padding(start = 48.dp, top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                if (!activeName.isNullOrBlank()) {
                    TvText(stringResource(R.string.tv_signed_in_as) + " ", ui.onSurfaceVariant, 18.sp, FontWeight.SemiBold)
                    TvText(activeName, ui.onSurface, 18.sp, FontWeight.Black)
                    Spacer(Modifier.width(16.dp))
                }
                TvButton(stringResource(R.string.tv_sign_out_question), null, ui.surfaceContainerHigh, ui.onSurface, height = 48.dp,
                    shape = RoundedCornerShape(24.dp)) { TvYoureIn.done(context); SessionActions.logout(context) }
            }
        }
    }
    LaunchedEffect(Unit) { runCatching { delay(200); first.requestFocus() } }
}

@Composable
private fun AccountCard(
    name: String,
    plan: String?,
    active: Boolean,
    modifier: Modifier = Modifier,
    outlined: Boolean = false,
    avatar: @Composable () -> Unit,
    onClick: () -> Unit,
) {
    val ui = tvScheme
    val shape = RoundedCornerShape(36.dp)
    Column(
        modifier.width(if (active) 203.dp else 190.dp).height(if (active) 246.dp else 230.dp)
            .tvFocus(shape, ui.surface, ui.coral, onClick = onClick).clip(shape)
            .background(if (active) ui.surfaceContainerHigh else ui.surfaceContainerLow)
            .then(if (outlined) Modifier.border(2.dp, ui.outlineVariant, shape) else Modifier)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        avatar()
        Spacer(Modifier.height(10.dp))
        TvText(name, ui.onSurface, 20.sp, FontWeight.Black)
        if (plan != null) {
            if (outlined) TvText(plan, ui.onSurfaceVariant, 16.sp, FontWeight.ExtraBold, Modifier.padding(top = 6.dp))
            else Box(Modifier.padding(top = 8.dp).clip(RoundedCornerShape(14.dp)).background(if (active) ui.surface.copy(alpha = 0.4f) else ui.surfaceContainerHigh)
                .padding(horizontal = 12.dp, vertical = 4.dp)) { TvText(plan, ui.onSurfaceVariant, 16.sp, FontWeight.ExtraBold) }
        }
    }
}
