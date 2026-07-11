package com.qrhealthcare.app.data.model

import com.google.gson.annotations.SerializedName

// ─── Auth ─────────────────────────────────────────────────────────────────────

data class User(
    val id: String = "",
    val email: String = "",
    val password: String = "",            // Empty in responses from Express backend
    @SerializedName("fullName")
    val fullName: String = "",
    val address: String = "",             // shipping address — edited from Account Settings
    val phone: String = "",               // contact phone
    val city: String = "",                // city / province
    val role: String = "user",            // "user" | "admin"
    val createdAt: Long = 0L
)

data class AuthResult(
    val user: User? = null,
    val token: String? = null,            // JWT in production, fake token for MockAPI
    val error: String? = null
)

// ─── Profile ──────────────────────────────────────────────────────────────────

data class Profile(
    val id: String = "",
    val userId: String = "",
    val profileType: String = "human",    // "human" | "pet"
    val fullName: String = "",
    val gender: String = "",
    val birthDate: String = "",           // ISO format: "1990-05-15"
    val bloodGroup: String = "",          // "O+", "A-", "AB+", etc.
    val height: String = "",              // "170cm"
    val weight: String = "",              // "65kg"
    val hairColor: String = "",
    val eyeColor: String = "",
    val identificationMark: String = "",
    val personalNumber: String = "",      // CCCD / Passport number
    val organDonor: Boolean = false,         // true = "đã đăng ký", false = "chưa đăng ký"
    val showOrganDonor: Boolean = true,      // when registered, whether to display it publicly
    // Privacy settings
    val isPrivate: Boolean = false,               // Master privacy toggle
    val subscriptionFrozen: Boolean = false,       // Forced private by unpaid maintenance plan — independent of isPrivate
    val hiddenFields: List<String> = emptyList(), // Field keys that are hidden when isPrivate=true
    // Nested data
    val emergencyContacts: List<EmergencyContact> = emptyList(),
    val allergies: List<Allergy> = emptyList(),
    val medications: List<Medication> = emptyList(),
    val medicalConditions: List<MedicalCondition> = emptyList(),
    val addresses: List<ProfileAddress> = emptyList(),
    val healthInsurance: List<Insurance> = emptyList(),  // bảo hiểm y tế
    val lifeInsurance: List<Insurance> = emptyList(),    // bảo hiểm nhân thọ
    val healthDocuments: List<String> = emptyList(), // relative URLs from POST /uploads, e.g. "/uploads/abc.jpg"
    val notes: String = "",                          // free-form personal notes (dietary plan, etc.)
    val viewCount: Int = 0,
    val createdAt: Long = 0L
)

// Fields that are ALWAYS visible to the public regardless of privacy settings.
// These are safety-critical for emergency responders: who the person is,
// their blood type, who to call, and what they're allergic to.
val ALWAYS_VISIBLE_FIELDS = listOf(
    "fullName", "gender", "bloodGroup", "emergencyContacts", "allergies"
)

data class EmergencyContact(
    val name: String = "",
    val phone: String = "",
    val relationship: String = ""
)

data class Allergy(
    val name: String = "",
    val severity: String = "",            // "Nhẹ" | "Trung bình" | "Nặng"
    val reaction: String = ""
)

data class Medication(
    val name: String = "",
    val dosage: String = "",
    val frequency: String = ""
)

data class MedicalCondition(
    val name: String = "",
    val diagnosedDate: String = "",
    val notes: String = ""
)

data class ProfileAddress(
    val street: String = "",
    val ward: String = "",
    val district: String = "",
    val city: String = "",
    val country: String = "Việt Nam"
)

data class Insurance(
    val provider: String = "",
    val policyNumber: String = "",
    val expiryDate: String = ""
)

// ─── Products ─────────────────────────────────────────────────────────────────

data class Product(
    val id: String = "",
    val slug: String = "",
    val name: String = "",
    val price: Long = 0L,
    val oldPrice: Long? = null,
    val badge: String = "",
    val description: String = "",
    val imageUrl: String = "",
    val category: String = "",            // "sticker" | "card" | "tag"
    // How many QR tags to generate per unit purchased of this product.
    // 0 = no QR tag (accessory/refill item), 1 = normal single item,
    // 2+ = combo/bundle containing multiple trackable items.
    // IMPORTANT: category alone doesn't tell you this — a combo and a plain
    // refill sticker can share the same category but need different counts.
    val qrTagsPerUnit: Int = 1,
    val emergencyContactRequired: Boolean = false,  // replaces legacy bloodGroupSelect
    val lowStock: Boolean = false,
    val quantity: String = "",
    val dimensions: String = "",
    val materials: String = "",
    val durability: List<String> = emptyList(),
    val shipping: String = ""
)

// ─── Cart (local only — never sent to API) ────────────────────────────────────

data class CartItem(
    val product: Product,
    val quantity: Int,
    val emergencyContact: String = ""
)

// ─── QR Tags ──────────────────────────────────────────────────────────────────

data class QrTag(
    val id: String = "",
    val tagCode: String = "",             // Unique code on physical product e.g. "QRH-A1B2"
    val pin: String = "",                 // 4-digit PIN on physical product e.g. "3847"
    val profileId: String? = null,        // null = not yet linked
    val productType: String = "",         // "sticker" | "card" | "tag"
    val orderId: String = "",
    val scanCount: Int = 0,              // Admin metric: how many times QR was scanned
    val createdAt: Long = 0L
)

// ─── Orders ───────────────────────────────────────────────────────────────────

data class Order(
    val id: String = "",
    val userId: String = "",
    val profileId: String = "",
    val items: List<OrderItem> = emptyList(),
    val totalAmount: Long = 0L,           // final, after discount
    val discountAmount: Long = 0L,        // 0 if no coupon
    val couponCode: String = "",          // empty if no coupon
    val paymentRef: String = "",          // QR transfer note for reconciliation (e.g. QRH12345678)
    val paymentMethod: String = "",       // "vietqr" | "cash" (legacy: "bank" | "momo" | "vnpay" | "google_play")
    val status: String = "pending",       // "pending" | "paid" | "shipped" | "delivered"
    val shippingAddress: ShippingAddress = ShippingAddress(), // per-order delivery details
    val qrTagIds: List<String> = emptyList(), // IDs of generated QR tags
    val isPromo: Boolean = false,   // true for the free-tag gift order (first paid subscription month)
    val shippingFee: Long = 0L,     // always 0 today — no fee system yet
    val createdAt: Long = 0L
)

data class ShippingAddress(
    val fullName: String = "",
    val phone: String = "",
    val address: String = "",
    val city: String = "",
    val note: String = ""
)

data class OrderItem(
    val productId: String = "",
    val productSlug: String = "",  // used by "Mua Lại" (buy again) to look the product back up
    val productName: String = "",
    val imageUrl: String = "",     // snapshot of the product image at order time, for the Shopee-style history list
    val price: Long = 0L,
    val quantity: Int = 0,
    val emergencyContact: String = ""
)

// ─── Coupons ──────────────────────────────────────────────────────────────────

data class Coupon(
    val id: String = "",
    val code: String = "",
    val description: String = "",
    val discountType: String = "fixed",   // "percent" | "fixed"
    val discountValue: Long = 0L,         // 1-100 for percent, VND amount for fixed
    val minOrderAmount: Long = 0L,
    val maxDiscount: Long? = null,
    val expiresAt: Long? = null,
    val active: Boolean = true,
    val usageLimit: Int? = null,
    val usageCount: Int = 0,
    val hidden: Boolean = false,          // secret code — not shown in store banner
    val createdAt: Long = 0L
)

/** Response shape from POST /coupons/validate — `valid` tells you everything. */
data class CouponValidation(
    val valid: Boolean = false,
    val coupon: Coupon? = null,
    val discountAmount: Long = 0L,
    val finalAmount: Long = 0L,
    val error: String? = null
)

// ─── Checkout funnel tracking ──────────────────────────────────────────────────

/**
 * Mirrors backend/src/models/CheckoutSession.js. One row per checkout attempt,
 * created when the user opens the shipping-info screen and updated as they
 * move forward — independent of whether an Order ever gets created. This is
 * what powers drop-out / abandonment reporting, since the Order collection
 * alone only contains completed checkouts.
 */
data class CheckoutSession(
    val id: String = "",
    val userId: String = "",
    val cartValue: Long = 0L,
    val itemCount: Int = 0,
    val step: Int = 1,           // furthest step reached: 1=shipping, 2=profile, 3=payment method, 4=confirmed
    val completed: Boolean = false,
    val orderId: String = "",
    val paymentMethod: String = "",
    val startedAt: Long = 0L,
    val updatedAt: Long = 0L
)

// Thrown by AppRepository.createProfile() when the server rejects creation
// because the maintenance subscription is required (limit reached / expired)
// — lets the UI distinguish "show the subscription popup" from any other
// generic profile-creation failure.
class SubscriptionRequiredException(message: String) : Exception(message)

// ─── Subscription (gói duy trì lưu trữ hồ sơ) ─────────────────────────────────

data class SubscriptionHistoryEntry(
    val event: String = "",          // trial_started | subscribed | renewed | cancelled | expired
    val plan: String = "",
    val amount: Long = 0L,
    val extraProfiles: Int = 0,
    val at: Long = 0L
)

/** Mirrors backend/src/models/Subscription.js. One doc per user. */
data class Subscription(
    val id: String = "",
    val userId: String = "",
    val status: String = "trial",    // trial | active | expired | cancelled
    val plan: String = "trial",       // trial | monthly | flexible | yearly
    val extraProfiles: Int = 0,
    val periodStart: Long = 0L,
    val periodEnd: Long = 0L,
    val lastAmount: Long = 0L,
    val paymentRef: String = "",
    val history: List<SubscriptionHistoryEntry> = emptyList(),
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    // Only present in the response of POST /subscriptions/renew, and only the
    // very first time a user actually pays (trial/expired/cancelled -> active).
    // Represents the free physical QR tag + free shipping promo.
    val promoTag: QrTag? = null
) {
    val isActive: Boolean get() = status == "active"
    val isTrial: Boolean get() = status == "trial"
    val isBlocked: Boolean get() = status == "expired" || status == "cancelled"
    val daysRemaining: Long get() = ((periodEnd - System.currentTimeMillis()) / (24 * 60 * 60 * 1000L)).coerceAtLeast(0)
    val totalSlots: Int get() = 5 + (if (status == "active") extraProfiles else 0)
}

/**
 * Mirrors computeAmount() in backend/src/routes/subscriptions.js — used to
 * show the live total on the subscription screen as the user types the
 * number of extra profiles. The backend is the source of truth for what's
 * actually charged; this is purely for instant UI feedback.
 * NOTE: yearly gets a discounted extra-profile rate — 4k/month-equivalent,
 * billed as 12 months upfront = 48k/profile/year — vs. monthly/flexible's
 * plain 5k/profile/month.
 */
object SubscriptionPricing {
    const val MONTHLY_BASE = 20_000L
    const val YEARLY_BASE = 199_000L
    const val EXTRA_PROFILE_PRICE = 5_000L         // monthly & flexible
    const val EXTRA_PROFILE_PRICE_YEARLY = 4_000L  // yearly: per month-equivalent, x12 below

    fun computeAmount(plan: String, extraProfiles: Int): Long {
        val extra = extraProfiles.coerceAtLeast(0)
        return if (plan == "yearly") YEARLY_BASE + extra * EXTRA_PROFILE_PRICE_YEARLY * 12
        else MONTHLY_BASE + extra * EXTRA_PROFILE_PRICE
    }
}

data class SubscriptionAdminStats(
    val subscribersWeek: Int = 0, val subscribersMonth: Int = 0, val subscribersYear: Int = 0, val subscribersLifetime: Int = 0,
    val cancellationsWeek: Int = 0, val cancellationsMonth: Int = 0, val cancellationsYear: Int = 0, val cancellationsLifetime: Int = 0,
    val activeCount: Int = 0, val trialCount: Int = 0, val expiredCount: Int = 0, val cancelledCount: Int = 0
)

// ─── Admin ────────────────────────────────────────────────────────────────────

data class AdminMetrics(
    val totalUsers: Int = 0,
    val totalProfiles: Int = 0,
    val totalOrders: Int = 0,
    val totalRevenue: Long = 0L,
    val totalQrScans: Int = 0,
    val recentOrders: List<Order> = emptyList(),
    val ordersByStatus: Map<String, Int> = emptyMap(),
    val productSalesCounts: Map<String, Int> = emptyMap(),  // productName → count sold
    // ─ Time-period revenue (always-on KPIs) ──────────────────────────────────
    val revenueToday: Long = 0L,
    val revenueThisWeek: Long = 0L,
    val revenueThisMonth: Long = 0L,
    val ordersToday: Int = 0,
    val newUsersToday: Int = 0,
    // ─ Trend chart data — last 14 days, oldest first ──────────────────────────
    val dailyRevenue: List<Long> = emptyList(),
    val dailyOrderCount: List<Int> = emptyList(),
    // ─ Full lists for the management tabs ────────────────────────────────────
    val allUsers: List<User> = emptyList(),
    val allOrders: List<Order> = emptyList(),
    val allCoupons: List<Coupon> = emptyList(),
    // ─ Checkout funnel: drop-out & abandonment ───────────────────────────────
    val checkoutSessionsStarted: Int = 0,        // total checkouts started (any step reached)
    val checkoutSessionsCompleted: Int = 0,      // reached step 4 (order confirmed)
    val checkoutAbandonmentRate: Double = 0.0,    // 0..1 — started but never completed
    val checkoutConversionRate: Double = 0.0,     // 0..1 — completed / started
    val abandonedCartValue: Long = 0L,            // sum of cartValue on incomplete sessions
    val dropOutByStep: Map<Int, Int> = emptyMap(),// step → count of sessions that stalled there (furthest reached, never progressed further, not completed)
    val allCheckoutSessions: List<CheckoutSession> = emptyList(),
    val subscriptionStats: SubscriptionAdminStats = SubscriptionAdminStats()
)
