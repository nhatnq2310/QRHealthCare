package com.qrhealthcare.app.ui.components

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter

/**
 * Renders a QR code bitmap from a string value.
 *
 * Usage:
 *   QrCodeImage(
 *       value = "qrhealthcare://profile/QRH-A1B2",
 *       modifier = Modifier.size(200.dp)
 *   )
 */
@Composable
fun QrCodeImage(
    value: String,
    modifier: Modifier = Modifier,
    sizePx: Int = 512
) {
    val bitmap = remember(value) { generateQrBitmap(value, sizePx) }
    Image(
        bitmap = bitmap.asImageBitmap(),
        contentDescription = "QR Code",
        modifier = modifier,
        contentScale = ContentScale.Fit
    )
}

fun generateQrBitmap(content: String, size: Int = 512): Bitmap {
    val hints = hashMapOf<EncodeHintType, Any>(
        EncodeHintType.MARGIN to 1,
        EncodeHintType.CHARACTER_SET to "UTF-8"
    )
    val writer = QRCodeWriter()
    val matrix = writer.encode(content, BarcodeFormat.QR_CODE, size, size, hints)
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)
    for (x in 0 until size) {
        for (y in 0 until size) {
            bitmap.setPixel(x, y, if (matrix[x, y]) AndroidColor.BLACK else AndroidColor.WHITE)
        }
    }
    return bitmap
}

/** Formats a Long price value as Vietnamese currency: 125,000 ₫ */
fun formatVND(amount: Long): String = "${"%,d".format(amount).replace(',', '.')} ₫"
