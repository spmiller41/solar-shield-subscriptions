package com.powersolutions.solarshield.dto;

public class SquareUpdateRequest {

    private String eventType;
    private String invoiceStatus;
    private String customerId;
    private String customerEmail;

    public SquareUpdateRequest() {}

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getInvoiceStatus() {
        return invoiceStatus;
    }

    public void setInvoiceStatus(String invoiceStatus) {
        this.invoiceStatus = invoiceStatus;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public String getCustomerEmail() {
        return customerEmail;
    }

    public void setCustomerEmail(String customerEmail) {
        this.customerEmail = customerEmail;
    }

    @Override
    public String toString() {
        return "SquareUpdateRequest{" +
                "eventType='" + eventType + '\'' +
                ", invoiceStatus='" + invoiceStatus + '\'' +
                ", customerId='" + customerId + '\'' +
                ", customerEmail='" + customerEmail + '\'' +
                '}';
    }

}