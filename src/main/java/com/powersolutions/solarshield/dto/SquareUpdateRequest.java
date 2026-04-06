package com.powersolutions.solarshield.dto;

import java.math.BigDecimal;

public class SquareUpdateRequest {

    private String title;
    private String subscriptionId;
    private String customerId;
    private String email;
    private String automaticPaymentSource;
    private BigDecimal amount;
    private String currency;
    private String status;

    public SquareUpdateRequest() {}

    public String getTitle() { return title; }

    public void setTitle(String title) { this.title = title; }

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
        return "SquareUpdateRequest{" +
                "title='" + title + '\'' +
                ", subscriptionId='" + subscriptionId + '\'' +
                ", customerId='" + customerId + '\'' +
                ", email='" + email + '\'' +
                ", automaticPaymentSource='" + automaticPaymentSource + '\'' +
                ", amount=" + amount +
                ", currency='" + currency + '\'' +
                ", status='" + status + '\'' +
                '}';
    }

}