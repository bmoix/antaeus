package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.exceptions.CurrencyMismatchException
import io.pleo.antaeus.core.exceptions.CustomerNotFoundException
import io.pleo.antaeus.core.exceptions.NetworkException
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import io.pleo.antaeus.models.Money
import mu.KotlinLogging

class BillingService(
    private val paymentProvider: PaymentProvider,
    private val invoiceService: InvoiceService
) {
    private val logger = KotlinLogging.logger {}

    fun payInvoice(invoice: Invoice): Unit {
        logger.debug { "Processing invoice '${invoice.id}' from customer '${invoice.customerId}'" }

        // invoices should be paid only ONCE
        if (markInvoiceAsProcessing(invoice.id) == 0) {
            logger.warn { "Invoice '${invoice.id}' from customer '${invoice.customerId} is already processed. Skipping..." }
            return
        }

        try {
            when (paymentProvider.charge(invoice)) {
                true -> updateInvoice(invoice.id, invoice.customerId, invoice.amount, InvoiceStatus.PAID)
                false -> retry()
            }
        }
        catch (e: CustomerNotFoundException) {
            logger.error { "Failed to pay invoice '${invoice.id} from customer '${invoice.customerId}. ${e.message}" }
            updateInvoice(invoice.id, invoice.customerId, invoice.amount, InvoiceStatus.ERROR)
        }
        catch (e: CurrencyMismatchException) {
            // TODO: change currency
        }
        catch (e: NetworkException) {
            // TODO: retry
        }
    }

    private fun markInvoiceAsProcessing(id: Int): Int {
        return invoiceService.updateToProcessing(id)
    }

    private fun updateInvoice(id: Int, customerId: Int, amount: Money, status: InvoiceStatus) {
        // TODO: handle DB failure
        val updated = invoiceService.update(id, customerId, amount, status)
        if (updated != 1) {
            // TODO: failed to update DB, retry
            throw Exception("failed DB update")
        }
    }

    private fun retry() {
        // TODO: retry queue with exponential backoff
    }
}
