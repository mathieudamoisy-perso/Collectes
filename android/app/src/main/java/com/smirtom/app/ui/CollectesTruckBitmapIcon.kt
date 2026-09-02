package com.smirtom.app.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.smirtom.app.R
import kotlin.math.roundToInt

/** Crop of [R.drawable.ic_launcher_fg] (1024×1024) — colored app-icon truck. */
private val TruckSrcOffset = IntOffset(250, 350)
private val TruckSrcSize = IntSize(520, 320)
val TruckIconAspectRatio = TruckSrcSize.width.toFloat() / TruckSrcSize.height

val CollectesTruckIconWidth = 24.dp
val CollectesTruckIconHeight = CollectesTruckIconWidth / TruckIconAspectRatio

private fun extractColoredTruckBitmap(
    source: ImageBitmap,
    blackThreshold: Int = 48,
): ImageBitmap {
    val androidSource = source.asAndroidBitmap()
    val cropped = Bitmap.createBitmap(
        androidSource,
        TruckSrcOffset.x,
        TruckSrcOffset.y,
        TruckSrcSize.width,
        TruckSrcSize.height,
    )
    val transparent = cropped.copy(Bitmap.Config.ARGB_8888, true)
    val pixels = IntArray(transparent.width * transparent.height)
    transparent.getPixels(pixels, 0, transparent.width, 0, 0, transparent.width, transparent.height)
    for (index in pixels.indices) {
        val pixel = pixels[index]
        val red = (pixel shr 16) and 0xFF
        val green = (pixel shr 8) and 0xFF
        val blue = pixel and 0xFF
        if (red < blackThreshold && green < blackThreshold && blue < blackThreshold) {
            pixels[index] = 0
        }
    }
    transparent.setPixels(pixels, 0, transparent.width, 0, 0, transparent.width, transparent.height)
    return transparent.asImageBitmap()
}

@Composable
fun rememberColoredTruckBitmap(): ImageBitmap {
    val launcherForeground = ImageBitmap.imageResource(R.drawable.ic_launcher_fg)
    return remember(launcherForeground) {
        extractColoredTruckBitmap(launcherForeground)
    }
}

@Composable
fun CollectesTruckBitmapIcon(
    modifier: Modifier = Modifier,
) {
    val truckBitmap = rememberColoredTruckBitmap()

    Canvas(modifier = modifier) {
        drawImage(
            image = truckBitmap,
            dstOffset = IntOffset.Zero,
            dstSize = IntSize(
                width = size.width.roundToInt().coerceAtLeast(1),
                height = size.height.roundToInt().coerceAtLeast(1),
            ),
        )
    }
}
