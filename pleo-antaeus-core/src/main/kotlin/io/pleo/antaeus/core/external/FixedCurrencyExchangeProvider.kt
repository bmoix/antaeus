package io.pleo.antaeus.core.external

import io.pleo.antaeus.core.exceptions.ExchangeRateNotFoundException
import io.pleo.antaeus.models.Currency
import java.math.BigDecimal

/*
    Implements the CurrencyExchangeProvider interface with fixed rates.
 */
class FixedCurrencyExchangeProvider: CurrencyExchangeProvider {
    private val rates = hashMapOf<Pair<Currency, Currency>, BigDecimal>(
            Pair(Currency.EUR, Currency.EUR) to BigDecimal("1.00"),
            Pair(Currency.EUR, Currency.USD) to BigDecimal("1.18468"),
            Pair(Currency.EUR, Currency.DKK) to BigDecimal("7.44054"),
            Pair(Currency.EUR, Currency.SEK) to BigDecimal("10.3901"),
            Pair(Currency.EUR, Currency.GBP) to BigDecimal("0.925847"),
            Pair(Currency.USD, Currency.EUR) to BigDecimal("0.844109"),
            Pair(Currency.USD, Currency.USD) to BigDecimal("1.00"),
            Pair(Currency.USD, Currency.DKK) to BigDecimal("6.28062"),
            Pair(Currency.USD, Currency.SEK) to BigDecimal("8.77037"),
            Pair(Currency.USD, Currency.GBP) to BigDecimal("0.781517"),
            Pair(Currency.DKK, Currency.EUR) to BigDecimal("0.134399"),
            Pair(Currency.DKK, Currency.USD) to BigDecimal("0.159220"),
            Pair(Currency.DKK, Currency.DKK) to BigDecimal("1.00"),
            Pair(Currency.DKK, Currency.SEK) to BigDecimal("1.39642"),
            Pair(Currency.DKK, Currency.GBP) to BigDecimal("0.124433"),
            Pair(Currency.SEK, Currency.EUR) to BigDecimal("0.0962455"),
            Pair(Currency.SEK, Currency.USD) to BigDecimal("0.114020"),
            Pair(Currency.SEK, Currency.DKK) to BigDecimal("0.716118"),
            Pair(Currency.SEK, Currency.SEK) to BigDecimal("1.00"),
            Pair(Currency.SEK, Currency.GBP) to BigDecimal("0.0891088"),
            Pair(Currency.GBP, Currency.EUR) to BigDecimal("1.08009"),
            Pair(Currency.GBP, Currency.USD) to BigDecimal("1.27956"),
            Pair(Currency.GBP, Currency.DKK) to BigDecimal("8.03645"),
            Pair(Currency.GBP, Currency.SEK) to BigDecimal("11.2222"),
            Pair(Currency.GBP, Currency.GBP) to BigDecimal("1.00"),
    )


    override fun getRate(source: Currency, destination: Currency): BigDecimal {
        return rates[Pair(source, destination)] ?: throw ExchangeRateNotFoundException(source, destination)
    }
}