package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.external.CurrencyExchangeProvider
import io.pleo.antaeus.models.Currency
import io.pleo.antaeus.models.Money
import mu.KotlinLogging
import java.lang.Exception
import java.math.BigDecimal

/*
    ExchangeService computes exchanges between currencies.
 */
class ExchangeService(
        private val customerService: CustomerService,
        private val invoiceService: InvoiceService,
        private val currencyExchangeProvider: CurrencyExchangeProvider
) {
    private val logger = KotlinLogging.logger {}

    /*
        Computes the exchange between the source and destination currency.
     */
    fun exchange(value: BigDecimal, source: Currency, destination: Currency): BigDecimal {
        try {
            val rate = currencyExchangeProvider.getRate(source, destination)
            return value * rate
        }
        catch (e: Exception) {
            logger.error(e) { "Failed to exchange '$value' from '$source' to '$destination'" }
            throw e
        }
    }

    /*
        Fixes the invoice's currency to match the customer's currency and exchanges its value.
     */
    fun fixInvoiceCurrency(invoiceId: Int) {
        logger.info { "Fixing currency of invoice '$invoiceId'" }
        val invoice = invoiceService.fetch(invoiceId)
        val customer = customerService.fetch(invoice.customerId)
        if (invoice.amount.currency == customer.currency) {
            logger.warn { "Invoice '$invoiceId' has the correct currency '${customer.currency}'. Skipping..." }
            return
        }

        val newAmount = exchange(invoice.amount.value, invoice.amount.currency, customer.currency)
        invoiceService.update(invoice.id, invoice.customerId, Money(newAmount, customer.currency), invoice.status)
        logger.info { "Exchange: ${invoice.amount.value} ${invoice.amount.currency} -> $newAmount ${customer.currency}" }
    }
}