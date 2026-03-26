package com.powersolutions.solarshield.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.powersolutions.solarshield.dto.SquareUpdateRequest;

public class SquareUpdateMapper {

    private final SquareUpdateRequest request;

    public SquareUpdateMapper(String json) throws JsonProcessingException {
        request = new SquareUpdateRequest();

        JsonNode root = new ObjectMapper().readTree(json);
        String eventType = root.path("type").asText();

        request.setEventType(eventType);

        switch (eventType) {
            case "invoice.updated":
            case "invoice.payment_made":
                mapInvoice(root);
                break;

            default:
                throw new IllegalArgumentException("Unsupported Square webhook type: " + eventType);
        }
    }

    private void mapInvoice(JsonNode root) {
        JsonNode invoice = root.path("data").path("object").path("invoice");
        JsonNode primaryRecipient = invoice.path("primary_recipient");

        request.setInvoiceStatus(readNullableText(invoice.path("status")));
        request.setCustomerId(readNullableText(primaryRecipient.path("customer_id")));
        request.setCustomerEmail(readNullableText(primaryRecipient.path("email_address")));
    }

    private String readNullableText(JsonNode node) {
        return node.isMissingNode() || node.isNull() ? null : node.asText();
    }

    public SquareUpdateRequest getRequest() {
        return request;
    }
}