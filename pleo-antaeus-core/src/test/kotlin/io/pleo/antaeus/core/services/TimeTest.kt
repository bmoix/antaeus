package io.pleo.antaeus.core.services

import io.pleo.antaeus.models.Periodicity
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime

class TimeTest {
    private data class TestCase(
            val expected: Long,
            val time: ZonedDateTime
    )

    @Test
    fun `timeUntilNextPeriod hourly`() {
        val cases = listOf<TestCase>(
                TestCase(2700000L, ZonedDateTime.parse("2020-09-13T10:15:00+00:00[UTC]")),
                TestCase(3600000L, ZonedDateTime.parse("2020-09-13T10:00:00+00:00[UTC]")),
                TestCase(1000L, ZonedDateTime.parse("2020-09-13T10:59:59+00:00[UTC]")),
        )

        for (case in cases) {
            assertEquals(case.expected, timeUntilNextPeriod(case.time, Periodicity.HOURLY))
        }
    }

    @Test
    fun `timeUntilNextPeriod daily`() {
        val cases = listOf<TestCase>(
                TestCase(49500000L, ZonedDateTime.parse("2020-09-13T10:15:00+00:00[UTC]")),
                TestCase(300000L, ZonedDateTime.parse("2020-09-13T23:55:00+00:00[UTC]")),
                TestCase(86400000L, ZonedDateTime.parse("2020-09-13T00:00:00+00:00[UTC]")),
        )

        for (case in cases) {
            assertEquals(case.expected, timeUntilNextPeriod(case.time, Periodicity.DAILY))
        }
    }

    @Test
    fun `timeUntilNextPeriod weekly`() {
        val cases = listOf<TestCase>(
                TestCase(49500000L, ZonedDateTime.parse("2020-09-13T10:15:00+00:00[UTC]")),
                TestCase(86700000L, ZonedDateTime.parse("2020-09-12T23:55:00+00:00[UTC]")),
                TestCase(604800000L, ZonedDateTime.parse("2020-09-14T00:00:00+00:00[UTC]")),
        )

        for (case in cases) {
            assertEquals(case.expected, timeUntilNextPeriod(case.time, Periodicity.WEEKLY))
        }
    }

    @Test
    fun `timeUntilNextPeriod monthly`() {
        val cases = listOf<TestCase>(
                TestCase(1518300000L, ZonedDateTime.parse("2020-09-13T10:15:00+00:00[UTC]")),
                TestCase(300000L, ZonedDateTime.parse("2020-10-31T23:55:00+00:00[UTC]")),
                TestCase(172800000L, ZonedDateTime.parse("2020-12-30T00:00:00+00:00[UTC]")),
        )

        for (case in cases) {
            assertEquals(case.expected, timeUntilNextPeriod(case.time, Periodicity.MONTHLY))
        }
    }
}