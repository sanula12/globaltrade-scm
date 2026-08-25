package com.globaltrade;

import com.globaltrade.entity.Shipment;
import com.globaltrade.entity.ShipmentStatus;
import com.globaltrade.entity.Vendor;
import com.globaltrade.security.SecurityService;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import javax.ejb.SessionContext;
import java.security.Principal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for SecurityService.
 * Focus: Role-based and ownership-based access control.
 *
 * These tests are critical for demonstrating the assignment's
 * requirement for "programmatic authorization" evaluation.
 */

@MockitoSettings(strictness = Strictness.LENIENT)
@ExtendWith(MockitoExtension.class)
@DisplayName("SecurityService Unit Tests")
class SecurityServiceTest {

    @Mock
    private SessionContext mockSessionContext;

    @Mock
    private Principal mockPrincipal;

    @InjectMocks
    private SecurityService securityService;

    private Shipment testShipment;
    private Vendor   testVendor;

    @BeforeEach
    void setUp() {
        testVendor = new Vendor("VND-001", "AsiaTech", "China");
        testVendor.setContactEmail("manager@asiatech.com");

        testShipment = new Shipment(
                "GT-SEC-001", "China", "Germany", "DHL"
        );
        testShipment.setVendor(testVendor);

        // Default: caller is "testuser"
        when(mockSessionContext.getCallerPrincipal())
                .thenReturn(mockPrincipal);
        when(mockPrincipal.getName())
                .thenReturn("testuser@globaltrade.com");
    }

    // ================================================================
    // TEST 1: Admin Has Full Access
    // ================================================================

    @Test
    @DisplayName("ADMIN role should have access to all shipments")
    void testAdminCanViewAnyShipment() {

        when(mockSessionContext.isCallerInRole("ADMIN"))
                .thenReturn(true);

        assertTrue(securityService.canViewShipment(testShipment),
                "Admin should be able to view any shipment");

        System.out.println("✓ ADMIN has unrestricted shipment access");
    }

    // ================================================================
    // TEST 2: Vendor Rep — Can Only View Own Shipments
    // ================================================================

    @Test
    @DisplayName("Vendor rep should only view their own vendor's shipments")
    void testVendorRep_CanViewOwnShipment() {

        // Caller IS the vendor contact
        when(mockPrincipal.getName())
                .thenReturn("manager@asiatech.com"); // matches vendor email
        when(mockSessionContext.isCallerInRole("ADMIN"))
                .thenReturn(false);
        when(mockSessionContext.isCallerInRole("LOGISTICS_COORDINATOR"))
                .thenReturn(false);
        when(mockSessionContext.isCallerInRole("WAREHOUSE_MANAGER"))
                .thenReturn(false);
        when(mockSessionContext.isCallerInRole("CUSTOMS_AGENT"))
                .thenReturn(false);
        when(mockSessionContext.isCallerInRole("VENDOR_REPRESENTATIVE"))
                .thenReturn(true);

        assertTrue(securityService.canViewShipment(testShipment),
                "Vendor rep should see their OWN vendor's shipments");

        System.out.println("✓ Vendor rep CAN view own shipment");
    }

    @Test
    @DisplayName("Vendor rep should NOT view other vendor's shipments")
    void testVendorRep_CannotViewOtherShipment() {

        // Caller is a DIFFERENT vendor's rep
        when(mockPrincipal.getName())
                .thenReturn("other@differentvendor.com"); // does NOT match
        when(mockSessionContext.isCallerInRole("ADMIN"))
                .thenReturn(false);
        when(mockSessionContext.isCallerInRole("LOGISTICS_COORDINATOR"))
                .thenReturn(false);
        when(mockSessionContext.isCallerInRole("WAREHOUSE_MANAGER"))
                .thenReturn(false);
        when(mockSessionContext.isCallerInRole("CUSTOMS_AGENT"))
                .thenReturn(false);
        when(mockSessionContext.isCallerInRole("VENDOR_REPRESENTATIVE"))
                .thenReturn(true);

        assertFalse(securityService.canViewShipment(testShipment),
                "Vendor rep should NOT see another vendor's shipments");

        System.out.println("✓ Vendor rep DENIED access to other vendor's shipment");
    }

    // ================================================================
    // TEST 3: Role Detection
    // ================================================================

    @Test
    @DisplayName("isAdmin() should correctly identify admin users")
    void testIsAdmin() {

        when(mockSessionContext.isCallerInRole("ADMIN"))
                .thenReturn(true);
        assertTrue(securityService.isAdmin(),
                "Should identify ADMIN role correctly");

        when(mockSessionContext.isCallerInRole("ADMIN"))
                .thenReturn(false);
        assertFalse(securityService.isAdmin(),
                "Non-admin should return false");
    }

    // ================================================================
    // TEST 4: Warehouse Manager — Can Modify Delivery Status
    // ================================================================

    @Test
    @DisplayName("Warehouse manager should mark shipments as DELIVERED")
    void testWarehouseManager_CanMarkDelivered() {

        when(mockSessionContext.isCallerInRole("ADMIN"))
                .thenReturn(false);
        when(mockSessionContext.isCallerInRole("LOGISTICS_COORDINATOR"))
                .thenReturn(false);
        when(mockSessionContext.isCallerInRole("WAREHOUSE_MANAGER"))
                .thenReturn(true);

        assertTrue(
                securityService.canModifyShipment(
                        testShipment, ShipmentStatus.DELIVERED),
                "Warehouse manager should be able to mark as DELIVERED"
        );

        assertFalse(
                securityService.canModifyShipment(
                        testShipment, ShipmentStatus.CANCELLED),
                "Warehouse manager should NOT be able to cancel shipments"
        );

        System.out.println("✓ Warehouse manager role permissions verified");
    }
}