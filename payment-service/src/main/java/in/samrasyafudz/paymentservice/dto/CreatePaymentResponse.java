package in.samrasyafudz.paymentservice.dto;

import java.math.BigDecimal;

public class CreatePaymentResponse {
    private Long paymentId;
    private String razorpayOrderId;
    private String razorpayKeyId;
    private BigDecimal amount;
    private String currency;

    public CreatePaymentResponse(Long paymentId, String razorpayOrderId, String razorpayKeyId,
                                 BigDecimal amount, String currency) {
        this.paymentId = paymentId;
        this.razorpayOrderId = razorpayOrderId;
        this.razorpayKeyId = razorpayKeyId;
        this.amount = amount;
        this.currency = currency;
    }

    public Long getPaymentId() {
        return paymentId;
    }

    public String getRazorpayOrderId() {
        return razorpayOrderId;
    }

    public String getRazorpayKeyId() {
        return razorpayKeyId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }
}