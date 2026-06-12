package com.qrhealthcare.app.ui.screens.publicprofile

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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.qrhealthcare.app.data.model.*
import com.qrhealthcare.app.ui.viewmodel.PublicProfileViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PublicProfileScreen(
    tagCode: String,
    onBack: () -> Unit,
    viewModel: PublicProfileViewModel = hiltViewModel()
) {
    val profile by viewModel.profile.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    LaunchedEffect(tagCode) { viewModel.loadByTagCode(tagCode) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Hồ Sơ Y Tế") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        when {
            isLoading -> Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Đang tải hồ sơ y tế...", style = MaterialTheme.typography.bodyMedium)
                }
            }

            error != null -> Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                    Icon(Icons.Default.ErrorOutline, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(64.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Không tìm thấy hồ sơ", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(error ?: "", style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { viewModel.loadByTagCode(tagCode) }) { Text("Thử lại") }
                }
            }

            profile != null -> PublicProfileContent(profile = profile!!, modifier = Modifier.padding(padding))
        }
    }
}

@Composable
private fun PublicProfileContent(profile: Profile, modifier: Modifier = Modifier) {
    fun isVisible(field: String): Boolean =
        !profile.isPrivate || field in ALWAYS_VISIBLE_FIELDS || !profile.hiddenFields.contains(field)

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // ── Emergency header ─────────────────────────────────────────────────
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (profile.profileType == "pet") Icons.Default.Pets else Icons.Default.Person,
                        null, tint = Color.White, modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(profile.fullName.ifBlank { "Không có tên" }, style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Black, color = Color.White)
                        Text(if (profile.profileType == "pet") "🐾 Thú cưng" else "👤 Người",
                            color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.bodyMedium)
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (profile.bloodGroup.isNotBlank()) {
                        InfoChip("🩸 ${profile.bloodGroup}", Color.White, MaterialTheme.colorScheme.primary)
                    }
                    if (profile.organDonor) {
                        InfoChip("💚 Hiến Tạng", Color.White, MaterialTheme.colorScheme.primary)
                    }
                    if (isVisible("gender") && profile.gender.isNotBlank()) {
                        InfoChip(profile.gender, Color.White, MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }

        // ── Physical stats ────────────────────────────────────────────────────
        val showStats = listOf("height","weight","hairColor","eyeColor").any { isVisible(it) }
        if (showStats && (profile.height.isNotBlank() || profile.weight.isNotBlank())) {
            PublicSection("📊 Thông Số Thể Chất", Icons.Default.BarChart) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    if (isVisible("height") && profile.height.isNotBlank())
                        StatCard("Chiều cao", profile.height, modifier = Modifier.weight(1f))
                    if (isVisible("weight") && profile.weight.isNotBlank())
                        StatCard("Cân nặng", profile.weight, modifier = Modifier.weight(1f))
                    if (isVisible("birthDate") && profile.birthDate.isNotBlank())
                        StatCard("Ngày sinh", profile.birthDate, modifier = Modifier.weight(1f))
                }
            }
        }

        // ── Emergency contacts ────────────────────────────────────────────────
        if (isVisible("emergencyContacts") && profile.emergencyContacts.isNotEmpty()) {
            PublicSection("📞 Liên Hệ Khẩn Cấp", Icons.Default.Call) {
                profile.emergencyContacts.forEach { contact ->
                    ContactRow(contact)
                }
            }
        }

        // ── Allergies ─────────────────────────────────────────────────────────
        if (isVisible("allergies") && profile.allergies.isNotEmpty()) {
            PublicSection("⚠️ Dị Ứng", Icons.Default.Warning, sectionColor = MaterialTheme.colorScheme.error) {
                profile.allergies.forEach { allergy ->
                    AllergyRow(allergy)
                }
            }
        }

        // ── Medications ────────────────────────────────────────────────────────
        if (isVisible("medications") && profile.medications.isNotEmpty()) {
            PublicSection("💊 Thuốc Đang Dùng", Icons.Default.Medication) {
                profile.medications.forEach { med ->
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(med.name, fontWeight = FontWeight.SemiBold)
                        Text("${med.dosage} · ${med.frequency}", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    HorizontalDivider()
                }
            }
        }

        // ── Medical conditions ────────────────────────────────────────────────
        if (isVisible("medicalConditions") && profile.medicalConditions.isNotEmpty()) {
            PublicSection("🏥 Bệnh Lý", Icons.Default.LocalHospital) {
                profile.medicalConditions.forEach { cond ->
                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        Text(cond.name, fontWeight = FontWeight.SemiBold)
                        if (cond.diagnosedDate.isNotBlank())
                            Text("Chẩn đoán: ${cond.diagnosedDate}", style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (cond.notes.isNotBlank())
                            Text(cond.notes, style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    HorizontalDivider()
                }
            }
        }

        // ── Insurance ─────────────────────────────────────────────────────────
        if (isVisible("insurance") && profile.insurance.isNotEmpty()) {
            PublicSection("🛡️ Bảo Hiểm", Icons.Default.HealthAndSafety) {
                profile.insurance.forEach { ins ->
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(ins.provider, fontWeight = FontWeight.SemiBold)
                        Text(ins.policyNumber, style = MaterialTheme.typography.bodySmall)
                    }
                    if (ins.expiryDate.isNotBlank())
                        Text("HH: ${ins.expiryDate}", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    HorizontalDivider()
                }
            }
        }

        // ── Privacy notice ────────────────────────────────────────────────────
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Chỉ hiển thị thông tin được chủ hồ sơ cho phép công khai.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun PublicSection(
    title: String,
    icon: ImageVector,
    sectionColor: Color = MaterialTheme.colorScheme.primary,
    content: @Composable ColumnScope.() -> Unit
) {
    Column {
        Surface(color = sectionColor, shape = RoundedCornerShape(50)) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(icon, null, tint = Color.White, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(title, style = MaterialTheme.typography.labelMedium, color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Card(shape = RoundedCornerShape(12.dp)) {
            Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) { content() }
        }
    }
}

@Composable
private fun InfoChip(label: String, textColor: Color, bgColor: Color) {
    Surface(shape = RoundedCornerShape(50), color = bgColor.copy(alpha = 0.3f)) {
        Text(label, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.bodySmall, color = textColor, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
        Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ContactRow(contact: EmergencyContact) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.Person, null, modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(contact.name, fontWeight = FontWeight.SemiBold)
            Text(contact.relationship, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(contact.phone, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
    }
    HorizontalDivider()
}

@Composable
private fun AllergyRow(allergy: Allergy) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Surface(
            shape = RoundedCornerShape(4.dp),
            color = when (allergy.severity) {
                "Nặng" -> MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
                "Vừa" -> Color(0xFFFFF3E0)
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        ) {
            Text(allergy.severity, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                style = MaterialTheme.typography.bodySmall,
                color = if (allergy.severity == "Nặng") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(allergy.name, fontWeight = FontWeight.SemiBold)
            if (allergy.reaction.isNotBlank())
                Text(allergy.reaction, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    HorizontalDivider()
}
