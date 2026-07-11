package com.qrhealthcare.app.ui.screens.profile

import android.Manifest
import android.content.Intent
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.qrhealthcare.app.ui.viewmodel.FamilyNotifyViewModel

/**
 * Two audiences share this one screen:
 *  - The PROFILE OWNER reaches it with [profileId] already known (button on
 *    their own profile card) — they see a shareable code + Share button to
 *    send to a family member.
 *  - A FAMILY MEMBER reaches it with no profileId (generic entry point from
 *    Account Settings) — they paste in the code the owner sent them, then
 *    tap "Đăng Ký" to register THIS device for scan alerts.
 *
 * Either way, tapping "Đăng Ký Thiết Bị Này" requests an FCM token and
 * registers it server-side. Requires the app's own Firebase setup to
 * actually deliver notifications (see app/FCM_SETUP.md) — the button fails
 * gracefully with a clear message otherwise.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FamilyNotifyScreen(
    profileId: String = "",
    onBack: () -> Unit,
    viewModel: FamilyNotifyViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    var codeInput by remember { mutableStateOf(profileId) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* granted or not — registration proceeds either way; without it, pushes just won't show visually */
        viewModel.registerThisDevice()
    }

    fun startRegistration() {
        viewModel.setProfileId(codeInput.trim())
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            viewModel.registerThisDevice()
        }
    }

    LaunchedEffect(profileId) {
        if (profileId.isNotBlank()) viewModel.setProfileId(profileId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Thông Báo Người Thân") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Quay lại") }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                shape = RoundedCornerShape(12.dp)) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("🔔 Tính năng dành cho gói duy trì hồ sơ", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "Khi có ai đó quét mã QR của hồ sơ này, người thân đã đăng ký sẽ nhận được thông báo ngay trên điện thoại — " +
                            "kèm tùy chọn xem vị trí người quét (nếu họ đồng ý chia sẻ) và xem đầy đủ thông tin hồ sơ. " +
                            "Chỉ hoạt động khi gói duy trì đang active.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (profileId.isNotBlank()) {
                // ── Owner view: share this profile's code ──────────────────────
                Text("Chia sẻ mã hồ sơ này cho người thân của bạn:", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Card(shape = RoundedCornerShape(10.dp)) {
                    Row(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            if (state.profileName.isNotBlank()) Text(state.profileName, style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(profileId, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        }
                        IconButton(onClick = { clipboard.setText(AnnotatedString(profileId)) }) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Sao chép")
                        }
                    }
                }
                Button(
                    onClick = {
                        val shareText = "Hãy tải app QR Healthcare, vào Cài Đặt → Thông Báo Người Thân, " +
                            "nhập mã: $profileId để nhận thông báo khi hồ sơ y tế của tôi bị quét."
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, shareText)
                        }
                        context.startActivity(Intent.createChooser(intent, "Chia sẻ mã hồ sơ"))
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Share, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Chia Sẻ Mã Cho Người Thân")
                }
                HorizontalDivider()
                Text("Hoặc đăng ký chính điện thoại này để nhận thông báo:", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                // ── Family member view: enter the code they were given ─────────
                Text("Nhập mã hồ sơ mà người thân đã gửi cho bạn:", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = codeInput,
                    onValueChange = { codeInput = it },
                    label = { Text("Mã hồ sơ") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (state.registered) {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    shape = RoundedCornerShape(10.dp)) {
                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(10.dp))
                        Text("Đã đăng ký! Bạn sẽ nhận thông báo khi hồ sơ này bị quét.", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            } else {
                Button(
                    onClick = { startRegistration() },
                    enabled = !state.isRegistering && codeInput.isNotBlank(),
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    if (state.isRegistering) CircularProgressIndicator(modifier = Modifier.size(18.dp), color = MaterialTheme.colorScheme.onPrimary)
                    else Text("Đăng Ký Thiết Bị Này", fontWeight = FontWeight.Bold)
                }
            }

            state.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
