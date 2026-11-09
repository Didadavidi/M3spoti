package com.music.spotui.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Edit
import com.music.spotui.ui.theme.deep
import com.music.spotui.ui.theme.onDeep
import com.music.spotui.ui.theme.coral
import com.music.spotui.ui.theme.onCoral
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import app.myspoti.engine.R
import app.myspoti.host.android.Logbook
import app.myspoti.host.android.MyspotiRuntime
import com.music.spotui.ui.screens.shareDiagnosticReport
import com.music.spotui.ui.theme.AppAccent
import com.music.spotui.ui.theme.AppSans
import com.music.spotui.ui.theme.AppText

/**
 * A failed sign-in had no way to a diagnostic report: the export lives in the settings, which
 * only a signed-in app reaches (the reported "Oops! Something went wrong" and e-mail loops came
 * with no log at all). What the page shows is noted first, then the usual share sheet.
 */
fun sendLoginReport(context: android.content.Context) {
    MyspotiRuntime.host.noteLoginPage {
        val file = runCatching { Logbook.export(context) }.getOrNull()
        if (file != null) shareDiagnosticReport(context, file)
        else android.widget.Toast.makeText(context, context.getString(R.string.diagnostic_export_failed), android.widget.Toast.LENGTH_SHORT).show()
    }
}

/**
 * "Help" over Spotify's sign-in page. The page is a WebView above the app's screens, so this is a
 * popup (a window of its own, above it), small, in the top corner Spotify's page leaves empty, and
 * not focusable: the keyboard and the form keep working under it.
 */
@Composable
fun LoginHelpButton() {
    var open by remember { mutableStateOf(false) }
    val top = WindowInsets.statusBars.getTop(LocalDensity.current)
    val gap = with(LocalDensity.current) { 12.dp.roundToPx() }
    Popup(alignment = Alignment.TopEnd, offset = IntOffset(-gap, top + gap)) {
        Text(
            stringResource(R.string.login_help_button),
            color = com.music.spotui.ui.theme.SystemColors.scheme.onSurface, fontFamily = AppSans, fontWeight = FontWeight.Bold, fontSize = 13.sp,
            modifier = Modifier.clip(RoundedCornerShape(50)).background(com.music.spotui.ui.theme.SystemColors.scheme.surfaceContainerHigh)
                .clickable { open = true }.padding(horizontal = 14.dp, vertical = 7.dp),
        )
    }
    if (open) LoginHelpDialog(onClose = { open = false })
}

/**
 * The design's "Didn't get the code?" sheet: a deep card with three things to check, then the two
 * ways on: start the sign-in over (Spotify sends a new code) or send a diagnostic report.
 */
@Composable
fun LoginHelpDialog(onClose: () -> Unit) {
    val ctx = LocalContext.current
    val s = com.music.spotui.ui.theme.SystemColors.scheme
    val deep = s.deep()
    val onDeep = s.onDeep()
    androidx.compose.ui.window.Dialog(onDismissRequest = onClose) {
        androidx.compose.foundation.layout.Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(32.dp)).background(deep).padding(22.dp),
        ) {
            androidx.compose.foundation.layout.Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.login_help_code_title), color = onDeep, modifier = Modifier.weight(1f),
                    fontFamily = com.music.spotui.ui.theme.FlexDisplay, fontWeight = FontWeight.Black, fontSize = 22.sp,
                )
                androidx.compose.foundation.layout.Box(
                    Modifier.size(40.dp).clip(CircleShape).background(onDeep.copy(alpha = 0.12f)).clickable(onClick = onClose),
                    contentAlignment = Alignment.Center,
                ) { androidx.compose.material3.Icon(com.music.spotui.ui.dots.DesignIcons.Close, stringResource(R.string.bottom_actions_close), tint = onDeep, modifier = Modifier.size(18.dp)) }
            }
            androidx.compose.foundation.layout.Spacer(Modifier.height(14.dp))
            listOf(
                androidx.compose.material.icons.Icons.Outlined.Email to R.string.login_tip_spam,
                androidx.compose.material.icons.Icons.Outlined.Schedule to R.string.login_tip_expire,
                androidx.compose.material.icons.Icons.Outlined.Edit to R.string.login_tip_spelling,
            ).forEach { (icon, text) ->
                androidx.compose.foundation.layout.Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 6.dp)) {
                    androidx.compose.foundation.layout.Box(
                        Modifier.size(34.dp).clip(RoundedCornerShape(12.dp)).background(s.primaryContainer),
                        contentAlignment = Alignment.Center,
                    ) { androidx.compose.material3.Icon(icon, null, tint = s.onPrimaryContainer, modifier = Modifier.size(18.dp)) }
                    Text(stringResource(text), color = onDeep, fontFamily = AppSans, fontWeight = FontWeight.SemiBold, fontSize = 15.sp,
                        modifier = Modifier.padding(start = 12.dp))
                }
            }
            Text(stringResource(R.string.login_help_detail), color = onDeep.copy(alpha = 0.7f), fontFamily = AppSans, fontSize = 13.sp,
                modifier = Modifier.padding(top = 10.dp))
            androidx.compose.foundation.layout.Row(
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth().padding(top = 18.dp),
            ) {
                // A half-finished flow is what Spotify answers "Oops! Something went wrong" to: from nothing.
                Text(
                    stringResource(R.string.login_help_restart), color = s.onCoral, fontFamily = AppSans, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.weight(1f).clip(RoundedCornerShape(50)).background(s.coral)
                        .clickable { onClose(); MyspotiRuntime.host.retryLogin() }.padding(vertical = 14.dp),
                )
                Text(
                    stringResource(R.string.login_help_send), color = onDeep, fontFamily = AppSans, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.weight(1f).clip(RoundedCornerShape(50)).border(2.dp, onDeep, RoundedCornerShape(50))
                        .clickable { onClose(); sendLoginReport(ctx) }.padding(vertical = 12.dp),
                )
            }
        }
    }
}

/** Under a wait that lasts (the loading screen after the sign-in), the same way to a report. */
@Composable
fun LoginReportLink(modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    Text(
        stringResource(R.string.login_help_slow),
        color = AppText.copy(alpha = 0.6f), fontFamily = AppSans, fontSize = 14.sp,
        modifier = modifier.clip(RoundedCornerShape(50)).clickable { sendLoginReport(ctx) }.padding(horizontal = 14.dp, vertical = 10.dp),
    )
}
