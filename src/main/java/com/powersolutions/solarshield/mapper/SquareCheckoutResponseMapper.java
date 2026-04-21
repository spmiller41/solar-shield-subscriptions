package com.powersolutions.solarshield.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.powersolutions.solarshield.dto.SquareCheckoutResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Parses the Square checkout-link response and enforces the fields the app requires.
 */
public final class SquareCheckoutResponseMapper {

    private static final Logger logger = LoggerFactory.getLogger(SquareCheckoutResponseMapper.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private SquareCheckoutResponseMapper() {}

    /**
     * Converts raw Square checkout JSON into the normalized checkout response DTO.
     */
    public static SquareCheckoutResponse fromJson(String json) throws JsonProcessingException {
        JsonNode root = OBJECT_MAPPER.readTree(json);
        JsonNode paymentLink = root.path("payment_link");

        if (paymentLink.isMissingNode() || paymentLink.isNull()) {
            throw mappingFailure("Square checkout response is missing payment_link");
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
            throw mappingFailure("Square checkout response is missing " + fieldName);
        }
        return value;
    }

    private static String readNullableText(JsonNode node) {
        return (node == null || node.isMissingNode() || node.isNull()) ? null : node.asText();
    }

    private static IllegalArgumentException mappingFailure(String message) {
        logger.warn("Square checkout response mapping failed: {}", message);
        return new IllegalArgumentException(message);
    }

}
