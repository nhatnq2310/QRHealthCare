package com.qrhealthcare.app.ui.screens.profile

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.qrhealthcare.app.data.api.ApiClient
import com.qrhealthcare.app.data.model.*
import com.qrhealthcare.app.ui.viewmodel.ProfileViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateProfileScreen(
    profileType: String = "human",
    editProfileId: String? = null,
    onSaved: () -> Unit,
    onBack: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val editState by viewModel.editState.collectAsState()
    var showConsentDialog by remember { mutableStateOf(editProfileId == null) }
    var consentGiven by remember { mutableStateOf(editProfileId != null) }
    var showQrLinkedTags by remember { mutableStateOf(false) }

    // Sub-item dialogs
    var showContactDialog by remember { mutableStateOf(false) }
    var showAllergyDialog by remember { mutableStateOf(false) }
    var showMedDialog by remember { mutableStateOf(false) }
    var showConditionDialog by remember { mutableStateOf(false) }
    var showHealthInsuranceDialog by remember { mutableStateOf(false) }
    var showLifeInsuranceDialog by remember { mutableStateOf(false) }
    var showAddressDialog by remember { mutableStateOf(false) }

    LaunchedEffect(editProfileId) { viewModel.loadProfileForEdit(editProfileId) }

    LaunchedEffect(editState.savedSuccessfully) {
        if (editState.savedSuccessfully) onSaved()
    }

    val profile = editState.profile

    // ── Image picker for health documents ────────────────────────────────────
    val context = LocalContext.current
    var uploadInFlight by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val uploadScope = rememberCoroutineScope()
    val pickHealthDocLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        uploadInFlight = true
        viewModel.uploadHealthDocument(context, uri) { success, err ->
            uploadInFlight = false
            uploadScope.launch {
                snackbarHostState.showSnackbar(
                    if (success) "Đã tải lên ảnh" else (err ?: "Tải lên thất bại")
                )
            }
        }
    }

    fun update(block: Profile.() -> Profile) = viewModel.updateEditProfile(profile.block())
    fun toggleHiddenField(key: String, hide: Boolean) {
        val current = profile.hiddenFields.toMutableList()
        if (hide) { if (!current.contains(key)) current.add(key) }
        else current.remove(key)
        update { copy(hiddenFields = current) }
    }

    // ── Consent dialog ───────────────────────────────────────────────────────
    if (showConsentDialog) {
        AlertDialog(
            onDismissRequest = { onBack() },
            icon = { Icon(Icons.Default.HealthAndSafety, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp)) },
            title = { Text("Xác Nhận Cung Cấp Thông Tin", fontWeight = FontWeight.Bold) },
            text = {
                Text("Bạn đồng ý cung cấp thông tin sức khỏe cá nhân để lưu trữ trên hệ thống MyQRHealthcare. " +
                        "Thông tin được bảo mật và chỉ hiển thị cho người được ủy quyền quét mã QR của bạn. " +
                        "Bạn có thể ẩn bất kỳ thông tin nào bằng công cụ bảo mật trong hồ sơ.",
                    style = MaterialTheme.typography.bodyMedium)
            },
            confirmButton = {
                Button(onClick = { consentGiven = true; showConsentDialog = false }) { Text("Tôi Đồng Ý") }
            },
            dismissButton = {
                TextButton(onClick = onBack) { Text("Không đồng ý") }
            }
        )
    }

    if (!consentGiven) return

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (editProfileId == null) "Tạo Hồ Sơ Mới" else "Chỉnh Sửa Hồ Sơ") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } },
                actions = {
                    TextButton(
                        onClick = { viewModel.saveProfile() },
                        enabled = !editState.isSaving
                    ) {
                        if (editState.isSaving) CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        else Text("Lưu", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Error ────────────────────────────────────────────────────────
            if (editState.error != null) {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                    Text(editState.error!!, modifier = Modifier.padding(12.dp), color = MaterialTheme.colorScheme.onErrorContainer)
                }
            }

            // ── Privacy master toggle ────────────────────────────────────────
            Card(colors = CardDefaults.cardColors(
                containerColor = if (profile.isPrivate) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
            )) {
                Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(if (profile.isPrivate) Icons.Default.Lock else Icons.Default.LockOpen,
                        null, tint = if (profile.isPrivate) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Chế Độ Riêng Tư", fontWeight = FontWeight.Bold)
                        Text(
                            if (profile.isPrivate)
                                "Bật — nếu không tích ô nào, mọi thông tin sẽ bị ẩn (trừ thông tin cấp cứu quan trọng). Nếu tích ô, chỉ những mục đã tích bị ẩn."
                            else "Tắt — tất cả thông tin hiển thị công khai",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(checked = profile.isPrivate, onCheckedChange = { update { copy(isPrivate = it) } })
                }
            }

            // ── Profile type ─────────────────────────────────────────────────
            if (editProfileId == null) {
                SectionHeader("Loại Hồ Sơ", Icons.Default.Category)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    listOf("human" to "👤 Người", "pet" to "🐾 Thú Cưng").forEach { (type, label) ->
                        FilterChip(
                            selected = profile.profileType == type,
                            onClick = { update { copy(profileType = type) } },
                            label = { Text(label) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // ── Personal Information ─────────────────────────────────────────
            SectionHeader("Thông Tin Cá Nhân", Icons.Default.Person)

            FieldWithPrivacy(label = "Họ và Tên *", fieldKey = "fullName",
                isPrivate = profile.isPrivate, hiddenFields = profile.hiddenFields,
                onToggleHide = { toggleHiddenField("fullName", it) }, alwaysVisible = true) {
                OutlinedTextField(value = profile.fullName, onValueChange = { update { copy(fullName = it) } },
                    modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text("Họ và Tên") })
            }
            FieldWithPrivacy(label = "Số CCCD / Hộ chiếu", fieldKey = "personalNumber",
                isPrivate = profile.isPrivate, hiddenFields = profile.hiddenFields,
                onToggleHide = { toggleHiddenField("personalNumber", it) }) {
                OutlinedTextField(value = profile.personalNumber, onValueChange = { update { copy(personalNumber = it) } },
                    modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text("Số CCCD / Hộ chiếu") })
            }
            FieldWithPrivacy(label = "Giới Tính", fieldKey = "gender",
                isPrivate = profile.isPrivate, hiddenFields = profile.hiddenFields,
                onToggleHide = { toggleHiddenField("gender", it) }, alwaysVisible = true) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Nam", "Nữ", "Khác").forEach { gender ->
                        FilterChip(selected = profile.gender == gender, onClick = { update { copy(gender = gender) } },
                            label = { Text(gender) })
                    }
                }
            }
            FieldWithPrivacy(label = "Ngày Sinh", fieldKey = "birthDate",
                isPrivate = profile.isPrivate, hiddenFields = profile.hiddenFields,
                onToggleHide = { toggleHiddenField("birthDate", it) }) {
                OutlinedTextField(value = profile.birthDate, onValueChange = { update { copy(birthDate = it) } },
                    modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text("Ngày Sinh (VD: 1990-05-15)") })
            }

            // ── Vital info ───────────────────────────────────────────────────
            SectionHeader("Thông Số Thể Chất", Icons.Default.MonitorHeart)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FieldWithPrivacy(label = "Nhóm Máu", fieldKey = "bloodGroup",
                    isPrivate = profile.isPrivate, hiddenFields = profile.hiddenFields,
                    onToggleHide = { toggleHiddenField("bloodGroup", it) }, alwaysVisible = true,
                    modifier = Modifier.weight(1f)) {
                    var expanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                        OutlinedTextField(value = profile.bloodGroup, onValueChange = {}, readOnly = true,
                            label = { Text("Nhóm Máu") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth())
                        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            listOf("O+","O-","A+","A-","B+","B-","AB+","AB-","Không rõ").forEach { bg ->
                                DropdownMenuItem(text = { Text(bg) }, onClick = { update { copy(bloodGroup = bg) }; expanded = false })
                            }
                        }
                    }
                }
                FieldWithPrivacy(label = "Chiều Cao", fieldKey = "height",
                    isPrivate = profile.isPrivate, hiddenFields = profile.hiddenFields,
                    onToggleHide = { toggleHiddenField("height", it) }, modifier = Modifier.weight(1f)) {
                    OutlinedTextField(value = profile.height, onValueChange = { update { copy(height = it) } },
                        modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text("Chiều cao (cm)") })
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FieldWithPrivacy(label = "Cân Nặng", fieldKey = "weight",
                    isPrivate = profile.isPrivate, hiddenFields = profile.hiddenFields,
                    onToggleHide = { toggleHiddenField("weight", it) }, modifier = Modifier.weight(1f)) {
                    OutlinedTextField(value = profile.weight, onValueChange = { update { copy(weight = it) } },
                        modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text("Cân nặng (kg)") })
                }
                FieldWithPrivacy(label = "Màu Tóc", fieldKey = "hairColor",
                    isPrivate = profile.isPrivate, hiddenFields = profile.hiddenFields,
                    onToggleHide = { toggleHiddenField("hairColor", it) }, modifier = Modifier.weight(1f)) {
                    OutlinedTextField(value = profile.hairColor, onValueChange = { update { copy(hairColor = it) } },
                        modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text("Màu Tóc") })
                }
            }

            // Organ donor — "đã đăng ký" / "chưa đăng ký" + optional show/hide
            SectionHeader("Đăng Ký Hiến Tạng", Icons.Default.VolunteerActivism)
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = profile.organDonor,
                            onClick = { update { copy(organDonor = true) } },
                            label = { Text("Đã đăng ký") },
                            leadingIcon = if (profile.organDonor) {
                                { Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp)) }
                            } else null,
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = !profile.organDonor,
                            onClick = { update { copy(organDonor = false) } },
                            label = { Text("Chưa đăng ký") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    // Show/hide sub-option — only when registered. Shown by default.
                    if (profile.organDonor) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Checkbox(
                                checked = profile.showOrganDonor,
                                onCheckedChange = { update { copy(showOrganDonor = it) } }
                            )
                            Text(
                                if (profile.showOrganDonor)
                                    "Hiển thị trạng thái hiến tạng trên hồ sơ công khai"
                                else
                                    "Ẩn trạng thái hiến tạng khỏi hồ sơ công khai",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // ── Emergency contacts (ALWAYS PUBLIC) ────────────────────────────
            SectionHeader("Liên Hệ Khẩn Cấp", Icons.Default.Call)
            AlwaysPublicNote(isPrivate = profile.isPrivate)
            profile.emergencyContacts.forEachIndexed { i, contact ->
                SubItemCard(
                    label = "${contact.name} · ${contact.relationship} · ${contact.phone}",
                    onDelete = { update { copy(emergencyContacts = emergencyContacts.toMutableList().also { it.removeAt(i) }) } }
                )
            }
            TextButton(onClick = { showContactDialog = true }) {
                Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Thêm liên hệ khẩn cấp")
            }

            // ── Allergies (ALWAYS PUBLIC — drug allergies are safety-critical) ─
            SectionHeader("Dị Ứng", Icons.Default.Warning)
            AlwaysPublicNote(isPrivate = profile.isPrivate)
            profile.allergies.forEachIndexed { i, allergy ->
                SubItemCard(
                    label = "${allergy.name} · ${allergy.severity} · ${allergy.reaction}",
                    onDelete = { update { copy(allergies = allergies.toMutableList().also { it.removeAt(i) }) } }
                )
            }
            TextButton(onClick = { showAllergyDialog = true }) {
                Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Thêm dị ứng")
            }

            // ── Medications ───────────────────────────────────────────────────
            SectionHeader("Thuốc Đang Dùng", Icons.Default.Medication)
            SectionPrivacyControl(fieldKey = "medications", label = "Thuốc đang dùng",
                isPrivate = profile.isPrivate, hiddenFields = profile.hiddenFields,
                onToggleHide = { toggleHiddenField("medications", it) })
            profile.medications.forEachIndexed { i, med ->
                SubItemCard(
                    label = "${med.name} · ${med.dosage} · ${med.frequency}",
                    onDelete = { update { copy(medications = medications.toMutableList().also { it.removeAt(i) }) } }
                )
            }
            TextButton(onClick = { showMedDialog = true }) {
                Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Thêm thuốc")
            }

            // ── Medical conditions ────────────────────────────────────────────
            SectionHeader("Bệnh Lý", Icons.Default.LocalHospital)
            SectionPrivacyControl(fieldKey = "medicalConditions", label = "Bệnh lý nền",
                isPrivate = profile.isPrivate, hiddenFields = profile.hiddenFields,
                onToggleHide = { toggleHiddenField("medicalConditions", it) })
            profile.medicalConditions.forEachIndexed { i, cond ->
                SubItemCard(
                    label = "${cond.name} · ${cond.diagnosedDate}",
                    onDelete = { update { copy(medicalConditions = medicalConditions.toMutableList().also { it.removeAt(i) }) } }
                )
            }
            TextButton(onClick = { showConditionDialog = true }) {
                Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Thêm bệnh lý")
            }

            // ── Health insurance (bảo hiểm y tế) ──────────────────────────────
            SectionHeader("Bảo Hiểm Y Tế", Icons.Default.HealthAndSafety)
            SectionPrivacyControl(fieldKey = "healthInsurance", label = "Bảo hiểm y tế",
                isPrivate = profile.isPrivate, hiddenFields = profile.hiddenFields,
                onToggleHide = { toggleHiddenField("healthInsurance", it) })
            profile.healthInsurance.forEachIndexed { i, ins ->
                SubItemCard(
                    label = "${ins.provider} · ${ins.policyNumber}",
                    onDelete = { update { copy(healthInsurance = healthInsurance.toMutableList().also { it.removeAt(i) }) } }
                )
            }
            TextButton(onClick = { showHealthInsuranceDialog = true }) {
                Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Thêm bảo hiểm y tế")
            }

            // ── Life insurance (bảo hiểm nhân thọ) ────────────────────────────
            SectionHeader("Bảo Hiểm Nhân Thọ", Icons.Default.Shield)
            SectionPrivacyControl(fieldKey = "lifeInsurance", label = "Bảo hiểm nhân thọ",
                isPrivate = profile.isPrivate, hiddenFields = profile.hiddenFields,
                onToggleHide = { toggleHiddenField("lifeInsurance", it) })
            profile.lifeInsurance.forEachIndexed { i, ins ->
                SubItemCard(
                    label = "${ins.provider} · ${ins.policyNumber}",
                    onDelete = { update { copy(lifeInsurance = lifeInsurance.toMutableList().also { it.removeAt(i) }) } }
                )
            }
            TextButton(onClick = { showLifeInsuranceDialog = true }) {
                Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Thêm bảo hiểm nhân thọ")
            }

            // ── Health documents (uploads) ──────────────────────────────────
            SectionHeader("Tài Liệu Y Tế (Tùy Chọn)", Icons.Default.Description)
            Text(
                "Tải lên ảnh giấy tờ y tế (đơn thuốc, kết quả xét nghiệm, sổ tiêm chủng...) để chứng minh thông tin phía trên.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            HealthDocumentsRow(
                urls = profile.healthDocuments,
                onUploadClick = {
                    pickHealthDocLauncher.launch("image/*")
                },
                onRemove = { url -> viewModel.removeHealthDocument(url) },
                isUploading = editState.isSaving && uploadInFlight
            )

            // ── Notes (free-form personal notes) ────────────────────────────
            SectionHeader("Ghi Chú Cá Nhân", Icons.Default.Notes)
            Text(
                "Dùng để ghi kế hoạch ăn uống, lưu ý hằng ngày, hoặc bất kỳ thông tin cá nhân nào bạn muốn.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedTextField(
                value = profile.notes,
                onValueChange = { update { copy(notes = it) } },
                placeholder = { Text("VD: Tránh ăn hải sản. Tập gym 5 buổi/tuần. Uống nhiều nước...") },
                minLines = 4,
                maxLines = 10,
                modifier = Modifier.fillMaxWidth()
            )

            // ── Save button ───────────────────────────────────────────────────
            Button(
                onClick = { viewModel.saveProfile() },
                enabled = !editState.isSaving && profile.fullName.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                if (editState.isSaving) CircularProgressIndicator(modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                else Text("Lưu Hồ Sơ", fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // ── Sub-item dialogs ─────────────────────────────────────────────────────
    if (showContactDialog) {
        AddEmergencyContactDialog(
            onAdd = { showContactDialog = false; update { copy(emergencyContacts = emergencyContacts + it) } },
            onDismiss = { showContactDialog = false }
        )
    }
    if (showAllergyDialog) {
        AddAllergyDialog(
            onAdd = { showAllergyDialog = false; update { copy(allergies = allergies + it) } },
            onDismiss = { showAllergyDialog = false }
        )
    }
    if (showMedDialog) {
        AddMedicationDialog(
            onAdd = { showMedDialog = false; update { copy(medications = medications + it) } },
            onDismiss = { showMedDialog = false }
        )
    }
    if (showConditionDialog) {
        AddConditionDialog(
            onAdd = { showConditionDialog = false; update { copy(medicalConditions = medicalConditions + it) } },
            onDismiss = { showConditionDialog = false }
        )
    }
    if (showHealthInsuranceDialog) {
        AddInsuranceDialog(
            onAdd = { showHealthInsuranceDialog = false; update { copy(healthInsurance = healthInsurance + it) } },
            onDismiss = { showHealthInsuranceDialog = false }
        )
    }
    if (showLifeInsuranceDialog) {
        AddInsuranceDialog(
            onAdd = { showLifeInsuranceDialog = false; update { copy(lifeInsurance = lifeInsurance + it) } },
            onDismiss = { showLifeInsuranceDialog = false }
        )
    }
}

// ── Reusable composables ───────────────────────────────────────────────────────

@Composable
private fun SectionHeader(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary)
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
}

@Composable
private fun FieldWithPrivacy(
    label: String,
    fieldKey: String,
    isPrivate: Boolean,
    hiddenFields: List<String>,
    onToggleHide: (Boolean) -> Unit,
    alwaysVisible: Boolean = false,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val isHidden = hiddenFields.contains(fieldKey)
    Column(modifier = modifier) {
        if (isPrivate && !alwaysVisible) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Checkbox(checked = isHidden, onCheckedChange = { onToggleHide(it) })
                Text("Ẩn \"$label\" khỏi hồ sơ công khai",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (alwaysVisible && isPrivate) {
            Text("🔓 \"$label\" luôn hiển thị công khai", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.tertiary, modifier = Modifier.padding(bottom = 4.dp))
        }
        content()
    }
}

/**
 * Shown under a section header for sections that are ALWAYS public (emergency
 * contacts, allergies). Only renders the reassurance line when the profile is
 * private — otherwise everything is public anyway and the note is noise.
 */
@Composable
private fun AlwaysPublicNote(isPrivate: Boolean) {
    if (!isPrivate) return
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(bottom = 4.dp)
    ) {
        Icon(
            Icons.Default.Public, null,
            tint = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            "Luôn hiển thị công khai (thông tin cấp cứu quan trọng)",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.tertiary
        )
    }
}

/**
 * Section-level privacy checkbox. Shown only when the profile is private.
 * Lets the user hide a whole collection section (medications, conditions,
 * insurance) from the public view by adding its key to hiddenFields.
 */
@Composable
private fun SectionPrivacyControl(
    fieldKey: String,
    label: String,
    isPrivate: Boolean,
    hiddenFields: List<String>,
    onToggleHide: (Boolean) -> Unit
) {
    if (!isPrivate) return
    val isHidden = hiddenFields.contains(fieldKey)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)
    ) {
        Checkbox(checked = isHidden, onCheckedChange = { onToggleHide(it) })
        Text(
            "Ẩn \"$label\" khỏi hồ sơ công khai",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SubItemCard(label: String, onDelete: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
            IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Default.Close, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
            }
        }
    }
}

// ── Add-item dialogs ───────────────────────────────────────────────────────────

@Composable
private fun AddEmergencyContactDialog(onAdd: (EmergencyContact) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var rel by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest = onDismiss,
        title = { Text("Thêm Liên Hệ Khẩn Cấp") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Họ tên") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Số điện thoại") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = rel, onValueChange = { rel = it }, label = { Text("Mối quan hệ (VD: Vợ, Cha)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            }
        },
        confirmButton = { Button(onClick = { onAdd(EmergencyContact(name, phone, rel)) }, enabled = name.isNotBlank() && phone.isNotBlank()) { Text("Thêm") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Hủy") } }
    )
}

@Composable
private fun AddAllergyDialog(onAdd: (Allergy) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var severity by remember { mutableStateOf("Nhẹ") }
    var reaction by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest = onDismiss,
        title = { Text("Thêm Dị Ứng") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Tên dị ứng") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Nhẹ","Vừa","Nặng").forEach { s -> FilterChip(selected = severity == s, onClick = { severity = s }, label = { Text(s) }) }
                }
                OutlinedTextField(value = reaction, onValueChange = { reaction = it }, label = { Text("Phản ứng") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            }
        },
        confirmButton = { Button(onClick = { onAdd(Allergy(name, severity, reaction)) }, enabled = name.isNotBlank()) { Text("Thêm") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Hủy") } }
    )
}

@Composable
private fun AddMedicationDialog(onAdd: (Medication) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var dosage by remember { mutableStateOf("") }
    var frequency by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest = onDismiss,
        title = { Text("Thêm Thuốc") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Tên thuốc") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = dosage, onValueChange = { dosage = it }, label = { Text("Liều lượng (VD: 500mg)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = frequency, onValueChange = { frequency = it }, label = { Text("Tần suất (VD: 2 lần/ngày)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            }
        },
        confirmButton = { Button(onClick = { onAdd(Medication(name, dosage, frequency)) }, enabled = name.isNotBlank()) { Text("Thêm") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Hủy") } }
    )
}

@Composable
private fun AddConditionDialog(onAdd: (MedicalCondition) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest = onDismiss,
        title = { Text("Thêm Bệnh Lý") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Tên bệnh") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = date, onValueChange = { date = it }, label = { Text("Ngày chẩn đoán (VD: 2020-01)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Ghi chú") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = { Button(onClick = { onAdd(MedicalCondition(name, date, notes)) }, enabled = name.isNotBlank()) { Text("Thêm") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Hủy") } }
    )
}

@Composable
private fun AddInsuranceDialog(onAdd: (Insurance) -> Unit, onDismiss: () -> Unit) {
    var provider by remember { mutableStateOf("") }
    var policy by remember { mutableStateOf("") }
    var expiry by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest = onDismiss,
        title = { Text("Thêm Bảo Hiểm") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = provider, onValueChange = { provider = it }, label = { Text("Nhà cung cấp (VD: Bảo Việt)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = policy, onValueChange = { policy = it }, label = { Text("Số hợp đồng") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = expiry, onValueChange = { expiry = it }, label = { Text("Ngày hết hạn (VD: 2026-12-31)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            }
        },
        confirmButton = { Button(onClick = { onAdd(Insurance(provider, policy, expiry)) }, enabled = provider.isNotBlank()) { Text("Thêm") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Hủy") } }
    )
}

// ─── Health-document gallery + uploader ──────────────────────────────────────

/**
 * Horizontal row of uploaded health-document thumbnails plus an "Add" tile
 * that triggers the image picker. URLs returned by the backend are relative
 * (`/uploads/...`) so we route them through ApiClient.uploadUrl() to get the
 * absolute URL that Coil can fetch over HTTP.
 */
@Composable
private fun HealthDocumentsRow(
    urls: List<String>,
    onUploadClick: () -> Unit,
    onRemove: (String) -> Unit,
    isUploading: Boolean
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    ) {
        // "Add" tile — always visible at the start
        item {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .size(96.dp)
                    .clip(RoundedCornerShape(10.dp))
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    if (isUploading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    } else {
                        // We use a wrapping Surface for click semantics so the
                        // ripple matches the rounded corners.
                        Surface(
                            onClick = onUploadClick,
                            color = Color.Transparent,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    Icons.Default.AddPhotoAlternate, null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Tải lên",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        }

        // One tile per uploaded document
        items(urls) { url ->
            Box(modifier = Modifier.size(96.dp)) {
                AsyncImage(
                    model = ApiClient.uploadUrl(url),
                    contentDescription = "Tài liệu y tế",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(10.dp))
                )
                // Delete badge in top-right
                Surface(
                    shape = androidx.compose.foundation.shape.CircleShape,
                    color = Color.Black.copy(alpha = 0.6f),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(24.dp)
                ) {
                    IconButton(
                        onClick = { onRemove(url) },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Default.Close, "Xóa", tint = Color.White,
                            modifier = Modifier.size(14.dp))
                    }
                }
            }
        }
    }
}
