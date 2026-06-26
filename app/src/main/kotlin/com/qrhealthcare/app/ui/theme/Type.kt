package com.qrhealthcare.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.qrhealthcare.app.R

// ── Albert Sans (bundled variable font, res/font/albert_sans_var.ttf) ─────────
// Matches the design reference. Bundled so it works offline with no network
// fetch. Each weight maps to the same variable file via a weight FontVariation.
private fun albertWeight(w: Int, fw: FontWeight) = Font(
    R.font.albert_sans_var,
    variationSettings = FontVariation.Settings(FontVariation.weight(w)),
    weight = fw
)

val AlbertSans = FontFamily(
    albertWeight(400, FontWeight.Normal),
    albertWeight(500, FontWeight.Medium),
    albertWeight(600, FontWeight.SemiBold),
    albertWeight(700, FontWeight.Bold),
    albertWeight(900, FontWeight.Black),
)

val Typography = Typography(
    headlineLarge = TextStyle(
        fontFamily = AlbertSans, fontWeight = FontWeight.Black, fontSize = 30.sp, lineHeight = 36.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = AlbertSans, fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 30.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = AlbertSans, fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 26.sp
    ),
    titleLarge = TextStyle(
        fontFamily = AlbertSans, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, lineHeight = 24.sp
    ),
    titleMedium = TextStyle(
        fontFamily = AlbertSans, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp
    ),
    titleSmall = TextStyle(
        fontFamily = AlbertSans, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = AlbertSans, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = AlbertSans, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp
    ),
    bodySmall = TextStyle(
        fontFamily = AlbertSans, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp
    ),
    labelLarge = TextStyle(
        fontFamily = AlbertSans, fontWeight = FontWeight.SemiBold, fontSize = 14.sp
    ),
    labelMedium = TextStyle(
        fontFamily = AlbertSans, fontWeight = FontWeight.Medium, fontSize = 12.sp
    ),
    labelSmall = TextStyle(
        fontFamily = AlbertSans, fontWeight = FontWeight.Medium, fontSize = 11.sp
    )
)
