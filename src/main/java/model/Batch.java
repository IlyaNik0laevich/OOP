package model;

import java.time.LocalDate;

/**
 * Партия товара.
 * @param number         Номер партии (уникальный идентификатор партии)
 * @param productionDate Дата производства
 * @param expirationDate Срок годности
 * @param quantity       Количество товара в партии
 */
public record Batch(
        String number,
        LocalDate productionDate,
        LocalDate expirationDate,
        int quantity
) {
    public Batch {
        if (quantity < 0) {
            throw new IllegalArgumentException("Количество товара не может быть отрицательным");
        }
        if (expirationDate.isBefore(productionDate)) {
            throw new IllegalArgumentException("Срок годности не может быть раньше даты производства");
        }
    }

    /**
     * Создает копию текущей партии с измененным количеством товара.
     * Необходим для реализации частичной отгрузки, так как record неизменяем.
     *
     * @param newQuantity новое количество товара
     * @return новый объект Batch
     */
    public Batch withQuantity(int newQuantity) {
        return new Batch(number, productionDate, expirationDate, newQuantity);
    }
}
