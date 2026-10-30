package com.marcoelizalde.pulse.traffic.dashboard.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Glow
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.marcoelizalde.pulse.BuildConfig
import com.marcoelizalde.pulse.traffic.dashboard.DashboardUiState
import com.marcoelizalde.pulse.traffic.dashboard.Formats
import com.marcoelizalde.pulse.traffic.dashboard.LocalFormats
import com.marcoelizalde.pulse.traffic.dashboard.i18n.PulseStrings
import com.marcoelizalde.pulse.traffic.dashboard.RankingOrder
import com.marcoelizalde.pulse.traffic.dashboard.RefreshInterval
import com.marcoelizalde.pulse.ui.theme.PulseColors
import com.marcoelizalde.pulse.ui.theme.PulseType
import kotlin.time.toJavaDuration

private val PanelWidth = 320.dp
private val OptionHeight = 34.dp
private val OptionShape = RoundedCornerShape(10.dp)
private val Scrim = PulseColors.Ink.copy(alpha = 0.6f)

/** A press of the remote's ☰ button; holding it repeats the press, so only the first one counts. */
internal fun KeyEvent.isMenuPress(): Boolean =
    key == Key.Menu && type == KeyEventType.KeyDown && nativeKeyEvent.repeatCount == 0

/**
 * Side panel opened with the remote's ☰ button or the header's menu button. It lives in its own dialog
 * window, so the D-pad cannot leave it and Back closes it; ☰ closes it too. Focus starts on the current
 * refresh interval, and goes back to where it was when the menu closes. Periods stay on the header pills.
 */
@Composable
fun DashboardMenu(
    state: DashboardUiState,
    onSelectRefreshInterval: (RefreshInterval) -> Unit,
    onSelectRankingOrder: (RankingOrder) -> Unit,
    onRefreshNow: () -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        // The scrim below dims the dashboard, so the window's own dim would darken it twice.
        val window = (LocalView.current.parent as? DialogWindowProvider)?.window
        SideEffect { window?.setDimAmount(0f) }
        val shown = remember { MutableTransitionState(false) }.apply { targetState = true }
        Box(
            Modifier
                .fillMaxSize()
                .background(Scrim)
                .onPreviewKeyEvent { event -> event.isMenuPress().also { if (it) onDismiss() } },
            contentAlignment = Alignment.CenterEnd,
        ) {
            AnimatedVisibility(visibleState = shown, enter = slideInHorizontally { it } + fadeIn()) {
                MenuPanel(state, onSelectRefreshInterval, onSelectRankingOrder, onRefreshNow)
            }
        }
    }
}

@Composable
private fun MenuPanel(
    state: DashboardUiState,
    onSelectRefreshInterval: (RefreshInterval) -> Unit,
    onSelectRankingOrder: (RankingOrder) -> Unit,
    onRefreshNow: () -> Unit,
) {
    val formats = LocalFormats.current
    val strings = formats.strings
    val currentInterval = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { currentInterval.requestFocus() } }

    // The panel reaches the screen's edge, so its content keeps the dashboard's 5% overscan-safe margins there.
    Column(
        Modifier
            .fillMaxHeight()
            .width(PanelWidth)
            .background(PulseColors.Surface)
            .padding(start = 20.dp, end = 48.dp, top = 27.dp, bottom = 27.dp),
    ) {
        Text(strings.menu, style = PulseType.title)
        Spacer(Modifier.height(14.dp))
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            MenuSection(strings.autoRefresh)
            RefreshInterval.entries.forEach { interval ->
                val current = interval == state.refreshInterval
                MenuOption(
                    strings.refreshInterval(interval.minutes),
                    checked = current,
                    onClick = { onSelectRefreshInterval(interval) },
                    modifier = if (current) Modifier.focusRequester(currentInterval) else Modifier,
                )
            }
            Spacer(Modifier.height(14.dp))
            MenuSection(strings.topProducts)
            RankingOrder.entries.forEach { order ->
                MenuOption(
                    strings.rankingOrder(order),
                    checked = order == state.rankingOrder,
                    onClick = { onSelectRankingOrder(order) },
                )
            }
            Spacer(Modifier.height(10.dp))
            Box(Modifier.fillMaxWidth().height(1.dp).background(PulseColors.Hairline))
            Spacer(Modifier.height(10.dp))
            MenuOption(
                if (state.isLoading) strings.refreshing else strings.refreshNow,
                checked = false,
                onClick = onRefreshNow,
                enabled = !state.isLoading,
            )
        }
        Spacer(Modifier.height(14.dp))
        Text(menuSource(state.isDemo, BuildConfig.VERSION_NAME, strings), style = PulseType.caption, color = PulseColors.TextMuted)
        Text(menuStatus(state, formats), style = PulseType.caption, color = PulseColors.TextMuted)
    }
}

@Composable
private fun MenuSection(title: String) {
    Overline(title, Modifier.padding(start = 12.dp, bottom = 4.dp))
}

@Composable
private fun MenuOption(
    label: String,
    checked: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(OptionHeight),
        enabled = enabled,
        shape = ClickableSurfaceDefaults.shape(OptionShape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.Transparent,
            contentColor = if (checked) PulseColors.TextPrimary else PulseColors.TextSecondary,
            focusedContainerColor = PulseColors.TextPrimary,
            focusedContentColor = PulseColors.Ink,
            disabledContainerColor = Color.Transparent,
            disabledContentColor = PulseColors.TextMuted,
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
    ) {
        Row(Modifier.fillMaxSize().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(label, modifier = Modifier.weight(1f), style = PulseType.pill, maxLines = 1)
            // The check mark carries the current choice, not the brighter text alone.
            if (checked) CheckGlyph(Modifier.size(14.dp))
        }
    }
}

/** The header's way into the menu, for remotes without a ☰ button (Google TV's has none). */
@Composable
fun MenuButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val label = LocalFormats.current.strings.menu
    Surface(
        onClick = onClick,
        modifier = modifier.size(40.dp).semantics { contentDescription = label },
        shape = ClickableSurfaceDefaults.shape(CircleShape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.Transparent,
            contentColor = PulseColors.TextSecondary,
            focusedContainerColor = PulseColors.TextPrimary,
            focusedContentColor = PulseColors.Ink,
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.08f),
        border = ClickableSurfaceDefaults.border(
            border = Border(BorderStroke(1.dp, PulseColors.Hairline), shape = CircleShape),
            focusedBorder = Border.None,
        ),
        glow = ClickableSurfaceDefaults.glow(focusedGlow = Glow(PulseColors.BrandTeal.copy(alpha = 0.55f), 10.dp)),
    ) {
        MenuGlyph(Modifier.align(Alignment.Center).size(width = 18.dp, height = 14.dp))
    }
}

/** Three lines in the content color, so the button reads as "menu" without relying on a font glyph. */
@Composable
private fun MenuGlyph(modifier: Modifier = Modifier) {
    val color = LocalContentColor.current
    Canvas(modifier) {
        val stroke = size.height * 0.16f
        listOf(0.12f, 0.5f, 0.88f).forEach { y ->
            drawLine(color, Offset(stroke / 2, size.height * y), Offset(size.width - stroke / 2, size.height * y), stroke, StrokeCap.Round)
        }
    }
}

@Composable
private fun CheckGlyph(modifier: Modifier = Modifier) {
    val color = LocalContentColor.current
    Canvas(modifier) {
        val check = Path().apply {
            moveTo(size.width * 0.12f, size.height * 0.55f)
            lineTo(size.width * 0.40f, size.height * 0.82f)
            lineTo(size.width * 0.88f, size.height * 0.20f)
        }
        drawPath(check, color, style = Stroke(width = size.width * 0.16f, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

/** The footer's second line: when the data on screen was loaded, and what happens next. */
internal fun menuStatus(state: DashboardUiState, formats: Formats): String {
    val strings = formats.strings
    val dashboard = state.dashboard ?: return strings.notLoadedYet
    val updated = strings.updated(formats.time(dashboard.updatedAt))
    val next = when {
        state.isLoading -> strings.statusRefreshing
        state.error != null -> strings.statusRetrying
        else -> strings.statusNext(formats.time(dashboard.updatedAt + state.refreshInterval.duration.toJavaDuration()))
    }
    return "$updated · $next"
}

internal fun menuSource(isDemo: Boolean, version: String, strings: PulseStrings): String =
    "${if (isDemo) strings.demoData else "DataFast"} · v$version"
