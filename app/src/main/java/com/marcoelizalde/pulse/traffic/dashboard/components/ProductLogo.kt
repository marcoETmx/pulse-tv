package com.marcoelizalde.pulse.traffic.dashboard.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import com.marcoelizalde.pulse.traffic.dashboard.monogram
import com.marcoelizalde.pulse.ui.theme.PulseColors
import com.marcoelizalde.pulse.ui.theme.PulseType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val LogoShape = RoundedCornerShape(8.dp)

// Logos sit on a light tile so dark or transparent marks stay visible on the dark card.
private val LogoTile = Color(0xF2F2F3F7)

/** The product's logo on a small tile, or its monogram while there is none. */
@Composable
fun ProductLogo(name: String, image: ByteArray?, modifier: Modifier = Modifier) {
    val bitmap by produceState<ImageBitmap?>(initialValue = null, image) {
        value = image?.let { bytes -> withContext(Dispatchers.Default) { decodeLogo(bytes) } }
    }
    val logo = bitmap
    Box(
        modifier
            .size(30.dp)
            .clip(LogoShape)
            .background(if (logo != null) LogoTile else PulseColors.SurfaceRaised),
        contentAlignment = Alignment.Center,
    ) {
        if (logo != null) {
            Image(logo, contentDescription = null, modifier = Modifier.fillMaxSize().padding(3.dp), contentScale = ContentScale.Fit)
        } else {
            Text(
                monogram(name),
                style = PulseType.caption.copy(fontWeight = FontWeight.SemiBold, fontSize = 15.sp),
                color = PulseColors.TextSecondary,
            )
        }
    }
}

/** Decodes at roughly the tile's pixel size so 512px icons do not waste the TV's memory. */
private fun decodeLogo(bytes: ByteArray, targetPx: Int = 96): ImageBitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
    var sampleSize = 1
    while (bounds.outWidth / (sampleSize * 2) >= targetPx && bounds.outHeight / (sampleSize * 2) >= targetPx) sampleSize *= 2
    val options = BitmapFactory.Options().apply { inSampleSize = sampleSize }
    return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)?.asImageBitmap()
}
