package com.powersolutions.solarshield.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.powersolutions.solarshield.dto.SquareCheckoutResponse;

public class SquareCheckoutResponseMapper {

    private final SquareCheckoutResponse response;

    public SquareCheckoutResponseMapper(String json) throws JsonProcessingException {
        JsonNode root = new ObjectMapper().readTree(json);
        JsonNode paymentLink = root.path("payment_link");

        this.response = new SquareCheckoutResponse(
                readNullableText(paymentLink.path("url")),
                readNullableText(paymentLink.path("order_id")),
                readNullableText(paymentLink.path("id"))
        );
    }

    public SquareCheckoutResponse getResponse() { return response; }

    private String readNullableText(JsonNode node) {
        return (node == null || node.isMissingNode() || node.isNull()) ? null : node.asText();
    }

}