package com.powersolutions.solarshield.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.powersolutions.solarshield.dto.SquareCheckoutResponse;
import com.powersolutions.solarshield.entity.Contact;
import com.powersolutions.solarshield.entity.Subscription;
import com.powersolutions.solarshield.enums.PlanTier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

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

    public SquareCheckoutResponse createSubscriptionPaymentLink(Subscription subscription,
                                                                Contact contact) {
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

            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode paymentLink = root.path("payment_link");

            String url = textOrNull(paymentLink.path("url"));
            String orderId = textOrNull(paymentLink.path("order_id"));
            String paymentLinkId = textOrNull(paymentLink.path("id"));

            logger.info("Created Square subscription checkout link. subscriptionId={}, orderId={}, paymentLinkId={}",
                    subscription.getId(), orderId, paymentLinkId);

            return new SquareCheckoutResponse(url, orderId, paymentLinkId);

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
        body.put("idempotency_key", subscription.getIdempotencyKey());
        body.put("checkout_options", buildCheckoutOptions(subscription));
        body.put("pre_populated_data", buildPrePopulatedData(contact));
        body.put("order", buildOrder(subscription, contact));
        return body;
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

        return prePopulatedData;
    }

    private Map<String, Object> buildOrder(Subscription subscription, Contact contact) {
        Map<String, Object> order = new HashMap<>();
        order.put("location_id", locationId);
        order.put("reference_id", String.valueOf(subscription.getId()));

        Map<String, String> metadata = new HashMap<>();
        metadata.put("local_subscription_id", String.valueOf(subscription.getId()));
        metadata.put("plan_tier", String.valueOf(subscription.getPlanTier()));
        metadata.put("local_contact_id", String.valueOf(subscription.getContactId()));
        metadata.put("customer_email", safe(contact.getEmail()));
        order.put("metadata", metadata);

        // 🔥 THIS is what Square was screaming about
        order.put("line_items", java.util.List.of(
                buildLineItem(subscription.getPlanTier())
        ));

        return order;
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

    private Map<String, Object> buildLineItem(PlanTier planTier) {
        Map<String, Object> lineItem = new HashMap<>();
        lineItem.put("name", getPlanName(planTier));
        lineItem.put("quantity", "1");

        Map<String, Object> priceMoney = new HashMap<>();
        priceMoney.put("amount", getAmountInCents(planTier));
        priceMoney.put("currency", "USD");

        lineItem.put("base_price_money", priceMoney);

        return lineItem;
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

    private String safe(String value) { return value == null ? "" : value; }

}