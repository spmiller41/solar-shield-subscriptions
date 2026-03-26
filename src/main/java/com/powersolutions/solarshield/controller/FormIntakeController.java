package com.powersolutions.solarshield.controller;

import com.powersolutions.solarshield.dto.FormIntakeRequest;
import com.powersolutions.solarshield.entity.Address;
import com.powersolutions.solarshield.entity.Contact;
import com.powersolutions.solarshield.entity.Subscription;
import com.powersolutions.solarshield.enums.SubscriptionResult;
import com.powersolutions.solarshield.enums.SubscriptionStatus;
import com.powersolutions.solarshield.mapper.FormIntakeMapper;
import com.powersolutions.solarshield.service.AddressSubscriptionService;
import com.powersolutions.solarshield.service.ContactService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/form")
public class FormIntakeController {

    private final ContactService contactService;
    private final AddressSubscriptionService addressSubscriptionService;

    @Autowired
    public FormIntakeController(ContactService contactService, AddressSubscriptionService addressSubscriptionService) {
        this.contactService = contactService;
        this.addressSubscriptionService = addressSubscriptionService;
    }

    @PostMapping(
            path = "/intake",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public void intake(@RequestParam Map<String, String> formParams) {
        FormIntakeRequest request = new FormIntakeMapper(formParams).getRequest();
        Contact contact = contactService.upsertAndGet(new Contact(request));
        SubscriptionResult result = addressSubscriptionService.handleAddressAndSubscription(request, contact);

        if (SubscriptionResult.PROCEED_TO_CHECKOUT.equals(result)) {
            System.out.println("Proceed to Square Checkout");
        } else {
            System.out.println(
                    "Redirect to fallback page. Let customer know a subscription is active and to contact support.");
        }
    }

}
