/*
    Defines the main() entry point of the app.
    Configures the database and sets up the REST web service.
 */

@file:JvmName("AntaeusApp")

package io.pleo.antaeus.app

import io.pleo.antaeus.core.external.FixedCurrencyExchangeProvider
import io.pleo.antaeus.core.external.UnstablePaymentProvider
import io.pleo.antaeus.core.services.*
import io.pleo.antaeus.data.AntaeusDal
import io.pleo.antaeus.data.CustomerTable
import io.pleo.antaeus.data.InvoiceTable
import io.pleo.antaeus.models.BillingAttempt
import io.pleo.antaeus.models.Periodicity
import io.pleo.antaeus.models.SchedulingConfig
import io.pleo.antaeus.rest.AntaeusRest
import kotlinx.coroutines.channels.Channel
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import setupInitialData
import java.io.File
import java.sql.Connection

fun main() {
    // The tables to create in the database.
    val tables = arrayOf(InvoiceTable, CustomerTable)

    val dbFile: File = File.createTempFile("antaeus-db", ".sqlite")
    // Connect to the database and create the needed tables. Drop any existing data.
    val db = Database
        .connect(url = "jdbc:sqlite:${dbFile.absolutePath}",
            driver = "org.sqlite.JDBC",
            user = "root",
            password = "")
        .also {
            TransactionManager.manager.defaultIsolationLevel = Connection.TRANSACTION_SERIALIZABLE
            transaction(it) {
                addLogger(StdOutSqlLogger)
                // Drop all existing tables to ensure a clean slate on each run
                SchemaUtils.drop(*tables)
                // Create all tables
                SchemaUtils.create(*tables)
            }
        }

    // Set up data access layer.
    val dal = AntaeusDal(db = db)

    // Insert example data in the database.
    setupInitialData(dal = dal)

    // Create core services
    val invoiceService = InvoiceService(dal = dal)
    val customerService = CustomerService(dal = dal)

    // Get third parties
    val paymentProvider = UnstablePaymentProvider(customerService)
    val currencyExchangeProvider = FixedCurrencyExchangeProvider()

    val exchangeService = ExchangeService(
            customerService = customerService,
            invoiceService = invoiceService,
            currencyExchangeProvider = currencyExchangeProvider,
    )

    // Communication channels between the billing and scheduling service
    val processingChannel = Channel<BillingAttempt>()
    val retryChannel = Channel<BillingAttempt>()

    val billingService = BillingService(
            paymentProvider = paymentProvider,
            invoiceService = invoiceService,
            exchangeService = exchangeService,
            processingChannel = processingChannel,
            retryChannel = retryChannel,
    )
    val schedulingService = SchedulingService(
            invoiceService = invoiceService,
            processingChannel =processingChannel,
            retryChannel = retryChannel,
            config = SchedulingConfig(
                    periodicity = Periodicity.MONTHLY,
                    backoffInitialWait = 1000L,
                    backoffMaxRetries = 12,
            )
    )

    // Start the billing and scheduling services in the background
    billingService.start()
    schedulingService.start()

    // Create REST web service
    AntaeusRest(
            invoiceService = invoiceService,
            customerService = customerService
    ).run()
}
