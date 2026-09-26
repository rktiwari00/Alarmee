package app.wakewalk.domain.scheduler

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

class AlarmTimeCalculatorTest {

    private val zoneId = ZoneId.of("UTC")

    @Test
    fun testOneTimeAlarmLaterToday() {
        // Today is 10:00 AM, alarm set for 14:30
        val now = ZonedDateTime.of(2026, 9, 19, 10, 0, 0, 0, zoneId)
        val nextTrigger = AlarmTimeCalculator.calculateNextTriggerTime(
            hour = 14,
            minute = 30,
            repeatDaysMask = 0,
            nowEpochMs = now.toInstant().toEpochMilli(),
            zoneId = zoneId
        )
        val result = ZonedDateTime.ofInstant(java.time.Instant.ofEpochMilli(nextTrigger), zoneId)
        assertEquals(2026, result.year)
        assertEquals(9, result.monthValue)
        assertEquals(19, result.dayOfMonth)
        assertEquals(14, result.hour)
        assertEquals(30, result.minute)
    }

    @Test
    fun testOneTimeAlarmEarlierTodayRollsToTomorrow() {
        // Today is 10:00 AM, alarm set for 07:00 AM (already passed today)
        val now = ZonedDateTime.of(2026, 9, 19, 10, 0, 0, 0, zoneId)
        val nextTrigger = AlarmTimeCalculator.calculateNextTriggerTime(
            hour = 7,
            minute = 0,
            repeatDaysMask = 0,
            nowEpochMs = now.toInstant().toEpochMilli(),
            zoneId = zoneId
        )
        val result = ZonedDateTime.ofInstant(java.time.Instant.ofEpochMilli(nextTrigger), zoneId)
        assertEquals(20, result.dayOfMonth) // Tomorrow
        assertEquals(7, result.hour)
        assertEquals(0, result.minute)
    }

    @Test
    fun testWeekdayOnlyAlarmFromFridayAfternoonRollsToMonday() {
        // 2026-09-18 was a Friday. 16:00 PM.
        // Alarm set for 08:00 AM on Weekdays (Mon-Fri mask = 1 + 2 + 4 + 8 + 16 = 31)
        val fridayAfternoon = ZonedDateTime.of(2026, 9, 18, 16, 0, 0, 0, zoneId)
        assertEquals(DayOfWeek.FRIDAY, fridayAfternoon.dayOfWeek)

        val weekdaysMask = AlarmTimeCalculator.WEEKDAYS_MASK // 31
        val nextTrigger = AlarmTimeCalculator.calculateNextTriggerTime(
            hour = 8,
            minute = 0,
            repeatDaysMask = weekdaysMask,
            nowEpochMs = fridayAfternoon.toInstant().toEpochMilli(),
            zoneId = zoneId
        )
        val result = ZonedDateTime.ofInstant(java.time.Instant.ofEpochMilli(nextTrigger), zoneId)
        assertEquals(DayOfWeek.MONDAY, result.dayOfWeek)
        assertEquals(21, result.dayOfMonth) // Friday 18 -> Mon 21
        assertEquals(8, result.hour)
        assertEquals(0, result.minute)
    }

    @Test
    fun testEverydayAlarmCalculatesCorrectly() {
        val everydayMask = AlarmTimeCalculator.EVERYDAY_MASK // 127
        val now = ZonedDateTime.of(2026, 9, 19, 6, 0, 0, 0, zoneId)
        val nextTrigger = AlarmTimeCalculator.calculateNextTriggerTime(
            hour = 7,
            minute = 0,
            repeatDaysMask = everydayMask,
            nowEpochMs = now.toInstant().toEpochMilli(),
            zoneId = zoneId
        )
        val result = ZonedDateTime.ofInstant(java.time.Instant.ofEpochMilli(nextTrigger), zoneId)
        assertEquals(19, result.dayOfMonth)
        assertEquals(7, result.hour)
    }

    @Test
    fun testFormatTimeRemaining() {
        val now = 1000_000L
        val trigger = now + (2 * 3600_000L) + (15 * 60_000L) // 2h 15m
        val formatted = AlarmTimeCalculator.formatTimeRemaining(trigger, now)
        assertTrue(formatted.contains("2 hours") || formatted.contains("2h"))
    }

    @Test
    fun testRepeatingAlarmCalculatesNextDayWhenTriggered() {
        // Today is Friday at 07:00:00.000 AM. Alarm triggers.
        val everydayMask = AlarmTimeCalculator.EVERYDAY_MASK
        val friday7am = ZonedDateTime.of(2026, 9, 18, 7, 0, 0, 0, zoneId)
        val nextTrigger = AlarmTimeCalculator.calculateNextOccurrenceAfter(
            hour = 7,
            minute = 0,
            repeatDaysMask = everydayMask,
            afterEpochMs = friday7am.toInstant().toEpochMilli(),
            zoneId = zoneId
        )
        val result = ZonedDateTime.ofInstant(java.time.Instant.ofEpochMilli(nextTrigger), zoneId)
        assertEquals(DayOfWeek.SATURDAY, result.dayOfWeek)
        assertEquals(19, result.dayOfMonth)
        assertEquals(7, result.hour)
        assertEquals(0, result.minute)
    }

    @Test
    fun testRepeatingAlarmWithEarlyClockDriftStillRollsToNextDay() {
        // Alarm fires 200ms before 07:00:00 AM (clock drift)
        val everydayMask = AlarmTimeCalculator.EVERYDAY_MASK
        val fridayEarly = ZonedDateTime.of(2026, 9, 18, 6, 59, 59, 800_000_000, zoneId)
        val nextTrigger = AlarmTimeCalculator.calculateNextOccurrenceAfter(
            hour = 7,
            minute = 0,
            repeatDaysMask = everydayMask,
            afterEpochMs = fridayEarly.toInstant().toEpochMilli(),
            zoneId = zoneId
        )
        val result = ZonedDateTime.ofInstant(java.time.Instant.ofEpochMilli(nextTrigger), zoneId)
        assertEquals(DayOfWeek.SATURDAY, result.dayOfWeek)
        assertEquals(19, result.dayOfMonth)
        assertEquals(7, result.hour)
    }
}
