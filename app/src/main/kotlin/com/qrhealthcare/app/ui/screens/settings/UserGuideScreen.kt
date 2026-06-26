@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.qrhealthcare.app.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Step-by-step user guide shown from Account → Hỗ Trợ → Hướng Dẫn Sử Dụng.
 * Walks a new user through the full lifecycle: create profile → buy sticker →
 * link QR → what happens when scanned. Plus a short FAQ.
 */
@Composable
fun UserGuideScreen(onBack: () -> Unit) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Hướng Dẫn Sử Dụng", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Quay lại")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Intro ──────────────────────────────────────────────────────────
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.FavoriteBorder, null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "QR Healthcare giúp bạn lưu thông tin y tế khẩn cấp vào mã QR. " +
                            "Khi gặp sự cố, người cứu hộ quét mã là thấy ngay thông tin quan trọng của bạn.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            // ── Steps ──────────────────────────────────────────────────────────
            Text("Các Bước Sử Dụng", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            GuideStep(
                number = 1,
                icon = Icons.Default.PersonAdd,
                title = "Tạo hồ sơ y tế",
                body = "Vào tab \"Hồ Sơ\" → bấm \"Tạo hồ sơ mới\". Điền họ tên, nhóm máu, " +
                    "liên hệ khẩn cấp, dị ứng và các thông tin y tế khác. Bạn có thể tạo hồ sơ " +
                    "cho người thân hoặc thú cưng."
            )
            GuideStep(
                number = 2,
                icon = Icons.Default.ShoppingBag,
                title = "Mua sticker / thẻ QR",
                body = "Vào tab \"Cửa Hàng\", chọn sản phẩm (sticker y tế, thẻ, tag móc khóa, " +
                    "tag thú cưng...) và thêm vào giỏ. Thanh toán bằng VietQR hoặc COD " +
                    "(thanh toán khi nhận hàng)."
            )
            GuideStep(
                number = 3,
                icon = Icons.Default.QrCode2,
                title = "Liên kết mã QR với hồ sơ",
                body = "Sau khi đặt hàng, mã QR được tạo ngay trong app. Vào tab \"Hồ Sơ\" → " +
                    "chọn hồ sơ → liên kết mã QR. Bạn có thể dùng ngay cả khi sticker vật lý " +
                    "đang trên đường giao đến."
            )
            GuideStep(
                number = 4,
                icon = Icons.Default.CameraAlt,
                title = "Khi cần cấp cứu",
                body = "Bất kỳ ai cũng có thể dùng camera điện thoại quét mã QR trên sticker của bạn. " +
                    "Mã sẽ mở trang web hiển thị thông tin y tế khẩn cấp — không cần cài app."
            )

            // ── Privacy explainer ──────────────────────────────────────────────
            Text("Quyền Riêng Tư", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Card(shape = RoundedCornerShape(12.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    GuideBullet("Bật \"Chế Độ Riêng Tư\" trong hồ sơ để ẩn bớt thông tin.")
                    GuideBullet("Nếu không tích ô nào: chỉ hiện thông tin cấp cứu quan trọng " +
                        "(tên, giới tính, nhóm máu, liên hệ khẩn cấp, dị ứng).")
                    GuideBullet("Nếu tích ô: chỉ những mục đã tích bị ẩn, còn lại vẫn hiển thị.")
                    GuideBullet("Tên, nhóm máu, liên hệ khẩn cấp và dị ứng luôn hiển thị " +
                        "vì rất quan trọng khi cấp cứu.")
                }
            }

            // ── FAQ ────────────────────────────────────────────────────────────
            Text("Câu Hỏi Thường Gặp", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            FaqItem(
                "Sticker bị mất thì sao?",
                "Mã QR của bạn vẫn lưu trên hệ thống. Bạn có thể mua sticker che phủ thêm " +
                    "hoặc đặt lại sản phẩm và liên kết với hồ sơ cũ."
            )
            FaqItem(
                "Người quét QR có thấy hết thông tin của tôi không?",
                "Không. Họ chỉ thấy những gì bạn cho phép hiển thị. Bật chế độ riêng tư " +
                    "để kiểm soát từng mục thông tin."
            )
            FaqItem(
                "Tôi cần mạng để mã QR hoạt động không?",
                "Người quét cần có mạng để mở trang thông tin. Mã QR chỉ chứa đường dẫn, " +
                    "không lưu dữ liệu trực tiếp trên sticker."
            )
            FaqItem(
                "Tôi tạo được bao nhiêu hồ sơ?",
                "Mỗi tài khoản tạo được tối đa 5 hồ sơ — đủ cho bản thân, gia đình và thú cưng."
            )

            Spacer(Modifier.height(8.dp))
            Text(
                "Cần thêm trợ giúp? Vào Hỗ Trợ → Liên Hệ Hỗ Trợ.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun GuideStep(number: Int, icon: ImageVector, title: String, body: String) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp)) {
            // Numbered circle
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    number.toString(),
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(icon, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(4.dp))
                Text(body, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun GuideBullet(text: String) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(
            Icons.Default.CheckCircle, null,
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(16.dp).padding(top = 2.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun FaqItem(question: String, answer: String) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Icon(Icons.Default.Help, null,
                    tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(question, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(6.dp))
            Text(answer, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 26.dp))
        }
    }
}
