package io.pleo.antaeus.core.services

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.pleo.antaeus.core.exceptions.CustomerNotFoundException
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.models.*
import kotlinx.coroutines.channels.Channel
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.lang.Exception
import java.math.BigDecimal

class BillingServiceTest {
    private val invoice = Invoice(
            1,
            42,
            Money(BigDecimal.valueOf(10.0), Currency.EUR),
            InvoiceStatus.PROCESSING
    )
    private val billingAttempt = BillingAttempt(invoice.id, 0)

    private val paymentProvider = mockk<PaymentProvider> {
        every { charge(any()) } returns true
    }

    private val invoiceService = mockk<InvoiceService> {
        every { updateToProcessing(invoice.id) } returns 1
        every { fetch(invoice.id) } returns invoice
        every { update(invoice.id, invoice.customerId, invoice.amount, any()) } returns 1
    }
    private val exchangeService = mockk<ExchangeService> {
        every { fixInvoiceCurrency(any()) } returns Unit
    }

    private val processingChannel = mockk<Channel<BillingAttempt>> {}
    private val retryChannel = mockk<Channel<BillingAttempt>> {}

    private val billingService = BillingService(
            paymentProvider, invoiceService, exchangeService, processingChannel, retryChannel)

    @Test
    fun `payInvoice succeeds`() {
        billingService.payInvoice(billingAttempt)

        verify(exactly = 1) {
            invoiceService.updateToProcessing(invoice.id)
            invoiceService.fetch(invoice.id)
            paymentProvider.charge(invoice)
            invoiceService.update(invoice.id, invoice.customerId, invoice.amount, InvoiceStatus.PAID)
        }
    }

    @Test
    fun `payInvoice skips invoice already paid`() {
        val invoiceServicePaid = mockk<InvoiceService> {
            every { updateToProcessing(invoice.id) } returns 0
        }
        val service = BillingService(paymentProvider, invoiceServicePaid, exchangeService, processingChannel, retryChannel)
        service.payInvoice(billingAttempt)

        verify(exactly = 0) {
            invoiceService.fetch(invoice.id)
            paymentProvider.charge(invoice)
        }
    }

    @Test
    fun `payInvoice fails with customer not found`() {
        val paymentProviderFail = mockk<PaymentProvider> {
            every { charge(any()) } throws CustomerNotFoundException(invoice.customerId)
        }
        val service = BillingService(paymentProviderFail, invoiceService, exchangeService, processingChannel, retryChannel)

        service.payInvoice(billingAttempt)

        verify(exactly = 1) {
            invoiceService.updateToProcessing(invoice.id)
            invoiceService.fetch(invoice.id)
            paymentProviderFail.charge(invoice)
            invoiceService.update(invoice.id, invoice.customerId, invoice.amount, InvoiceStatus.ERROR)
        }
    }

    @Test
    fun `payInvoice fails updating DB after charge`() {
        val invoiceServiceFail = mockk<InvoiceService> {
            every { updateToProcessing(invoice.id) } returns 1
            every { fetch(invoice.id) } returns invoice
            every { update(invoice.id, invoice.customerId, invoice.amount, InvoiceStatus.PAID) } returns 0
        }
        val service = BillingService(paymentProvider, invoiceServiceFail, exchangeService, processingChannel, retryChannel)

        val exception = assertThrows<Exception> {
            service.payInvoice(billingAttempt)
        }
        assertEquals("failed DB update", exception.message)

        verify(exactly = 1) {
            invoiceServiceFail.updateToProcessing(invoice.id)
            invoiceServiceFail.fetch(invoice.id)
            paymentProvider.charge(invoice)
            invoiceServiceFail.update(invoice.id, invoice.customerId, invoice.amount, InvoiceStatus.PAID)
        }
    }
}