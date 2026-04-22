package com.powersolutions.solarshield.zoho.event;

/**
 * Published after an invoice mutation commits so Zoho invoice-row sync can run safely.
 */
public record InvoiceChangedEvent(int invoiceId) {
}
