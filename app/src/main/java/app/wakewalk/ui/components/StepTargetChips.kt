package app.wakewalk.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StepTargetChips(
    selectedTarget: Int,
    onTargetSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val quickOptions = listOf(50, 100, 150, 200, 250, 300, 500)

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Walking Challenge",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "🚶 $selectedTarget steps",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }

        Text(
            text = "You must physically walk this many steps to dismiss the alarm.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp, bottom = 8.dp)
        )

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            quickOptions.forEach { target ->
                FilterChip(
                    selected = selectedTarget == target,
                    onClick = { onTargetSelected(target) },
                    label = { Text("$target steps") }
                )
            }
        }

        Column(modifier = Modifier.padding(top = 8.dp)) {
            Text(
                text = "Custom Target: $selectedTarget steps",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Slider(
                value = selectedTarget.toFloat(),
                onValueChange = { onTargetSelected(it.toInt()) },
                valueRange = 50f..1000f,
                steps = 18, // Steps in increments of 50
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
