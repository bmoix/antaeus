package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.exceptions.InvoiceAlreadyPaidException
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.data.AntaeusDal
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus

class BillingService(
    private val paymentProvider: PaymentProvider,
    private val dal: AntaeusDal
) {
    fun payInvoice(invoice: Invoice): Unit {
        if (invoice.status == InvoiceStatus.PAID) throw InvoiceAlreadyPaidException(invoice.id, invoice.customerId)

        val success = paymentProvider.charge(invoice)
        if (success) {
            // money was charged, we can't fail here
            val count = dal.updateInvoiceStatus(invoice.id, InvoiceStatus.PAID)
            if (count != 1) {
                // TODO: failed to update DB, retry
                throw Exception("failed DB update")
            }
        } else {
            // TODO: error handling and retry
            throw Exception("failed charge")
        }
    }
}
