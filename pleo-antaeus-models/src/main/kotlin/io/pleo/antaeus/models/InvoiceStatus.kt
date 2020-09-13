package io.pleo.antaeus.models

enum class InvoiceStatus {
    PENDING,
    PAID,
    PROCESSING,
    ERROR,
    INSUFFICIENT_FUNDS,
    CURRENCY_MISMATCH,
    NETWORK_ERROR
}
