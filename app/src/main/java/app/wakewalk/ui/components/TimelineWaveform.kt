package app.wakewalk.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import app.wakewalk.data.local.entity.AlarmEntity
import app.wakewalk.ui.theme.ActiveTickDark
import app.wakewalk.ui.theme.ActiveTickLight
import app.wakewalk.ui.theme.CanaryYellow
import app.wakewalk.ui.theme.NeutralTickDark
import app.wakewalk.ui.theme.NeutralTickLight

@Composable
fun TimelineWaveform(
    alarms: List<AlarmEntity>,
    nextAlarmMinuteOfDay: Int? = null,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val baseTickColor = if (isDark) NeutralTickDark else NeutralTickLight
    val activeAlarmTickColor = if (isDark) ActiveTickLight else ActiveTickDark

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp)
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val totalTicks = 48 // 48 marks across 24h (one every 30 mins)
            val availableWidth = size.width
            val height = size.height
            val spacing = availableWidth / (totalTicks - 1)

            for (i in 0 until totalTicks) {
                val tickMinute = i * 30
                val x = i * spacing

                val isNextAlarm = nextAlarmMinuteOfDay != null &&
                        (nextAlarmMinuteOfDay in tickMinute until (tickMinute + 30))

                val hasAlarm = alarms.any { alarm ->
                    alarm.isEnabled && (alarm.hour * 60 + alarm.minute in tickMinute until (tickMinute + 30))
                }

                when {
                    isNextAlarm -> {
                        // Canary Yellow next alarm indicator
                        drawLine(
                            color = CanaryYellow,
                            start = Offset(x, 0f),
                            end = Offset(x, height),
                            strokeWidth = 3.5.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }
                    hasAlarm -> {
                        // High contrast active alarm indicator
                        drawLine(
                            color = activeAlarmTickColor,
                            start = Offset(x, height * 0.15f),
                            end = Offset(x, height * 0.85f),
                            strokeWidth = 2.5.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }
                    i % 2 == 0 -> {
                        // Hour tick
                        drawLine(
                            color = baseTickColor,
                            start = Offset(x, height * 0.25f),
                            end = Offset(x, height * 0.75f),
                            strokeWidth = 1.5.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }
                    else -> {
                        // Half-hour subtle tick
                        drawLine(
                            color = baseTickColor.copy(alpha = 0.6f),
                            start = Offset(x, height * 0.35f),
                            end = Offset(x, height * 0.65f),
                            strokeWidth = 1.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }
                }
            }
        }
    }
}
