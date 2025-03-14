/*
    Implements the data access layer (DAL).
    The data access layer generates and executes requests to the database.

    See the `mappings` module for the conversions between database rows and Kotlin objects.
 */

package io.pleo.antaeus.data

import io.pleo.antaeus.models.Currency
import io.pleo.antaeus.models.Customer
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import io.pleo.antaeus.models.Money
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

class AntaeusDal(private val db: Database) {
    fun fetchInvoice(id: Int): Invoice? {
        // transaction(db) runs the internal query as a new database transaction.
        return transaction(db) {
            // Returns the first invoice with matching id.
            InvoiceTable
                .select { InvoiceTable.id.eq(id) }
                .firstOrNull()
                ?.toInvoice()
        }
    }

    fun fetchInvoices(): List<Invoice> {
        return transaction(db) {
            InvoiceTable
                .selectAll()
                .map { it.toInvoice() }
        }
    }

    fun fetchInvoicesByStatus(status: InvoiceStatus): List<Invoice> {
        return transaction(db) {
            InvoiceTable
                    .select{ InvoiceTable.status.eq(status.toString()) }
                    .map { it.toInvoice() }
        }
    }

    fun fetchInvoicesByMultipleStatus(statuses: List<InvoiceStatus>): List<Invoice> {
        val statusesString = statuses.map { it.toString() }
        return transaction(db) {
            InvoiceTable
                    .select{ InvoiceTable.status.inList(statusesString) }
                    .map { it.toInvoice() }
        }
    }

    fun createInvoice(amount: Money, customer: Customer, status: InvoiceStatus = InvoiceStatus.PENDING): Invoice? {
        val id = transaction(db) {
            // Insert the invoice and returns its new id.
            InvoiceTable
                .insert {
                    it[this.value] = amount.value
                    it[this.currency] = amount.currency.toString()
                    it[this.status] = status.toString()
                    it[this.customerId] = customer.id
                } get InvoiceTable.id
        }

        return fetchInvoice(id)
    }

    fun updateInvoice(id: Int, customerId: Int, amount: Money, status: InvoiceStatus): Int {
        return transaction(db) {
            InvoiceTable
                    .update({ InvoiceTable.id.eq(id) and InvoiceTable.customerId.eq(customerId) }) {
                        it[this.value] = amount.value
                        it[this.currency] = amount.currency.toString()
                        it[this.status] = status.toString()
                    }
        }
    }

    /*
        Updates an Invoice to a PROCESSING status.
        Invoices in PAID or PROCESSING status can't be processed.
     */
    fun updateInvoiceToProcessing(id: Int): Int {
        return transaction(db) {
            InvoiceTable
                    .update({
                        InvoiceTable.id.eq(id) and
                                InvoiceTable.status.neq(InvoiceStatus.PAID.toString()) and
                                InvoiceTable.status.neq(InvoiceStatus.PROCESSING.toString())
                    }) {
                        it[this.status] = InvoiceStatus.PROCESSING.toString()
                    }
        }
    }

    fun fetchCustomer(id: Int): Customer? {
        return transaction(db) {
            CustomerTable
                .select { CustomerTable.id.eq(id) }
                .firstOrNull()
                ?.toCustomer()
        }
    }

    fun fetchCustomers(): List<Customer> {
        return transaction(db) {
            CustomerTable
                .selectAll()
                .map { it.toCustomer() }
        }
    }

    fun createCustomer(currency: Currency): Customer? {
        val id = transaction(db) {
            // Insert the customer and return its new id.
            CustomerTable.insert {
                it[this.currency] = currency.toString()
            } get CustomerTable.id
        }

        return fetchCustomer(id)
    }
}
