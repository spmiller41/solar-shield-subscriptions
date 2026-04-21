package com.powersolutions.solarshield.service.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

/**
 * Verifies Square webhook signatures using the configured signature key and public notification URL.
 */
@Service
public class SquareSignatureVerifier {

    private static final Logger logger = LoggerFactory.getLogger(SquareSignatureVerifier.class);

    @Value("${square.webhook.signature.key}")
    private String signatureKey;

    @Value("${square.webhook.notification.url}")
    private String notificationUrl;

    /**
     * Returns true when the supplied Square signature matches the expected signature for the payload.
     */
    public boolean isValidSignature(String payload, String headerSignature) {
        try {
            if (payload == null || payload.isBlank()) {
                logger.warn("Square signature verification failed because the payload was blank");
                return false;
            }

            if (headerSignature == null || headerSignature.isBlank()) {
                logger.warn("Square signature verification failed because the signature header was blank");
                return false;
            }

            if (signatureKey == null || signatureKey.isBlank()) {
                logger.error("Square signature verification failed because the signature key is not configured");
                return false;
            }

            if (notificationUrl == null || notificationUrl.isBlank()) {
                logger.error("Square signature verification failed because the notification URL is not configured");
                return false;
            }

            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(signatureKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKeySpec);

            String message = notificationUrl + payload;
            byte[] digest = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));

            String generatedSignature = Base64.getEncoder().encodeToString(digest);

            boolean valid = MessageDigest.isEqual(
                    generatedSignature.getBytes(StandardCharsets.UTF_8),
                    headerSignature.getBytes(StandardCharsets.UTF_8)
            );

            if (!valid) {
                logger.warn("Square signature verification failed due to signature mismatch");
            }

            return valid;
        } catch (Exception ex) {
            logger.error("Square signature verification failed due to an internal error", ex);
            return false;
        }
    }

}
