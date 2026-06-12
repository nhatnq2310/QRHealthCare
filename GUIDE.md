# QR Healthcare – Android App Setup Guide
> Kotlin + Jetpack Compose · MockAPI → Express.js + MongoDB migration path

---

## Overview

This guide walks you through:
1. Setting up **MockAPI** as a temporary backend
2. Creating the **Android Studio** project
3. Copying source files and running the app
4. Migrating from MockAPI → **Express.js + MongoDB** later

Architecture: **MVVM + Repository pattern** — swapping backends only requires changing the repository implementation, nothing in the UI or ViewModels.

---

## PART 1 — MockAPI Setup

### 1.1 Create Account & Project
1. Go to **https://mockapi.io** → Sign up (free)
2. Click **"New Project"** → Name: `qrhealthcare` → Create
3. Copy your **Project ID** from the URL (looks like `64abc123def456`) — you'll need it later

Your base URL will be:
```
https://{YOUR_PROJECT_ID}.mockapi.io/api/v1/
```

---

### 1.2 Create Resource 1 — `users`

Click **"+ New Resource"** → Name: `users`

Schema (click the pencil icon on each field to edit):

| Field       | Type   | Default    |
|-------------|--------|------------|
| `email`     | String | —          |
| `password`  | String | —          |
| `fullName`  | String | —          |
| `role`      | String | `user`     |
| `createdAt` | Number | (auto)     |

> **⚠️ Note:** MockAPI stores passwords as plain text. This is for prototyping ONLY. In production (Express.js), always use bcrypt hashing.

After creating, click **"Generate"** to seed 2–3 fake users, then manually edit one to have `role: "admin"`.

---

### 1.3 Create Resource 2 — `profiles`

Name: `profiles`

| Field                | Type    | Default   |
|----------------------|---------|-----------|
| `userId`             | String  | —         |
| `profileType`        | String  | `human`   |
| `fullName`           | String  | —         |
| `gender`             | String  | —         |
| `birthDate`          | String  | —         |
| `bloodGroup`         | String  | —         |
| `height`             | String  | —         |
| `weight`             | String  | —         |
| `hairColor`          | String  | —         |
| `eyeColor`           | String  | —         |
| `identificationMark` | String  | —         |
| `organDonor`         | Boolean | `false`   |
| `isPrivate`          | Boolean | `false`   |
| `hiddenFields`       | Object  | `[]`      |
| `personalNumber`     | String  | —         |
| `emergencyContacts`  | Object  | `[]`      |
| `allergies`          | Object  | `[]`      |
| `medications`        | Object  | `[]`      |
| `medicalConditions`  | Object  | `[]`      |
| `addresses`          | Object  | `[]`      |
| `insurance`          | Object  | `[]`      |
| `viewCount`          | Number  | `0`       |
| `createdAt`          | Number  | (auto)    |

> Fields typed as **Object** store arrays of JSON objects (emergency contacts, allergies, etc.)

---

### 1.4 Create Resource 3 — `products`

Name: `products`

| Field            | Type    | Default |
|------------------|---------|---------|
| `slug`           | String  | —       |
| `name`           | String  | —       |
| `price`          | Number  | —       |
| `oldPrice`       | Number  | —       |
| `badge`          | String  | —       |
| `description`    | String  | —       |
| `imageUrl`       | String  | —       |
| `category`       | String  | —       |
| `bloodGroupSelect`| Boolean| `false` |
| `lowStock`       | Boolean | `false` |
| `quantity`       | String  | —       |
| `dimensions`     | String  | —       |
| `materials`      | String  | —       |
| `durability`     | Object  | `[]`    |
| `shipping`       | String  | —       |

**Seed your products manually** after creation. Click "Generate", then delete the random data and add real products. Example JSON for one product:

```json
{
  "slug": "combo-sticker-y-te",
  "name": "Combo Sticker Y Tế + Sticker Nhóm Máu",
  "price": 125000,
  "oldPrice": 250000,
  "badge": "Bán Chạy",
  "description": "2 Sticker Y Tế + 2 Sticker Che Phủ + 2 Sticker Nhóm Máu + Sticker Phản Quang 3M",
  "imageUrl": "https://placehold.co/400x400/e53935/ffffff?text=Combo+Sticker",
  "category": "sticker",
  "bloodGroupSelect": true,
  "lowStock": true,
  "quantity": "7 Sticker",
  "dimensions": "7.6 x 2.5 cm",
  "materials": "Vinyl chất lượng cao với lớp phủ Matt Lamination",
  "durability": ["Chống Nước", "Chống Tia UV", "Chống Bụi"],
  "shipping": "Gửi trong vòng 24 giờ sau khi đặt hàng"
}
```

Repeat for all 7 products from the web app.

---

### 1.5 Create Resource 4 — `orders`

Name: `orders`

| Field           | Type   | Default   |
|-----------------|--------|-----------|
| `userId`        | String | —         |
| `profileId`     | String | —         |
| `items`         | Object | `[]`      |
| `totalAmount`   | Number | `0`       |
| `paymentMethod` | String | —         |
| `status`        | String | `pending` |
| `qrTagIds`      | Object | `[]`      |
| `createdAt`     | Number | (auto)    |

---

### 1.6 Create Resource 5 — `qrtags`

Name: `qrtags`

| Field         | Type    | Default |
|---------------|---------|---------|
| `tagCode`     | String  | —       |
| `pin`         | String  | —       |
| `profileId`   | String  | `null`  |
| `productType` | String  | —       |
| `orderId`     | String  | —       |
| `scanCount`   | Number  | `0`     |
| `createdAt`   | Number  | (auto)  |

> `tagCode` = unique 8-char code printed on physical product (e.g. `QRH-A1B2`)
> `pin` = 4-digit PIN printed on product (e.g. `3847`)

---

## PART 2 — Android Studio Project

### 2.1 Create New Project
1. Open **Android Studio** (Hedgehog or newer)
2. **File → New → New Project**
3. Choose template: **"Empty Activity"** (Compose)
4. Configure:
   - **Name:** `QR Healthcare`
   - **Package:** `com.qrhealthcare.app`
   - **Save location:** your preferred folder
   - **Language:** Kotlin
   - **Minimum SDK:** API 26 (Android 8.0)
   - **Build configuration language:** Kotlin DSL (.kts)
5. Click **Finish**

---

### 2.2 Update `libs.versions.toml`

Open `gradle/libs.versions.toml` and add the following inside each section:

```toml
[versions]
# ADD THESE:
hilt = "2.52"
retrofit = "2.11.0"
okhttp = "4.12.0"
coroutines = "1.8.1"
navigationCompose = "2.8.5"
hiltNavigationCompose = "1.2.0"
datastore = "1.1.1"
coil = "2.7.0"
zxing = "3.5.3"

[libraries]
# ADD THESE:
hilt-android = { group = "com.google.dagger", name = "hilt-android", version.ref = "hilt" }
hilt-compiler = { group = "com.google.dagger", name = "hilt-android-compiler", version.ref = "hilt" }
hilt-navigation-compose = { group = "androidx.hilt", name = "hilt-navigation-compose", version.ref = "hiltNavigationCompose" }
retrofit = { group = "com.squareup.retrofit2", name = "retrofit", version.ref = "retrofit" }
retrofit-gson = { group = "com.squareup.retrofit2", name = "converter-gson", version.ref = "retrofit" }
okhttp-logging = { group = "com.squareup.okhttp3", name = "logging-interceptor", version.ref = "okhttp" }
coroutines-android = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-android", version.ref = "coroutines" }
navigation-compose = { group = "androidx.navigation", name = "navigation-compose", version.ref = "navigationCompose" }
datastore = { group = "androidx.datastore", name = "datastore-preferences", version.ref = "datastore" }
coil-compose = { group = "io.coil-kt", name = "coil-compose", version.ref = "coil" }
zxing = { group = "com.google.zxing", name = "core", version.ref = "zxing" }

[plugins]
# ADD THIS:
hilt = { id = "com.google.dagger.hilt.android", version.ref = "hilt" }
```

---

### 2.3 Copy Source Files

Copy all `.kt` files from the provided source directory into their matching paths under:
```
app/src/main/kotlin/com/qrhealthcare/app/
```

Copy `AndroidManifest.xml` and `strings.xml` to their paths.

---

### 2.4 Set Your MockAPI URL

Open `app/src/main/kotlin/com/qrhealthcare/app/data/api/ApiClient.kt`

Find this line and replace with your project ID:
```kotlin
private const val BASE_URL = "https://YOUR_PROJECT_ID.mockapi.io/api/v1/"
```

---

### 2.5 Build & Run

1. Connect your Android device or start an emulator (API 26+)
2. Click ▶ **Run** in Android Studio
3. The app will build and install

**First-time setup:**
- Register an account via the app
- To test admin: manually change `role` to `"admin"` in MockAPI dashboard
- Add products in MockAPI dashboard

---

## PART 3 — Migrating to Express.js + MongoDB

When you're ready to publish on Google Play:

### 3.1 Backend Changes (Express.js)

Your Express.js server should expose the same REST endpoints:
```
POST   /api/v1/auth/login
POST   /api/v1/auth/register
GET    /api/v1/profiles?userId=xxx
POST   /api/v1/profiles
PUT    /api/v1/profiles/:id
DELETE /api/v1/profiles/:id
GET    /api/v1/products
GET    /api/v1/orders?userId=xxx
POST   /api/v1/orders
GET    /api/v1/qrtags?tagCode=xxx
PUT    /api/v1/qrtags/:id
```

### 3.2 Android Changes (only 3 files to edit)

**1. `ApiClient.kt`** — Change base URL:
```kotlin
// FROM:
private const val BASE_URL = "https://YOUR_PROJECT_ID.mockapi.io/api/v1/"
// TO:
private const val BASE_URL = "https://your-express-server.com/api/v1/"
```

**2. `ApiClient.kt`** — Uncomment the auth interceptor:
```kotlin
.addInterceptor(AuthInterceptor(tokenManager))
```

**3. `AppRepository.kt`** — Update `login()` to use real JWT:
```kotlin
// The login() function already has a TODO for JWT — implement it
```

**Everything else stays the same.** The MVVM + Repository pattern isolates the data layer completely.

### 3.3 MongoDB Collections

Match these collection names to the MockAPI resources:
- `users` → `users` collection
- `profiles` → `profiles` collection  
- `products` → `products` collection
- `orders` → `orders` collection
- `qr_tags` → `qrtags` collection

### 3.4 Google Play Billing

For the simulated payment → real CH Play payment:
- Add `com.android.billingclient:billing-ktx:7.0.0` to dependencies
- Implement `BillingClient` in `PaymentScreen.kt` (there's a TODO marker)
- Register products in Google Play Console as "in-app products" or "subscriptions"

---

## PART 4 — App Features Summary

| Feature | Description |
|---------|-------------|
| **Home** | Intro, profile lookup by ID |
| **Shop** | Product listing, cart, simulated payment (COD / bank / MoMo / VNPay / Google Pay) |
| **Profile** | Create up to 5 profiles (human/pet), privacy toggle, field checkboxes |
| **QR Linking** | Link physical QR tag to profile via tagCode + PIN |
| **Public Profile** | Doctor/rescuer scans QR → sees public health info only |
| **Admin Dashboard** | Total users, profiles, orders, revenue, recent orders, product sales |

---

## Admin Login

Create an admin account by:
1. Register normally in the app
2. Go to MockAPI → `users` resource → find your user
3. Change `role` from `"user"` to `"admin"`
4. Log out and log in again → Admin tab appears

---

## Key Design Decisions

- **No real auth in MockAPI** — passwords are plain text, login is done by client-side lookup. **Do not use real data in prototype.**
- **1 profile = many QR tags** — each physical product has a unique tagCode/PIN
- **QR value** = `qrhealthcare://profile/{tagCode}` (deep link) — registered in AndroidManifest
- **Privacy logic** — `isPrivate: true` + `hiddenFields: ["bloodGroup"]` hides those fields from public view; `full_name`, `gender`, `blood_group`, `organ_donor` are always visible
- **Max 5 profiles per user** — enforced in ProfileViewModel before calling API
