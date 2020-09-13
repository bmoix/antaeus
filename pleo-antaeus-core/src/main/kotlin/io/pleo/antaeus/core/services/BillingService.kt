package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.exceptions.CurrencyMismatchException
import io.pleo.antaeus.core.exceptions.CustomerNotFoundException
import io.pleo.antaeus.core.exceptions.NetworkException
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.models.BillingAttempt
import io.pleo.antaeus.models.InvoiceStatus
import io.pleo.antaeus.models.Money
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.launch
import mu.KotlinLogging

class BillingService(
        private val paymentProvider: PaymentProvider,
        private val invoiceService: InvoiceService,
        private val processingChannel: ReceiveChannel<BillingAttempt>,
        private val retryChannel: SendChannel<BillingAttempt>
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
        for (billingAttempt in processingChannel) {
            payInvoice(billingAttempt)
        }
    }

    fun payInvoice(billingAttempt: BillingAttempt): Unit {
        logger.info { "Processing invoice '${billingAttempt.invoiceId}'" }

        // invoices should be paid only ONCE
        if (markInvoiceAsProcessing(billingAttempt.invoiceId) == 0) {
            logger.warn { "Invoice '${billingAttempt.invoiceId}' is already processed. Skipping..." }
            return
        }
        val invoice = invoiceService.fetch(billingAttempt.invoiceId)

        try {
            when (paymentProvider.charge(invoice)) {
                true -> {
                    logger.info { "Invoice '${invoice.id}' from customer '${invoice.customerId}' was paid SUCCESSFULLY"}
                    updateInvoice(invoice.id, invoice.customerId, invoice.amount, InvoiceStatus.PAID)
                }
                false -> {
                    logger.warn { "Invoice '${invoice.id}' from customer '${invoice.customerId}' FAILED. Retrying..."}
                    updateInvoice(invoice.id, invoice.customerId, invoice.amount, InvoiceStatus.INSUFFICIENT_FUNDS)
                    retry(BillingAttempt(billingAttempt.invoiceId, billingAttempt.numAttempts + 1))
                }
            }
        }
        catch (e: CustomerNotFoundException) {
            logger.error(e) { "Failed to pay invoice '${invoice.id} from customer '${invoice.customerId}" }
            updateInvoice(invoice.id, invoice.customerId, invoice.amount, InvoiceStatus.ERROR)
        }
        catch (e: CurrencyMismatchException) {
            logger.error(e) { "Failed to pay invoice '${invoice.id} from customer '${invoice.customerId}" }
            updateInvoice(invoice.id, invoice.customerId, invoice.amount, InvoiceStatus.CURRENCY_MISMATCH)
            // TODO: change currency
        }
        catch (e: NetworkException) {
            logger.error(e) { "Failed to pay invoice '${invoice.id} from customer '${invoice.customerId}" }
            updateInvoice(invoice.id, invoice.customerId, invoice.amount, InvoiceStatus.NETWORK_ERROR)
            retry(BillingAttempt(billingAttempt.invoiceId, billingAttempt.numAttempts + 1))
        }
    }

    private fun markInvoiceAsProcessing(id: Int): Int {
        return invoiceService.updateToProcessing(id)
    }

    private fun updateInvoice(id: Int, customerId: Int, amount: Money, status: InvoiceStatus): Unit {
        // TODO: handle DB failure
        val updated = invoiceService.update(id, customerId, amount, status)
        if (updated != 1) {
            // TODO: failed to update DB, retry
            throw Exception("failed DB update")
        }
    }

    private fun retry(billingAttempt: BillingAttempt): Unit {
        GlobalScope.launch {
            retryChannel.send(billingAttempt)
        }
    }
}
