package com.qrhealthcare.app.ui.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions

/**
 * Returns a launcher you can `.invoke()` from a Compose click handler to open
 * the device camera and scan a QR code. The [onResult] callback fires with the
 * raw decoded string (or is silently dropped if the user cancels).
 *
 * Usage:
 *   val scan = rememberQrScanner { code -> ... }
 *   Button(onClick = { scan() }) { Text("Quét QR") }
 *
 * Camera permission is requested by the embedded scanner activity itself —
 * you don't need to handle it here, but it MUST be declared in the manifest
 * (it already is).
 */
@Composable
fun rememberQrScanner(onResult: (String) -> Unit): () -> Unit {
    val launcher = rememberLauncherForActivityResult(ScanContract()) { result ->
        result.contents?.let(onResult)
    }
    val options = remember {
        ScanOptions().apply {
            setDesiredBarcodeFormats(ScanOptions.QR_CODE)
            setPrompt("Đưa mã QR vào khung hình")
            setBeepEnabled(true)
            setOrientationLocked(true)
            setBarcodeImageEnabled(false)
        }
    }
    return { launcher.launch(options) }
}

/**
 * Extracts a tagCode from anything a user might scan:
 *   "qrhealthcare://profile/QRH-A1B2"      -> "QRH-A1B2"
 *   "https://qrhealthcare.com/qr/QRH-A1B2" -> "QRH-A1B2"
 *   "QRH-A1B2"                              -> "QRH-A1B2"
 *
 * Returns null if no recognizable tag code is found.
 */
fun parseScannedTagCode(scanned: String): String? {
    val trimmed = scanned.trim()
    if (trimmed.isEmpty()) return null
    // Deep link or web link
    val lastSegment = trimmed.substringAfterLast('/', missingDelimiterValue = trimmed)
    // Strip any querystring (e.g. "?utm=...")
    val codeOnly = lastSegment.substringBefore('?').substringBefore('#').uppercase()
    // Accept either the canonical "QRH-XXXX" or any short alphanumeric token
    return when {
        codeOnly.matches(Regex("^QRH-[A-Z0-9]{4,}$")) -> codeOnly
        codeOnly.matches(Regex("^[A-Z0-9-]{4,32}$"))  -> codeOnly
        else -> null
    }
}
