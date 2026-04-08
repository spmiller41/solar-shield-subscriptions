package com.powersolutions.solarshield.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.powersolutions.solarshield.dto.SquareInvoicePaymentRequest;
import com.powersolutions.solarshield.enums.SquareEventType;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class SquareUpdateMapper {

    private final SquareInvoicePaymentRequest request;

    public SquareUpdateMapper(String json) throws JsonProcessingException {
        this.request = new SquareInvoicePaymentRequest();

        JsonNode root = new ObjectMapper().readTree(json);

        String eventId = readNullableText(root.path("event_id"));
        request.setEventId(eventId);

        String eventTypeValue = readNullableText(root.path("type"));
        SquareEventType eventType = SquareEventType.fromValue(eventTypeValue);
        JsonNode dataObject = root.path("data").path("object");

        request.setEventType(eventType);

        if (eventType == null) {
            throw new IllegalArgumentException("Unsupported Square webhook type: " + eventTypeValue);
        }

        switch (eventType) {
            case INVOICE_CREATED:
            case INVOICE_UPDATED:
            case INVOICE_PAYMENT_MADE:
                mapInvoice(dataObject.path("invoice"));
                break;

            case PAYMENT_CREATED:
            case PAYMENT_UPDATED:
                mapPayment(dataObject.path("payment"));
                break;

            default:
                throw new IllegalArgumentException("Unsupported Square webhook type: " + eventTypeValue);
        }
    }

    private void mapInvoice(JsonNode invoice) {
        JsonNode primaryRecipient = invoice.path("primary_recipient");
        JsonNode paymentRequests = invoice.path("payment_requests");

        request.setTitle(readNullableText(invoice.path("title")));
        request.setSubscriptionId(readNullableText(invoice.path("subscription_id")));
        request.setOrderId(readNullableText(invoice.path("order_id")));
        request.setCustomerId(readNullableText(primaryRecipient.path("customer_id")));
        request.setEmail(readNullableText(primaryRecipient.path("email_address")));
        request.setStatus(readNullableText(invoice.path("status")));

        if (paymentRequests.isArray() && !paymentRequests.isEmpty()) {
            JsonNode firstPaymentRequest = paymentRequests.get(0);

            request.setAutomaticPaymentSource(
                    readNullableText(firstPaymentRequest.path("automatic_payment_source"))
            );

            JsonNode moneyNode = resolveInvoiceMoneyNode(firstPaymentRequest);

            if (moneyNode != null) {
                setMoneyFields(moneyNode);
            }
        }
    }

    private void mapPayment(JsonNode payment) {
        request.setOrderId(readNullableText(payment.path("order_id")));
        request.setCustomerId(readNullableText(payment.path("customer_id")));
        request.setEmail(readNullableText(payment.path("buyer_email_address")));
        request.setStatus(readNullableText(payment.path("status")));

        // These do not appear on payment webhook payloads
        request.setTitle(null);
        request.setSubscriptionId(null);
        request.setAutomaticPaymentSource(null);

        JsonNode moneyNode = resolvePaymentMoneyNode(payment);
        if (moneyNode != null) {
            setMoneyFields(moneyNode);
        }
    }

    private JsonNode resolveInvoiceMoneyNode(JsonNode paymentRequest) {
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

    private JsonNode resolvePaymentMoneyNode(JsonNode payment) {
        JsonNode totalMoney = payment.path("total_money");
        JsonNode amountMoney = payment.path("amount_money");
        JsonNode approvedMoney = payment.path("approved_money");

        if (!totalMoney.isMissingNode() && !totalMoney.isNull()) {
            return totalMoney;
        }

        if (!amountMoney.isMissingNode() && !amountMoney.isNull()) {
            return amountMoney;
        }

        if (!approvedMoney.isMissingNode() && !approvedMoney.isNull()) {
            return approvedMoney;
        }

        return null;
    }

    private void setMoneyFields(JsonNode moneyNode) {
        long amountInCents = moneyNode.path("amount").asLong(0);

        request.setAmount(
                BigDecimal.valueOf(amountInCents)
                        .movePointLeft(2)
                        .setScale(2, RoundingMode.HALF_UP)
        );

        request.setCurrency(readNullableText(moneyNode.path("currency")));
    }

    private String readNullableText(JsonNode node) {
        return node.isMissingNode() || node.isNull() ? null : node.asText();
    }

    public SquareInvoicePaymentRequest getRequest() {
        return request;
    }

}