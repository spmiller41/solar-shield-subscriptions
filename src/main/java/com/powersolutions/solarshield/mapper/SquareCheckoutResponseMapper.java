package com.powersolutions.solarshield.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.powersolutions.solarshield.dto.SquareCheckoutResponse;

public final class SquareCheckoutResponseMapper {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private SquareCheckoutResponseMapper() {}

    public static SquareCheckoutResponse fromJson(String json) throws JsonProcessingException {
        JsonNode root = OBJECT_MAPPER.readTree(json);
        JsonNode paymentLink = root.path("payment_link");

        if (paymentLink.isMissingNode() || paymentLink.isNull()) {
            throw new IllegalArgumentException("Square checkout response is missing payment_link");
        }

        return new SquareCheckoutResponse(
                readRequiredText(paymentLink, "url"),
                readRequiredText(paymentLink, "order_id"),
                readRequiredText(paymentLink, "id")
        );
    }

    private static String readRequiredText(JsonNode parentNode, String fieldName) {
        String value = readNullableText(parentNode.path(fieldName));
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Square checkout response is missing " + fieldName);
        }
        return value;
    }

    private static String readNullableText(JsonNode node) {
        return (node == null || node.isMissingNode() || node.isNull()) ? null : node.asText();
    }

}
