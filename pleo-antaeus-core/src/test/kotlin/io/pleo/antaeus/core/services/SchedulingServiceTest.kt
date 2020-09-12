package io.pleo.antaeus.core.services

import io.mockk.mockk
import io.pleo.antaeus.models.Periodicity
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SchedulingServiceTest {
    val invoiceService = mockk<InvoiceService> {}
    val billingService = mockk<BillingService> {}
    val schedulingService = SchedulingService(invoiceService, billingService)

    @Test
    fun `timeUntilNextPeriod succeeds`() {
        assertEquals(1000L, schedulingService.timeUntilNextPeriod(Periodicity.HOURLY))
        assertEquals(2000L, schedulingService.timeUntilNextPeriod(Periodicity.DAILY))
        assertEquals(3000L, schedulingService.timeUntilNextPeriod(Periodicity.WEEKLY))
        assertEquals(4000L, schedulingService.timeUntilNextPeriod(Periodicity.MONTHLY))
    }
}
