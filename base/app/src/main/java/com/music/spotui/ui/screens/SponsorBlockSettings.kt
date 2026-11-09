package com.music.spotui.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.myspoti.engine.R
import com.music.spotui.data.preferences.SponsorBlockPref
import com.music.spotui.ui.theme.coral
import com.music.spotui.ui.theme.pink

/**
 * The Podcasts group of the settings, in the design: SponsorBlock as a card with its badge, then
 * "What to skip" with a coloured dot per kind of segment and a switch each, and where the
 * segments come from (see [com.music.spotui.podcast.SponsorSkip]).
 */
@Composable
internal fun SponsorBlockSettings() {
    val ctx = LocalContext.current
    val s = com.music.spotui.ui.theme.SystemColors.scheme
    Section(R.string.settings_section_podcasts)
    OptionRow(
        label = stringResource(R.string.sponsorblock_title),
        detail = stringResource(R.string.sponsorblock_detail),
        checked = SponsorBlockPref.on,
        icon = com.music.spotui.ui.dots.DesignIcons.Next, badge = 0,
        onChecked = { SponsorBlockPref.set(ctx, it) },
    )
    if (!SponsorBlockPref.on) return
    Text(
        stringResource(R.string.sponsorblock_what_to_skip), color = s.primary, fontFamily = com.music.spotui.ui.theme.FlexDisplay,
        fontSize = 16.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(start = 26.dp, end = 20.dp, top = 12.dp, bottom = 6.dp),
    )
    SegmentRow(s.coral, stringResource(R.string.sponsorblock_cat_sponsor), stringResource(R.string.sponsorblock_cat_sponsor_detail),
        SponsorBlockPref.sponsor) { SponsorBlockPref.setSponsor(ctx, it) }
    SegmentRow(Color(0xFFF2C94C), stringResource(R.string.sponsorblock_cat_selfpromo), stringResource(R.string.sponsorblock_cat_selfpromo_detail),
        SponsorBlockPref.selfPromo) { SponsorBlockPref.setSelfPromo(ctx, it) }
    SegmentRow(s.primary, stringResource(R.string.sponsorblock_cat_intro_outro), stringResource(R.string.sponsorblock_cat_intro_outro_detail),
        SponsorBlockPref.introOutro) { SponsorBlockPref.setIntroOutro(ctx, it) }
    SegmentRow(s.pink, stringResource(R.string.sponsorblock_cat_interaction), stringResource(R.string.sponsorblock_cat_interaction_detail),
        SponsorBlockPref.interaction) { SponsorBlockPref.setInteraction(ctx, it) }
    Text(
        stringResource(R.string.sponsorblock_source_note), color = s.onSurfaceVariant, fontSize = 12.sp,
        modifier = Modifier.padding(start = 26.dp, end = 20.dp, top = 8.dp, bottom = 4.dp),
    )
}

/** A kind of segment: its colour dot, its name and what it is, and the design's switch. */
@Composable
private fun SegmentRow(dot: Color, label: String, detail: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    val s = com.music.spotui.ui.theme.SystemColors.scheme
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().settingsCard()
            .toggleable(checked, role = Role.Switch, onValueChange = onChecked)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Box(Modifier.size(12.dp).clip(CircleShape).background(dot))
        Column(Modifier.weight(1f).padding(start = 14.dp, end = 10.dp)) {
            Text(label, color = s.onSurface, style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
            Text(detail, color = s.onSurfaceVariant, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
        DesignSwitch(checked)
    }
}
