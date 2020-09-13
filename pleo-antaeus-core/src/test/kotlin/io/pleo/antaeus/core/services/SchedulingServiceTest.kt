package io.pleo.antaeus.core.services

import io.mockk.mockk

class SchedulingServiceTest {
    val invoiceService = mockk<InvoiceService> {}
    val billingService = mockk<BillingService> {}
    val schedulingService = SchedulingService(invoiceService, billingService)
}
