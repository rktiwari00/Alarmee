package app.wakewalk.domain.scheduler

import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

object AlarmTimeCalculator {

    const val MON_BIT = 1 shl 0 // 1
    const val TUE_BIT = 1 shl 1 // 2
    const val WED_BIT = 1 shl 2 // 4
    const val THU_BIT = 1 shl 3 // 8
    const val FRI_BIT = 1 shl 4 // 16
    const val SAT_BIT = 1 shl 5 // 32
    const val SUN_BIT = 1 shl 6 // 64

    const val WEEKDAYS_MASK = MON_BIT or TUE_BIT or WED_BIT or THU_BIT or FRI_BIT // 31
    const val WEEKENDS_MASK = SAT_BIT or SUN_BIT // 96
    const val EVERYDAY_MASK = WEEKDAYS_MASK or WEEKENDS_MASK // 127

    fun dayOfWeekToBit(day: DayOfWeek): Int {
        return when (day) {
            DayOfWeek.MONDAY -> MON_BIT
            DayOfWeek.TUESDAY -> TUE_BIT
            DayOfWeek.WEDNESDAY -> WED_BIT
            DayOfWeek.THURSDAY -> THU_BIT
            DayOfWeek.FRIDAY -> FRI_BIT
            DayOfWeek.SATURDAY -> SAT_BIT
            DayOfWeek.SUNDAY -> SUN_BIT
        }
    }

    fun isDaySelected(mask: Int, day: DayOfWeek): Boolean {
        return (mask and dayOfWeekToBit(day)) != 0
    }

    fun calculateNextTriggerTime(
        hour: Int,
        minute: Int,
        repeatDaysMask: Int,
        nowEpochMs: Long = System.currentTimeMillis(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): Long {
        val now = ZonedDateTime.ofInstant(Instant.ofEpochMilli(nowEpochMs), zoneId)
        val todayTarget = now
            .withHour(hour)
            .withMinute(minute)
            .withSecond(0)
            .withNano(0)

        // Case 1: One-time alarm (no repeat days selected)
        if (repeatDaysMask == 0) {
            return if (todayTarget.isAfter(now)) {
                todayTarget.toInstant().toEpochMilli()
            } else {
                todayTarget.plusDays(1).toInstant().toEpochMilli()
            }
        }

        // Case 2: Repeating alarm
        // Check if today is selected and the alarm time is still in the future
        if (isDaySelected(repeatDaysMask, now.dayOfWeek) && todayTarget.isAfter(now)) {
            return todayTarget.toInstant().toEpochMilli()
        }

        // Otherwise find the earliest matching day within the next 7 days
        for (offset in 1..7) {
            val candidate = todayTarget.plusDays(offset.toLong())
            if (isDaySelected(repeatDaysMask, candidate.dayOfWeek)) {
                return candidate.toInstant().toEpochMilli()
            }
        }

        // Fallback
        return todayTarget.plusDays(1).toInstant().toEpochMilli()
    }

    fun calculateNextOccurrenceAfter(
        hour: Int,
        minute: Int,
        repeatDaysMask: Int,
        afterEpochMs: Long = System.currentTimeMillis(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): Long {
        // Advance at least 60 seconds into the future so that if the alarm triggered
        // at or slightly before its scheduled minute, it rolls safely to the next occurrence.
        val safeEpochMs = afterEpochMs + 60_000L
        return calculateNextTriggerTime(hour, minute, repeatDaysMask, safeEpochMs, zoneId)
    }

    fun formatTimeRemaining(triggerEpochMs: Long, nowEpochMs: Long = System.currentTimeMillis()): String {
        val diffMs = (triggerEpochMs - nowEpochMs).coerceAtLeast(0L)
        val totalMinutes = ChronoUnit.MINUTES.between(
            Instant.ofEpochMilli(nowEpochMs),
            Instant.ofEpochMilli(triggerEpochMs)
        )

        if (totalMinutes <= 0) return "Alarm due now"

        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60

        return when {
            hours == 0L -> "Alarm in ${minutes}m"
            minutes == 0L -> "Alarm in ${hours}h"
            else -> "Alarm in ${hours}h ${minutes}m"
        }
    }

    fun describeRepeatDays(mask: Int): String {
        return when (mask) {
            0 -> "Once"
            EVERYDAY_MASK -> "Every day"
            WEEKDAYS_MASK -> "Weekdays"
            WEEKENDS_MASK -> "Weekends"
            else -> {
                val days = mutableListOf<String>()
                if (isDaySelected(mask, DayOfWeek.MONDAY)) days.add("Mon")
                if (isDaySelected(mask, DayOfWeek.TUESDAY)) days.add("Tue")
                if (isDaySelected(mask, DayOfWeek.WEDNESDAY)) days.add("Wed")
                if (isDaySelected(mask, DayOfWeek.THURSDAY)) days.add("Thu")
                if (isDaySelected(mask, DayOfWeek.FRIDAY)) days.add("Fri")
                if (isDaySelected(mask, DayOfWeek.SATURDAY)) days.add("Sat")
                if (isDaySelected(mask, DayOfWeek.SUNDAY)) days.add("Sun")
                days.joinToString(", ")
            }
        }
    }
}
