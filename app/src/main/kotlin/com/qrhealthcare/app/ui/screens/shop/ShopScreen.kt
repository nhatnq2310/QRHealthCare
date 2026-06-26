package com.qrhealthcare.app.ui.screens.shop

import androidx.compose.foundation.background
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import com.qrhealthcare.app.ui.theme.WarningAmber
import com.qrhealthcare.app.ui.theme.WarningBg
import com.qrhealthcare.app.ui.theme.SuccessGreen
import com.qrhealthcare.app.ui.theme.SuccessBg
import com.qrhealthcare.app.ui.theme.InfoBlue
import com.qrhealthcare.app.ui.theme.InfoBg
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.qrhealthcare.app.data.model.Product
import com.qrhealthcare.app.data.model.Coupon
import com.qrhealthcare.app.ui.components.formatVND
import com.qrhealthcare.app.ui.navigation.Routes
import com.qrhealthcare.app.ui.viewmodel.CartViewModel
import com.qrhealthcare.app.ui.viewmodel.ShopViewModel

@Composable
fun ShopScreen(
    navController: NavController,
    viewModel: ShopViewModel = hiltViewModel(),
    cartViewModel: CartViewModel = com.qrhealthcare.app.ui.util.activityViewModel()
) {
    val state by viewModel.state.collectAsState()
    val cartState by cartViewModel.state.collectAsState()

    LaunchedEffect(Unit) { viewModel.loadProducts() }

    Column(modifier = Modifier.fillMaxSize()) {

        // ── Product grid (header promo + title scroll together as grid items) ──
        if (state.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return
        }
        if (state.error != null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Không tải được sản phẩm", color = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { viewModel.loadProducts() }) { Text("Thử lại") }
                }
            }
            return
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            // ── Hero promo card (rounded, floating, full-width) ────────────────
            item(span = { GridItemSpan(2) }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.secondary
                                )
                            )
                        )
                        .padding(24.dp)
                ) {
                    Column {
                        Text(
                            text = "Mỗi giây đều quan trọng khi nói đến an toàn",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            lineHeight = MaterialTheme.typography.titleLarge.fontSize * 1.25f
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Sẵn sàng trong mọi tình huống với QR Healthcare Medical ID",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Surface(
                            color = Color.White,
                            shape = RoundedCornerShape(50),
                            shadowElevation = 0.dp
                        ) {
                            Text(
                                "Khám phá sản phẩm ↓",
                                modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // ── Rotating voucher banner (cycles every 3s, loops) ──────────────
            item(span = { GridItemSpan(2) }) {
                RotatingVoucherBanner(coupons = state.publicCoupons)
            }

            // ── Section title ──────────────────────────────────────────────────
            item(span = { GridItemSpan(2) }) {
                Text(
                    text = "Sản Phẩm Của Chúng Tôi",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            items(state.products) { product ->
                ProductCard(
                    product = product,
                    onClick = { navController.navigate(Routes.productDetail(product.slug)) }
                )
            }
            item(span = { GridItemSpan(2) }) { Spacer(modifier = Modifier.height(8.dp)) }
        }
    }
}

/** A single voucher entry shown in the rotating banner. */
private data class Voucher(
    val icon: String,
    val title: String,
    val subtitle: String,
    val bg: Color,
    val fg: Color
)

/**
 * Rotating voucher banner: shows the store's active PUBLIC vouchers one at a
 * time, each for 3 seconds, looping with a smooth crossfade + slide. Driven by
 * live coupons from the server (secret/expired/used-up codes are filtered out
 * server-side, so they never appear here). Falls back to a generic message if
 * there are no public vouchers.
 */
@Composable
private fun RotatingVoucherBanner(coupons: List<Coupon>) {
    // Map server coupons → display rows, cycling through a few accent palettes.
    val palettes = listOf(
        Triple("🎁", WarningBg, WarningAmber),
        Triple("🚚", SuccessBg, SuccessGreen),
        Triple("💎", InfoBg, InfoBlue)
    )
    val vouchers = coupons.mapIndexed { i, c ->
        val (icon, bg, fg) = palettes[i % palettes.size]
        val discount = if (c.discountType == "percent")
            "Giảm ${c.discountValue}%"
        else
            "Giảm ${formatVND(c.discountValue)}"
        Voucher(
            icon = icon,
            title = c.description.ifBlank { discount },
            subtitle = "Nhập mã ${c.code}",
            bg = bg,
            fg = fg
        )
    }

    // Nothing to advertise → simple neutral banner.
    if (vouchers.isEmpty()) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("🛡️", style = MaterialTheme.typography.headlineSmall)
                Text(
                    "An toàn sức khỏe trong tầm tay",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        return
    }

    var index by remember(vouchers.size) { mutableStateOf(0) }

    // Advance every 3 seconds, looping. Only needed when 2+ vouchers exist.
    LaunchedEffect(vouchers.size) {
        if (vouchers.size <= 1) return@LaunchedEffect
        while (true) {
            kotlinx.coroutines.delay(3000)
            index = (index + 1) % vouchers.size
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        AnimatedContent(
            targetState = index.coerceIn(0, vouchers.size - 1),
            transitionSpec = {
                (slideInHorizontally { it / 2 } + fadeIn(animationSpec = tween(400)))
                    .togetherWith(slideOutHorizontally { -it / 2 } + fadeOut(animationSpec = tween(400)))
            },
            label = "voucher"
        ) { i ->
            val v = vouchers[i]
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = v.bg,
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(v.icon, style = MaterialTheme.typography.headlineSmall)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            v.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = v.fg,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            v.subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = v.fg.copy(alpha = 0.85f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        if (vouchers.size > 1) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                vouchers.indices.forEach { i ->
                    val active = i == index
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 3.dp)
                            .size(if (active) 8.dp else 6.dp)
                            .clip(RoundedCornerShape(50))
                            .background(
                                if (active) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                            )
                    )
                }
            }
        }
    }
}

@Composable
fun ProductCard(product: Product, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column {
            Box {
                // Clean white image plate so product PNGs (often transparent) sit
                // on a consistent background regardless of source.
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = com.qrhealthcare.app.data.api.ApiClient.uploadUrl(product.imageUrl),
                        contentDescription = product.name,
                        modifier = Modifier.fillMaxSize().padding(8.dp),
                        contentScale = ContentScale.Fit
                    )
                }
                // Badge — brand red pill, top-left
                if (product.badge.isNotBlank()) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(bottomEnd = 12.dp, topStart = 16.dp),
                        modifier = Modifier.align(Alignment.TopStart)
                    ) {
                        Text(
                            text = product.badge,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
                // Low-stock chip — amber, top-right
                if (product.lowStock) {
                    Surface(
                        color = WarningAmber,
                        shape = RoundedCornerShape(bottomStart = 12.dp, topEnd = 16.dp),
                        modifier = Modifier.align(Alignment.TopEnd)
                    ) {
                        Text(
                            text = "Sắp hết",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                        )
                    }
                }
            }
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    minLines = 2
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (product.oldPrice != null) {
                        Text(
                            text = formatVND(product.oldPrice),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textDecoration = TextDecoration.LineThrough
                        )
                    }
                    Text(
                        text = formatVND(product.price),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
