package in.samrasyafudz.paymentservice.exception;

public class OrderOwnershipException extends RuntimeException {
    public OrderOwnershipException() {
        super("This order does not belong to you.");
    }
}