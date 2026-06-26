package com.qrhealthcare.app.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.*
import androidx.navigation.compose.*
import com.qrhealthcare.app.ui.screens.admin.AdminDashboardScreen
import com.qrhealthcare.app.ui.screens.auth.LoginScreen
import com.qrhealthcare.app.ui.screens.auth.RegisterScreen
import com.qrhealthcare.app.ui.screens.home.HomeScreen
import com.qrhealthcare.app.ui.screens.orders.OrderHistoryScreen
import com.qrhealthcare.app.ui.screens.profile.CreateProfileScreen
import com.qrhealthcare.app.ui.screens.profile.ManageProfileScreen
import com.qrhealthcare.app.ui.screens.publicprofile.PublicProfileScreen
import com.qrhealthcare.app.ui.screens.settings.AccountSettingsScreen
import com.qrhealthcare.app.ui.screens.shop.*
import com.qrhealthcare.app.ui.viewmodel.AuthViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

// ─── Route constants ──────────────────────────────────────────────────────────
object Routes {
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val HOME = "home"
    const val SHOP = "shop"
    const val PRODUCT_DETAIL = "shop/{slug}"
    const val CART = "cart"
    const val CHECKOUT = "checkout"
    const val PAYMENT = "payment"
    const val ORDER_SUCCESS = "order_success"
    const val PROFILES = "profiles"
    const val CREATE_PROFILE = "profile/create?type={type}&id={id}"
    const val PUBLIC_PROFILE = "public/{tagCode}"
    const val ADMIN = "admin"
    const val SETTINGS = "settings"
    const val ORDER_HISTORY = "orders"
    const val USER_GUIDE = "user_guide"

    fun productDetail(slug: String) = "shop/$slug"
    fun publicProfile(tagCode: String) = "public/$tagCode"
    fun createProfile(type: String = "human", id: String = "") = "profile/create?type=$type&id=$id"
}

// ─── Bottom nav items ─────────────────────────────────────────────────────────
data class BottomNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val adminOnly: Boolean = false
)

val bottomNavItems = listOf(
    BottomNavItem(Routes.HOME, "Trang Chủ", Icons.Default.Home),
    BottomNavItem(Routes.SHOP, "Cửa Hàng", Icons.Default.ShoppingBag),
    BottomNavItem(Routes.PROFILES, "Hồ Sơ", Icons.Default.Person),
    BottomNavItem(Routes.ADMIN, "Admin", Icons.Default.Dashboard, adminOnly = true),
    BottomNavItem(Routes.SETTINGS, "Tài Khoản", Icons.Default.Settings),
)

// ─── Root Navigation ──────────────────────────────────────────────────────────
@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = com.qrhealthcare.app.ui.util.activityViewModel()
    val authState by authViewModel.authState.collectAsState()
    val cartViewModel: com.qrhealthcare.app.ui.viewmodel.CartViewModel = com.qrhealthcare.app.ui.util.activityViewModel()
    val cartState by cartViewModel.state.collectAsState()
    val currentRoute by navController.currentBackStackEntryAsState()
    val scope = rememberCoroutineScope()

    // Determine start destination based on login state
    val startDestination = if (authState.isLoggedIn) Routes.HOME else Routes.LOGIN

    // Hide the bubble on screens where it would be redundant or in the way.
    val hideBubbleOn = setOf(
        Routes.LOGIN, Routes.REGISTER,
        Routes.CART, Routes.PAYMENT,
        Routes.PUBLIC_PROFILE,
        Routes.CREATE_PROFILE
    )
    // Use substringBefore("/{") then ("?") to strip nav-arg templates so
    // routes like "shop/{slug}" or "profile/create?type={type}&id={id}" match
    // the runtime route "shop/foo" or "profile/create?type=human".
    val routeNow = currentRoute?.destination?.route.orEmpty()
    val showCartBubble = authState.isLoggedIn &&
        hideBubbleOn.none { hidden ->
            val base = hidden.substringBefore("/{").substringBefore("?")
            routeNow == hidden || routeNow.startsWith(base)
        }

    androidx.compose.foundation.layout.Box(modifier = Modifier.fillMaxSize()) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // Auth screens (no bottom nav)
        composable(Routes.LOGIN) {
            LoginScreen(
                onLoginSuccess = { navController.navigate(Routes.HOME) { popUpTo(Routes.LOGIN) { inclusive = true } } },
                onNavigateToRegister = { navController.navigate(Routes.REGISTER) }
            )
        }
        composable(Routes.REGISTER) {
            RegisterScreen(
                onRegisterSuccess = { navController.navigate(Routes.HOME) { popUpTo(Routes.REGISTER) { inclusive = true } } },
                onNavigateToLogin = { navController.popBackStack() }
            )
        }

        // Public profile — accessible without login (from QR scan deep link)
        composable(
            route = Routes.PUBLIC_PROFILE,
            arguments = listOf(navArgument("tagCode") { type = NavType.StringType }),
            deepLinks = listOf(navDeepLink { uriPattern = "qrhealthcare://profile/{tagCode}" })
        ) { backstack ->
            val tagCode = backstack.arguments?.getString("tagCode") ?: ""
            PublicProfileScreen(tagCode = tagCode, onBack = { navController.popBackStack() })
        }

        // Main app with bottom navigation
        composable(Routes.HOME) { MainScaffold(navController, authState.userRole) { HomeScreen(navController) } }
        composable(Routes.SHOP) { MainScaffold(navController, authState.userRole) { ShopScreen(navController) } }
        composable(
            Routes.PRODUCT_DETAIL,
            arguments = listOf(navArgument("slug") { type = NavType.StringType })
        ) { backstack ->
            val slug = backstack.arguments?.getString("slug") ?: ""
            ProductDetailScreen(slug = slug, onBack = { navController.popBackStack() }, navController = navController)
        }
        composable(Routes.CART) {
            CartScreen(navController = navController)
        }
        composable(Routes.CHECKOUT) {
            CheckoutScreen(navController = navController)
        }
        composable(Routes.PAYMENT) {
            PaymentScreen(navController = navController)
        }
        composable(Routes.PROFILES) { MainScaffold(navController, authState.userRole) { ManageProfileScreen(navController) } }
        composable(
            Routes.CREATE_PROFILE,
            arguments = listOf(
                navArgument("type") { defaultValue = "human" },
                navArgument("id") { defaultValue = "" }
            )
        ) { backstack ->
            val type = backstack.arguments?.getString("type") ?: "human"
            val id = backstack.arguments?.getString("id") ?: ""
            CreateProfileScreen(
                profileType = type,
                editProfileId = id.ifBlank { null },
                onSaved = { navController.popBackStack() },
                onBack = { navController.popBackStack() }
            )
        }
        composable(Routes.ADMIN) { MainScaffold(navController, authState.userRole) { AdminDashboardScreen() } }
        composable(Routes.SETTINGS) {
            MainScaffold(navController, authState.userRole) {
                AccountSettingsScreen(
                    onLogout = {
                        scope.launch {
                            authViewModel.logout()
                            navController.navigate(Routes.LOGIN) { popUpTo(0) { inclusive = true } }
                        }
                    },
                    onOrderHistory = { navController.navigate(Routes.ORDER_HISTORY) },
                    onUserGuide = { navController.navigate(Routes.USER_GUIDE) }
                )
            }
        }
        composable(Routes.ORDER_HISTORY) {
            OrderHistoryScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.USER_GUIDE) {
            com.qrhealthcare.app.ui.screens.settings.UserGuideScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }

    // Floating cart bubble overlay — sits above NavHost so it appears
    // on top of whatever screen is currently rendered.
    if (showCartBubble) {
        com.qrhealthcare.app.ui.components.FloatingCartBubble(
            itemCount = cartState.items.sumOf { it.quantity },
            onClick = { navController.navigate(Routes.CART) }
        )
    }

    // Floating QR bubble — same visibility rules as cart bubble.
    // Tap opens a bottom sheet listing the user's profiles and their QR codes.
    if (showCartBubble) {
        var showQrPicker by remember { mutableStateOf(false) }
        com.qrhealthcare.app.ui.components.FloatingQrBubble(
            onClick = { showQrPicker = true }
        )
        if (showQrPicker) {
            com.qrhealthcare.app.ui.components.QrPickerDialog(
                onDismiss = { showQrPicker = false }
            )
        }
    }
    } // close wrapping Box
}

// ─── Scaffold with Bottom Nav ─────────────────────────────────────────────────
@Composable
fun MainScaffold(
    navController: NavController,
    userRole: String,
    content: @Composable () -> Unit
) {
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
    val visibleItems = bottomNavItems.filter { !it.adminOnly || userRole == "admin" }

    Scaffold(
        bottomBar = {
            NavigationBar {
                visibleItems.forEach { item ->
                    NavigationBarItem(
                        selected = currentRoute == item.route,
                        onClick = {
                            if (currentRoute != item.route) {
                                navController.navigate(item.route) {
                                    popUpTo(Routes.HOME) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }
        }
    ) { padding ->
        androidx.compose.foundation.layout.Box(
            modifier = Modifier.padding(padding)
        ) { content() }
    }
}
