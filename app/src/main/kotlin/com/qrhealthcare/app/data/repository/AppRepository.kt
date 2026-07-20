package com.qrhealthcare.app.data.repository

import com.qrhealthcare.app.data.api.ApiClient
import com.qrhealthcare.app.data.api.ApiService
import com.qrhealthcare.app.data.api.LoginRequest
import com.qrhealthcare.app.data.api.RegisterRequest
import com.qrhealthcare.app.data.api.ResetPasswordRequest
import com.qrhealthcare.app.data.api.ChangePasswordRequest
import com.qrhealthcare.app.data.local.SessionManager
import com.qrhealthcare.app.data.model.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AppRepository — single source of truth for all data operations.
 *
 * Connects to the Express + MongoDB backend. The previous MockAPI prototype
 * did client-side password compare; we now hit POST /auth/login and POST
 * /auth/register which return a JWT. The JWT is persisted in SessionManager
 * and attached to every authenticated request by ApiClient's interceptor.
 */
@Singleton
class AppRepository @Inject constructor(
    private val api: ApiService,
    private val session: SessionManager
) {

    init {
        // Keep ApiClient.authToken hydrated from persisted storage so the
        // interceptor has a valid token across app restarts (i.e. when the
        // user was previously logged in and the process was killed).
        @OptIn(kotlinx.coroutines.DelicateCoroutinesApi::class)
        GlobalScope.launch {
            session.token.collect { stored -> ApiClient.authToken = stored }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // AUTH
    // ═══════════════════════════════════════════════════════════════════════

    suspend fun login(email: String, password: String): AuthResult {
        return try {
            val response = api.login(LoginRequest(email = email, password = password))
            if (!response.isSuccessful) {
                val msg = response.errorBody()?.string()?.let { extractErrorMessage(it) }
                return AuthResult(error = msg ?: "Đăng nhập thất bại (${response.code()})")
            }
            val body = response.body() ?: return AuthResult(error = "Phản hồi trống từ máy chủ")
            val user = body.user ?: return AuthResult(error = body.error ?: "Đăng nhập thất bại")
            val token = body.token ?: return AuthResult(error = "Máy chủ không trả về token")

            session.saveSession(
                userId = user.id,
                email = user.email,
                fullName = user.fullName,
                address = user.address,
                role = user.role,
                token = token,
                phone = user.phone,
                city = user.city
            )
            ApiClient.authToken = token
            AuthResult(user = user, token = token)
        } catch (e: Exception) {
            AuthResult(error = "Lỗi kết nối: ${e.message}")
        }
    }

    suspend fun register(email: String, password: String, fullName: String): AuthResult {
        return try {
            val response = api.register(
                RegisterRequest(
                    email = email,
                    password = password,
                    fullName = fullName
                )
            )
            if (!response.isSuccessful) {
                val msg = response.errorBody()?.string()?.let { extractErrorMessage(it) }
                return AuthResult(error = msg ?: "Đăng ký thất bại (${response.code()})")
            }
            val body = response.body() ?: return AuthResult(error = "Phản hồi trống từ máy chủ")
            val user = body.user ?: return AuthResult(error = body.error ?: "Đăng ký thất bại")
            val token = body.token ?: return AuthResult(error = "Máy chủ không trả về token")

            session.saveSession(
                userId = user.id,
                email = user.email,
                fullName = user.fullName,
                address = user.address,
                role = user.role,
                token = token,
                phone = user.phone,
                city = user.city
            )
            ApiClient.authToken = token
            AuthResult(user = user, token = token)
        } catch (e: Exception) {
            AuthResult(error = "Lỗi kết nối: ${e.message}")
        }
    }

    suspend fun logout() {
        session.clearSession()
        ApiClient.authToken = null
    }

    /** Reset password without email (verify by email + full name). */
    suspend fun resetPassword(email: String, fullName: String, newPassword: String): Result<String> = try {
        val response = api.resetPassword(ResetPasswordRequest(email.trim(), fullName.trim(), newPassword))
        if (!response.isSuccessful) {
            val msg = response.errorBody()?.string()?.let { extractErrorMessage(it) }
                ?: "Lỗi ${response.code()}"
            Result.failure(Exception(msg))
        } else {
            Result.success(response.body()?.message ?: "Đặt lại mật khẩu thành công")
        }
    } catch (e: Exception) {
        Result.failure(Exception("Lỗi kết nối: ${e.message}"))
    }

    /** Change password for the logged-in user (verify current password). */
    suspend fun changePassword(currentPassword: String, newPassword: String): Result<String> = try {
        val email = session.email.first() ?: return Result.failure(Exception("Chưa đăng nhập"))
        val response = api.changePassword(ChangePasswordRequest(email, currentPassword, newPassword))
        if (!response.isSuccessful) {
            val msg = response.errorBody()?.string()?.let { extractErrorMessage(it) }
                ?: "Lỗi ${response.code()}"
            Result.failure(Exception(msg))
        } else {
            Result.success(response.body()?.message ?: "Đổi mật khẩu thành công")
        }
    } catch (e: Exception) {
        Result.failure(Exception("Lỗi kết nối: ${e.message}"))
    }

    /** Update the logged-in user's shipping address on the server, then mirror it in the local session. */
    suspend fun updateUserAddress(address: String, phone: String = "", city: String = ""): Result<User> = try {
        val userId = session.userId.first() ?: return Result.failure(Exception("Chưa đăng nhập"))
        val response = api.updateUserAddress(
            userId,
            com.qrhealthcare.app.data.api.UpdateAddressRequest(address, phone, city)
        )
        if (!response.isSuccessful) {
            val msg = response.errorBody()?.string()?.let { extractErrorMessage(it) }
                ?: "Lỗi ${response.code()}"
            Result.failure(Exception(msg))
        } else {
            response.body()?.let {
                session.saveAddress(it.address, it.phone, it.city)
                Result.success(it)
            } ?: Result.failure(Exception("Phản hồi trống"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    /**
     * Upload a health-document image to the server. Returns the *relative*
     * URL ("/uploads/<filename>") that should be appended to the profile's
     * `healthDocuments` list and persisted with the next profile save.
     */
    suspend fun uploadImage(bytes: ByteArray, mimetype: String): Result<String> = try {
        val mediaType = mimetype.toMediaTypeOrNull() ?: "image/jpeg".toMediaTypeOrNull()
        val extension = mimetype.substringAfter('/', "jpg").take(8)

        val body = bytes.toRequestBody(mediaType)
        val part = okhttp3.MultipartBody.Part.createFormData("file", "upload.$extension", body)
        val response = api.uploadFile(part)
        if (!response.isSuccessful) {
            val msg = response.errorBody()?.string()?.let { extractErrorMessage(it) }
                ?: "Lỗi ${response.code()}"
            Result.failure(Exception(msg))
        } else {
            response.body()?.url?.let { Result.success(it) }
                ?: Result.failure(Exception("Phản hồi trống"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    /**
     * Validate a coupon code against the current cart subtotal. Always returns
     * a Result.success — the [CouponValidation.valid] flag is what tells you
     * whether the code is actually usable. Network errors return Result.failure.
     */
    suspend fun validateCoupon(code: String, subtotal: Long): Result<CouponValidation> = try {
        val response = api.validateCoupon(
            com.qrhealthcare.app.data.api.CouponValidateRequest(code.trim().uppercase(), subtotal)
        )
        val body = response.body()
        when {
            response.isSuccessful && body != null -> Result.success(body)
            else -> Result.failure(Exception("Không thể kiểm tra mã giảm giá (${response.code()})"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun getCurrentUserId() = session.userId.first()
    suspend fun getCurrentUserRole() = session.role.first()
    val isLoggedIn = session.isLoggedIn

    /** Public, advertisable coupons for the store banner (excludes secret/expired/used-up). */
    suspend fun getPublicCoupons(): Result<List<Coupon>> = try {
        val response = api.getPublicCoupons()
        val body = response.body()
        if (response.isSuccessful && body != null) Result.success(body)
        else Result.failure(Exception("Lỗi ${response.code()}"))
    } catch (e: Exception) {
        Result.failure(e)
    }

    /** Pull the JSON `error` field from a non-2xx response body, if present. */
    private fun extractErrorMessage(raw: String): String? {
        // Cheap parse — avoids pulling in a JSON dep just for one error field.
        val match = Regex("\"error\"\\s*:\\s*\"([^\"]+)\"").find(raw)
        return match?.groupValues?.get(1)
    }

    private fun isSubscriptionRequiredError(raw: String): Boolean =
        Regex("\"needsSubscription\"\\s*:\\s*true").containsMatchIn(raw)

    // ═══════════════════════════════════════════════════════════════════════
    // PROFILES
    // ═══════════════════════════════════════════════════════════════════════

    suspend fun getMyProfiles(): Result<List<Profile>> {
        return try {
            val userId = session.userId.first() ?: return Result.failure(Exception("Chưa đăng nhập"))
            val response = api.getProfilesByUser(userId)
            if (response.isSuccessful) {
                Result.success(response.body() ?: emptyList())
            } else {
                Result.failure(Exception("Lỗi tải hồ sơ"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getProfileById(id: String): Result<Profile> {
        return try {
            val response = api.getProfileById(id)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Không tìm thấy hồ sơ"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createProfile(profile: Profile): Result<Profile> {
        return try {
            val userId = session.userId.first() ?: return Result.failure(Exception("Chưa đăng nhập"))

            // Slot limit (base 5 + any extra purchased via the subscription's
            // flexible plan) is enforced authoritatively server-side, since it
            // depends on subscription status which can change server-side
            // (expiry) independent of anything the client knows.
            val response = api.createProfile(profile.copy(userId = userId, createdAt = System.currentTimeMillis()))
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val raw = response.errorBody()?.string() ?: ""
                if (response.code() == 403 && isSubscriptionRequiredError(raw)) {
                    Result.failure(SubscriptionRequiredException(extractErrorMessage(raw) ?: "Cần đăng ký gói duy trì lưu trữ hồ sơ"))
                } else {
                    Result.failure(Exception(extractErrorMessage(raw) ?: "Không thể tạo hồ sơ"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateProfile(profile: Profile): Result<Profile> {
        return try {
            val response = api.updateProfile(profile.id, profile)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Không thể cập nhật hồ sơ"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteProfile(id: String): Result<Unit> {
        return try {
            val response = api.deleteProfile(id)
            if (response.isSuccessful) Result.success(Unit)
            else Result.failure(Exception("Không thể xóa hồ sơ"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // FAMILY SCAN NOTIFICATIONS (subscription perk — requires Firebase setup,
    // see app/FCM_SETUP.md. All functions fail gracefully with a clear error
    // message if Firebase hasn't been configured yet, instead of crashing.)
    // ═══════════════════════════════════════════════════════════════════════

    /** Gets this device's FCM token. Fails with a friendly message if Firebase isn't configured (see FCM_SETUP.md). */
    suspend fun getFcmToken(): Result<String> {
        return try {
            val token = com.google.firebase.messaging.FirebaseMessaging.getInstance().token.await()
            Result.success(token)
        } catch (e: IllegalStateException) {
            // "Default FirebaseApp is not initialized" — happens when google-services.json hasn't been added yet.
            Result.failure(Exception("Tính năng thông báo chưa được cấu hình (cần thiết lập Firebase). Liên hệ nhà phát triển."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Registers this device to get notified whenever [profileId]'s QR is scanned. Returns the family access token used to build the full-view link. */
    suspend fun registerFamilyDevice(profileId: String, fcmToken: String): Result<String> {
        return try {
            val response = api.registerFamilyDevice(profileId, mapOf("fcmToken" to fcmToken))
            val token = response.body()?.get("familyAccessToken")
            if (response.isSuccessful && token != null) Result.success(token)
            else Result.failure(Exception("Không thể đăng ký thiết bị"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun unregisterFamilyDevice(profileId: String, fcmToken: String): Result<Unit> {
        return try {
            api.unregisterFamilyDevice(profileId, mapOf("fcmToken" to fcmToken))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Sends an immediate diagnostic test push (bypasses the scan trigger and
     * subscription check) so setup problems can be isolated: returns a
     * human-readable message telling you which half is misconfigured, if any.
     */
    suspend fun sendFamilyTestNotify(profileId: String): Result<String> {
        return try {
            val response = api.sendFamilyTestNotify(profileId)
            val body = response.body()
            if (!response.isSuccessful || body == null) {
                return Result.failure(Exception("Không thể gửi thông báo thử"))
            }
            val tokenCount = (body["tokenCount"] as? Double)?.toInt() ?: 0
            val disabled = body["disabled"] as? Boolean ?: false
            val sent = (body["sent"] as? Double)?.toInt() ?: 0
            val msg = when {
                tokenCount == 0 -> "Chưa có thiết bị nào đăng ký cho hồ sơ này — hãy đăng ký trước."
                disabled -> "Backend chưa cấu hình Firebase (thiếu FIREBASE_SERVICE_ACCOUNT_JSON) — xem app/FCM_SETUP.md."
                sent > 0 -> "Đã gửi thành công tới $sent thiết bị! Kiểm tra điện thoại đã đăng ký."
                else -> "Gửi thất bại — token có thể đã hết hạn. Hãy đăng ký lại thiết bị."
            }
            Result.success(msg)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // SUBSCRIPTION (gói duy trì lưu trữ hồ sơ)
    // ═══════════════════════════════════════════════════════════════════════

    /** Null = user has no profiles yet, so no trial has started. */
    suspend fun getMySubscription(): Result<Subscription?> {
        return try {
            val userId = session.userId.first() ?: return Result.failure(Exception("Chưa đăng nhập"))
            val response = api.getSubscription(userId)
            if (response.isSuccessful) Result.success(response.body())
            else Result.failure(Exception("Không thể tải thông tin đăng ký"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Pay for/renew the maintenance plan via VietQR. [plan] is "monthly",
     * "flexible", or "yearly"; [extraProfiles] only applies meaningfully to
     * "flexible" (ignored — always 0 — for the other two on the backend's
     * own validation, but we pass through what the UI collected either way).
     */
    suspend fun renewSubscription(plan: String, extraProfiles: Int, paymentRef: String): Result<Subscription> {
        return try {
            val userId = session.userId.first() ?: return Result.failure(Exception("Chưa đăng nhập"))
            val body = mapOf(
                "userId" to userId,
                "plan" to plan,
                "extraProfiles" to extraProfiles,
                "paymentRef" to paymentRef
            )
            val response = api.renewSubscription(body)
            if (response.isSuccessful && response.body() != null) Result.success(response.body()!!)
            else {
                val raw = response.errorBody()?.string() ?: ""
                Result.failure(Exception(extractErrorMessage(raw) ?: "Không thể xử lý đăng ký"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun cancelSubscription(): Result<Subscription> {
        return try {
            val userId = session.userId.first() ?: return Result.failure(Exception("Chưa đăng nhập"))
            val response = api.cancelSubscription(mapOf("userId" to userId))
            if (response.isSuccessful && response.body() != null) Result.success(response.body()!!)
            else Result.failure(Exception("Không thể hủy đăng ký"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getSubscriptionAdminStats(): Result<SubscriptionAdminStats> {
        return try {
            val response = api.getSubscriptionAdminStats()
            if (response.isSuccessful && response.body() != null) Result.success(response.body()!!)
            else Result.failure(Exception("Không thể tải thống kê đăng ký"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // PUBLIC PROFILE (for QR scan)
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Get a public profile via QR tag code.
     * Looks up the QR tag → gets profileId → fetches profile → increments scan count.
     */
    suspend fun getPublicProfileByTagCode(tagCode: String): Result<Pair<Profile, QrTag>> {
        return try {
            val tags = api.getQrTagByCode(tagCode).body() ?: emptyList()
            val tag = tags.firstOrNull() ?: return Result.failure(Exception("Mã QR không hợp lệ"))
            val profileId = tag.profileId ?: return Result.failure(Exception("QR chưa được liên kết với hồ sơ"))

            val profile = api.getProfileById(profileId).body()
                ?: return Result.failure(Exception("Hồ sơ không tồn tại"))

            // Increment scan count (fire and forget)
            try { api.updateQrTag(tag.id, tag.copy(scanCount = tag.scanCount + 1)) } catch (_: Exception) {}

            Result.success(Pair(profile, tag))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // QR TAG LINKING
    // ═══════════════════════════════════════════════════════════════════════

    suspend fun linkQrTag(tagCode: String, pin: String, profileId: String): Result<QrTag> {
        return try {
            val tags = api.getQrTagByCode(tagCode).body() ?: emptyList()
            val tag = tags.firstOrNull() ?: return Result.failure(Exception("Mã tag không tồn tại"))

            if (tag.pin != pin) return Result.failure(Exception("PIN không đúng"))
            if (tag.profileId != null) return Result.failure(Exception("Tag đã được liên kết với hồ sơ khác"))

            val updated = tag.copy(profileId = profileId)
            val response = api.updateQrTag(tag.id, updated)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Không thể liên kết tag"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getLinkedTagsForProfile(profileId: String): Result<List<QrTag>> {
        return try {
            val response = api.getQrTagsByProfile(profileId)
            Result.success(response.body() ?: emptyList())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Admin use — the QR tags generated for an order, so admin can view/export the actual images for the physical-production vendor. */
    suspend fun getQrTagsForOrder(orderId: String): Result<List<QrTag>> {
        return try {
            val response = api.getQrTagsByOrder(orderId)
            Result.success(response.body() ?: emptyList())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // PRODUCTS
    // ═══════════════════════════════════════════════════════════════════════

    suspend fun getProducts(): Result<List<Product>> {
        return try {
            val response = api.getProducts()
            when {
                response.isSuccessful -> {
                    val body = response.body() ?: emptyList()
                    println("[DEBUG] getProducts() SUCCESS: ${body.size} products retrieved")
                    Result.success(body)
                }
                else -> {
                    val errorMsg = response.errorBody()?.string() ?: "Unknown error (${response.code()})"
                    println("[DEBUG] getProducts() ERROR: $errorMsg")
                    Result.failure(Exception("Failed to fetch products: $errorMsg"))
                }
            }
        } catch (e: Exception) {
            println("[DEBUG] getProducts() EXCEPTION: ${e.message} | ${e.javaClass.simpleName}")
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun getProductBySlug(slug: String): Result<Product?> {
        return try {
            val products = api.getProductBySlug(slug).body() ?: emptyList()
            Result.success(products.firstOrNull())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // ORDERS & CHECKOUT
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Creates an order and generates unique QR tags for each item quantity.
     * Returns the created order with the generated QR tags' tagCodes and PINs.
     */
    // ═══════════════════════════════════════════════════════════════════════
    // CHECKOUT FUNNEL TRACKING (drop-out / abandonment)
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Call once, the moment the user lands on the shipping-info screen — this
     * is "checkout started". Returns the session id so the caller can report
     * further progress with [updateCheckoutSession]. Best-effort: a failure
     * here should never block checkout itself.
     */
    suspend fun startCheckoutSession(cartValue: Long, itemCount: Int): Result<String> = try {
        val userId = session.userId.first() ?: return Result.failure(Exception("Chưa đăng nhập"))
        val body = mapOf("userId" to userId, "cartValue" to cartValue, "itemCount" to itemCount)
        val response = api.startCheckoutSession(body)
        response.body()?.id?.let { Result.success(it) } ?: Result.failure(Exception("Phản hồi trống"))
    } catch (e: Exception) {
        Result.failure(e)
    }

    /**
     * Report that the checkout session reached [step] (2=profile selected,
     * 3=payment method chosen), or pass [completed]=true with [orderId] once
     * the order is actually placed. Best-effort — swallow failures so this
     * never interrupts the checkout flow.
     */
    suspend fun updateCheckoutSession(
        sessionId: String,
        step: Int? = null,
        paymentMethod: String? = null,
        completed: Boolean = false,
        orderId: String? = null
    ) {
        if (sessionId.isBlank()) return
        val body = buildMap<String, Any> {
            step?.let { put("step", it) }
            paymentMethod?.let { put("paymentMethod", it) }
            if (completed) put("completed", true)
            orderId?.let { put("orderId", it) }
        }
        if (body.isEmpty()) return
        runCatching { api.updateCheckoutSession(sessionId, body) }
    }

    suspend fun placeOrder(
        items: List<CartItem>,
        profileId: String,
        paymentMethod: String,
        couponCode: String = "",
        discountAmount: Long = 0L,
        shippingAddress: ShippingAddress = ShippingAddress(),
        paymentRef: String = ""
    ): Result<Pair<Order, List<QrTag>>> {
        return try {
            val userId = session.userId.first() ?: return Result.failure(Exception("Chưa đăng nhập"))

            // Generate QR tags for each item × quantity — but only as many as
            // the product actually calls for (product.qrTagsPerUnit). A combo
            // package can need 2 tags per unit; an accessory/refill item (e.g.
            // "Sticker Che Phủ Thêm") needs 0. Category alone can't tell us
            // this, since a combo and a plain refill can share a category.
            val generatedTags = mutableListOf<QrTag>()
            for (item in items) {
                val tagsForThisItem = item.product.qrTagsPerUnit * item.quantity
                repeat(tagsForThisItem) {
                    val tag = QrTag(
                        tagCode = generateTagCode(),
                        pin = generatePin(),
                        profileId = null,  // Unlinked until user links manually
                        productType = item.product.category,
                        createdAt = System.currentTimeMillis()
                    )
                    // We'll attach orderId after order is created
                    generatedTags.add(tag)
                }
            }

            // Build order items
            val orderItems = items.map { cartItem ->
                OrderItem(
                    productId = cartItem.product.id,
                    productSlug = cartItem.product.slug,
                    productName = cartItem.product.name,
                    imageUrl = cartItem.product.imageUrl,
                    price = cartItem.product.price,
                    quantity = cartItem.quantity,
                    emergencyContact = cartItem.emergencyContact
                )
            }
            val subtotal = items.sumOf { it.product.price * it.quantity }
            val finalTotal = (subtotal - discountAmount).coerceAtLeast(0L)

            // Create the order
            val order = Order(
                userId = userId,
                profileId = profileId,
                items = orderItems,
                totalAmount = finalTotal,
                discountAmount = discountAmount,
                couponCode = couponCode,
                paymentMethod = paymentMethod,
                status = "pending",
                paymentRef = paymentRef,
                shippingAddress = shippingAddress,
                createdAt = System.currentTimeMillis()
            )
            val createdOrder = api.createOrder(order).body()
                ?: return Result.failure(Exception("Không thể tạo đơn hàng"))

            // Create QR tags linked to order
            val createdTags = mutableListOf<QrTag>()
            for (tag in generatedTags) {
                val response = api.createQrTag(tag.copy(orderId = createdOrder.id))
                response.body()?.let { createdTags.add(it) }
            }

            Result.success(Pair(createdOrder, createdTags))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getMyOrders(): Result<List<Order>> {
        return try {
            val userId = session.userId.first() ?: return Result.failure(Exception("Chưa đăng nhập"))
            val response = api.getOrdersByUser(userId)
            Result.success(response.body() ?: emptyList())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // ADMIN
    // ═══════════════════════════════════════════════════════════════════════

    suspend fun getAdminMetrics(): Result<AdminMetrics> {
        return try {
            val users    = api.getAllUsers().body()    ?: emptyList()
            val profiles = api.getAllProfiles().body() ?: emptyList()
            val orders   = api.getAllOrders().body()   ?: emptyList()
            val tags     = api.getAllQrTags().body()   ?: emptyList()
            val coupons  = runCatching { api.getAllCoupons().body() ?: emptyList() }.getOrDefault(emptyList())
            val checkoutSessions = runCatching { api.getAllCheckoutSessions().body() ?: emptyList() }.getOrDefault(emptyList())
            val subscriptionStats = runCatching { api.getSubscriptionAdminStats().body() ?: SubscriptionAdminStats() }.getOrDefault(SubscriptionAdminStats())

            // ─ Lifetime KPIs ─────────────────────────────────────────────────
            // Pending orders haven't been confirmed/paid yet — don't count them
            // as recognised revenue.
            val recognisedOrders = orders.filter { it.status != "pending" && it.status != "cancelled" }
            val totalRevenue = recognisedOrders.sumOf { it.totalAmount }
            val totalScans = tags.sumOf { it.scanCount }
            val ordersByStatus = orders.groupBy { it.status }.mapValues { it.value.size }

            val productSales = mutableMapOf<String, Int>()
            for (order in orders) for (item in order.items) {
                productSales[item.productName] = (productSales[item.productName] ?: 0) + item.quantity
            }

            // ─ Time-period KPIs (in user's local time) ───────────────────────
            val now = java.util.Calendar.getInstance()
            val startOfToday = (now.clone() as java.util.Calendar).apply {
                set(java.util.Calendar.HOUR_OF_DAY, 0); set(java.util.Calendar.MINUTE, 0)
                set(java.util.Calendar.SECOND, 0); set(java.util.Calendar.MILLISECOND, 0)
            }.timeInMillis
            val startOfWeek = startOfToday - 6L * 24 * 60 * 60 * 1000   // rolling 7-day window
            val startOfMonth = startOfToday - 29L * 24 * 60 * 60 * 1000 // rolling 30-day window

            val revenueToday     = recognisedOrders.filter { it.createdAt >= startOfToday }.sumOf { it.totalAmount }
            val revenueThisWeek  = recognisedOrders.filter { it.createdAt >= startOfWeek }.sumOf { it.totalAmount }
            val revenueThisMonth = recognisedOrders.filter { it.createdAt >= startOfMonth }.sumOf { it.totalAmount }
            val ordersToday      = orders.count { it.createdAt >= startOfToday }
            val newUsersToday    = users.count { it.createdAt >= startOfToday }

            // ─ 14-day daily trend (oldest first) ─────────────────────────────
            val msPerDay = 24L * 60 * 60 * 1000
            val dailyRevenue = (13 downTo 0).map { daysBack ->
                val dayStart = startOfToday - daysBack * msPerDay
                val dayEnd = dayStart + msPerDay
                recognisedOrders.filter { it.createdAt in dayStart until dayEnd }.sumOf { it.totalAmount }
            }
            val dailyOrderCount = (13 downTo 0).map { daysBack ->
                val dayStart = startOfToday - daysBack * msPerDay
                val dayEnd = dayStart + msPerDay
                orders.count { it.createdAt in dayStart until dayEnd }
            }

            val recentOrders = orders.sortedByDescending { it.createdAt }.take(10)

            // ─ Checkout funnel: drop-out & abandonment ────────────────────────
            // A session that never reached completed=true — regardless of which
            // step it stalled at — counts as abandoned. "Drop-out by step" buckets
            // each abandoned session under the furthest step it reached, so you
            // can see exactly where in the funnel people are quitting.
            val sessionsStarted = checkoutSessions.size
            val sessionsCompleted = checkoutSessions.count { it.completed }
            val abandonedSessions = checkoutSessions.filter { !it.completed }
            val abandonmentRate = if (sessionsStarted > 0) abandonedSessions.size.toDouble() / sessionsStarted else 0.0
            val conversionRate = if (sessionsStarted > 0) sessionsCompleted.toDouble() / sessionsStarted else 0.0
            val abandonedCartValue = abandonedSessions.sumOf { it.cartValue }
            val dropOutByStep = abandonedSessions.groupBy { it.step }.mapValues { it.value.size }

            Result.success(
                AdminMetrics(
                    totalUsers = users.size,
                    totalProfiles = profiles.size,
                    totalOrders = orders.size,
                    totalRevenue = totalRevenue,
                    totalQrScans = totalScans,
                    recentOrders = recentOrders,
                    ordersByStatus = ordersByStatus,
                    productSalesCounts = productSales,
                    revenueToday = revenueToday,
                    revenueThisWeek = revenueThisWeek,
                    revenueThisMonth = revenueThisMonth,
                    ordersToday = ordersToday,
                    newUsersToday = newUsersToday,
                    dailyRevenue = dailyRevenue,
                    dailyOrderCount = dailyOrderCount,
                    allUsers = users.sortedByDescending { it.createdAt },
                    allOrders = orders.sortedByDescending { it.createdAt },
                    allCoupons = coupons,
                    checkoutSessionsStarted = sessionsStarted,
                    checkoutSessionsCompleted = sessionsCompleted,
                    checkoutAbandonmentRate = abandonmentRate,
                    checkoutConversionRate = conversionRate,
                    abandonedCartValue = abandonedCartValue,
                    dropOutByStep = dropOutByStep,
                    allCheckoutSessions = checkoutSessions.sortedByDescending { it.startedAt },
                    subscriptionStats = subscriptionStats
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ─── Admin actions ────────────────────────────────────────────────────────

    /** Update an order's status. Returns the updated order. */
    suspend fun updateOrderStatus(order: Order, newStatus: String): Result<Order> = try {
        val response = api.updateOrder(order.id, order.copy(status = newStatus))
        response.body()?.let { Result.success(it) } ?: Result.failure(Exception("Phản hồi trống"))
    } catch (e: Exception) {
        Result.failure(e)
    }

    /** Create a new coupon. */
    suspend fun createCoupon(coupon: Coupon): Result<Coupon> = try {
        val response = api.createCoupon(coupon.copy(id = "", code = coupon.code.uppercase().trim()))
        if (!response.isSuccessful) {
            val msg = response.errorBody()?.string()?.let { extractErrorMessage(it) } ?: "Lỗi ${response.code()}"
            Result.failure(Exception(msg))
        } else {
            response.body()?.let { Result.success(it) } ?: Result.failure(Exception("Phản hồi trống"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    /** Toggle active / change discount / etc on an existing coupon. */
    suspend fun updateCoupon(coupon: Coupon): Result<Coupon> = try {
        val response = api.updateCoupon(coupon.id, coupon)
        response.body()?.let { Result.success(it) } ?: Result.failure(Exception("Phản hồi trống"))
    } catch (e: Exception) {
        Result.failure(e)
    }

    // ═══════════════════════════════════════════════════════════════════════
    // HELPERS
    // ═══════════════════════════════════════════════════════════════════════

    /** Generates a unique 8-char QR code like "QRH-A1B2" */
    private fun generateTagCode(): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789" // No ambiguous chars
        val suffix = (1..4).map { chars.random() }.joinToString("")
        return "QRH-$suffix"
    }

    /** Generates a 4-digit numeric PIN */
    private fun generatePin(): String = (1000..9999).random().toString()
}
