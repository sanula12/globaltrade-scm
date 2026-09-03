package com.globaltrade;

import com.globaltrade.exception.*;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for EJB Exception Handling framework.
 *
 * Verifies correct @ApplicationException behaviour:
 * - Exception hierarchy
 * - Business data carried by exceptions
 * - Application vs System exception classification
 *
 * These tests demonstrate understanding of the difference between
 * Application Exceptions and System Exceptions — a key assignment requirement.
 */
@DisplayName("Exception Handling Framework Tests")
class ExceptionHandlingTest {

    // ================================================================
    // TEST 1: Application Exception Classification
    // ================================================================

    @Test
    @DisplayName("ShipmentNotFoundException should be an Application Exception")
    void testShipmentNotFoundException_IsApplicationException() {

        // @ApplicationException is on the class — verify it's present
        jakarta.ejb.ApplicationException annotation =
                ShipmentNotFoundException.class
                        .getAnnotation(jakarta.ejb.ApplicationException.class);

        assertNotNull(annotation,
                "ShipmentNotFoundException must have @ApplicationException");
        assertFalse(annotation.rollback(),
                "ShipmentNotFoundException should NOT rollback (read-only operation)");

        System.out.println("✓ ShipmentNotFoundException: Application Exception, rollback=false");
    }

    @Test
    @DisplayName("InventoryShortageException should rollback transaction")
    void testInventoryShortageException_ShouldRollback() {

        jakarta.ejb.ApplicationException annotation =
                InventoryShortageException.class
                        .getAnnotation(jakarta.ejb.ApplicationException.class);

        assertNotNull(annotation,
                "InventoryShortageException must have @ApplicationException");
        assertTrue(annotation.rollback(),
                "InventoryShortageException MUST rollback (partial write risk)");

        System.out.println("✓ InventoryShortageException: Application Exception, rollback=true");
    }

    @Test
    @DisplayName("CarrierSystemException should be a System Exception (no annotation)")
    void testCarrierSystemException_IsSystemException() {

        jakarta.ejb.ApplicationException annotation =
                CarrierSystemException.class
                        .getAnnotation(jakarta.ejb.ApplicationException.class);

        assertNull(annotation,
                "CarrierSystemException should NOT have @ApplicationException" +
                        " — it is a System Exception that always rolls back");

        // It should be a RuntimeException (unchecked)
        assertTrue(
                RuntimeException.class.isAssignableFrom(CarrierSystemException.class),
                "CarrierSystemException must extend RuntimeException"
        );

        System.out.println("✓ CarrierSystemException: System Exception (no annotation, always rolls back)");
    }

    // ================================================================
    // TEST 2: Exception Business Data
    // ================================================================

    @Test
    @DisplayName("InventoryShortageException should carry business data")
    void testInventoryShortageException_CarriesData() {

        InventoryShortageException ex =
                new InventoryShortageException("SKU-TEST-001", 100, 30);

        assertEquals("SKU-TEST-001", ex.getProductSku());
        assertEquals(100, ex.getRequested());
        assertEquals(30,  ex.getAvailable());
        assertTrue(ex.getMessage().contains("SKU-TEST-001"),
                "Exception message should include SKU");
        assertTrue(ex.getMessage().contains("100"),
                "Exception message should include requested quantity");
        assertTrue(ex.getMessage().contains("30"),
                "Exception message should include available quantity");

        System.out.println("✓ Exception carries business data: " + ex.getMessage());
    }

    @Test
    @DisplayName("CustomsException should have rollback=true for data integrity")
    void testCustomsException_RollbackTrue() {

        jakarta.ejb.ApplicationException annotation =
                CustomsException.class
                        .getAnnotation(jakarta.ejb.ApplicationException.class);

        assertNotNull(annotation);
        assertTrue(annotation.rollback(),
                "Customs failures must rollback to prevent incorrect clearance records");

        // Verify exception data
        CustomsException ex = new CustomsException(
                "GT-2024-001", "Missing import documentation"
        );
        assertEquals("GT-2024-001", ex.getTrackingNumber());
        assertTrue(ex.getMessage().contains("Missing import documentation"));

        System.out.println("✓ CustomsException: rollback=true, data integrity protected");
    }

    // ================================================================
    // TEST 3: Exception Hierarchy
    // ================================================================

    @Test
    @DisplayName("All Application Exceptions should extend RuntimeException")
    void testAllApplicationExceptions_ExtendRuntimeException() {

        // Our app exceptions extend RuntimeException (unchecked)
        // but @ApplicationException overrides the system exception behaviour
        assertTrue(RuntimeException.class.isAssignableFrom(
                ShipmentNotFoundException.class));
        assertTrue(RuntimeException.class.isAssignableFrom(
                InventoryShortageException.class));
        assertTrue(RuntimeException.class.isAssignableFrom(
                VendorValidationException.class));
        assertTrue(RuntimeException.class.isAssignableFrom(
                CustomsException.class));
        assertTrue(RuntimeException.class.isAssignableFrom(
                CarrierSystemException.class));

        System.out.println("✓ All exceptions extend RuntimeException (unchecked)");
    }
}
