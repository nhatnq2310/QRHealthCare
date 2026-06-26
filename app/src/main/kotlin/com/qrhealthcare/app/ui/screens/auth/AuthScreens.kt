package com.qrhealthcare.app.ui.screens.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.qrhealthcare.app.ui.viewmodel.AuthViewModel
import com.qrhealthcare.app.util.Validators

// ─── LOGIN ────────────────────────────────────────────────────────────────────

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onNavigateToRegister: () -> Unit,
    onNavigateToForgot: () -> Unit = {},
    viewModel: AuthViewModel = com.qrhealthcare.app.ui.util.activityViewModel()
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    val state by viewModel.authState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(48.dp))

        // Logo
        Surface(
            color = MaterialTheme.colorScheme.primary,
            shape = MaterialTheme.shapes.small
        ) {
            Text(
                text = "QR",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Healthcare",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(40.dp))
        Text(
            text = "Đăng Nhập Tài Khoản",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(24.dp))

        // Email
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(12.dp))

        // Password
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Mật khẩu") },
            singleLine = true,
            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                IconButton(onClick = { showPassword = !showPassword }) {
                    Icon(
                        if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = null
                    )
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        // Forgot-password link, right-aligned
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onNavigateToForgot) {
                Text("Quên mật khẩu?", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary)
            }
        }

        if (errorMsg != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = errorMsg!!,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = {
                errorMsg = Validators.email(email) ?: Validators.password(password, minLen = 1)
                if (errorMsg != null) return@Button
                viewModel.login(email.trim(), password) { success, err ->
                    if (success) onLoginSuccess() else errorMsg = err
                }
            },
            enabled = !state.isLoading,
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Text("Đăng Nhập", style = MaterialTheme.typography.labelLarge)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Chưa có tài khoản? ", style = MaterialTheme.typography.bodyMedium)
            TextButton(onClick = onNavigateToRegister) {
                Text("Đăng Ký", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ─── REGISTER ─────────────────────────────────────────────────────────────────

@Composable
fun RegisterScreen(
    onRegisterSuccess: () -> Unit,
    onNavigateToLogin: () -> Unit,
    viewModel: AuthViewModel = com.qrhealthcare.app.ui.util.activityViewModel()
) {
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    val state by viewModel.authState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(48.dp))

        Surface(
            color = MaterialTheme.colorScheme.primary,
            shape = MaterialTheme.shapes.small
        ) {
            Text(
                text = "QR",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text("Healthcare", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)

        Spacer(modifier = Modifier.height(40.dp))
        Text("Tạo Tài Khoản Mới", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = fullName,
            onValueChange = { fullName = it },
            label = { Text("Họ và Tên") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Mật khẩu (ít nhất 6 ký tự)") },
            singleLine = true,
            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                IconButton(onClick = { showPassword = !showPassword }) {
                    Icon(if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility, null)
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            label = { Text("Xác nhận mật khẩu") },
            singleLine = true,
            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                IconButton(onClick = { showPassword = !showPassword }) {
                    Icon(
                        if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = null
                    )
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        if (errorMsg != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(errorMsg!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = {
                errorMsg = Validators.required(fullName, "Họ và tên")
                    ?: Validators.email(email)
                    ?: Validators.password(password)
                    ?: if (password != confirmPassword) "Mật khẩu xác nhận không khớp" else null
                if (errorMsg != null) return@Button
                viewModel.register(email.trim(), password, fullName.trim()) { success, err ->
                    if (success) onRegisterSuccess() else errorMsg = err
                }
            },
            enabled = !state.isLoading,
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
            } else {
                Text("Đăng Ký", style = MaterialTheme.typography.labelLarge)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Đã có tài khoản? ", style = MaterialTheme.typography.bodyMedium)
            TextButton(onClick = onNavigateToLogin) {
                Text("Đăng Nhập", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ─── FORGOT PASSWORD ──────────────────────────────────────────────────────────

/**
 * Email-less password reset. The user proves ownership by entering their
 * email AND the exact full name on the account, then sets a new password.
 * (For a real launch, swap this for an emailed reset link.)
 */
@Composable
fun ForgotPasswordScreen(
    onBack: () -> Unit,
    onResetSuccess: () -> Unit,
    viewModel: AuthViewModel = com.qrhealthcare.app.ui.util.activityViewModel()
) {
    var email by remember { mutableStateOf("") }
    var fullName by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var successMsg by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(40.dp))
        Text("Đặt Lại Mật Khẩu", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Nhập email và họ tên chính xác trên tài khoản để xác minh, sau đó đặt mật khẩu mới.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(28.dp))

        OutlinedTextField(
            value = email, onValueChange = { email = it },
            label = { Text("Email") }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = fullName, onValueChange = { fullName = it },
            label = { Text("Họ và tên (như khi đăng ký)") }, singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = newPassword, onValueChange = { newPassword = it },
            label = { Text("Mật khẩu mới") }, singleLine = true,
            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                IconButton(onClick = { showPassword = !showPassword }) {
                    Icon(if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility, null)
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = confirmPassword, onValueChange = { confirmPassword = it },
            label = { Text("Xác nhận mật khẩu mới") }, singleLine = true,
            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth()
        )

        if (errorMsg != null) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(errorMsg!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
        if (successMsg != null) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(successMsg!!, color = MaterialTheme.colorScheme.tertiary,
                style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        }

        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = {
                errorMsg = when {
                    email.isBlank() -> "Vui lòng nhập email"
                    fullName.isBlank() -> "Vui lòng nhập họ tên"
                    newPassword.length < 6 -> "Mật khẩu phải có ít nhất 6 ký tự"
                    newPassword != confirmPassword -> "Mật khẩu xác nhận không khớp"
                    else -> null
                }
                if (errorMsg != null) return@Button
                loading = true
                viewModel.resetPassword(email.trim(), fullName.trim(), newPassword) { ok, msg ->
                    loading = false
                    if (ok) {
                        successMsg = "✓ ${msg ?: "Đặt lại mật khẩu thành công"}. Đang chuyển về đăng nhập..."
                        errorMsg = null
                        onResetSuccess()
                    } else {
                        errorMsg = msg
                    }
                }
            },
            enabled = !loading,
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            if (loading) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
            } else {
                Text("Đặt Lại Mật Khẩu", style = MaterialTheme.typography.labelLarge)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        TextButton(onClick = onBack) {
            Text("Quay lại đăng nhập", color = MaterialTheme.colorScheme.primary)
        }
    }
}
