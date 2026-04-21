package com.powersolutions.solarshield.mapper;

import com.powersolutions.solarshield.dto.FormIntakeRequest;
import com.powersolutions.solarshield.enums.PlanTier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.regex.Pattern;

/**
 * Normalizes raw form parameters into the internal intake request model.
 */
public final class FormIntakeMapper {

    private static final Logger logger = LoggerFactory.getLogger(FormIntakeMapper.class);
    private static final Pattern SIMPLE_EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final Map<String, String> STATE_CODES = Map.ofEntries(
            Map.entry("CT", "CT"), Map.entry("CONNECTICUT", "CT"),
            Map.entry("NJ", "NJ"), Map.entry("NEW JERSEY", "NJ"),
            Map.entry("NY", "NY"), Map.entry("NEW YORK", "NY")
    );

    private final FormIntakeRequest request;

    /**
     * Maps validated form fields into the normalized intake DTO used by downstream services.
     */
    public FormIntakeMapper(Map<String, String> formParams) {
        if (formParams == null) {
            throw validationFailure("Form parameters are required.");
        }

        request = new FormIntakeRequest();
        request.setFirstName(requiredTrim(formParams, "First Name"));
        request.setLastName(requiredTrim(formParams, "Last Name"));
        request.setEmail(normalizeEmail(formParams.get("Email")));
        request.setPhone(normalizePhone(formParams.get("Phone")));
        request.setStreet(normalizeUpperRequired(formParams, "Address"));
        request.setCity(normalizeUpperRequired(formParams, "City"));
        request.setState(normalizeState(formParams.get("US States")));
        request.setZip(normalizeZip(formParams.get("Zip")));
        request.setPlanTier(mapPlanTier(formParams.get("Home Solar Shield Level")));
    }

    /**
     * Returns the normalized intake request produced by this mapper.
     */
    public FormIntakeRequest getRequest() { return request; }

    private String requiredTrim(Map<String, String> formParams, String fieldName) {
        String value = trim(formParams.get(fieldName));
        if (value == null || value.isBlank()) {
            throw validationFailure(fieldName + " is required.");
        }
        return value;
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }

    private String normalizeEmail(String email) {
        String normalized = trim(email);
        if (normalized == null || normalized.isBlank()) {
            return null;
        }

        String lowercaseEmail = normalized.toLowerCase();
        if (!SIMPLE_EMAIL_PATTERN.matcher(lowercaseEmail).matches()) {
            throw validationFailure("Invalid email address.");
        }

        return lowercaseEmail;
    }

    private String normalizePhone(String phone) {
        if (phone == null || phone.isBlank()) {
            throw validationFailure("Phone is required.");
        }

        String digits = phone.replaceAll("\\D", "");

        if (digits.length() == 10) {
            return "+1" + digits;
        }

        if (digits.length() == 11 && digits.startsWith("1")) {
            return "+" + digits;
        }

        throw validationFailure("Invalid phone number.");
    }

    private String normalizeUpperRequired(Map<String, String> formParams, String fieldName) {
        return requiredTrim(formParams, fieldName).toUpperCase();
    }

    private String normalizeState(String state) {
        if (state == null || state.isBlank()) {
            throw validationFailure("State is required.");
        }

        String normalized = state.trim().toUpperCase();
        String stateCode = STATE_CODES.get(normalized);
        if (stateCode == null) {
            throw validationFailure("State must be NY, NJ, or CT.");
        }

        return stateCode;
    }

    private String normalizeZip(String zip) {
        if (zip == null || zip.isBlank()) {
            throw validationFailure("Zip is required.");
        }

        String digits = zip.replaceAll("\\D", "");
        if (digits.length() == 5) {
            return digits;
        }

        if (digits.length() == 9) {
            return digits.substring(0, 5);
        }

        throw validationFailure("Invalid zip code.");
    }

    private PlanTier mapPlanTier(String level) {
        if (level == null || level.isBlank()) {
            throw validationFailure("Plan tier is required.");
        }

        String normalized = level.trim().toUpperCase();

        if (normalized.contains("SILVER")) {
            return PlanTier.SILVER;
        }

        if (normalized.contains("GOLD")) {
            return PlanTier.GOLD;
        }

        if (normalized.contains("PLATINUM")) {
            return PlanTier.PLATINUM;
        }

        if (normalized.contains("TEST")) {
            return PlanTier.TEST;
        }

        throw validationFailure("Unknown plan tier.");
    }

    private IllegalArgumentException validationFailure(String message) {
        logger.warn("Form intake mapping failed: {}", message);
        return new IllegalArgumentException(message);
    }

}
