package com.music.spotui.data.preferences

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

/**
 * Whether Android opens Spotify links in this app. Since Android 12 a web link opens in an app only
 * if the app is verified for the domain (Spotify's own app, never this one) or the user allowed it
 * under the app's "Open by default" settings; until then links open in the browser. This reads that
 * choice and opens the page where it is made.
 */
object LinkHandling {
    private const val PREF = "settings_prefs"
    private const val KEY_NEVER = "links_prompt_never"

    /** The address that matters: shared links are open.spotify.com links. */
    private const val MAIN_HOST = "open.spotify.com"

    /** Prompted already in this run of the app (once per launch at most). */
    var promptedThisRun = false

    fun enabled(c: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        val m = c.getSystemService(android.content.pm.verify.domain.DomainVerificationManager::class.java) ?: return true
        val state = runCatching { m.getDomainVerificationUserState(c.packageName) }.getOrNull() ?: return true
        if (!state.isLinkHandlingAllowed) return false
        val host = state.hostToStateMap[MAIN_HOST] ?: return false
        return host != android.content.pm.verify.domain.DomainVerificationUserState.DOMAIN_STATE_NONE
    }

    /** The app's "Open by default" page (Android 12+), else its app info page. */
    fun openSettings(c: Context) {
        val pkg = android.net.Uri.parse("package:" + c.packageName)
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            Intent(android.provider.Settings.ACTION_APP_OPEN_BY_DEFAULT_SETTINGS, pkg)
        else Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS, pkg)
        runCatching { c.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }.onFailure {
            runCatching {
                c.startActivity(Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS, pkg).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
        }
    }

    fun neverAsk(c: Context): Boolean = c.getSharedPreferences(PREF, Context.MODE_PRIVATE).getBoolean(KEY_NEVER, false)
    fun setNeverAsk(c: Context) = c.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().putBoolean(KEY_NEVER, true).apply()
}

/** [LinkHandling.enabled], read again each time the app comes back (from the settings page). */
@Composable
fun rememberLinksEnabled(): Boolean {
    val context = LocalContext.current
    val state = remember { mutableStateOf(LinkHandling.enabled(context)) }
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_RESUME) state.value = LinkHandling.enabled(context) }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    return state.value
}
