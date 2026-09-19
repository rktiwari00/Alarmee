package app.wakewalk.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.wakewalk.domain.scheduler.AlarmTimeCalculator
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

        // 7 circle day chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            days.forEach { (day, letter) ->
                val isSelected = AlarmTimeCalculator.isDaySelected(repeatDaysMask, day)
                val bit = AlarmTimeCalculator.dayOfWeekToBit(day)

                FilterChip(
                    selected = isSelected,
                    onClick = {
                        val newMask = if (isSelected) {
                            repeatDaysMask and bit.inv()
                        } else {
                            repeatDaysMask or bit
                        }
                        onMaskChanged(newMask)
                    },
                    label = {
                        Text(
                            text = letter,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    shape = CircleShape,
                    modifier = Modifier.size(44.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
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
