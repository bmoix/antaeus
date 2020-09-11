package io.pleo.antaeus.core.exceptions

class InvoiceAlreadyPaidException(invoiceId: Int, customerId: Int) :
        Exception("Invoice '$invoiceId' from customer '$customerId' is already paid.")