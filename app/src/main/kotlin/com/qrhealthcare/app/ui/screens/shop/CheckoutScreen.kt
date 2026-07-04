@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.qrhealthcare.app.ui.screens.shop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.qrhealthcare.app.data.model.ShippingAddress
import com.qrhealthcare.app.ui.components.formatVND
import com.qrhealthcare.app.ui.navigation.Routes
import com.qrhealthcare.app.ui.util.activityViewModel
import com.qrhealthcare.app.ui.viewmodel.AuthViewModel
import com.qrhealthcare.app.ui.viewmodel.CartViewModel

/**
 * Full-page checkout / shipping-details screen. Reached from the cart when the
 * user wants to enter (or change) their delivery address before paying.
 *
 * Pre-fills from the user's saved profile/address when available. On continue,
 * stores the address into CartViewModel and proceeds to the payment screen.
 */
@Composable
fun CheckoutScreen(
    navController: NavController,
    cartViewModel: CartViewModel = activityViewModel(),
    authViewModel: AuthViewModel = activityViewModel()
) {
    val cartState by cartViewModel.state.collectAsState()
    val authState by authViewModel.authState.collectAsState()

    // Pre-fill: existing per-order address first, else fall back to saved
    // account fields (name, address, phone, city) so returning users barely
    // have to type anything.
    var fullName by remember { mutableStateOf(cartState.shippingAddress.fullName.ifBlank { authState.fullName }) }
    var phone    by remember { mutableStateOf(cartState.shippingAddress.phone.ifBlank { authState.phone }) }
    var address  by remember { mutableStateOf(cartState.shippingAddress.address.ifBlank { authState.address }) }
    var city     by remember { mutableStateOf(cartState.shippingAddress.city.ifBlank { authState.city }) }
    var note     by remember { mutableStateOf(cartState.shippingAddress.note) }

    var attemptedSubmit by remember { mutableStateOf(false) }

    // "Checkout started" for drop-out/abandonment reporting — fired once per
    // session the moment this screen is shown, before any data is entered.
    LaunchedEffect(Unit) { cartViewModel.startCheckoutTracking() }

    fun phoneValid(p: String) = p.trim().length >= 8 && p.trim().all { it.isDigit() || it in "+ -()" }

    val nameError = attemptedSubmit && fullName.isBlank()
    val phoneError = attemptedSubmit && !phoneValid(phone)
    val addressError = attemptedSubmit && address.isBlank()
    val cityError = attemptedSubmit && city.isBlank()

    val shipping = if (cartState.items.isNotEmpty()) 0L else 0L  // free shipping
    val total = cartViewModel.finalTotal + shipping

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Thông Tin Giao Hàng", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
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
            // ── Section: shipping details ──────────────────────────────────────
            SectionHeader(icon = Icons.Default.LocalShipping, title = "Địa chỉ nhận hàng")

            CheckoutField(
                value = fullName, onValueChange = { fullName = it },
                label = "Họ và tên", icon = Icons.Default.Person,
                isError = nameError, errorText = "Vui lòng nhập họ tên"
            )
            CheckoutField(
                value = phone, onValueChange = { phone = it },
                label = "Số điện thoại", icon = Icons.Default.Call,
                keyboardType = KeyboardType.Phone,
                isError = phoneError, errorText = "Số điện thoại không hợp lệ"
            )
            CheckoutField(
                value = address, onValueChange = { address = it },
                label = "Địa chỉ (số nhà, đường, phường)", icon = Icons.Default.Home,
                isError = addressError, errorText = "Vui lòng nhập địa chỉ"
            )
            CheckoutField(
                value = city, onValueChange = { city = it },
                label = "Tỉnh / Thành phố", icon = Icons.Default.LocationCity,
                isError = cityError, errorText = "Vui lòng nhập tỉnh/thành phố"
            )
            CheckoutField(
                value = note, onValueChange = { note = it },
                label = "Ghi chú giao hàng (không bắt buộc)", icon = Icons.Default.EditNote,
                isError = false, errorText = ""
            )

            // ── Section: order summary ─────────────────────────────────────────
            SectionHeader(icon = Icons.Default.ReceiptLong, title = "Tóm tắt đơn hàng")

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    cartState.items.forEach { item ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                item.product.name,
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1, overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                "×${item.quantity}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                            Text(
                                formatVND(item.product.price * item.quantity),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    SummaryRow("Tạm tính", formatVND(cartViewModel.subtotal))
                    if (cartState.discountAmount > 0L) {
                        SummaryRow(
                            "Giảm giá" + (cartState.appliedCoupon?.code?.let { " ($it)" } ?: ""),
                            "-" + formatVND(cartState.discountAmount),
                            valueColor = MaterialTheme.colorScheme.primary
                        )
                    }
                    SummaryRow("Phí giao hàng", "Miễn phí", valueColor = MaterialTheme.colorScheme.secondary)
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Tổng cộng", fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleMedium)
                        Text(
                            formatVND(total),
                            fontWeight = FontWeight.Black,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // ── Continue button ────────────────────────────────────────────────
            Button(
                onClick = {
                    attemptedSubmit = true
                    val ok = fullName.isNotBlank() && phoneValid(phone) &&
                        address.isNotBlank() && city.isNotBlank()
                    if (ok) {
                        cartViewModel.setShippingAddress(
                            ShippingAddress(
                                fullName = fullName.trim(),
                                phone = phone.trim(),
                                address = address.trim(),
                                city = city.trim(),
                                note = note.trim()
                            )
                        )
                        // Also persist to the account so these fields auto-fill
                        // next time (best-effort; checkout proceeds regardless).
                        authViewModel.updateAddress(address.trim(), phone.trim(), city.trim()) { _, _ -> }
                        navController.navigate(Routes.PAYMENT)
                    }
                },
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(14.dp),
                enabled = cartState.items.isNotEmpty()
            ) {
                Text("TIẾP TỤC THANH TOÁN", fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun SectionHeader(icon: ImageVector, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(18.dp))
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun CheckoutField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: ImageVector,
    keyboardType: KeyboardType = KeyboardType.Text,
    isError: Boolean = false,
    errorText: String = ""
) {
    Column {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            leadingIcon = { Icon(icon, null) },
            singleLine = true,
            isError = isError,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.secondary,
                focusedLabelColor = MaterialTheme.colorScheme.secondary,
                focusedLeadingIconColor = MaterialTheme.colorScheme.secondary
            )
        )
        if (isError && errorText.isNotBlank()) {
            Text(
                errorText,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
            )
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String, valueColor: Color = Color.Unspecified) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = if (valueColor == Color.Unspecified) MaterialTheme.colorScheme.onSurface else valueColor
        )
    }
}
