package com.powersolutions.solarshield.entity;

import com.powersolutions.solarshield.dto.SquareInvoicePaymentRequest;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "square_webhook_event")
public class SquareWebhookEvent {

    @Id @Column(name = "event_id") private String eventId;
    @Column(name = "created_at") private LocalDateTime createdAt;

    public SquareWebhookEvent() {}

    public SquareWebhookEvent(SquareInvoicePaymentRequest request) {
        setEventId(request.getEventId());
        setCreatedAt(LocalDateTime.now());
    }

    public String getEventId() { return eventId; }

    public void setEventId(String eventId) { this.eventId = eventId; }

    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() {
        return "SquareWebhookEvent{" +
                "eventId='" + eventId + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }

}