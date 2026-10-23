package com.marcoelizalde.pulse.traffic.dashboard.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import com.marcoelizalde.pulse.ui.theme.PulseColors

/**
 * Soft glows behind the dashboard. Static on purpose: full-screen gradients redrawn every frame
 * drop a Fire TV Stick Lite to ~22 fps, so the layer is painted once and reused as a texture.
 */
@Composable
fun AuroraBackground(modifier: Modifier = Modifier) {
    Canvas(modifier.fillMaxSize().graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }) {
        drawRect(PulseColors.Ink)
        val w = size.width
        val h = size.height
        glow(Offset(w * 0.14f, h * 0.06f), w * 0.42f, PulseColors.BrandTeal.copy(alpha = 0.15f))
        glow(Offset(w * 0.88f, h * 0.24f), w * 0.38f, PulseColors.BrandViolet.copy(alpha = 0.14f))
        glow(Offset(w * 0.55f, h * 1.04f), w * 0.48f, PulseColors.BrandBlue.copy(alpha = 0.13f))
    }
}

private fun DrawScope.glow(center: Offset, radius: Float, color: Color) {
    drawCircle(Brush.radialGradient(listOf(color, Color.Transparent), center, radius), radius, center)
}
