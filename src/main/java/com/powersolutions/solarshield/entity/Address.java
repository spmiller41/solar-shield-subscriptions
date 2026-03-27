package com.powersolutions.solarshield.entity;

import com.powersolutions.solarshield.dto.FormIntakeRequest;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "address")
public class Address {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private int id;

    @Column(name = "street") private String street;
    @Column(name = "city") private String city;
    @Column(name = "state") private String state;
    @Column(name = "zip") private String zip;
    @Column(name = "created_at") private LocalDateTime createdAt;

    public Address() {}

    public Address(FormIntakeRequest request) {
        setStreet(request.getStreet());
        setCity(request.getCity());
        setState(request.getState());
        setZip(request.getZip());
        setCreatedAt(LocalDateTime.now());
    }

    public int getId() { return id; }

    public void setId(int id) { this.id = id; }

    public String getStreet() { return street; }

    public void setStreet(String street) { this.street = street; }

    public String getCity() { return city; }

    public void setCity(String city) { this.city = city; }

    public String getState() { return state; }

    public void setState(String state) { this.state = state; }

    public String getZip() { return zip; }

    public void setZip(String zip) { this.zip = zip; }

    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() {
        return "Address{" +
                "id=" + id +
                ", street='" + street + '\'' +
                ", city='" + city + '\'' +
                ", state='" + state + '\'' +
                ", zip='" + zip + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }

}
