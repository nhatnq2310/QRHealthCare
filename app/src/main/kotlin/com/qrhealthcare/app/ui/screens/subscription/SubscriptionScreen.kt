package com.qrhealthcare.app.ui.screens.subscription

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.qrhealthcare.app.data.payment.MerchantConfig
import com.qrhealthcare.app.data.payment.VietQR
import com.qrhealthcare.app.ui.components.QrCodeImage
import com.qrhealthcare.app.ui.components.formatVND
import com.qrhealthcare.app.ui.viewmodel.SubscriptionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionScreen(
    navController: NavController,
    viewModel: SubscriptionViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var showPaymentStep by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { viewModel.load() }

    val orderRef = remember { "SUB" + System.currentTimeMillis().toString().takeLast(8) }
    LaunchedEffect(orderRef) { viewModel.setPaymentRef(orderRef) }

    if (state.renewSuccess) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissRenewSuccess(); navController.popBackStack() },
            icon = { Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(40.dp)) },
            title = { Text("Đăng Ký Thành Công! 🎉", fontWeight = FontWeight.Bold) },
            text = { Text("Gói duy trì lưu trữ hồ sơ của bạn đã được kích hoạt. Hồ sơ của bạn (nếu đã bị khóa) đã được mở lại.") },
            confirmButton = {
                Button(onClick = { viewModel.dismissRenewSuccess(); navController.popBackStack() }) { Text("Đóng") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gói Duy Trì Hồ Sơ") },
                navigationIcon = {
                    IconButton(onClick = { if (showPaymentStep) showPaymentStep = false else navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())) {

            // ── Current status ────────────────────────────────────────────────
            state.subscription?.let { sub ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = when (sub.status) {
                            "active" -> MaterialTheme.colorScheme.primaryContainer
                            "trial" -> MaterialTheme.colorScheme.secondaryContainer
                            else -> MaterialTheme.colorScheme.errorContainer
                        }
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            when (sub.status) {
                                "trial" -> "🎁 Đang dùng thử miễn phí"
                                "active" -> "✅ Đang hoạt động — ${planLabel(sub.plan)}"
                                "expired" -> "⚠️ Đã hết hạn — Hồ sơ đã bị khóa"
                                else -> "❌ Đã hủy đăng ký"
                            },
                            fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        if (sub.status == "trial" || sub.status == "active") {
                            Text("Còn lại ${sub.daysRemaining} ngày", style = MaterialTheme.typography.bodyMedium)
                        }
                        Text("Số hồ sơ tối đa: ${sub.totalSlots}", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (sub.status == "expired" || sub.status == "cancelled") {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Hồ sơ của bạn hiện đang ở chế độ riêng tư (chỉ hiển thị thông tin cơ bản) cho đến khi bạn gia hạn.",
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }

            if (!showPaymentStep) {
                // ── Plan selection ──────────────────────────────────────────────
                Column(modifier = Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Chọn gói duy trì:", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)

                    PlanCard(
                        title = "Gói Tháng", price = "20.000đ / tháng",
                        description = "Duy trì lưu trữ tối đa 5 hồ sơ",
                        selected = state.selectedPlan == "monthly",
                        onClick = { viewModel.selectPlan("monthly") }
                    )

                    PlanCard(
                        title = "Gói Linh Hoạt", price = "Từ 20.000đ / tháng",
                        description = "Muốn thêm hồ sơ? Chỉ 5.000đ / hồ sơ thêm mỗi tháng",
                        selected = state.selectedPlan == "flexible",
                        onClick = { viewModel.selectPlan("flexible") }
                    ) {
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = if (state.extraProfilesInput == 0) "" else state.extraProfilesInput.toString(),
                            onValueChange = { txt -> viewModel.setExtraProfiles(txt.filter(Char::isDigit).toIntOrNull() ?: 0) },
                            label = { Text("Số hồ sơ muốn thêm") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Tổng cộng: ${formatVND(com.qrhealthcare.app.data.model.SubscriptionPricing.computeAmount("monthly", state.extraProfilesInput))} / tháng",
                            fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary
                        )
                    }

                    PlanCard(
                        title = "Gói Năm", price = "199.000đ / năm",
                        description = "Tiết kiệm hơn — duy trì tối đa 5 hồ sơ trong 1 năm. Thêm hồ sơ chỉ 4.000đ/tháng (48.000đ/năm) mỗi hồ sơ",
                        selected = state.selectedPlan == "yearly",
                        onClick = { viewModel.selectPlan("yearly") }
                    ) {
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = if (state.extraProfilesInput == 0) "" else state.extraProfilesInput.toString(),
                            onValueChange = { txt -> viewModel.setExtraProfiles(txt.filter(Char::isDigit).toIntOrNull() ?: 0) },
                            label = { Text("Số hồ sơ muốn thêm") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Tổng cộng: ${formatVND(com.qrhealthcare.app.data.model.SubscriptionPricing.computeAmount("yearly", state.extraProfilesInput))} / năm",
                            fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Số tiền cần thanh toán: ${formatVND(state.computedAmount)}",
                        style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold
                    )

                    Button(
                        onClick = { showPaymentStep = true },
                        modifier = Modifier.fillMaxWidth().height(50.dp)
                    ) { Text("Tiếp Theo →", fontWeight = FontWeight.Bold) }

                    if (state.subscription?.status == "active") {
                        OutlinedButton(
                            onClick = { viewModel.cancel { _, _ -> } },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) { Text("Hủy Đăng Ký") }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            } else {
                // ── Payment step (VietQR only) ──────────────────────────────────
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Thanh Toán Qua VietQR", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                    val vietQrString = remember(state.computedAmount, orderRef) {
                        VietQR.build(
                            bankBin = MerchantConfig.BANK_BIN,
                            accountNumber = MerchantConfig.ACCOUNT_NUMBER,
                            amount = state.computedAmount,
                            description = orderRef
                        )
                    }

                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Quét mã VietQR bằng app ngân hàng", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(12.dp))
                            QrCodeImage(value = vietQrString, modifier = Modifier.size(240.dp))
                            Spacer(modifier = Modifier.height(12.dp))
                            InfoRow("Ngân hàng", MerchantConfig.BANK_NAME)
                            InfoRow("Số tài khoản", MerchantConfig.ACCOUNT_NUMBER)
                            InfoRow("Chủ tài khoản", MerchantConfig.ACCOUNT_NAME)
                            InfoRow("Số tiền", formatVND(state.computedAmount))
                            InfoRow("Nội dung", orderRef)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "⚠️ Vui lòng nhập đúng nội dung \"$orderRef\" khi chuyển khoản để chúng tôi đối chiếu.",
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error
                            )
                        }
                    }

                    Button(
                        onClick = { viewModel.confirmPayment { _, _ -> } },
                        enabled = !state.isProcessing,
                        modifier = Modifier.fillMaxWidth().height(52.dp)
                    ) {
                        if (state.isProcessing) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
                        } else {
                            Text("TÔI ĐÃ CHUYỂN KHOẢN — XÁC NHẬN", fontWeight = FontWeight.Bold)
                        }
                    }

                    state.error?.let {
                        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

private fun planLabel(plan: String) = when (plan) {
    "monthly" -> "Gói Tháng"
    "flexible" -> "Gói Linh Hoạt"
    "yearly" -> "Gói Năm"
    else -> plan
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun PlanCard(
    title: String,
    price: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit,
    extraContent: (@Composable ColumnScope.() -> Unit)? = null
) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = selected, onClick = onClick)
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, fontWeight = FontWeight.Bold)
                    Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(price, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
            if (selected) extraContent?.invoke(this)
        }
    }
}
