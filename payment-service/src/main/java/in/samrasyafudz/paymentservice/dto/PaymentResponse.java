package in.samrasyafudz.paymentservice.dto;

import java.math.BigDecimal;

public class PaymentResponse {
    private Long id;
    private Long orderId;
    private String status;
    private BigDecimal amount;
    private String method;

    public PaymentResponse(Long id, Long orderId, String status, BigDecimal amount, String method) {
        this.id = id;
        this.orderId = orderId;
        this.status = status;
        this.amount = amount;
        this.method = method;
    }

    public Long getId() {
        return id;
    }

    public Long getOrderId() {
        return orderId;
    }

    public String getStatus() {
        return status;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getMethod() {
        return method;
    }
}