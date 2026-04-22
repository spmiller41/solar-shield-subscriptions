package com.powersolutions.solarshield.service.model;

import com.powersolutions.solarshield.entity.Invoice;

/**
 * Carries the final invoice state plus whether the current webhook materially changed it.
 */
public record InvoiceMutationResult(Invoice invoice, boolean changed) {
}
