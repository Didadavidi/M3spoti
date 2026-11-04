package com.music.spotui.data.preferences

/*
 * The app's look: Classic (today's) or Immersive, picked during the onboarding.
 */

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** The two looks. [CLASSIC] is the default, so nothing changes for anyone who has not chosen. */
enum class Look {
    CLASSIC, IMMERSIVE;

    companion object {
        fun of(name: String?): Look = entries.firstOrNull { it.name == name } ?: CLASSIC
    }
}

object LookPref {
    /** Pinned to CLASSIC: the Immersive look and its onboarding/switch UI are gone. */
    var look by mutableStateOf(Look.CLASSIC)
        private set

    fun set(c: Context, value: Look) {
        // No-op: only one look remains.
    }

    /** The look step of the onboarding is never shown now. */
    fun isChosen(c: Context): Boolean = true
    fun setChosen(c: Context) {}

    /** The new-look popup is never shown now. */
    fun isOffered(c: Context): Boolean = true
    fun setOffered(c: Context) {}
    fun clearOffered(c: Context) {}
}
