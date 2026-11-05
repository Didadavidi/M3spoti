package com.music.spotui.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.myspoti.engine.R
import com.music.spotui.data.preferences.LinkHandling
import com.music.spotui.data.preferences.rememberLinksEnabled
import com.music.spotui.ui.theme.SystemColors

/**
 * Asks, once per launch, to let Android open Spotify links in this app while that is not allowed
 * (Android 12+ never verifies Spotify's domains for it). "Allow" opens the app's "Open by default"
 * page; "Don't ask again" is remembered; the Settings row stays to turn it on later.
 */
@Composable
fun LinksPrompt() {
    val on = rememberLinksEnabled()
    val context = LocalContext.current
    var ask by remember { mutableStateOf(!LinkHandling.promptedThisRun && !LinkHandling.neverAsk(context)) }
    if (!ask || on) return
    val s = SystemColors.scheme
    val close = { ask = false; LinkHandling.promptedThisRun = true }
    AlertDialog(
        onDismissRequest = close,
        containerColor = s.surfaceContainerHigh,
        shape = RoundedCornerShape(32.dp),
        icon = { Icon(Icons.Outlined.Link, null, tint = s.primary) },
        title = { Text(stringResource(R.string.links_prompt_title), color = s.onSurface, style = MaterialTheme.typography.headlineSmall) },
        text = { Text(stringResource(R.string.links_prompt_text), color = s.onSurfaceVariant, fontSize = 15.sp) },
        confirmButton = {
            Button(
                onClick = { close(); LinkHandling.openSettings(context) },
                colors = ButtonDefaults.buttonColors(containerColor = s.primary, contentColor = s.onPrimary),
            ) { Text(stringResource(R.string.links_prompt_allow), fontWeight = FontWeight.ExtraBold) }
        },
        dismissButton = {
            Row {
                TextButton(onClick = { close(); LinkHandling.setNeverAsk(context) }) {
                    Text(stringResource(R.string.links_prompt_never), color = s.onSurfaceVariant)
                }
                TextButton(onClick = close) { Text(stringResource(R.string.links_prompt_later), color = s.primary) }
            }
        },
    )
}
