package io.pleo.antaeus.core.services

import io.mockk.mockk
import io.pleo.antaeus.models.BillingAttempt
import kotlinx.coroutines.channels.Channel

class SchedulingServiceTest {
    val invoiceService = mockk<InvoiceService> {}
    val processingChannel = mockk<Channel<BillingAttempt>> {}
    val retryChannel = mockk<Channel<BillingAttempt>> {}

    val schedulingService = SchedulingService(invoiceService, processingChannel, retryChannel)
}
