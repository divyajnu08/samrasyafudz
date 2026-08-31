package in.samrasyafudz.paymentservice.exception;

public class InvalidPaymentSignatureException extends RuntimeException {
    public InvalidPaymentSignatureException() {
        super("Payment verification failed — signature mismatch.");
    }
}