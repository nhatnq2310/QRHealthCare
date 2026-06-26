package com.qrhealthcare.app.ui.screens.intro

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Animated intro (≈4s) shown each time the app opens.
 *
 * Storyboard:
 *   1. (0–0.9s)  White medical cross (matches launcher icon) spins in place.
 *   2. (0.9–1.7s) Cross arms retract → morphs into a round "water" sphere.
 *   3. (1.7–2.5s) Sphere morphs into a heart (tilted 90° to the right).
 *   4. (2.5–3.3s) Heart beats twice.
 *   5. (3.3–4.0s) Heart scales up FROM ITS OWN CENTER to fill the screen, fades → app.
 *
 * A single white shape on the brand-red background, drawn on a Canvas as a ring
 * of interpolated points so the morph reads as a smooth liquid change. The
 * starting cross is sized/placed to match the OS splash icon so the hand-off
 * from the system splash is seamless.
 */
@Composable
fun AnimatedIntroScreen(onFinished: () -> Unit) {
    val primary = MaterialTheme.colorScheme.primary
    val white = Color.White

    val t = remember { Animatable(0f) }
    val screenAlpha = remember { Animatable(1f) }

    LaunchedEffect(Unit) {
        t.animateTo(1f, tween(durationMillis = 4000, easing = LinearEasing))
        screenAlpha.animateTo(0f, tween(220))
        onFinished()
    }

    val p = t.value
    val spinEnd = 0.22f
    val sphereEnd = 0.42f
    val heartEnd = 0.62f
    val beatEnd = 0.82f
    // 0.82–1.0 = expand + fade

    fun frac(from: Float, to: Float) = ((p - from) / (to - from)).coerceIn(0f, 1f)

    val spin = frac(0f, spinEnd)
    val toSphere = frac(spinEnd, sphereEnd)
    val toHeart = frac(sphereEnd, heartEnd)
    val beat = frac(heartEnd, beatEnd)
    val expand = frac(beatEnd, 1f)

    // Cross spins one full turn while it's still a cross, then stops.
    val rotation = 360f * EaseInOutCubic.transform(spin)

    // Two heartbeats: a sine envelope that pulses twice.
    val beatScale = if (beat > 0f && expand == 0f) {
        1f + 0.16f * max(0f, sin(beat * 2f * PI.toFloat() * 2f))
    } else 1f

    // Expand: blow the shape up to comfortably cover the screen.
    val expandScale = 1f + EaseInCubic.transform(expand) * 22f

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(primary)
            .alpha(screenAlpha.value),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            // Base radius chosen so the cross roughly matches the launcher-icon
            // size at the moment of hand-off from the system splash.
            val baseR = size.minDimension * 0.13f

            val scale = beatScale * expandScale
            val r = baseR * scale

            // The morph shape lives inside an invisible square whose center is
            // pinned at the screen center. The heart is built centered in that
            // square, so it simply grows from the middle of the screen until the
            // white fills everything — no offset juggling.
            val path = buildMorphPath(
                cx = cx,
                cy = cy,
                radius = r,
                crossToSphere = toSphere,
                sphereToHeart = toHeart,
                rotationDeg = rotation
            )
            drawPath(path, color = white)
        }
    }
}

/**
 * Builds a closed path blending cross → circle → heart, sampled as N points.
 *
 * Two-pass: first compute every point relative to a local origin, then measure
 * the shape's bounding box and translate it so the box CENTER lands exactly on
 * (cx, cy). This is the "invisible square" idea — whatever the current shape is,
 * it sits centered in that square at the screen center and grows from there.
 *
 * `rotationDeg` spins the shape (used during the cross spin phase).
 */
private fun buildMorphPath(
    cx: Float,
    cy: Float,
    radius: Float,
    crossToSphere: Float,
    sphereToHeart: Float,
    rotationDeg: Float
): Path {
    val n = 160
    val rot = rotationDeg * PI.toFloat() / 180f

    val xs = FloatArray(n + 1)
    val ys = FloatArray(n + 1)
    var minX = Float.MAX_VALUE
    var maxX = -Float.MAX_VALUE
    var minY = Float.MAX_VALUE
    var maxY = -Float.MAX_VALUE

    // Pass 1 — compute points around a local (0,0) origin.
    for (i in 0..n) {
        val a = (i.toFloat() / n) * 2f * PI.toFloat()

        val crossR = crossRadius(a) * radius
        val circleR = radius
        val heartR = heartRadius(a) * radius

        val rCrossCircle = lerp(crossR, circleR, smooth(crossToSphere))
        val rr = lerp(rCrossCircle, heartR, smooth(sphereToHeart))

        val ar = a + rot
        val x = rr * cos(ar)
        val y = rr * sin(ar)
        xs[i] = x
        ys[i] = y
        if (x < minX) minX = x
        if (x > maxX) maxX = x
        if (y < minY) minY = y
        if (y > maxY) maxY = y
    }

    // Bounding-box center (the center of the invisible square).
    val boxCx = (minX + maxX) / 2f
    val boxCy = (minY + maxY) / 2f

    // Pass 2 — translate so the box center sits on (cx, cy).
    val path = Path()
    for (i in 0..n) {
        val px = cx + (xs[i] - boxCx)
        val py = cy + (ys[i] - boxCy)
        if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
    }
    path.close()
    return path
}

/**
 * Medical-cross profile in polar form, matched to the launcher icon's
 * proportions: equal arms, arm half-width ≈ 0.5 of arm reach (the launcher
 * cross arms are 8 units wide over a 36-unit span ≈ 0.22 ratio of width to
 * full span; in polar terms that maps to armHalf below).
 */
private fun crossRadius(angle: Float): Float {
    // Equal-arm plus sign matched to the launcher icon. The launcher cross has
    // arms ~8 units wide over a ~36-unit span (width/half-span ≈ 0.44). armHalf
    // is the sine threshold within which we're "on" an arm; 0.30 gives slim
    // arms close to the launcher proportions.
    val armHalf = 0.30f
    fun bar(a: Float): Float {
        val ay = abs(sin(a))
        return if (ay <= armHalf) 1f else armHalf / ay
    }
    val v = bar(angle)
    val h = bar(angle - PI.toFloat() / 2f)
    return max(v, h).coerceAtMost(1.18f)
}

/**
 * Polar heart curve, upright in CANVAS coordinates (y points down): two lobes
 * at the top, point at the bottom — matching the reference heart icon and the
 * phone's vertical orientation. Normalized to roughly fit radius 1.
 */
private fun heartRadius(angle: Float): Float {
    // Standard polar heart magnitude. With (r·cosθ, r·sinθ) in canvas coords
    // (y down), offsetting the sampling angle by +π places the tip (largest
    // radius) pointing DOWN and the lobed dip pointing UP — an upright heart.
    val t = angle + PI.toFloat()
    val sinT = sin(t)
    val cosT = cos(t)
    val r = 2f - 2f * sinT + (sinT * sqrt(abs(cosT))) / (sinT + 1.4f)
    return (r / 3.4f).coerceIn(0f, 1.25f)
}

private fun lerp(a: Float, b: Float, f: Float) = a + (b - a) * f
private fun smooth(x: Float): Float = x * x * (3f - 2f * x)
