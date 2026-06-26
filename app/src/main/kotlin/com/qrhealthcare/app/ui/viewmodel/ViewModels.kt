package com.qrhealthcare.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qrhealthcare.app.data.model.*
import com.qrhealthcare.app.data.repository.AppRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

// ═══════════════════════════════════════════════════════════════════════════════
// AUTH VIEW MODEL
// ═══════════════════════════════════════════════════════════════════════════════

data class AuthState(
    val isInitialized: Boolean = false,  // true after the first session snapshot arrives
    val isLoggedIn: Boolean = false,
    val userId: String = "",
    val userRole: String = "user",
    val email: String = "",
    val fullName: String = "",
    val address: String = "",
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val repo: AppRepository,
    private val session: com.qrhealthcare.app.data.local.SessionManager
) : ViewModel() {

    private val _authState = MutableStateFlow(AuthState())
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    init {
        // Collect every persisted session field so the state stays accurate
        // even on a fresh AuthViewModel instance (e.g. after process death
        // or — more commonly — after the user navigates to a screen whose
        // hiltViewModel() creates a new VM tied to that backstack entry).
        viewModelScope.launch {
            combine(
                session.isLoggedIn,
                session.userId,
                session.email,
                session.fullName,
                session.address,
                session.role
            ) { values ->
                // combine(6) returns Array<Any?> in this signature — destructure positionally.
                val loggedIn = values[0] as Boolean
                val userId   = values[1] as String?
                val email    = values[2] as String?
                val fullName = values[3] as String?
                val address  = values[4] as String?
                val role     = values[5] as String?
                AuthState(
                    isInitialized = true,
                    isLoggedIn = loggedIn,
                    userId = userId.orEmpty(),
                    userRole = role?.ifBlank { "user" } ?: "user",
                    email = email.orEmpty(),
                    fullName = fullName.orEmpty(),
                    address = address.orEmpty()
                )
            }.collect { snapshot ->
                // Preserve transient UI flags (isLoading, error) while replacing the rest.
                _authState.update { current ->
                    snapshot.copy(isLoading = current.isLoading, error = current.error)
                }
            }
        }
    }

    fun login(email: String, password: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            _authState.update { it.copy(isLoading = true, error = null) }
            val result = repo.login(email, password)
            if (result.user != null) {
                _authState.update { it.copy(
                    isLoggedIn = true,
                    userId = result.user.id,
                    userRole = result.user.role,
                    email = result.user.email,
                    fullName = result.user.fullName,
                    isLoading = false
                )}
                onResult(true, null)
            } else {
                _authState.update { it.copy(isLoading = false, error = result.error) }
                onResult(false, result.error)
            }
        }
    }

    fun register(email: String, password: String, fullName: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            _authState.update { it.copy(isLoading = true, error = null) }
            val result = repo.register(email, password, fullName)
            if (result.user != null) {
                _authState.update { it.copy(
                    isLoggedIn = true,
                    userId = result.user.id,
                    userRole = result.user.role,
                    email = result.user.email,
                    fullName = result.user.fullName,
                    isLoading = false
                )}
                onResult(true, null)
            } else {
                _authState.update { it.copy(isLoading = false, error = result.error) }
                onResult(false, result.error)
            }
        }
    }

    fun logout() = viewModelScope.launch {
        repo.logout()
        _authState.value = AuthState()
    }

    /** Edit the logged-in user's shipping address. Persists to backend and to the session. */
    fun updateAddress(newAddress: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            _authState.update { it.copy(isLoading = true, error = null) }
            repo.updateUserAddress(newAddress.trim()).fold(
                onSuccess = {
                    // session.saveAddress inside the repo triggers the combine collector,
                    // which refreshes authState.address automatically.
                    _authState.update { it.copy(isLoading = false) }
                    onResult(true, null)
                },
                onFailure = { err ->
                    _authState.update { it.copy(isLoading = false, error = err.message) }
                    onResult(false, err.message)
                }
            )
        }
    }

    /** Reset password without login (verified by email + full name). */
    fun resetPassword(email: String, fullName: String, newPassword: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            repo.resetPassword(email, fullName, newPassword).fold(
                onSuccess = { onResult(true, it) },
                onFailure = { onResult(false, it.message) }
            )
        }
    }

    /** Change password for the logged-in user. */
    fun changePassword(currentPassword: String, newPassword: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            repo.changePassword(currentPassword, newPassword).fold(
                onSuccess = { onResult(true, it) },
                onFailure = { onResult(false, it.message) }
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// PROFILE VIEW MODEL
// ═══════════════════════════════════════════════════════════════════════════════

data class ProfileListState(
    val profiles: List<Profile> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

data class ProfileEditState(
    val profile: Profile = Profile(),
    val linkedTags: List<QrTag> = emptyList(),
    val isSaving: Boolean = false,
    val isLoading: Boolean = false,
    val savedSuccessfully: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val repo: AppRepository
) : ViewModel() {

    private val _listState = MutableStateFlow(ProfileListState())
    val listState: StateFlow<ProfileListState> = _listState.asStateFlow()

    private val _editState = MutableStateFlow(ProfileEditState())
    val editState: StateFlow<ProfileEditState> = _editState.asStateFlow()

    fun loadMyProfiles() {
        viewModelScope.launch {
            _listState.update { it.copy(isLoading = true, error = null) }
            repo.getMyProfiles().fold(
                onSuccess = { _listState.update { s -> s.copy(profiles = it, isLoading = false) } },
                onFailure = { _listState.update { s -> s.copy(error = it.message, isLoading = false) } }
            )
        }
    }

    fun loadProfileForEdit(id: String?) {
        viewModelScope.launch {
            if (id == null) {
                _editState.value = ProfileEditState()
                return@launch
            }
            _editState.update { it.copy(isLoading = true) }
            repo.getProfileById(id).fold(
                onSuccess = { profile ->
                    val tags = repo.getLinkedTagsForProfile(id).getOrDefault(emptyList())
                    _editState.update { it.copy(profile = profile, linkedTags = tags, isLoading = false) }
                },
                onFailure = { _editState.update { s -> s.copy(error = it.message, isLoading = false) } }
            )
        }
    }

    fun updateEditProfile(updated: Profile) {
        _editState.update { it.copy(profile = updated) }
    }

    fun saveProfile() {
        viewModelScope.launch {
            _editState.update { it.copy(isSaving = true, error = null) }
            val profile = _editState.value.profile
            val result = if (profile.id.isBlank()) repo.createProfile(profile) else repo.updateProfile(profile)
            result.fold(
                onSuccess = { _editState.update { s -> s.copy(isSaving = false, savedSuccessfully = true) } },
                onFailure = { _editState.update { s -> s.copy(isSaving = false, error = it.message) } }
            )
        }
    }

    fun deleteProfile(id: String, onDone: () -> Unit) {
        viewModelScope.launch {
            repo.deleteProfile(id)
            loadMyProfiles()
            onDone()
        }
    }

    fun linkQrTag(tagCode: String, pin: String, profileId: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            repo.linkQrTag(tagCode, pin, profileId).fold(
                onSuccess = {
                    loadProfileForEdit(profileId) // Refresh linked tags
                    onResult(true, null)
                },
                onFailure = { onResult(false, it.message) }
            )
        }
    }

    /** One-shot fetch of QR tags linked to a profile, used by the per-profile "Xem QR" dialog. */
    fun fetchTagsForProfile(profileId: String, onResult: (List<QrTag>) -> Unit) {
        viewModelScope.launch {
            val tags = repo.getLinkedTagsForProfile(profileId).getOrDefault(emptyList())
            onResult(tags)
        }
    }

    /**
     * Read an image picked by the user, upload it to the backend, and append
     * the returned URL to the in-memory profile's healthDocuments. The list
     * is persisted along with the rest of the profile on the next saveProfile().
     */
    fun uploadHealthDocument(
        context: android.content.Context,
        uri: android.net.Uri,
        onResult: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            _editState.update { it.copy(isSaving = true, error = null) }
            try {
                val cr = context.contentResolver
                val mime = cr.getType(uri) ?: "image/jpeg"
                val bytes = cr.openInputStream(uri)?.use { it.readBytes() }
                if (bytes == null) {
                    _editState.update { it.copy(isSaving = false) }
                    onResult(false, "Không đọc được ảnh đã chọn")
                    return@launch
                }
                repo.uploadImage(bytes, mime).fold(
                    onSuccess = { url ->
                        _editState.update { s ->
                            s.copy(
                                isSaving = false,
                                profile = s.profile.copy(
                                    healthDocuments = s.profile.healthDocuments + url
                                )
                            )
                        }
                        onResult(true, null)
                    },
                    onFailure = { err ->
                        _editState.update { it.copy(isSaving = false, error = err.message) }
                        onResult(false, err.message)
                    }
                )
            } catch (e: Exception) {
                _editState.update { it.copy(isSaving = false, error = e.message) }
                onResult(false, e.message)
            }
        }
    }

    /** Remove an uploaded document from the in-memory profile (saved on next saveProfile). */
    fun removeHealthDocument(url: String) {
        _editState.update { s ->
            s.copy(profile = s.profile.copy(
                healthDocuments = s.profile.healthDocuments.filter { it != url }
            ))
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// SHOP VIEW MODEL
// ═══════════════════════════════════════════════════════════════════════════════

data class ShopState(
    val products: List<Product> = emptyList(),
    val selectedProduct: Product? = null,
    val publicCoupons: List<Coupon> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class ShopViewModel @Inject constructor(
    private val repo: AppRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ShopState())
    val state: StateFlow<ShopState> = _state.asStateFlow()

    fun loadProducts() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            repo.getProducts().fold(
                onSuccess = { _state.update { s -> s.copy(products = it, isLoading = false) } },
                onFailure = { _state.update { s -> s.copy(error = it.message, isLoading = false) } }
            )
        }
        // Load public coupons for the rotating banner (best-effort, non-blocking).
        viewModelScope.launch {
            repo.getPublicCoupons().onSuccess { coupons ->
                _state.update { it.copy(publicCoupons = coupons) }
            }
        }
    }

    fun loadProductBySlug(slug: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            repo.getProductBySlug(slug).fold(
                onSuccess = { _state.update { s -> s.copy(selectedProduct = it, isLoading = false) } },
                onFailure = { _state.update { s -> s.copy(error = it.message, isLoading = false) } }
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// CART VIEW MODEL
// ═══════════════════════════════════════════════════════════════════════════════

data class CartState(
    val items: List<CartItem> = emptyList(),
    val selectedProfileId: String = "",
    val paymentMethod: String = "",
    val shippingAddress: ShippingAddress = ShippingAddress(),
    // ─ Coupon state ──────────────────────────────────────────────────────────
    val appliedCoupon: Coupon? = null,
    val discountAmount: Long = 0L,
    val couponError: String? = null,
    val isApplyingCoupon: Boolean = false,
    // ─ Order placement state ─────────────────────────────────────────────────
    val isPlacingOrder: Boolean = false,
    val orderSuccess: Boolean = false,
    val generatedTags: List<QrTag> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class CartViewModel @Inject constructor(
    private val repo: AppRepository
) : ViewModel() {

    private val _state = MutableStateFlow(CartState())
    val state: StateFlow<CartState> = _state.asStateFlow()

    val totalItems: Int get() = _state.value.items.sumOf { it.quantity }
    val subtotal: Long get() = _state.value.items.sumOf { it.product.price * it.quantity }
    /** Subtotal minus any applied coupon discount, floored at 0. Used by VietQR + UI. */
    val finalTotal: Long get() = (subtotal - _state.value.discountAmount).coerceAtLeast(0L)

    fun addItem(product: Product, quantity: Int, emergencyContact: String = "") {
        _state.update { s ->
            val existing = s.items.find { it.product.id == product.id }
            val newItems = if (existing != null) {
                s.items.map { if (it.product.id == product.id) it.copy(quantity = it.quantity + quantity) else it }
            } else {
                s.items + CartItem(product, quantity, emergencyContact)
            }
            // Any cart change invalidates an applied coupon — discounts depend on
            // the subtotal and the min-order rule, both of which may now fail.
            s.copy(items = newItems, appliedCoupon = null, discountAmount = 0L, couponError = null)
        }
    }

    fun removeItem(productId: String) {
        _state.update { it.copy(
            items = it.items.filter { i -> i.product.id != productId },
            appliedCoupon = null, discountAmount = 0L, couponError = null
        )}
    }

    fun updateQuantity(productId: String, qty: Int) {
        if (qty < 1) { removeItem(productId); return }
        _state.update { s -> s.copy(
            items = s.items.map { if (it.product.id == productId) it.copy(quantity = qty) else it },
            appliedCoupon = null, discountAmount = 0L, couponError = null
        )}
    }

    fun setProfile(profileId: String) = _state.update { it.copy(selectedProfileId = profileId) }

    fun setShippingAddress(address: ShippingAddress) = _state.update { it.copy(shippingAddress = address) }
    fun setPaymentMethod(method: String) = _state.update { it.copy(paymentMethod = method) }

    // ─── Coupon ──────────────────────────────────────────────────────────────

    /**
     * Validate a coupon code against the current subtotal. On success the
     * discount is applied to [finalTotal] (which propagates to the VietQR
     * generator and the order placement). On failure we surface the
     * server's error message in [CartState.couponError].
     */
    fun applyCoupon(code: String) {
        val trimmed = code.trim()
        if (trimmed.isBlank()) {
            _state.update { it.copy(couponError = "Vui lòng nhập mã giảm giá") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isApplyingCoupon = true, couponError = null) }
            repo.validateCoupon(trimmed, subtotal).fold(
                onSuccess = { v ->
                    if (v.valid && v.coupon != null) {
                        _state.update { it.copy(
                            appliedCoupon = v.coupon,
                            discountAmount = v.discountAmount,
                            couponError = null,
                            isApplyingCoupon = false
                        )}
                    } else {
                        _state.update { it.copy(
                            appliedCoupon = null,
                            discountAmount = 0L,
                            couponError = v.error ?: "Mã giảm giá không hợp lệ",
                            isApplyingCoupon = false
                        )}
                    }
                },
                onFailure = { err ->
                    _state.update { it.copy(
                        couponError = "Lỗi kết nối: ${err.message}",
                        isApplyingCoupon = false
                    )}
                }
            )
        }
    }

    fun clearCoupon() = _state.update {
        it.copy(appliedCoupon = null, discountAmount = 0L, couponError = null)
    }

    fun placeOrder(onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            _state.update { it.copy(isPlacingOrder = true, error = null) }
            val s = _state.value
            repo.placeOrder(
                items = s.items,
                profileId = s.selectedProfileId,
                paymentMethod = s.paymentMethod,
                couponCode = s.appliedCoupon?.code ?: "",
                discountAmount = s.discountAmount,
                shippingAddress = s.shippingAddress
            ).fold(
                onSuccess = { (_, tags) ->
                    _state.update { it.copy(
                        isPlacingOrder = false,
                        orderSuccess = true,
                        generatedTags = tags,
                        items = emptyList(),
                        appliedCoupon = null,
                        discountAmount = 0L
                    )}
                    onResult(true, null)
                },
                onFailure = {
                    _state.update { st -> st.copy(isPlacingOrder = false, error = it.message) }
                    onResult(false, it.message)
                }
            )
        }
    }

    fun clearCart() = _state.update { CartState() }

    // ── Floating-cart-bubble position ─────────────────────────────────────────
    // Persists where the user dragged the bubble so it doesn't snap back to
    // the default corner every time they navigate to a new screen. Activity-
    // scoped because CartViewModel itself is activity-scoped (see SharedViewModels).
    // Plain vars (not StateFlow) — the bubble owns the live state and reads these
    // only on first composition; no recomposition needed when they change.
    var bubbleX: Float = -1f
        private set
    var bubbleY: Float = -1f
        private set

    fun saveBubblePosition(x: Float, y: Float) {
        bubbleX = x
        bubbleY = y
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// ADMIN VIEW MODEL
// ═══════════════════════════════════════════════════════════════════════════════

@HiltViewModel
class AdminViewModel @Inject constructor(
    private val repo: AppRepository
) : ViewModel() {

    private val _metrics = MutableStateFlow<AdminMetrics?>(null)
    val metrics: StateFlow<AdminMetrics?> = _metrics.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // Which tab is open in the dashboard. Persisted across recompositions.
    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()
    fun selectTab(index: Int) { _selectedTab.value = index }

    fun loadMetrics() {
        viewModelScope.launch {
            _isLoading.value = true
            repo.getAdminMetrics().fold(
                onSuccess = { _metrics.value = it; _isLoading.value = false },
                onFailure = { _error.value = it.message; _isLoading.value = false }
            )
        }
    }

    // ─── Order actions ───────────────────────────────────────────────────────

    fun updateOrderStatus(order: Order, newStatus: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            repo.updateOrderStatus(order, newStatus).fold(
                onSuccess = {
                    loadMetrics() // refresh all metrics + the orders list in-place
                    onResult(true, null)
                },
                onFailure = { onResult(false, it.message) }
            )
        }
    }

    // ─── Coupon actions ──────────────────────────────────────────────────────

    fun createCoupon(coupon: Coupon, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            repo.createCoupon(coupon).fold(
                onSuccess = { loadMetrics(); onResult(true, null) },
                onFailure = { onResult(false, it.message) }
            )
        }
    }

    fun updateCoupon(coupon: Coupon, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            repo.updateCoupon(coupon).fold(
                onSuccess = { loadMetrics(); onResult(true, null) },
                onFailure = { onResult(false, it.message) }
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// PUBLIC PROFILE VIEW MODEL
// ═══════════════════════════════════════════════════════════════════════════════

@HiltViewModel
class PublicProfileViewModel @Inject constructor(
    private val repo: AppRepository
) : ViewModel() {

    private val _profile = MutableStateFlow<Profile?>(null)
    val profile: StateFlow<Profile?> = _profile.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun loadByTagCode(tagCode: String) {
        viewModelScope.launch {
            _isLoading.value = true
            repo.getPublicProfileByTagCode(tagCode).fold(
                onSuccess = { (profile, _) -> _profile.value = profile; _isLoading.value = false },
                onFailure = { _error.value = it.message; _isLoading.value = false }
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// ORDER HISTORY VIEW MODEL
// ═══════════════════════════════════════════════════════════════════════════════

data class OrderHistoryState(
    val orders: List<Order> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class OrderHistoryViewModel @Inject constructor(
    private val repo: AppRepository
) : ViewModel() {

    private val _state = MutableStateFlow(OrderHistoryState())
    val state: StateFlow<OrderHistoryState> = _state.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            repo.getMyOrders().fold(
                onSuccess = { orders ->
                    // Newest first
                    _state.update { it.copy(orders = orders.sortedByDescending { o -> o.createdAt }, isLoading = false) }
                },
                onFailure = { _state.update { s -> s.copy(error = it.message, isLoading = false) } }
            )
        }
    }
}
