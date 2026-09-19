package app.wakewalk.ui.create

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.wakewalk.domain.scheduler.AlarmTimeCalculator
import app.wakewalk.ui.components.DualDiscTimePicker
import app.wakewalk.ui.theme.CanaryYellow

@Composable
fun CreateEditAlarmScreen(
    alarmId: Long,
    viewModel: CreateEditAlarmViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(alarmId) {
        viewModel.initForAlarm(alarmId)
    }

    val state by viewModel.uiState.collectAsState()
    val isDark = isSystemInDarkTheme()
    val context = androidx.compose.ui.platform.LocalContext.current

    val screenBgColor = if (isDark) Color(0xFF0F1014) else Color(0xFFD3D7DD)
    val textColor = if (isDark) Color.White else Color(0xFF111827)
    val subtitleColor = if (isDark) Color(0xFF9CA3AF) else Color(0xFF6B7280)

    var showDeleteConfirm by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(screenBgColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Bar: Spacer or subtle Delete button if in Edit Mode
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 40.dp),
                horizontalArrangement = Arrangement.End
            ) {
                if (state.isEditMode) {
                    IconButton(onClick = { showDeleteConfirm = true }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Alarm",
                            tint = if (isDark) Color(0xFFFF6B6B) else Color(0xFFBA1A1A)
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.height(48.dp))
                }
            }

            // Central Hero: Dual-Disc Curved Time Picker (from Pinterest image 3)
            DualDiscTimePicker(
                hour = state.hour,
                minute = state.minute,
                onTimeChanged = { h, m -> viewModel.setTime(h, m) },
                modifier = Modifier.fillMaxWidth()
            )

            // 3 Minimalist Setting Items (Sound, Snooze/Steps, Repeat) directly below dials
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. Sound
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .weight(1f)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {
                                // Toggle vibration / sound
                                viewModel.setVibrationEnabled(!state.vibrationEnabled)
                            }
                        )
                ) {
                    Icon(
                        imageVector = Icons.Outlined.MusicNote,
                        contentDescription = "Sound",
                        tint = textColor,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Sound",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                    Text(
                        text = if (state.vibrationEnabled) "Wake up + Vib" else "Wake up",
                        fontSize = 11.sp,
                        color = subtitleColor
                    )
                }

                // 2. Goal / Snooze Steps
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .weight(1f)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {
                                val next = when (state.targetSteps) {
                                    50 -> 100
                                    100 -> 150
                                    150 -> 200
                                    200 -> 300
                                    else -> 50
                                }
                                viewModel.setTargetSteps(next)
                            }
                        )
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.DirectionsWalk,
                        contentDescription = "Walk Goal",
                        tint = textColor,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Goal",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                    Text(
                        text = "${state.targetSteps} steps",
                        fontSize = 11.sp,
                        color = subtitleColor
                    )
                }

                // 3. Repeat
                val repeatDesc = AlarmTimeCalculator.describeRepeatDays(state.repeatDaysMask)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .weight(1f)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {
                                val newMask = when (state.repeatDaysMask) {
                                    0 -> AlarmTimeCalculator.WEEKDAYS_MASK
                                    AlarmTimeCalculator.WEEKDAYS_MASK -> AlarmTimeCalculator.EVERYDAY_MASK
                                    else -> 0
                                }
                                viewModel.setRepeatDaysMask(newMask)
                            }
                        )
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Repeat,
                        contentDescription = "Repeat",
                        tint = textColor,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Repeat",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                    Text(
                        text = if (state.repeatDaysMask == 0) "No" else repeatDesc,
                        fontSize = 11.sp,
                        color = subtitleColor
                    )
                }
            }

            // Bottom Action Bar: [✕]   "Choose time"   [✔]
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp, top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Cancel 'X'
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onNavigateBack
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cancel",
                        tint = textColor,
                        modifier = Modifier.size(26.dp)
                    )
                }

                // Center Label
                Text(
                    text = if (state.isEditMode) "Edit Alarm" else "Choose time",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = subtitleColor
                )

                // Confirm '✔' button
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF111827))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {
                                viewModel.saveAlarm { message ->
                                    android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_LONG).show()
                                    onNavigateBack()
                                }
                            }
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Confirm",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }

        // Delete Confirmation Dialog
        if (showDeleteConfirm) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirm = false },
                title = { Text("Delete Alarm?") },
                text = { Text("Are you sure you want to delete this alarm?") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showDeleteConfirm = false
                            viewModel.deleteAlarm(onNavigateBack)
                        }
                    ) {
                        Text("Delete", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirm = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}
