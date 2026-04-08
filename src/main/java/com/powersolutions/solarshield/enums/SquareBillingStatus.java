package com.powersolutions.solarshield.enums;

/**
 * Represents a unified billing status across Square invoice and payment webhooks.
 * <p>
 * This enum normalizes both invoice-level and payment-level statuses into a single
 * ranked system used to safely process asynchronous webhook events.
 * <p>
 * The rank defines precedence and is used to enforce forward-only state transitions:
 * - Higher rank = more authoritative state
 * - Lower rank updates are ignored to prevent stale or out-of-order webhook overwrites
 * <p>
 * Key guarantees:
 * - Prevents downgrading successful payments (e.g., COMPLETED/PAID -> FAILED)
 * - Allows recovery flows (e.g., FAILED -> PAID)
 * - Enables simple comparison logic instead of complex conditional branching
 * <p>
 * Designed specifically for webhook-driven billing systems where event ordering
 * is not guaranteed and duplicate delivery may occur.
 */
public enum SquareBillingStatus {

    DRAFT(1),
    APPROVED(2),
    PENDING(2),
    FAILED(3),
    CANCELED(3),
    COMPLETED(4),
    PAID(5);

    private final int rank;

    SquareBillingStatus(int rank) { this.rank = rank; }

    public int getRank() { return rank; }

    public static SquareBillingStatus fromValue(String value) {
        if (value == null) return null;

        try {
            return SquareBillingStatus.valueOf(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

}