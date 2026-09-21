package app.wakewalk.ui.alarm

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.wakewalk.data.preferences.UserPreferences
import app.wakewalk.domain.model.ChallengeType
import app.wakewalk.domain.model.StepTrackingMode
import app.wakewalk.ui.components.CameraBarcodeScanner
import kotlinx.coroutines.delay
import java.time.LocalTime
import java.time.format.DateTimeFormatter

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
            .background(Color.Black)
            .padding(24.dp)
    ) {
        when {
            uiState.isCompleted -> {
                // Completed Success View
                CompletionCard(
                    steps = uiState.completedSteps,
                    durationSeconds = uiState.durationSeconds,
                    challengeType = uiState.challengeType,
                    qrLabel = uiState.targetQrLabel,
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
                // Active Ringing & Challenge View
                if (uiState.challengeType == ChallengeType.QR_CODE) {
                    QrChallengeRingingContent(
                        uiState = uiState,
                        currentTimeString = currentTimeString,
                        onEmergencyClick = { viewModel.showEmergencyDialog() },
                        onBarcodeDetected = { viewModel.onBarcodeScanned(it) },
                        onTorchToggle = { viewModel.toggleTorch() }
                    )
                } else {
                    WalkChallengeRingingContent(
                        uiState = uiState,
                        currentTimeString = currentTimeString,
                        onEmergencyClick = { viewModel.showEmergencyDialog() }
                    )
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
private fun WalkChallengeRingingContent(
    uiState: AlarmUiState,
    currentTimeString: String,
    onEmergencyClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top: Current Time & Sensor Banner
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 32.dp)
        ) {
            Text(
                text = currentTimeString,
                style = MaterialTheme.typography.displayLarge.copy(fontSize = 64.sp),
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "WALK TO DISMISS",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 2.sp,
                color = MaterialTheme.colorScheme.primary
            )

            // Sensor fallback notice
            val mode = uiState.session?.trackingMode
            if (mode == StepTrackingMode.ACCELEROMETER_FALLBACK) {
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF2C2411)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color(0xFFFFB74D),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Accelerometer fallback active. Keep in hand.",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFFFB74D)
                        )
                    }
                }
            }
        }

        // Middle: Giant Step Progress & Milestones
        val currentSteps = uiState.session?.currentSteps ?: 0
        val targetSteps = uiState.session?.targetSteps ?: 150
        val progressRatio = (currentSteps.toFloat() / targetSteps.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
        val animatedProgress by animateFloatAsState(targetValue = progressRatio, label = "stepProgress")

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier.size(240.dp),
                    strokeWidth = 14.dp,
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = Color(0xFF222222),
                    strokeCap = StrokeCap.Round
                )

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.DirectionsWalk,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$currentSteps",
                        style = MaterialTheme.typography.displayMedium.copy(fontSize = 52.sp),
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                    Text(
                        text = "/ $targetSteps STEPS",
                        style = MaterialTheme.typography.labelLarge,
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
                fontWeight = FontWeight.Medium,
                color = if (currentSteps > 0) MaterialTheme.colorScheme.primary else Color.LightGray,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Shaking phone will not register. Genuine steps required.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.DarkGray,
                textAlign = TextAlign.Center
            )
        }

        // Bottom: Subordinate Emergency Stop
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            TextButton(
                onClick = onEmergencyClick,
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

@Composable
private fun QrChallengeRingingContent(
    uiState: AlarmUiState,
    currentTimeString: String,
    onEmergencyClick: () -> Unit,
    onBarcodeDetected: (String) -> Unit,
    onTorchToggle: () -> Unit
) {
    val currentSteps = uiState.completedSteps
    val targetSteps = uiState.targetSteps.coerceAtLeast(1)
    val hasMetStepRequirement = currentSteps >= targetSteps
    val stepsRemaining = (targetSteps - currentSteps).coerceAtLeast(0)
    val progressRatio = (currentSteps.toFloat() / targetSteps.toFloat()).coerceIn(0f, 1f)

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top Section: Time, Target Badge & Walk Progress
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        ) {
            Text(
                text = currentTimeString,
                style = MaterialTheme.typography.displayMedium.copy(fontSize = 38.sp),
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "WALK & SCAN TO DISMISS",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Target Info & Step Meter Card
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = uiState.targetQrLabel ?: "Target Code",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        }

                        Text(
                            text = "$currentSteps / $targetSteps steps",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (hasMetStepRequirement) Color(0xFF81C784) else Color(0xFFFFB74D)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    LinearProgressIndicator(
                        progress = { progressRatio },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = if (hasMetStepRequirement) Color(0xFF81C784) else Color(0xFFFFB74D),
                        trackColor = Color(0xFF333333)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Dual Verification Status Banner
            if (!hasMetStepRequirement) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF2C2411)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.DirectionsWalk,
                            contentDescription = null,
                            tint = Color(0xFFFFB74D),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Walk to your target item first",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFFB74D)
                            )
                            Text(
                                text = "$stepsRemaining more steps required to unlock scanning.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFFFD54F).copy(alpha = 0.85f)
                            )
                        }
                    }
                }
            } else {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF142B1A)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF81C784),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Step requirement complete!",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF81C784)
                            )
                            Text(
                                text = "Point camera at target code to dismiss alarm.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFA5D6A7)
                            )
                        }
                    }
                }
            }

            // Error Banner if scan was rejected
            if (uiState.qrScanError != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF3B1818)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color(0xFFFF6B6B),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = uiState.qrScanError,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFFFF8A80)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Live Barcode / QR Camera Viewfinder
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(20.dp))
        ) {
            CameraBarcodeScanner(
                modifier = Modifier.fillMaxSize(),
                isTorchEnabled = uiState.isTorchEnabled,
                onTorchToggle = onTorchToggle,
                onBarcodeDetected = onBarcodeDetected,
                reticleBorderColor = if (hasMetStepRequirement) MaterialTheme.colorScheme.primary else Color(0xFFFFB74D),
                instructionText = if (hasMetStepRequirement) {
                    "Point at ${uiState.targetQrLabel ?: "target code"}"
                } else {
                    "Walk $stepsRemaining more steps to unlock"
                }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Bottom Emergency Option
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        ) {
            TextButton(
                onClick = onEmergencyClick,
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

@Composable
private fun CompletionCard(
    steps: Int,
    durationSeconds: Long,
    challengeType: ChallengeType = ChallengeType.WALK,
    qrLabel: String? = null,
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier.padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(64.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

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

            val summaryText = if (challengeType == ChallengeType.QR_CODE) {
                "$steps steps walked & ${qrLabel ?: "code"} verified in $timeString"
            } else {
                "$steps steps completed in $timeString"
            }

            Text(
                text = summaryText,
                style = MaterialTheme.typography.bodyLarge,
                color = Color.LightGray,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Good morning ☀️",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(28.dp))

            Button(
                onClick = onDone,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text(
                    text = "Good Morning",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
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
        colors = CardDefaults.cardColors(containerColor = Color(0xFF281E1E)),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier.padding(28.dp),
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
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF444444))
            ) {
                Text(text = "Close", color = Color.White)
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
