package com.globaltrade;

import com.globaltrade.entity.Inventory;
import com.globaltrade.exception.InventoryShortageException;
import com.globaltrade.service.InventoryService;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.ejb.SessionContext;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import java.math.BigDecimal;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for InventoryService.
 * Focus: InventoryShortageException (rollback=true) behaviour
 * and stock reservation logic.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("InventoryService Unit Tests")
class InventoryServiceTest {

    @Mock
    private EntityManager mockEntityManager;

    @Mock
    private SessionContext mockSessionContext;

    @Mock
    private TypedQuery<Inventory> mockQuery;

    @InjectMocks
    private InventoryService inventoryService;

    private Inventory testInventory;

    @BeforeEach
    void setUp() {
        testInventory = new Inventory(
                "SKU-ELEC-001", "Industrial Sensors", "Singapore-WH1", 100
        );
        testInventory.setReorderThreshold(20);
        testInventory.setUnitPrice(BigDecimal.valueOf(149.99));
    }

    // ================================================================
    // TEST 1: Successful Stock Reservation
    // ================================================================

    @Test
    @DisplayName("Should reserve stock when sufficient quantity available")
    void testReserveStock_Success() {

        // ARRANGE — findBySku will need the named query
        when(mockEntityManager.createNamedQuery("Inventory.findBySku",
                Inventory.class))
                .thenReturn(mockQuery);
        when(mockQuery.setParameter(anyString(), any()))
                .thenReturn(mockQuery);
        when(mockQuery.getResultList())
                .thenReturn(Collections.singletonList(testInventory));
        when(mockEntityManager.merge(any(Inventory.class)))
                .thenReturn(testInventory);

        // ACT — reserve 30 units (we have 100)
        assertDoesNotThrow(
                () -> inventoryService.reserveStock("SKU-ELEC-001", 30),
                "Should not throw when sufficient stock available"
        );

        // ASSERT — quantity should be reduced
        assertEquals(70, testInventory.getQuantity(),
                "Quantity should be 100 - 30 = 70 after reservation");

        System.out.println("✓ Stock reservation: 100 → 70 units");
    }

    // ================================================================
    // TEST 2: Inventory Shortage — Application Exception (rollback=true)
    // ================================================================

    @Test
    @DisplayName("Should throw InventoryShortageException when stock insufficient")
    void testReserveStock_Shortage() {

        // ARRANGE — only 5 units available
        testInventory.setQuantity(5);

        when(mockEntityManager.createNamedQuery("Inventory.findBySku",
                Inventory.class))
                .thenReturn(mockQuery);
        when(mockQuery.setParameter(anyString(), any()))
                .thenReturn(mockQuery);
        when(mockQuery.getResultList())
                .thenReturn(Collections.singletonList(testInventory));

        // ACT & ASSERT — try to reserve 50 units when only 5 available
        InventoryShortageException exception = assertThrows(
                InventoryShortageException.class,
                () -> inventoryService.reserveStock("SKU-ELEC-001", 50),
                "Should throw InventoryShortageException"
        );

        // Verify exception carries correct business data
        assertEquals("SKU-ELEC-001", exception.getProductSku());
        assertEquals(50, exception.getRequested());
        assertEquals(5,  exception.getAvailable());

        // Verify quantity was NOT changed (transaction would rollback)
        assertEquals(5, testInventory.getQuantity(),
                "Quantity should remain unchanged — rollback=true protects data");

        // Verify em.merge() was NEVER called (shortage detected before write)
        verify(mockEntityManager, never()).merge(any());

        System.out.println("✓ InventoryShortageException (rollback=true): "
                + exception.getMessage());
    }

    // ================================================================
    // TEST 3: Low Stock Detection
    // ================================================================

    @Test
    @DisplayName("isLowStock() should return true when quantity at threshold")
    void testLowStockDetection() {

        // Exactly AT the threshold
        testInventory.setQuantity(20);
        testInventory.setReorderThreshold(20);
        assertTrue(testInventory.isLowStock(),
                "Should be low stock when quantity equals threshold");

        // Below threshold
        testInventory.setQuantity(5);
        assertTrue(testInventory.isLowStock(),
                "Should be low stock when below threshold");

        // Above threshold
        testInventory.setQuantity(100);
        assertFalse(testInventory.isLowStock(),
                "Should NOT be low stock when above threshold");

        System.out.println("✓ Low stock detection logic verified");
    }

    // ================================================================
    // TEST 4: Add New Inventory
    // ================================================================

    @Test
    @DisplayName("Should add new inventory item successfully")
    void testAddInventory_Success() {

        doNothing().when(mockEntityManager).persist(any(Inventory.class));

        Inventory newItem = new Inventory(
                "SKU-NEW-001", "New Product", "Dubai-WH3", 500
        );

        Inventory result = inventoryService.addInventory(newItem);

        assertNotNull(result);
        assertEquals("SKU-NEW-001", result.getProductSku());
        verify(mockEntityManager, times(1)).persist(newItem);

        System.out.println("✓ Inventory item added: " + result.getProductSku());
    }
}