package com.qrhealthcare.app.ui.screens.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import com.qrhealthcare.app.ui.viewmodel.AuthViewModel
import kotlinx.coroutines.launch

@Composable
fun AccountSettingsScreen(
    onLogout: () -> Unit,
    onOrderHistory: () -> Unit = {},
    onUserGuide: () -> Unit = {},
    onSubscription: () -> Unit = {},
    viewModel: AuthViewModel = com.qrhealthcare.app.ui.util.activityViewModel(),
    subscriptionViewModel: com.qrhealthcare.app.ui.viewmodel.SubscriptionViewModel = hiltViewModel()
) {
    val state by viewModel.authState.collectAsState()
    val subState by subscriptionViewModel.state.collectAsState()
    val context = LocalContext.current
    var showSupportContactDialog by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { subscriptionViewModel.load() }
    var showLogoutConfirm by remember { mutableStateOf(false) }
    var showAddressDialog by remember { mutableStateOf(false) }
    var showChangePasswordDialog by remember { mutableStateOf(false) }
    var showPrivacyPolicyDialog by remember { mutableStateOf(false) }
    var showTermsDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    if (showAddressDialog) {
        AddressEditDialog(
            current = state.address,
            currentPhone = state.phone,
            currentCity = state.city,
            isSaving = state.isLoading,
            onSave = { newAddress, newPhone, newCity ->
                viewModel.updateAddress(newAddress, newPhone, newCity) { success, err ->
                    showAddressDialog = false
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            if (success) "Đã cập nhật thông tin giao hàng" else (err ?: "Cập nhật thất bại")
                        )
                    }
                }
            },
            onDismiss = { showAddressDialog = false }
        )
    }

    if (showChangePasswordDialog) {
        ChangePasswordDialog(
            onSave = { current, new, done ->
                viewModel.changePassword(current, new) { success, msg ->
                    if (success) {
                        showChangePasswordDialog = false
                        scope.launch { snackbarHostState.showSnackbar(msg ?: "Đổi mật khẩu thành công") }
                    }
                    done(success, msg)
                }
            },
            onDismiss = { showChangePasswordDialog = false }
        )
    }

    if (showLogoutConfirm) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirm = false },
            icon = { Icon(Icons.Default.Logout, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Đăng Xuất?") },
            text = { Text("Bạn có chắc muốn đăng xuất khỏi tài khoản không?") },
            confirmButton = {
                Button(
                    onClick = { showLogoutConfirm = false; onLogout() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Đăng Xuất") }
            },
            dismissButton = { TextButton(onClick = { showLogoutConfirm = false }) { Text("Hủy") } }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Tài Khoản", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

        // ── Avatar section ───────────────────────────────────────────────────
        Card(shape = RoundedCornerShape(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(56.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = state.fullName.firstOrNull()?.uppercase() ?: "?",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        state.fullName.ifBlank { "Người dùng" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        state.email,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (state.userRole == "admin") {
                        Spacer(modifier = Modifier.height(4.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                "⚡ Admin",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // ── Account info ─────────────────────────────────────────────────────
        Text("Thông Tin Tài Khoản", style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

        Card(shape = RoundedCornerShape(12.dp)) {
            Column {
                InfoRow(Icons.Default.Email, "Email", state.email)
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                InfoRow(Icons.Default.Person, "Họ và Tên", state.fullName.ifBlank { "Chưa cập nhật" })
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                // Address — tap to edit
                EditableInfoRow(
                    icon = Icons.Default.LocationOn,
                    label = "Địa Chỉ",
                    value = state.address.ifBlank { "Chưa cập nhật — bấm để thêm" },
                    isPlaceholder = state.address.isBlank(),
                    onClick = { showAddressDialog = true }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                // Phone — tap to edit (same dialog as address)
                EditableInfoRow(
                    icon = Icons.Default.Phone,
                    label = "Số Điện Thoại",
                    value = state.phone.ifBlank { "Chưa cập nhật — bấm để thêm" },
                    isPlaceholder = state.phone.isBlank(),
                    onClick = { showAddressDialog = true }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                InfoRow(Icons.Default.Shield, "Vai Trò", if (state.userRole == "admin") "Quản Trị Viên" else "Người Dùng")
            }
        }

        // ── App info ─────────────────────────────────────────────────────────
        Text("Ứng Dụng", style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

        Card(shape = RoundedCornerShape(12.dp)) {
            Column {
                InfoRow(Icons.Default.Info, "Phiên Bản", "1.0.0 (Beta)")
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                InfoRow(Icons.Default.Security, "Bảo Mật", "Dữ liệu mã hóa an toàn")
            }
        }

        // ── Subscription ─────────────────────────────────────────────────────
        Text("Gói Duy Trì Hồ Sơ", style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

        Card(shape = RoundedCornerShape(12.dp)) {
            Column {
                val sub = subState.subscription
                LinkRow(
                    Icons.Default.WorkspacePremium,
                    "Gói Duy Trì Lưu Trữ Hồ Sơ",
                    onSubscription
                )
                if (sub != null) {
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            when (sub.status) {
                                "trial" -> "Dùng thử — còn ${sub.daysRemaining} ngày"
                                "active" -> "Đang hoạt động — còn ${sub.daysRemaining} ngày"
                                "expired" -> "Đã hết hạn — hồ sơ đang bị khóa"
                                else -> "Đã hủy — hồ sơ đang bị khóa"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = if (sub.status == "expired" || sub.status == "cancelled")
                                MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // ── Orders ───────────────────────────────────────────────────────────
        Text("Đơn Hàng", style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

        Card(shape = RoundedCornerShape(12.dp)) {
            LinkRow(Icons.Default.ReceiptLong, "Lịch Sử Đơn Hàng", onOrderHistory)
        }

        // ── Security ─────────────────────────────────────────────────────────
        Text("Bảo Mật", style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

        Card(shape = RoundedCornerShape(12.dp)) {
            LinkRow(Icons.Default.Lock, "Đổi Mật Khẩu") { showChangePasswordDialog = true }
        }

        // ── Links ────────────────────────────────────────────────────────────
        Text("Hỗ Trợ", style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

        Card(shape = RoundedCornerShape(12.dp)) {
            Column {
                LinkRow(Icons.Default.Help, "Hướng Dẫn Sử Dụng", onUserGuide)
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                LinkRow(Icons.Default.Mail, "Liên Hệ Hỗ Trợ") { showSupportContactDialog = true }
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                LinkRow(Icons.Default.PrivacyTip, "Chính Sách Bảo Mật") { showPrivacyPolicyDialog = true }
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                LinkRow(Icons.Default.Gavel, "Điều Khoản Dịch Vụ") { showTermsDialog = true }
            }
        }

        if (showSupportContactDialog) {
            SupportContactDialog(
                onDismiss = { showSupportContactDialog = false },
                onOpenFanpage = {
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.facebook.com/profile.php?id=61587210686575"))
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        context.startActivity(intent)
                    } catch (_: Exception) { /* no browser/FB app — silently ignore */ }
                    showSupportContactDialog = false
                },
                onSendEmail = {
                    try {
                        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:truongmtse171420@gmail.com"))
                        intent.putExtra(Intent.EXTRA_SUBJECT, "Hỗ trợ QR Healthcare")
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        context.startActivity(intent)
                    } catch (_: Exception) { /* no email app — silently ignore */ }
                    showSupportContactDialog = false
                }
            )
        }

        if (showPrivacyPolicyDialog) {
            PolicyDialog(title = "Chính Sách Bảo Mật", body = PRIVACY_POLICY_TEXT, onDismiss = { showPrivacyPolicyDialog = false })
        }
        if (showTermsDialog) {
            PolicyDialog(title = "Điều Khoản Dịch Vụ", body = TERMS_OF_SERVICE_TEXT, onDismiss = { showTermsDialog = false })
        }

        // ── Danger zone ──────────────────────────────────────────────────────
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = { showLogoutConfirm = true },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(10.dp)
        ) {
            Icon(Icons.Default.Logout, null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Đăng Xuất", fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "© 2026 MyQRHealthcare. Made with ❤️ in Việt Nam.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        Spacer(modifier = Modifier.height(16.dp))
    }
    // Snackbar overlay — sits at the bottom inside the wrapping Box.
    SnackbarHost(
        hostState = snackbarHostState,
        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp)
    )
    } // close wrapping Box
}

@Composable
private fun InfoRow(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold,
            maxLines = 1)
    }
}

/**
 * Like InfoRow but tappable, with a trailing chevron, used for fields the
 * user can edit (currently just the shipping address).
 */
@Composable
private fun EditableInfoRow(
    icon: ImageVector,
    label: String,
    value: String,
    isPlaceholder: Boolean,
    onClick: () -> Unit
) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(0.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isPlaceholder) FontWeight.Normal else FontWeight.SemiBold,
                color = if (isPlaceholder) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface,
                maxLines = 2
            )
        }
        Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun AddressEditDialog(
    current: String,
    currentPhone: String,
    currentCity: String,
    isSaving: Boolean,
    onSave: (address: String, phone: String, city: String) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember(current) { mutableStateOf(current) }
    var phone by remember(currentPhone) { mutableStateOf(currentPhone) }
    var city by remember(currentCity) { mutableStateOf(currentCity) }
    fun phoneOk(p: String) = p.trim().length >= 8 && p.trim().all { it.isDigit() || it in "+ -()" }
    AlertDialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        icon = { Icon(Icons.Default.LocationOn, null, tint = MaterialTheme.colorScheme.primary) },
        title = { Text("Thông Tin Giao Hàng", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Thông tin này sẽ tự điền khi bạn đặt hàng.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Địa chỉ") },
                    placeholder = { Text("VD: 123 Lê Lợi, Bến Nghé, Quận 1") },
                    minLines = 2,
                    maxLines = 4,
                    enabled = !isSaving,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Số điện thoại") },
                    placeholder = { Text("VD: 0901234567") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    enabled = !isSaving,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = city,
                    onValueChange = { city = it },
                    label = { Text("Tỉnh / Thành phố") },
                    placeholder = { Text("VD: TP. Hồ Chí Minh") },
                    singleLine = true,
                    enabled = !isSaving,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(text, phone, city) },
                enabled = !isSaving && text.trim().length >= 5 && phoneOk(phone)
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else Text("Lưu")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSaving) { Text("Hủy") }
        }
    )
}

@Composable
private fun LinkRow(icon: ImageVector, label: String, onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(0.dp)
    ) {
        Icon(icon, null, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface)
        Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ChangePasswordDialog(
    onSave: (current: String, new: String, done: (Boolean, String?) -> Unit) -> Unit,
    onDismiss: () -> Unit
) {
    var current by remember { mutableStateOf("") }
    var newPass by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var show by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { if (!saving) onDismiss() },
        icon = { Icon(Icons.Default.Lock, null, tint = MaterialTheme.colorScheme.primary) },
        title = { Text("Đổi Mật Khẩu") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = current, onValueChange = { current = it },
                    label = { Text("Mật khẩu hiện tại") }, singleLine = true,
                    visualTransformation = if (show) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        IconButton(onClick = { show = !show }) {
                            Icon(if (show) Icons.Default.VisibilityOff else Icons.Default.Visibility, null)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = newPass, onValueChange = { newPass = it },
                    label = { Text("Mật khẩu mới") }, singleLine = true,
                    visualTransformation = if (show) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = confirm, onValueChange = { confirm = it },
                    label = { Text("Xác nhận mật khẩu mới") }, singleLine = true,
                    visualTransformation = if (show) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth()
                )
                if (error != null) {
                    Text(error!!, color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                enabled = !saving,
                onClick = {
                    error = when {
                        current.isBlank() -> "Nhập mật khẩu hiện tại"
                        newPass.length < 6 -> "Mật khẩu mới phải có ít nhất 6 ký tự"
                        newPass != confirm -> "Mật khẩu xác nhận không khớp"
                        newPass == current -> "Mật khẩu mới phải khác mật khẩu cũ"
                        else -> null
                    }
                    if (error != null) return@Button
                    saving = true
                    onSave(current, newPass) { ok, msg ->
                        saving = false
                        if (!ok) error = msg
                    }
                }
            ) {
                if (saving) CircularProgressIndicator(modifier = Modifier.size(18.dp),
                    color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                else Text("Lưu")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !saving) { Text("Hủy") } }
    )
}

// ═══════════════════════════════════════════════════════════════════════════════
// PRIVACY POLICY & TERMS OF SERVICE — shown in-app as a popup when tapped
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun PolicyDialog(title: String, body: String, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.95f).fillMaxHeight(0.85f),
            shape = RoundedCornerShape(16.dp),
            tonalElevation = 4.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, contentDescription = "Đóng") }
                }
                HorizontalDivider()
                Column(
                    modifier = Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()).padding(16.dp)
                ) {
                    Text(body, style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(24.dp))
                }
                HorizontalDivider()
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth().padding(16.dp)
                ) { Text("Đã Hiểu") }
            }
        }
    }
}

// NOTE: this is a working draft tailored to what this app actually does
// (health-profile creation, QR-tag linking, VietQR payments, the profile
// storage subscription plan). It is NOT a substitute for review by a
// Vietnamese lawyer before publishing — in particular the health-data and
// payment-processing sections should be checked against Nghị định
// 13/2023/NĐ-CP (bảo vệ dữ liệu cá nhân) before this goes live.
private const val PRIVACY_POLICY_TEXT = """
Cập nhật lần cuối: 07/07/2026

1. THÔNG TIN CHÚNG TÔI THU THẬP

Khi bạn sử dụng QR Healthcare, chúng tôi thu thập:
• Thông tin tài khoản: email, họ tên, số điện thoại, địa chỉ giao hàng.
• Thông tin hồ sơ y tế: họ tên, ngày sinh, nhóm máu, dị ứng, bệnh nền, liên hệ khẩn cấp, và các tài liệu y tế bạn tải lên (nếu có).
• Thông tin đơn hàng: sản phẩm đã mua, địa chỉ giao hàng, phương thức thanh toán (VietQR hoặc COD).
• Thông tin gói duy trì hồ sơ: trạng thái đăng ký, lịch sử thanh toán, ngày hết hạn.
• Dữ liệu sử dụng: các bước bạn thực hiện trong quá trình đặt hàng, dùng để cải thiện trải nghiệm và giảm tỷ lệ bỏ dở giỏ hàng.

2. MỤC ĐÍCH SỬ DỤNG

Chúng tôi sử dụng thông tin trên để:
• Tạo và hiển thị hồ sơ y tế của bạn khi có người quét mã QR (trong trường hợp khẩn cấp).
• Xử lý đơn hàng, giao hàng và xác nhận thanh toán.
• Quản lý gói duy trì lưu trữ hồ sơ và gửi thông báo nhắc gia hạn.
• Liên hệ hỗ trợ khi bạn cần trợ giúp.

3. HỒ SƠ Y TẾ — THÔNG TIN NHẠY CẢM

Thông tin y tế của bạn được coi là dữ liệu cá nhân nhạy cảm. Bạn có toàn quyền kiểm soát:
• Bạn quyết định hồ sơ ở chế độ công khai hay riêng tư, và có thể chọn ẩn từng mục thông tin cụ thể.
• Khi hồ sơ ở chế độ riêng tư, người quét mã QR chỉ thấy các thông tin thiết yếu cho cấp cứu (nhóm máu, dị ứng nghiêm trọng, liên hệ khẩn cấp).
• Nếu gói duy trì lưu trữ hồ sơ hết hạn mà chưa gia hạn, hồ sơ của bạn sẽ tạm thời chuyển sang chế độ riêng tư cho đến khi bạn thanh toán lại — dữ liệu của bạn không bị xóa, chỉ tạm ẩn.

4. THANH TOÁN

Chúng tôi không lưu trữ thông tin tài khoản ngân hàng của bạn. Thanh toán qua VietQR được thực hiện trực tiếp qua ứng dụng ngân hàng của bạn; chúng tôi chỉ nhận được xác nhận giao dịch và nội dung chuyển khoản để đối chiếu đơn hàng.

5. LƯU TRỮ DỮ LIỆU

Dữ liệu được lưu trữ trên hệ thống máy chủ (MongoDB) có kiểm soát truy cập. Chúng tôi lưu trữ thông tin của bạn trong suốt thời gian bạn sử dụng dịch vụ, và có thể xóa hồ sơ theo yêu cầu của bạn.

6. CHIA SẺ THÔNG TIN

Chúng tôi không bán thông tin cá nhân của bạn cho bên thứ ba. Thông tin hồ sơ y tế chỉ được hiển thị công khai (một phần hoặc toàn bộ, tùy cài đặt riêng tư của bạn) khi có người quét mã QR tương ứng.

7. QUYỀN CỦA BẠN

Bạn có quyền: xem, chỉnh sửa, hoặc xóa hồ sơ của mình bất cứ lúc nào; yêu cầu xuất toàn bộ dữ liệu cá nhân; đóng tài khoản.

8. LIÊN HỆ

Nếu có câu hỏi về chính sách này, vui lòng liên hệ qua mục "Liên Hệ Hỗ Trợ" trong ứng dụng.
"""

private const val TERMS_OF_SERVICE_TEXT = """
Cập nhật lần cuối: 07/07/2026

1. CHẤP NHẬN ĐIỀU KHOẢN

Bằng việc tạo tài khoản và sử dụng QR Healthcare, bạn đồng ý với các điều khoản dưới đây.

2. TÀI KHOẢN VÀ HỒ SƠ

• Bạn chịu trách nhiệm về tính chính xác của thông tin hồ sơ y tế mà bạn nhập, vì thông tin này có thể được sử dụng trong tình huống khẩn cấp.
• Mỗi tài khoản được lưu trữ tối đa 5 hồ sơ miễn phí. Muốn thêm hồ sơ, bạn cần đăng ký Gói Linh Hoạt hoặc nâng cấp gói duy trì.

3. GÓI DUY TRÌ LƯU TRỮ HỒ SƠ

• Mỗi tài khoản được dùng thử miễn phí 30 ngày kể từ khi tạo hồ sơ đầu tiên.
• Sau thời gian dùng thử, cần đăng ký một trong các gói: Gói Tháng (20.000đ/tháng), Gói Linh Hoạt (20.000đ + 5.000đ/hồ sơ thêm/tháng), hoặc Gói Năm (199.000đ/năm, hồ sơ thêm chỉ 48.000đ/năm).
• Nếu không gia hạn đúng hạn, hồ sơ của bạn sẽ chuyển sang chế độ riêng tư (chỉ hiển thị thông tin cơ bản khi quét QR) cho đến khi bạn thanh toán lại.
• Bạn có thể hủy gói bất cứ lúc nào; việc hủy có hiệu lực ngay và hồ sơ sẽ chuyển sang chế độ riêng tư.

4. ĐẶT HÀNG VÀ THANH TOÁN

• Đơn hàng được xác nhận sau khi bạn hoàn tất bước thanh toán (VietQR hoặc thanh toán khi nhận hàng - COD).
• Thanh toán qua VietQR yêu cầu bạn nhập đúng nội dung chuyển khoản để chúng tôi đối chiếu và xử lý đơn hàng.
• Sản phẩm được giao trong vòng 24 giờ kể từ khi đặt hàng, tùy khu vực.

5. SỬ DỤNG MÃ QR

• Mỗi mã QR gắn với một hồ sơ cụ thể. Việc chia sẻ mã QR đồng nghĩa với việc cho phép người khác xem thông tin hồ sơ (theo mức độ riêng tư bạn đã cài đặt).
• Bạn chịu trách nhiệm bảo quản sticker/thẻ/tag QR đã mua.

6. GIỚI HẠN TRÁCH NHIỆM

Thông tin trong hồ sơ y tế chỉ mang tính chất hỗ trợ tham khảo trong tình huống khẩn cấp, không thay thế chẩn đoán hoặc tư vấn y tế chuyên môn. Chúng tôi không chịu trách nhiệm về các quyết định y tế được đưa ra dựa trên thông tin hiển thị từ hồ sơ.

7. CHẤM DỨT DỊCH VỤ

Chúng tôi có quyền tạm ngưng hoặc chấm dứt tài khoản vi phạm điều khoản sử dụng, bao gồm hành vi gian lận thanh toán hoặc cung cấp thông tin sai sự thật gây ảnh hưởng đến an toàn của người khác.

8. THAY ĐỔI ĐIỀU KHOẢN

Chúng tôi có thể cập nhật điều khoản này theo thời gian. Phiên bản mới nhất luôn có sẵn trong mục Tài Khoản của ứng dụng.
"""

@Composable
private fun SupportContactDialog(
    onDismiss: () -> Unit,
    onOpenFanpage: () -> Unit,
    onSendEmail: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Mail, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp)) },
        title = { Text("Liên Hệ Hỗ Trợ", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Chọn cách bạn muốn liên hệ với chúng tôi:", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth().clickable(onClick = onOpenFanpage).padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.OpenInNew, contentDescription = null, tint = Color(0xFF1877F2))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Fanpage Facebook", fontWeight = FontWeight.SemiBold)
                        Text("Nhắn tin trực tiếp qua Facebook", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                HorizontalDivider()
                Row(
                    modifier = Modifier.fillMaxWidth().clickable(onClick = onSendEmail).padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Email, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Email", fontWeight = FontWeight.SemiBold)
                        Text("truongmtse171420@gmail.com", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Đóng") } }
    )
}
