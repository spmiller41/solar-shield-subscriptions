package com.powersolutions.solarshield.service.api;

import com.powersolutions.solarshield.dto.FormIntakeRequest;
import com.powersolutions.solarshield.entity.Contact;
import com.powersolutions.solarshield.enums.SubscriptionResult;

public interface AddressSubscriptionService {

    SubscriptionResult handleAddressAndSubscription(FormIntakeRequest request, Contact contact);

}