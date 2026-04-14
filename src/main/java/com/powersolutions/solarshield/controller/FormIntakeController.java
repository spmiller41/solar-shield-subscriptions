package com.powersolutions.solarshield.controller;

import com.powersolutions.solarshield.dto.FormIntakeRequest;
import com.powersolutions.solarshield.dto.SubscriptionProcessingResult;
import com.powersolutions.solarshield.entity.Contact;
import com.powersolutions.solarshield.enums.SubscriptionResult;
import com.powersolutions.solarshield.mapper.FormIntakeMapper;
import com.powersolutions.solarshield.service.impl.AddressSubscriptionServiceImpl;
import com.powersolutions.solarshield.service.impl.ContactServiceImpl;
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

    private final ContactServiceImpl contactServiceImpl;
    private final AddressSubscriptionServiceImpl addressSubscriptionServiceImpl;

    @Autowired
    public FormIntakeController(ContactServiceImpl contactServiceImpl, AddressSubscriptionServiceImpl addressSubscriptionServiceImpl) {
        this.contactServiceImpl = contactServiceImpl;
        this.addressSubscriptionServiceImpl = addressSubscriptionServiceImpl;
    }

    @PostMapping(
            path = "/intake",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public void intake(@RequestParam Map<String, String> formParams) {
        FormIntakeRequest request = new FormIntakeMapper(formParams).getRequest();
        Contact contact = contactServiceImpl.upsertAndGet(new Contact(request));
        SubscriptionProcessingResult result = addressSubscriptionServiceImpl.handleAddressAndSubscription(request, contact);

        if (SubscriptionResult.PROCEED_TO_CHECKOUT.equals(result.getResult())) {
            System.out.println("Proceed to Square Checkout");
        } else {
            System.out.println(
                    "Redirect to fallback page. Let customer know a subscription is active and to contact support.");
        }
    }

}
