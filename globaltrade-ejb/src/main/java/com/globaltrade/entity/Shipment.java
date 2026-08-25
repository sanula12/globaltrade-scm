package com.globaltrade.entity;


import javax.persistence.*;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "shipments")
public class Shipment implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tracking_number", nullable = false, unique = true, length = 50)
    private String trackingNumber;

    @Column(name = "origin_country", nullable = false, length = 50)
    private String originCountry;

    @Column(name = "destination_country", nullable = false, length = 50)
    private String destinationCountry;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 30)
    private ShipmentStatus status = ShipmentStatus.PENDING;

    @Column(name = "carrier_name", length = 100)
    private String carrierName;

    @Column(name = "estimated_delivery")
    private LocalDate estimatedDelivery;

    @Column(name = "actual_delivery")
    private LocalDate actualDelivery;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_id")
    private Vendor vendor;

    @Column(name = "customs_cleared")
    private Boolean customsCleared = false;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if(this.status == null){
            this.status = ShipmentStatus.PENDING;
        }
    }

    public Shipment() {}

    public Shipment(String trackingNumber, String originCountry,
                    String destinationCountry, String carrierName){

        this.trackingNumber = trackingNumber;
        this.originCountry = originCountry;
        this.destinationCountry = destinationCountry;
        this.carrierName = carrierName;
    }

    public Long getId() { return id; }

    public String getTrackingNumber() { return trackingNumber; }

    public void setTrackingNumber(String trackingNumber) {
        this.trackingNumber = trackingNumber;
    }
    public String getOriginCountry() { return originCountry; }

    public void setOriginCountry(String originCountry) {
        this.originCountry = originCountry;
    }
    public String getDestinationCountry() { return destinationCountry; }

    public void setDestinationCountry(String destinationCountry) {
        this.destinationCountry = destinationCountry;
    }

    public ShipmentStatus getStatus() { return status; }

    public void setStatus(ShipmentStatus status) { this.status = status; }

    public String getCarrierName() { return carrierName; }

    public void setCarrierName(String carrierName) { this.carrierName = carrierName; }

    public LocalDate getEstimatedDelivery() { return estimatedDelivery; }

    public void setEstimatedDelivery(LocalDate estimatedDelivery) {
        this.estimatedDelivery = estimatedDelivery;
    }

    public LocalDate getActualDelivery() { return actualDelivery; }

    public void setActualDelivery(LocalDate actualDelivery) {
        this.actualDelivery = actualDelivery;
    }

    public Vendor getVendor() { return vendor; }

    public void setVendor(Vendor vendor) { this.vendor = vendor; }

    public Boolean getCustomsCleared() { return customsCleared; }

    public void setCustomsCleared(Boolean customsCleared) {
        this.customsCleared = customsCleared;
    }

    public LocalDateTime getCreatedAt() { return createdAt; }

    @Override
    public String toString(){
        return "Shipment{id=" + id +
                ", trackingNumber='" + trackingNumber + "'" +
                ", status=" + status + "}";
    }

}
