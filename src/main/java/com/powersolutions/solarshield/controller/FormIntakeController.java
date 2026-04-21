package com.powersolutions.solarshield.controller;

import com.powersolutions.solarshield.dto.FormIntakeRequest;
import com.powersolutions.solarshield.dto.SubscriptionProcessingResult;
import com.powersolutions.solarshield.entity.Contact;
import com.powersolutions.solarshield.enums.SubscriptionResult;
import com.powersolutions.solarshield.mapper.FormIntakeMapper;
import com.powersolutions.solarshield.service.impl.AddressSubscriptionServiceImpl;
import com.powersolutions.solarshield.service.impl.ContactServiceImpl;
import com.powersolutions.solarshield.service.impl.SubscriptionLifecycleServiceImpl;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/form")
public class FormIntakeController {

    private static final Logger logger = LoggerFactory.getLogger(FormIntakeController.class);

    private final ContactServiceImpl contactServiceImpl;
    private final AddressSubscriptionServiceImpl addressSubscriptionServiceImpl;
    private final SubscriptionLifecycleServiceImpl subscriptionLifecycleServiceImpl;

    public FormIntakeController(ContactServiceImpl contactServiceImpl,
                                AddressSubscriptionServiceImpl addressSubscriptionServiceImpl,
                                SubscriptionLifecycleServiceImpl subscriptionLifecycleServiceImpl) {
        this.contactServiceImpl = contactServiceImpl;
        this.addressSubscriptionServiceImpl = addressSubscriptionServiceImpl;
        this.subscriptionLifecycleServiceImpl = subscriptionLifecycleServiceImpl;
    }

    @PostMapping(
            path = "/intake",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public void intake(@RequestParam Map<String, String> formParams, HttpServletResponse response) throws IOException {
        FormIntakeRequest request = new FormIntakeMapper(formParams).getRequest();
        Contact contact = contactServiceImpl.upsertAndGet(new Contact(request));
        SubscriptionProcessingResult result = addressSubscriptionServiceImpl.handleAddressAndSubscription(request, contact);
        Optional<String> checkoutLink =
                subscriptionLifecycleServiceImpl.getOrCreateCheckoutLinkWithPrebuiltResponse(result);

        if (checkoutLink.isPresent()) {
            logger.info("Redirecting customer to Square checkout link for subscriptionId={}",
                    result.getSubscription().getId());
            response.setStatus(HttpStatus.FOUND.value());
            response.setHeader("Location", checkoutLink.get());
            response.setHeader("X-Redirect-Target", checkoutLink.get());
            response.setContentType(MediaType.TEXT_PLAIN_VALUE);
            response.getWriter().write("Redirecting customer to Square checkout: " + checkoutLink.get());
            return;
        }

        if (SubscriptionResult.ACTIVE_EXISTS.equals(result.getResult())) {
            logger.info("Redirect to page notifying customer they already have an active subscription");
            response.setStatus(HttpStatus.CONFLICT.value());
            response.setContentType(MediaType.TEXT_PLAIN_VALUE);
            response.getWriter().write("Redirect to page notifying customer they already have an active subscription");
            return;
        }

        logger.warn("No checkout redirect was available for subscriptionId={}",
                result.getSubscription() != null ? result.getSubscription().getId() : null);
        response.setStatus(HttpStatus.BAD_GATEWAY.value());
        response.setContentType(MediaType.TEXT_PLAIN_VALUE);
        response.getWriter().write("No checkout redirect was available");
    }

}
