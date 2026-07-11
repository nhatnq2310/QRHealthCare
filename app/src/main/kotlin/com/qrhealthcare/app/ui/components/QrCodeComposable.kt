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

/**
 * Admin-only: saves a generated QR bitmap to the device's Pictures gallery so
 * it can be sent to the physical-production vendor. Regular users never call
 * this — the app deliberately doesn't render QR images to end users at all
 * (see OrderSuccessDialog / ShowProfileQrsDialog / QrPickerDialog), since a
 * user-visible scannable image would let anyone skip buying the physical tag.
 */
fun saveQrBitmapToGallery(context: android.content.Context, bitmap: Bitmap, displayName: String): Boolean {
    return try {
        val resolver = context.contentResolver
        val values = android.content.ContentValues().apply {
            put(android.provider.MediaStore.Images.Media.DISPLAY_NAME, "$displayName.png")
            put(android.provider.MediaStore.Images.Media.MIME_TYPE, "image/png")
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                put(android.provider.MediaStore.Images.Media.RELATIVE_PATH, "Pictures/QRHealthcare")
            }
        }
        val uri = resolver.insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return false
        resolver.openOutputStream(uri)?.use { out -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, out) } ?: return false
        true
    } catch (e: Exception) {
        false
    }
}

/** Formats a Long price value as Vietnamese currency: 125,000 ₫ */
fun formatVND(amount: Long): String = "${"%,d".format(amount).replace(',', '.')} ₫"
