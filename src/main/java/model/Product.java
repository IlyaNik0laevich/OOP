package model;

import java.util.Objects;

/**
 * Класс, представляющий товар.
 * Ключом равенства (identity) является уникальный артикул (SKU).
 */
public final class Product {
    private final String sku;
    private final String name;
    private final Category category;

    /**
     * Конструктор товара.
     *
     * @param sku      Артикул (уникальный идентификатор)
     * @param name     Наименование товара
     * @param category Категория (определяет условия хранения)
     */
    public Product(String sku, String name, Category category) {
        this.sku = Objects.requireNonNull(sku, "SKU не может быть null");
        this.name = Objects.requireNonNull(name, "Название не может быть null");
        this.category = Objects.requireNonNull(category, "Категория не может быть null");
    }

    public String getSku() { return sku; }
    public String getName() { return name; }
    public Category getCategory() { return category; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Product product = (Product) o;
        return Objects.equals(sku, product.sku);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sku);
    }

    @Override
    public String toString() {
        return String.format("Товар[%s, '%s', %s]", sku, name, category);
    }
}
