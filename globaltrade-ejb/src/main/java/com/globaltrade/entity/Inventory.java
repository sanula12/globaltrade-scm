package com.globaltrade.entity;

import jakarta.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "inventory")
@NamedQueries({
        @NamedQuery(
                name = "Inventory.findLowStock",
                query = "SELECT i FROM Inventory i WHERE i.quantity <= i.reorderThreshold"
        ),
        @NamedQuery(
                name  = "Inventory.findBySku",
                query = "SELECT i FROM Inventory i WHERE i.productSku = :sku"
        ),

        @NamedQuery(
                name  = "Inventory.findByWarehouse",
                query = "SELECT i FROM Inventory i WHERE i.warehouseLocation = :location"
        )

})
public class Inventory implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_sku", nullable = false, length = 50)
    private String productSku;

    @Column(name = "product_name", nullable = false, length = 100)
    private String productName;

    @Column(name = "warehouse_location", nullable = false, length = 100)
    private String warehouseLocation;

    @Column(name = "quantity")
    private Integer quantity = 0;

    @Column(name = "reorder_threshold")
    private Integer reorderThreshold = 10;

    @Column(name = "unit_price", precision = 10, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;

    @PrePersist
    @PostUpdate
    protected void onUpdate() {
        this.lastUpdated = LocalDateTime.now();
    }

    public Inventory() {}
    public Inventory(String productSku, String productName,
                     String warehouseLocation, Integer quantity) {
        this.productSku = productSku;
        this.productName = productName;
        this.warehouseLocation = warehouseLocation;
        this.quantity = quantity;
    }

    public Long getId() { return id; }

    public String getProductSku() { return productSku; }

    public void setProductSku(String productSku) { this.productSku = productSku; }

    public String getProductName() { return productName; }

    public void setProductName(String productName) { this.productName = productName; }

    public String getWarehouseLocation() { return warehouseLocation; }

    public void setWarehouseLocation(String warehouseLocation) {
        this.warehouseLocation = warehouseLocation;
    }

    public Integer getQuantity() { return quantity; }

    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public Integer getReorderThreshold() { return reorderThreshold; }

    public void setReorderThreshold(Integer reorderThreshold) {
        this.reorderThreshold = reorderThreshold;
    }

    public BigDecimal getUnitPrice() { return unitPrice; }

    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }

    public LocalDateTime getLastUpdated() { return lastUpdated; }

    public boolean isLowStock() {
        return quantity != null && reorderThreshold != null
                && quantity <= reorderThreshold;
    }

    @Override
    public String toString() {
        return "Inventory{sku='" + productSku + "', qty=" + quantity
                + ", location='" + warehouseLocation + "'}";
    }

}
