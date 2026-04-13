package com.powersolutions.solarshield.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.powersolutions.solarshield.dto.SquareCheckoutResponse;
import com.powersolutions.solarshield.entity.Contact;
import com.powersolutions.solarshield.entity.Subscription;
import com.powersolutions.solarshield.enums.PlanTier;
import com.powersolutions.solarshield.mapper.SquareCheckoutResponseMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class SquareSubscriptionCheckoutService {

    private static final Logger logger = LoggerFactory.getLogger(SquareSubscriptionCheckoutService.class);

    @Value("${square.api.url}")
    private String squareApiUrl;

    @Value("${square.access.token}")
    private String accessToken;

    @Value("${square.location.id}")
    private String locationId;

    @Value("${square.subscription.plan.silver}")
    private String silverPlanVariationId;

    @Value("${square.subscription.plan.gold}")
    private String goldPlanVariationId;

    @Value("${square.subscription.plan.platinum}")
    private String platinumPlanVariationId;

    @Value("${square.subscription.plan.test}")
    private String testPlanVariationId;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public SquareSubscriptionCheckoutService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public SquareCheckoutResponse createSubscriptionPaymentLink(Subscription subscription, Contact contact) {
        try {
            HttpHeaders headers = buildHeaders();
            Map<String, Object> requestBody = buildPayload(subscription, contact);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<String> response =
                    restTemplate.postForEntity(squareApiUrl, entity, String.class);

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                logger.error("Failed to create Square subscription payment link. Status: {}, Body: {}",
                        response.getStatusCode(), response.getBody());
                return null;
            }

            SquareCheckoutResponse checkoutResponse =
                    new SquareCheckoutResponseMapper(response.getBody()).getResponse();

            logger.info("Created Square subscription checkout link. subscriptionId={}, orderId={}, paymentLinkId={}",
                    subscription.getId(), checkoutResponse.getOrderId(), checkoutResponse.getPaymentLinkId());

            return checkoutResponse;

        } catch (Exception ex) {
            logger.error("Exception creating Square subscription payment link for subscriptionId={}: {}",
                    subscription.getId(), ex.getMessage(), ex);
            return null;
        }
    }

    private HttpHeaders buildHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(accessToken);
        headers.set("Square-Version", "2025-03-19");
        return headers;
    }

    private Map<String, Object> buildPayload(Subscription subscription, Contact contact) {
        Map<String, Object> body = new HashMap<>();
        body.put("idempotency_key", UUID.randomUUID().toString());
        body.put("checkout_options", buildCheckoutOptions(subscription));
        body.put("pre_populated_data", buildPrePopulatedData(contact));
        body.put("quick_pay", buildQuickPay(subscription));
        return body;
    }

    private Map<String, Object> buildQuickPay(Subscription subscription) {
        Map<String, Object> quickPay = new HashMap<>();

        quickPay.put("name", getPlanName(subscription.getPlanTier()));
        quickPay.put("price_money", Map.of(
                "amount", getAmountInCents(subscription.getPlanTier()),
                "currency", "USD"
        ));
        quickPay.put("location_id", locationId);

        quickPay.put("description", getPlanDescription(subscription.getPlanTier()));

        return quickPay;
    }

    private String getPlanDescription(PlanTier planTier) {
        return switch (planTier) {
            case SILVER -> "Annual solar panel cleaning and system inspection to keep your system running efficiently.";
            case GOLD -> "Includes panel cleaning, system inspection, and priority service for any maintenance needs.";
            case PLATINUM -> "Full-service plan with cleaning, inspection, priority service, and extended system support.";
            case TEST -> "Test subscription plan for internal use.";
        };
    }

    private Map<String, Object> buildCheckoutOptions(Subscription subscription) {
        Map<String, Object> checkoutOptions = new HashMap<>();
        checkoutOptions.put("subscription_plan_id", getPlanVariationId(subscription.getPlanTier()));
        return checkoutOptions;
    }

    private Map<String, Object> buildPrePopulatedData(Contact contact) {
        Map<String, Object> prePopulatedData = new HashMap<>();

        if (contact.getEmail() != null && !contact.getEmail().isBlank()) {
            prePopulatedData.put("buyer_email", contact.getEmail().trim());
        }

        if (contact.getPhone() != null && !contact.getPhone().isBlank()) {
            prePopulatedData.put("buyer_phone_number", contact.getPhone().trim());
        }

        if (contact.getFirstName() != null && !contact.getFirstName().isBlank()
                && contact.getLastName() != null && !contact.getLastName().isBlank()) {

            prePopulatedData.put(
                    "buyer_name",
                    contact.getFirstName().trim() + " " + contact.getLastName().trim()
            );
        }

        return prePopulatedData;
    }

    private long getAmountInCents(PlanTier planTier) {
        return switch (planTier) {
            case SILVER -> 995L;
            case GOLD -> 1995L;
            case PLATINUM -> 2995L;
            case TEST -> 100L;
        };
    }

    private String getPlanName(PlanTier planTier) {
        return switch (planTier) {
            case SILVER -> "Home Solar Shield - Silver";
            case GOLD -> "Home Solar Shield - Gold";
            case PLATINUM -> "Home Solar Shield - Platinum";
            case TEST -> "Home Solar Shield - Test";
        };
    }

    private String getPlanVariationId(PlanTier planTier) {
        return switch (planTier) {
            case SILVER -> silverPlanVariationId;
            case GOLD -> goldPlanVariationId;
            case PLATINUM -> platinumPlanVariationId;
            case TEST -> testPlanVariationId;
        };
    }

    private String textOrNull(JsonNode node) {
        return (node == null || node.isMissingNode() || node.isNull()) ? null : node.asText();
    }

}