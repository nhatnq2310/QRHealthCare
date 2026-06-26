package com.qrhealthcare.app.ui.screens.settings

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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.qrhealthcare.app.ui.viewmodel.AuthViewModel
import kotlinx.coroutines.launch

@Composable
fun AccountSettingsScreen(
    onLogout: () -> Unit,
    onOrderHistory: () -> Unit = {},
    onUserGuide: () -> Unit = {},
    viewModel: AuthViewModel = com.qrhealthcare.app.ui.util.activityViewModel()
) {
    val state by viewModel.authState.collectAsState()
    var showLogoutConfirm by remember { mutableStateOf(false) }
    var showAddressDialog by remember { mutableStateOf(false) }
    var showChangePasswordDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    if (showAddressDialog) {
        AddressEditDialog(
            current = state.address,
            isSaving = state.isLoading,
            onSave = { newAddress ->
                viewModel.updateAddress(newAddress) { success, err ->
                    showAddressDialog = false
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            if (success) "Đã cập nhật địa chỉ" else (err ?: "Cập nhật thất bại")
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
                LinkRow(Icons.Default.Mail, "Liên Hệ Hỗ Trợ") {}
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                LinkRow(Icons.Default.PrivacyTip, "Chính Sách Bảo Mật") {}
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                LinkRow(Icons.Default.Gavel, "Điều Khoản Dịch Vụ") {}
            }
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
    isSaving: Boolean,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember(current) { mutableStateOf(current) }
    AlertDialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        icon = { Icon(Icons.Default.LocationOn, null, tint = MaterialTheme.colorScheme.primary) },
        title = { Text("Địa Chỉ Nhận Hàng", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Nhập địa chỉ đầy đủ (số nhà, đường, phường, quận, thành phố).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Địa chỉ") },
                    placeholder = { Text("VD: 123 Lê Lợi, Bến Nghé, Quận 1, TP.HCM") },
                    minLines = 2,
                    maxLines = 5,
                    enabled = !isSaving,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(text) },
                enabled = !isSaving && text.trim().length >= 5
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
