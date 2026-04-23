package com.powersolutions.solarshield.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.powersolutions.solarshield.dto.SquareInvoicePaymentRequest;
import com.powersolutions.solarshield.mapper.SquareUpdateMapper;
import com.powersolutions.solarshield.service.api.SquareWebhookService;
import com.powersolutions.solarshield.service.security.SquareSignatureVerifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/payment")
public class SquareController {

    private static final Logger logger = LoggerFactory.getLogger(SquareController.class);
    private static final String SQUARE_SIGNATURE_HEADER = "x-square-hmacsha256-signature";

    private final SquareSignatureVerifier signatureVerifier;
    private final SquareWebhookService webhookService;

    public SquareController(SquareSignatureVerifier signatureVerifier, SquareWebhookService webhookService) {
        this.signatureVerifier = signatureVerifier;
        this.webhookService = webhookService;
    }

    /**
     * Accepts signed Square webhooks and returns an explicit HTTP status for success, rejection, or malformed payloads.
     */
    @PostMapping("/updates")
    public ResponseEntity<Map<String, String>> paymentUpdate(
            @RequestBody(required = false) String squareRequest,
            @RequestHeader(value = SQUARE_SIGNATURE_HEADER, required = false) String signatureHeader
    ) {
        return processWebhook(squareRequest, signatureHeader, true, "/updates");
    }


    /**
     * Accepts unsigned Square webhook payloads for manual replay and local testing.
    // @PostMapping("/updates_test")
    public ResponseEntity<Map<String, String>> paymentUpdateTest(@RequestBody(required = false) String squareRequest) {
        return processWebhook(squareRequest, null, false, "/updates_test");
    }
    */

    private ResponseEntity<Map<String, String>> processWebhook(String squareRequest,
                                                               String signatureHeader,
                                                               boolean verifySignature,
                                                               String endpoint) {
        if (squareRequest == null || squareRequest.isBlank()) {
            logger.warn("Rejected Square webhook at {} because the payload was blank", endpoint);
            return ResponseEntity.badRequest().body(errorBody("Square webhook payload is required"));
        }

        if (verifySignature && (signatureHeader == null || signatureHeader.isBlank())) {
            logger.warn("Rejected Square webhook at {} because the signature header was missing", endpoint);
            return ResponseEntity.badRequest().body(errorBody("Square signature header is required"));
        }

        if (verifySignature && !signatureVerifier.isValidSignature(squareRequest, signatureHeader)) {
            logger.warn("Rejected Square webhook at {} because signature verification failed", endpoint);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorBody("Invalid Square signature"));
        }

        try {
            SquareInvoicePaymentRequest request = new SquareUpdateMapper(squareRequest).getRequest();
            webhookService.handleWebhook(request);
            return ResponseEntity.ok(Map.of("status", "processed"));
        } catch (JsonProcessingException | IllegalArgumentException ex) {
            logger.warn("Rejected Square webhook at {} because the payload was invalid: {}", endpoint, ex.getMessage());
            return ResponseEntity.badRequest().body(errorBody("Invalid Square webhook payload"));
        } catch (RuntimeException ex) {
            logger.error("Square webhook processing failed at {}", endpoint, ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(errorBody("Failed to process Square webhook"));
        }
    }

    private Map<String, String> errorBody(String message) {
        return Map.of("error", message);
    }

}
