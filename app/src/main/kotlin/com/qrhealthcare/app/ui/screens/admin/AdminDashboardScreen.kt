@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.qrhealthcare.app.ui.screens.admin

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.qrhealthcare.app.data.model.*
import com.qrhealthcare.app.ui.components.formatVND
import com.qrhealthcare.app.ui.viewmodel.AdminViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val dateFmt = SimpleDateFormat("dd/MM HH:mm", Locale("vi", "VN"))
private val dateFmtLong = SimpleDateFormat("dd/MM/yyyy", Locale("vi", "VN"))
private val ORDER_STATUSES = listOf("pending", "paid", "shipped", "delivered", "cancelled")

/**
 * Admin dashboard — tabbed layout covering everything an operator needs:
 *  - Tổng Quan  : time-period KPIs, 14-day revenue chart, breakdowns
 *  - Đơn Hàng   : full order list + status updater (essential for COD flow)
 *  - Người Dùng : user directory
 *  - Mã Giảm Giá: coupon CRUD
 */
@Composable
fun AdminDashboardScreen(viewModel: AdminViewModel = hiltViewModel()) {
    val metrics by viewModel.metrics.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val selectedTab by viewModel.selectedTab.collectAsState()

    LaunchedEffect(Unit) { viewModel.loadMetrics() }

    Column(modifier = Modifier.fillMaxSize()) {
        // ── Header with refresh ────────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Dashboard Admin", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
            if (isLoading) CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
            else IconButton(onClick = { viewModel.loadMetrics() }) {
                Icon(Icons.Default.Refresh, contentDescription = "Làm mới")
            }
        }

        // ── Tab row ────────────────────────────────────────────────────────────
        TabRow(selectedTabIndex = selectedTab) {
            listOf(
                "Tổng Quan" to Icons.Default.Dashboard,
                "Đơn Hàng" to Icons.Default.Inventory2,
                "Người Dùng" to Icons.Default.People,
                "Mã Giảm Giá" to Icons.Default.LocalOffer
            ).forEachIndexed { i, (label, icon) ->
                Tab(
                    selected = selectedTab == i,
                    onClick = { viewModel.selectTab(i) },
                    icon = { Icon(icon, null, modifier = Modifier.size(18.dp)) },
                    text = { Text(label, style = MaterialTheme.typography.labelSmall, maxLines = 1) }
                )
            }
        }

        // ── Tab content ────────────────────────────────────────────────────────
        if (error != null) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Text(error!!, modifier = Modifier.padding(12.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer)
            }
        }

        val m = metrics
        when {
            m == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                if (!isLoading) Text("Chưa có dữ liệu") else CircularProgressIndicator()
            }
            selectedTab == 0 -> OverviewTab(m)
            selectedTab == 1 -> OrdersTab(m, viewModel)
            selectedTab == 2 -> UsersTab(m)
            selectedTab == 3 -> CouponsTab(m, viewModel)
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// TAB 1 — OVERVIEW
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun OverviewTab(m: AdminMetrics) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ── Time-period revenue tiles ──────────────────────────────────────────
        item {
            Text("Doanh Thu", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                KpiCard("Hôm Nay",     formatVND(m.revenueToday),     Icons.Default.Today,
                    Color(0xFFFFEBEE), modifier = Modifier.weight(1f), compact = true)
                KpiCard("7 Ngày",      formatVND(m.revenueThisWeek),  Icons.Default.DateRange,
                    Color(0xFFE3F2FD), modifier = Modifier.weight(1f), compact = true)
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                KpiCard("30 Ngày",    formatVND(m.revenueThisMonth), Icons.Default.CalendarMonth,
                    Color(0xFFE8F5E9), modifier = Modifier.weight(1f), compact = true)
                KpiCard("Tổng Cộng", formatVND(m.totalRevenue),     Icons.Default.AccountBalance,
                    Color(0xFFFFF8E1), modifier = Modifier.weight(1f), compact = true)
            }
        }

        // ── 14-day revenue trend ───────────────────────────────────────────────
        if (m.dailyRevenue.any { it > 0 }) {
            item {
                Card(shape = RoundedCornerShape(12.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Doanh thu 14 ngày", style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold)
                            Text("Tổng: ${formatVND(m.dailyRevenue.sum())}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        SparkLine(values = m.dailyRevenue.map { it.toFloat() },
                            color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }

        // ── Activity counters ──────────────────────────────────────────────────
        item {
            Text("Hoạt Động", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                KpiCard("Đơn hôm nay", m.ordersToday.toString(), Icons.Default.ShoppingBag,
                    Color(0xFFF3E5F5), modifier = Modifier.weight(1f), compact = true)
                KpiCard("User mới", m.newUsersToday.toString(), Icons.Default.PersonAdd,
                    Color(0xFFE0F7FA), modifier = Modifier.weight(1f), compact = true)
                KpiCard("Lượt quét QR", m.totalQrScans.toString(), Icons.Default.QrCodeScanner,
                    Color(0xFFFFF3E0), modifier = Modifier.weight(1f), compact = true)
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                KpiCard("Tổng User", m.totalUsers.toString(), Icons.Default.People,
                    MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.weight(1f), compact = true)
                KpiCard("Hồ sơ", m.totalProfiles.toString(), Icons.Default.Person,
                    Color(0xFFE3F2FD), modifier = Modifier.weight(1f), compact = true)
                KpiCard("Tổng đơn", m.totalOrders.toString(), Icons.Default.Receipt,
                    Color(0xFFE8F5E9), modifier = Modifier.weight(1f), compact = true)
            }
        }

        // ── Orders by status (mini bar chart) ──────────────────────────────────
        if (m.ordersByStatus.isNotEmpty()) {
            item {
                Card(shape = RoundedCornerShape(12.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Đơn theo trạng thái", style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(12.dp))
                        val maxCount = (m.ordersByStatus.values.maxOrNull() ?: 1).coerceAtLeast(1)
                        ORDER_STATUSES.forEach { status ->
                            val count = m.ordersByStatus[status] ?: 0
                            StatusBarRow(prettyStatus(status), count, maxCount, statusColor(status))
                        }
                    }
                }
            }
        }

        // ── Top products ───────────────────────────────────────────────────────
        if (m.productSalesCounts.isNotEmpty()) {
            item {
                Card(shape = RoundedCornerShape(12.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Sản Phẩm Bán Chạy", style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        m.productSalesCounts.entries.sortedByDescending { it.value }.take(5)
                            .forEachIndexed { idx, (name, count) ->
                                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("${idx + 1}. $name",
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.weight(1f),
                                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text("$count đã bán",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary)
                                }
                            }
                    }
                }
            }
        }

        // ── Recent orders (mini list) ──────────────────────────────────────────
        item {
            Text("Đơn Hàng Gần Đây", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        items(m.recentOrders, key = { it.id }) { order ->
            Card(shape = RoundedCornerShape(10.dp)) {
                Row(modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("#${order.id.takeLast(6).uppercase()}",
                            style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        Text(if (order.createdAt > 0) dateFmt.format(Date(order.createdAt)) else "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline)
                    }
                    Text(formatVND(order.totalAmount),
                        style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    StatusPill(order.status)
                }
            }
        }
        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// TAB 2 — ORDERS (list + status updater)
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun OrdersTab(m: AdminMetrics, viewModel: AdminViewModel) {
    var statusFilter by remember { mutableStateOf("all") }
    var search by remember { mutableStateOf("") }
    var editingOrder by remember { mutableStateOf<Order?>(null) }

    editingOrder?.let { order ->
        OrderStatusDialog(
            order = order,
            onSave = { newStatus ->
                viewModel.updateOrderStatus(order, newStatus) { _, _ -> }
                editingOrder = null
            },
            onDismiss = { editingOrder = null }
        )
    }

    val filteredOrders = remember(m.allOrders, statusFilter, search) {
        m.allOrders.asSequence()
            .filter { statusFilter == "all" || it.status == statusFilter }
            .filter {
                if (search.isBlank()) true
                else it.id.contains(search, ignoreCase = true) ||
                    it.userId.contains(search, ignoreCase = true) ||
                    it.couponCode.contains(search, ignoreCase = true)
            }
            .toList()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Search box
        OutlinedTextField(
            value = search,
            onValueChange = { search = it },
            placeholder = { Text("Tìm mã đơn / user ID / coupon...") },
            leadingIcon = { Icon(Icons.Default.Search, null) },
            trailingIcon = if (search.isNotBlank()) {
                { IconButton(onClick = { search = "" }) { Icon(Icons.Default.Clear, "Xóa") } }
            } else null,
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
        )

        // Status filter chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatusFilterChip("Tất cả", "all", statusFilter, m.allOrders.size) { statusFilter = it }
            ORDER_STATUSES.forEach { s ->
                StatusFilterChip(prettyStatus(s), s, statusFilter, m.ordersByStatus[s] ?: 0) { statusFilter = it }
            }
        }

        // Order list
        if (filteredOrders.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Không có đơn hàng phù hợp", color = MaterialTheme.colorScheme.outline)
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredOrders, key = { it.id }) { order ->
                    AdminOrderCard(order, onClick = { editingOrder = order })
                }
                item { Spacer(modifier = Modifier.height(24.dp)) }
            }
        }
    }
}

@Composable
private fun AdminOrderCard(order: Order, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("#${order.id.takeLast(6).uppercase()}",
                        style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    if (order.createdAt > 0) {
                        Text(dateFmt.format(Date(order.createdAt)),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline)
                    }
                }
                StatusPill(order.status)
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(order.items.joinToString(", ") { "${it.productName} ×${it.quantity}" },
                style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(modifier = Modifier.height(4.dp))
            Row(modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    prettyPayment(order.paymentMethod) +
                        (if (order.couponCode.isNotBlank()) " · ${order.couponCode}" else ""),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
                Text(formatVND(order.totalAmount),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun OrderStatusDialog(order: Order, onSave: (String) -> Unit, onDismiss: () -> Unit) {
    var selected by remember(order.id) { mutableStateOf(order.status) }
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Edit, null, tint = MaterialTheme.colorScheme.primary) },
        title = { Text("Đơn #${order.id.takeLast(6).uppercase()}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Order summary
                Text("Tổng: ${formatVND(order.totalAmount)}",
                    style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary)
                if (order.discountAmount > 0) {
                    Text("Đã giảm: ${formatVND(order.discountAmount)} (${order.couponCode})",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline)
                }
                Text("Thanh toán: ${prettyPayment(order.paymentMethod)}",
                    style = MaterialTheme.typography.bodySmall)
                if (order.items.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Sản phẩm:", style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold)
                    order.items.forEach { item ->
                        Text("• ${item.productName} ×${item.quantity}" +
                            (if (item.emergencyContact.isNotBlank()) " (SĐT: ${item.emergencyContact})" else ""),
                            style = MaterialTheme.typography.bodySmall)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text("Cập nhật trạng thái:", style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold)
                ORDER_STATUSES.forEach { status ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = selected == status, onClick = { selected = status })
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(prettyStatus(status), modifier = Modifier.weight(1f))
                        StatusPill(status)
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onSave(selected) }, enabled = selected != order.status) {
                Text("Lưu")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Hủy") } }
    )
}

// ═══════════════════════════════════════════════════════════════════════════════
// TAB 3 — USERS
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun UsersTab(m: AdminMetrics) {
    var search by remember { mutableStateOf("") }
    val filtered = remember(m.allUsers, search) {
        if (search.isBlank()) m.allUsers
        else m.allUsers.filter {
            it.email.contains(search, ignoreCase = true) ||
                it.fullName.contains(search, ignoreCase = true)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = search,
            onValueChange = { search = it },
            placeholder = { Text("Tìm theo email hoặc tên...") },
            leadingIcon = { Icon(Icons.Default.Search, null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
        )

        Text(
            "${filtered.size} / ${m.allUsers.size} người dùng",
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline
        )

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filtered, key = { it.id }) { user -> AdminUserCard(user) }
            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun AdminUserCard(user: User) {
    Card(shape = RoundedCornerShape(10.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically) {
            // Avatar initials circle
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape)
                    .background(if (user.role == "admin") Color(0xFFE53935) else MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    (user.fullName.ifBlank { user.email }).take(1).uppercase(),
                    color = if (user.role == "admin") Color.White else MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(user.fullName.ifBlank { "(chưa đặt tên)" },
                    style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Text(user.email, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline)
                if (user.address.isNotBlank()) {
                    Text("📍 ${user.address}", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                if (user.createdAt > 0) {
                    Text("Đăng ký: ${dateFmtLong.format(Date(user.createdAt))}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline)
                }
            }
            if (user.role == "admin") {
                Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFFE53935)) {
                    Text("ADMIN", color = Color.White,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// TAB 4 — COUPONS (CRUD)
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun CouponsTab(m: AdminMetrics, viewModel: AdminViewModel) {
    var editingCoupon by remember { mutableStateOf<Coupon?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }

    if (showCreateDialog) {
        CouponEditorDialog(
            initial = null,
            onSave = { coupon ->
                viewModel.createCoupon(coupon) { _, _ -> }
                showCreateDialog = false
            },
            onDismiss = { showCreateDialog = false }
        )
    }
    editingCoupon?.let { coupon ->
        CouponEditorDialog(
            initial = coupon,
            onSave = { updated ->
                viewModel.updateCoupon(updated) { _, _ -> }
                editingCoupon = null
            },
            onDismiss = { editingCoupon = null }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 96.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (m.allCoupons.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(top = 64.dp),
                        contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.LocalOffer, null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.outline)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Chưa có mã giảm giá",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.outline)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Bấm dấu + để tạo mã mới",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline)
                        }
                    }
                }
            } else {
                items(m.allCoupons, key = { it.id }) { coupon ->
                    AdminCouponCard(
                        coupon,
                        onClick = { editingCoupon = coupon },
                        onToggleActive = {
                            viewModel.updateCoupon(coupon.copy(active = !coupon.active)) { _, _ -> }
                        }
                    )
                }
            }
        }

        FloatingActionButton(
            onClick = { showCreateDialog = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(24.dp),
            containerColor = MaterialTheme.colorScheme.primary
        ) {
            Icon(Icons.Default.Add, "Tạo mã mới", tint = Color.White)
        }
    }
}

@Composable
private fun AdminCouponCard(coupon: Coupon, onClick: () -> Unit, onToggleActive: () -> Unit) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (coupon.active) MaterialTheme.colorScheme.surface
            else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = RoundedCornerShape(6.dp),
                        color = if (coupon.active) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outline) {
                        Text(coupon.code,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            color = Color.White, fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelMedium)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        if (coupon.discountType == "percent") "${coupon.discountValue}% off"
                        else "${formatVND(coupon.discountValue)} off",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (coupon.active) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outline
                    )
                }
                if (coupon.description.isNotBlank()) {
                    Text(coupon.description, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline)
                }
                val parts = buildList {
                    if (coupon.minOrderAmount > 0) add("min ${formatVND(coupon.minOrderAmount)}")
                    if (coupon.usageLimit != null) add("đã dùng ${coupon.usageCount}/${coupon.usageLimit}")
                    if (coupon.expiresAt != null) add("hết hạn ${dateFmtLong.format(Date(coupon.expiresAt))}")
                }
                if (parts.isNotEmpty()) {
                    Text(parts.joinToString(" · "),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline)
                }
            }
            Switch(checked = coupon.active, onCheckedChange = { onToggleActive() })
        }
    }
}

@Composable
private fun CouponEditorDialog(
    initial: Coupon?,
    onSave: (Coupon) -> Unit,
    onDismiss: () -> Unit
) {
    var code by remember { mutableStateOf(initial?.code ?: "") }
    var description by remember { mutableStateOf(initial?.description ?: "") }
    var isPercent by remember { mutableStateOf((initial?.discountType ?: "fixed") == "percent") }
    var value by remember { mutableStateOf(initial?.discountValue?.toString() ?: "") }
    var minOrder by remember { mutableStateOf(initial?.minOrderAmount?.toString() ?: "0") }
    var active by remember { mutableStateOf(initial?.active ?: true) }

    fun build(): Coupon? {
        val v = value.toLongOrNull() ?: return null
        val mo = minOrder.toLongOrNull() ?: 0L
        val codeTrim = code.uppercase().trim()
        if (codeTrim.isBlank()) return null
        return (initial ?: Coupon()).copy(
            code = codeTrim,
            description = description.trim(),
            discountType = if (isPercent) "percent" else "fixed",
            discountValue = v,
            minOrderAmount = mo,
            active = active
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.LocalOffer, null, tint = MaterialTheme.colorScheme.primary) },
        title = { Text(if (initial == null) "Tạo Mã Giảm Giá" else "Chỉnh Sửa Mã") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = code, onValueChange = { code = it.uppercase() },
                    label = { Text("Mã (VD: SUMMER10)") }, singleLine = true,
                    enabled = initial == null,  // can't change code after creation
                    modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = description, onValueChange = { description = it },
                    label = { Text("Mô tả") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth())
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FilterChip(selected = isPercent, onClick = { isPercent = true },
                        label = { Text("Phần trăm") }, modifier = Modifier.weight(1f))
                    Spacer(modifier = Modifier.width(8.dp))
                    FilterChip(selected = !isPercent, onClick = { isPercent = false },
                        label = { Text("Số tiền cố định") }, modifier = Modifier.weight(1f))
                }
                OutlinedTextField(value = value, onValueChange = { value = it.filter { c -> c.isDigit() } },
                    label = { Text(if (isPercent) "Phần trăm giảm (1-100)" else "Số tiền giảm (VND)") },
                    singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = minOrder, onValueChange = { minOrder = it.filter { c -> c.isDigit() } },
                    label = { Text("Đơn tối thiểu (VND)") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth())
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Đang hoạt động", modifier = Modifier.weight(1f))
                    Switch(checked = active, onCheckedChange = { active = it })
                }
            }
        },
        confirmButton = {
            Button(onClick = { build()?.let(onSave) }, enabled = code.isNotBlank() && value.isNotBlank()) {
                Text(if (initial == null) "Tạo" else "Lưu")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Hủy") } }
    )
}

// ═══════════════════════════════════════════════════════════════════════════════
// SHARED UI HELPERS
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun KpiCard(
    label: String,
    value: String,
    icon: ImageVector,
    bgColor: Color,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(if (compact) 10.dp else 14.dp)) {
            Icon(icon, null, modifier = Modifier.size(if (compact) 18.dp else 22.dp),
                tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(if (compact) 2.dp else 4.dp))
            Text(label,
                style = if (compact) MaterialTheme.typography.labelSmall else MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value,
                style = if (compact) MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun StatusPill(status: String) {
    val color = statusColor(status)
    Surface(shape = RoundedCornerShape(12.dp), color = color.copy(alpha = 0.15f)) {
        Text(prettyStatus(status),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            color = color, fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun StatusFilterChip(
    label: String, value: String, current: String, count: Int, onClick: (String) -> Unit
) {
    FilterChip(
        selected = current == value,
        onClick = { onClick(value) },
        label = { Text("$label ($count)", style = MaterialTheme.typography.labelMedium) }
    )
}

@Composable
private fun StatusBarRow(label: String, count: Int, max: Int, color: Color) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodySmall)
            Text(count.toString(), style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Box(modifier = Modifier.fillMaxWidth().height(6.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)) {
            Box(modifier = Modifier
                .fillMaxWidth(fraction = count.toFloat() / max.toFloat())
                .fillMaxHeight()
                .clip(RoundedCornerShape(3.dp))
                .background(color))
        }
    }
}

/** Tiny SVG-style line chart of recent revenue. Auto-scales to data peak. */
@Composable
private fun SparkLine(values: List<Float>, color: Color) {
    if (values.isEmpty()) return
    val max = (values.maxOrNull() ?: 0f).coerceAtLeast(1f)
    Canvas(modifier = Modifier.fillMaxWidth().height(80.dp)) {
        val w = size.width
        val h = size.height
        val step = if (values.size > 1) w / (values.size - 1) else w

        // Soft fill under the line
        val fillPath = Path().apply {
            moveTo(0f, h)
            values.forEachIndexed { i, v ->
                val x = i * step
                val y = h - (v / max) * h * 0.85f
                if (i == 0) lineTo(x, y) else lineTo(x, y)
            }
            lineTo(w, h)
            close()
        }
        drawPath(fillPath, color = color.copy(alpha = 0.15f))

        // Main line
        val linePath = Path().apply {
            values.forEachIndexed { i, v ->
                val x = i * step
                val y = h - (v / max) * h * 0.85f
                if (i == 0) moveTo(x, y) else lineTo(x, y)
            }
        }
        drawPath(linePath, color = color, style = Stroke(width = 3f))

        // Dots
        values.forEachIndexed { i, v ->
            val x = i * step
            val y = h - (v / max) * h * 0.85f
            drawCircle(color = color, radius = 3f, center = Offset(x, y))
        }
    }
}

private fun prettyStatus(s: String): String = when (s) {
    "pending"   -> "Chờ xử lý"
    "paid"      -> "Đã thanh toán"
    "shipped"   -> "Đang giao"
    "delivered" -> "Đã giao"
    "cancelled" -> "Đã hủy"
    else -> s
}

private fun statusColor(s: String): Color = when (s) {
    "pending"   -> Color(0xFFFF9800)
    "paid"      -> Color(0xFF388E3C)
    "shipped"   -> Color(0xFF1976D2)
    "delivered" -> Color(0xFF388E3C)
    "cancelled" -> Color(0xFFD32F2F)
    else        -> Color(0xFF757575)
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
