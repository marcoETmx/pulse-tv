package com.marcoelizalde.pulse.traffic.dashboard.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Border
import androidx.tv.material3.Glow
import androidx.tv.material3.SelectableSurfaceDefaults
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.marcoelizalde.pulse.traffic.dashboard.DashboardUiState
import com.marcoelizalde.pulse.traffic.dashboard.LocalFormats
import com.marcoelizalde.pulse.traffic.domain.Period
import com.marcoelizalde.pulse.ui.theme.PulseColors
import com.marcoelizalde.pulse.ui.theme.PulseType
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun DashboardHeader(
    state: DashboardUiState,
    onSelect: (Period) -> Unit,
    selectedPeriodFocus: FocusRequester,
    onOpenMenu: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        PulseGlyph(Modifier.size(width = 36.dp, height = 26.dp))
        Spacer(Modifier.width(12.dp))
        // The date range of the data on screen (dimmed while another period loads), with its status below.
        Column(Modifier.weight(1f)) {
            state.dashboard?.window?.let { window ->
                // Shrinks to fit (a range across years is long) rather than push the period selector.
                BasicText(
                    LocalFormats.current.range(window),
                    modifier = Modifier.fillMaxWidth(),
                    style = PulseType.clock.copy(
                        color = if (state.isShowingStalePeriod) PulseColors.TextMuted else PulseColors.TextPrimary,
                    ),
                    maxLines = 1,
                    autoSize = TextAutoSize.StepBased(minFontSize = 16.sp, maxFontSize = PulseType.clock.fontSize),
                )
            }
            StatusBar(state)
        }
        Spacer(Modifier.width(16.dp))
        PeriodSelector(state.selectedPeriod, onSelect, selectedPeriodFocus)
        Spacer(Modifier.width(32.dp))
        Clock()
        Spacer(Modifier.width(16.dp))
        MenuButton(onClick = onOpenMenu)
    }
}

@Composable
fun PeriodSelector(selected: Period, onSelect: (Period) -> Unit, selectedPeriodFocus: FocusRequester) {
    val pill = RoundedCornerShape(50)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Period.entries.forEach { period ->
            val isSelected = period == selected
            Surface(
                selected = isSelected,
                onClick = { onSelect(period) },
                modifier = if (isSelected) Modifier.focusRequester(selectedPeriodFocus) else Modifier,
                shape = SelectableSurfaceDefaults.shape(pill),
                colors = SelectableSurfaceDefaults.colors(
                    containerColor = Color.Transparent,
                    contentColor = PulseColors.TextSecondary,
                    focusedContainerColor = PulseColors.TextPrimary,
                    focusedContentColor = PulseColors.Ink,
                    selectedContainerColor = PulseColors.Series.copy(alpha = 0.22f),
                    selectedContentColor = PulseColors.TextPrimary,
                    focusedSelectedContainerColor = PulseColors.TextPrimary,
                    focusedSelectedContentColor = PulseColors.Ink,
                ),
                scale = SelectableSurfaceDefaults.scale(focusedScale = 1.08f),
                border = SelectableSurfaceDefaults.border(
                    border = Border(BorderStroke(1.dp, PulseColors.Hairline), shape = pill),
                    focusedBorder = Border.None,
                    selectedBorder = Border(BorderStroke(1.dp, PulseColors.Series.copy(alpha = 0.7f)), shape = pill),
                    focusedSelectedBorder = Border.None,
                ),
                glow = SelectableSurfaceDefaults.glow(
                    focusedGlow = Glow(PulseColors.BrandTeal.copy(alpha = 0.55f), 10.dp),
                ),
            ) {
                Text(
                    LocalFormats.current.strings.period(period),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    style = PulseType.pill,
                )
            }
        }
    }
}

@Composable
private fun Clock() {
    val now by produceState(LocalDateTime.now()) {
        while (true) {
            value = LocalDateTime.now()
            delay(60_000 - System.currentTimeMillis() % 60_000)
        }
    }
    Column(horizontalAlignment = Alignment.End) {
        Text(ClockTime.format(now), style = PulseType.clock)
        Text(ClockDate.format(now), style = PulseType.caption, color = PulseColors.TextMuted)
    }
}

private val ClockTime = DateTimeFormatter.ofPattern("HH:mm", Locale.US)
private val ClockDate = DateTimeFormatter.ofPattern("EEE, MMM d", Locale.US)
