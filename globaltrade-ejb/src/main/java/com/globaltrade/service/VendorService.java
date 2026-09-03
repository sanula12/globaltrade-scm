package com.globaltrade.service;

import com.globaltrade.entity.Vendor;
import com.globaltrade.entity.VendorStatus;
import com.globaltrade.exception.VendorValidationException;
import com.globaltrade.intercepter.AuditInterceptor;
import com.globaltrade.intercepter.PerformanceInterceptor;
import com.globaltrade.intercepter.ValidationInterceptor;

import jakarta.annotation.Resource;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.EJB;
import jakarta.ejb.SessionContext;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.interceptor.Interceptors;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.util.List;
import java.util.logging.Logger;

@Stateless
@Interceptors({AuditInterceptor.class, PerformanceInterceptor.class})
public class VendorService {

    private static final Logger logger =
            Logger.getLogger(VendorService.class.getName());

    @PersistenceContext(unitName = "GlobalTradePU")
    private EntityManager em;

    @Resource
    private SessionContext sessionContext;


    @EJB
    private InventoryService inventoryService;

    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    @RolesAllowed({"LOGISTICS_COORDINATOR", "ADMIN", "VENDOR_REPRESENTATIVE"})
    public List<Vendor> findAllVendors() {
        return em.createQuery(
                "SELECT v FROM Vendor v ORDER BY v.name", Vendor.class
        ).getResultList();
    }

    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    @RolesAllowed({"LOGISTICS_COORDINATOR", "ADMIN", "VENDOR_REPRESENTATIVE"})
    public Vendor findById(Long id) {
        Vendor vendor = em.find(Vendor.class, id);
        if (vendor == null) {
            throw new VendorValidationException("Vendor not found with ID: " + id);
        }

        if (sessionContext.isCallerInRole("VENDOR_REPRESENTATIVE")) {
            String callerName = sessionContext.getCallerPrincipal().getName();
            // Check if the caller's username matches this vendor's contact email
            if (!callerName.equals(vendor.getContactEmail())) {
                throw new jakarta.ejb.EJBAccessException(
                        "Vendor representatives can only view their own records"
                );
            }
        }

        return vendor;
    }

    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    @RolesAllowed({"LOGISTICS_COORDINATOR", "ADMIN"})
    public List<Vendor> findByStatus(VendorStatus status) {
        return em.createQuery(
                "SELECT v FROM Vendor v WHERE v.status = :status", Vendor.class
        ).setParameter("status", status).getResultList();
    }

    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    @RolesAllowed({"LOGISTICS_COORDINATOR", "ADMIN"})
    public List<Vendor> findUnderperformingVendors(double threshold) {
        return em.createQuery(
                        "SELECT v FROM Vendor v WHERE v.performanceScore < :threshold "
                                + "AND v.status = :active ORDER BY v.performanceScore ASC",
                        Vendor.class
                ).setParameter("threshold", BigDecimal.valueOf(threshold))
                .setParameter("active",    VendorStatus.ACTIVE)
                .getResultList();
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    @RolesAllowed({"ADMIN"})
    @Interceptors({AuditInterceptor.class, PerformanceInterceptor.class,
            ValidationInterceptor.class})
    public Vendor registerVendor(Vendor vendor) {

        // Check for duplicate vendor code
        List<Vendor> existing = em.createQuery(
                "SELECT v FROM Vendor v WHERE v.vendorCode = :code", Vendor.class
        ).setParameter("code", vendor.getVendorCode()).getResultList();

        if (!existing.isEmpty()) {
            throw new VendorValidationException(
                    "vendorCode",
                    "Vendor code already exists: " + vendor.getVendorCode()
            );
        }

        em.persist(vendor);
        logger.info("New vendor registered: " + vendor.getName()
                + " (" + vendor.getVendorCode() + ")");
        return vendor;
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    @RolesAllowed({"LOGISTICS_COORDINATOR", "ADMIN"})
    public Vendor updatePerformanceScore(Long vendorId, BigDecimal newScore) {

        Vendor vendor = em.find(Vendor.class, vendorId);
        if (vendor == null) {
            throw new VendorValidationException("Vendor not found: " + vendorId);
        }

        BigDecimal oldScore = vendor.getPerformanceScore();
        vendor.setPerformanceScore(newScore);

        // Automatic status change based on performance threshold
        if (newScore.compareTo(BigDecimal.valueOf(60.0)) < 0) {
            vendor.setStatus(VendorStatus.UNDER_REVIEW);
            logger.warning("[VENDOR] " + vendor.getName()
                    + " flagged for review — score dropped to " + newScore);
        } else if (newScore.compareTo(BigDecimal.valueOf(80.0)) >= 0
                && vendor.getStatus() == VendorStatus.UNDER_REVIEW) {

            vendor.setStatus(VendorStatus.ACTIVE);
            logger.info("[VENDOR] " + vendor.getName()
                    + " restored to ACTIVE status — score recovered to " + newScore);
        }

        em.merge(vendor);
        logger.info("[VENDOR] Performance score updated for " + vendor.getName()
                + ": " + oldScore + " → " + newScore);

        return vendor;
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    @RolesAllowed({"ADMIN"})
    public void suspendVendor(Long vendorId, String reason) {

        Vendor vendor = em.find(Vendor.class, vendorId);
        if (vendor == null) {
            throw new VendorValidationException("Vendor not found: " + vendorId);
        }

        vendor.setStatus(VendorStatus.SUSPENDED);
        em.merge(vendor);

        logger.warning("[VENDOR] Vendor SUSPENDED: " + vendor.getName()
                + " | Reason: " + reason
                + " | By: " + sessionContext.getCallerPrincipal().getName());
    }
}
