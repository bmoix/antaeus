package io.pleo.antaeus.models

data class SchedulingConfig(
        val periodicity: Periodicity,
        val backoffInitialWait: Long,
        val backoffMaxRetries: Int,
)