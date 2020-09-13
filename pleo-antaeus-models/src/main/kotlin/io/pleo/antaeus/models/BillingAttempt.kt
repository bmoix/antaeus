package io.pleo.antaeus.models

data class BillingAttempt(
    val invoiceId: Int,
    val numAttempts: Int
)