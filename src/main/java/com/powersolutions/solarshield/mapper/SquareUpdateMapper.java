package com.powersolutions.solarshield.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.powersolutions.solarshield.dto.SquareUpdateRequest;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class SquareUpdateMapper {

    private final SquareUpdateRequest request;

    public SquareUpdateMapper(String json) throws JsonProcessingException {
        this.request = new SquareUpdateRequest();

        JsonNode root = new ObjectMapper().readTree(json);
        JsonNode invoice = root.path("data").path("object").path("invoice");
        JsonNode primaryRecipient = invoice.path("primary_recipient");
        JsonNode paymentRequests = invoice.path("payment_requests");

        request.setTitle(readNullableText(invoice.path("title")));
        request.setSubscriptionId(readNullableText(invoice.path("subscription_id")));
        request.setCustomerId(readNullableText(primaryRecipient.path("customer_id")));
        request.setEmail(readNullableText(primaryRecipient.path("email_address")));
        request.setStatus(readNullableText(invoice.path("status")));

        if (paymentRequests.isArray() && !paymentRequests.isEmpty()) {
            JsonNode firstPaymentRequest = paymentRequests.get(0);

            request.setAutomaticPaymentSource(
                    readNullableText(firstPaymentRequest.path("automatic_payment_source"))
            );

            JsonNode moneyNode = resolveMoneyNode(firstPaymentRequest);

            if (moneyNode != null) {
                long amountInCents = moneyNode.path("amount").asLong(0);
                request.setAmount(BigDecimal.valueOf(amountInCents)
                        .movePointLeft(2)
                        .setScale(2, RoundingMode.HALF_UP));
                request.setCurrency(readNullableText(moneyNode.path("currency")));
            }
        }
    }

    private JsonNode resolveMoneyNode(JsonNode paymentRequest) {
        JsonNode totalCompletedAmountMoney = paymentRequest.path("total_completed_amount_money");
        JsonNode computedAmountMoney = paymentRequest.path("computed_amount_money");

        long completedAmount = totalCompletedAmountMoney.path("amount").asLong(0);

        if (completedAmount > 0) {
            return totalCompletedAmountMoney;
        }

        if (!computedAmountMoney.isMissingNode() && !computedAmountMoney.isNull()) {
            return computedAmountMoney;
        }

        return null;
    }

    private String readNullableText(JsonNode node) {
        return node.isMissingNode() || node.isNull() ? null : node.asText();
    }

    public SquareUpdateRequest getRequest() {
        return request;
    }
}