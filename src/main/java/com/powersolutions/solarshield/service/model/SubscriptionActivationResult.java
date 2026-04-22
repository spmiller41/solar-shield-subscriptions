package com.powersolutions.solarshield.service.model;

import com.powersolutions.solarshield.entity.Subscription;

/**
 * Captures the saved subscription and whether this call caused the first ACTIVE transition.
 */
public record SubscriptionActivationResult(Subscription subscription, boolean newlyActivated) {
}
