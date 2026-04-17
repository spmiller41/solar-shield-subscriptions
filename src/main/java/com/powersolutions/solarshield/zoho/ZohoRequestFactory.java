package com.powersolutions.solarshield.zoho;

import com.powersolutions.solarshield.entity.Address;
import com.powersolutions.solarshield.entity.Contact;
import com.powersolutions.solarshield.entity.Subscription;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class ZohoRequestFactory {

    private static final ZoneId EASTERN_ZONE = ZoneId.of("America/New_York");
    private static final DateTimeFormatter ZOHO_DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");

    private ZohoRequestFactory() {}

    public static String formatDateTime(OffsetDateTime dateTime) {
        return dateTime.format(ZOHO_DATE_TIME_FORMATTER);
    }

    public static String formatDateTime(LocalDateTime dateTime) {
        return dateTime.atZone(EASTERN_ZONE).format(ZOHO_DATE_TIME_FORMATTER);
    }

    public static HttpHeaders buildHeaders(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(accessToken);
        return headers;
    }

    public static String buildUpsertEndpoint(String baseUrl, ModuleApiName zohoModule) {
        String normalizedBaseUrl = baseUrl.endsWith("/") ? baseUrl : baseUrl + "/";
        return normalizedBaseUrl + zohoModule + "/upsert";
    }

    public static Map<String, Object> buildAccountBody(Subscription subscription, Address address, Contact contact) {
        validateAccountInputs(subscription, address, contact);

        Map<String, Object> record = new HashMap<>();

        // --- Contact fields ---
        putIfNotNull(record, ZohoSolarShieldFields.NAME, buildName(contact));
        putIfNotNull(record, ZohoSolarShieldFields.FIRST_NAME, contact.getFirstName());
        putIfNotNull(record, ZohoSolarShieldFields.LAST_NAME, contact.getLastName());
        putIfNotNull(record, ZohoSolarShieldFields.EMAIL, contact.getEmail());
        putIfNotNull(record, ZohoSolarShieldFields.PHONE, contact.getPhone());

        // --- Address fields ---
        putIfNotNull(record, ZohoSolarShieldFields.COUNTRY, "United States"); // Static
        putIfNotNull(record, ZohoSolarShieldFields.STREET, address.getStreet());
        putIfNotNull(record, ZohoSolarShieldFields.CITY, address.getCity());
        putIfNotNull(record, ZohoSolarShieldFields.STATE, address.getState());
        putIfNotNull(record, ZohoSolarShieldFields.ZIP, address.getZip());

        // --- Account fields ---
        putIfNotNull(record, ZohoSolarShieldFields.ACCOUNT_STATUS, subscription.getSubscriptionStatus());
        putIfNotNull(record, ZohoSolarShieldFields.PLAN, subscription.getPlanTier());
        putIfNotNull(record, ZohoSolarShieldFields.ACTIVATED_AT, formatDateTimeIfPresent(subscription.getActivatedAt()));
        putIfNotNull(record, ZohoSolarShieldFields.ACCOUNT_ID, subscription.getExternalUid());


        // --- Wrap in data array ---
        List<Map<String, Object>> data = List.of(record);

        // --- Duplicate rules ---
        List<String> duplicateChecks = List.of(ZohoSolarShieldFields.ACCOUNT_ID);

        // --- Skip Cadences ---
        List<Map<String, String>> skipCadences = List.of(
                Map.of("name", "cadences", "action", "insert"),
                Map.of("name", "cadences", "action", "update")
        );

        // --- Final request body ---
        Map<String, Object> body = new HashMap<>();
        body.put(ZohoSolarShieldFields.DATA, data);
        body.put("duplicate_check_fields", duplicateChecks);
        body.put("skip_feature_execution", skipCadences);
        body.put("trigger", List.of()); // suppress workflows

        return body;
    }

    private static void validateAccountInputs(Subscription subscription, Address address, Contact contact) {
        Objects.requireNonNull(subscription, "subscription is required");
        Objects.requireNonNull(address, "address is required");
        Objects.requireNonNull(contact, "contact is required");

        if (subscription.getExternalUid() == null || subscription.getExternalUid().isBlank()) {
            throw new IllegalArgumentException("subscription externalUid is required for Zoho upserts");
        }
    }

    private static String formatDateTimeIfPresent(LocalDateTime dateTime) {
        return dateTime == null ? null : formatDateTime(dateTime);
    }

    private static void putIfNotNull(Map<String, Object> obj, String key, Object value) {
        if (value != null) {
            obj.put(key, value);
        }
    }

    private static String buildName(Contact contact) {
        String first = contact.getFirstName() != null ? contact.getFirstName().trim() : "";
        String last  = contact.getLastName()  != null ? contact.getLastName().trim()  : "";

        String fullName = (first + " " + last).trim();
        return fullName.isEmpty() ? "Unnamed Record" : fullName;
    }

}
