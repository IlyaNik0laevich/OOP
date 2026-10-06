package service;

import model.Category;
import model.Product;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ProductTest {

    @Test
    @DisplayName("Products with the same SKU are equal regardless of other fields")
    void sameSkuMeansEqual() {
        Product a = new Product("SKU-MILK", "Whole Milk", Category.REFRIGERATED);
        Product b = new Product("SKU-MILK", "Different Name", Category.FROZEN);

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    @DisplayName("Products with different SKUs are not equal")
    void differentSkuMeansNotEqual() {
        Product a = new Product("SKU-MILK", "Whole Milk", Category.REFRIGERATED);
        Product b = new Product("SKU-BREAD", "Bread", Category.NORMAL);

        assertNotEquals(a, b);
    }

    @Test
    @DisplayName("SKU comparison is case-sensitive (documents current behavior)")
    void skuIsCaseSensitive() {
        Product upper = new Product("SKU-MILK", "Milk", Category.NORMAL);
        Product lower = new Product("sku-milk", "Milk", Category.NORMAL);

        assertNotEquals(upper, lower);
    }

    @Test
    @DisplayName("Product is usable as a HashMap key")
    void worksAsHashMapKey() {
        var map = new java.util.HashMap<Product, String>();
        map.put(new Product("SKU-1", "A", Category.NORMAL), "value");

        assertEquals("value", map.get(new Product("SKU-1", "B", Category.FROZEN)));
    }

    @Test
    @DisplayName("Null fields throw NullPointerException")
    void nullFieldsThrow() {
        assertThrows(NullPointerException.class,
                () -> new Product(null, "Name", Category.NORMAL));
        assertThrows(NullPointerException.class,
                () -> new Product("SKU", null, Category.NORMAL));
        assertThrows(NullPointerException.class,
                () -> new Product("SKU", "Name", null));
    }

    @Test
    @DisplayName("toString contains SKU and name")
    void toStringContainsSkuAndName() {
        Product p = new Product("SKU-MILK", "Whole Milk", Category.REFRIGERATED);

        assertTrue(p.toString().contains("SKU-MILK"));
        assertTrue(p.toString().contains("Whole Milk"));
    }
}