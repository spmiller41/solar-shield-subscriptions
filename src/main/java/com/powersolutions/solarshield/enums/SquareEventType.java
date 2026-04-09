package com.powersolutions.solarshield.enums;

public enum SquareEventType {

    INVOICE_CREATED("invoice.created"),
    INVOICE_UPDATED("invoice.updated"),
    INVOICE_PAYMENT_MADE("invoice.payment_made"),
    PAYMENT_CREATED("payment.created"),
    PAYMENT_UPDATED("payment.updated"),
    INVOICE_SCHEDULED_CHARGE_FAILED("invoice.scheduled_charge_failed");

    private final String value;

    SquareEventType(String value) { this.value = value; }

    public String getValue() { return value; }

    public static SquareEventType fromValue(String value) {
        for (SquareEventType type : values()) {
            if (type.value.equals(value)) {
                return type;
            }
        }
        return null;
    }

}