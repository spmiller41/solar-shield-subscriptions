package com.powersolutions.solarshield.zoho.event;

/**
 * Published after an invoice mutation commits so Zoho invoice-row sync can run safely.
 * When activationSyncScheduled is true, the first-activation listener will handle invoice sync.
 */
public record InvoiceChangedEvent(int invoiceId, boolean activationSyncScheduled) {
}
