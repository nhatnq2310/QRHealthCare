package com.qrhealthcare.app.ui.screens.orders

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.qrhealthcare.app.data.api.ApiClient
import com.qrhealthcare.app.data.model.Order
import com.qrhealthcare.app.data.model.OrderItem
import com.qrhealthcare.app.ui.components.formatVND
import com.qrhealthcare.app.ui.navigation.Routes
import com.qrhealthcare.app.ui.viewmodel.CartViewModel
import com.qrhealthcare.app.ui.viewmodel.OrderHistoryViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val dateFmt = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("vi", "VN"))

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderHistoryScreen(
    onBack: () -> Unit,
    navController: NavController? = null,
    viewModel: OrderHistoryViewModel = hiltViewModel(),
    cartViewModel: CartViewModel = com.qrhealthcare.app.ui.util.activityViewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    var detailOrder by remember { mutableStateOf<Order?>(null) }
    var isBuyingAgain by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { viewModel.load() }

    fun buyAgain(order: Order) {
        isBuyingAgain = true
        viewModel.buyAgain(order, cartViewModel) { addedCount, skippedCount ->
            isBuyingAgain = false
            detailOrder = null
            val msg = when {
                addedCount > 0 && skippedCount == 0 -> "Đã thêm $addedCount sản phẩm vào giỏ hàng"
                addedCount > 0 && skippedCount > 0 -> "Đã thêm $addedCount sản phẩm — $skippedCount sản phẩm không còn bán"
                else -> "Sản phẩm trong đơn này không còn bán"
            }
            android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
            if (addedCount > 0) navController?.navigate(Routes.CART)
        }
    }

    detailOrder?.let { order ->
        OrderDetailDialog(
            order = order,
            isBuyingAgain = isBuyingAgain,
            onBuyAgain = { buyAgain(order) },
            onDismiss = { detailOrder = null }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Lịch Sử Đơn Hàng", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Quay lại")
                    }
                }
            )
        }
    ) { padding ->
        when {
            state.isLoading -> Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }

            state.error != null -> Box(
                Modifier.fillMaxSize().padding(padding).padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    state.error ?: "",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            state.orders.isEmpty() -> Column(
                Modifier.fillMaxSize().padding(padding).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Default.ReceiptLong, null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.outline
                )
                Spacer(Modifier.height(12.dp))
                Text("Bạn chưa có đơn hàng nào", style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Các đơn hàng đã đặt sẽ hiển thị tại đây.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(state.orders, key = { it.id }) { order ->
                    OrderCard(
                        order = order,
                        onClick = { detailOrder = order },
                        onBuyAgain = { buyAgain(order) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ProductThumb(imageUrl: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        if (imageUrl.isNotBlank()) {
            AsyncImage(
                model = ApiClient.uploadUrl(imageUrl),
                contentDescription = null,
                modifier = Modifier.fillMaxSize().padding(4.dp),
                contentScale = ContentScale.Fit
            )
        } else {
            Icon(Icons.Default.Inventory2, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
        }
    }
}

@Composable
private fun OrderCard(order: Order, onClick: () -> Unit, onBuyAgain: () -> Unit) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            // Header row: order id (short) + status pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Đơn #${order.id.takeLast(6).uppercase()}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    if (order.isPromo) {
                        Spacer(Modifier.width(6.dp))
                        Surface(color = MaterialTheme.colorScheme.tertiaryContainer, shape = RoundedCornerShape(6.dp)) {
                            Text("QUÀ TẶNG", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }
                }
                StatusPill(order.status)
            }

            Spacer(Modifier.height(4.dp))
            if (order.createdAt > 0) {
                Text(
                    dateFmt.format(Date(order.createdAt)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
            Spacer(Modifier.height(10.dp))

            // Shopee-style: thumbnail row + summary line, tap card for full detail
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Row(horizontalArrangement = Arrangement.spacedBy((-10).dp)) {
                    order.items.take(3).forEach { item ->
                        ProductThumb(item.imageUrl, modifier = Modifier.size(56.dp))
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        order.items.joinToString(", ") { it.productName },
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 2, overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        "${order.items.sumOf { it.quantity }} sản phẩm",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }

            HorizontalDivider(Modifier.padding(vertical = 10.dp))

            // Footer: total + payment method
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Tổng: ${formatVND(order.totalAmount)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onClick, contentPadding = PaddingValues(horizontal = 12.dp)) {
                        Text("Chi Tiết", style = MaterialTheme.typography.labelMedium)
                    }
                    if (!order.isPromo) {
                        Button(onClick = onBuyAgain, contentPadding = PaddingValues(horizontal = 12.dp)) {
                            Text("Mua Lại", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OrderDetailDialog(
    order: Order,
    isBuyingAgain: Boolean,
    onBuyAgain: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.ReceiptLong, null, tint = MaterialTheme.colorScheme.primary) },
        title = { Text("Đơn #${order.id.takeLast(6).uppercase()}") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState())
            ) {
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    StatusPill(order.status)
                    if (order.createdAt > 0) {
                        Text(dateFmt.format(Date(order.createdAt)), style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline)
                    }
                }

                Spacer(Modifier.height(2.dp))
                Text("Sản phẩm:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                order.items.forEach { item ->
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        ProductThumb(item.imageUrl, modifier = Modifier.size(48.dp))
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(item.productName, style = MaterialTheme.typography.bodyMedium)
                            Text("×${item.quantity}" + (item.emergencyContact.takeIf { it.isNotBlank() }?.let { " · SĐT: $it" } ?: ""),
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                        }
                        Text(formatVND(item.price * item.quantity), style = MaterialTheme.typography.bodyMedium)
                    }
                }

                HorizontalDivider()

                if (order.discountAmount > 0) {
                    Text("Đã giảm: ${formatVND(order.discountAmount)}" +
                        (if (order.couponCode.isNotBlank()) " (${order.couponCode})" else ""),
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                }
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text("Tổng cộng", fontWeight = FontWeight.Bold)
                    Text(formatVND(order.totalAmount), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
                Text("Vận chuyển: ${if (order.shippingFee <= 0) "Miễn phí" else formatVND(order.shippingFee)}",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                Text("Thanh toán: ${prettyPayment(order.paymentMethod)}", style = MaterialTheme.typography.bodySmall)
                if (order.paymentRef.isNotBlank()) {
                    Text("Mã đối soát: ${order.paymentRef}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                }

                val sa = order.shippingAddress
                if (sa.address.isNotBlank() || sa.fullName.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text("Giao đến:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                    if (sa.fullName.isNotBlank())
                        Text("${sa.fullName}" + (if (sa.phone.isNotBlank()) " — ${sa.phone}" else ""),
                            style = MaterialTheme.typography.bodySmall)
                    val line = listOf(sa.address, sa.city).filter { it.isNotBlank() }.joinToString(", ")
                    if (line.isNotBlank()) Text(line, style = MaterialTheme.typography.bodySmall)
                }

                if (order.qrTagIds.isNotEmpty()) {
                    Spacer(Modifier.height(2.dp))
                    Text("${order.qrTagIds.size} mã QR đã tạo cho đơn này — sản phẩm vật lý sẽ được giao riêng.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                }
            }
        },
        confirmButton = {
            if (!order.isPromo) {
                Button(onClick = onBuyAgain, enabled = !isBuyingAgain) {
                    if (isBuyingAgain) CircularProgressIndicator(modifier = Modifier.size(16.dp), color = MaterialTheme.colorScheme.onPrimary)
                    else Text("Mua Lại")
                }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Đóng") } }
    )
}

@Composable
private fun StatusPill(status: String) {
    val (label, bg, fg) = when (status) {
        "pending"   -> Triple("Chờ xử lý", Color(0xFFFFF3CD), Color(0xFF856404))
        "paid"      -> Triple("Đã thanh toán", Color(0xFFD1E7DD), Color(0xFF0F5132))
        "shipped"   -> Triple("Đang giao", Color(0xFFCFE2FF), Color(0xFF084298))
        "delivered" -> Triple("Đã giao", Color(0xFFD1E7DD), Color(0xFF0F5132))
        "cancelled" -> Triple("Đã hủy", Color(0xFFF8D7DA), Color(0xFF842029))
        else        -> Triple(status, Color(0xFFE9ECEF), Color(0xFF495057))
    }
    Box(
        Modifier.clip(RoundedCornerShape(12.dp))
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = fg, fontWeight = FontWeight.SemiBold)
    }
}

private fun prettyPayment(method: String): String = when (method) {
    "cash"        -> "Tiền mặt (COD)"
    "vietqr"      -> "VietQR"
    "bank"        -> "Chuyển khoản"
    "momo"        -> "MoMo"
    "vnpay"       -> "VNPay"
    "google_play" -> "Google Pay"
    else          -> method.ifBlank { "—" }
}
