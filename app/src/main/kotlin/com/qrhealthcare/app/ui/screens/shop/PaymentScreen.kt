package com.qrhealthcare.app.ui.screens.shop

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.qrhealthcare.app.data.model.Profile
import com.qrhealthcare.app.data.model.QrTag
import com.qrhealthcare.app.data.payment.MerchantConfig
import com.qrhealthcare.app.data.payment.VietQR
import com.qrhealthcare.app.ui.components.QrCodeImage
import com.qrhealthcare.app.ui.components.formatVND
import com.qrhealthcare.app.ui.navigation.Routes
import com.qrhealthcare.app.ui.navigation.navigateToTab
import com.qrhealthcare.app.ui.viewmodel.CartViewModel
import com.qrhealthcare.app.ui.viewmodel.ProfileViewModel

private val PAYMENT_METHODS = listOf(
    Triple("vietqr", "📲", "VietQR (Chuyển khoản ngân hàng)"),
    Triple("cash",   "💵", "Tiền Mặt (Thanh toán khi nhận hàng)"),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentScreen(
    navController: NavController,
    cartViewModel: CartViewModel = com.qrhealthcare.app.ui.util.activityViewModel(),
    profileViewModel: ProfileViewModel = hiltViewModel(),
    authViewModel: com.qrhealthcare.app.ui.viewmodel.AuthViewModel = com.qrhealthcare.app.ui.util.activityViewModel()
) {
    val cartState by cartViewModel.state.collectAsState()
    val profileState by profileViewModel.listState.collectAsState()
    val authState by authViewModel.authState.collectAsState()

    var step by remember { mutableIntStateOf(1) }  // 1=profile, 2=payment, 3=confirm
    var selectedPayment by remember { mutableStateOf("") }
    var showSuccessDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { profileViewModel.loadMyProfiles() }

    // ── Order success dialog ─────────────────────────────────────────────────
    if (showSuccessDialog) {
        OrderSuccessDialog(
            tags = cartState.generatedTags,
            onDismiss = {
                showSuccessDialog = false
                navController.navigate(Routes.SHOP) { popUpTo(Routes.HOME) { saveState = false } }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(when(step) { 1 -> "Chọn Hồ Sơ"; 2 -> "Thanh Toán"; else -> "Xác Nhận" }) },
                navigationIcon = {
                    IconButton(onClick = { if (step > 1) step-- else navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())) {

            // Step indicator
            Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Hồ Sơ", "Thanh Toán", "Xác Nhận").forEachIndexed { i, label ->
                    val active = step >= i + 1
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(label, modifier = Modifier.padding(vertical = 6.dp),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                            color = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    }
                    if (i < 2) { HorizontalDivider(modifier = Modifier.weight(0.2f).align(Alignment.CenterVertically)) }
                }
            }

            // ── STEP 1: Select profile ───────────────────────────────────────
            if (step == 1) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Chọn hồ sơ để ghi lên sản phẩm:", style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)

                    if (profileState.isLoading) {
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    } else if (profileState.profiles.isEmpty()) {
                        Card { Column(modifier = Modifier.padding(16.dp)) {
                            Text("Bạn chưa có hồ sơ nào.", fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(onClick = { navController.navigateToTab(Routes.PROFILES) }) { Text("Tạo hồ sơ ngay") }
                        }}
                    } else {
                        profileState.profiles.forEach { profile ->
                            ProfileSelectCard(
                                profile = profile,
                                selected = cartState.selectedProfileId == profile.id,
                                onClick = { cartViewModel.setProfile(profile.id) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { step = 2 },
                        enabled = cartState.selectedProfileId.isNotBlank(),
                        modifier = Modifier.fillMaxWidth().height(50.dp)
                    ) { Text("Tiếp Theo →") }
                }
            }

            // ── STEP 2: Payment method ───────────────────────────────────────
            if (step == 2) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Tổng thanh toán: ${formatVND(cartViewModel.finalTotal)}",
                        style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                    PAYMENT_METHODS.forEach { (key, emoji, label) ->
                        OutlinedCard(
                            modifier = Modifier.fillMaxWidth().clickable { selectedPayment = key },
                            border = BorderStroke(
                                width = if (selectedPayment == key) 2.dp else 1.dp,
                                color = if (selectedPayment == key) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                            )
                        ) {
                            Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(emoji, style = MaterialTheme.typography.titleLarge)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.weight(1f))
                                if (selectedPayment == key) {
                                    Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }

                    if (selectedPayment == "cash") {
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                            Text("Bạn sẽ thanh toán cho nhân viên giao hàng khi nhận hàng.",
                                modifier = Modifier.padding(12.dp),
                                style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    if (selectedPayment == "vietqr") {
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                            Text(
                                "Bước tiếp theo sẽ hiển thị mã VietQR. Bạn dùng app ngân hàng quét QR " +
                                    "để chuyển khoản, sau đó xác nhận đã thanh toán.",
                                modifier = Modifier.padding(12.dp),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { step = 3 },
                        enabled = selectedPayment.isNotBlank(),
                        modifier = Modifier.fillMaxWidth().height(50.dp)
                    ) { Text("Tiếp Theo →") }
                }
            }

            // ── STEP 3: Confirm ──────────────────────────────────────────────
            if (step == 3) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Xác Nhận Đơn Hàng", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                    // Delivery-address card. Address can come from either the
                    // user's saved account address OR the per-order shipping
                    // address collected on the Checkout screen.
                    val orderAddr = cartState.shippingAddress
                    val effectiveAddress = when {
                        orderAddr.address.isNotBlank() ->
                            listOfNotNull(
                                orderAddr.fullName.ifBlank { null },
                                orderAddr.phone.ifBlank { null },
                                listOf(orderAddr.address, orderAddr.city).filter { it.isNotBlank() }.joinToString(", ").ifBlank { null }
                            ).joinToString(" · ")
                        authState.address.isNotBlank() -> authState.address
                        else -> ""
                    }
                    val hasAddress = effectiveAddress.isNotBlank()
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (hasAddress) MaterialTheme.colorScheme.surfaceVariant
                            else MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.LocationOn, null,
                                    tint = if (hasAddress) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Địa Chỉ Giao Hàng",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f)
                                )
                                TextButton(onClick = { navController.navigate(Routes.CHECKOUT) }) {
                                    Text(if (hasAddress) "Sửa" else "Thêm")
                                }
                            }
                            if (hasAddress) {
                                Text(effectiveAddress, style = MaterialTheme.typography.bodyMedium)
                            } else {
                                Text(
                                    "Chưa có địa chỉ. Bấm \"Thêm\" để nhập địa chỉ giao hàng.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }

                    Card {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            cartState.items.forEach { item ->
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("${item.product.name} x${item.quantity}",
                                        modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                                    Text(formatVND(item.product.price * item.quantity),
                                        style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                }
                            }
                            HorizontalDivider()
                            // Subtotal
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Tạm tính", style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(formatVND(cartViewModel.subtotal), style = MaterialTheme.typography.bodyMedium)
                            }
                            // Discount line — only when a coupon is applied
                            if (cartState.discountAmount > 0L) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(
                                        "Giảm giá" + (cartState.appliedCoupon?.code?.let { " ($it)" } ?: ""),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        "-" + formatVND(cartState.discountAmount),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                            HorizontalDivider()
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Tổng cộng", fontWeight = FontWeight.Bold)
                                Text(formatVND(cartViewModel.finalTotal), fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Thanh toán", style = MaterialTheme.typography.bodySmall)
                                Text(PAYMENT_METHODS.find { it.first == selectedPayment }?.third ?: selectedPayment,
                                    style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }

                    // VietQR display — generated locally from MerchantConfig
                    if (selectedPayment == "vietqr") {
                        // Transient reference shown in the bank-app note field.
                        // We pre-compute it so the QR stays stable while the user is on this screen.
                        val orderRef = remember {
                            "QRH" + System.currentTimeMillis().toString().takeLast(8)
                        }
                        val vietQrString = remember(orderRef) {
                            VietQR.build(
                                bankBin = MerchantConfig.BANK_BIN,
                                accountNumber = MerchantConfig.ACCOUNT_NUMBER,
                                amount = cartViewModel.finalTotal,
                                description = orderRef
                            )
                        }
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    "Quét mã VietQR bằng app ngân hàng",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                QrCodeImage(value = vietQrString, modifier = Modifier.size(240.dp))
                                Spacer(modifier = Modifier.height(12.dp))
                                BankInfoRow("Ngân hàng", MerchantConfig.BANK_NAME)
                                BankInfoRow("Số tài khoản", MerchantConfig.ACCOUNT_NUMBER)
                                BankInfoRow("Chủ tài khoản", MerchantConfig.ACCOUNT_NAME)
                                BankInfoRow("Số tiền", formatVND(cartViewModel.finalTotal))
                                BankInfoRow("Nội dung", orderRef)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "⚠️ Vui lòng nhập đúng nội dung \"$orderRef\" khi chuyển khoản để chúng tôi đối chiếu đơn hàng.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }

                    Button(
                        onClick = {
                            cartViewModel.setPaymentMethod(selectedPayment)
                            cartViewModel.placeOrder { success, _ ->
                                if (success) showSuccessDialog = true
                            }
                        },
                        enabled = !cartState.isPlacingOrder && hasAddress,
                        modifier = Modifier.fillMaxWidth().height(52.dp)
                    ) {
                        if (cartState.isPlacingOrder) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
                        } else {
                            Text(
                                if (selectedPayment == "vietqr")
                                    "TÔI ĐÃ CHUYỂN KHOẢN — XÁC NHẬN"
                                else
                                    "XÁC NHẬN ĐẶT HÀNG",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BankInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ProfileSelectCard(profile: Profile, selected: Boolean, onClick: () -> Unit) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        border = BorderStroke(
            if (selected) 2.dp else 1.dp,
            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
        )
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(if (profile.profileType == "pet") Icons.Default.Pets else Icons.Default.Person,
                contentDescription = null,
                tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(36.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(profile.fullName.ifBlank { "Chưa đặt tên" }, fontWeight = FontWeight.Bold)
                Text(if (profile.profileType == "pet") "Thú cưng" else "Người",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (selected) Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
        }
    }
}

// ── Order Success Dialog ───────────────────────────────────────────────────────
@Composable
private fun OrderSuccessDialog(tags: List<QrTag>, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(40.dp)) },
        title = { Text("Mua Hàng Thành Công! 🎉", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Đơn hàng đã xác nhận. Đây là mã QR và PIN của sản phẩm:",
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                tags.forEachIndexed { i, tag ->
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Sản phẩm ${i + 1}", fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                            QrCodeImage(
                                value = com.qrhealthcare.app.data.api.ApiClient.publicProfileUrl(tag.tagCode),
                                modifier = Modifier.size(160.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column {
                                    Text("Mã Tag", style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(tag.tagCode, fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyLarge)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("PIN", style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(tag.pin, fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.primary)
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("⚠️ Lưu lại mã PIN này! Bạn cần nó để liên kết với hồ sơ.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) { Text("Đóng") }
        }
    )
}
