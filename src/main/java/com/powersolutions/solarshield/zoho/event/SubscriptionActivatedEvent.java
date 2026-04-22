package com.powersolutions.solarshield.zoho.event;

/**
 * Published after a subscription transitions to ACTIVE so Zoho parent and invoice sync can run safely.
 */
public record SubscriptionActivatedEvent(int subscriptionId) {
}
