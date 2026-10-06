package service;

import exception.WarehouseOperationException;
import model.Batch;
import model.Product;

import java.time.LocalDate;
import java.util.*;

public class Warehouse {
    // Инкапсулированное состояние: доступ только через публичные методы
    private final Map<Product, List<Batch>> stock = new HashMap<>();

    /**
     * Прием новой партии товара на склад.
     */
    public void acceptBatch(Product product, Batch batch) {
        Objects.requireNonNull(product, "Product cannot be null");
        Objects.requireNonNull(batch, "Batch cannot be null");
        stock.computeIfAbsent(product, k -> new ArrayList<>()).add(batch);
    }

    /**
     * Получение остатка годного товара на заданную дату.
     */
    public int getAvailableStock(Product product, LocalDate date) {
        Objects.requireNonNull(product, "Product cannot be null");
        Objects.requireNonNull(date, "Date cannot be null");

        return stock.getOrDefault(product, List.of()).stream()
                .filter(batch -> isNotExpired(batch, date))
                .mapToInt(Batch::quantity)
                .sum();
    }

    /**
     * Список просроченных партий товара на заданную дату.
     */
    public List<Batch> getExpiredBatches(Product product, LocalDate date) {
        Objects.requireNonNull(product, "Product cannot be null");
        Objects.requireNonNull(date, "Date cannot be null");

        return stock.getOrDefault(product, List.of()).stream()
                .filter(batch -> !isNotExpired(batch, date))
                .toList(); // Возвращает неизменяемый список
    }

    /**
     * Возвращает копию списка всех партий товара (защита от утечки состояния).
     */
    public List<Batch> getBatchesForProduct(Product product) {
        return stock.containsKey(product) ? List.copyOf(stock.get(product)) : List.of();
    }

    /**
     * Отгрузка товара по правилу FEFO (First Expired, First Out).
     */
    public void shipProduct(Product product, int requestedQuantity, LocalDate currentDate)
            throws WarehouseOperationException {

        Objects.requireNonNull(product, "Product cannot be null");
        Objects.requireNonNull(currentDate, "Current date cannot be null");

        if (requestedQuantity <= 0) {
            throw new IllegalArgumentException("Requested quantity must be positive");
        }

        int available = getAvailableStock(product, currentDate);
        if (available < requestedQuantity) {
            throw new WarehouseOperationException(String.format(
                    "Cannot ship %d items of '%s'. Only %d non-expired items available on %s",
                    requestedQuantity, product.getName(), available, currentDate
            ));
        }

        List<Batch> batches = stock.get(product);

        // FEFO: сортируем годные партии по возрастанию expirationDate
        var validBatches = batches.stream()
                .filter(batch -> isNotExpired(batch, currentDate))
                .sorted(Comparator.comparing(Batch::expirationDate))
                .toList();

        int remainingToShip = requestedQuantity;

        for (Batch batch : validBatches) {
            if (remainingToShip == 0) {
                break;
            }

            batches.remove(batch);

            if (batch.quantity() <= remainingToShip) {
                // Партия уходит целиком
                remainingToShip -= batch.quantity();
            } else {
                // Забираем часть партии, остаток возвращаем в виде нового Batch
                Batch remainingBatch = batch.withQuantity(batch.quantity() - remainingToShip);
                batches.add(remainingBatch);
                remainingToShip = 0;
            }
        }
    }

    private boolean isNotExpired(Batch batch, LocalDate currentDate) {
        return !batch.expirationDate().isBefore(currentDate);
    }
}