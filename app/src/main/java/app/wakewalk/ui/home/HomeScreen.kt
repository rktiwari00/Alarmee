package app.wakewalk.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.wakewalk.data.local.entity.AlarmEntity
import app.wakewalk.ui.components.AlarmCard
import app.wakewalk.ui.components.FloatingBottomPill
import app.wakewalk.ui.components.TimelineWaveform
import app.wakewalk.ui.components.WeekdaySelectorRow
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.LocalTime

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToCreate: () -> Unit,
    onNavigateToEdit: (Long) -> Unit,
    onNavigateToStats: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val alarms by viewModel.alarms.collectAsState()
    val streakDays by viewModel.streakDays.collectAsState()
    val playingAlarmId by viewModel.playingAlarmId.collectAsState()

    val nextAlarmText = viewModel.getNextAlarmTimeRemaining(alarms)
    val nextAlarmMinute = viewModel.getNextAlarmMinuteOfDay(alarms)

    var selectedDay by remember { mutableStateOf(LocalDate.now().dayOfWeek) }
    var alarmToDelete by remember { mutableStateOf<AlarmEntity?>(null) }

    // Live current time fallback if no alarm
    var currentTime by remember { mutableStateOf(LocalTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            currentTime = LocalTime.now()
        }
    }

    // Determine hero time to display: next upcoming alarm if exists, else current time
    val (heroHour, heroMinute) = if (nextAlarmMinute != null) {
        Pair(nextAlarmMinute / 60, nextAlarmMinute % 60)
    } else {
        Pair(currentTime.hour, currentTime.minute)
    }

    val displayHour = when (val h = heroHour % 12) {
        0 -> 12
        else -> h
    }
    val heroHourString = String.format("%02d", displayHour)
    val heroMinuteString = String.format("%02d", heroMinute)

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
            // Top Header: "Alarm" + Settings & Streak badge
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onNavigateToSettings
                    )
                ) {
                    Text(
                        text = "Alarm",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Settings",
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier
                            .padding(start = 4.dp)
                            .size(24.dp)
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (streakDays > 0) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                        ) {
                            Text(
                                text = "🔥 $streakDays Days",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = onNavigateToSettings,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = "Settings",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            // Hero Digital Time (matching the Pinterest reference: bold hour, lighter minute)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, bottom = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "$heroHourString:",
                        fontSize = 76.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onBackground,
                        letterSpacing = (-2).sp
                    )
                    Text(
                        text = heroMinuteString,
                        fontSize = 76.sp,
                        fontWeight = FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f),
                        letterSpacing = (-2).sp
                    )
                }

                // Countdown Subtitle
                val subtitleText = if (nextAlarmText != null) {
                    buildAnnotatedString {
                        append("The next alarm clock in ")
                        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)) {
                            append(nextAlarmText.removePrefix("in "))
                        }
                    }
                } else {
                    buildAnnotatedString {
                        append("No upcoming alarms scheduled")
                    }
                }

                Text(
                    text = subtitleText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            // 24-Hour Activity / Timeline Waveform
            TimelineWaveform(
                alarms = alarms,
                nextAlarmMinuteOfDay = nextAlarmMinute,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
            )

            // Weekday Chips Row
            WeekdaySelectorRow(
                selectedDay = selectedDay,
                onDaySelected = { selectedDay = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp)
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Alarm Cards List
            if (alarms.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "🚶‍♂️", fontSize = 48.sp)
                        Text(
                            text = "No Alarms Set",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 12.dp)
                        )
                        Text(
                            text = "Tap + to create an alarm that forces you out of bed.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(top = 4.dp, bottom = 100.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(alarms, key = { it.id }) { alarm ->
                        AlarmCard(
                            alarm = alarm,
                            isPlaying = playingAlarmId == alarm.id,
                            onPlayToggle = { viewModel.toggleSoundPreview(alarm) },
                            onToggle = { viewModel.toggleAlarm(alarm) },
                            onClick = { onNavigateToEdit(alarm.id) },
                            onLongClick = { alarmToDelete = alarm }
                        )
                    }
                }
            }
        }

        // Floating Bottom Capsule Bar
        FloatingBottomPill(
            onAddClick = onNavigateToCreate,
            onCalendarClick = onNavigateToStats,
            statusText = if (nextAlarmText != null) "Next: $nextAlarmText" else "WakeWalk • Ready",
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 20.dp)
        )

        // Delete confirmation dialog
        alarmToDelete?.let { alarm ->
            AlertDialog(
                onDismissRequest = { alarmToDelete = null },
                title = { Text("Delete Alarm?") },
                text = { Text("Are you sure you want to delete this alarm?") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.deleteAlarm(alarm)
                            alarmToDelete = null
                        }
                    ) {
                        Text("Delete", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { alarmToDelete = null }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}
