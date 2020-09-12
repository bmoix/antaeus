package io.pleo.antaeus.core.services

import io.pleo.antaeus.models.InvoiceStatus
import io.pleo.antaeus.models.Periodicity
import kotlinx.coroutines.*
import mu.KotlinLogging

/*
    SchedulingService main responsibility is to decide when an invoice has to be paid.
 */
class SchedulingService(
        private val invoiceService: InvoiceService,
        private val billingService: BillingService
) {
    private val logger = KotlinLogging.logger {}

    // Starts the service by launching a worker to schedule the bills to be paid with a specified periodicity.
    fun start(periodicity: Periodicity) = runBlocking<Unit> {
        logger.info { "Starting the Scheduling Service with a ${periodicity.toString()} periodicity" }

        launch {
            scheduleBills(periodicity)
        }
    }

    private suspend fun scheduleBills(periodicity: Periodicity) {
        // TODO: add more control of the loop
        while (true) {
            val t = timeUntilNextPeriod(periodicity)
            logger.debug { "Going to sleep for $t ms..." }
            delay(t)
            logger.debug { "Waking up" }

            val invoices = invoiceService.fetchByStatus(InvoiceStatus.PENDING)
            logger.info { "Invoices to be scheduled: ${invoices.size}" }
            for (invoice in invoices) {
                // TODO: put in a queue
                billingService.payInvoice(invoice)
            }
        }
    }

    fun timeUntilNextPeriod(periodicity: Periodicity): Long {
        // TODO: implement
        return when (periodicity) {
            Periodicity.HOURLY -> 1000L
            Periodicity.DAILY -> 2000L
            Periodicity.WEEKLY -> 3000L
            Periodicity.MONTHLY -> 4000L
        }
    }
}