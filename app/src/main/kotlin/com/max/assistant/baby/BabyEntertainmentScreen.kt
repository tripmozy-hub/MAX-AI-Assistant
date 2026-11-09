package com.max.assistant.baby

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.max.assistant.tts.VoiceLanguage

/**
 * Dedicated Baby Entertainment Mode Screen.
 * Displays cute animated cartoon characters, floating interactive bubbles,
 * trilingual lyrics/stories, and caregiver safety support.
 */
@Composable
fun BabyEntertainmentScreen(
    state: BabyModeState,
    currentLanguage: VoiceLanguage = VoiceLanguage.HINDI,
    onNextActivity: () -> Unit,
    onSelectLullaby: () -> Unit,
    onSelectRhymes: () -> Unit,
    onSelectAnimalSounds: () -> Unit,
    onSelectStory: () -> Unit,
    onPopBubble: (Int) -> Unit,
    onStopBabyMode: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "BabyAnimTransition")

    // Smooth hopping / floating animation for cartoon character
    val hopOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -24f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "hopOffset"
    )

    // Gentle wiggling / waddling rotation
    val wiggleRotation by infiniteTransition.animateFloat(
        initialValue = -7f,
        targetValue = 7f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 650, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "wiggleRotation"
    )

    // Breathing pulse scale
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0B132B), // Gentle Deep Midnight Navy
                        Color(0xFF1C2541),
                        Color(0xFF0F172A)
                    )
                )
            )
    ) {
        // Floating Interactive Bubbles Layer
        state.bubbles.forEach { bubble ->
            if (!bubble.isPopped) {
                val floatAnim by infiniteTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = -35f * bubble.speed,
                    animationSpec = infiniteRepeatable(
                        animation = tween(
                            durationMillis = (1800 / bubble.speed).toInt(),
                            easing = LinearEasing
                        ),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "bubbleFloat_${bubble.id}"
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                ) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .offset(
                                x = (bubble.xRatio * 320).dp,
                                y = ((bubble.yRatio * 580) + floatAnim).dp
                            )
                            .size(bubble.sizeDp.dp)
                            .clip(CircleShape)
                            .background(Color(bubble.colorHex))
                            .border(1.5.dp, Color.White.copy(alpha = 0.6f), CircleShape)
                            .clickable { onPopBubble(bubble.id) },
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize().padding(6.dp)) {
                            drawCircle(
                                color = Color.White.copy(alpha = 0.5f),
                                radius = size.minDimension / 5,
                                center = Offset(size.width * 0.35f, size.height * 0.35f)
                            )
                        }
                    }
                }
            }
        }

        // Main Content Column
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Bar: Title & Safety Controls
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Cute Baby Mode Badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF1E293B).copy(alpha = 0.9f))
                            .border(1.dp, Color(0xFFFFB5C5).copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "🍼 BABY ENTERTAINMENT",
                            color = Color(0xFFFFB5C5),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        )
                    }

                    // Stop Button
                    IconButton(
                        onClick = onStopBabyMode,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFF5252).copy(alpha = 0.2f))
                            .border(1.dp, Color(0xFFFF5252).copy(alpha = 0.6f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Stop Baby Mode",
                            tint = Color(0xFFFF8A80),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Safe Volume Indicator Pill
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF102A24).copy(alpha = 0.85f))
                        .border(1.dp, Color(0xFF00E676).copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.VolumeDown,
                        contentDescription = "Safe Volume",
                        tint = Color(0xFF00E676),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Safe Baby Volume Protected (35% Max)",
                        color = Color(0xFF00E676),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Center Stage: Animated Cartoon Character
            val char = state.currentContent?.character ?: BabyCharacter.BUNNY
            Box(
                modifier = Modifier
                    .size(180.dp)
                    .offset(y = hopOffset.dp)
                    .rotate(wiggleRotation)
                    .scale(pulseScale)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color(char.primaryColorHex).copy(alpha = 0.35f),
                                Color(char.primaryColorHex).copy(alpha = 0.08f),
                                Color.Transparent
                            )
                        )
                    )
                    .border(2.dp, Color(char.primaryColorHex).copy(alpha = 0.6f), CircleShape)
                    .clickable { onNextActivity() },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = char.emoji,
                        fontSize = 84.sp
                    )
                    Text(
                        text = char.displayName,
                        color = Color(char.primaryColorHex),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Content Lyrics / Story Card
            val content = state.currentContent
            if (content != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF131D31).copy(alpha = 0.92f)),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = Brush.horizontalGradient(
                            listOf(Color(0xFF80DEEA).copy(alpha = 0.3f), Color(0xFFFFB5C5).copy(alpha = 0.3f))
                        )
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Activity Badge
                        Text(
                            text = content.activityType.displayName.uppercase(),
                            color = Color(0xFF80DEEA),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        // Title
                        Text(
                            text = content.title,
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Rhyme / Lullaby Lyrics / Story
                        Text(
                            text = content.displayText,
                            color = Color(0xFFE2E8F0),
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            textAlign = TextAlign.Center,
                            fontFamily = FontFamily.SansSerif
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = content.interactiveHint,
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Caregiver Distress Guidance Alert (If crying trigger is active)
            val tip = state.caregiverTip
            if (!tip.isNullOrBlank()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF2E1C1D)),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = Brush.horizontalGradient(listOf(Color(0xFFFF8A80), Color(0xFFFFD180)))
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "💡 Caregiver Support",
                            color = Color(0xFFFFD180),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = tip,
                            color = Color(0xFFFFE0B2),
                            fontSize = 11.sp,
                            lineHeight = 16.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Interactive Activity Navigation Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ActivityPillButton("🌙 Lullaby", Color(0xFF80DEEA), onSelectLullaby)
                ActivityPillButton("🎵 Rhymes", Color(0xFFFFB5C5), onSelectRhymes)
                ActivityPillButton("🐶 Animals", Color(0xFFFFF176), onSelectAnimalSounds)
                ActivityPillButton("📖 Story", Color(0xFFB39DDB), onSelectStory)
                ActivityPillButton("🎲 Next", Color(0xFFA7FFEB), onNextActivity)
            }
        }
    }
}

@Composable
private fun ActivityPillButton(
    label: String,
    accentColor: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF1E293B).copy(alpha = 0.9f))
            .border(1.dp, accentColor.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 7.dp)
    ) {
        Text(
            text = label,
            color = accentColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
