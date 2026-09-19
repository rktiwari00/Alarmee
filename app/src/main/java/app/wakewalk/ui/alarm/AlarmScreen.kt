package app.wakewalk.ui.alarm

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.wakewalk.data.preferences.UserPreferences
import app.wakewalk.domain.model.StepTrackingMode
import app.wakewalk.ui.theme.CanaryYellow
import kotlinx.coroutines.delay
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun AlarmScreen(
    viewModel: AlarmViewModel,
    onDismissActivity: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val preferences by viewModel.preferencesFlow.collectAsState()

    var currentTimeString by remember {
        mutableStateOf(LocalTime.now().format(DateTimeFormatter.ofPattern("hh:mm a")))
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            currentTimeString = LocalTime.now().format(DateTimeFormatter.ofPattern("hh:mm a"))
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0C0D11))
            .padding(24.dp)
    ) {
        when {
            uiState.isCompleted -> {
                // Completed Success View
                CompletionCard(
                    steps = uiState.completedSteps,
                    durationSeconds = uiState.durationSeconds,
                    onDone = onDismissActivity,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            uiState.isEmergencyDismissed -> {
                // Emergency Dismissed View
                EmergencyDismissedCard(
                    onClose = onDismissActivity,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            else -> {
                // Active Ringing & Step Challenge View
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Top: Minimalist Clock Dial & Title (from Pinterest image 2)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(top = 28.dp)
                    ) {
                        Text(
                            text = currentTimeString,
                            style = MaterialTheme.typography.displayLarge.copy(fontSize = 58.sp),
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "WALK TO DISMISS",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp,
                            color = CanaryYellow
                        )

                        // Sensor fallback notice
                        val mode = uiState.session?.trackingMode
                        if (mode == StepTrackingMode.ACCELEROMETER_FALLBACK) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF242200)),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = CanaryYellow,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Accelerometer fallback active. Keep phone in hand.",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = CanaryYellow
                                    )
                                }
                            }
                        }
                    }

                    // Middle: Giant Step Progress Ring with Canary Yellow Accents
                    val currentSteps = uiState.session?.currentSteps ?: 0
                    val targetSteps = uiState.session?.targetSteps ?: 150
                    val progressRatio = (currentSteps.toFloat() / targetSteps.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
                    val animatedProgress by animateFloatAsState(targetValue = progressRatio, label = "stepProgress")

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            // Perimeter tick clock dial background
                            Canvas(modifier = Modifier.size(260.dp)) {
                                val radius = size.minDimension / 2f
                                val center = Offset(size.width / 2f, size.height / 2f)
                                val tickCount = 48
                                for (i in 0 until tickCount) {
                                    val angle = (i * 360f / tickCount) * (Math.PI / 180f)
                                    val startR = radius - 8.dp.toPx()
                                    val endR = radius - 2.dp.toPx()
                                    val startX = center.x + startR * cos(angle).toFloat()
                                    val startY = center.y + startR * sin(angle).toFloat()
                                    val endX = center.x + endR * cos(angle).toFloat()
                                    val endY = center.y + endR * sin(angle).toFloat()

                                    drawLine(
                                        color = Color(0xFF262832),
                                        start = Offset(startX, startY),
                                        end = Offset(endX, endY),
                                        strokeWidth = 1.5.dp.toPx(),
                                        cap = StrokeCap.Round
                                    )
                                }
                            }

                            // Dynamic progress ring
                            CircularProgressIndicator(
                                progress = { animatedProgress },
                                modifier = Modifier.size(240.dp),
                                strokeWidth = 12.dp,
                                color = CanaryYellow,
                                trackColor = Color(0xFF1E2028),
                                strokeCap = StrokeCap.Round
                            )

                            // Inner readout
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.DirectionsWalk,
                                    contentDescription = null,
                                    tint = CanaryYellow,
                                    modifier = Modifier.size(38.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "$currentSteps",
                                    style = MaterialTheme.typography.displayMedium.copy(fontSize = 54.sp),
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                                Text(
                                    text = "/ $targetSteps STEPS",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Gray,
                                    letterSpacing = 1.5.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(28.dp))

                        // Gait & movement guidance
                        Text(
                            text = if (currentSteps > 0) "Walking detected — keep moving!" else "Get out of bed and start walking...",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = if (currentSteps > 0) CanaryYellow else Color.LightGray,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Shaking phone will not register. Genuine steps required.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF6B7280),
                            textAlign = TextAlign.Center
                        )
                    }

                    // Bottom: Subordinate Emergency Stop Button
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    ) {
                        TextButton(
                            onClick = { viewModel.showEmergencyDialog() },
                            colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF888888))
                        ) {
                            Text(
                                text = "Emergency Stop",
                                style = MaterialTheme.typography.bodyMedium,
                                letterSpacing = 1.sp
                            )
                        }
                    }
                }
            }
        }

        // Emergency Dismissal Dialog
        if (uiState.emergencyDialogVisible) {
            EmergencyDismissalDialog(
                method = preferences.emergencyDismissalMethod,
                phraseInput = uiState.emergencyPhraseInput,
                onPhraseChanged = { viewModel.onEmergencyPhraseChanged(it) },
                isPhraseValid = viewModel.isPhraseValid(),
                onConfirmPhrase = { viewModel.submitEmergencyPhrase() },
                onLongPressCompleted = { durationMs -> viewModel.updateLongPressDuration(durationMs) },
                onDismiss = { viewModel.dismissEmergencyDialog() }
            )
        }
    }
}

@Composable
private fun CompletionCard(
    steps: Int,
    durationSeconds: Long,
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF161820)),
        shape = RoundedCornerShape(26.dp)
    ) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = CanaryYellow,
                modifier = Modifier.size(68.dp)
            )

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "You're Awake!",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(8.dp))

            val minutes = durationSeconds / 60
            val seconds = durationSeconds % 60
            val timeString = if (minutes > 0) "${minutes}m ${seconds}s" else "${seconds}s"

            Text(
                text = "$steps steps completed in $timeString",
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFFD1D5DB)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Good morning ☀️",
                style = MaterialTheme.typography.titleMedium,
                color = CanaryYellow,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(28.dp))

            Button(
                onClick = onDone,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CanaryYellow,
                    contentColor = Color(0xFF111827)
                )
            ) {
                Text(
                    text = "Good Morning",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun EmergencyDismissedCard(
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF221618)),
        shape = RoundedCornerShape(26.dp)
    ) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = Color(0xFFFF6B6B),
                modifier = Modifier.size(56.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Alarm Stopped",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Dismissed via emergency bypass.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.LightGray,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onClose,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B3F4A))
            ) {
                Text(text = "Close", color = Color.White, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun EmergencyDismissalDialog(
    method: String,
    phraseInput: String,
    onPhraseChanged: (String) -> Unit,
    isPhraseValid: Boolean,
    onConfirmPhrase: () -> Unit,
    onLongPressCompleted: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Emergency Dismissal",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Only use this for genuine emergencies. Normal wake-up requires physical walking.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))

                if (method == UserPreferences.METHOD_LONG_PRESS) {
                    LongPressEmergencyControl(onLongPressCompleted = onLongPressCompleted)
                } else {
                    // Default: Typing phrase
                    Text(
                        text = "Type \"I AM AWAKE\":",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = phraseInput,
                        onValueChange = onPhraseChanged,
                        singleLine = true,
                        placeholder = { Text("I AM AWAKE") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            if (method != UserPreferences.METHOD_LONG_PRESS) {
                Button(
                    onClick = onConfirmPhrase,
                    enabled = isPhraseValid,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text("Stop Alarm")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun LongPressEmergencyControl(
    onLongPressCompleted: (Long) -> Unit
) {
    var isPressing by remember { mutableStateOf(false) }
    var pressStartTime by remember { mutableLongStateOf(0L) }
    var progress by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(isPressing) {
        if (isPressing) {
            pressStartTime = System.currentTimeMillis()
            while (isPressing) {
                val elapsed = System.currentTimeMillis() - pressStartTime
                progress = (elapsed.toFloat() / 5000f).coerceIn(0f, 1f)
                if (elapsed >= 5000L) {
                    onLongPressCompleted(elapsed)
                    break
                }
                delay(50)
            }
        } else {
            progress = 0f
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            isPressing = true
                            tryAwaitRelease()
                            isPressing = false
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                progress = { progress },
                modifier = Modifier.size(90.dp),
                strokeWidth = 6.dp,
                color = MaterialTheme.colorScheme.error,
                trackColor = Color.LightGray.copy(alpha = 0.3f)
            )
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(
                        if (isPressing) MaterialTheme.colorScheme.error else Color.DarkGray
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isPressing) "${String.format("%.1f", progress * 5)}s" else "Hold",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "Press and hold for 5 continuous seconds",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
