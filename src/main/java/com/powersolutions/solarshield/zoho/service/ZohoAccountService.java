package com.powersolutions.solarshield.zoho.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.powersolutions.solarshield.entity.Address;
import com.powersolutions.solarshield.entity.Contact;
import com.powersolutions.solarshield.entity.Invoice;
import com.powersolutions.solarshield.entity.Subscription;
import com.powersolutions.solarshield.repo.InvoiceRepo;
import com.powersolutions.solarshield.repo.SubscriptionRepo;
import com.powersolutions.solarshield.zoho.ModuleApiName;
import com.powersolutions.solarshield.zoho.ZohoRequestFactory;
import com.powersolutions.solarshield.zoho.ZohoSolarShieldFields;
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

import java.util.*;

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
    private final InvoiceRepo invoiceRepo;
    private final ObjectMapper objectMapper;

    public ZohoAccountService(RestTemplate restTemplate,
                              TokenService tokenService,
                              SubscriptionRepo subscriptionRepo,
                              InvoiceRepo invoiceRepo,
                              ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.tokenService = tokenService;
        this.subscriptionRepo = subscriptionRepo;
        this.invoiceRepo = invoiceRepo;
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

    /**
     * Syncs invoice rows into the Zoho account subform and stores any newly-created Zoho subform row IDs.
     */
    public void syncAccountInvoices(Subscription subscription, List<Invoice> invoices) {
        if (subscription == null) {
            throw new IllegalArgumentException("subscription is required");
        }

        if (subscription.getZohoRecordId() == null || subscription.getZohoRecordId().isBlank()) {
            throw new IllegalArgumentException("subscription zohoRecordId is required before syncing Zoho invoices");
        }

        if (invoices == null || invoices.isEmpty()) {
            logger.info("Skipping Zoho invoice subform sync for subscriptionId={} because there are no invoices",
                    subscription.getId());
            return;
        }

        List<Invoice> invoicesToSync = invoices.stream()
                .filter(Objects::nonNull)
                .toList();

        if (invoicesToSync.isEmpty()) {
            logger.info("Skipping Zoho invoice subform sync for subscriptionId={} because there are no usable invoices",
                    subscription.getId());
            return;
        }

        Map<String, Object> requestBody = ZohoRequestFactory.buildInvoiceSubformBody(subscription, invoicesToSync);
        String endpoint = ZohoRequestFactory.buildModuleEndpoint(baseUrl, ModuleApiName.SOLAR_SHIELD_ACCOUNT);
        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(
                requestBody,
                ZohoRequestFactory.buildHeaders(tokenService.getAccessToken(ModuleApiName.SOLAR_SHIELD_ACCOUNT))
        );

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    endpoint,
                    HttpMethod.PUT,
                    requestEntity,
                    String.class
            );

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new IllegalStateException(
                        "Zoho invoice subform sync returned status " + response.getStatusCode()
                                + " for subscriptionId " + subscription.getId()
                );
            }

            reconcileInvoiceSubformIds(subscription, invoicesToSync);

            logger.info("Zoho invoice subform sync succeeded for subscriptionId={} zohoRecordId={} invoiceCount={}",
                    subscription.getId(), subscription.getZohoRecordId(), invoicesToSync.size());
        } catch (HttpStatusCodeException ex) {
            logger.error(
                    "Zoho invoice subform sync failed for subscriptionId {} with status {} and response {}",
                    subscription.getId(),
                    ex.getStatusCode(),
                    ex.getResponseBodyAsString(),
                    ex
            );
            throw new IllegalStateException("Zoho invoice subform sync failed for subscriptionId " + subscription.getId(), ex);
        } catch (RestClientException ex) {
            logger.error("Zoho invoice subform sync client error for subscriptionId {}", subscription.getId(), ex);
            throw new IllegalStateException("Zoho invoice subform sync request failed for subscriptionId " + subscription.getId(), ex);
        }
    }

    private void reconcileInvoiceSubformIds(Subscription subscription, List<Invoice> invoices) {
        Map<String, String> subformIdsByOrderId = fetchZohoInvoiceSubformIds(subscription);
        List<Invoice> invoicesToUpdate = new ArrayList<>();

        for (Invoice invoice : invoices) {
            if (invoice.getZohoSubformId() != null && !invoice.getZohoSubformId().isBlank()) {
                continue;
            }

            String orderId = invoice.getOrderId();
            if (orderId == null || orderId.isBlank()) {
                continue;
            }

            String zohoSubformId = subformIdsByOrderId.get(orderId);
            if (zohoSubformId == null || zohoSubformId.isBlank()) {
                continue;
            }

            invoice.setZohoSubformId(zohoSubformId);
            invoicesToUpdate.add(invoice);
        }

        if (!invoicesToUpdate.isEmpty()) {
            invoiceRepo.saveAll(invoicesToUpdate);
        }
    }

    private Map<String, String> fetchZohoInvoiceSubformIds(Subscription subscription) {
        String endpoint = ZohoRequestFactory.buildRecordEndpoint(
                baseUrl,
                ModuleApiName.SOLAR_SHIELD_ACCOUNT,
                subscription.getZohoRecordId()
        );

        HttpEntity<Void> requestEntity = new HttpEntity<>(
                ZohoRequestFactory.buildHeaders(tokenService.getAccessToken(ModuleApiName.SOLAR_SHIELD_ACCOUNT))
        );

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    endpoint,
                    HttpMethod.GET,
                    requestEntity,
                    String.class
            );

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null || response.getBody().isBlank()) {
                throw new IllegalStateException(
                        "Zoho account fetch returned invalid response for subscriptionId " + subscription.getId()
                );
            }

            return parseInvoiceSubformIds(response.getBody());
        } catch (HttpStatusCodeException ex) {
            logger.error(
                    "Zoho account fetch for invoice subforms failed for subscriptionId {} with status {} and response {}",
                    subscription.getId(),
                    ex.getStatusCode(),
                    ex.getResponseBodyAsString(),
                    ex
            );
            throw new IllegalStateException("Zoho account fetch for invoice subforms failed for subscriptionId " + subscription.getId(), ex);
        } catch (JsonProcessingException ex) {
            logger.error("Unable to parse Zoho account fetch response for subscriptionId {}", subscription.getId(), ex);
            throw new IllegalStateException("Zoho account fetch response parsing failed for subscriptionId " + subscription.getId(), ex);
        } catch (RestClientException ex) {
            logger.error("Zoho account fetch client error for subscriptionId {}", subscription.getId(), ex);
            throw new IllegalStateException("Zoho account fetch request failed for subscriptionId " + subscription.getId(), ex);
        }
    }

    private Map<String, String> parseInvoiceSubformIds(String responseBody) throws JsonProcessingException {
        JsonNode root = objectMapper.readTree(responseBody);
        JsonNode data = root.path(ZohoSolarShieldFields.DATA);

        if (!data.isArray() || data.isEmpty()) {
            return Map.of();
        }

        JsonNode record = data.get(0);
        JsonNode subformRows = record.path(ZohoSolarShieldFields.ACCOUNT_INVOICES);

        if (!subformRows.isArray() || subformRows.isEmpty()) {
            return Map.of();
        }

        Map<String, String> subformIdsByOrderId = new HashMap<>();
        for (JsonNode row : subformRows) {
            String orderId = readText(row, ZohoSolarShieldFields.INVOICE_ORDER_ID);
            String subformId = readText(row, ZohoSolarShieldFields.RECORD_ID);

            if (orderId == null || orderId.isBlank() || subformId == null || subformId.isBlank()) {
                continue;
            }

            subformIdsByOrderId.put(orderId, subformId);
        }

        return subformIdsByOrderId;
    }

    private String readText(JsonNode node, String fieldName) {
        JsonNode field = node.path(fieldName);
        return field.isMissingNode() || field.isNull() ? null : field.asText();
    }

}
