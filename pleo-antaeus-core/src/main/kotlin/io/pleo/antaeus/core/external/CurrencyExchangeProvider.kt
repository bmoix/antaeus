package io.pleo.antaeus.core.external

import io.pleo.antaeus.models.Currency
import java.math.BigDecimal

interface CurrencyExchangeProvider {
    /*
        Provides exchange rates between different currencies.

        Returns:
            The exchange rate between the source currency and the destination currency.

        Throws:
            `ExchangeRateNotFoundException`: when the exchange rate is not available.
            `NetworkException`: when a network error happens.
     */

    fun getRate(source: Currency, destination: Currency): BigDecimal
}