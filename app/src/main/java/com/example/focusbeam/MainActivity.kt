package com.example.focusbeam  // <-- burayı kendi paket adınla aynı yap

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

// ------------------ MODE TANIMLARI ---------------------

enum class SessionType(
    val label: String,
    val color: Color
) {
    FOCUS("Odak", Color(0xFFFF6B9C)),
    SHORT_BREAK("Kısa Mola", Color(0xFF5DD6FF)),
    LONG_BREAK("Uzun Mola", Color(0xFFFFB86C))
}

// ------------------ ACTIVITY --------------------------

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                FocusBeamApp()
            }
        }
    }
}

// ------------------ ANA APP ---------------------------

@Composable
fun FocusBeamApp() {
    // Süreler (dakika)
    var focusMinutes by remember { mutableStateOf(45) }
    var shortMinutes by remember { mutableStateOf(10) }
    var longMinutes by remember { mutableStateOf(20) }

    var currentType by remember { mutableStateOf(SessionType.FOCUS) }

    fun currentMinutes(): Int = when (currentType) {
        SessionType.FOCUS -> focusMinutes
        SessionType.SHORT_BREAK -> shortMinutes
        SessionType.LONG_BREAK -> longMinutes
    }

    var secondsLeft by remember { mutableStateOf(currentMinutes() * 60) }
    var isRunning by remember { mutableStateOf(false) }
    var justFinished by remember { mutableStateOf(false) }

    // Günlük istatistik
    var completedFocusBlocks by remember { mutableStateOf(0) }
    var totalFocusMinutes by remember { mutableStateOf(0) }

    // Mod veya süre değişince timer reset
    LaunchedEffect(currentType, focusMinutes, shortMinutes, longMinutes) {
        if (!isRunning) {
            secondsLeft = currentMinutes() * 60
            justFinished = false
        }
    }

    // Timer mekanizması
    LaunchedEffect(isRunning, currentType) {
        if (!isRunning) return@LaunchedEffect
        justFinished = false
        while (isRunning && secondsLeft > 0) {
            delay(1000)
            secondsLeft -= 1
        }
        if (secondsLeft <= 0) {
            secondsLeft = 0
            isRunning = false
            justFinished = true
            if (currentType == SessionType.FOCUS) {
                completedFocusBlocks += 1
                totalFocusMinutes += currentMinutes()
            }
        }
    }

    val totalSeconds = currentMinutes() * 60
    val rawProgress =
        if (totalSeconds == 0) 0f else 1f - (secondsLeft / totalSeconds.toFloat())
    val animatedProgress by animateFloatAsState(
        targetValue = rawProgress.coerceIn(0f, 1f),
        label = "progress"
    )

    val minutes = secondsLeft / 60
    val secs = secondsLeft % 60
    val timeText = String.format("%02d:%02d", minutes, secs)

    // Yeni tema: turkuaz + pembe + şeftali
    val bgGradient = Brush.verticalGradient(
        listOf(
            Color(0xFF162641),
            Color(0xFF1D1635),
            Color(0xFF120821)
        )
    )

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .background(bgGradient),
        color = Color.Transparent
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(bgGradient)
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            // HEADER
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "FocusBeam",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Odak bloklarını kendine göre ayarla.",
                        fontSize = 14.sp,
                        color = Color(0xFFBFD5FF),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    Color(0xFF5DD6FF),
                                    Color(0xFFFF8AC6),
                                    Color(0xFFFFD08A)
                                )
                            )
                        )
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "Bugünkü odak",
                        fontSize = 10.sp,
                        color = Color(0xFF0E1018)
                    )
                    Text(
                        text = "${totalFocusMinutes} dk",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF0B0610)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // MOD SEÇİMİ
            SessionTypeRow(
                currentType = currentType,
                onTypeChange = { type ->
                    currentType = type
                }
            )

            Spacer(modifier = Modifier.height(14.dp))

            // SÜRE AYARLAMA SATIRI
            DurationRow(
                focusMinutes = focusMinutes,
                shortMinutes = shortMinutes,
                longMinutes = longMinutes,
                currentType = currentType,
                isRunning = isRunning,
                onChange = { type, delta ->
                    val newValue = when (type) {
                        SessionType.FOCUS -> (focusMinutes + delta).coerceIn(5, 180)
                        SessionType.SHORT_BREAK -> (shortMinutes + delta).coerceIn(3, 60)
                        SessionType.LONG_BREAK -> (longMinutes + delta).coerceIn(5, 90)
                    }
                    when (type) {
                        SessionType.FOCUS -> focusMinutes = newValue
                        SessionType.SHORT_BREAK -> shortMinutes = newValue
                        SessionType.LONG_BREAK -> longMinutes = newValue
                    }
                }
            )

            Spacer(modifier = Modifier.height(20.dp))

            // TIMER
            FocusTimerSection(
                currentType = currentType,
                timeText = timeText,
                isRunning = isRunning,
                justFinished = justFinished,
                progress = animatedProgress,
                onToggle = {
                    if (secondsLeft == 0) {
                        secondsLeft = currentMinutes() * 60
                        justFinished = false
                    }
                    isRunning = !isRunning
                },
                onReset = {
                    isRunning = false
                    secondsLeft = currentMinutes() * 60
                    justFinished = false
                }
            )

            Spacer(modifier = Modifier.height(18.dp))

            // STATS
            StatsRowForFocus(
                completedBlocks = completedFocusBlocks,
                totalMinutes = totalFocusMinutes
            )
        }
    }
}

// ------------------ COMPOSABLE PARÇALAR --------------------

@Composable
fun SessionTypeRow(
    currentType: SessionType,
    onTypeChange: (SessionType) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(999.dp))
            .background(Color(0xFF14172B))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        SessionType.values().forEach { type ->
            val selected = type == currentType
            val bg = if (selected) {
                Brush.horizontalGradient(
                    listOf(
                        type.color,
                        type.color.copy(alpha = 0.8f)
                    )
                )
            } else {
                Brush.horizontalGradient(
                    listOf(
                        Color(0xFF20233A),
                        Color(0xFF16172A)
                    )
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(bg)
            ) {
                TextButton(
                    onClick = { onTypeChange(type) },
                    modifier = Modifier.fillMaxSize(),
                    colors = ButtonDefaults.textButtonColors(
                        containerColor = Color.Transparent,
                        contentColor = if (selected) Color.White else Color(0xFFCBCFFF)
                    ),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(
                        text = type.label,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

@Composable
fun DurationRow(
    focusMinutes: Int,
    shortMinutes: Int,
    longMinutes: Int,
    currentType: SessionType,
    isRunning: Boolean,
    onChange: (SessionType, Int) -> Unit
) {
    val label = when (currentType) {
        SessionType.FOCUS -> "Odak süresi"
        SessionType.SHORT_BREAK -> "Kısa mola süresi"
        SessionType.LONG_BREAK -> "Uzun mola süresi"
    }
    val currentValue = when (currentType) {
        SessionType.FOCUS -> focusMinutes
        SessionType.SHORT_BREAK -> shortMinutes
        SessionType.LONG_BREAK -> longMinutes
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(
                        Color(0xFF1B1D3C),
                        Color(0xFF261739)
                    )
                )
            )
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Text(
            text = "$label",
            fontSize = 12.sp,
            color = Color(0xFFD6D9FF)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$currentValue dk",
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SmallRoundButton(
                    label = "-5",
                    enabled = !isRunning,
                    onClick = { onChange(currentType, -5) }
                )
                SmallRoundButton(
                    label = "+5",
                    enabled = !isRunning,
                    onClick = { onChange(currentType, +5) }
                )
            }
        }
        if (isRunning) {
            Text(
                text = "Zamanlayıcı çalışırken süreyi değiştiremezsin.",
                fontSize = 10.sp,
                color = Color(0xFF9EA4EA),
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
fun SmallRoundButton(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(999.dp),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF30355F),
            disabledContainerColor = Color(0xFF262844),
            contentColor = Color(0xFFE7EAFF),
            disabledContentColor = Color(0xFF7D80A8)
        )
    ) {
        Text(text = label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun FocusTimerSection(
    currentType: SessionType,
    timeText: String,
    isRunning: Boolean,
    justFinished: Boolean,
    progress: Float,
    onToggle: () -> Unit,
    onReset: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .weight(1.1f)
                .aspectRatio(1f),
            contentAlignment = Alignment.Center
        ) {
            TimerCircle(
                progress = progress,
                color = currentType.color,
                label = currentType.label,
                timeText = timeText,
                isRunning = isRunning,
                justFinished = justFinished
            )
        }

        Column(
            modifier = Modifier.weight(0.9f),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val text = when {
                justFinished -> "Tur bitti, mola verebilirsin. ✨"
                isRunning -> "Odak devam ediyor…"
                else -> "Süreyi ayarla, sonra başlat."
            }
            Text(
                text = text,
                fontSize = 14.sp,
                color = Color(0xFFE3D7FF),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = onToggle,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(999.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = currentType.color
                )
            ) {
                Text(
                    text = when {
                        justFinished -> "Yeni Tur Başlat"
                        isRunning -> "Duraklat"
                        else -> "Başlat"
                    },
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            OutlinedButton(
                onClick = onReset,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(999.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color(0xFFCBCFFF)
                ),
                border = ButtonDefaults.outlinedButtonBorder.copy(width = 1.dp)
            ) {
                Text(text = "Sıfırla", fontSize = 13.sp)
            }
        }
    }
}

@Composable
fun TimerCircle(
    progress: Float,
    color: Color,
    label: String,
    timeText: String,
    isRunning: Boolean,
    justFinished: Boolean
) {
    val outerGradient = Brush.sweepGradient(
        listOf(
            color.copy(alpha = 0.2f),
            color.copy(alpha = 0.9f),
            color.copy(alpha = 0.2f)
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(CircleShape)
            .background(Color(0xFF08091B)),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize(0.96f)
                .clip(CircleShape)
                .background(outerGradient)
        )

        CircularProgressIndicator(
            progress = { progress },
            color = color,
            trackColor = Color(0xFF181B3C),
            strokeWidth = 10.dp,
            modifier = Modifier.fillMaxSize(0.88f)
        )

        Box(
            modifier = Modifier
                .fillMaxSize(0.72f)
                .clip(CircleShape)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFF161833),
                            Color(0xFF060718)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = label,
                    fontSize = 13.sp,
                    color = Color(0xFFB8BBF0),
                    letterSpacing = 0.16.sp
                )
                Text(
                    text = timeText,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = when {
                        justFinished -> "Tur tamamlandı"
                        isRunning -> "Çalışma modu açık"
                        else -> "Hazır beklemede"
                    },
                    fontSize = 11.sp,
                    color = Color(0xFF9B9FE2)
                )
            }
        }
    }
}

@Composable
fun StatsRowForFocus(
    completedBlocks: Int,
    totalMinutes: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        StatCard(
            title = "Tamamlanan odak turu",
            value = completedBlocks.toString(),
            subtitle = "Ayarladığın odak blokları",
            color = Color(0xFFFF6B9C),
            modifier = Modifier.weight(1f)
        )
        StatCard(
            title = "Toplam odak süresi",
            value = "$totalMinutes dk",
            subtitle = "Bugünkü çalışma",
            color = Color(0xFF5DD6FF),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    subtitle: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        color.copy(alpha = 0.5f),
                        Color(0xFF0B0C23)
                    )
                )
            )
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Text(text = title, fontSize = 11.sp, color = Color(0xFFE4E4FF))
        Text(
            text = value,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            text = subtitle,
            fontSize = 11.sp,
            color = Color(0xFFB3B6E9)
        )
    }
}
