package com.qrhealthcare.app.data.api

import com.qrhealthcare.app.data.model.*
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.*

// ─── Auth request/response payloads (Express + JWT) ──────────────────────────

data class LoginRequest(val email: String, val password: String)
data class RegisterRequest(
    val email: String,
    val password: String,
    val fullName: String,
    val role: String = "user"
)
data class AuthResponse(val user: User? = null, val token: String? = null, val error: String? = null)

// ─── Coupon payloads ─────────────────────────────────────────────────────────

data class CouponValidateRequest(val code: String, val subtotal: Long)
data class UpdateAddressRequest(val address: String)

data class UploadResponse(
    val url: String = "",       // relative path like "/uploads/abc.jpg"
    val filename: String = "",
    val size: Long = 0L,
    val mimetype: String = ""
)

/**
 * Retrofit interface mapping to the Express + MongoDB backend.
 *
 * The same endpoints worked with MockAPI during prototyping. The only
 * additions for production are POST /auth/login and POST /auth/register
 * which return a JWT — everything else is unchanged.
 */
interface ApiService {

    // ─── Auth ─────────────────────────────────────────────────────────────────

    @POST("auth/login")
    suspend fun login(@Body body: LoginRequest): Response<AuthResponse>

    @POST("auth/register")
    suspend fun register(@Body body: RegisterRequest): Response<AuthResponse>

    // ─── Users ────────────────────────────────────────────────────────────────

    /** Get all users — for admin use only */
    @GET("users")
    suspend fun getAllUsers(): Response<List<User>>

    /** Find user by email (used for login simulation in MockAPI) */
    @GET("users")
    suspend fun getUsersByEmail(@Query("email") email: String): Response<List<User>>

    /** Register new user */
    @POST("users")
    suspend fun createUser(@Body user: User): Response<User>

    /** Update user (for account settings) */
    @PUT("users/{id}")
    suspend fun updateUser(@Path("id") id: String, @Body user: User): Response<User>

    /** Partial update — only modifies the `address` field. */
    @PUT("users/{id}")
    suspend fun updateUserAddress(@Path("id") id: String, @Body body: UpdateAddressRequest): Response<User>

    // ─── Coupons ──────────────────────────────────────────────────────────────

    @POST("coupons/validate")
    suspend fun validateCoupon(@Body body: CouponValidateRequest): Response<CouponValidation>

    @GET("coupons")
    suspend fun getAllCoupons(): Response<List<Coupon>>

    @POST("coupons")
    suspend fun createCoupon(@Body coupon: Coupon): Response<Coupon>

    @PUT("coupons/{id}")
    suspend fun updateCoupon(@Path("id") id: String, @Body coupon: Coupon): Response<Coupon>

    // ─── File uploads (health documents) ─────────────────────────────────────

    @Multipart
    @POST("uploads")
    suspend fun uploadFile(@Part file: MultipartBody.Part): Response<UploadResponse>

    // ─── Profiles ─────────────────────────────────────────────────────────────

    /** Get all profiles — for admin use only */
    @GET("profiles")
    suspend fun getAllProfiles(): Response<List<Profile>>

    /** Get profiles belonging to a specific user */
    @GET("profiles")
    suspend fun getProfilesByUser(@Query("userId") userId: String): Response<List<Profile>>

    /** Get a single profile by its ID */
    @GET("profiles/{id}")
    suspend fun getProfileById(@Path("id") id: String): Response<Profile>

    /** Create a new profile */
    @POST("profiles")
    suspend fun createProfile(@Body profile: Profile): Response<Profile>

    /** Update an existing profile */
    @PUT("profiles/{id}")
    suspend fun updateProfile(@Path("id") id: String, @Body profile: Profile): Response<Profile>

    /** Delete a profile */
    @DELETE("profiles/{id}")
    suspend fun deleteProfile(@Path("id") id: String): Response<Profile>

    // ─── Products ─────────────────────────────────────────────────────────────

    /** Get all products for the shop */
    @GET("products")
    suspend fun getProducts(): Response<List<Product>>

    /** Get single product by MockAPI ID */
    @GET("products/{id}")
    suspend fun getProductById(@Path("id") id: String): Response<Product>

    /** Get product by slug (e.g. "combo-sticker-y-te") */
    @GET("products")
    suspend fun getProductBySlug(@Query("slug") slug: String): Response<List<Product>>

    // ─── QR Tags ──────────────────────────────────────────────────────────────

    /** Get all QR tags — admin use */
    @GET("qrtags")
    suspend fun getAllQrTags(): Response<List<QrTag>>

    /** Find a QR tag by its printed tagCode */
    @GET("qrtags")
    suspend fun getQrTagByCode(@Query("tagCode") tagCode: String): Response<List<QrTag>>

    /** Get all QR tags linked to a specific profile */
    @GET("qrtags")
    suspend fun getQrTagsByProfile(@Query("profileId") profileId: String): Response<List<QrTag>>

    /** Create a new QR tag (called when order is placed) */
    @POST("qrtags")
    suspend fun createQrTag(@Body tag: QrTag): Response<QrTag>

    /** Update QR tag (link to profile, increment scan count) */
    @PUT("qrtags/{id}")
    suspend fun updateQrTag(@Path("id") id: String, @Body tag: QrTag): Response<QrTag>

    // ─── Orders ───────────────────────────────────────────────────────────────

    /** Get all orders — admin use */
    @GET("orders")
    suspend fun getAllOrders(): Response<List<Order>>

    /** Get orders placed by a specific user */
    @GET("orders")
    suspend fun getOrdersByUser(@Query("userId") userId: String): Response<List<Order>>

    /** Place a new order */
    @POST("orders")
    suspend fun createOrder(@Body order: Order): Response<Order>

    /** Update order status */
    @PUT("orders/{id}")
    suspend fun updateOrder(@Path("id") id: String, @Body order: Order): Response<Order>
}
