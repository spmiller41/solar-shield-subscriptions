package com.powersolutions.solarshield.enums;

public enum SquarePaymentStatus {

    APPROVED,
    COMPLETED,
    FAILED,
    CANCELED,
    PENDING;

    public static SquarePaymentStatus fromValue(String value) {
        try {
            return SquarePaymentStatus.valueOf(value);
        } catch (Exception e) {
            return null;
        }
    }

}