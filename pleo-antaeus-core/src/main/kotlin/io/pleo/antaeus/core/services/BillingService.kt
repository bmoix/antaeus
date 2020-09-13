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

/*
    BillingService processes payments of invoices.

    It launches a worker that listens to the processingChannel for new invoices to be paid.
    Failed invoices are sent to the retryChannel.
 */
class BillingService(
        private val paymentProvider: PaymentProvider,
        private val invoiceService: InvoiceService,
        private val exchangeService: ExchangeService,
        private val processingChannel: ReceiveChannel<BillingAttempt>,
        private val retryChannel: SendChannel<BillingAttempt>
) {
    private val logger = KotlinLogging.logger {}

    /*
        Starts the service by launching a worker to process all the incoming bills
        through the processingChannel.
     */
    fun start() {
        logger.info { "Starting the Billing Service" }

        GlobalScope.launch {
            processInvoices()
        }
    }

    /*
        Processes all the invoices coming from the processingChannel
     */
    private suspend fun processInvoices() {
        for (billingAttempt in processingChannel) {
            payInvoice(billingAttempt)
        }
    }

    /*
        Pays the invoice with the specified ID.
        If something goes wrong, updates the invoice status and sends it to retry if needed.
     */
    fun payInvoice(billingAttempt: BillingAttempt) {
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
                    retry(billingAttempt)
                }
            }
        }
        catch (e: CustomerNotFoundException) {
            logger.error { "Failed to pay invoice '${invoice.id} from customer '${invoice.customerId}'. ${e.message}" }
            updateInvoice(invoice.id, invoice.customerId, invoice.amount, InvoiceStatus.ERROR)
        }
        catch (e: CurrencyMismatchException) {
            logger.warn { "Failed to pay invoice '${invoice.id} from customer '${invoice.customerId}'. ${e.message}" }
            updateInvoice(invoice.id, invoice.customerId, invoice.amount, InvoiceStatus.CURRENCY_MISMATCH)
            exchangeService.fixInvoiceCurrency(invoice.id)
            retry(billingAttempt)
        }
        catch (e: NetworkException) {
            logger.warn { "Failed to pay invoice '${invoice.id} from customer '${invoice.customerId}'. ${e.message}" }
            updateInvoice(invoice.id, invoice.customerId, invoice.amount, InvoiceStatus.NETWORK_ERROR)
            retry(billingAttempt)
        }
    }

    /*
        Checks that the invoice is not being processed by someone else or is already paid
        and marks it as PROCESSING so no one will process it again.
     */
    private fun markInvoiceAsProcessing(id: Int): Int {
        return invoiceService.updateToProcessing(id)
    }

    /*
        Updates the invoice's values.
     */
    private fun updateInvoice(id: Int, customerId: Int, amount: Money, status: InvoiceStatus) {
        val updated = invoiceService.update(id, customerId, amount, status)
        if (updated != 1) {
            throw Exception("failed DB update")
        }
    }

    /*
        Sends the billing attempt to the scheduler to be retried.
     */
    private fun retry(billingAttempt: BillingAttempt) {
        GlobalScope.launch {
            retryChannel.send(BillingAttempt(billingAttempt.invoiceId, billingAttempt.numAttempts + 1))
        }
    }
}
