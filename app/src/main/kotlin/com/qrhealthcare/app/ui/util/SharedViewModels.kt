package com.qrhealthcare.app.ui.util

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.compose.ui.platform.LocalContext

/**
 * Returns a Hilt-injected ViewModel scoped to the **Activity**, not the
 * current NavBackStackEntry. Use this for any ViewModel whose state must be
 * shared across multiple screens — e.g. AuthViewModel (so the logged-in
 * user info stays consistent everywhere) and CartViewModel (so items added
 * on the product detail screen are still there on the cart screen).
 *
 * Plain `hiltViewModel()` creates a brand-new instance per screen entry,
 * which means state lives only as long as you're on that screen. That's
 * correct for screen-local ViewModels (ProfileViewModel, ShopViewModel)
 * but wrong for app-wide state.
 */
@Composable
inline fun <reified VM : ViewModel> activityViewModel(): VM {
    val activity = LocalContext.current as ComponentActivity
    return hiltViewModel(activity)
}
