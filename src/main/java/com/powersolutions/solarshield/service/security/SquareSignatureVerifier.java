package com.powersolutions.solarshield.service.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.net.URI;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Base64;

/**
 * Verifies Square webhook signatures using the configured signature key and public notification URL.
 */
@Service
public class SquareSignatureVerifier {

    private static final Logger logger = LoggerFactory.getLogger(SquareSignatureVerifier.class);
    private static final String HMAC_SHA_256 = "HmacSHA256";

    private final byte[] signatureKeyBytes;
    private final String notificationUrl;

    public SquareSignatureVerifier(@Value("${square.webhook.signature.key}") String signatureKey,
                                   @Value("${square.webhook.notification.url}") String notificationUrl) {
        this.signatureKeyBytes = normalizeRequiredProperty("square.webhook.signature.key", signatureKey)
                .getBytes(StandardCharsets.UTF_8);
        this.notificationUrl = validateNotificationUrl(notificationUrl);
    }

    /**
     * Returns true when the supplied Square signature matches the expected signature for the payload.
     */
    public boolean isValidSignature(String payload, String headerSignature) {
        if (payload == null) {
            logger.warn("Square signature verification failed because the payload was missing");
            return false;
        }

        if (headerSignature == null || headerSignature.isBlank()) {
            logger.warn("Square signature verification failed because the signature header was blank");
            return false;
        }

        try {
            byte[] generatedSignature = generateSignature(payload);
            byte[] receivedSignature = Base64.getDecoder().decode(headerSignature.trim());

            boolean valid = MessageDigest.isEqual(generatedSignature, receivedSignature);

            if (!valid) {
                logger.warn("Square signature verification failed due to signature mismatch");
            }

            return valid;
        } catch (IllegalArgumentException ex) {
            logger.warn("Square signature verification failed because the signature header was not valid Base64");
            return false;
        } catch (GeneralSecurityException ex) {
            logger.error("Square signature verification failed due to an internal error", ex);
            return false;
        }
    }

    private byte[] generateSignature(String payload) throws GeneralSecurityException {
        Mac mac = Mac.getInstance(HMAC_SHA_256);
        mac.init(new SecretKeySpec(signatureKeyBytes, HMAC_SHA_256));
        return mac.doFinal((notificationUrl + payload).getBytes(StandardCharsets.UTF_8));
    }

    private String validateNotificationUrl(String notificationUrl) {
        String normalizedNotificationUrl = normalizeRequiredProperty("square.webhook.notification.url", notificationUrl);

        try {
            URI uri = URI.create(normalizedNotificationUrl);
            if (!uri.isAbsolute() || uri.getHost() == null) {
                throw new IllegalArgumentException("Notification URL must be absolute");
            }
            return uri.toString();
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException(
                    "Property square.webhook.notification.url must be a valid absolute URL",
                    ex
            );
        }
    }

    private String normalizeRequiredProperty(String propertyName, String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Property " + propertyName + " must be configured");
        }
        return value.trim();
    }

}
