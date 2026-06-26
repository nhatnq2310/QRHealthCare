package com.qrhealthcare.app.ui.screens.intro

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/**
 * Medical-themed animated intro shown each time the app opens.
 *
 * Sequence (~2.6s total):
 *   1. A rounded medical cross draws itself stroke-by-stroke.
 *   2. An ECG/heartbeat line sweeps across beneath it.
 *   3. The app name + tagline fade and slide up.
 *   4. The whole screen fades out and hands off to the app.
 */
@Composable
fun AnimatedIntroScreen(onFinished: () -> Unit) {
    val primary = MaterialTheme.colorScheme.primary
    val onPrimary = Color.White

    val crossProgress = remember { Animatable(0f) }
    val ecgProgress = remember { Animatable(0f) }
    val pulseScale = remember { Animatable(0.8f) }
    val textAlpha = remember { Animatable(0f) }
    val textOffset = remember { Animatable(20f) }
    val screenAlpha = remember { Animatable(1f) }

    LaunchedEffect(Unit) {
        crossProgress.animateTo(1f, tween(700, easing = FastOutSlowInEasing))
        ecgProgress.animateTo(1f, tween(800, easing = LinearEasing))
        pulseScale.animateTo(
            1f,
            spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
        )
        textOffset.animateTo(0f, tween(500, easing = FastOutSlowInEasing))
        textAlpha.animateTo(1f, tween(500))
        delay(600)
        screenAlpha.animateTo(0f, tween(450, easing = FastOutLinearInEasing))
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(primary)
            .alpha(screenAlpha.value),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Canvas(
                modifier = Modifier
                    .size(160.dp)
                    .scale(pulseScale.value)
            ) {
                val w = size.width
                val h = size.height
                val armThickness = w * 0.26f
                val cx = w / 2f
                val cy = h / 2f
                val half = w * 0.30f

                val crossPath = Path().apply {
                    addRoundRect(
                        androidx.compose.ui.geometry.RoundRect(
                            left = cx - armThickness / 2, top = cy - half,
                            right = cx + armThickness / 2, bottom = cy + half,
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(armThickness * 0.3f)
                        )
                    )
                    addRoundRect(
                        androidx.compose.ui.geometry.RoundRect(
                            left = cx - half, top = cy - armThickness / 2,
                            right = cx + half, bottom = cy + armThickness / 2,
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(armThickness * 0.3f)
                        )
                    )
                }

                val measure = PathMeasure()
                measure.setPath(crossPath, false)
                val total = measure.length
                val revealed = Path()
                measure.getSegment(0f, total * crossProgress.value, revealed, true)
                if (measure.nextContour()) {
                    val len2 = measure.length
                    measure.getSegment(0f, len2 * crossProgress.value, revealed, true)
                }

                drawPath(
                    path = revealed,
                    color = onPrimary,
                    style = Stroke(width = w * 0.035f, cap = StrokeCap.Round)
                )
                if (crossProgress.value >= 1f) {
                    drawPath(crossPath, color = onPrimary.copy(alpha = (pulseScale.value - 0.8f) / 0.2f))
                }

                if (ecgProgress.value > 0f) {
                    val baseY = h * 0.92f
                    val ecg = Path()
                    ecg.moveTo(0f, baseY)
                    val pts = listOf(
                        0.30f to 0f, 0.38f to -0.18f, 0.46f to 0.30f,
                        0.54f to -0.42f, 0.62f to 0.10f, 0.70f to 0f, 1f to 0f
                    )
                    for ((fx, fy) in pts) {
                        ecg.lineTo(w * fx, baseY + h * fy)
                    }
                    val em = PathMeasure()
                    em.setPath(ecg, false)
                    val seg = Path()
                    em.getSegment(0f, em.length * ecgProgress.value, seg, true)
                    drawPath(
                        seg,
                        color = onPrimary.copy(alpha = 0.9f),
                        style = Stroke(width = w * 0.022f, cap = StrokeCap.Round)
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .alpha(textAlpha.value)
                    .offset(y = textOffset.value.dp)
            ) {
                Text(
                    "QR Healthcare",
                    color = onPrimary,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Black
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Thông tin y tế trong tầm tay",
                    color = onPrimary.copy(alpha = 0.85f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
