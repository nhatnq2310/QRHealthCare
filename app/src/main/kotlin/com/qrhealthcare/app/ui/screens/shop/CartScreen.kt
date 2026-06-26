package com.qrhealthcare.app.ui.screens.shop

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.qrhealthcare.app.data.model.CartItem
import com.qrhealthcare.app.data.model.Coupon
import com.qrhealthcare.app.ui.components.formatVND
import com.qrhealthcare.app.ui.navigation.Routes
import com.qrhealthcare.app.ui.viewmodel.AuthViewModel
import com.qrhealthcare.app.ui.viewmodel.CartViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartScreen(
    navController: NavController,
    cartViewModel: CartViewModel = com.qrhealthcare.app.ui.util.activityViewModel(),
    authViewModel: AuthViewModel = com.qrhealthcare.app.ui.util.activityViewModel()
) {
    val cartState by cartViewModel.state.collectAsState()
    val authState by authViewModel.authState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Giỏ Hàng (${cartViewModel.totalItems})") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (cartState.items.isEmpty()) {
            // Empty cart
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Default.ShoppingCart, contentDescription = null,
                        modifier = Modifier.size(72.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Giỏ hàng trống", style = MaterialTheme.typography.titleMedium)
                    Button(onClick = { navController.navigate(Routes.SHOP) }) { Text("Tiếp tục mua sắm") }
                    if (!authState.isLoggedIn) {
                        TextButton(onClick = { navController.navigate(Routes.LOGIN) }) {
                            Text("Đăng nhập để thanh toán nhanh hơn")
                        }
                    }
                }
            }
            return@Scaffold
        }

        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(cartState.items, key = { it.product.id }) { item ->
                    CartItemRow(
                        item = item,
                        onQuantityChange = { cartViewModel.updateQuantity(item.product.id, it) },
                        onRemove = { cartViewModel.removeItem(item.product.id) }
                    )
                }
            }

            // Coupon panel — sits just above the order summary
            CouponPanel(
                appliedCoupon = cartState.appliedCoupon,
                discountAmount = cartState.discountAmount,
                isApplying = cartState.isApplyingCoupon,
                error = cartState.couponError,
                onApply = { code -> cartViewModel.applyCoupon(code) },
                onClear = { cartViewModel.clearCoupon() }
            )

            // Order summary
            Surface(tonalElevation = 4.dp) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    // Subtotal
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Tạm tính", style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(formatVND(cartViewModel.subtotal), style = MaterialTheme.typography.bodyMedium)
                    }
                    // Discount line — only shown when a coupon is applied
                    if (cartState.discountAmount > 0L) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(
                                "Giảm giá" + (cartState.appliedCoupon?.code?.let { " ($it)" } ?: ""),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                "-" + formatVND(cartState.discountAmount),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(8.dp))
                    // Final total
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Tổng cộng", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(formatVND(cartViewModel.finalTotal), style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                    Text("Đã bao gồm thuế. Phí vận chuyển tính khi thanh toán.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            if (!authState.isLoggedIn) { navController.navigate(Routes.LOGIN); return@Button }
                            // No saved address → collect shipping details first.
                            // Otherwise go straight to payment (address can still be
                            // edited there if needed).
                            if (authState.address.isBlank()) {
                                navController.navigate(Routes.CHECKOUT)
                            } else {
                                navController.navigate(Routes.PAYMENT)
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) { Text("TIẾN HÀNH THANH TOÁN", fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
}

/**
 * Tappable panel for applying / removing a discount code. The actual
 * validation is server-side — we just show the result. When [appliedCoupon]
 * is non-null we render a "code applied" chip with a remove button;
 * otherwise we render the input field + Apply button.
 */
@Composable
private fun CouponPanel(
    appliedCoupon: Coupon?,
    discountAmount: Long,
    isApplying: Boolean,
    error: String?,
    onApply: (String) -> Unit,
    onClear: () -> Unit
) {
    var code by remember { mutableStateOf("") }
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (appliedCoupon != null)
                MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.LocalOffer, null, tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Mã Giảm Giá", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(8.dp))

            if (appliedCoupon != null) {
                // Applied state — code chip + remove button + savings line
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primary
                    ) {
                        Text(
                            appliedCoupon.code,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Tiết kiệm ${formatVND(discountAmount)}",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = onClear) { Text("Xóa") }
                }
                if (appliedCoupon.description.isNotBlank()) {
                    Text(appliedCoupon.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                // Input state — text field + apply button
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = code,
                        onValueChange = { code = it.uppercase() },
                        placeholder = { Text("Nhập mã (VD: WELCOME10)") },
                        singleLine = true,
                        enabled = !isApplying,
                        isError = error != null,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onApply(code) },
                        enabled = !isApplying && code.isNotBlank()
                    ) {
                        if (isApplying) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp),
                                color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                        } else Text("Áp Dụng")
                    }
                }
                if (error != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(error, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
private fun CartItemRow(
    item: CartItem,
    onQuantityChange: (Int) -> Unit,
    onRemove: () -> Unit
) {
    Card(shape = RoundedCornerShape(12.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = item.product.imageUrl,
                contentDescription = item.product.name,
                modifier = Modifier.size(72.dp),
                contentScale = ContentScale.Fit
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(item.product.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, maxLines = 2)
                Text(formatVND(item.product.price), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (item.emergencyContact.isNotBlank()) {
                    Text("SĐT khẩn cấp: ${item.emergencyContact}", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedIconButton(
                        onClick = { onQuantityChange(item.quantity - 1) },
                        modifier = Modifier.size(30.dp)
                    ) { Icon(Icons.Default.Remove, contentDescription = null, modifier = Modifier.size(14.dp)) }
                    Text(item.quantity.toString(), modifier = Modifier.padding(horizontal = 12.dp),
                        fontWeight = FontWeight.Bold)
                    OutlinedIconButton(
                        onClick = { onQuantityChange(item.quantity + 1) },
                        modifier = Modifier.size(30.dp)
                    ) { Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp)) }
                    Spacer(modifier = Modifier.weight(1f))
                    Text(formatVND(item.product.price * item.quantity),
                        style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                }
            }
            IconButton(onClick = onRemove) {
                Icon(Icons.Default.Delete, contentDescription = "Xóa", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}
