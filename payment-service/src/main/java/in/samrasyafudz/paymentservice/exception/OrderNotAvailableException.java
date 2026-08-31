package in.samrasyafudz.paymentservice.exception;

public class OrderNotAvailableException extends RuntimeException {
    public OrderNotAvailableException(Long orderId) {
        super("Order not available: " + orderId);
    }
}