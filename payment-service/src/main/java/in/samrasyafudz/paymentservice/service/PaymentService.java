package in.samrasyafudz.paymentservice.service;

import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;
import in.samrasyafudz.paymentservice.client.OrderDto;
import in.samrasyafudz.paymentservice.client.OrderServiceClient;
import in.samrasyafudz.paymentservice.client.ProductServiceClient;
import in.samrasyafudz.paymentservice.dto.CreatePaymentRequest;
import in.samrasyafudz.paymentservice.dto.CreatePaymentResponse;
import in.samrasyafudz.paymentservice.dto.PaymentResponse;
import in.samrasyafudz.paymentservice.dto.VerifyPaymentRequest;
import in.samrasyafudz.paymentservice.entity.Payment;
import in.samrasyafudz.paymentservice.entity.PaymentStatus;
import in.samrasyafudz.paymentservice.exception.*;
import in.samrasyafudz.paymentservice.repository.PaymentRepository;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderServiceClient orderServiceClient;
    private final ProductServiceClient productServiceClient;

    @Value("${razorpay.key-id}")
    private String razorpayKeyId;

    @Value("${razorpay.key-secret}")
    private String razorpayKeySecret;

    public PaymentService(PaymentRepository paymentRepository, OrderServiceClient orderServiceClient,
                          ProductServiceClient productServiceClient) {
        this.paymentRepository = paymentRepository;
        this.orderServiceClient = orderServiceClient;
        this.productServiceClient = productServiceClient;
    }

    @Transactional
    public CreatePaymentResponse createPayment(Long userId, CreatePaymentRequest request) {
        OrderDto order = orderServiceClient.getOrder(request.getOrderId());

        if (!order.getUserId().equals(userId)) {
            throw new OrderOwnershipException();
        }
        if (!"PENDING".equals(order.getStatus())) {
            throw new OrderNotPayableException(order.getStatus());
        }

        try {
            RazorpayClient razorpay = new RazorpayClient(razorpayKeyId, razorpayKeySecret);

            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", order.getTotalAmount().multiply(BigDecimal.valueOf(100)).intValue());
            orderRequest.put("currency", "INR");
            orderRequest.put("receipt", "order_" + order.getId());

            JSONObject method = new JSONObject();
            method.put("upi", true);
            orderRequest.put("method", method);

            com.razorpay.Order razorpayOrder = razorpay.orders.create(orderRequest);

            Payment payment = new Payment();
            payment.setOrderId(order.getId());
            payment.setUserId(userId);
            payment.setRazorpayOrderId(razorpayOrder.get("id"));
            payment.setAmount(order.getTotalAmount());
            payment.setMethod("upi");
            payment.setStatus(PaymentStatus.CREATED);
            paymentRepository.save(payment);

            return new CreatePaymentResponse(
                    payment.getId(), payment.getRazorpayOrderId(), razorpayKeyId,
                    payment.getAmount(), payment.getCurrency()
            );
        } catch (RazorpayException e) {
            throw new PaymentGatewayException("Could not create Razorpay order", e);
        }
    }

    @Transactional
    public PaymentResponse verifyPayment(Long userId, VerifyPaymentRequest request) {
        Payment payment = paymentRepository.findByRazorpayOrderId(request.getRazorpayOrderId())
                .orElseThrow(PaymentNotFoundException::new);

        if (!payment.getUserId().equals(userId)) {
            throw new OrderOwnershipException();
        }

        boolean valid = verifySignature(
                request.getRazorpayOrderId(), request.getRazorpayPaymentId(), request.getRazorpaySignature()
        );

        if (!valid) {
            payment.setStatus(PaymentStatus.FAILED);
            paymentRepository.save(payment);
            throw new InvalidPaymentSignatureException();
        }

        completePayment(payment, request.getRazorpayPaymentId());
        return toResponse(payment);
    }

    @Transactional
    public void completeFromWebhook(String razorpayOrderId, String razorpayPaymentId) {
        Payment payment = paymentRepository.findByRazorpayOrderId(razorpayOrderId)
                .orElseThrow(PaymentNotFoundException::new);

        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            return;
        }

        completePayment(payment, razorpayPaymentId);
    }

    private void completePayment(Payment payment, String razorpayPaymentId) {
        payment.setRazorpayPaymentId(razorpayPaymentId);
        payment.setStatus(PaymentStatus.SUCCESS);
        paymentRepository.save(payment);

        OrderDto order = orderServiceClient.getOrder(payment.getOrderId());
        for (OrderDto.OrderItemDto item : order.getItems()) {
            productServiceClient.deductStock(item.getVariantId(), item.getQuantity());
        }

        orderServiceClient.confirmOrder(payment.getOrderId());
    }

    private boolean verifySignature(String razorpayOrderId, String razorpayPaymentId, String signature) {
        try {
            Map<String, String> attributes = new HashMap<>();
            attributes.put("razorpay_order_id", razorpayOrderId);
            attributes.put("razorpay_payment_id", razorpayPaymentId);
            attributes.put("razorpay_signature", signature);
            return Utils.verifyPaymentSignature((JSONObject) attributes, razorpayKeySecret);
        } catch (RazorpayException e) {
            return false;
        }
    }

    public boolean verifyWebhookSignature(String payload, String signature, String webhookSecret) {
        try {
            return Utils.verifyWebhookSignature(payload, signature, webhookSecret);
        } catch (RazorpayException e) {
            return false;
        }
    }

    private PaymentResponse toResponse(Payment payment) {
        return new PaymentResponse(
                payment.getId(), payment.getOrderId(), payment.getStatus().name(),
                payment.getAmount(), payment.getMethod()
        );
    }
}