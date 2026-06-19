package com.qrhealthcare.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * Floating shortcut that opens a popup showing the user's health profiles and
 * their linked QR codes. Teal circle with a white QR icon — visually paired
 * with the cart bubble but differentiated by color.
 *
 * Defaults to bottom-LEFT so it doesn't collide with the cart bubble's
 * bottom-right default. Position is in-memory only (resets on app restart).
 */
@Composable
fun FloatingQrBubble(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val bubbleSize = 56.dp
    val edgePadding = 12.dp
    val bottomReserved = 96.dp
    val topReserved = 72.dp

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val maxWidthPx = with(density) { maxWidth.toPx() }
        val maxHeightPx = with(density) { maxHeight.toPx() }
        val bubbleSizePx = with(density) { bubbleSize.toPx() }
        val edgePadPx = with(density) { edgePadding.toPx() }
        val bottomReservedPx = with(density) { bottomReserved.toPx() }
        val topReservedPx = with(density) { topReserved.toPx() }

        val minX = edgePadPx
        val maxX = (maxWidthPx - bubbleSizePx - edgePadPx).coerceAtLeast(minX)
        val minY = topReservedPx
        val maxY = (maxHeightPx - bubbleSizePx - bottomReservedPx).coerceAtLeast(minY)

        // Default: bottom-left (opposite corner from the cart bubble).
        var offsetX by remember(maxWidthPx) { mutableStateOf(minX) }
        var offsetY by remember(maxHeightPx) { mutableStateOf(maxY) }

        Box(
            modifier = Modifier
                .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                .size(bubbleSize)
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { onClick() })
                }
                .pointerInput(Unit) {
                    detectDragGestures { _, drag ->
                        offsetX = (offsetX + drag.x).coerceIn(minX, maxX)
                        offsetY = (offsetY + drag.y).coerceIn(minY, maxY)
                    }
                }
        ) {
            // White circular bubble with orange QR icon — matches the cart
            // bubble's white-with-color pattern, but different icon hue.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .shadow(elevation = 8.dp, shape = CircleShape)
                    .clip(CircleShape)
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.QrCode2,
                    contentDescription = "Xem QR hồ sơ",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}