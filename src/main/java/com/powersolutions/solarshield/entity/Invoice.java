package com.powersolutions.solarshield.entity;

import com.powersolutions.solarshield.dto.SquareInvoicePaymentRequest;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "invoice")
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private int id;

    // FK -> Subscription table
    @Column(name = "subscription_id") private Integer subscriptionId;

    @Column(name = "customer_subscription_id") private String customerSubscriptionId;
    @Column(name = "order_id") private String orderId;
    @Column(name = "amount") private BigDecimal amount;
    @Column(name = "currency") private String currency;
    @Column(name = "status") private String status;
    @Column(name = "updated_at") private LocalDateTime updatedAt;

    public Invoice() {}

    public Invoice(Subscription subscription, SquareInvoicePaymentRequest request) {
        setSubscriptionId(subscription.getId());
        setCustomerSubscriptionId(request.getSubscriptionId());
        setOrderId(request.getOrderId());
        setAmount(request.getAmount());
        setCurrency(request.getCurrency());
        setStatus(request.getStatus());
        setUpdatedAt(LocalDateTime.now());
    }

    public int getId() { return id; }

    public void setId(Integer id) { this.id = id; }

    public String getCustomerSubscriptionId() { return customerSubscriptionId; }

    public void setCustomerSubscriptionId(String customerSubscriptionId) { this.customerSubscriptionId = customerSubscriptionId;}

    public Integer getSubscriptionId() { return subscriptionId; }

    public void setSubscriptionId(int subscriptionId) { this.subscriptionId = subscriptionId; }

    public String getOrderId() { return orderId; }

    public void setOrderId(String orderId) { this.orderId = orderId; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public BigDecimal getAmount() { return amount; }

    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getCurrency() { return currency; }

    public void setCurrency(String currency) { this.currency = currency; }

    public String getStatus() { return status; }

    public void setStatus(String status) { this.status = status; }

    @Override
    public String toString() {
        return "Invoice{" +
                "id=" + id +
                ", subscriptionId=" + subscriptionId +
                ", customerSubscriptionId='" + customerSubscriptionId + '\'' +
                ", orderId='" + orderId + '\'' +
                ", amount=" + amount +
                ", currency='" + currency + '\'' +
                ", status='" + status + '\'' +
                ", updatedAt=" + updatedAt +
                '}';
    }

}
