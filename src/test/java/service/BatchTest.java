package service;


import model.Batch;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class BatchTest {

    private static final LocalDate PROD = LocalDate.of(2026, 10, 1);

    @Test
    @DisplayName("Negative quantity is rejected")
    void negativeQuantityRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new Batch("B1", PROD, PROD.plusDays(5), -1));
    }

    @Test
    @DisplayName("Expiration date before production date is rejected")
    void expirationBeforeProductionRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new Batch("B1", PROD, PROD.minusDays(1), 10));
    }

    @Test
    @DisplayName("Expiration date equal to production date is accepted")
    void sameDayProductionAndExpirationAccepted() {
        assertDoesNotThrow(() -> new Batch("B1", PROD, PROD, 10));
    }

    @Test
    @DisplayName("Null dates throw NullPointerException")
    void nullDatesThrowNpe() {
        assertThrows(NullPointerException.class,
                () -> new Batch("B1", null, PROD, 10));
        assertThrows(NullPointerException.class,
                () -> new Batch("B1", PROD, null, 10));
    }

    @Test
    @DisplayName("Zero quantity is currently accepted (documents current behavior)")
    void zeroQuantityCurrentlyAccepted() {
        // Если решите запрещать нулевое количество, замените на assertThrows
        Batch empty = new Batch("B0", PROD, PROD.plusDays(5), 0);

        assertEquals(0, empty.quantity());
    }

    @Test
    @DisplayName("withQuantity returns a new instance and keeps the original unchanged")
    void withQuantityReturnsNewInstance() {
        Batch original = new Batch("B1", PROD, PROD.plusDays(5), 10);

        Batch copy = original.withQuantity(3);

        assertNotSame(original, copy);
        assertEquals(10, original.quantity());
        assertEquals(3, copy.quantity());
        assertEquals(original.number(), copy.number());
        assertEquals(original.expirationDate(), copy.expirationDate());
    }
}
