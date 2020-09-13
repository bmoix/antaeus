package io.pleo.antaeus.core.services

import io.pleo.antaeus.models.InvoiceStatus
import io.pleo.antaeus.models.Periodicity
import kotlinx.coroutines.*
import mu.KotlinLogging
import java.time.*
import java.time.format.DateTimeFormatter.ISO_ZONED_DATE_TIME

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
            val currTime = ZonedDateTime.now(ZoneId.of("UTC"))
            val timeToSleep = timeUntilNextPeriod(currTime, periodicity)
            val timeToWakeUp = currTime.plusNanos(timeToSleep * 1000000L)
            logger.debug { "Going to sleep for $timeToSleep ms until ${timeToWakeUp.format(ISO_ZONED_DATE_TIME)}..." }
            delay(timeToSleep)
            logger.debug { "Waking up" }

            val invoices = invoiceService.fetchByStatus(InvoiceStatus.PENDING)
            logger.info { "Invoices to be scheduled: ${invoices.size}" }
            for (invoice in invoices) {
                // TODO: put in a queue
                billingService.payInvoice(invoice)
            }
        }
    }
}