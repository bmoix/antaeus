package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.exceptions.CurrencyMismatchException
import io.pleo.antaeus.core.exceptions.CustomerNotFoundException
import io.pleo.antaeus.core.exceptions.NetworkException
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import io.pleo.antaeus.models.Money
import io.pleo.antaeus.models.Periodicity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging

class BillingService(
        private val paymentProvider: PaymentProvider,
        private val invoiceService: InvoiceService,
        private val processingChannel: ReceiveChannel<Invoice>,
) {
    private val logger = KotlinLogging.logger {}

    // Starts the service by launching a worker to process all the incoming bills through the processingChannel.
    fun start() {
        logger.info { "Starting the Billing Service" }

        GlobalScope.launch {
            processInvoices()
        }
    }

    // Processes all the invoices coming from the processingChannel
    private suspend fun processInvoices(): Unit {
        for (invoice in processingChannel) {
            payInvoice(invoice)
        }
    }

    fun payInvoice(invoice: Invoice): Unit {
        logger.info { "Processing invoice '${invoice.id}' from customer '${invoice.customerId}'" }

        // invoices should be paid only ONCE
        if (markInvoiceAsProcessing(invoice.id) == 0) {
            logger.warn { "Invoice '${invoice.id}' from customer '${invoice.customerId} is already processed. Skipping..." }
            return
        }

        try {
            when (paymentProvider.charge(invoice)) {
                true -> {
                    logger.info { "Invoice '${invoice.id}' from customer '${invoice.customerId}' was paid SUCCESSFULLY"}
                    updateInvoice(invoice.id, invoice.customerId, invoice.amount, InvoiceStatus.PAID)
                }
                false -> {
                    logger.info { "Invoice '${invoice.id}' from customer '${invoice.customerId}' FAILED. Retrying..."}
                    retry()
                }
            }
        }
        catch (e: CustomerNotFoundException) {
            logger.error(e) { "Failed to pay invoice '${invoice.id} from customer '${invoice.customerId}" }
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
