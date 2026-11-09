package com.music.spotui.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.outlined.Person
import com.music.spotui.ui.theme.coral
import com.music.spotui.ui.theme.onCoral
import com.music.spotui.ui.theme.pink
import com.music.spotui.ui.theme.onPink
import com.music.spotui.ui.theme.soft
import com.music.spotui.ui.theme.onSoft
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.myspoti.engine.R
import app.myspoti.host.android.MyspotiRuntime
import app.myspoti.host.android.SavedAccount
import com.music.spotui.di.Graph
import com.music.spotui.di.SessionActions
import com.music.spotui.ui.immersive.Immersive
import com.music.spotui.ui.navigation.isImmersive
import com.music.spotui.ui.theme.AppAccent
import com.music.spotui.ui.theme.AppMuted
import com.music.spotui.ui.theme.AppText
import com.music.spotui.ui.theme.tone

/** The colours the account rows are drawn in: the Classic look's or the Immersive one's. */
private class AccountsLook(val text: Color, val muted: Color, val accent: Color, val destructive: Color, val dialog: Color)

@Composable
private fun accountsLook(): AccountsLook =
    if (isImmersive()) AccountsLook(Immersive.Text, Immersive.TextSecondary, Immersive.Accent, Immersive.Destructive, Color.Transparent)
    else com.music.spotui.ui.theme.SystemColors.scheme.let { s ->
        AccountsLook(s.onSurface, s.onSurfaceVariant, s.primary, s.error, s.surfaceContainerHigh)
    }

/**
 * The Spotify accounts kept on this phone, in both looks (Settings, and the sheet behind the
 * avatar): each with its name, picture and plan, the active one ticked. Another one is switched to
 * after a word that playback stops; "Add account" opens Spotify's sign-in in a new WebView profile;
 * Remove deletes an account's sign-in from the phone. A WebView without profiles keeps one account
 * at a time, and says so: adding signs this one out.
 */
@Composable
fun AccountsList(onLeave: () -> Unit = {}) {
    val look = accountsLook()
    val ctx = LocalContext.current
    val host = MyspotiRuntime.host
    val list by host.accounts.list.collectAsStateWithLifecycle()
    val livePlan by app.myspoti.spotify.AccountPlan.premium.collectAsStateWithLifecycle()
    var switchTo by remember { mutableStateOf<SavedAccount?>(null) }
    var remove by remember { mutableStateOf<SavedAccount?>(null) }
    var addSingle by remember { mutableStateOf(false) }
    // The account in use first, then the one used last; one not signed in yet (an "Add account" left) last.
    val shown = remember(list) {
        val active = list.current ?: SavedAccount(profile = list.active)
        listOf(active) + list.accounts.filter { it.profile != list.active }.sortedWith(compareBy<SavedAccount> { !it.known }.thenByDescending { it.usedAt })
    }
    val s = com.music.spotui.ui.theme.SystemColors.scheme
    // One colour per account, in the design's order (the active one in the primary container).
    val badges = listOf(s.primaryContainer to s.onPrimaryContainer, s.coral to s.onCoral, s.pink to s.onPink, s.soft to s.onSoft)
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        shown.forEachIndexed { i, account ->
            val active = account.profile == list.active
            // The account in use: its name and picture as read now, before they are saved.
            val name = (if (active) com.music.spotui.data.api.ProfileCache.name else null) ?: account.name
            val image = (if (active) com.music.spotui.data.api.ProfileCache.imageUrl else null) ?: account.imageUrl
            val premium = if (active) livePlan ?: account.premium else account.premium
            val shownName = name?.takeIf { it.isNotBlank() } ?: stringResource(if (account.known || active) R.string.spotify_account else R.string.accounts_not_signed_in)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(if (i == 0) 24.dp else 18.dp))
                    .background(if (active) s.primaryContainer.copy(alpha = 0.55f) else s.surfaceContainer)
                    .clickable(enabled = !active, role = Role.Button) { switchTo = account }
                    .padding(start = 12.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
            ) {
                val (bg, fg) = badges[i % badges.size]
                Box(Modifier.size(48.dp).clip(RoundedCornerShape(16.dp)).background(bg), contentAlignment = Alignment.Center) {
                    if (image != null) ProfileAvatar(url = image, name = shownName, size = 48.dp)
                    else Text(shownName.take(1).uppercase(), color = fg, fontSize = 20.sp, fontWeight = FontWeight.Black)
                }
                Column(Modifier.weight(1f).padding(start = 14.dp)) {
                    Text(shownName, color = s.onSurface, style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (active) Text(stringResource(R.string.accounts_in_use), color = s.onSurfaceVariant, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
                val plan = when (premium) {
                    true -> stringResource(R.string.accounts_plan_premium)
                    false -> stringResource(R.string.accounts_plan_free)
                    null -> null
                }
                if (plan != null) Text(
                    plan, color = s.onSurface, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(horizontal = 6.dp).clip(RoundedCornerShape(50))
                        .background(if (premium == true) s.surface else s.surfaceContainerHigh).padding(horizontal = 10.dp, vertical = 5.dp),
                )
                if (active) Box(Modifier.padding(horizontal = 6.dp).size(32.dp).clip(CircleShape).background(s.primary), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Check, contentDescription = stringResource(R.string.accounts_in_use), tint = s.onPrimary, modifier = Modifier.size(18.dp))
                }
                // The account in use is removed by "Log out"; the others from here.
                else IconButton(onClick = { remove = account }) {
                    Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.accounts_remove), tint = s.onSurfaceVariant, modifier = Modifier.size(20.dp))
                }
            }
        }
        Box(Modifier.fillMaxWidth().padding(vertical = 8.dp).height(1.dp).background(s.outlineVariant))
        AccountAction(Icons.Filled.Add, stringResource(R.string.accounts_add),
            stringResource(if (host.multiProfile) R.string.accounts_add_detail else R.string.accounts_add_single_detail), s.primaryContainer, s.onPrimaryContainer) {
            if (host.multiProfile) { onLeave(); SessionActions.addAccount(Graph.app) } else addSingle = true
        }
        AccountAction(Icons.Outlined.Person, stringResource(R.string.account_page_title), stringResource(R.string.account_page_detail), s.surfaceContainerHigh, s.onSurface) {
            onLeave()
            com.music.spotui.ui.screens.AccountBrowser.open(Graph.app, com.music.spotui.ui.screens.AccountBrowser.ACCOUNT_URL)
        }
        Text(
            stringResource(if (host.multiProfile) R.string.accounts_footer else R.string.accounts_single_footer),
            color = s.onSurfaceVariant, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp),
        )
    }
    switchTo?.let { account ->
        val name = account.name?.takeIf { it.isNotBlank() } ?: stringResource(R.string.spotify_account)
        LookAlertDialog(
            onDismissRequest = { switchTo = null },
            containerColor = look.dialog,
            title = { Text(stringResource(R.string.accounts_switch_title, name), color = look.text) },
            text = { Text(stringResource(R.string.accounts_switch_text), color = look.muted) },
            confirmButton = {
                TextButton(onClick = { switchTo = null; onLeave(); SessionActions.switchAccount(Graph.app, account.profile) }) {
                    Text(stringResource(R.string.accounts_switch_confirm), color = look.accent)
                }
            },
            dismissButton = { TextButton(onClick = { switchTo = null }) { Text(stringResource(R.string.add_to_playlist_button_cancel), color = look.text) } },
        )
    }
    remove?.let { account ->
        val name = account.name?.takeIf { it.isNotBlank() } ?: stringResource(if (account.known) R.string.spotify_account else R.string.accounts_not_signed_in)
        LookAlertDialog(
            onDismissRequest = { remove = null },
            containerColor = look.dialog,
            title = { Text(stringResource(R.string.accounts_remove_title, name), color = look.text) },
            text = { Text(stringResource(R.string.accounts_remove_text), color = look.muted) },
            confirmButton = {
                TextButton(onClick = { remove = null; SessionActions.removeAccount(ctx, account.profile) }) {
                    Text(stringResource(R.string.accounts_remove), color = look.destructive)
                }
            },
            dismissButton = { TextButton(onClick = { remove = null }) { Text(stringResource(R.string.add_to_playlist_button_cancel), color = look.text) } },
        )
    }
    if (addSingle) LookAlertDialog(
        onDismissRequest = { addSingle = false },
        containerColor = look.dialog,
        title = { Text(stringResource(R.string.accounts_add), color = look.text) },
        text = { Text(stringResource(R.string.accounts_single_footer), color = look.muted) },
        confirmButton = {
            TextButton(onClick = { addSingle = false; onLeave(); SessionActions.addAccount(Graph.app) }) {
                Text(stringResource(R.string.connect_device_menu_logout), color = look.accent)
            }
        },
        dismissButton = { TextButton(onClick = { addSingle = false }) { Text(stringResource(R.string.add_to_playlist_button_cancel), color = look.text) } },
    )
}

/** The detail of the "Switch account" row behind the avatar: the other accounts kept, by name. */
@Composable
fun switchAccountDetail(): String {
    val list by MyspotiRuntime.host.accounts.list.collectAsStateWithLifecycle()
    return list.others.mapNotNull { it.name?.takeIf { n -> n.isNotBlank() } }.joinToString(", ")
        .ifEmpty { stringResource(R.string.accounts_switch_row_detail) }
}

/** "Add another account", "Manage your account": a round tinted badge, the label and what it does. */
@Composable
private fun AccountAction(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, detail: String, bg: Color, fg: Color, onClick: () -> Unit) {
    val s = com.music.spotui.ui.theme.SystemColors.scheme
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).clickable(role = Role.Button, onClick = onClick).padding(horizontal = 4.dp, vertical = 8.dp),
    ) {
        Box(Modifier.size(44.dp).clip(CircleShape).background(bg), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(22.dp))
        }
        Column(Modifier.padding(start = 14.dp)) {
            Text(label, color = s.onSurface, style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
            Text(detail, color = s.onSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

/** [AccountsList] in the design's sheet: "Accounts" big, a round close button, the cards. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountsSheet(onDismiss: () -> Unit, onLeave: () -> Unit = onDismiss) {
    val s = com.music.spotui.ui.theme.SystemColors.scheme
    LookSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = s.surface) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(bottom = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 16.dp, bottom = 12.dp)) {
                Text(stringResource(R.string.accounts_title), color = s.onSurface, modifier = Modifier.weight(1f),
                    fontFamily = com.music.spotui.ui.theme.FlexDisplay, fontWeight = FontWeight.Black, fontSize = 28.sp)
                Box(Modifier.size(44.dp).clip(CircleShape).background(s.surfaceContainerHigh).clickable(onClick = onDismiss), contentAlignment = Alignment.Center) {
                    Icon(com.music.spotui.ui.dots.DesignIcons.Close, stringResource(R.string.bottom_actions_close), tint = s.onSurface, modifier = Modifier.size(20.dp))
                }
            }
            // Switching or adding leaves the screens: the sheet goes, and whatever opened it.
            AccountsList(onLeave = onLeave)
        }
    }
}
