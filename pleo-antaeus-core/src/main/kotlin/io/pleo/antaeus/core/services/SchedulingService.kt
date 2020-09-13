package io.pleo.antaeus.core.services

import io.pleo.antaeus.models.BillingAttempt
import io.pleo.antaeus.models.InvoiceStatus
import io.pleo.antaeus.models.Periodicity
import io.pleo.antaeus.models.SchedulingConfig
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mu.KotlinLogging
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter.ISO_ZONED_DATE_TIME

/*
    SchedulingService schedules when the invoices should be paid.
 */
class SchedulingService(
        private val invoiceService: InvoiceService,
        private val processingChannel: SendChannel<BillingAttempt>,
        private val retryChannel: Channel<BillingAttempt>,
        private val config: SchedulingConfig,
) {
    private val logger = KotlinLogging.logger {}

    /*
        Starts the service by launching 3 different workers:
            1. Initial scheduling of failed invoices by external factors.
            2. Main worker that schedules payments with a periodicity.
            3. Worker that schedules retries with exponential backoff.
     */
    fun start() {
        logger.info { "Starting the Scheduling Service..." }

        GlobalScope.launch {
            init()
        }
        GlobalScope.launch {
            scheduleBills(config.periodicity)
        }
        GlobalScope.launch {
            scheduleRetries()
        }
    }

    /*
        Schedules new bills to be paid with a periodicity.
        Waits until the next period to schedule new bills.
     */
    private suspend fun scheduleBills(periodicity: Periodicity) {
        logger.info { "Staring scheduler worker with a ${periodicity.toString()} periodicity..." }
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
                processingChannel.send(BillingAttempt(invoice.id, 0))
            }
        }
    }

    /*
        Schedules failed invoices with an exponential backoff.
     */
    private suspend fun scheduleRetries() = coroutineScope {
        logger.info { "Starting retrier worker..."}
        for (retry in retryChannel) {
            launch {
                val timeToSleep = exponentialBackoff(config.backoffInitialWait, config.backoffMaxRetries, retry.numAttempts)
                logger.info { "Scheduling retry for invoice '${retry.invoiceId}' in $timeToSleep ms" }
                delay(timeToSleep)
                processingChannel.send(retry)
            }
        }
    }

    /*
        Schedules bills for invoices failed due to external factors.
     */
    private suspend fun init() {
        logger.info { "Starting initializer worker..."}
        val invoicesToRetry = invoiceService.fetchByMultipleStatus(
                listOf(InvoiceStatus.INSUFFICIENT_FUNDS, InvoiceStatus.NETWORK_ERROR))
        logger.info { "Scheduling ${invoicesToRetry.size} failed invoices" }
        for (invoice in invoicesToRetry) {
            processingChannel.send(BillingAttempt(invoice.id, 0))
        }
    }
}