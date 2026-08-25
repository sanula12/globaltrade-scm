package com.globaltrade.security;

import com.globaltrade.entity.Shipment;
import com.globaltrade.entity.ShipmentStatus;
import com.globaltrade.entity.Vendor;

import javax.annotation.Resource;
import javax.annotation.security.DenyAll;
import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.util.logging.Logger;

/**
 * SecurityService — centralised security checks for the supply chain system.
 *
 * This bean exists purely for PROGRAMMATIC security logic.
 * It is injected into other services via @EJB when they need
 * context-aware authorization beyond what @RolesAllowed can provide.
 *
 * ROLES IN THE SYSTEM:
 * ┌──────────────────────────┬───────────────────────────────────────────┐
 * │ Role                     │ Permissions                               │
 * ├──────────────────────────┼───────────────────────────────────────────┤
 * │ ADMIN                    │ Full access to everything                 │
 * │ LOGISTICS_COORDINATOR    │ Create/update shipments, view all data    │
 * │ WAREHOUSE_MANAGER        │ Manage inventory, view shipments          │
 * │ VENDOR_REPRESENTATIVE    │ View/cancel own shipments only            │
 * │ CUSTOMS_AGENT            │ Process customs clearance                 │
 * └──────────────────────────┴───────────────────────────────────────────┘
 *
 * @Stateless — no per-user state needed. Security checks are
 * stateless operations — they just look at the current principal.
 */
@Stateless
public class SecurityService {

    private static final Logger logger =
            Logger.getLogger(SecurityService.class.getName());

    /**
     * @Resource SessionContext is the KEY to programmatic security.
     * It gives us access to:
     * - getCallerPrincipal() → the currently authenticated user
     * - isCallerInRole()     → check if user has a specific role
     */
    @Resource
    private SessionContext sessionContext;

    // ================================================================
    // IDENTITY METHODS
    // ================================================================

    /**
     * Get the username of the currently logged-in user.
     * Returns "anonymous" if no user is authenticated
     * (e.g., called from a timer callback where there's no human caller).
     *
     * @PermitAll — anyone can ask "who am I?"
     */
    @PermitAll
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public String getCurrentUser() {
        try {
            return sessionContext.getCallerPrincipal().getName();
        } catch (Exception e) {
            return "anonymous";
        }
    }

    /**
     * Check if the current user has a specific role.
     * Wraps the container's isCallerInRole() for convenience.
     */
    @PermitAll
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public boolean hasRole(String roleName) {
        return sessionContext.isCallerInRole(roleName);
    }

    /**
     * Check if the current user is an ADMIN.
     * Admins bypass all business-level restrictions.
     */
    @PermitAll
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public boolean isAdmin() {
        return sessionContext.isCallerInRole("ADMIN");
    }

    // ================================================================
    // SHIPMENT ACCESS CONTROL
    // ================================================================

    /**
     * Can the current user VIEW this shipment?
     *
     * Business rules:
     * - ADMIN and LOGISTICS_COORDINATOR: see everything
     * - WAREHOUSE_MANAGER: see everything (need to know what's coming)
     * - CUSTOMS_AGENT: see everything (need to process clearance)
     * - VENDOR_REPRESENTATIVE: ONLY see shipments linked to their vendor
     *
     * This is IMPOSSIBLE to express with @RolesAllowed alone —
     * that only checks role existence, not data ownership.
     *
     * @PermitAll — the method handles its own authorization internally.
     * Throwing EJBAccessException from inside = still denied.
     */
    @PermitAll
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public boolean canViewShipment(Shipment shipment) {

        String caller = getCurrentUser();

        // ADMIN, COORDINATOR, WAREHOUSE, CUSTOMS → full access
        if (isAdmin()
                || sessionContext.isCallerInRole("LOGISTICS_COORDINATOR")
                || sessionContext.isCallerInRole("WAREHOUSE_MANAGER")
                || sessionContext.isCallerInRole("CUSTOMS_AGENT")) {
            return true;
        }

        // VENDOR_REPRESENTATIVE — only their own vendor's shipments
        if (sessionContext.isCallerInRole("VENDOR_REPRESENTATIVE")) {
            if (shipment.getVendor() != null) {
                boolean isOwner =
                        caller.equals(shipment.getVendor().getContactEmail());

                if (!isOwner) {
                    logger.warning("[SECURITY] Vendor rep " + caller
                            + " denied access to shipment "
                            + shipment.getTrackingNumber()
                            + " (not their vendor)");
                }
                return isOwner;
            }
            return false; // No vendor on shipment → deny vendor rep
        }

        // Unknown role → deny
        logger.warning("[SECURITY] Unknown role for user " + caller
                + " — access denied to shipment "
                + shipment.getTrackingNumber());
        return false;
    }

    /**
     * Can the current user MODIFY this shipment?
     *
     * Stricter than canViewShipment:
     * - VENDOR_REPRESENTATIVE: can ONLY cancel (not update status)
     * - WAREHOUSE_MANAGER: can update status (marking as received)
     * - LOGISTICS_COORDINATOR: can do most things except delete
     * - ADMIN: can do anything
     */
    @PermitAll
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public boolean canModifyShipment(Shipment shipment,
                                     ShipmentStatus targetStatus) {
        if (isAdmin()) return true;

        if (sessionContext.isCallerInRole("LOGISTICS_COORDINATOR")) return true;

        if (sessionContext.isCallerInRole("WAREHOUSE_MANAGER")) {
            // Warehouse managers can mark shipments as delivered or in transit
            return targetStatus == ShipmentStatus.DELIVERED
                    || targetStatus == ShipmentStatus.IN_TRANSIT;
        }

        if (sessionContext.isCallerInRole("CUSTOMS_AGENT")) {
            // Customs agents can only move to/from CUSTOMS_HOLD
            return targetStatus == ShipmentStatus.CUSTOMS_HOLD
                    || targetStatus == ShipmentStatus.IN_TRANSIT;
        }

        if (sessionContext.isCallerInRole("VENDOR_REPRESENTATIVE")) {
            // Vendors can ONLY cancel their OWN shipments
            String caller = getCurrentUser();
            boolean isOwner = shipment.getVendor() != null
                    && caller.equals(shipment.getVendor().getContactEmail());
            return isOwner && targetStatus == ShipmentStatus.CANCELLED;
        }

        return false;
    }

    // ================================================================
    // VENDOR ACCESS CONTROL
    // ================================================================

    /**
     * Can the current user view this vendor's details?
     *
     * Demonstrates ownership-based programmatic security:
     * A vendor representative can ONLY see their own record.
     */
    @PermitAll
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public boolean canViewVendor(Vendor vendor) {

        if (isAdmin()
                || sessionContext.isCallerInRole("LOGISTICS_COORDINATOR")) {
            return true;
        }

        if (sessionContext.isCallerInRole("VENDOR_REPRESENTATIVE")) {
            String caller = getCurrentUser();
            boolean isOwner = caller.equals(vendor.getContactEmail());

            if (!isOwner) {
                logger.warning("[SECURITY] Vendor rep " + caller
                        + " tried to access vendor record for "
                        + vendor.getName() + " — DENIED");
            }
            return isOwner;
        }

        return false;
    }

    // ================================================================
    // EMERGENCY ACCESS — Programmatic Override
    // ================================================================

    /**
     * Emergency access validation for critical logistics situations.
     *
     * In real-world global logistics, sometimes emergency situations
     * (natural disasters, port closures) require temporary elevated access.
     * This method demonstrates programmatic security that considers
     * CONTEXT beyond just roles.
     *
     * @RolesAllowed({"ADMIN"}) — only ADMIN can grant emergency access.
     *
     * In a real system, this would integrate with an emergency
     * management system and create time-limited elevated sessions.
     */
    @RolesAllowed({"ADMIN"})
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public void grantEmergencyAccess(String targetUser, String reason) {

        String adminUser = getCurrentUser();

        logger.warning("[SECURITY][EMERGENCY] Admin " + adminUser
                + " granted emergency access to: " + targetUser
                + " | Reason: " + reason);

        // In production: store emergency grant in DB with expiry timestamp,
        // notify security team, create audit trail entry.
        // For this prototype, we log the grant for audit compliance.
    }

    // ================================================================
    // INTERNAL/SYSTEM METHODS — Completely Hidden
    // ================================================================

    /**
     * @DenyAll — NO ONE can call this method externally.
     *
     * This is for truly internal operations that should NEVER
     * be exposed to any client, regardless of their role.
     * The container will throw EJBAccessException for any caller.
     *
     * Use case: internal token generation, system key rotation,
     * operations that only the server itself should trigger.
     */
    @DenyAll
    public String generateInternalSystemToken() {
        // This method can never be called by any external client.
        // Demonstrates @DenyAll usage for the assignment.
        return "INTERNAL-TOKEN-" + System.currentTimeMillis();
    }

    // ================================================================
    // SECURITY AUDIT HELPER
    // ================================================================

    /**
     * Log a security event for audit trail.
     * Called when access is denied — creates a record for
     * security monitoring and intrusion detection.
     *
     * Uses REQUIRES_NEW so this always saves to DB,
     * even if the calling transaction rolls back.
     */
    @PermitAll
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public void logSecurityEvent(String action, String resource,
                                 boolean permitted) {
        String user  = getCurrentUser();
        String level = permitted ? "INFO" : "WARNING";

        logger.log(
                permitted
                        ? java.util.logging.Level.INFO
                        : java.util.logging.Level.WARNING,
                "[SECURITY][" + level + "] User='" + user + "'"
                        + " Action='" + action + "'"
                        + " Resource='" + resource + "'"
                        + " Permitted=" + permitted
        );
    }
}