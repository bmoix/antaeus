package io.pleo.antaeus.core.services

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.pleo.antaeus.core.exceptions.CustomerNotFoundException
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.models.Currency
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import io.pleo.antaeus.models.Money
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
            InvoiceStatus.PENDING
    )

    private val paymentProvider = mockk<PaymentProvider> {
        every { charge(any()) } returns true
    }
    private val invoiceService = mockk<InvoiceService> {
        every { updateToProcessing(invoice.id) } returns 1
        every { update(invoice.id, invoice.customerId, invoice.amount, any()) } returns 1
    }
    private val processingChannel = mockk<Channel<Invoice>> {}

    private val billingService = BillingService(paymentProvider, invoiceService, processingChannel)

    @Test
    fun `payInvoice succeeds`() {
        billingService.payInvoice(invoice)

        verify(exactly = 1) {
            paymentProvider.charge(invoice)
            invoiceService.updateToProcessing(invoice.id)
            invoiceService.update(invoice.id, invoice.customerId, invoice.amount, InvoiceStatus.PAID)
        }
    }

    @Test
    fun `payInvoice skips invoice already paid`() {
        val invoiceServicePaid = mockk<InvoiceService> {
            every { updateToProcessing(invoice.id) } returns 0
        }
        val service = BillingService(paymentProvider, invoiceServicePaid, processingChannel)
        service.payInvoice(invoice)

        verify(exactly = 0) {
            paymentProvider.charge(invoice)
        }
    }

    @Test
    fun `payInvoice fails with customer not found`() {
        val paymentProviderFail = mockk<PaymentProvider> {
            every { charge(any()) } throws CustomerNotFoundException(invoice.customerId)
        }
        val service = BillingService(paymentProviderFail, invoiceService, processingChannel)

        service.payInvoice(invoice)

        verify(exactly = 1) {
            invoiceService.updateToProcessing(invoice.id)
            paymentProviderFail.charge(invoice)
            invoiceService.update(invoice.id, invoice.customerId, invoice.amount, InvoiceStatus.ERROR)
        }
    }

    @Test
    fun `payInvoice fails updating DB after charge`() {
        val invoiceService = mockk<InvoiceService> {
            every { updateToProcessing(invoice.id) } returns 1
            every { update(invoice.id, invoice.customerId, invoice.amount, InvoiceStatus.PAID) } returns 0
        }
        val service = BillingService(paymentProvider, invoiceService, processingChannel)

        val exception = assertThrows<Exception> {
            service.payInvoice(invoice)
        }
        assertEquals("failed DB update", exception.message)
    }
}