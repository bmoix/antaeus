package io.pleo.antaeus.core.services

import io.mockk.mockk
import io.pleo.antaeus.models.Invoice
import kotlinx.coroutines.channels.Channel

class SchedulingServiceTest {
    val invoiceService = mockk<InvoiceService> {}
    val processingChannel = mockk<Channel<Invoice>> {}
    val retryChannel = mockk<Channel<Invoice>> {}

    val schedulingService = SchedulingService(invoiceService, processingChannel, retryChannel)
}
