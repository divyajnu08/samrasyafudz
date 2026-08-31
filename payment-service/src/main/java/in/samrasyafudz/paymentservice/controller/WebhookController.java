package in.samrasyafudz.paymentservice.controller;

import in.samrasyafudz.paymentservice.service.PaymentService;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class WebhookController {

    private final PaymentService paymentService;

    @Value("${razorpay.webhook-secret}")
    private String webhookSecret;

    public WebhookController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/api/payments/webhook")
    public ResponseEntity<Void> handleWebhook(@RequestBody String payload,
                                              @RequestHeader("X-Razorpay-Signature") String signature) {
        if (!paymentService.verifyWebhookSignature(payload, signature, webhookSecret)) {
            return ResponseEntity.status(401).build();
        }

        JSONObject event = new JSONObject(payload);
        String eventType = event.getString("event");

        if ("payment.captured".equals(eventType)) {
            JSONObject paymentEntity = event.getJSONObject("payload")
                    .getJSONObject("payment").getJSONObject("entity");
            String razorpayOrderId = paymentEntity.getString("order_id");
            String razorpayPaymentId = paymentEntity.getString("id");

            paymentService.completeFromWebhook(razorpayOrderId, razorpayPaymentId);
        }

        return ResponseEntity.ok().build();
    }
}