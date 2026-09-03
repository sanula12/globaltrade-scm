package com.globaltrade.entity;

import jakarta.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


@Entity
@Table(name = "vendors")
public class Vendor implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "vendor_code", nullable = false, unique = true, length = 20)
    private String vendorCode;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "country", length = 50)
    private String country;

    @Column(name = "contact_email", length = 100)
    private String contactEmail;

    @Column(name = "performance_score", precision = 5, scale = 2)
    private BigDecimal performanceScore = BigDecimal.valueOf(100.00);

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    private VendorStatus status = VendorStatus.ACTIVE;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "vendor", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<Shipment> shipments = new ArrayList<>();

    @PrePersist
    protected void onCreate(){
        this.createdAt = LocalDateTime.now();
    }

    public Vendor() {}
    public Vendor(String vendorCode, String name, String country) {
        this.vendorCode = vendorCode;
        this.name = name;
        this.country = country;
    }

    public Long getId() { return id; }

    public String getVendorCode() { return vendorCode; }

    public void setVendorCode(String vendorCode) { this.vendorCode = vendorCode; }

    public String getName() { return name; }

    public void setName(String name) { this.name = name; }

    public String getCountry() { return country; }

    public void setCountry(String country) { this.country = country; }

    public String getContactEmail() { return contactEmail; }

    public void setContactEmail(String contactEmail) { this.contactEmail = contactEmail; }

    public BigDecimal getPerformanceScore() { return performanceScore; }

    public void setPerformanceScore(BigDecimal performanceScore) {
        this.performanceScore = performanceScore;
    }

    public VendorStatus getStatus() { return status; }

    public void setStatus(VendorStatus status) { this.status = status; }

    public List<Shipment> getShipments() { return shipments; }

    public void setShipments(List<Shipment> shipments) { this.shipments = shipments; }

    public LocalDateTime getCreatedAt() { return createdAt; }

    @Override
    public String toString() {
        return "Vendor{id=" + id + ", code='" + vendorCode + "', name='" + name + "'}";
    }

}
