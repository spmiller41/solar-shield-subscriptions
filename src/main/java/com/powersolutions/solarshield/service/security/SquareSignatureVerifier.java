package com.powersolutions.solarshield.service.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

@Service
public class SquareSignatureVerifier {

    @Value("${square.webhook.signature.key}")
    private String signatureKey;

    @Value("${square.webhook.notification.url}")
    private String notificationUrl;

    public boolean isValidSignature(String payload, String headerSignature) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(signatureKey.getBytes(), "HmacSHA256");
            mac.init(secretKeySpec);

            String message = notificationUrl + payload;
            byte[] digest = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));

            String generatedSignature = Base64.getEncoder().encodeToString(digest);

            return MessageDigest.isEqual(generatedSignature.getBytes(), headerSignature.getBytes());
        } catch (Exception ex) {
            return false;
        }
    }

}
