package com.qrhealthcare.app.ui.screens.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.qrhealthcare.app.data.model.Profile
import com.qrhealthcare.app.ui.navigation.Routes
import com.qrhealthcare.app.ui.viewmodel.AuthViewModel
import com.qrhealthcare.app.ui.viewmodel.ProfileViewModel

@Composable
fun ManageProfileScreen(
    navController: NavController,
    viewModel: ProfileViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = com.qrhealthcare.app.ui.util.activityViewModel(),
    subscriptionViewModel: com.qrhealthcare.app.ui.viewmodel.SubscriptionViewModel = hiltViewModel()
) {
    val state by viewModel.listState.collectAsState()
    val authState by authViewModel.authState.collectAsState()
    val subState by subscriptionViewModel.state.collectAsState()
    var profileTypeChoice by remember { mutableStateOf("human") }
    var showLinkDialog by remember { mutableStateOf(false) }
    var linkTargetProfileId by remember { mutableStateOf("") }
    var showSubscriptionPopup by remember { mutableStateOf(false) }
    // For the "Xem QR" dialog
    var qrsDialogProfile by remember { mutableStateOf<Profile?>(null) }
    var qrsDialogTags by remember { mutableStateOf<List<com.qrhealthcare.app.data.model.QrTag>>(emptyList()) }
    var qrsDialogLoading by remember { mutableStateOf(false) }

    // Effective slot limit: base 5, or 5+extraProfiles while an active
    // subscription grants extra room via the flexible plan. If the plan is
    // expired/cancelled, creation is blocked outright regardless of count —
    // the backend is authoritative on this, this is just for the UI hint.
    // Admins are fully exempt — the backend never creates a trial/limit for
    // them, so subState.subscription will always be null for an admin user.
    val isAdmin = authState.userRole == "admin"
    val sub = subState.subscription
    val totalSlots = sub?.totalSlots ?: 5
    val isBlocked = sub?.isBlocked == true
    val canCreateProfile = isAdmin || (!isBlocked && state.profiles.size < totalSlots)

    LaunchedEffect(Unit) { subscriptionViewModel.load() }

    if (showSubscriptionPopup) {
        SubscriptionRequiredDialog(
            reason = if (isBlocked) "Gói duy trì lưu trữ hồ sơ của bạn đã hết hạn hoặc đã bị hủy."
                     else "Bạn đã đạt giới hạn $totalSlots hồ sơ.",
            onUpgrade = {
                showSubscriptionPopup = false
                navController.navigate(Routes.SUBSCRIPTION)
            },
            onDismiss = { showSubscriptionPopup = false }
        )
    }

    LaunchedEffect(authState.isInitialized, authState.isLoggedIn) {
        // Wait until the session has been read from disk at least once,
        // otherwise the initial default (isLoggedIn=false) kicks the user
        // to LOGIN before the real value arrives.
        if (!authState.isInitialized) return@LaunchedEffect
        if (authState.isLoggedIn) viewModel.loadMyProfiles()
        else navController.navigate(Routes.LOGIN)
    }

    // Link QR Tag dialog
    if (showLinkDialog) {
        LinkQrTagDialog(
            profileId = linkTargetProfileId,
            onLink = { tagCode, pin ->
                viewModel.linkQrTag(tagCode, pin, linkTargetProfileId) { success, err ->
                    if (success) showLinkDialog = false
                }
            },
            onDismiss = { showLinkDialog = false }
        )
    }

    // Show-QRs dialog
    qrsDialogProfile?.let { p ->
        ShowProfileQrsDialog(
            profile = p,
            tags = qrsDialogTags,
            isLoading = qrsDialogLoading,
            onDismiss = { qrsDialogProfile = null; qrsDialogTags = emptyList() }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ── Create profile card ──────────────────────────────────────────────
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.PersonAdd, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(32.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Thêm Hồ Sơ Mới", style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("human" to "👤 Người", "pet" to "🐾 Thú Cưng").forEach { (type, label) ->
                            OutlinedButton(
                                onClick = { profileTypeChoice = type },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (profileTypeChoice == type)
                                        MaterialTheme.colorScheme.onPrimary
                                    else
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                    contentColor = if (profileTypeChoice == type)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.onPrimary
                                ),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.5f))
                            ) { Text(label, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold) }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = {
                            if (canCreateProfile) {
                                navController.navigate(Routes.createProfile(profileTypeChoice))
                            } else {
                                showSubscriptionPopup = true
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.onPrimary,
                            contentColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Tiếp Theo →", fontWeight = FontWeight.Bold)
                    }
                    if (!canCreateProfile) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("⚠️ Đã đạt giới hạn $totalSlots hồ sơ — nâng cấp gói duy trì để thêm", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f))
                    }
                }
            }
        }

        // ── Profile list header ──────────────────────────────────────────────
        item {
            Text(
                if (isAdmin) "Quản Lý Hồ Sơ (${state.profiles.size} — Không giới hạn)"
                else "Quản Lý Hồ Sơ (${state.profiles.size}/$totalSlots)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary)
        }

        // ── Subscription reminder banner (7 / 3 days before expiry) ──────────
        if (subState.shouldShowReminder) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.clickable { navController.navigate(Routes.SUBSCRIPTION) }
                ) {
                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Notifications, contentDescription = null)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Gói duy trì hồ sơ sắp hết hạn", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                            Text("Còn ${sub?.daysRemaining ?: 0} ngày — gia hạn ngay để tránh bị khóa hồ sơ",
                                style = MaterialTheme.typography.bodySmall)
                        }
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                    }
                }
            }
        }

        if (state.isLoading) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        }

        if (!state.isLoading && state.profiles.isEmpty()) {
            item {
                Text("Bạn chưa có hồ sơ nào. Hãy tạo hồ sơ đầu tiên!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        // ── Profile cards ────────────────────────────────────────────────────
        items(state.profiles, key = { it.id }) { profile ->
            ProfileListCard(
                profile = profile,
                onEdit = { navController.navigate(Routes.createProfile("human", profile.id)) },
                onDelete = { viewModel.deleteProfile(profile.id) { viewModel.loadMyProfiles() } },
                onLinkQr = { showLinkDialog = true; linkTargetProfileId = profile.id },
                onFamilyNotify = { navController.navigate(Routes.familyNotify(profile.id)) },
                onShowQrs = {
                    qrsDialogProfile = profile
                    qrsDialogLoading = true
                    viewModel.fetchTagsForProfile(profile.id) { tags ->
                        qrsDialogTags = tags
                        qrsDialogLoading = false
                    }
                }
            )
        }
    }
}

@Composable
private fun ProfileListCard(
    profile: Profile,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onLinkQr: () -> Unit,
    onShowQrs: () -> Unit,
    onFamilyNotify: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Xóa hồ sơ?") },
            text = { Text("Bạn có chắc muốn xóa hồ sơ \"${profile.fullName}\"? Hành động này không thể hoàn tác.") },
            confirmButton = {
                TextButton(onClick = { onDelete(); showDeleteConfirm = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                    Text("Xóa")
                }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("Hủy") } }
        )
    }

    Card(shape = RoundedCornerShape(12.dp)) {
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(46.dp)
                ) {
                    Icon(
                        if (profile.profileType == "pet") Icons.Default.Pets else Icons.Default.Person,
                        null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(10.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(profile.fullName.ifBlank { "Chưa đặt tên" },
                        style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Surface(shape = RoundedCornerShape(4.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                            Text(if (profile.profileType == "pet") "🐾 Thú cưng" else "👤 Người",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                        if (profile.isPrivate) {
                            Surface(shape = RoundedCornerShape(4.dp), color = MaterialTheme.colorScheme.error.copy(alpha = 0.15f)) {
                                Text("🔒 Riêng tư", style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            // Row 1: Edit + View QRs
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onEdit, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Edit, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Sửa", style = MaterialTheme.typography.bodySmall)
                }
                OutlinedButton(onClick = onShowQrs, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.QrCode2, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Xem QR", style = MaterialTheme.typography.bodySmall)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            // Row 2: Link QR + Delete
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onLinkQr, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.QrCode, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Liên kết QR", style = MaterialTheme.typography.bodySmall)
                }
                OutlinedButton(
                    onClick = { showDeleteConfirm = true },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.Delete, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Xóa", style = MaterialTheme.typography.bodySmall)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            // Row 3: Family scan-notification (subscription perk)
            OutlinedButton(onClick = onFamilyNotify, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Notifications, null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Thông Báo Cho Người Thân Khi Bị Quét", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

// ── Link QR Tag Dialog ────────────────────────────────────────────────────────
@Composable
private fun LinkQrTagDialog(
    profileId: String,
    onLink: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var tagCode by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    val scan = com.qrhealthcare.app.ui.components.rememberQrScanner { scanned ->
        com.qrhealthcare.app.ui.components.parseScannedTagCode(scanned)?.let { tagCode = it }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.QrCode, null, tint = MaterialTheme.colorScheme.primary) },
        title = { Text("Liên Kết Tag QR", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Quét mã QR hoặc nhập thủ công mã tag và PIN được in trên sản phẩm.",
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedButton(onClick = { scan() }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.QrCodeScanner, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Quét mã QR bằng camera")
                }
                OutlinedTextField(
                    value = tagCode, onValueChange = { tagCode = it.uppercase() },
                    label = { Text("Mã Tag (VD: QRH-A1B2)") },
                    singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = pin, onValueChange = { if (it.length <= 4) pin = it },
                    label = { Text("PIN (4 chữ số)") },
                    singleLine = true, modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = { onLink(tagCode.trim(), pin.trim()) },
                enabled = tagCode.isNotBlank() && pin.length == 4) {
                Text("Liên Kết")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Hủy") } }
    )
}

// ── Show-QRs Dialog (per-profile linked QR codes) ─────────────────────────────
@Composable
private fun ShowProfileQrsDialog(
    profile: Profile,
    tags: List<com.qrhealthcare.app.data.model.QrTag>,
    isLoading: Boolean,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.QrCode2, null, tint = MaterialTheme.colorScheme.primary) },
        title = { Text("Mã QR của ${profile.fullName.ifBlank { "hồ sơ" }}", fontWeight = FontWeight.Bold) },
        text = {
            when {
                isLoading -> Box(
                    Modifier.fillMaxWidth().padding(24.dp),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator() }

                tags.isEmpty() -> Text(
                    "Chưa có mã QR nào được liên kết với hồ sơ này. Mua sản phẩm và liên kết tag để bắt đầu.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                else -> androidx.compose.foundation.lazy.LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.heightIn(max = 460.dp)
                ) {
                    items(tags, key = { it.id }) { tag ->
                        Card(
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(
                                Modifier.fillMaxWidth().padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                com.qrhealthcare.app.ui.components.QrCodeImage(
                                    value = com.qrhealthcare.app.data.api.ApiClient.publicProfileUrl(tag.tagCode),
                                    modifier = Modifier.size(150.dp)
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(tag.tagCode, fontWeight = FontWeight.Bold)
                                Text("PIN: ${tag.pin}", style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                                if (tag.scanCount > 0) {
                                    Text("Đã quét ${tag.scanCount} lần",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.outline)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Đóng") } }
    )
}

@Composable
private fun SubscriptionRequiredDialog(
    reason: String,
    onUpgrade: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.WorkspacePremium, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(40.dp)) },
        title = { Text("Cần Gói Duy Trì Hồ Sơ", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(reason, style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Đăng ký gói duy trì lưu trữ hồ sơ để tạo thêm hồ sơ mới và giữ hồ sơ của bạn luôn hiển thị đầy đủ.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = { Button(onClick = onUpgrade) { Text("Xem Các Gói") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Để Sau") } }
    )
}
