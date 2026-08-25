package com.globaltrade;

import com.globaltrade.entity.Shipment;
import com.globaltrade.entity.ShipmentStatus;
import com.globaltrade.exception.ShipmentNotFoundException;
import com.globaltrade.exception.InventoryShortageException;
import com.globaltrade.service.ShipmentService;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.ejb.EJBAccessException;
import javax.ejb.SessionContext;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import java.security.Principal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ShipmentService.
 *
 * @ExtendWith(MockitoExtension.class) — tells JUnit 5 to use Mockito.
 * Mockito will automatically:
 * - Create mock objects for @Mock fields
 * - Inject them into @InjectMocks fields
 *
 * We test:
 * 1. Successful shipment creation
 * 2. ShipmentNotFoundException (Application Exception, rollback=false)
 * 3. Status update with role check
 * 4. Overdue shipment detection (timer service support)
 * 5. Customs clearance validation
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ShipmentService Unit Tests")
class ShipmentServiceTest {

    /**
     * @Mock creates a FAKE EntityManager.
     * We control exactly what it returns — no real DB needed.
     */
    @Mock
    private EntityManager mockEntityManager;

    @Mock
    private SessionContext mockSessionContext;

    @Mock
    private TypedQuery<Shipment> mockQuery;

    /**
     * @InjectMocks creates a REAL ShipmentService instance
     * and injects the @Mock objects into its @PersistenceContext
     * and @Resource fields automatically.
     */
    @InjectMocks
    private ShipmentService shipmentService;

    // ---- Test Data Setup ----

    private Shipment testShipment;

    @BeforeEach
    void setUp() {
        testShipment = new Shipment(
                "GT-TEST-001", "China", "Germany", "DHL"
        );
        testShipment.setEstimatedDelivery(LocalDate.now().plusDays(7));
    }

    // ================================================================
    // TEST 1: Successful Shipment Creation
    // ================================================================

    @Test
    @DisplayName("Should create shipment successfully")
    void testCreateShipment_Success() {

        // ARRANGE — set up mock behaviour
        // When code calls em.createQuery(...), return our mock query
        when(mockEntityManager.createQuery(anyString(), eq(Shipment.class)))
                .thenReturn(mockQuery);
        // When code calls query.setParameter(...), return the same query
        when(mockQuery.setParameter(anyString(), any()))
                .thenReturn(mockQuery);
        // When code calls query.getResultList(), return empty list
        // (no existing shipment with this tracking number)
        when(mockQuery.getResultList())
                .thenReturn(Collections.emptyList());

        // When code calls em.persist(shipment), do nothing (void method)
        doNothing().when(mockEntityManager).persist(any(Shipment.class));

        // ACT — call the real method
        Shipment result = shipmentService.createShipment(testShipment);

        // ASSERT — verify the result
        assertNotNull(result,
                "Created shipment should not be null");
        assertEquals("GT-TEST-001", result.getTrackingNumber(),
                "Tracking number should be preserved");
        assertEquals(ShipmentStatus.PENDING, result.getStatus(),
                "New shipment should be PENDING");

        // Verify em.persist() was actually called once
        verify(mockEntityManager, times(1)).persist(testShipment);
    }

    // ================================================================
    // TEST 2: Shipment Not Found — Application Exception
    // ================================================================

    @Test
    @DisplayName("Should throw ShipmentNotFoundException for unknown tracking number")
    void testFindByTrackingNumber_NotFound() {

        // ARRANGE — query returns empty list (shipment doesn't exist)
        when(mockEntityManager.createQuery(anyString(), eq(Shipment.class)))
                .thenReturn(mockQuery);
        when(mockQuery.setParameter(anyString(), any()))
                .thenReturn(mockQuery);
        when(mockQuery.getResultList())
                .thenReturn(Collections.emptyList());

        // ACT & ASSERT — expect ShipmentNotFoundException to be thrown
        ShipmentNotFoundException exception = assertThrows(
                ShipmentNotFoundException.class,
                () -> shipmentService.findByTrackingNumber("NONEXISTENT-999"),
                "Should throw ShipmentNotFoundException for unknown tracking number"
        );

        // Verify the exception message is meaningful
        assertTrue(exception.getMessage().contains("NONEXISTENT-999"),
                "Exception message should contain the missing tracking number");

        // KEY POINT: This is an Application Exception (rollback=false)
        // The transaction is NOT rolled back — client handles gracefully
        System.out.println("✓ Application Exception correctly thrown: "
                + exception.getMessage());
    }

    // ================================================================
    // TEST 3: Find Shipment by ID Successfully
    // ================================================================

    @Test
    @DisplayName("Should find shipment by ID")
    void testFindById_Success() {

        // ARRANGE — em.find() returns our test shipment
        when(mockEntityManager.find(Shipment.class, 1L))
                .thenReturn(testShipment);

        // ACT
        Shipment result = shipmentService.findById(1L);

        // ASSERT
        assertNotNull(result);
        assertEquals("GT-TEST-001", result.getTrackingNumber());
        verify(mockEntityManager, times(1)).find(Shipment.class, 1L);
    }

    @Test
    @DisplayName("Should throw ShipmentNotFoundException for unknown ID")
    void testFindById_NotFound() {

        // ARRANGE — em.find() returns null (not in DB)
        when(mockEntityManager.find(Shipment.class, 999L))
                .thenReturn(null);

        // ACT & ASSERT
        assertThrows(
                ShipmentNotFoundException.class,
                () -> shipmentService.findById(999L),
                "Should throw ShipmentNotFoundException for ID 999"
        );
    }

    // ================================================================
    // TEST 4: Vendor Representative Role Restriction
    // ================================================================

    @Test
    @DisplayName("Vendor rep should only be able to cancel shipments")
    void testUpdateStatus_VendorRepCanOnlyCancel() {

        // ARRANGE
        when(mockEntityManager.find(Shipment.class, 1L))
                .thenReturn(testShipment);

        // Mock the SessionContext to say caller is a VENDOR_REPRESENTATIVE
        when(mockSessionContext.isCallerInRole("VENDOR_REPRESENTATIVE"))
                .thenReturn(true);

        // ACT & ASSERT — vendor trying to mark as DELIVERED should fail
        assertThrows(
                EJBAccessException.class,
                () -> shipmentService.updateShipmentStatus(
                        1L, ShipmentStatus.DELIVERED),
                "Vendor rep should NOT be able to mark shipment as DELIVERED"
        );

        System.out.println("✓ Programmatic security correctly blocked " +
                "vendor rep from marking shipment DELIVERED");
    }

    // ================================================================
    // TEST 5: Customs Clearance — Missing Vendor
    // ================================================================

    @Test
    @DisplayName("Customs clearance should fail if no vendor is associated")
    void testCustomsClearance_NoVendor() {

        // ARRANGE — shipment with no vendor
        Shipment shipmentNoVendor = new Shipment(
                "GT-TEST-002", "USA", "UK", "FedEx"
        );
        // vendor is null by default

        when(mockEntityManager.find(Shipment.class, 2L))
                .thenReturn(shipmentNoVendor);

        // ACT & ASSERT
        assertThrows(
                com.globaltrade.exception.CustomsException.class,
                () -> shipmentService.processCustomsClearance(2L),
                "Should throw CustomsException when vendor is missing"
        );
    }

    // ================================================================
    // TEST 6: Find Multiple Shipments by Status
    // ================================================================

    @Test
    @DisplayName("Should return list of delayed shipments")
    void testFindByStatus_Delayed() {

        // ARRANGE — prepare two delayed shipments
        Shipment s1 = new Shipment("GT-D-001", "China", "USA", "DHL");
        s1.setStatus(ShipmentStatus.DELAYED);

        Shipment s2 = new Shipment("GT-D-002", "India", "UK", "FedEx");
        s2.setStatus(ShipmentStatus.DELAYED);

        when(mockEntityManager.createQuery(anyString(), eq(Shipment.class)))
                .thenReturn(mockQuery);
        when(mockQuery.setParameter(anyString(), any()))
                .thenReturn(mockQuery);
        when(mockQuery.getResultList())
                .thenReturn(Arrays.asList(s1, s2));

        // ACT
        List<Shipment> delayed =
                shipmentService.findByStatus(ShipmentStatus.DELAYED);

        // ASSERT
        assertEquals(2, delayed.size(),
                "Should find exactly 2 delayed shipments");
        assertTrue(delayed.stream().allMatch(
                        s -> s.getStatus() == ShipmentStatus.DELAYED),
                "All returned shipments should be DELAYED"
        );
    }
}