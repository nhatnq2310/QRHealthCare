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
import kotlin.math.cos
import kotlin.math.sin

/**
 * Animated intro (≈4s) shown each time the app opens.
 *
 * Storyboard:
 *   1. (0–1.0s)  White medical cross spins in place (540°).
 *   2. (1.0–1.8s) Cross arms retract → morphs into a round "water" sphere.
 *   3. (1.8–2.6s) Sphere morphs into a heart shape.
 *   4. (2.6–3.4s) Heart beats twice.
 *   5. (3.4–4.0s) Heart scales up to fill the screen, then fades → app.
 *
 * Everything is a single white shape on the brand-red background, drawn on a
 * Canvas. The cross→sphere and sphere→heart transitions are done by
 * interpolating a ring of points, so the morph reads as a smooth liquid change.
 */
@Composable
fun AnimatedIntroScreen(onFinished: () -> Unit) {
    val primary = MaterialTheme.colorScheme.primary
    val white = Color.White

    // Master timeline 0f..1f mapped to ~4000ms.
    val t = remember { Animatable(0f) }
    val screenAlpha = remember { Animatable(1f) }

    LaunchedEffect(Unit) {
        t.animateTo(1f, tween(durationMillis = 4000, easing = LinearEasing))
        screenAlpha.animateTo(0f, tween(250))
        onFinished()
    }

    // ── Phase boundaries (fractions of the 0..1 timeline) ──────────────────────
    val p = t.value
    val spinEnd = 0.25f      // 1.0s
    val sphereEnd = 0.45f    // 1.8s
    val heartEnd = 0.65f     // 2.6s
    val beatEnd = 0.85f      // 3.4s
    // 0.85–1.0 = expand + fade

    // Phase-local progress helpers
    fun frac(from: Float, to: Float) = ((p - from) / (to - from)).coerceIn(0f, 1f)

    val spin = frac(0f, spinEnd)
    val toSphere = frac(spinEnd, sphereEnd)
    val toHeart = frac(sphereEnd, heartEnd)
    val beat = frac(heartEnd, beatEnd)
    val expand = frac(beatEnd, 1f)

    // Rotation: spin during phase 1, then settle.
    val rotation = 540f * EaseInOutCubic.transform(spin)

    // Beat scale — two pulses using a sine envelope.
    val beatScale = if (beat > 0f && expand == 0f) {
        1f + 0.18f * sin(beat * 2f * PI.toFloat() * 2f).let { if (it < 0) 0f else it }
    } else 1f

    // Expand scale — blow the heart up to cover the screen.
    val expandScale = 1f + EaseInCubic.transform(expand) * 14f

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
            val baseR = size.minDimension * 0.13f   // base radius of the icon

            val scale = beatScale * expandScale
            val r = baseR * scale

            // Build the current morph shape as a ring of points.
            // We interpolate between three "states":
            //   cross  → sphere (toSphere)
            //   sphere → heart  (toHeart)
            val path = buildMorphPath(
                cx = cx, cy = cy, radius = r,
                crossToSphere = toSphere,
                sphereToHeart = toHeart,
                rotationDeg = rotation
            )
            drawPath(path, color = white)
        }
    }
}

/**
 * Builds a closed path that is a blend of a cross, a circle, and a heart,
 * sampled as N points around the center. `crossToSphere` morphs cross→circle;
 * `sphereToHeart` then morphs circle→heart. `rotationDeg` spins the whole shape.
 */
private fun buildMorphPath(
    cx: Float,
    cy: Float,
    radius: Float,
    crossToSphere: Float,
    sphereToHeart: Float,
    rotationDeg: Float
): Path {
    val n = 120
    val rot = rotationDeg * PI.toFloat() / 180f
    val path = Path()

    for (i in 0..n) {
        val a = (i.toFloat() / n) * 2f * PI.toFloat()   // angle 0..2π

        // Radius of a "cross" at this angle: a plus/cross shape via a smooth
        // approximation — narrower between arms, full length along the arms.
        val crossR = crossRadius(a) * radius
        // Circle radius is constant.
        val circleR = radius
        // Heart radius at this angle (polar heart curve).
        val heartR = heartRadius(a) * radius

        // Stage 1: cross → circle
        val rCrossCircle = lerp(crossR, circleR, smooth(crossToSphere))
        // Stage 2: circle → heart
        val rr = lerp(rCrossCircle, heartR, smooth(sphereToHeart))

        // Apply rotation
        val ar = a + rot
        val x = cx + rr * cos(ar)
        val y = cy + rr * sin(ar)
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    return path
}

/** Smooth cross/plus profile in polar form, normalized ~0..1.2. */
private fun crossRadius(angle: Float): Float {
    // Four arms at 0, 90, 180, 270 degrees. Use max of two rotated "bar" profiles.
    val armWidth = 0.32f
    fun bar(a: Float): Float {
        val c = cos(a)
        val s = sin(a)
        // distance to a rounded bar of half-width armWidth and length 1
        val ax = kotlin.math.abs(c)
        val ay = kotlin.math.abs(s)
        // along-arm reach vs across-arm reach
        return if (ay <= armWidth) 1f else armWidth / ay
    }
    val v = bar(angle)
    val h = bar(angle - PI.toFloat() / 2f)
    return maxOf(v, h).coerceAtMost(1.15f)
}

/** Polar heart curve, normalized so it roughly fits radius 1. Pointed end down. */
private fun heartRadius(angle: Float): Float {
    // Classic polar heart: r = sin(t)*sqrt(|cos(t)|)/(sin(t)+1.4) variants are
    // fiddly; use the well-known 2 - 2sin + (sin*sqrt|cos|)/(sin+1.4) shape,
    // oriented so the cusp points up and tip points down by offsetting angle.
    val t = angle + PI.toFloat() / 2f   // rotate so heart sits upright
    val sinT = sin(t)
    val cosT = cos(t)
    val r = 2f - 2f * sinT + (sinT * kotlin.math.sqrt(kotlin.math.abs(cosT))) / (sinT + 1.4f)
    return (r / 3.2f).coerceIn(0f, 1.2f)
}

private fun lerp(a: Float, b: Float, f: Float) = a + (b - a) * f
private fun smooth(x: Float): Float = x * x * (3f - 2f * x)  // smoothstep
