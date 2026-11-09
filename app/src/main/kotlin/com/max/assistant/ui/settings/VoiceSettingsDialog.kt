package com.max.assistant.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.max.assistant.tts.VoiceLanguage
import com.max.assistant.tts.VoiceSettings

@Composable
fun VoiceSettingsDialog(
    settings: VoiceSettings,
    isMuted: Boolean,
    isSpeaking: Boolean,
    onDismiss: () -> Unit,
    onInputLanguageChange: (VoiceLanguage) -> Unit,
    onOutputLanguageChange: (VoiceLanguage) -> Unit,
    onPitchChange: (Float) -> Unit,
    onSpeedChange: (Float) -> Unit,
    onTestVoice: () -> Unit,
    onStopSpeaking: () -> Unit,
    onToggleMute: () -> Unit,
    onToggleShortWakeWord: (Boolean) -> Unit = {},
    onToggleMuteListeningBeep: (Boolean) -> Unit = {}
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF0C1420),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF38B6FF)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Voice Settings",
                            tint = Color(0xFF00F0FF),
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "VOICE SETTINGS",
                            color = Color(0xFF00F0FF),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color(0xFF88AACC)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Status info
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF131D2D))
                        .border(1.dp, Color(0xFF1E2E44), RoundedCornerShape(8.dp))
                        .padding(10.dp)
                ) {
                    Column {
                        Text(
                            text = "ENGINE TELEMETRY",
                            color = Color(0xFF6B8AFF),
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = settings.engineStatusMessage,
                            color = Color(0xFFCCEEFF),
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "Active Profile: ${settings.currentVoiceName}",
                            color = Color(0xFF88AACC),
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Input Language Selector
                SectionHeader("INPUT SPEECH RECOGNITION")
                LanguagePillSelector(
                    selected = settings.inputLanguage,
                    onSelected = onInputLanguageChange
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Output Language Selector
                SectionHeader("OUTPUT VOICE SYNTHESIS")
                LanguagePillSelector(
                    selected = settings.outputLanguage,
                    onSelected = onOutputLanguageChange
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Pitch Control
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "VOICE PITCH (DEEP MALE)",
                        color = Color(0xFF88AACC),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "${String.format(java.util.Locale.US, "%.2f", settings.pitch)}x",
                        color = Color(0xFF00F0FF),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Slider(
                    value = settings.pitch,
                    onValueChange = onPitchChange,
                    valueRange = 0.50f..1.30f,
                    steps = 15,
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFF00F0FF),
                        activeTrackColor = Color(0xFF00F0FF),
                        inactiveTrackColor = Color(0xFF1E2E44)
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Speed Control
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SPEECH SPEED (CALM CADENCE)",
                        color = Color(0xFF88AACC),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "${String.format(java.util.Locale.US, "%.2f", settings.speed)}x",
                        color = Color(0xFF00F0FF),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Slider(
                    value = settings.speed,
                    onValueChange = onSpeedChange,
                    valueRange = 0.50f..1.30f,
                    steps = 15,
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFF00F0FF),
                        activeTrackColor = Color(0xFF00F0FF),
                        inactiveTrackColor = Color(0xFF1E2E44)
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Wake Word & Continuous Listening Toggles (Module 5 & 10)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "WAKE WORD (HEY MAX)",
                            color = Color(0xFFCCEEFF),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "Foreground service hotword detection",
                            color = Color(0xFF6B8AFF),
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Switch(
                        checked = settings.wakeWordEnabled,
                        onCheckedChange = { /* state handled in settings */ },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFF00F0FF),
                            checkedTrackColor = Color(0xFF0055AA)
                        )
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Short Wake Word Toggle ("MAX")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                        Text(
                            text = "PREFER SHORT WAKE WORD ('MAX')",
                            color = Color(0xFFCCEEFF),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "Trigger on 'MAX' and common phonetic matches ('Mac', 'Mex', 'Maks', 'मैक्स')",
                            color = Color(0xFF6B8AFF),
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Switch(
                        checked = settings.allowShortWakeWord,
                        onCheckedChange = onToggleShortWakeWord,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFF00F0FF),
                            checkedTrackColor = Color(0xFF0055AA)
                        )
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Mute Listening Beep Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                        Text(
                            text = "MUTE LISTENING BEEP (SILENT)",
                            color = Color(0xFFCCEEFF),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "Silences the annoying system microphone beep. Subtle haptic feedback is used instead.",
                            color = Color(0xFF6B8AFF),
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Switch(
                        checked = settings.muteListeningBeep,
                        onCheckedChange = onToggleMuteListeningBeep,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFF00F0FF),
                            checkedTrackColor = Color(0xFF0055AA)
                        )
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Gemini Cloud AI Configuration
                val context = androidx.compose.ui.platform.LocalContext.current
                val geminiProvider = remember { com.max.assistant.ai.GeminiAiProvider(context) }
                var apiKeyInput by remember { mutableStateOf(geminiProvider.getApiKey()) }
                var keySavedNotice by remember { mutableStateOf(false) }

                SectionHeader("GEMINI CLOUD AI CONFIGURATION")
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF131D2D))
                        .border(1.dp, Color(0xFF1E2E44), RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "MODEL: gemini-3.5-flash-lite",
                            color = Color(0xFF00F0FF),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFF00E676).copy(alpha = 0.15f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "ONLINE",
                                color = Color(0xFF00E676),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = apiKeyInput,
                        onValueChange = { 
                            apiKeyInput = it
                            keySavedNotice = false
                        },
                        label = { Text("Gemini API Key", fontSize = 10.sp, color = Color(0xFF88AACC)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = androidx.compose.ui.text.TextStyle(
                            color = Color.White,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF00F0FF),
                            unfocusedBorderColor = Color(0xFF1E2E44),
                            focusedContainerColor = Color(0xFF0C1420),
                            unfocusedContainerColor = Color(0xFF0C1420)
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (keySavedNotice) {
                            Text(
                                text = "✓ Key Saved & Active",
                                color = Color(0xFF00E676),
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        } else {
                            Text(
                                text = "Official API key configured",
                                color = Color(0xFF6B8AFF),
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Button(
                            onClick = {
                                geminiProvider.saveApiKey(apiKeyInput)
                                keySavedNotice = true
                            },
                            shape = RoundedCornerShape(6.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0066CC)),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("SAVE KEY", fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Test Voice & Stop Speaking Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onTestVoice,
                        modifier = Modifier.weight(1f).height(46.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0066CC))
                    ) {
                        Icon(imageVector = Icons.Default.VolumeUp, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("TEST VOICE", fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }

                    OutlinedButton(
                        onClick = onStopSpeaking,
                        enabled = isSpeaking,
                        modifier = Modifier.weight(1f).height(46.dp),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isSpeaking) Color(0xFFFF4444) else Color(0xFF445566)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = if (isSpeaking) Color(0xFFFF4444) else Color(0xFF667788)
                        )
                    ) {
                        Icon(imageVector = Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("STOP TALK", fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Mute / Unmute Button in Settings
                OutlinedButton(
                    onClick = onToggleMute,
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isMuted) Color(0xFFFF9900) else Color(0xFF38B6FF)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (isMuted) Color(0xFFFF9900).copy(alpha = 0.15f) else Color(0xFF0F1D33),
                        contentColor = if (isMuted) Color(0xFFFF9900) else Color(0xFF38B6FF)
                    )
                ) {
                    Icon(
                        imageVector = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isMuted) "UNMUTE MICROPHONE & VOICE" else "MUTE MICROPHONE & VOICE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        color = Color(0xFF88AACC),
        fontSize = 10.sp,
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
    )
}

@Composable
private fun LanguagePillSelector(
    selected: VoiceLanguage,
    onSelected: (VoiceLanguage) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        VoiceLanguage.values().forEach { lang ->
            val isSelected = selected == lang
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isSelected) Color(0xFF0055AA) else Color(0xFF131D2D))
                    .border(
                        1.dp,
                        if (isSelected) Color(0xFF00F0FF) else Color(0xFF1E2E44),
                        RoundedCornerShape(8.dp)
                    )
                    .clickable { onSelected(lang) }
                    .padding(vertical = 8.dp, horizontal = 2.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = when (lang) {
                        VoiceLanguage.AUTO -> "AUTO"
                        VoiceLanguage.HINGLISH -> "HING"
                        VoiceLanguage.ENGLISH -> "ENG"
                        VoiceLanguage.HINDI -> "हिंदी"
                        VoiceLanguage.BENGALI -> "বাংলা"
                    },
                    color = if (isSelected) Color.White else Color(0xFF88AACC),
                    fontSize = 10.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    fontFamily = FontFamily.Monospace,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
