package app.wakewalk.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun DualDiscTimePicker(
    hour: Int,
    minute: Int,
    onTimeChanged: (hour: Int, minute: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val density = LocalDensity.current

    // Colors matching Pinterest image 3
    val discColor = if (isDark) Color(0xFF1E202A) else Color(0xFFE8ECF2)
    val numberColor = if (isDark) android.graphics.Color.WHITE else android.graphics.Color.parseColor("#111827")
    val mutedAlpha = if (isDark) 0.35f else 0.38f

    val stepYDp = 54.dp
    val stepY = with(density) { stepYDp.toPx() }
    val curveOffsetPx = with(density) { 6.5.dp.toPx() }

    var hourDragAccumulator by remember { mutableFloatStateOf(0f) }
    var minuteDragAccumulator by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(390.dp)
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onVerticalDrag = { change, dragAmount ->
                        change.consume()
                        val isLeftHalf = change.position.x < size.width / 2f
                        if (isLeftHalf) {
                            hourDragAccumulator += dragAmount
                            val steps = (hourDragAccumulator / stepY).toInt()
                            if (steps != 0) {
                                val newHour = (hour - steps).mod(24)
                                onTimeChanged(newHour, minute)
                                hourDragAccumulator -= steps * stepY
                            }
                        } else {
                            minuteDragAccumulator += dragAmount
                            val steps = (minuteDragAccumulator / stepY).toInt()
                            if (steps != 0) {
                                val newMinute = (minute - steps).mod(60)
                                onTimeChanged(hour, newMinute)
                                minuteDragAccumulator -= steps * stepY
                            }
                        }
                    }
                )
            }
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val isLeftHalf = offset.x < size.width / 2f
                    val centerY = size.height / 2f
                    val clickedStep = ((offset.y - centerY) / stepY).roundToInt()
                    if (clickedStep != 0 && abs(clickedStep) <= 3) {
                        if (isLeftHalf) {
                            val newHour = (hour + clickedStep).mod(24)
                            onTimeChanged(newHour, minute)
                        } else {
                            val newMinute = (minute + clickedStep).mod(60)
                            onTimeChanged(hour, newMinute)
                        }
                    }
                }
            }
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val width = size.width
            val height = size.height
            val centerY = height / 2f

            // Left & Right giant circular discs (curved geometry from Pinterest image 3)
            val discRadius = width * 0.85f
            drawCircle(
                color = discColor,
                radius = discRadius,
                center = Offset(width * 0.44f - discRadius + 40.dp.toPx(), centerY)
            )
            drawCircle(
                color = discColor,
                radius = discRadius,
                center = Offset(width * 0.56f + discRadius - 40.dp.toPx(), centerY)
            )

            // Draw numbers along the curved paths
            drawIntoCanvas { canvas ->
                val nativeCanvas = canvas.nativeCanvas

                val paint = android.graphics.Paint().apply {
                    isAntiAlias = true
                    textAlign = android.graphics.Paint.Align.CENTER
                    typeface = android.graphics.Typeface.create(
                        android.graphics.Typeface.SANS_SERIF,
                        android.graphics.Typeface.BOLD
                    )
                }

                // Range of visible rows: -3 to +3
                for (offsetRow in -3..3) {
                    val y = centerY + offsetRow * stepY + with(density) { 16.dp.toPx() }
                    val dist = abs(offsetRow)

                    val fontSizeSp = when (dist) {
                        0 -> 54f
                        1 -> 42f
                        2 -> 32f
                        else -> 24f
                    }
                    val alpha = when (dist) {
                        0 -> 1f
                        1 -> 0.65f
                        2 -> 0.38f
                        else -> 0.18f
                    }

                    paint.textSize = with(density) { fontSizeSp.dp.toPx() }
                    paint.color = numberColor
                    paint.alpha = (alpha * 255).toInt()

                    // Curvature: items further from center move outwards
                    val curveShift = offsetRow * offsetRow * curveOffsetPx

                    // Left disc: Hours
                    val currentH = (hour + offsetRow).mod(24)
                    val hourX = width * 0.42f - curveShift
                    nativeCanvas.drawText(String.format("%02d", currentH), hourX, y, paint)

                    // Right disc: Minutes
                    val currentM = (minute + offsetRow).mod(60)
                    val minuteX = width * 0.58f + curveShift
                    nativeCanvas.drawText(String.format("%02d", currentM), minuteX, y, paint)
                }
            }
        }
    }
}
