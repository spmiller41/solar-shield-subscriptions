package com.powersolutions.solarshield.zoho.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.powersolutions.solarshield.zoho.ModuleApiName;
import com.powersolutions.solarshield.zoho.TokenResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * TokenService manages access tokens for Zoho CRM API based on module type.
 */
@Service
public class TokenService {

    private static final Logger logger = LoggerFactory.getLogger(TokenService.class);
    private static final int EXPIRY_BUFFER_SECONDS = 60;

    @Value("${zoho.token.base.url}")
    private String baseUrl;

    @Value("${zoho.custom.module.refresh.token}")
    private String refreshToken;

    @Value("${zoho.client.id}")
    private String clientId;

    @Value("${zoho.client.secret}")
    private String clientSecret;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    private final Map<ModuleApiName, TokenDetails> tokenStore = new ConcurrentHashMap<>();
    private final Map<ModuleApiName, Object> moduleLocks = new ConcurrentHashMap<>();

    public TokenService(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Retrieves a token for accessing Zoho CRM API for a specific module.
     *
     * @param moduleApiName - The type of module (e.g., "custom_module").
     * @return the access token as a String.
     */
    public String getAccessToken(ModuleApiName moduleApiName) {
        TokenDetails tokenDetails = tokenStore.get(moduleApiName);

        if (tokenDetails != null && !tokenDetails.isExpired()) {
            return tokenDetails.accessToken();
        }

        synchronized (moduleLocks.computeIfAbsent(moduleApiName, ignored -> new Object())) {
            tokenDetails = tokenStore.get(moduleApiName);
            if (tokenDetails == null || tokenDetails.isExpired()) {
                tokenDetails = refreshTokenForModule(moduleApiName, getRefreshTokenForModule(moduleApiName));
                tokenStore.put(moduleApiName, tokenDetails);
            }
            return tokenDetails.accessToken();
        }
    }

    private String getRefreshTokenForModule(ModuleApiName moduleApiName) {
        return switch (moduleApiName) {
            case SOLAR_SHIELD_ACCOUNT -> refreshToken;
        };
    }

    private TokenDetails refreshTokenForModule(ModuleApiName moduleApiName, String refreshToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> requestBody = new LinkedMultiValueMap<>();
        requestBody.add("refresh_token", refreshToken);
        requestBody.add("client_id", clientId);
        requestBody.add("client_secret", clientSecret);
        requestBody.add("grant_type", "refresh_token");

        HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(baseUrl, HttpMethod.POST, requestEntity, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                String responseBody = response.getBody();
                TokenResponse tokenResponse = objectMapper.readValue(responseBody, TokenResponse.class);
                validateTokenResponse(moduleApiName, tokenResponse);

                TokenDetails tokenDetails =
                        new TokenDetails(tokenResponse.getAccessToken(), Instant.now(), tokenResponse.getExpiresIn());
                logger.info("Token refreshed for module {}. Expires in: {} seconds", moduleApiName, tokenDetails.expiresIn());
                return tokenDetails;
            }

            throw new IllegalStateException("Zoho token request failed with status " + response.getStatusCode());
        } catch (Exception ex) {
            logger.error("Failed to refresh Zoho token for module {}", moduleApiName, ex);
            throw new IllegalStateException("Unable to refresh Zoho token for module " + moduleApiName, ex);
        }
    }

    private void validateTokenResponse(ModuleApiName moduleApiName, TokenResponse tokenResponse) {
        if (tokenResponse == null || tokenResponse.getAccessToken() == null || tokenResponse.getAccessToken().isBlank()) {
            throw new IllegalStateException("Zoho token response did not include an access token for module " + moduleApiName);
        }

        if (tokenResponse.getExpiresIn() <= 0) {
            throw new IllegalStateException("Zoho token response returned invalid expiry for module " + moduleApiName);
        }
    }

    private record TokenDetails(String accessToken, Instant lastRefreshed, int expiresIn) {
        public boolean isExpired() {
            return Instant.now().getEpochSecond() - lastRefreshed.getEpochSecond() >= expiresIn - EXPIRY_BUFFER_SECONDS;
        }
    }

}
