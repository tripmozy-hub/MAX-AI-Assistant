package com.max.assistant.ui.dashboard

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.max.assistant.speech.AssistantState
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Modern, clean, and smooth ambient aurora background for the Galaxy S25 Ultra AMOLED display.
 */
@Composable
fun ModernAmbientBackground(
    state: AssistantState,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "ambient_anim")

    val drift1 by infiniteTransition.animateFloat(
        initialValue = -30f,
        targetValue = 30f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "drift1"
    )
    val drift2 by infiniteTransition.animateFloat(
        initialValue = 20f,
        targetValue = -20f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "drift2"
    )

    val (glowColor1, glowColor2) = when (state) {
        AssistantState.STOPPED -> Pair(Color(0xFF1E2430), Color(0xFF11141A))
        AssistantState.MUTED -> Pair(Color(0xFF3D260D), Color(0xFF241505))
        AssistantState.LISTENING_FOR_COMMAND -> Pair(Color(0xFF063A4D), Color(0xFF031E33))
        AssistantState.THINKING, AssistantState.PROCESSING -> Pair(Color(0xFF2E1052), Color(0xFF160938))
        AssistantState.SPEAKING -> Pair(Color(0xFF0A3C59), Color(0xFF032540))
        AssistantState.AWAITING_PIN -> Pair(Color(0xFF38290B), Color(0xFF1C1303))
        else -> Pair(Color(0xFF0C273D), Color(0xFF081829))
    }

    val animatedGlow1 by animateColorAsState(
        targetValue = glowColor1,
        animationSpec = tween(800, easing = FastOutSlowInEasing),
        label = "glow1"
    )
    val animatedGlow2 by animateColorAsState(
        targetValue = glowColor2,
        animationSpec = tween(800, easing = FastOutSlowInEasing),
        label = "glow2"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        // Base deep dark space
        drawRect(color = Color(0xFF07090E))

        // Center primary aurora bloom
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    animatedGlow1.copy(alpha = 0.55f),
                    animatedGlow1.copy(alpha = 0.25f),
                    Color.Transparent
                ),
                center = Offset(size.width * 0.5f + drift1, size.height * 0.42f + drift2),
                radius = size.width * 0.85f
            ),
            center = Offset(size.width * 0.5f + drift1, size.height * 0.42f + drift2),
            radius = size.width * 0.85f
        )

        // Top secondary soft accent bloom
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    animatedGlow2.copy(alpha = 0.45f),
                    Color.Transparent
                ),
                center = Offset(size.width * 0.25f - drift2, size.height * 0.15f),
                radius = size.width * 0.65f
            ),
            center = Offset(size.width * 0.25f - drift2, size.height * 0.15f),
            radius = size.width * 0.65f
        )
    }
}

/**
 * Modern, simple, attractive, and silky-smooth AI orb.
 * Reacts dynamically to speech audio level (RMS) and assistant state with organic fluid breathing.
 */
@Composable
fun ModernFluidAiOrb(
    modifier: Modifier = Modifier,
    rmsLevel: Float,
    state: AssistantState
) {
    val infiniteTransition = rememberInfiniteTransition(label = "fluid_orb_anim")

    // Slow organic breathing pulse
    val breathingPulse by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orb_breath"
    )

    // Smooth continuous aura rotation
    val auraRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (state) {
                    AssistantState.THINKING, AssistantState.PROCESSING -> 3000
                    AssistantState.SPEAKING -> 4500
                    AssistantState.LISTENING_FOR_COMMAND -> 6000
                    else -> 12000
                },
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "aura_rot"
    )

    // Secondary ripple phase for dynamic wavelets
    val ripplePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ripple_phase"
    )

    // Smoothly animated target colors for the current state
    val (primaryColor, accentColor, coreColor) = when (state) {
        AssistantState.STOPPED -> Triple(Color(0xFF5A6677), Color(0xFF384352), Color(0xFF161B22))
        AssistantState.MUTED -> Triple(Color(0xFFFF9900), Color(0xFFFFB347), Color(0xFF2A1904))
        AssistantState.LISTENING_FOR_COMMAND -> Triple(Color(0xFF00E5FF), Color(0xFF00B0FF), Color(0xFF052B3B))
        AssistantState.THINKING, AssistantState.PROCESSING -> Triple(Color(0xFFBD00FF), Color(0xFF00E5FF), Color(0xFF240638))
        AssistantState.SPEAKING -> Triple(Color(0xFF00E5FF), Color(0xFF76FF03), Color(0xFF042D3B))
        AssistantState.AWAITING_PIN -> Triple(Color(0xFFFFB74D), Color(0xFFFF9800), Color(0xFF2A1B05))
        else -> Triple(Color(0xFF00E5FF), Color(0xFF2979FF), Color(0xFF052233))
    }

    val animatedPrimary by animateColorAsState(
        targetValue = primaryColor,
        animationSpec = tween(500, easing = FastOutSlowInEasing),
        label = "primary_col"
    )
    val animatedAccent by animateColorAsState(
        targetValue = accentColor,
        animationSpec = tween(500, easing = FastOutSlowInEasing),
        label = "accent_col"
    )
    val animatedCore by animateColorAsState(
        targetValue = coreColor,
        animationSpec = tween(500, easing = FastOutSlowInEasing),
        label = "core_col"
    )

    // Smooth RMS volume dampener
    val smoothRms by animateFloatAsState(
        targetValue = rmsLevel.coerceIn(0f, 1f),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "smooth_rms"
    )

    Box(
        modifier = modifier.size(280.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val baseRadius = (size.minDimension / 2f) * 0.48f
            val dynamicScale = breathingPulse + (smoothRms * 0.22f)
            val currentRadius = baseRadius * dynamicScale

            // 1. Soft Ambient Outer Halo
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        animatedPrimary.copy(alpha = if (state == AssistantState.STOPPED) 0.08f else 0.32f + (smoothRms * 0.3f)),
                        animatedAccent.copy(alpha = 0.12f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = currentRadius * 2.1f
                ),
                center = center,
                radius = currentRadius * 2.1f
            )

            // 2. Dynamic Expanding Soundwave Ripple Rings (Active during listening/speaking)
            if (state == AssistantState.LISTENING_FOR_COMMAND || state == AssistantState.SPEAKING || smoothRms > 0.05f) {
                for (i in 0..2) {
                    val ringOffset = (ripplePhase + i * 0.33f) % 1f
                    val ringRadius = currentRadius * (1.1f + ringOffset * 0.85f)
                    val ringAlpha = (1f - ringOffset) * (0.35f + smoothRms * 0.45f)
                    drawCircle(
                        color = animatedPrimary.copy(alpha = ringAlpha.coerceIn(0f, 1f)),
                        radius = ringRadius,
                        center = center,
                        style = Stroke(width = (1.5f + (1f - ringOffset) * 2f).dp.toPx())
                    )
                }
            }

            // 3. Fluid Rotating Iridescent Aura Arcs
            rotate(degrees = auraRotation, pivot = center) {
                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            animatedPrimary.copy(alpha = 0.85f),
                            animatedAccent.copy(alpha = 0.95f),
                            Color.White.copy(alpha = 0.9f),
                            animatedPrimary.copy(alpha = 0.2f),
                            animatedPrimary.copy(alpha = 0.85f)
                        ),
                        center = center
                    ),
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = Offset(center.x - currentRadius * 1.08f, center.y - currentRadius * 1.08f),
                    size = Size(currentRadius * 2.16f, currentRadius * 2.16f),
                    style = Stroke(
                        width = (3.5f + smoothRms * 3f).dp.toPx(),
                        cap = StrokeCap.Round
                    )
                )
            }

            // Counter-rotating accent ring for high-end depth
            rotate(degrees = -auraRotation * 0.6f, pivot = center) {
                drawArc(
                    color = animatedAccent.copy(alpha = 0.55f),
                    startAngle = 45f,
                    sweepAngle = 120f,
                    useCenter = false,
                    topLeft = Offset(center.x - currentRadius * 1.18f, center.y - currentRadius * 1.18f),
                    size = Size(currentRadius * 2.36f, currentRadius * 2.36f),
                    style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round)
                )
                drawArc(
                    color = Color.White.copy(alpha = 0.7f),
                    startAngle = 225f,
                    sweepAngle = 70f,
                    useCenter = false,
                    topLeft = Offset(center.x - currentRadius * 1.18f, center.y - currentRadius * 1.18f),
                    size = Size(currentRadius * 2.36f, currentRadius * 2.36f),
                    style = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            // 4. Glossy Organic Orb Body (Layered Radial Gradients)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.25f),
                        animatedCore.copy(alpha = 0.95f),
                        animatedPrimary.copy(alpha = 0.7f)
                    ),
                    center = Offset(center.x - currentRadius * 0.25f, center.y - currentRadius * 0.25f),
                    radius = currentRadius
                ),
                radius = currentRadius,
                center = center
            )

            // Inner glass rim highlight
            drawCircle(
                color = Color.White.copy(alpha = 0.2f),
                radius = currentRadius - 2.dp.toPx(),
                center = center,
                style = Stroke(width = 1.dp.toPx())
            )

            // 5. Center Pulsing Luminous Core
            val innerCoreRadius = currentRadius * 0.45f * (0.9f + smoothRms * 0.3f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White,
                        animatedPrimary.copy(alpha = 0.9f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = innerCoreRadius
                ),
                radius = innerCoreRadius,
                center = center
            )
        }

        // Modern, clean, and elegant "MAX" title
        Text(
            text = "MAX",
            color = Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.SansSerif,
            letterSpacing = 4.sp
        )
    }
}

/**
 * Coordinate grid background matching the AMOLED HUD aesthetic.
 */
@Composable
fun SciFiGridBackground(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val gridSpacing = 42.dp.toPx()
        val gridColor = Color(0xFF221A0F).copy(alpha = 0.5f)
        val tickColor = Color(0xFF5A4326).copy(alpha = 0.7f)

        // Vertical grid lines
        var x = 0f
        while (x <= size.width) {
            drawLine(
                color = gridColor,
                start = Offset(x, 0f),
                end = Offset(x, size.height),
                strokeWidth = 1f
            )
            // Top ruler tick marks
            drawLine(
                color = tickColor,
                start = Offset(x, 0f),
                end = Offset(x, 14f),
                strokeWidth = 1.5f
            )
            x += gridSpacing
        }

        // Horizontal grid lines
        var y = 0f
        while (y <= size.height) {
            drawLine(
                color = gridColor,
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 1f
            )
            y += gridSpacing
        }

        // Top sci-fi bracket line: ┌───\______/───┐
        val topBracketY = 32.dp.toPx()
        val midX = size.width / 2f
        val trapWidth = 90.dp.toPx()

        val bracketColor = Color(0xFF7A5826)
        drawLine(
            color = bracketColor,
            start = Offset(midX - trapWidth - 80f, topBracketY),
            end = Offset(midX - trapWidth, topBracketY),
            strokeWidth = 1.8f
        )
        drawLine(
            color = bracketColor,
            start = Offset(midX - trapWidth, topBracketY),
            end = Offset(midX - trapWidth + 24f, topBracketY + 14f),
            strokeWidth = 1.8f
        )
        drawLine(
            color = bracketColor,
            start = Offset(midX - trapWidth + 24f, topBracketY + 14f),
            end = Offset(midX + trapWidth - 24f, topBracketY + 14f),
            strokeWidth = 1.8f
        )
        drawLine(
            color = bracketColor,
            start = Offset(midX + trapWidth - 24f, topBracketY + 14f),
            end = Offset(midX + trapWidth, topBracketY),
            strokeWidth = 1.8f
        )
        drawLine(
            color = bracketColor,
            start = Offset(midX + trapWidth, topBracketY),
            end = Offset(midX + trapWidth + 80f, topBracketY),
            strokeWidth = 1.8f
        )
    }
}

private data class NeuralNode3D(
    val x: Float,
    val y: Float,
    val z: Float,
    val baseR: Float,
    val pulsePhase: Float
)

/**
 * Procedural 3D Golden Neural Sphere Visualizer.
 * Matches the user's uploaded reference image:
 * Luminous amber synaptic sphere with rotating orbital shell,
 * inclined equatorial celestial ring, crossing energetic diagonal streak,
 * and speech-reactive audio displacement.
 */
@Composable
fun HolographicCoreVisualizer(
    modifier: Modifier = Modifier,
    rmsLevel: Float,
    state: AssistantState
) {
    val infiniteTransition = rememberInfiniteTransition(label = "sphere_anim")

    // Continuous 3D rotation
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (state) {
                    AssistantState.SPEAKING -> 4000
                    AssistantState.LISTENING_FOR_COMMAND -> 6000
                    AssistantState.STOPPED, AssistantState.MUTED -> 20000
                    else -> 10000
                },
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    // Breathing pulse
    val breathingPulse by infiniteTransition.animateFloat(
        initialValue = 0.97f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    // Pre-generate stable 3D nodes on multiple concentric shells
    val nodes = remember {
        val list = mutableListOf<NeuralNode3D>()
        val rnd = Random(42)
        val count = 160
        for (i in 0 until count) {
            val u = rnd.nextFloat()
            val v = rnd.nextFloat()
            val theta = (u * 2.0 * Math.PI).toFloat()
            val phi = Math.acos((2.0 * v - 1.0).coerceIn(-1.0, 1.0)).toFloat()
            val shell = if (rnd.nextFloat() < 0.45f) 0.65f else if (rnd.nextFloat() < 0.75f) 0.95f else 1.20f
            val r = 110f * shell

            val nx = r * sin(phi) * cos(theta)
            val ny = r * sin(phi) * sin(theta)
            val nz = r * cos(phi)
            list.add(NeuralNode3D(nx, ny, nz, r, rnd.nextFloat() * 6.28f))
        }
        list
    }

    // Dynamic color palettes matching the amber neural sphere
    val (primaryGold, filamentGold, coreGlow) = when (state) {
        AssistantState.STOPPED -> Triple(Color(0xFF6E563B), Color(0xFF4A3A27), Color(0xFF332616))
        AssistantState.MUTED -> Triple(Color(0xFFFF9900), Color(0xFFCC7000), Color(0xFF663300))
        AssistantState.LISTENING_FOR_COMMAND -> Triple(Color(0xFFFFB347), Color(0xFFFFA000), Color(0xFFFF8F00))
        AssistantState.SPEAKING -> Triple(Color(0xFFFFE082), Color(0xFFFFB74D), Color(0xFFFF9100))
        AssistantState.AWAITING_PIN -> Triple(Color(0xFFFFCC00), Color(0xFFFF9900), Color(0xFF664400))
        else -> Triple(Color(0xFFFFA726), Color(0xFFFF8F00), Color(0xFF553300))
    }

    Box(
        modifier = modifier.size(290.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val activeBoost = rmsLevel * 22f
            val pulse = breathingPulse + (rmsLevel * 0.15f)

            // 1. Soft radial back-glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        filamentGold.copy(alpha = if (state == AssistantState.STOPPED) 0.12f else 0.42f + (rmsLevel * 0.3f)),
                        Color.Transparent
                    ),
                    center = center,
                    radius = (size.minDimension / 2f) * pulse
                ),
                center = center,
                radius = (size.minDimension / 2f) * pulse
            )

            // 2. Project 3D nodes onto 2D plane with rotation
            val rotYRad = Math.toRadians(rotationAngle.toDouble()).toFloat()
            val rotXRad = 0.28f // static tilt
            val cosY = cos(rotYRad)
            val sinY = sin(rotYRad)
            val cosX = cos(rotXRad)
            val sinX = sin(rotXRad)

            data class ProjectedPt(val x: Float, val y: Float, val z: Float, val alpha: Float, val size: Float)
            val projected = nodes.map { node ->
                val currentR = (node.baseR + activeBoost) * pulse
                val origLen = sqrt(node.x * node.x + node.y * node.y + node.z * node.z).coerceAtLeast(1f)
                val nx = (node.x / origLen) * currentR
                val ny = (node.y / origLen) * currentR
                val nz = (node.z / origLen) * currentR

                // Rotate Y
                val x1 = nx * cosY - nz * sinY
                val z1 = nx * sinY + nz * cosY

                // Rotate X
                val y2 = ny * cosX - z1 * sinX
                val z2 = ny * sinX + z1 * cosX

                val fov = 340f
                val scale = fov / (fov + z2 + 100f)
                val alpha = ((z2 + 120f) / 240f).coerceIn(0.15f, 0.95f)
                ProjectedPt(
                    x = center.x + x1 * scale,
                    y = center.y + y2 * scale,
                    z = z2,
                    alpha = alpha,
                    size = (scale * 2.2f) + (rmsLevel * 2.0f)
                )
            }.sortedBy { it.z }

            // 3. Draw neural filaments connecting nearby projected points
            val filamentMaxDistSq = 1400f * (1f + rmsLevel * 0.8f)
            for (i in projected.indices step 2) {
                val p1 = projected[i]
                for (j in (i + 1) until minOf(i + 6, projected.size)) {
                    val p2 = projected[j]
                    val dx = p1.x - p2.x
                    val dy = p1.y - p2.y
                    val distSq = dx * dx + dy * dy
                    if (distSq < filamentMaxDistSq) {
                        val lineAlpha = (1f - distSq / filamentMaxDistSq) * minOf(p1.alpha, p2.alpha) * (0.45f + rmsLevel * 0.45f)
                        drawLine(
                            color = filamentGold.copy(alpha = lineAlpha.coerceIn(0f, 1f)),
                            start = Offset(p1.x, p1.y),
                            end = Offset(p2.x, p2.y),
                            strokeWidth = 1.0f + (rmsLevel * 1.2f)
                        )
                    }
                }
            }

            // 4. Draw 3D neural nodes/particles
            for (p in projected) {
                drawCircle(
                    color = if (p.z > 20f) Color(0xFFFFF0D0).copy(alpha = p.alpha) else primaryGold.copy(alpha = p.alpha),
                    radius = p.size.coerceAtLeast(1.0f),
                    center = Offset(p.x, p.y)
                )
            }

            // 5. Inclined Saturn-like Celestial Equatorial Ring (Matching Image)
            rotate(degrees = -10f, pivot = center) {
                val ringRadiusX = 118f * pulse
                val ringRadiusY = 38f * pulse

                // Main glowing ring
                drawOval(
                    color = primaryGold.copy(alpha = 0.85f + (rmsLevel * 0.15f)),
                    topLeft = Offset(center.x - ringRadiusX, center.y - ringRadiusY),
                    size = Size(ringRadiusX * 2, ringRadiusY * 2),
                    style = Stroke(width = 3.2f + (rmsLevel * 2.5f))
                )

                // Secondary dashed orbit ring
                val outerRadiusX = 132f * pulse
                val outerRadiusY = 44f * pulse
                drawOval(
                    color = filamentGold.copy(alpha = 0.5f),
                    topLeft = Offset(center.x - outerRadiusX, center.y - outerRadiusY),
                    size = Size(outerRadiusX * 2, outerRadiusY * 2),
                    style = Stroke(width = 1.2f)
                )
            }

            // 6. Bright Crossing Diagonal Streak Line (Matching Image top-left to bottom-right)
            val streakLength = 105f * pulse
            val streakAngleRad = Math.toRadians(-58.0).toFloat()
            val dxStreak = cos(streakAngleRad) * streakLength
            val dyStreak = sin(streakAngleRad) * streakLength
            drawLine(
                color = Color(0xFFFFF7E6).copy(alpha = 0.9f),
                start = Offset(center.x - dxStreak, center.y - dyStreak),
                end = Offset(center.x + dxStreak, center.y + dyStreak),
                strokeWidth = 2.6f + (rmsLevel * 2.0f),
                cap = StrokeCap.Round
            )

            // 7. Dense Glowing Synaptic Core
            val coreRadius = 38f * pulse + (rmsLevel * 10f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFFFF5DC),
                        primaryGold.copy(alpha = 0.9f),
                        filamentGold.copy(alpha = 0.4f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = coreRadius
                ),
                center = center,
                radius = coreRadius
            )
        }

        // Central "MAX" title in clean, elegant typography within the core
        Text(
            text = "MAX",
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.SansSerif,
            letterSpacing = 4.sp
        )
    }
}

/**
 * Procedural JARVIS HUD Holographic Reactor Visualizer.
 * Matches the user's uploaded reference image of the monitor:
 * Glowing cyan/blue neon ring, compass tick ring, rotating orbital arcs,
 * inner dark glass disc, and centered futuristic "MAX" typography.
 */
@Composable
fun JarvisHudVisualizer(
    modifier: Modifier = Modifier,
    rmsLevel: Float,
    state: AssistantState
) {
    val infiniteTransition = rememberInfiniteTransition(label = "jarvis_anim")

    val outerRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(18000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "outer_rot"
    )

    val innerRotation by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "inner_rot"
    )

    val breathingPulse by infiniteTransition.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "jarvis_pulse"
    )

    val (cyanPrimary, cyanAccent, coreBg) = when (state) {
        AssistantState.STOPPED -> Triple(Color(0xFF556677), Color(0xFF334455), Color(0xFF0C1014))
        AssistantState.MUTED -> Triple(Color(0xFFFF9900), Color(0xFFCC7000), Color(0xFF140C04))
        AssistantState.LISTENING_FOR_COMMAND -> Triple(Color(0xFF00FFFF), Color(0xFF00B4D8), Color(0xFF041824))
        AssistantState.SPEAKING -> Triple(Color(0xFFE0F7FA), Color(0xFF00E5FF), Color(0xFF052236))
        AssistantState.AWAITING_PIN -> Triple(Color(0xFFFFCC00), Color(0xFFFF9900), Color(0xFF161104))
        else -> Triple(Color(0xFF00E5FF), Color(0xFF0090FF), Color(0xFF030D17))
    }

    Box(
        modifier = modifier.size(290.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val pulse = breathingPulse + (rmsLevel * 0.12f)
            val baseRadius = (size.minDimension / 2f) * 0.88f * pulse

            // 1. Soft Outer Cyan Bloom
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        cyanPrimary.copy(alpha = if (state == AssistantState.STOPPED) 0.1f else 0.35f + (rmsLevel * 0.25f)),
                        cyanAccent.copy(alpha = 0.1f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = baseRadius * 1.15f
                ),
                center = center,
                radius = baseRadius * 1.15f
            )

            // 2. Fine Outer Circular Ring
            drawCircle(
                color = cyanPrimary.copy(alpha = 0.3f),
                radius = baseRadius,
                center = center,
                style = Stroke(width = 1.2f)
            )

            // 3. 360-degree Radial Compass Ticks (Exact JARVIS HUD detail)
            rotate(degrees = outerRotation * 0.2f, pivot = center) {
                val tickOuter = baseRadius
                for (deg in 0 until 360 step 10) {
                    val rad = Math.toRadians(deg.toDouble()).toFloat()
                    val isMajor = deg % 30 == 0
                    val tickInner = tickOuter - if (isMajor) 10.dp.toPx() else 6.dp.toPx()
                    val x1 = center.x + tickOuter * cos(rad)
                    val y1 = center.y + tickOuter * sin(rad)
                    val x2 = center.x + tickInner * cos(rad)
                    val y2 = center.y + tickInner * sin(rad)

                    drawLine(
                        color = cyanPrimary.copy(alpha = if (isMajor) 0.8f else 0.4f),
                        start = Offset(x1, y1),
                        end = Offset(x2, y2),
                        strokeWidth = if (isMajor) 2f else 1.2f
                    )
                }
            }

            // 4. Rotating Outer Orbital Arcs (Matching JARVIS monitor photo)
            rotate(degrees = outerRotation, pivot = center) {
                drawArc(
                    color = Color.White.copy(alpha = 0.9f),
                    startAngle = 10f,
                    sweepAngle = 55f,
                    useCenter = false,
                    topLeft = Offset(center.x - baseRadius, center.y - baseRadius),
                    size = Size(baseRadius * 2, baseRadius * 2),
                    style = Stroke(width = 2.8f, cap = StrokeCap.Round)
                )
            }

            rotate(degrees = innerRotation, pivot = center) {
                val arcRadius = baseRadius * 0.92f
                drawArc(
                    color = cyanPrimary.copy(alpha = 0.85f),
                    startAngle = 180f,
                    sweepAngle = 40f,
                    useCenter = false,
                    topLeft = Offset(center.x - arcRadius, center.y - arcRadius),
                    size = Size(arcRadius * 2, arcRadius * 2),
                    style = Stroke(width = 3.2f, cap = StrokeCap.Round)
                )
                drawArc(
                    color = cyanAccent.copy(alpha = 0.7f),
                    startAngle = 320f,
                    sweepAngle = 35f,
                    useCenter = false,
                    topLeft = Offset(center.x - arcRadius, center.y - arcRadius),
                    size = Size(arcRadius * 2, arcRadius * 2),
                    style = Stroke(width = 2f, cap = StrokeCap.Round)
                )
            }

            // 5. Middle Glowing Neon Ring (The signature glowing ring from photo)
            val middleRingRadius = baseRadius * 0.58f
            drawCircle(
                color = cyanPrimary.copy(alpha = 0.95f),
                radius = middleRingRadius,
                center = center,
                style = Stroke(width = 3.6f + (rmsLevel * 2.5f))
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.7f),
                radius = middleRingRadius - 2.dp.toPx(),
                center = center,
                style = Stroke(width = 1.2f)
            )

            // 6. Inner Dark Core Disc
            val innerDiscRadius = middleRingRadius - 5.dp.toPx()
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        coreBg.copy(alpha = 0.95f),
                        coreBg.copy(alpha = 0.98f),
                        cyanPrimary.copy(alpha = 0.75f)
                    ),
                    center = center,
                    radius = innerDiscRadius
                ),
                radius = innerDiscRadius,
                center = center
            )
            drawCircle(
                color = cyanPrimary.copy(alpha = 0.85f),
                radius = innerDiscRadius,
                center = center,
                style = Stroke(width = 1.8f)
            )
        }

        // 7. Central "MAX" title in clean, high-tech glowing typography
        Text(
            text = "MAX",
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.SansSerif,
            letterSpacing = 6.sp
        )
    }
}

/**
 * Cyber Silver 3D Helmet with Reactive Cybernetic Eyes.
 * Sculpted metallic titanium/silver armor plates, dual reactive optics,
 * dynamic pupil dilatation with audio RMS, gaze tracking, and AMOLED pure black backdrop.
 */
@Composable
fun CyberHelmetVisualizer(
    modifier: Modifier = Modifier,
    rmsLevel: Float,
    state: AssistantState
) {
    val infiniteTransition = rememberInfiniteTransition(label = "cyber_helmet_anim")

    // Breathing pulse for metallic plate glow
    val breathingPulse by infiniteTransition.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "helmet_breathe"
    )

    // Subtle gaze drift
    val gazeDriftX by infiniteTransition.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(4500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "gaze_x"
    )

    // Dynamic eye colors matching MAX assistant state
    val (eyePrimary, eyeGlow, visorBg) = when (state) {
        AssistantState.STOPPED -> Triple(Color(0xFF64748B), Color(0xFF334155), Color(0xFF0A0E14))
        AssistantState.MUTED -> Triple(Color(0xFFFF9900), Color(0xFFCC7000), Color(0xFF140D04))
        AssistantState.LISTENING_FOR_COMMAND -> Triple(Color(0xFF00F0FF), Color(0xFF0090FF), Color(0xFF031422))
        AssistantState.THINKING, AssistantState.PROCESSING -> Triple(Color(0xFFD946EF), Color(0xFF9333EA), Color(0xFF1A0B2E))
        AssistantState.SPEAKING -> Triple(Color(0xFF38BDF8), Color(0xFF0284C7), Color(0xFF051B2E))
        AssistantState.AWAITING_PIN -> Triple(Color(0xFFFACC15), Color(0xFFCA8A04), Color(0xFF191404))
        else -> Triple(Color(0xFF00E5FF), Color(0xFF0284C7), Color(0xFF04101D))
    }

    Box(
        modifier = modifier.size(300.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val pulse = breathingPulse + (rmsLevel * 0.14f)
            val hw = size.width * 0.44f * pulse
            val hh = size.height * 0.44f * pulse

            // 1. Cyber Ambient Atmosphere
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        eyeGlow.copy(alpha = if (state == AssistantState.STOPPED) 0.08f else 0.32f + (rmsLevel * 0.28f)),
                        Color.Transparent
                    ),
                    center = center,
                    radius = hw * 1.35f
                ),
                center = center,
                radius = hw * 1.35f
            )

            // 2. Sculpted Cyber-Silver Cranial Dome (Outer Helmet)
            val helmetTop = center.y - hh * 0.95f
            val helmetBottom = center.y + hh * 0.95f
            val helmetLeft = center.x - hw * 0.88f
            val helmetRight = center.x + hw * 0.88f

            val helmetPath = androidx.compose.ui.graphics.Path().apply {
                moveTo(center.x, helmetTop)
                // Crown to right temple
                cubicTo(
                    helmetRight * 0.85f, helmetTop + 10f,
                    helmetRight + 12f, center.y - hh * 0.3f,
                    helmetRight, center.y
                )
                // Right cheek to jaw
                lineTo(helmetRight - hw * 0.18f, center.y + hh * 0.55f)
                lineTo(center.x + hw * 0.28f, helmetBottom)
                // Chin center
                lineTo(center.x - hw * 0.28f, helmetBottom)
                // Left jaw to cheek
                lineTo(helmetLeft + hw * 0.18f, center.y + hh * 0.55f)
                lineTo(helmetLeft, center.y)
                // Left temple to crown
                cubicTo(
                    helmetLeft - 12f, center.y - hh * 0.3f,
                    helmetLeft * 0.85f, helmetTop + 10f,
                    center.x, helmetTop
                )
                close()
            }

            // Silver metallic gradient for shell
            val silverMetallic = Brush.linearGradient(
                colors = listOf(
                    Color(0xFFF1F5F9), // Highlight silver
                    Color(0xFFCBD5E1),
                    Color(0xFF94A3B8), // Metallic midtone
                    Color(0xFF475569),
                    Color(0xFF1E293B)  // Steel shadow
                ),
                start = Offset(helmetLeft, helmetTop),
                end = Offset(helmetRight, helmetBottom)
            )
            drawPath(path = helmetPath, brush = silverMetallic)
            drawPath(path = helmetPath, color = Color.White.copy(alpha = 0.5f), style = Stroke(width = 2.2f))

            // 3. Central Visor Plate (High-gloss dark tinted titanium visor)
            val visorTop = center.y - hh * 0.28f
            val visorBottom = center.y + hh * 0.28f
            val visorLeft = center.x - hw * 0.65f
            val visorRight = center.x + hw * 0.65f

            val visorPath = androidx.compose.ui.graphics.Path().apply {
                moveTo(center.x, visorTop - 8f)
                lineTo(visorRight, visorTop + 12f)
                lineTo(visorRight - hw * 0.12f, visorBottom - 8f)
                lineTo(center.x, visorBottom + 10f)
                lineTo(visorLeft + hw * 0.12f, visorBottom - 8f)
                lineTo(visorLeft, visorTop + 12f)
                close()
            }

            val visorGradient = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF030712),
                    visorBg,
                    Color(0xFF050B14)
                ),
                startY = visorTop,
                endY = visorBottom
            )
            drawPath(path = visorPath, brush = visorGradient)
            drawPath(path = visorPath, color = eyePrimary.copy(alpha = 0.75f), style = Stroke(width = 2.0f))

            // 4. Reactive Cybernetic Optics (Dual Glowing Eyes)
            val eyeSpacing = hw * 0.32f
            val eyeY = center.y - 2f
            val eyeWidth = 32.dp.toPx() * (1f + rmsLevel * 0.25f)
            val eyeHeight = (12.dp.toPx() + (rmsLevel * 14.dp.toPx())).coerceAtMost(28.dp.toPx())

            // Left Eye
            val leftEyeCenter = Offset(center.x - eyeSpacing + gazeDriftX, eyeY)
            drawOval(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White,
                        eyePrimary,
                        eyePrimary.copy(alpha = 0.4f),
                        Color.Transparent
                    ),
                    center = leftEyeCenter,
                    radius = eyeWidth
                ),
                topLeft = Offset(leftEyeCenter.x - eyeWidth / 2f, leftEyeCenter.y - eyeHeight / 2f),
                size = Size(eyeWidth, eyeHeight)
            )

            // Right Eye
            val rightEyeCenter = Offset(center.x + eyeSpacing + gazeDriftX, eyeY)
            drawOval(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White,
                        eyePrimary,
                        eyePrimary.copy(alpha = 0.4f),
                        Color.Transparent
                    ),
                    center = rightEyeCenter,
                    radius = eyeWidth
                ),
                topLeft = Offset(rightEyeCenter.x - eyeWidth / 2f, rightEyeCenter.y - eyeHeight / 2f),
                size = Size(eyeWidth, eyeHeight)
            )

            // 5. HUD Targeting Visor Reticles (Horizontal scanning line & cross-ticks)
            val scanLineAlpha = (0.4f + rmsLevel * 0.6f).coerceIn(0.2f, 1.0f)
            drawLine(
                color = eyePrimary.copy(alpha = scanLineAlpha),
                start = Offset(visorLeft + 10f, eyeY),
                end = Offset(visorRight - 10f, eyeY),
                strokeWidth = 1.4f
            )

            // Reticle brackets on eyes
            drawCircle(color = eyePrimary.copy(alpha = scanLineAlpha * 0.8f), radius = eyeWidth * 0.7f, center = leftEyeCenter, style = Stroke(width = 1.2f))
            drawCircle(color = eyePrimary.copy(alpha = scanLineAlpha * 0.8f), radius = eyeWidth * 0.7f, center = rightEyeCenter, style = Stroke(width = 1.2f))

            // 6. Audio-Reactive Mouth Equalizer Grille (Chin Vents)
            val ventY = center.y + hh * 0.58f
            val barCount = 7
            val barSpacing = 8.dp.toPx()
            val startX = center.x - (barCount / 2f * barSpacing)

            for (i in 0 until barCount) {
                val distFromCenter = kotlin.math.abs(i - barCount / 2)
                val barHeight = (4.dp.toPx() + (rmsLevel * 18.dp.toPx() * (1f - distFromCenter * 0.2f))).coerceAtLeast(3.dp.toPx())
                val bx = startX + (i * barSpacing)
                drawLine(
                    color = eyePrimary.copy(alpha = 0.85f),
                    start = Offset(bx, ventY - barHeight / 2f),
                    end = Offset(bx, ventY + barHeight / 2f),
                    strokeWidth = 3.5f,
                    cap = StrokeCap.Round
                )
            }
        }

        // Forehead Futuristic MAX Insignia
        Text(
            text = "MAX",
            color = Color(0xFFF1F5F9),
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.SansSerif,
            letterSpacing = 4.sp,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}
