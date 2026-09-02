package in.samrasyafudz.orderservice.service;

import in.samrasyafudz.orderservice.client.ProductDto;
import in.samrasyafudz.orderservice.client.ProductServiceClient;
import in.samrasyafudz.orderservice.dto.AddToCartRequest;
import in.samrasyafudz.orderservice.dto.CartItemResponse;
import in.samrasyafudz.orderservice.dto.CartResponse;
import in.samrasyafudz.orderservice.entity.CartItem;
import in.samrasyafudz.orderservice.exception.InsufficientStockException;
import in.samrasyafudz.orderservice.repository.CartItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CartService {

    private final CartItemRepository cartItemRepository;
    private final ProductServiceClient productServiceClient;

    public CartService(CartItemRepository cartItemRepository, ProductServiceClient productServiceClient) {
        this.cartItemRepository = cartItemRepository;
        this.productServiceClient = productServiceClient;
    }

    @Transactional(readOnly = true)
    public CartResponse getCart(Long userId) {
        var responses = cartItemRepository.findByUserIdOrderByIdAsc(userId).stream()
                .map(this::toResponse)
                .toList();
        return new CartResponse(responses);
    }

    @Transactional
    public CartResponse addToCart(Long userId, AddToCartRequest request) {
        ProductDto product = productServiceClient.getProduct(request.getProductId());
        ProductDto.VariantDto variant = product.getVariants().stream()
                .filter(v -> v.getId().equals(request.getVariantId()))
                .findFirst()
                .orElseThrow(() -> new InsufficientStockException(product.getName(), 0));

        if (variant.getStockQuantity() < request.getQuantity()) {
            throw new InsufficientStockException(product.getName(), variant.getStockQuantity());
        }

        CartItem item = cartItemRepository.findByUserIdAndVariantId(userId, request.getVariantId())
                .orElseGet(CartItem::new);

        item.setUserId(userId);
        item.setProductId(product.getId());
        item.setVariantId(variant.getId());
        item.setProductName(product.getName());
        item.setWeightGrams(variant.getWeightGrams());
        item.setUnitPrice(variant.getPrice());
        item.setQuantity(
                item.getQuantity() == null ? request.getQuantity() : item.getQuantity() + request.getQuantity()
        );

        cartItemRepository.save(item);
        return getCart(userId);
    }

    @Transactional
    public CartResponse updateQuantity(Long userId, Long productId, Long variantId, Integer quantity) {
        List<CartItem> items = cartItemRepository.findByUserIdOrderByIdAsc(userId);
        if (items != null) {
            for (CartItem item : items) {
                if (item.getProductId().equals(productId) && item.getVariantId().equals(variantId)) {
                    if (quantity <= 0) {
                        cartItemRepository.delete(item);
                        continue;
                    }
                    item.setQuantity(quantity);
                    cartItemRepository.save(item);
                }
            }
        }
        return getCart(userId);
    }

    @Transactional
    public CartResponse removeItem(Long userId, Long productId, Long variantId) {
        List<CartItem> items = cartItemRepository.findByUserIdOrderByIdAsc(userId);
        if (items != null) {
            for (CartItem item : items) {
                if (item.getProductId().equals(productId) && item.getVariantId().equals(variantId)) {
                    cartItemRepository.delete(item);
                }
            }
        }
        return getCart(userId);
    }

    private CartItemResponse toResponse(CartItem item) {
        return new CartItemResponse(
                item.getId(), item.getProductId(), item.getVariantId(), item.getProductName(),
                item.getWeightGrams(), item.getUnitPrice(), item.getQuantity()
        );
    }
}