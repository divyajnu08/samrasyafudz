package in.samrasyafudz.orderservice.controller;

import in.samrasyafudz.orderservice.dto.AddToCartRequest;
import in.samrasyafudz.orderservice.dto.CartResponse;
import in.samrasyafudz.orderservice.dto.UpdateCartItemRequest;
import in.samrasyafudz.commonsecurity.AuthenticatedUser;
import in.samrasyafudz.orderservice.service.CartService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cart")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    public CartResponse getCart(@AuthenticationPrincipal AuthenticatedUser user) {
        return cartService.getCart(user.userId());
    }

    @PostMapping
    public CartResponse addToCart(@AuthenticationPrincipal AuthenticatedUser user,
                                  @Valid @RequestBody AddToCartRequest request) {
        return cartService.addToCart(user.userId(), request);
    }

    @PutMapping("/product/{productId}/variant/{variantId}")
    public CartResponse updateQuantity(@AuthenticationPrincipal AuthenticatedUser user,
                                       @PathVariable Long productId, @PathVariable Long variantId,
                                       @Valid @RequestBody UpdateCartItemRequest request) {
        return cartService.updateQuantity(user.userId(), productId, variantId, request.getQuantity());
    }

    @DeleteMapping("/product/{productId}/variant/{variantId}")
    public CartResponse removeItem(@AuthenticationPrincipal AuthenticatedUser user,
                                   @PathVariable Long productId, @PathVariable Long variantId) {
        return cartService.removeItem(user.userId(), productId, variantId);
    }
}
