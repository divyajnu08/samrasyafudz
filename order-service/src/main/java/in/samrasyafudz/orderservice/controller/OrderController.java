package in.samrasyafudz.orderservice.controller;

import in.samrasyafudz.orderservice.dto.CheckoutRequest;
import in.samrasyafudz.orderservice.dto.OrderResponse;
import in.samrasyafudz.commonsecurity.AuthenticatedUser;
import in.samrasyafudz.orderservice.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping("/checkout")
    public OrderResponse checkout(@AuthenticationPrincipal AuthenticatedUser user,
                                  @RequestHeader("Authorization") String authHeader,
                                  @Valid @RequestBody CheckoutRequest request) {
        return orderService.checkout(user.userId(), request, authHeader);
    }
    
    @GetMapping
    public List<OrderResponse> listOrders(@AuthenticationPrincipal AuthenticatedUser user) {
        return orderService.listOrders(user.userId());
    }

    @GetMapping("/{orderId}")
    public OrderResponse getOrder(@AuthenticationPrincipal AuthenticatedUser user,
                                  @PathVariable Long orderId) {
        return orderService.getOrder(user.userId(), orderId);
    }

    @PostMapping("/{orderId}/cancel")
    public OrderResponse cancelOrder(@AuthenticationPrincipal AuthenticatedUser user,
                                     @PathVariable Long orderId) {
        return orderService.cancelOrder(user.userId(), orderId);
    }
}