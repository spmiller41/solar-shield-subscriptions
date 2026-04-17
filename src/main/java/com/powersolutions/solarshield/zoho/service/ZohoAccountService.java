package com.powersolutions.solarshield.zoho.service;

import com.powersolutions.solarshield.entity.Address;
import com.powersolutions.solarshield.entity.Contact;
import com.powersolutions.solarshield.entity.Subscription;
import com.powersolutions.solarshield.zoho.ModuleApiName;
import com.powersolutions.solarshield.zoho.ZohoRequestFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Handles Zoho CRM account operations for the Solar Shield module, including account upserts
 * and future account-related API calls.
 */
@Service
public class ZohoAccountService {

    private static final Logger logger = LoggerFactory.getLogger(ZohoAccountService.class);

    @Value("${zoho.api.base.url}")
    private String baseUrl;

    private final RestTemplate restTemplate;
    private final TokenService tokenService;

    public ZohoAccountService(RestTemplate restTemplate, TokenService tokenService) {
        this.restTemplate = restTemplate;
        this.tokenService = tokenService;
    }

    /**
     * Upserts a Solar Shield account in Zoho using Account_ID as the deduplication key.
     */
    public String upsertAccount(Subscription subscription, Address address, Contact contact) {
        Map<String, Object> requestBody = ZohoRequestFactory.buildAccountBody(subscription, address, contact);
        String accountId = subscription.getExternalUid();
        String endpoint = ZohoRequestFactory.buildUpsertEndpoint(baseUrl, ModuleApiName.SOLAR_SHIELD_ACCOUNT);
        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(
                requestBody,
                ZohoRequestFactory.buildHeaders(tokenService.getAccessToken(ModuleApiName.SOLAR_SHIELD_ACCOUNT))
        );

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    endpoint,
                    HttpMethod.POST,
                    requestEntity,
                    String.class
            );

            String responseBody = response.getBody();
            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new IllegalStateException(
                        "Zoho account upsert returned status " + response.getStatusCode() + " for accountId " + accountId
                );
            }

            logger.info("Zoho account upsert succeeded for accountId {} with status {}", accountId, response.getStatusCode());
            return responseBody;
        } catch (HttpStatusCodeException ex) {
            logger.error(
                    "Zoho account upsert failed for accountId {} with status {} and response {}",
                    accountId,
                    ex.getStatusCode(),
                    ex.getResponseBodyAsString(),
                    ex
            );
            throw new IllegalStateException("Zoho account upsert failed for accountId " + accountId, ex);
        } catch (RestClientException ex) {
            logger.error("Zoho account upsert client error for accountId {}", accountId, ex);
            throw new IllegalStateException("Zoho account upsert request failed for accountId " + accountId, ex);
        }
    }

}
