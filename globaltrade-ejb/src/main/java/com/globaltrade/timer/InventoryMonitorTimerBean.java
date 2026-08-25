package com.globaltrade.timer;

import com.globaltrade.service.InventoryService;
import com.globaltrade.service.VendorService;
import com.globaltrade.entity.Vendor;

import javax.ejb.*;
import java.util.List;
import java.util.logging.Logger;

/**
 * InventoryMonitorTimerBean — automated inventory and vendor monitoring.
 *
 * Demonstrates:
 * - Multiple @Schedule timers in ONE bean
 * - Different timer frequencies for different tasks
 * - @Singleton for timer beans that maintain state between firings
 *
 * WHY @Singleton here instead of @Stateless?
 * If we need to track "how many times has low stock been detected
 * in a row?" across timer firings, @Stateless won't work
 * (each firing may use a different pool instance).
 * @Singleton ensures the SAME instance handles every timer callback,
 * so we can maintain counters and state between firings.
 */
@Singleton
public class InventoryMonitorTimerBean {

    private static final Logger logger =
            Logger.getLogger(InventoryMonitorTimerBean.class.getName());

    @EJB
    private InventoryService inventoryService;

    @EJB
    private VendorService vendorService;

    // State maintained between timer firings (only possible with @Singleton)
    private int consecutiveLowStockCount = 0;

    /**
     * DECLARATIVE TIMER: Inventory check every 6 hours.
     *
     * Runs every 6 hours to check inventory levels and alert if low stock.
     * 6-hour interval chosen to balance timely alerts with system load.
     */
    @Schedule(second = "0", minute = "0", hour = "*/6",
            persistent = true, info = "INVENTORY_CHECK")
    public void checkInventoryLevels() {

        logger.info("[TIMER] Inventory check triggered");

        try {
            var lowStockItems = inventoryService.checkAndAlertLowStock();

            if (!lowStockItems.isEmpty()) {
                consecutiveLowStockCount++;
                logger.warning("[TIMER] Low stock detected "
                        + consecutiveLowStockCount
                        + " consecutive check(s)");

                // After 3 consecutive low-stock alerts, escalate urgency
                if (consecutiveLowStockCount >= 3) {
                    logger.severe("[TIMER] CRITICAL: Low stock persisting "
                            + "for " + consecutiveLowStockCount + " checks! "
                            + "Immediate procurement action required!");
                }
            } else {
                // Reset counter when stock is healthy
                consecutiveLowStockCount = 0;
                logger.info("[TIMER] All inventory levels healthy.");
            }

        } catch (Exception e) {
            logger.severe("[TIMER] Inventory check failed: " + e.getMessage());
        }
    }

    /**
     * DECLARATIVE TIMER: Daily vendor performance review at 2:00 AM.
     *
     * Runs at 2 AM every day to evaluate vendor performance scores.
     * 2 AM chosen because it's low traffic — fits EJB best practice
     * of scheduling resource-intensive tasks during off-peak hours.
     */
    @Schedule(second = "0", minute = "0", hour = "2",
            dayOfWeek = "*", persistent = true,
            info = "VENDOR_PERFORMANCE_CHECK")
    public void reviewVendorPerformance() {

        logger.info("[TIMER] Vendor performance review started");

        try {
            // Find vendors with performance score below 75%
            List<Vendor> underperforming =
                    vendorService.findUnderperformingVendors(75.0);

            if (underperforming.isEmpty()) {
                logger.info("[TIMER] All vendors performing above threshold.");
            } else {
                logger.warning("[TIMER] " + underperforming.size()
                        + " underperforming vendors detected:");

                for (Vendor v : underperforming) {
                    logger.warning("[TIMER] Vendor: " + v.getName()
                            + " | Country: " + v.getCountry()
                            + " | Score: " + v.getPerformanceScore()
                            + " | Status: " + v.getStatus());
                }
            }

        } catch (Exception e) {
            logger.severe("[TIMER] Vendor review failed: " + e.getMessage());
        }
    }
}