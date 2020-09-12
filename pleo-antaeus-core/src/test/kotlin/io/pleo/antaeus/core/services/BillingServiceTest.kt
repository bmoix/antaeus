package io.pleo.antaeus.core.services

import io.mockk.every
import io.mockk.mockk
import io.pleo.antaeus.core.exceptions.InvoiceAlreadyPaidException
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.data.AntaeusDal
import io.pleo.antaeus.models.Currency
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import io.pleo.antaeus.models.Money
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
        every { updateInvoice(invoice.id, invoice.customerId, invoice.amount, InvoiceStatus.PAID) } returns 1
    }

    private val billingService = BillingService(paymentProvider, invoiceService)

    @Test
    fun `payInvoice succeeds`() {
        billingService.payInvoice(invoice)
    }

    @Test
    fun `payInvoice fails with invoice already paid`() {
        val invoicePaid = Invoice(
                2,
                42,
                Money(BigDecimal.valueOf(100.0), Currency.EUR),
                InvoiceStatus.PAID
        )

        assertThrows<InvoiceAlreadyPaidException> {
            billingService.payInvoice(invoicePaid)
        }
    }

    @Test
    fun `payInvoice fails updating DB after charge`() {
        val invoiceService = mockk<InvoiceService> {
            every { updateInvoice(invoice.id, invoice.customerId, invoice.amount, InvoiceStatus.PAID) } returns 0
        }
        val service = BillingService(paymentProvider, invoiceService)

        val exception = assertThrows<Exception> {
            service.payInvoice(invoice)
        }
        assertEquals("failed DB update", exception.message)
    }
}