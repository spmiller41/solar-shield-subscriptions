package com.powersolutions.solarshield.controller;

import com.powersolutions.solarshield.dto.FormIntakeRequest;
import com.powersolutions.solarshield.entity.Address;
import com.powersolutions.solarshield.entity.Contact;
import com.powersolutions.solarshield.entity.Subscription;
import com.powersolutions.solarshield.enums.SubscriptionStatus;
import com.powersolutions.solarshield.mapper.FormIntakeMapper;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/form")
public class FormIntakeController {

    @PostMapping(
            path = "/intake",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public void intake(@RequestParam Map<String, String> formParams) {
        FormIntakeRequest request = new FormIntakeMapper(formParams).getRequest();
        Contact c = new Contact(request);
        Address a = new Address(request);
        Subscription s = new Subscription(request, c, a, SubscriptionStatus.PENDING_PAYMENT);
        System.out.println(c);
        System.out.println(a);
        System.out.println(s);
    }

}
