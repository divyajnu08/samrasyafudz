package in.samrasyafudz.paymentservice.exception;

public class OrderNotPayableException extends RuntimeException {
    public OrderNotPayableException(String status) {
        super("This order cannot be paid for — current status: " + status);
    }
}