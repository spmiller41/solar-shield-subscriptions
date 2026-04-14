package com.powersolutions.solarshield.service.api;

import com.powersolutions.solarshield.dto.FormIntakeRequest;

public interface SubscriptionLifecycleService {

    public String getOrCreateCheckoutLink(FormIntakeRequest request);

}
