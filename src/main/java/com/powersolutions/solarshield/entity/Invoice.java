package com.powersolutions.solarshield.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "invoice")
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private int id;

    @Column(name = "subscription_id") private int subscriptionId;
    @Column(name = "order_id") private String orderId;
    @Column(name = "amount") private double amount;
    @Column(name = "currency") private String currency;
    @Column(name = "status") private String status;
    @Column(name = "created_at") private LocalDateTime createdAt;
    @Column(name = "updated_at") private LocalDateTime updatedAt;

    public Invoice() {}

    public int getId() { return id; }

    public void setId(int id) { this.id = id; }

    public int getSubscriptionId() { return subscriptionId; }

    public void setSubscriptionId(int subscriptionId) { this.subscriptionId = subscriptionId; }

    public String getOrderId() { return orderId; }

    public void setOrderId(String orderId) { this.orderId = orderId; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public double getAmount() { return amount; }

    public void setAmount(double amount) { this.amount = amount; }

    public String getCurrency() { return currency; }

    public void setCurrency(String currency) { this.currency = currency; }

    public String getStatus() { return status; }

    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() {
        return "Invoice{" +
                "id=" + id +
                ", subscriptionId=" + subscriptionId +
                ", orderId='" + orderId + '\'' +
                ", amount=" + amount +
                ", currency='" + currency + '\'' +
                ", status='" + status + '\'' +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }

}
