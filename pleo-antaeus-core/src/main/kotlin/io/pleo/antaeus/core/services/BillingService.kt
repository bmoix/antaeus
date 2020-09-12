package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.exceptions.CurrencyMismatchException
import io.pleo.antaeus.core.exceptions.CustomerNotFoundException
import io.pleo.antaeus.core.exceptions.InvoiceAlreadyPaidException
import io.pleo.antaeus.core.exceptions.NetworkException
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import io.pleo.antaeus.models.Money

class BillingService(
    private val paymentProvider: PaymentProvider,
    private val invoiceService: InvoiceService
) {
    fun payInvoice(invoice: Invoice): Unit {
        if (invoice.status == InvoiceStatus.PAID) throw InvoiceAlreadyPaidException(invoice.id, invoice.customerId)

        try {
            when (paymentProvider.charge(invoice)) {
                true -> updateInvoice(invoice.id, invoice.customerId, invoice.amount, InvoiceStatus.PAID)
                false -> retry()
            }
        }
        catch (e: CustomerNotFoundException) {
            // TODO: mark as failed
        }
        catch (e: CurrencyMismatchException) {
            // TODO: change currency
        }
        catch (e: NetworkException) {
            // TODO: retry
        }
    }

    private fun updateInvoice(id: Int, customerId: Int, amount: Money, status: InvoiceStatus) {
        // TODO: handle DB failure
        val updated = invoiceService.updateInvoice(id, customerId, amount, status)
        if (updated != 1) {
            // TODO: failed to update DB, retry
            throw Exception("failed DB update")
        }
    }

    private fun retry() {
        // TODO: retry queue with exponential backoff
    }
}
