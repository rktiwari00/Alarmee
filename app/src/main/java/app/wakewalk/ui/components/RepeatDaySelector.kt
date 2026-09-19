package app.wakewalk.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.wakewalk.domain.scheduler.AlarmTimeCalculator
import app.wakewalk.ui.theme.CanaryYellow
import java.time.DayOfWeek

@Composable
fun RepeatDaySelector(
    repeatDaysMask: Int,
    onMaskChanged: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val days = listOf(
        DayOfWeek.MONDAY to "M",
        DayOfWeek.TUESDAY to "T",
        DayOfWeek.WEDNESDAY to "W",
        DayOfWeek.THURSDAY to "T",
        DayOfWeek.FRIDAY to "F",
        DayOfWeek.SATURDAY to "S",
        DayOfWeek.SUNDAY to "S"
    )

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Repeat",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // 7 circular Canary Yellow chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            days.forEach { (day, letter) ->
                val isSelected = AlarmTimeCalculator.isDaySelected(repeatDaysMask, day)
                val bit = AlarmTimeCalculator.dayOfWeekToBit(day)

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(
                            color = if (isSelected) CanaryYellow else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {
                                val newMask = if (isSelected) {
                                    repeatDaysMask and bit.inv()
                                } else {
                                    repeatDaysMask or bit
                                }
                                onMaskChanged(newMask)
                            }
                        )
                ) {
                    Text(
                        text = letter,
                        fontSize = 14.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) {
                            Color(0xFF111827)
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }
        }

        // Quick presets
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SuggestionChip(
                onClick = { onMaskChanged(AlarmTimeCalculator.EVERYDAY_MASK) },
                label = { Text("Every day") }
            )
            SuggestionChip(
                onClick = { onMaskChanged(AlarmTimeCalculator.WEEKDAYS_MASK) },
                label = { Text("Weekdays") }
            )
            SuggestionChip(
                onClick = { onMaskChanged(AlarmTimeCalculator.WEEKENDS_MASK) },
                label = { Text("Weekends") }
            )
            if (repeatDaysMask != 0) {
                SuggestionChip(
                    onClick = { onMaskChanged(0) },
                    label = { Text("Once") }
                )
            }
        }
    }
}
