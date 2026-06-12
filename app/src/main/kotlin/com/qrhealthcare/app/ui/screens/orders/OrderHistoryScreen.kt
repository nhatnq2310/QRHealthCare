package com.qrhealthcare.app.ui.screens.orders

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.qrhealthcare.app.data.model.Order
import com.qrhealthcare.app.ui.components.formatVND
import com.qrhealthcare.app.ui.viewmodel.OrderHistoryViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val dateFmt = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("vi", "VN"))

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderHistoryScreen(
    onBack: () -> Unit,
    viewModel: OrderHistoryViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(Unit) { viewModel.load() }

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
                items(state.orders, key = { it.id }) { order -> OrderCard(order) }
            }
        }
    }
}

@Composable
private fun OrderCard(order: Order) {
    Card(
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
                Text(
                    "Đơn #${order.id.takeLast(6).uppercase()}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
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
            Spacer(Modifier.height(8.dp))

            // Item lines
            order.items.forEach { item ->
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "${item.productName}  ×${item.quantity}" +
                            (item.emergencyContact.takeIf { it.isNotBlank() }?.let { " · SĐT: $it" } ?: ""),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        formatVND(item.price * item.quantity),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            HorizontalDivider(Modifier.padding(vertical = 8.dp))

            // Footer: total + payment method
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "PT thanh toán: ${prettyPayment(order.paymentMethod)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
                Text(
                    formatVND(order.totalAmount),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            if (order.qrTagIds.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    "${order.qrTagIds.size} mã QR đã tạo cho đơn này",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
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
