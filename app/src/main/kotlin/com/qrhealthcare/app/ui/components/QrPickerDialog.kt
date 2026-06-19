package com.qrhealthcare.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.qrhealthcare.app.data.api.ApiClient
import com.qrhealthcare.app.data.model.Profile
import com.qrhealthcare.app.data.model.QrTag
import com.qrhealthcare.app.ui.viewmodel.ProfileViewModel

/**
 * Popup shown when the user taps the floating QR bubble. Two-state UI:
 *   1. List of the user's profiles, each with the count of linked QR tags.
 *   2. After tapping a profile, shows the QR codes linked to that profile.
 *
 * Loads profiles fresh on open so changes from elsewhere in the app are
 * reflected immediately.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QrPickerDialog(
    onDismiss: () -> Unit,
    profileViewModel: ProfileViewModel = hiltViewModel()
) {
    val listState by profileViewModel.listState.collectAsState()
    var selectedProfile by remember { mutableStateOf<Profile?>(null) }
    var profileTags by remember { mutableStateOf<List<QrTag>>(emptyList()) }
    var loadingTags by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(Unit) { profileViewModel.loadMyProfiles() }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 200.dp, max = 600.dp)
                .padding(start = 16.dp, end = 16.dp, bottom = 24.dp)
        ) {
            if (selectedProfile == null) {
                ProfileListView(
                    profiles = listState.profiles,
                    isLoading = listState.isLoading,
                    onProfileClick = { p ->
                        selectedProfile = p
                        loadingTags = true
                        profileViewModel.fetchTagsForProfile(p.id) { tags ->
                            profileTags = tags
                            loadingTags = false
                        }
                    }
                )
            } else {
                ProfileQrView(
                    profile = selectedProfile!!,
                    tags = profileTags,
                    isLoading = loadingTags,
                    onBack = {
                        selectedProfile = null
                        profileTags = emptyList()
                    }
                )
            }
        }
    }
}

@Composable
private fun ProfileListView(
    profiles: List<Profile>,
    isLoading: Boolean,
    onProfileClick: (Profile) -> Unit
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
            Icon(Icons.Default.QrCode2, null, tint = Color(0xFFFF6F00))
            Spacer(Modifier.width(8.dp))
            Text("Mã QR của bạn", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        Text(
            "Chọn hồ sơ để xem mã QR liên kết",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        when {
            isLoading && profiles.isEmpty() ->
                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            profiles.isEmpty() ->
                EmptyState(
                    icon = Icons.Default.Person,
                    title = "Chưa có hồ sơ",
                    subtitle = "Hãy tạo hồ sơ trong tab Hồ Sơ trước."
                )
            else ->
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(profiles, key = { it.id }) { profile ->
                        ProfileRow(profile, onClick = { onProfileClick(profile) })
                    }
                }
        }
    }
}

@Composable
private fun ProfileRow(profile: Profile, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(44.dp).clip(CircleShape).background(Color(0xFFFF6F00)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (profile.profileType == "pet") Icons.Default.Pets else Icons.Default.Person,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    profile.fullName.ifBlank { "(chưa đặt tên)" },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                Text(
                    if (profile.profileType == "pet") "Thú cưng" else "Người",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
            Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.outline)
        }
    }
}

@Composable
private fun ProfileQrView(
    profile: Profile,
    tags: List<QrTag>,
    isLoading: Boolean,
    onBack: () -> Unit
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 12.dp)) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Quay lại")
            }
            Text(
                profile.fullName.ifBlank { "Hồ sơ" },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
        }

        when {
            isLoading ->
                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            tags.isEmpty() ->
                EmptyState(
                    icon = Icons.Default.QrCode2,
                    title = "Chưa có QR liên kết",
                    subtitle = "Mua sticker hoặc tag để có QR, sau đó vào tab Hồ Sơ để liên kết."
                )
            else ->
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(tags, key = { it.id }) { tag -> QrCard(tag) }
                }
        }
    }
}

@Composable
private fun QrCard(tag: QrTag) {
    Card(shape = RoundedCornerShape(12.dp)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            QrCodeImage(
                value = ApiClient.publicProfileUrl(tag.tagCode),
                modifier = Modifier.size(180.dp)
            )
            Spacer(Modifier.height(8.dp))
            Text(tag.tagCode, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
            Text(
                "Đã quét ${tag.scanCount} lần",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

@Composable
private fun EmptyState(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.outline)
        Spacer(Modifier.height(8.dp))
        Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(4.dp))
        Text(
            subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
    }
}