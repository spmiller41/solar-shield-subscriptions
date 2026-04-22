package com.powersolutions.solarshield.zoho.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.powersolutions.solarshield.entity.Address;
import com.powersolutions.solarshield.entity.Contact;
import com.powersolutions.solarshield.entity.Subscription;
import com.powersolutions.solarshield.repo.SubscriptionRepo;
import com.powersolutions.solarshield.zoho.ModuleApiName;
import com.powersolutions.solarshield.zoho.ZohoRequestFactory;
import com.powersolutions.solarshield.zoho.ZohoUpsertResponse;
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
    private final SubscriptionRepo subscriptionRepo;
    private final ObjectMapper objectMapper;

    public ZohoAccountService(RestTemplate restTemplate,
                              TokenService tokenService,
                              SubscriptionRepo subscriptionRepo,
                              ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.tokenService = tokenService;
        this.subscriptionRepo = subscriptionRepo;
        this.objectMapper = objectMapper;
    }

    /**
     * Upserts a Solar Shield account in Zoho using Account_ID as the deduplication key and stores the Zoho record ID.
     */
    public ZohoUpsertResponse upsertAccount(Subscription subscription, Address address, Contact contact) {
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

            if (responseBody == null || responseBody.isBlank()) {
                throw new IllegalStateException("Zoho account upsert returned an empty response body for accountId " + accountId);
            }

            ZohoUpsertResponse upsertResponse = objectMapper.readValue(responseBody, ZohoUpsertResponse.class);
            if (!upsertResponse.hasSuccessfulFirstResult()) {
                throw new IllegalStateException("Zoho account upsert returned a non-success result for accountId " + accountId);
            }

            String zohoRecordId = upsertResponse.getFirstRecordId();
            if (zohoRecordId == null || zohoRecordId.isBlank()) {
                throw new IllegalStateException("Zoho account upsert response did not include a record id for accountId " + accountId);
            }

            persistZohoRecordId(subscription, zohoRecordId);

            logger.info("Zoho account upsert succeeded for accountId {} zohoRecordId {} with status {}",
                    accountId, zohoRecordId, response.getStatusCode());
            return upsertResponse;
        } catch (HttpStatusCodeException ex) {
            logger.error(
                    "Zoho account upsert failed for accountId {} with status {} and response {}",
                    accountId,
                    ex.getStatusCode(),
                    ex.getResponseBodyAsString(),
                    ex
            );
            throw new IllegalStateException("Zoho account upsert failed for accountId " + accountId, ex);
        } catch (JsonProcessingException ex) {
            logger.error("Unable to parse Zoho account upsert response for accountId {}", accountId, ex);
            throw new IllegalStateException("Zoho account upsert response parsing failed for accountId " + accountId, ex);
        } catch (RestClientException ex) {
            logger.error("Zoho account upsert client error for accountId {}", accountId, ex);
            throw new IllegalStateException("Zoho account upsert request failed for accountId " + accountId, ex);
        }
    }

    private void persistZohoRecordId(Subscription subscription, String zohoRecordId) {
        if (zohoRecordId.equals(subscription.getZohoRecordId())) {
            return;
        }

        subscription.setZohoRecordId(zohoRecordId);
        subscriptionRepo.save(subscription);
    }

}
