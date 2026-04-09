package com.powersolutions.solarshield.dto;

public class SquareCheckoutResponse {

    private final String checkoutLink;
    private final String orderId;
    private final String paymentLinkId;

    public SquareCheckoutResponse(String checkoutLink, String orderId, String paymentLinkId) {
        this.checkoutLink = checkoutLink;
        this.orderId = orderId;
        this.paymentLinkId = paymentLinkId;
    }

    public String getCheckoutLink() { return checkoutLink; }

    public String getOrderId() { return orderId; }

    public String getPaymentLinkId() { return paymentLinkId; }

    @Override
    public String toString() {
        return "SquareCheckoutResponse{" +
                "checkoutLink='" + checkoutLink + '\'' +
                ", orderId='" + orderId + '\'' +
                ", paymentLinkId='" + paymentLinkId + '\'' +
                '}';
    }

}