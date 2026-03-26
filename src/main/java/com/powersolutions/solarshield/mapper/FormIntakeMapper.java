package com.powersolutions.solarshield.mapper;

import com.powersolutions.solarshield.dto.FormIntakeRequest;
import com.powersolutions.solarshield.enums.PlanTier;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

public class FormIntakeMapper {

    private final FormIntakeRequest request;

    public FormIntakeMapper(@RequestParam Map<String, String> formParams) {
        request = new FormIntakeRequest();

        request.setFirstName(trim(formParams.get("First Name")));
        request.setLastName(trim(formParams.get("Last Name")));
        request.setEmail(trim(formParams.get("Email")));
        request.setPhone(normalizePhone(formParams.get("Phone")));
        request.setStreet(trim(formParams.get("Address")));
        request.setCity(trim(formParams.get("City")));
        request.setState(normalizeState(formParams.get("US States")));
        request.setZip(trim(formParams.get("Zip")));
        request.setPlanTier(mapPlanTier(formParams.get("Home Solar Shield Level")));
    }

    public FormIntakeRequest getRequest() { return request; }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }

    private String normalizePhone(String phone) {
        if (phone == null || phone.isBlank()) {
            throw new IllegalArgumentException("Phone is required.");
        }

        String digits = phone.replaceAll("\\D", "");

        if (digits.length() == 10) {
            return "+1" + digits;
        }

        if (digits.length() == 11 && digits.startsWith("1")) {
            return "+" + digits;
        }

        throw new IllegalArgumentException("Invalid phone number: " + phone);
    }

    private String normalizeState(String state) {
        if (state == null || state.isBlank()) {
            throw new IllegalArgumentException("State is required.");
        }

        String normalized = state.trim().toUpperCase();

        return switch (normalized) {
            case "NY", "NEW YORK" -> "NY";
            default -> normalized;
        };
    }

    private PlanTier mapPlanTier(String level) {
        if (level == null || level.isBlank()) {
            throw new IllegalArgumentException("Plan tier is required.");
        }

        String normalized = level.trim().toUpperCase();

        if (normalized.contains("SILVER")) { return PlanTier.SILVER; }

        if (normalized.contains("GOLD")) { return PlanTier.GOLD; }

        if (normalized.contains("PLATINUM")) { return PlanTier.PLATINUM; }

        throw new IllegalArgumentException("Unknown plan tier: " + level);
    }

}
