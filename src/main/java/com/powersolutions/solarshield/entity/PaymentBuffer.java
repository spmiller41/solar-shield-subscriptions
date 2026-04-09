package com.powersolutions.solarshield.entity;

import com.powersolutions.solarshield.dto.SquareInvoicePaymentRequest;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "payment_buffer")
public class PaymentBuffer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private int id;

    @Column(name = "order_id") private String orderId;
    @Column(name = "status") private String status;
    @Column(name = "created_at") private LocalDateTime createdAt;

    public PaymentBuffer() {}

    public PaymentBuffer(SquareInvoicePaymentRequest request) {
        setOrderId(request.getOrderId());
        setStatus(request.getStatus());
        setCreatedAt(LocalDateTime.now());
    }

    public int getId() { return id; }

    public void setId(int id) { this.id = id; }

    public String getOrderId() { return orderId; }

    public void setOrderId(String orderId) { this.orderId = orderId; }

    public String getStatus() { return status; }

    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() {
        return "PaymentBuffer{" +
                "id=" + id +
                ", orderId='" + orderId + '\'' +
                ", status='" + status + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }

}