package com.powersolutions.solarshield.dto;

import com.powersolutions.solarshield.enums.SquareEventType;

import java.math.BigDecimal;

public class SquareInvoicePaymentRequest {

    private SquareEventType eventType;
    private String title;
    private String subscriptionId;
    private String orderId;
    private String customerId;
    private String email;
    private String automaticPaymentSource;
    private BigDecimal amount;
    private String currency;
    private String status;

    public SquareInvoicePaymentRequest() {}

    public SquareEventType getEventType() { return eventType; }

    public void setEventType(SquareEventType eventType) { this.eventType = eventType; }

    public String getTitle() { return title; }

    public void setTitle(String title) { this.title = title; }

    public String getOrderId() { return orderId; }

    public void setOrderId(String orderId) { this.orderId = orderId; }

    public String getSubscriptionId() { return subscriptionId; }

    public void setSubscriptionId(String subscriptionId) { this.subscriptionId = subscriptionId; }

    public String getCustomerId() { return customerId; }

    public void setCustomerId(String customerId) { this.customerId = customerId; }

    public String getEmail() { return email; }

    public void setEmail(String email) { this.email = email; }

    public String getAutomaticPaymentSource() { return automaticPaymentSource; }

    public void setAutomaticPaymentSource(String automaticPaymentSource) { this.automaticPaymentSource = automaticPaymentSource; }

    public BigDecimal getAmount() { return amount; }

    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getCurrency() { return currency; }

    public void setCurrency(String currency) { this.currency = currency; }

    public String getStatus() { return status; }

    public void setStatus(String status) { this.status = status; }

    @Override
    public String toString() {
        return "SquareInvoicePaymentRequest{" +
                "eventType='" + eventType + '\'' +
                ", title='" + title + '\'' +
                ", subscriptionId='" + subscriptionId + '\'' +
                ", orderId='" + orderId + '\'' +
                ", customerId='" + customerId + '\'' +
                ", email='" + email + '\'' +
                ", automaticPaymentSource='" + automaticPaymentSource + '\'' +
                ", amount=" + amount +
                ", currency='" + currency + '\'' +
                ", status='" + status + '\'' +
                '}';
    }

}