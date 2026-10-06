package service;

import exception.WarehouseOperationException;
import model.Batch;
import model.Category;
import model.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class WarehouseTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 10);

    private Warehouse warehouse;
    private Product milk;
    private Product cheese;

    @BeforeEach
    void setUp() {
        warehouse = new Warehouse();
        milk = new Product("SKU-MILK", "Whole Milk", Category.REFRIGERATED);
        cheese = new Product("SKU-CHEESE", "Cheddar", Category.REFRIGERATED);
    }

    // ---------- Приём партий и остатки ----------

    @Test
    @DisplayName("Accepted batch appears in stock and in the batch list")
    void acceptBatchAddsStock() {
        Batch batch = new Batch("B1", TODAY, TODAY.plusDays(10), 50);

        warehouse.acceptBatch(milk, batch);

        assertEquals(50, warehouse.getAvailableStock(milk, TODAY));
        assertEquals(List.of(batch), warehouse.getBatchesForProduct(milk));
    }

    @Test
    @DisplayName("Available stock sums all non-expired batches")
    void availableStockSumsValidBatches() {
        warehouse.acceptBatch(milk, new Batch("B1", TODAY, TODAY.plusDays(5), 20));
        warehouse.acceptBatch(milk, new Batch("B2", TODAY, TODAY.plusDays(10), 30));

        assertEquals(50, warehouse.getAvailableStock(milk, TODAY));
    }

    @Test
    @DisplayName("Available stock excludes expired batches")
    void availableStockExcludesExpired() {
        warehouse.acceptBatch(milk, new Batch("B-OLD", TODAY.minusDays(15), TODAY.minusDays(1), 40));
        warehouse.acceptBatch(milk, new Batch("B-NEW", TODAY.minusDays(2), TODAY.plusDays(5), 60));

        assertEquals(60, warehouse.getAvailableStock(milk, TODAY));
    }

    @Test
    @DisplayName("Unknown product has zero stock and empty batch lists")
    void unknownProductIsEmpty() {
        assertEquals(0, warehouse.getAvailableStock(cheese, TODAY));
        assertTrue(warehouse.getExpiredBatches(cheese, TODAY).isEmpty());
        assertTrue(warehouse.getBatchesForProduct(cheese).isEmpty());
    }

    @Test
    @DisplayName("Expired batches are identified correctly")
    void expiredBatchesIdentified() {
        Batch expired = new Batch("B-EXP", TODAY.minusDays(10), TODAY.minusDays(1), 15);
        Batch valid = new Batch("B-OK", TODAY.minusDays(2), TODAY.plusDays(4), 25);
        warehouse.acceptBatch(milk, expired);
        warehouse.acceptBatch(milk, valid);

        List<Batch> expiredBatches = warehouse.getExpiredBatches(milk, TODAY);

        assertEquals(List.of(expired), expiredBatches);
    }

    @Test
    @DisplayName("Batch is still valid on its expiration date")
    void batchValidOnExpirationDate() {
        Batch batch = new Batch("B-BOUND", TODAY.minusDays(5), TODAY, 10);
        warehouse.acceptBatch(milk, batch);

        assertEquals(10, warehouse.getAvailableStock(milk, TODAY));
        assertTrue(warehouse.getExpiredBatches(milk, TODAY).isEmpty());
    }

    @Test
    @DisplayName("Batch is expired one day after its expiration date")
    void batchExpiredOneDayAfter() {
        Batch batch = new Batch("B-BOUND-EXP", TODAY.minusDays(10), TODAY.minusDays(1), 10);
        warehouse.acceptBatch(milk, batch);

        assertEquals(0, warehouse.getAvailableStock(milk, TODAY));
        assertEquals(List.of(batch), warehouse.getExpiredBatches(milk, TODAY));
    }

    @Test
    @DisplayName("Stock of one product does not affect another product")
    void productsAreIsolated() throws WarehouseOperationException {
        warehouse.acceptBatch(milk, new Batch("M1", TODAY, TODAY.plusDays(5), 10));
        warehouse.acceptBatch(cheese, new Batch("C1", TODAY, TODAY.plusDays(5), 7));

        warehouse.shipProduct(milk, 10, TODAY);

        assertEquals(0, warehouse.getAvailableStock(milk, TODAY));
        assertEquals(7, warehouse.getAvailableStock(cheese, TODAY));
    }

    // ---------- Отгрузка (FEFO) ----------

    @Test
    @DisplayName("FEFO: batch with the earliest expiration date ships first")
    void fefoEarliestShippedFirst() throws WarehouseOperationException {
        Batch later = new Batch("B-LATER", TODAY, TODAY.plusDays(20), 100);
        Batch sooner = new Batch("B-SOONER", TODAY, TODAY.plusDays(5), 50);
        // Добавляем в обратном порядке, чтобы проверить сортировку
        warehouse.acceptBatch(milk, later);
        warehouse.acceptBatch(milk, sooner);

        warehouse.shipProduct(milk, 50, TODAY);

        assertEquals(List.of(later), warehouse.getBatchesForProduct(milk));
    }

    @Test
    @DisplayName("FEFO never ships an expired batch, even if it expires earliest")
    void fefoSkipsExpiredBatch() throws WarehouseOperationException {
        Batch expired = new Batch("B-EXP", TODAY.minusDays(10), TODAY.minusDays(1), 50);
        Batch valid = new Batch("B-OK", TODAY.minusDays(2), TODAY.plusDays(5), 20);
        warehouse.acceptBatch(milk, expired);
        warehouse.acceptBatch(milk, valid);

        warehouse.shipProduct(milk, 10, TODAY);

        assertEquals(List.of(expired), warehouse.getExpiredBatches(milk, TODAY));
        assertEquals(10, warehouse.getAvailableStock(milk, TODAY));
    }

    @Test
    @DisplayName("Shipping exactly the batch quantity removes the batch")
    void exactShipmentRemovesBatch() throws WarehouseOperationException {
        warehouse.acceptBatch(milk, new Batch("B1", TODAY, TODAY.plusDays(5), 10));

        warehouse.shipProduct(milk, 10, TODAY);

        assertTrue(warehouse.getBatchesForProduct(milk).isEmpty());
        assertEquals(0, warehouse.getAvailableStock(milk, TODAY));
    }

    @Test
    @DisplayName("Partial shipment reduces the batch quantity")
    void partialShipmentSplitsBatch() throws WarehouseOperationException {
        warehouse.acceptBatch(milk, new Batch("B1", TODAY, TODAY.plusDays(10), 100));

        warehouse.shipProduct(milk, 30, TODAY);

        List<Batch> remaining = warehouse.getBatchesForProduct(milk);
        assertEquals(1, remaining.size());
        assertEquals("B1", remaining.getFirst().number());
        assertEquals(70, remaining.getFirst().quantity());
    }

    @Test
    @DisplayName("Shipment spanning several batches takes them in FEFO order")
    void shipmentSpanningMultipleBatches() throws WarehouseOperationException {
        Batch b1 = new Batch("B1", TODAY, TODAY.plusDays(3), 10);
        Batch b2 = new Batch("B2", TODAY, TODAY.plusDays(7), 20);
        Batch b3 = new Batch("B3", TODAY, TODAY.plusDays(14), 30);
        warehouse.acceptBatch(milk, b1);
        warehouse.acceptBatch(milk, b2);
        warehouse.acceptBatch(milk, b3);

        // 25 шт: 10 из B1 (уходит целиком), 15 из B2 (остаётся 5), B3 не трогаем
        warehouse.shipProduct(milk, 25, TODAY);

        List<Batch> remaining = warehouse.getBatchesForProduct(milk);
        assertEquals(35, warehouse.getAvailableStock(milk, TODAY));
        assertEquals(2, remaining.size());
        assertTrue(remaining.contains(new Batch("B2", TODAY, TODAY.plusDays(7), 5)));
        assertTrue(remaining.contains(b3));
    }

    // ---------- Ошибки отгрузки ----------

    @Test
    @DisplayName("Shipment exceeding available stock throws WarehouseOperationException")
    void shipmentExceedingStockThrows() {
        warehouse.acceptBatch(milk, new Batch("B1", TODAY, TODAY.plusDays(5), 10));

        WarehouseOperationException ex = assertThrows(
                WarehouseOperationException.class,
                () -> warehouse.shipProduct(milk, 11, TODAY)
        );

        assertTrue(ex.getMessage().contains("Cannot ship 11 items"));
    }

    @Test
    @DisplayName("Shipment fails when all stock is expired")
    void shipmentWithOnlyExpiredStockThrows() {
        warehouse.acceptBatch(milk, new Batch("B-OLD", TODAY.minusDays(10), TODAY.minusDays(1), 50));

        assertThrows(WarehouseOperationException.class,
                () -> warehouse.shipProduct(milk, 10, TODAY));
    }

    @Test
    @DisplayName("Shipment fails when valid stock is insufficient, ignoring expired stock")
    void shipmentIgnoresExpiredStockWhenCounting() {
        warehouse.acceptBatch(milk, new Batch("B-EXP", TODAY.minusDays(10), TODAY.minusDays(1), 50));
        warehouse.acceptBatch(milk, new Batch("B-OK", TODAY.minusDays(2), TODAY.plusDays(5), 20));

        assertThrows(WarehouseOperationException.class,
                () -> warehouse.shipProduct(milk, 25, TODAY));
    }

    @Test
    @DisplayName("Failed shipment leaves stock unchanged")
    void failedShipmentDoesNotChangeStock() {
        warehouse.acceptBatch(milk, new Batch("B1", TODAY, TODAY.plusDays(5), 10));
        List<Batch> before = warehouse.getBatchesForProduct(milk);

        assertThrows(WarehouseOperationException.class,
                () -> warehouse.shipProduct(milk, 11, TODAY));

        assertEquals(before, warehouse.getBatchesForProduct(milk));
    }

    @Test
    @DisplayName("Shipping an unknown product throws WarehouseOperationException")
    void shipUnknownProductThrows() {
        assertThrows(WarehouseOperationException.class,
                () -> warehouse.shipProduct(cheese, 1, TODAY));
    }

    @Test
    @DisplayName("Non-positive quantity throws IllegalArgumentException")
    void nonPositiveQuantityThrows() {
        warehouse.acceptBatch(milk, new Batch("B1", TODAY, TODAY.plusDays(5), 10));

        assertThrows(IllegalArgumentException.class,
                () -> warehouse.shipProduct(milk, 0, TODAY));
        assertThrows(IllegalArgumentException.class,
                () -> warehouse.shipProduct(milk, -5, TODAY));
    }

    @Test
    @DisplayName("Null arguments throw NullPointerException")
    void nullArgumentsThrowNpe() {
        Batch batch = new Batch("B1", TODAY, TODAY.plusDays(5), 10);

        assertThrows(NullPointerException.class, () -> warehouse.acceptBatch(null, batch));
        assertThrows(NullPointerException.class, () -> warehouse.acceptBatch(milk, null));
        assertThrows(NullPointerException.class, () -> warehouse.getAvailableStock(null, TODAY));
        assertThrows(NullPointerException.class, () -> warehouse.getAvailableStock(milk, null));
        assertThrows(NullPointerException.class, () -> warehouse.getExpiredBatches(null, TODAY));
        assertThrows(NullPointerException.class,
                () -> warehouse.shipProduct(milk, 1, null));
    }

    // ---------- Инкапсуляция ----------

    @Test
    @DisplayName("Returned batch list cannot be modified from outside")
    void batchListIsUnmodifiable() {
        warehouse.acceptBatch(milk, new Batch("B1", TODAY, TODAY.plusDays(5), 10));
        List<Batch> batches = warehouse.getBatchesForProduct(milk);

        assertThrows(UnsupportedOperationException.class,
                () -> batches.add(new Batch("HACK", TODAY, TODAY.plusDays(1), 999)));
    }

    @Test
    @DisplayName("Expired batch list cannot be modified from outside")
    void expiredListIsUnmodifiable() {
        warehouse.acceptBatch(milk, new Batch("B-EXP", TODAY.minusDays(10), TODAY.minusDays(1), 10));
        List<Batch> expired = warehouse.getExpiredBatches(milk, TODAY);

        assertThrows(UnsupportedOperationException.class, expired::clear);
    }

    @Test
    @DisplayName("Modifying the returned copy does not change the warehouse state")
    void returnedCopyDoesNotAffectStock() {
        warehouse.acceptBatch(milk, new Batch("B1", TODAY, TODAY.plusDays(5), 10));

        assertThrows(UnsupportedOperationException.class,
                () -> warehouse.getBatchesForProduct(milk).clear());

        assertEquals(10, warehouse.getAvailableStock(milk, TODAY));
    }
}