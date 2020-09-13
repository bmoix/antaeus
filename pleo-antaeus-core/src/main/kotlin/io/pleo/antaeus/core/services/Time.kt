package io.pleo.antaeus.core.services

import io.pleo.antaeus.models.Periodicity
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

/*
    Returns the time in milliseconds from the given time until the next period.

    The next period starts at the beginning of the period, eg the next HOURLY period
    starts at the next hour with the minutes and second set to zero (10:30:15 -> 11:00:00).
    The same rule applies for all the valid periods.
 */
fun timeUntilNextPeriod(time: ZonedDateTime, periodicity: Periodicity): Long {
    var nextTime =  when (periodicity) {
        Periodicity.HOURLY -> {
            time
                .plusHours(1)
        }
        Periodicity.DAILY -> {
            time
                .plusDays(1)
                .withHour(0)
        }
        Periodicity.WEEKLY -> {
            val oneWeekPlusMonday = 8L
            val daysUntilNextWeek = oneWeekPlusMonday - time.dayOfWeek.value
            time
                .plusDays(daysUntilNextWeek)
                .withHour(0)
        }
        Periodicity.MONTHLY -> {
            time
                .plusMonths(1)
                .withDayOfMonth(1)
                .withHour(0)
        }
    }
    nextTime = nextTime
            .withMinute(0)
            .withSecond(0)
            .withNano(0)
    return time.until(nextTime, ChronoUnit.MILLIS)
}