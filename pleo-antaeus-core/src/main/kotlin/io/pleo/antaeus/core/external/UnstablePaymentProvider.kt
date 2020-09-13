package io.pleo.antaeus.core.external

import io.pleo.antaeus.core.exceptions.CurrencyMismatchException
import io.pleo.antaeus.core.exceptions.NetworkException
import io.pleo.antaeus.core.services.CustomerService
import io.pleo.antaeus.models.Invoice
import kotlin.random.Random

/*
    Implements the PaymentProvider interface with basic checks and a random result.
 */
class UnstablePaymentProvider(
        private val customerService: CustomerService
): PaymentProvider {

    override fun charge(invoice: Invoice): Boolean {
        val customer = customerService.fetch(invoice.customerId)

        if (invoice.amount.currency != customer.currency) {
            throw CurrencyMismatchException(invoice.id, invoice.customerId)
        }

        return when (Random.nextInt(3)) {
            0 -> true
            1 -> false
            else -> throw NetworkException()
        }
    }
}