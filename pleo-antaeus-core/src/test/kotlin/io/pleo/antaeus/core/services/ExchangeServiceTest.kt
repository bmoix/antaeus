package io.pleo.antaeus.core.services

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.pleo.antaeus.core.external.CurrencyExchangeProvider
import io.pleo.antaeus.models.*
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class ExchangeServiceTest {
    private val invoice = Invoice(
            1,
            42,
            Money(BigDecimal.valueOf(10.0), Currency.EUR),
            InvoiceStatus.PROCESSING
    )

    private val currencyExchangeProvider = mockk<CurrencyExchangeProvider> {
        every { getRate(Currency.EUR, Currency.DKK) } returns BigDecimal.valueOf(7.5)
    }

    private val customerService = mockk<CustomerService> {
        every { fetch(42) } returns Customer(42, Currency.DKK)
    }

    private val invoiceService = mockk<InvoiceService> {
        every { fetch(1) } returns invoice
        every { update(1, 42, any(), any()) } returns 1
    }

    private val exchangeService = ExchangeService(customerService, invoiceService, currencyExchangeProvider)

    @Test
    fun `exchange succeeds`() {
        val expected = BigDecimal("75.00")
        val actual = exchangeService.exchange(BigDecimal("10.00"), Currency.EUR, Currency.DKK)

        assertTrue(expected.compareTo(actual) == 0)

        verify(exactly = 1) {
            currencyExchangeProvider.getRate(Currency.EUR, Currency.DKK)
        }
    }

    @Test
    fun `fixInvoiceCurrency succeeds`() {
        exchangeService.fixInvoiceCurrency(invoice.id)

        verify(exactly = 1) {
            invoiceService.fetch(invoice.id)
            customerService.fetch(invoice.customerId)
            currencyExchangeProvider.getRate(Currency.EUR, Currency.DKK)
            invoiceService.update(invoice.id, invoice.customerId, Money(BigDecimal("75.00"), Currency.DKK), invoice.status)
        }
    }
}