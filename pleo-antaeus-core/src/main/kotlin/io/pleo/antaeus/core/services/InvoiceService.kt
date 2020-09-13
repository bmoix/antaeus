/*
    Implements endpoints related to invoices.
 */

package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.exceptions.InvoiceNotFoundException
import io.pleo.antaeus.data.AntaeusDal
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import io.pleo.antaeus.models.Money

class InvoiceService(private val dal: AntaeusDal) {
    fun fetchAll(): List<Invoice> {
        return dal.fetchInvoices()
    }

    fun fetch(id: Int): Invoice {
        return dal.fetchInvoice(id) ?: throw InvoiceNotFoundException(id)
    }

    fun fetchByStatus(status: InvoiceStatus): List<Invoice> {
        return dal.fetchInvoicesByStatus(status)
    }

    fun fetchByMultipleStatus(statuses: List<InvoiceStatus>): List<Invoice> {
        return dal.fetchInvoicesByMultipleStatus(statuses)
    }

    fun update(id: Int, customerId: Int, amount: Money, status: InvoiceStatus): Int {
        return dal.updateInvoice(id, customerId, amount, status)
    }

    fun updateToProcessing(id: Int): Int {
        return dal.updateInvoiceToProcessing(id)
    }
}
