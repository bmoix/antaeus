package io.pleo.antaeus.core.exceptions

import io.pleo.antaeus.models.Currency

class ExchangeRateNotFoundException(source: Currency, destination: Currency) :
        Exception("Exchange rate between '${source}' and '${destination} not found")