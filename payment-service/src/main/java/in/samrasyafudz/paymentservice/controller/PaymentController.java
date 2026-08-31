package in.samrasyafudz.paymentservice.controller;

import in.samrasyafudz.paymentservice.dto.CreatePaymentRequest;
import in.samrasyafudz.paymentservice.dto.CreatePaymentResponse;
import in.samrasyafudz.paymentservice.dto.PaymentResponse;
import in.samrasyafudz.paymentservice.dto.VerifyPaymentRequest;
import in.samrasyafudz.paymentservice.security.AuthenticatedUser;
import in.samrasyafudz.paymentservice.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping
    public CreatePaymentResponse createPayment(@AuthenticationPrincipal AuthenticatedUser user,
                                               @Valid @RequestBody CreatePaymentRequest request) {
        return paymentService.createPayment(user.userId(), request);
    }

    @PostMapping("/verify")
    public PaymentResponse verifyPayment(@AuthenticationPrincipal AuthenticatedUser user,
                                         @Valid @RequestBody VerifyPaymentRequest request) {
        return paymentService.verifyPayment(user.userId(), request);
    }
}