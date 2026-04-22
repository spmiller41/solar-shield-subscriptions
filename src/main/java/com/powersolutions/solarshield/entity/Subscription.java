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
    @Column(name = "contact_id") private int contactId;

    // FK
    @Column(name = "address_id") private int addressId;

    @Enumerated(EnumType.STRING) @Column(name = "plan_tier") private PlanTier planTier;
    @Enumerated(EnumType.STRING) @Column(name = "subscription_status") private SubscriptionStatus subscriptionStatus;

    @Column(name = "customer_subscription_id") private String customerSubscriptionId;
    @Column(name = "square_order_id") private String squareOrderId;
    @Column(name = "customerId") private String customerId;
    @Column(name = "email") private String email;
    @Column(name = "created_at") private LocalDateTime createdAt;
    @Column(name = "updated_at") private LocalDateTime updatedAt;
    @Column(name = "activated_at") private LocalDateTime activatedAt;
    @Column(name = "square_checkout_link") private String squareCheckoutLink;
    @Column(name = "zoho_record_id") private String zohoRecordId;

    @Column(name = "external_uid", nullable = false, unique = true, updatable = false)
    private String externalUid;

    public Subscription() {}

    public Subscription(FormIntakeRequest request, Contact contact, Address address, SubscriptionStatus status) {
        setContactId(contact.getId());
        setAddressId(address.getId());
        setPlanTier(request.getPlanTier());
        setSubscriptionStatus(status);
        setCreatedAt(LocalDateTime.now());
        setUpdatedAt(LocalDateTime.now());
    }

    @PrePersist
    private void prePersist() {
        if (externalUid == null || externalUid.isBlank()) {
            externalUid = UUID.randomUUID().toString();
        }
    }

    public int getId() { return id; }

    public void setId(int id) { this.id = id; }

    public int getContactId() { return contactId; }

    public void setContactId(int contactId) { this.contactId = contactId; }

    public int getAddressId() { return addressId; }

    public void setAddressId(int addressId) { this.addressId = addressId; }

    public PlanTier getPlanTier() { return planTier; }

    public void setPlanTier(PlanTier planTier) { this.planTier = planTier; }

    public SubscriptionStatus getSubscriptionStatus() { return subscriptionStatus; }

    public void setSubscriptionStatus(SubscriptionStatus subscriptionStatus) { this.subscriptionStatus = subscriptionStatus; }

    public String getCustomerSubscriptionId() { return customerSubscriptionId; }

    public void setCustomerSubscriptionId(String customerSubscriptionId) { this.customerSubscriptionId = customerSubscriptionId; }

    public String getSquareOrderId() { return squareOrderId; }

    public void setSquareOrderId(String squareOrderId) { this.squareOrderId = squareOrderId; }

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

    public String getSquareCheckoutLink() { return squareCheckoutLink; }

    public void setSquareCheckoutLink(String squareCheckoutLink) { this.squareCheckoutLink = squareCheckoutLink; }

    public String getExternalUid() { return externalUid; }

    public String getZohoRecordId() { return zohoRecordId; }

    public void setZohoRecordId(String zohoRecordId) { this.zohoRecordId = zohoRecordId; }

    @Override
    public String toString() {
        return "Subscription{" +
                "id=" + id +
                ", contactId=" + contactId +
                ", addressId=" + addressId +
                ", planTier=" + planTier +
                ", subscriptionStatus=" + subscriptionStatus +
                ", customerSubscriptionId='" + customerSubscriptionId + '\'' +
                ", squareOrderId='" + squareOrderId + '\'' +
                ", customerId='" + customerId + '\'' +
                ", email='" + email + '\'' +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                ", activatedAt=" + activatedAt +
                ", squareCheckoutLink='" + squareCheckoutLink + '\'' +
                ", zohoRecordId='" + zohoRecordId + '\'' +
                ", externalUid='" + externalUid + '\'' +
                '}';
    }

}