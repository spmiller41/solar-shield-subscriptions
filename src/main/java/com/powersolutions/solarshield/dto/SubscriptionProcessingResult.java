package com.powersolutions.solarshield.dto;

import com.powersolutions.solarshield.entity.Subscription;
import com.powersolutions.solarshield.enums.SubscriptionResult;

public class SubscriptionProcessingResult {

    private final Subscription subscription;
    private final SubscriptionResult result;

    public SubscriptionProcessingResult(Subscription subscription, SubscriptionResult result) {
        this.subscription = subscription;
        this.result = result;
    }

    public Subscription getSubscription() { return subscription; }

    public SubscriptionResult getResult() { return result; }

    @Override
    public String toString() {
        return "SubscriptionProcessingResult{" +
                "subscription=" + subscription +
                ", result=" + result +
                '}';
    }

}
