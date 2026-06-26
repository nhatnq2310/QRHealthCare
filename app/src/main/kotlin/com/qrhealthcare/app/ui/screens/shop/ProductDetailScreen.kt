package com.qrhealthcare.app.ui.screens.shop

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
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.qrhealthcare.app.ui.components.formatVND
import com.qrhealthcare.app.ui.navigation.Routes
import com.qrhealthcare.app.ui.viewmodel.CartViewModel
import com.qrhealthcare.app.ui.viewmodel.ShopViewModel



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailScreen(
    slug: String,
    onBack: () -> Unit,
    navController: NavController,
    viewModel: ShopViewModel = hiltViewModel(),
    cartViewModel: CartViewModel = com.qrhealthcare.app.ui.util.activityViewModel()
) {
    val state by viewModel.state.collectAsState()
    var quantity by remember { mutableIntStateOf(1) }
    var emergencyContact by remember { mutableStateOf("") }
    var contactError by remember { mutableStateOf<String?>(null) }
    var addedToCart by remember { mutableStateOf(false) }

    LaunchedEffect(slug) { viewModel.loadProductBySlug(slug) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chi Tiết Sản Phẩm") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        val product = state.selectedProduct

        if (state.isLoading || product == null) {
            Box(Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // Product image
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.White)
                    .height(280.dp),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = com.qrhealthcare.app.data.api.ApiClient.uploadUrl(product.imageUrl),
                    contentDescription = product.name,
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    contentScale = ContentScale.Fit
                )
            }

            Column(modifier = Modifier.padding(16.dp)) {
                // Badge
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        product.badge,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))

                Text(product.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))

                // Price row
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (product.oldPrice != null) {
                        Text(formatVND(product.oldPrice), style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant, textDecoration = TextDecoration.LineThrough)
                    }
                    Text(formatVND(product.price), style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
                Text("Đã bao gồm thuế.", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)

                // Low stock warning
                if (product.lowStock) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("⚠️ Sắp hết hàng! Nhanh tay 🔥", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                    LinearProgressIndicator(
                        progress = { 0.3f },
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // Emergency contact phone field — replaces the old blood-type selector.
                // Only shown for products that need a contact (e.g. the medical sticker combo);
                // for most products this section is hidden.
                if (product.emergencyContactRequired) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Số Điện Thoại Khẩn Cấp", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    Text(
                        "Số sẽ được lưu cùng đơn hàng, dùng để liên hệ khi có tình huống y tế khẩn cấp.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = emergencyContact,
                        onValueChange = {
                            // Phone digits and a leading + only.
                            emergencyContact = it.filter { c -> c.isDigit() || c == '+' }.take(15)
                            contactError = null
                        },
                        placeholder = { Text("VD: 0901234567") },
                        singleLine = true,
                        isError = contactError != null,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Phone
                        ),
                        leadingIcon = { Icon(Icons.Default.Phone, null) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (contactError != null) {
                        Text(contactError!!, color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall)
                    }
                }

                // Quantity selector
                Spacer(modifier = Modifier.height(16.dp))
                Text("Số Lượng", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedIconButton(onClick = { if (quantity > 1) quantity-- }) {
                        Icon(Icons.Default.Remove, contentDescription = "Giảm")
                    }
                    Text(
                        text = quantity.toString(),
                        modifier = Modifier.padding(horizontal = 20.dp),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    OutlinedIconButton(onClick = { quantity++ }) {
                        Icon(Icons.Default.Add, contentDescription = "Tăng")
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Validate then add; reused by both Add-to-cart and Buy-now.
                fun tryAddToCart(thenNavigate: Boolean) {
                    if (product.emergencyContactRequired && emergencyContact.length < 9) {
                        contactError = "Vui lòng nhập số điện thoại hợp lệ (ít nhất 9 chữ số)"
                        return
                    }
                    cartViewModel.addItem(
                        product, quantity,
                        if (product.emergencyContactRequired) emergencyContact else ""
                    )
                    if (thenNavigate) navController.navigate(Routes.CART)
                    else addedToCart = true
                }

                // Add to cart
                OutlinedButton(
                    onClick = { tryAddToCart(thenNavigate = false) },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) {
                    Icon(Icons.Default.ShoppingCart, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (addedToCart) "✓ Đã Thêm Vào Giỏ!" else "Thêm Vào Giỏ")
                }
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = { tryAddToCart(thenNavigate = true) },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) { Text("MUA NGAY", fontWeight = FontWeight.Bold) }

                Spacer(modifier = Modifier.height(20.dp))

                // Description
                Text("Số Lượng - ${product.quantity}", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(product.description, style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)

                // Specs
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                SpecRow("📐 Kích Thước", product.dimensions)
                HorizontalDivider()
                SpecRow("💎 Chất Liệu", product.materials)
                HorizontalDivider()
                SpecRow("⚡ Độ Bền", product.durability.joinToString(" · "))
                HorizontalDivider()
                SpecRow("🚚 Vận Chuyển", product.shipping)
                HorizontalDivider()

                // Trust badges
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    TrustBadge("🔒", "Thanh Toán\nAn Toàn")
                    TrustBadge("🚀", "Giao Hàng\nNhanh")
                    TrustBadge("🕐", "Hỗ Trợ\n24/7")
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

@Composable
private fun SpecRow(title: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
        Text(title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun TrustBadge(emoji: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(emoji, style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(4.dp))
        Text(label, style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}

