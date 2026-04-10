package com.powersolutions.solarshield.entity;

import com.powersolutions.solarshield.dto.FormIntakeRequest;
import com.powersolutions.solarshield.enums.PlanTier;
import com.powersolutions.solarshield.enums.SubscriptionStatus;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "subscription")
public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private int id;

    // FK
    @Column(name = "contact_id") public int contactId;

    // FK
    @Column(name = "address_id") public int addressId;

    @Enumerated(EnumType.STRING) @Column(name = "plan_tier") public PlanTier planTier;
    @Enumerated(EnumType.STRING) @Column(name = "subscription_status") public SubscriptionStatus subscriptionStatus;
    @Column(name = "idempotency_key") public String idempotencyKey;
    @Column(name = "customer_subscription_id") public String customerSubscriptionId;
    @Column(name = "customerId") public String customerId;
    @Column(name = "email") public String email;
    @Column(name = "created_at") public LocalDateTime createdAt;
    @Column(name = "updated_at") public LocalDateTime updatedAt;
    @Column(name = "activated_at") public LocalDateTime activatedAt;


    public Subscription() {}

    public Subscription(FormIntakeRequest request, Contact contact, Address address, SubscriptionStatus status) {
        setContactId(contact.getId());
        setAddressId(address.getId());
        setIdempotencyKey(UUID.randomUUID().toString());
        setPlanTier(request.getPlanTier());
        setSubscriptionStatus(status);
        setCreatedAt(LocalDateTime.now());
        setUpdatedAt(LocalDateTime.now());

    }

    public int getId() { return id; }

    public void setId(int id) { this.id = id; }

    public int getContactId() { return contactId; }

    public void setContactId(int contactId) { this.contactId = contactId; }

    public int getAddressId() { return addressId; }

    public void setAddressId(int addressId) { this.addressId = addressId; }

    public String getIdempotencyKey() { return idempotencyKey; }

    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }

    public PlanTier getPlanTier() { return planTier; }

    public void setPlanTier(PlanTier planTier) { this.planTier = planTier; }

    public SubscriptionStatus getSubscriptionStatus() { return subscriptionStatus; }

    public void setSubscriptionStatus(SubscriptionStatus subscriptionStatus) { this.subscriptionStatus = subscriptionStatus; }

    public String getCustomerSubscriptionId() { return customerSubscriptionId; }

    public void setCustomerSubscriptionId(String customerSubscriptionId) { this.customerSubscriptionId = customerSubscriptionId; }

    public String getCustomerId() { return customerId; }

    public void setCustomerId(String customerId) { this.customerId = customerId; }

    public String getEmail() { return email; }

    public void setEmail(String email) { this.email = email; }

    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public LocalDateTime getActivatedAt() { return activatedAt; }

    public void setActivatedAt(LocalDateTime activatedAt) { this.activatedAt = activatedAt; }

    @Override
    public String toString() {
        return "Subscription{" +
                "id=" + id +
                ", contactId=" + contactId +
                ", addressId=" + addressId +
                ", planTier=" + planTier +
                ", subscriptionStatus=" + subscriptionStatus +
                ", idempotencyKey='" + idempotencyKey + '\'' +
                ", customerSubscriptionId='" + customerSubscriptionId + '\'' +
                ", customerId='" + customerId + '\'' +
                ", email='" + email + '\'' +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                ", activatedAt=" + activatedAt +
                '}';
    }

}
