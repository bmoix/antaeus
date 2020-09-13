package io.pleo.antaeus.core.services

import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import io.pleo.antaeus.models.Periodicity
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import mu.KotlinLogging
import java.time.*
import java.time.format.DateTimeFormatter.ISO_ZONED_DATE_TIME

/*
    SchedulingService main responsibility is to decide when an invoice has to be paid.
 */
class SchedulingService(
        private val invoiceService: InvoiceService,
        private val processingChannel: SendChannel<Invoice>,
        private val retryChannel: Channel<Invoice>
) {
    private val logger = KotlinLogging.logger {}

    // Starts the service by launching a worker to schedule the bills to be paid with a specified periodicity.
    fun start(periodicity: Periodicity) {
        logger.info { "Starting the Scheduling Service with a ${periodicity.toString()} periodicity" }

        GlobalScope.launch {
            scheduleBills(periodicity)
        }
        GlobalScope.launch {
            scheduleRetries()
        }
    }

    private suspend fun scheduleBills(periodicity: Periodicity) {
        // TODO: add more control of the loop
        while (true) {
            val currTime = ZonedDateTime.now(ZoneId.of("UTC"))
            val timeToSleep = timeUntilNextPeriod(currTime, periodicity)
            val timeToWakeUp = currTime.plusNanos(timeToSleep * 1000000L)
            logger.info { "Going to sleep for $timeToSleep ms until ${timeToWakeUp.format(ISO_ZONED_DATE_TIME)}..." }
            delay(timeToSleep)
            logger.info { "Waking up" }

            val invoices = invoiceService.fetchByStatus(InvoiceStatus.PENDING)
            logger.info { "Invoices to be scheduled: ${invoices.size}" }
            for (invoice in invoices) {
                processingChannel.send(invoice)
            }
        }
    }

    private suspend fun scheduleRetries() {
        for (retry in retryChannel) {
            logger.info { "Scheduling retry for invoice '${retry.id}'" }
            processingChannel.send(retry)
        }
    }
}