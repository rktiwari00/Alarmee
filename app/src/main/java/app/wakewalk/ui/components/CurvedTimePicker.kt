package app.wakewalk.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.wakewalk.ui.theme.CanaryYellow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import kotlin.math.abs

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CurvedTimePicker(
    hour: Int,
    minute: Int,
    onTimeChanged: (hour: Int, minute: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()

    // 24 hours and 60 minutes
    val hours = (0..23).toList()
    val minutes = (0..59).toList()

    val itemHeight = 56.dp
    val totalHeight = itemHeight * 5 // 5 visible rows

    // Virtual infinite repeat multiplier
    val repeatMultiplier = 100
    val initialHourIndex = (repeatMultiplier / 2) * 24 + hour
    val initialMinuteIndex = (repeatMultiplier / 2) * 60 + minute

    val hourListState = rememberLazyListState(initialFirstVisibleItemIndex = (initialHourIndex - 2).coerceAtLeast(0))
    val minuteListState = rememberLazyListState(initialFirstVisibleItemIndex = (initialMinuteIndex - 2).coerceAtLeast(0))

    val hourFling = rememberSnapFlingBehavior(lazyListState = hourListState)
    val minuteFling = rememberSnapFlingBehavior(lazyListState = minuteListState)

    // Current focused index derived from list state
    val focusedHourIndex by remember {
        derivedStateOf { hourListState.firstVisibleItemIndex + 2 }
    }
    val focusedMinuteIndex by remember {
        derivedStateOf { minuteListState.firstVisibleItemIndex + 2 }
    }

    // Sync scroll to state changes
    LaunchedEffect(hourListState) {
        snapshotFlow { focusedHourIndex }
            .distinctUntilChanged()
            .collect { index ->
                val selectedHour = index % 24
                if (selectedHour != hour) {
                    onTimeChanged(selectedHour, minute)
                }
            }
    }

    LaunchedEffect(minuteListState) {
        snapshotFlow { focusedMinuteIndex }
            .distinctUntilChanged()
            .collect { index ->
                val selectedMinute = index % 60
                if (selectedMinute != minute) {
                    onTimeChanged(hour, selectedMinute)
                }
            }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(totalHeight),
        contentAlignment = Alignment.Center
    ) {
        // Curved / Hourglass focal backdrop container (from Pinterest image 3)
        Box(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .height(itemHeight + 8.dp)
                .clip(RoundedCornerShape(26.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Hours Column
            Box(
                modifier = Modifier
                    .width(120.dp)
                    .height(totalHeight),
                contentAlignment = Alignment.Center
            ) {
                LazyColumn(
                    state = hourListState,
                    flingBehavior = hourFling,
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = itemHeight * 2)
                ) {
                    items(24 * repeatMultiplier) { index ->
                        val h = index % 24
                        val isCenter = index == focusedHourIndex

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(itemHeight)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = {
                                        coroutineScope.launch {
                                            hourListState.animateScrollToItem((index - 2).coerceAtLeast(0))
                                        }
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = String.format("%02d", h),
                                fontSize = if (isCenter) 48.sp else 30.sp,
                                fontWeight = if (isCenter) FontWeight.ExtraBold else FontWeight.Medium,
                                color = if (isCenter) {
                                    MaterialTheme.colorScheme.onSurface
                                } else {
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
                                }
                            )
                        }
                    }
                }
            }

            // Divider colon
            Text(
                text = ":",
                fontSize = 44.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            // Minutes Column
            Box(
                modifier = Modifier
                    .width(120.dp)
                    .height(totalHeight),
                contentAlignment = Alignment.Center
            ) {
                LazyColumn(
                    state = minuteListState,
                    flingBehavior = minuteFling,
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = itemHeight * 2)
                ) {
                    items(60 * repeatMultiplier) { index ->
                        val m = index % 60
                        val isCenter = index == focusedMinuteIndex

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(itemHeight)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = {
                                        coroutineScope.launch {
                                            minuteListState.animateScrollToItem((index - 2).coerceAtLeast(0))
                                        }
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = String.format("%02d", m),
                                fontSize = if (isCenter) 48.sp else 30.sp,
                                fontWeight = if (isCenter) FontWeight.ExtraBold else FontWeight.Medium,
                                color = if (isCenter) {
                                    MaterialTheme.colorScheme.onSurface
                                } else {
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
