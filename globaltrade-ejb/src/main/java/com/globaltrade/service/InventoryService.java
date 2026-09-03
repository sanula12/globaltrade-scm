package com.globaltrade.service;

import com.globaltrade.entity.Inventory;
import com.globaltrade.exception.InventoryShortageException;
import com.globaltrade.intercepter.AuditInterceptor;
import com.globaltrade.intercepter.PerformanceInterceptor;
import com.globaltrade.intercepter.ValidationInterceptor;

import jakarta.annotation.Resource;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.SessionContext;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.interceptor.Interceptors;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import java.util.logging.Logger;

@Stateless
@Interceptors({AuditInterceptor.class, PerformanceInterceptor.class})
public class InventoryService {

    private static final Logger logger =
            Logger.getLogger(InventoryService.class.getName());

    @PersistenceContext(unitName = "GlobalTradePU")
    private EntityManager em;

    @Resource
    private SessionContext sessionContext;

    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    @RolesAllowed({"WAREHOUSE_MANAGER", "LOGISTICS_COORDINATOR", "ADMIN"})
    public List<Inventory> findLowStockItems() {
        return em.createNamedQuery("Inventory.findLowStock", Inventory.class)
                .getResultList();
    }

    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    @RolesAllowed({"WAREHOUSE_MANAGER", "LOGISTICS_COORDINATOR", "ADMIN"})
    public List<Inventory> findByWarehouse(String warehouseLocation) {
        return em.createNamedQuery("Inventory.findByWarehouse", Inventory.class)
                .setParameter("location", warehouseLocation)
                .getResultList();
    }

    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    @RolesAllowed({"WAREHOUSE_MANAGER", "LOGISTICS_COORDINATOR", "ADMIN"})
    public Inventory findBySku(String sku) {
        List<Inventory> results =
                em.createNamedQuery("Inventory.findBySku", Inventory.class)
                        .setParameter("sku", sku)
                        .getResultList();

        if (results.isEmpty()) {
            throw new RuntimeException("Inventory not found for SKU: " + sku);
        }
        return results.get(0);
    }

    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    @RolesAllowed({"WAREHOUSE_MANAGER", "LOGISTICS_COORDINATOR", "ADMIN"})
    public List<Inventory> findAll() {
        return em.createQuery("SELECT i FROM Inventory i ORDER BY i.productName",
                        Inventory.class)
                .getResultList();
    }



    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    @RolesAllowed({"WAREHOUSE_MANAGER", "ADMIN"})
    @Interceptors({AuditInterceptor.class, PerformanceInterceptor.class,
            ValidationInterceptor.class})
    public Inventory addInventory(Inventory inventory) {
        em.persist(inventory);
        logger.info("New inventory added: " + inventory.getProductSku()
                + " at " + inventory.getWarehouseLocation());
        return inventory;
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    @RolesAllowed({"LOGISTICS_COORDINATOR", "ADMIN"})
    public void reserveStock(String sku, int quantityNeeded) {

        Inventory inventory = findBySku(sku);

        logger.info("Reserving " + quantityNeeded + " units of " + sku
                + " (available: " + inventory.getQuantity() + ")");

        if (inventory.getQuantity() < quantityNeeded) {

            throw new InventoryShortageException(
                    sku, quantityNeeded, inventory.getQuantity()
            );
        }

        inventory.setQuantity(inventory.getQuantity() - quantityNeeded);
        em.merge(inventory);

        if (inventory.isLowStock()) {
            logger.warning("[INVENTORY] LOW STOCK ALERT: " + sku
                    + " at " + inventory.getWarehouseLocation()
                    + " — only " + inventory.getQuantity() + " units remaining!"
                    + " Reorder threshold: " + inventory.getReorderThreshold());
        }
    }


    @TransactionAttribute(TransactionAttributeType.MANDATORY)
    @RolesAllowed({"WAREHOUSE_MANAGER", "ADMIN"})
    public void restockInventory(String sku, int quantityAdded) {

        Inventory inventory = findBySku(sku);

        int oldQty = inventory.getQuantity();
        inventory.setQuantity(oldQty + quantityAdded);
        em.merge(inventory);

        logger.info("[INVENTORY] Restocked " + sku + ": "
                + oldQty + " → " + inventory.getQuantity() + " units");
    }


    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    @RolesAllowed({"ADMIN", "WAREHOUSE_MANAGER"})
    public List<Inventory> checkAndAlertLowStock() {
        List<Inventory> lowStock = findLowStockItems();

        if (!lowStock.isEmpty()) {
            logger.warning("[TIMER][INVENTORY] " + lowStock.size()
                    + " items below reorder threshold:");
            for (Inventory item : lowStock) {
                logger.warning("  → SKU: " + item.getProductSku()
                        + " | Qty: " + item.getQuantity()
                        + " | Threshold: " + item.getReorderThreshold()
                        + " | Location: " + item.getWarehouseLocation());
            }
        } else {
            logger.info("[TIMER][INVENTORY] All stock levels are healthy.");
        }

        return lowStock;
    }
}
