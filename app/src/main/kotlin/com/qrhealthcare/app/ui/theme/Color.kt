package com.qrhealthcare.app.ui.theme

import androidx.compose.ui.graphics.Color

// ════════════════════════════════════════════════════════════════════════════
//  QR HEALTHCARE — COLOR SYSTEM
//  Primary  : brand red (unchanged — the app's identity + call-to-action color)
//  Secondary: medical teal (trust, calm, "healthcare" feeling)
//  Neutrals : soft cool grays (clinical, clean, easy on the eyes)
// ════════════════════════════════════════════════════════════════════════════

// ─── Primary (Brand Red — matches web app --primary: 0 84% 50%) ─────────────
val Red500   = Color(0xFFE53935)  // Primary red — main actions, brand
val Red600   = Color(0xFFD32F2F)  // Darker red — pressed / emphasis
val Red700   = Color(0xFFC62828)  // Deepest red — gradients
val Red50    = Color(0xFFFFEBEE)  // Very light red — tinted backgrounds
val RedAlpha = Color(0x1AE53935)  // 10% red — subtle highlights

// ─── Secondary (Medical Teal — the healthcare accent) ───────────────────────
val Teal500  = Color(0xFF00897B)  // Primary teal — secondary actions, health accents
val Teal600  = Color(0xFF00796B)  // Darker teal — pressed
val Teal400  = Color(0xFF26A69A)  // Lighter teal — icons, highlights
val Teal50   = Color(0xFFE0F2F1)  // Very light teal — tinted cards/backgrounds
val Teal100  = Color(0xFFB2DFDB)  // Light teal — borders, chips

// ─── Neutral (Cool clinical grays) ───────────────────────────────────────────
val White     = Color(0xFFFFFFFF)
val OffWhite  = Color(0xFFF8FAFB)  // App background — soft cool white, not stark
val Black      = Color(0xFF1A2027)  // Text — slightly blue-black, softer than pure black
val Gray50     = Color(0xFFF1F4F6)  // Secondary surfaces
val Gray100    = Color(0xFFE3E8EC)  // Borders, dividers
val Gray200     = Color(0xFFD1D9DF)  // Stronger borders
val Gray400     = Color(0xFF8A97A3)  // Muted text, placeholder
val Gray600     = Color(0xFF5A6772)  // Secondary text
val Gray700     = Color(0xFF3D4852)  // Subtitle text
val Gray900     = Color(0xFF1A2027)  // Same as Black

// ─── Status colors (health-app appropriate) ─────────────────────────────────
val SuccessGreen = Color(0xFF2E9E5B)  // Confirmations, "delivered", success
val SuccessBg    = Color(0xFFE6F4EC)  // Success tinted background
val WarningAmber = Color(0xFFE8A317)  // Pending, caution
val WarningBg    = Color(0xFFFCF3E2)  // Warning tinted background
val InfoBlue     = Color(0xFF2A7DE1)  // Information, shipping
val InfoBg       = Color(0xFFE7F0FC)  // Info tinted background
val DangerRed    = Color(0xFFE53935)  // Errors (== brand red)
