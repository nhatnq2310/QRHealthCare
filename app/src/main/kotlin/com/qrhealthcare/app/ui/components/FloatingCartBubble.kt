package com.qrhealthcare.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qrhealthcare.app.ui.util.activityViewModel
import com.qrhealthcare.app.ui.viewmodel.CartViewModel
import kotlin.math.roundToInt

/**
 * Floating shortcut to the cart. White circle with the brand-red cart icon
 * and a red badge in the top-right showing the item count. Tap → open cart;
 * drag → reposition anywhere on screen.
 *
 * Position is persisted via [CartViewModel] (activity-scoped) so the bubble
 * stays where the user put it even when they navigate between screens.
 *
 * Place this as a sibling overlay of the main content — e.g. inside the
 * Box that wraps NavHost — so it floats above every screen that needs it.
 */
@Composable
fun FloatingCartBubble(
    itemCount: Int,
    onClick: () -> Unit,
    cartViewModel: CartViewModel = activityViewModel(),
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val bubbleSize = 56.dp
    val badgeSize = 22.dp
    val edgePadding = 12.dp
    // Reserved space at the bottom so the bubble can't park itself under the
    // bottom nav bar (~80dp + breathing room). On screens without a bottom
    // bar (ProductDetail, CreateProfile…) this just creates a slightly taller
    // dead zone at the bottom — invisible, harmless.
    val bottomReserved = 96.dp
    // Reserved at the top so the bubble doesn't bury itself behind a top app bar.
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

        // Default position: lower-right corner.
        val defaultX = maxX
        val defaultY = maxY

        val savedX = cartViewModel.bubbleX
        val savedY = cartViewModel.bubbleY
        var offsetX by remember(maxWidthPx) {
            mutableStateOf(if (savedX >= 0f) savedX.coerceIn(minX, maxX) else defaultX)
        }
        var offsetY by remember(maxHeightPx) {
            mutableStateOf(if (savedY >= 0f) savedY.coerceIn(minY, maxY) else defaultY)
        }

        Box(
            modifier = Modifier
                .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                .size(bubbleSize)
                // Tap and drag are layered as separate pointerInput blocks —
                // Compose's touch-slop machinery handles disambiguation: a
                // movement < slop is a tap, anything beyond becomes a drag.
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { onClick() })
                }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragEnd = { cartViewModel.saveBubblePosition(offsetX, offsetY) }
                    ) { _, drag ->
                        offsetX = (offsetX + drag.x).coerceIn(minX, maxX)
                        offsetY = (offsetY + drag.y).coerceIn(minY, maxY)
                    }
                }
        ) {
            // White circular bubble with shadow + brand-red cart icon
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .shadow(elevation = 8.dp, shape = CircleShape)
                    .clip(CircleShape)
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.ShoppingCart,
                    contentDescription = "Mở giỏ hàng",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            }

            // Item-count badge — small red circle overlapping the top-right.
            if (itemCount > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 4.dp, y = (-4).dp)
                        .size(badgeSize)
                        .shadow(elevation = 2.dp, shape = CircleShape)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (itemCount > 99) "99+" else itemCount.toString(),
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
