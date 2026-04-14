package com.powersolutions.solarshield.service.api;

import com.powersolutions.solarshield.dto.FormIntakeRequest;
import com.powersolutions.solarshield.dto.SubscriptionProcessingResult;
import com.powersolutions.solarshield.entity.Contact;

public interface AddressSubscriptionService {

    SubscriptionProcessingResult handleAddressAndSubscription(FormIntakeRequest request, Contact contact);

}