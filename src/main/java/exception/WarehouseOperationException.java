package exception;

/**
 * Исключение, выбрасываемое при ошибках складских операций:
 * попытке отгрузить товар сверх остатка или при нехватке годного товара.
 */
public class WarehouseOperationException extends Exception {
    public WarehouseOperationException(String message) {
        super(message);
    }
}
